package com.banco.workflow.service;

import com.banco.workflow.model.Policy;
import com.banco.workflow.model.ProcessInstance;
import com.banco.workflow.model.Task;
import com.banco.workflow.model.User;
import com.banco.workflow.model.WorkflowDefinition;
import com.banco.workflow.repository.PolicyRepository;
import com.banco.workflow.repository.ProcessInstanceRepository;
import com.banco.workflow.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProcessInstanceService {

    private final ProcessInstanceRepository repository;
    private final PolicyRepository policyRepository;
    private final TaskRepository taskRepository;
    private final WorkflowRuntimeService workflowRuntimeService;
    private final UserService userService;
    private final WorkflowDefinitionService workflowDefinitionService;

    public List<ProcessInstance> getProcessesByUserId(String userId) {
        return repository.findByInitiatedByUserId(userId);
    }

    public List<ProcessInstance> getCurrentUserProcesses() {
        User user = userService.getCurrentAuthenticatedUser()
                .orElseThrow(() -> new RuntimeException("No hay usuario autenticado"));
        return getProcessesByUserId(user.getId());
    }

    public Optional<ProcessInstance> getProcessById(String id) {
        return repository.findById(id);
    }

    public ProcessInstance createProcess(String policyId, Map<String, Object> variables, String clientId) throws Exception {
        User initiator = userService.getCurrentAuthenticatedUser()
                .orElseGet(() -> userService.getUserByUsername(clientId).orElseThrow(() -> new RuntimeException("Usuario no encontrado")));

        Policy policy = policyRepository.findById(policyId)
                .orElseThrow(() -> new RuntimeException("Policy no encontrada"));
        if (!"PUBLISHED".equals(policy.getStatus())) {
            throw new RuntimeException("La política no está publicada");
        }
        WorkflowDefinition definition = workflowDefinitionService.getDefinitionByPolicy(policy.getId(), policy.getVersion())
                .orElseThrow(() -> new RuntimeException("La política no tiene una definición ejecutable publicada"));
        return workflowRuntimeService.startProcess(policy, definition, variables, initiator);
    }

    public void pauseProcess(String processId) throws Exception {
        ProcessInstance process = repository.findById(processId)
                .orElseThrow(() -> new RuntimeException("Process not found"));
        process.setStatus("PAUSED");
        repository.save(process);
    }

    public void resumeProcess(String processId) throws Exception {
        ProcessInstance process = repository.findById(processId)
                .orElseThrow(() -> new RuntimeException("Process not found"));
        process.setStatus("RUNNING");
        repository.save(process);
    }

    public void cancelProcess(String processId, String reason) throws Exception {
        ProcessInstance process = repository.findById(processId)
                .orElseThrow(() -> new RuntimeException("Process not found"));
        process.setStatus("CANCELLED");
        repository.save(process);
    }

    public void addComment(String processInstanceId, String comment) {
        ProcessInstance process = repository.findById(processInstanceId)
                .orElseThrow(() -> new RuntimeException("Process not found"));
        ProcessInstance.HistoryEntry entry = ProcessInstance.HistoryEntry.builder()
                .id(java.util.UUID.randomUUID().toString())
                .processInstanceId(processInstanceId)
                .nodeId("comment")
                .nodeType("COMMENT")
                .nodeName("Comentario")
                .timestamp(java.time.LocalDateTime.now())
                .status("COMMENT_ADDED")
                .taskData(Map.of("comment", comment))
                .taskResult(Map.of("comment", comment))
                .completedByUserId(userService.getCurrentAuthenticatedUser().map(User::getId).orElse("system"))
                .build();
        process.getHistory().add(entry);
        repository.save(process);
    }

    public List<ProcessInstance> getCompletedProcesses(String userId, Integer limit) {
        return repository.findByInitiatedByUserId(userId).stream()
                .filter(process -> "COMPLETED".equals(process.getStatus()) || "FAILED".equals(process.getStatus()))
                .limit(limit)
                .toList();
    }

    public List<ProcessInstance> getProcessesByStatus(String status, Integer limit) {
        return repository.findByStatus(status).stream().limit(limit).toList();
    }

    public List<Task> getOpenTasks(String processId) {
        return taskRepository.findByProcessInstanceIdAndStatus(processId, "PENDING");
    }
}
