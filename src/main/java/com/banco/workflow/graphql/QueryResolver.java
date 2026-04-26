package com.banco.workflow.graphql;

import com.banco.workflow.model.DocumentUpload;
import com.banco.workflow.model.Notification;
import com.banco.workflow.model.Policy;
import com.banco.workflow.model.ProcessInstance;
import com.banco.workflow.model.Task;
import com.banco.workflow.model.User;
import com.banco.workflow.service.DocumentService;
import com.banco.workflow.service.NotificationService;
import com.banco.workflow.service.PolicyService;
import com.banco.workflow.service.ProcessInstanceService;
import com.banco.workflow.service.TaskService;
import com.banco.workflow.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
@Slf4j
public class QueryResolver {

    private final ProcessInstanceService processInstanceService;
    private final TaskService taskService;
    private final PolicyService policyService;
    private final DocumentService documentService;
    private final NotificationService notificationService;
    private final UserService userService;

    @QueryMapping
    public List<ProcessInstance> processes(@Argument String userId) {
        return processInstanceService.getProcessesByUserId(userId);
    }

    @QueryMapping
    public ProcessInstance process(@Argument String id) {
        return processInstanceService.getProcessById(id)
                .orElseThrow(() -> new RuntimeException("Process not found: " + id));
    }

    @QueryMapping
    public List<Task> tasks(@Argument String assignee) {
        return taskService.getTasksByAssignee(assignee);
    }

    @QueryMapping
    public Task task(@Argument String id) {
        return taskService.getTaskById(id)
                .orElseThrow(() -> new RuntimeException("Task not found: " + id));
    }

    @QueryMapping
    public List<Policy> policies() {
        return policyService.getAllPolicies();
    }

    @QueryMapping
    public Policy policy(@Argument String id) {
        return policyService.getPolicyById(id)
                .orElseThrow(() -> new RuntimeException("Policy not found: " + id));
    }

    @QueryMapping
    public List<Policy> publishedPolicies() {
        return policyService.getPublishedPolicies();
    }

    @QueryMapping
    public List<DocumentUpload> documents(@Argument String processInstanceId) {
        return documentService.getDocumentsByProcessId(processInstanceId);
    }

    @QueryMapping
    public DocumentUpload document(@Argument String id) {
        return documentService.getDocumentById(id)
                .orElseThrow(() -> new RuntimeException("Document not found: " + id));
    }

    @QueryMapping
    public List<Notification> notifications(@Argument String userId, @Argument Integer limit) {
        return notificationService.getNotificationsByUserId(userId, limit != null ? limit : 50);
    }

    @QueryMapping
    public List<Notification> unreadNotifications(@Argument String userId) {
        return notificationService.getUnreadNotifications(userId);
    }

    @QueryMapping
    public User currentUser() {
        return userService.getCurrentAuthenticatedUser().orElse(null);
    }

    @QueryMapping
    public User user(@Argument String id) {
        return userService.getUserById(id)
                .orElseThrow(() -> new RuntimeException("User not found: " + id));
    }

    @QueryMapping
    public Map<String, Object> userStatistics(@Argument String userId) {
        return userService.getUserStatistics(userId);
    }

    @QueryMapping
    public List<ProcessInstance> processHistory(@Argument String userId, @Argument Integer limit) {
        return processInstanceService.getCompletedProcesses(userId, limit != null ? limit : 20);
    }

    @QueryMapping("processesByStatus")
    public List<ProcessInstance> processesByStatus(@Argument String status, @Argument Integer limit) {
        return processInstanceService.getProcessesByStatus(status, limit != null ? limit : 20);
    }

    @QueryMapping("tasksByStatus")
    public List<Task> tasksByStatus(@Argument String status, @Argument Integer limit) {
        return taskService.getTasksByStatus(status, limit != null ? limit : 20);
    }
}
