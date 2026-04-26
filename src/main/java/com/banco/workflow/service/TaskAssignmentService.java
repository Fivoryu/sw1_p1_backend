package com.banco.workflow.service;

import com.banco.workflow.model.BpmnNode;
import com.banco.workflow.model.ProcessInstance;
import com.banco.workflow.model.Task;
import com.banco.workflow.model.User;
import com.banco.workflow.config.TaskQueueWebSocketHandler;
import com.banco.workflow.repository.TaskRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;

@Service
public class TaskAssignmentService {

    private final TaskRepository taskRepository;
    private final NotificationService notificationService;
    private final TaskQueueWebSocketHandler taskQueueWebSocketHandler;

    public TaskAssignmentService(
            TaskRepository taskRepository,
            NotificationService notificationService,
            TaskQueueWebSocketHandler taskQueueWebSocketHandler
    ) {
        this.taskRepository = taskRepository;
        this.notificationService = notificationService;
        this.taskQueueWebSocketHandler = taskQueueWebSocketHandler;
    }

    public Task createHumanTask(ProcessInstance instance, BpmnNode node) {
        Task task = new Task();
        task.setId(UUID.randomUUID().toString());
        task.setProcessInstanceId(instance.getId());
        task.setNodeId(node.getId());
        task.setNodeName(node.getName());
        task.setNodeType(node.getType());
        task.setAssignee(null);
        task.setCandidateRole(node.getAssignedRole());
        task.setDepartmentAssigned(node.getAssignedRole());
        task.setStatus("PENDING");
        task.setTitle(node.getName());
        task.setDescription("Tarea humana generada por el motor de workflow");
        task.setPriority("MEDIUM");
        task.setCreatedAt(LocalDateTime.now());
        task.setDueDate(LocalDateTime.now().plusDays(2));
        task.setTaskType("USER_TASK");
        task.setFormData(instance.getVariables());
        task.setVariables(instance.getVariables());
        task.setRequiredDocuments(Collections.emptyList());
        task.setFormId(node.getFormId());
        task.setCustomerName(String.valueOf(instance.getVariables().getOrDefault("clienteNombre",
                instance.getVariables().getOrDefault("nombres", "Cliente"))));
        task.setCustomerDni(String.valueOf(instance.getVariables().getOrDefault("clienteDni",
                instance.getVariables().getOrDefault("documento", "S/N"))));
        Task saved = taskRepository.save(task);

        notificationService.createNotification(
                saved.getCandidateRole(),
                "Nueva tarea disponible",
                "Se generó la tarea " + node.getName(),
                "TASK_ASSIGNED",
                saved.getId()
        );
        taskQueueWebSocketHandler.broadcastToRole(saved.getCandidateRole(),
                "{\"type\":\"TASK_CREATED\",\"taskId\":\"" + saved.getId() + "\"}");
        return saved;
    }

    public List<Task> getOpenTasksForUser(User user) {
        LinkedHashSet<Task> tasks = new LinkedHashSet<>();
        tasks.addAll(taskRepository.findByAssignee(user.getUsername()));
        if (user.getRoles() != null) {
            for (String role : user.getRoles()) {
                tasks.addAll(taskRepository.findByCandidateRole(role));
            }
        }
        return tasks.stream()
                .filter(task -> "PENDING".equals(task.getStatus()) || "IN_PROGRESS".equals(task.getStatus()))
                .toList();
    }
}
