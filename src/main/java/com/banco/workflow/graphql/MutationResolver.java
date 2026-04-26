package com.banco.workflow.graphql;

import com.banco.workflow.graphql.input.CompleteTaskInput;
import com.banco.workflow.graphql.input.UploadDocumentInput;
import com.banco.workflow.model.DocumentUpload;
import com.banco.workflow.model.Policy;
import com.banco.workflow.model.ProcessInstance;
import com.banco.workflow.model.Task;
import com.banco.workflow.service.DocumentService;
import com.banco.workflow.service.NotificationService;
import com.banco.workflow.service.PolicyService;
import com.banco.workflow.service.ProcessInstanceService;
import com.banco.workflow.service.TaskService;
import com.banco.workflow.service.TemporalWorkflowService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.stereotype.Controller;

import java.util.Base64;
import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
@Slf4j
public class MutationResolver {

    private final ProcessInstanceService processInstanceService;
    private final TaskService taskService;
    private final PolicyService policyService;
    private final DocumentService documentService;
    private final NotificationService notificationService;
    private final TemporalWorkflowService temporalWorkflowService;

    @MutationMapping
    public CompleteTaskResponse completeTask(@Argument CompleteTaskInput input) {
        try {
            Task task = taskService.getTaskById(input.getTaskId())
                    .orElseThrow(() -> new RuntimeException("Task not found"));

            temporalWorkflowService.completeTask(task.getProcessInstanceId(), task.getId(), input.getResult());

            return CompleteTaskResponse.builder()
                    .success(true)
                    .message("Task completed successfully")
                    .task(taskService.getTaskById(task.getId()).orElse(task))
                    .nextTasks(taskService.getNextTasks(task.getProcessInstanceId()))
                    .build();
        } catch (Exception e) {
            log.error("Error completing task", e);
            return CompleteTaskResponse.builder()
                    .success(false)
                    .message("Error: " + e.getMessage())
                    .build();
        }
    }

    @MutationMapping
    public CompleteTaskResponse rejectTask(@Argument String taskId, @Argument String reason) {
        try {
            Task task = taskService.getTaskById(taskId)
                    .orElseThrow(() -> new RuntimeException("Task not found"));
            task.setStatus("REJECTED");
            task.setRejectionReason(reason);
            taskService.updateTask(task);

            return CompleteTaskResponse.builder()
                    .success(true)
                    .message("Task rejected successfully")
                    .task(task)
                    .build();
        } catch (Exception e) {
            log.error("Error rejecting task", e);
            return CompleteTaskResponse.builder()
                    .success(false)
                    .message("Error: " + e.getMessage())
                    .build();
        }
    }

    @MutationMapping
    public DocumentUploadResponse uploadDocument(@Argument UploadDocumentInput input) {
        try {
            byte[] fileBytes = Base64.getDecoder().decode(input.getFileData());
            DocumentUpload document = documentService.saveDocument(
                    input.getProcessInstanceId(),
                    input.getFileName(),
                    fileBytes,
                    input.getMimeType()
            );

            return DocumentUploadResponse.builder()
                    .success(true)
                    .message("Document uploaded successfully")
                    .document(document)
                    .build();
        } catch (Exception e) {
            log.error("Error uploading document", e);
            return DocumentUploadResponse.builder()
                    .success(false)
                    .message("Error uploading document: " + e.getMessage())
                    .build();
        }
    }

    @MutationMapping
    public DocumentUploadResponse validateDocument(@Argument String documentId) {
        try {
            documentService.validateDocument(documentId);
            return DocumentUploadResponse.builder()
                    .success(true)
                    .message("Document validated successfully")
                    .build();
        } catch (Exception e) {
            return DocumentUploadResponse.builder()
                    .success(false)
                    .message("Error: " + e.getMessage())
                    .build();
        }
    }

    @MutationMapping
    public GenericResponse rejectDocument(@Argument String documentId, @Argument String reason) {
        try {
            documentService.rejectDocument(documentId, reason);
            return GenericResponse.builder().success(true).message("Document rejected successfully").build();
        } catch (Exception e) {
            return GenericResponse.builder().success(false).message("Error: " + e.getMessage()).build();
        }
    }

    @MutationMapping
    public ProcessInstance startProcess(@Argument Map<String, Object> input) {
        try {
            String policyId = (String) input.get("policyId");
            @SuppressWarnings("unchecked")
            Map<String, Object> variables = (Map<String, Object>) input.get("variables");
            String clientId = (String) input.get("clientId");
            return processInstanceService.createProcess(policyId, variables, clientId);
        } catch (Exception e) {
            throw new RuntimeException("Error starting process: " + e.getMessage(), e);
        }
    }

    @MutationMapping
    public GenericResponse pauseProcess(@Argument String processId) {
        try {
            processInstanceService.pauseProcess(processId);
            return GenericResponse.builder().success(true).message("Process paused successfully").build();
        } catch (Exception e) {
            return GenericResponse.builder().success(false).message("Error: " + e.getMessage()).build();
        }
    }

    @MutationMapping
    public GenericResponse resumeProcess(@Argument String processId) {
        try {
            processInstanceService.resumeProcess(processId);
            return GenericResponse.builder().success(true).message("Process resumed successfully").build();
        } catch (Exception e) {
            return GenericResponse.builder().success(false).message("Error: " + e.getMessage()).build();
        }
    }

    @MutationMapping
    public GenericResponse cancelProcess(@Argument String processId, @Argument String reason) {
        try {
            processInstanceService.cancelProcess(processId, reason);
            return GenericResponse.builder().success(true).message("Process cancelled successfully").build();
        } catch (Exception e) {
            return GenericResponse.builder().success(false).message("Error: " + e.getMessage()).build();
        }
    }

    @MutationMapping
    public Task reassignTask(@Argument String taskId, @Argument String newAssignee) {
        Task task = taskService.getTaskById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));
        task.setAssignee(newAssignee);
        taskService.updateTask(task);
        return task;
    }

    @MutationMapping
    public GenericResponse markNotificationAsRead(@Argument String id) {
        notificationService.markAsRead(id);
        return GenericResponse.builder().success(true).message("Notification marked as read").build();
    }

    @MutationMapping
    public GenericResponse markAllNotificationsAsRead(@Argument String userId) {
        notificationService.markAllAsRead(userId);
        return GenericResponse.builder().success(true).message("All notifications marked as read").build();
    }

    @MutationMapping
    public Policy createPolicy(@Argument Map<String, Object> input) {
        try {
            String bpmnXml = (String) (input.get("bpmnXml") != null ? input.get("bpmnXml") : input.get("bpmnDefinition"));
            return policyService.createPolicy(
                    (String) input.get("name"),
                    (String) input.get("description"),
                    bpmnXml,
                    List.of(),
                    List.of(),
                    input.get("collaborationEnabled") instanceof Boolean enabled ? enabled : null,
                    input.get("collaborationMode") instanceof String mode ? mode : null
            );
        } catch (Exception e) {
            throw new RuntimeException("Error creating policy: " + e.getMessage(), e);
        }
    }

    @MutationMapping
    public GenericResponse publishPolicy(@Argument String policyId) {
        try {
            policyService.publishPolicy(policyId);
            return GenericResponse.builder().success(true).message("Policy published successfully").build();
        } catch (Exception e) {
            log.error("Error publishing policy {}", policyId, e);
            return GenericResponse.builder().success(false).message("Error: " + e.getMessage()).build();
        }
    }

    @MutationMapping
    public GenericResponse deprecatePolicy(@Argument String policyId) {
        policyService.deprecatePolicy(policyId);
        return GenericResponse.builder().success(true).message("Policy deprecated successfully").build();
    }

    @MutationMapping
    public GenericResponse addComment(@Argument String processInstanceId, @Argument String comment) {
        processInstanceService.addComment(processInstanceId, comment);
        return GenericResponse.builder().success(true).message("Comment added successfully").build();
    }
}
