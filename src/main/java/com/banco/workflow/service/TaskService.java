package com.banco.workflow.service;

import com.banco.workflow.model.Task;
import com.banco.workflow.model.User;
import com.banco.workflow.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class TaskService {

    private final TaskRepository taskRepository;
    private final UserService userService;
    private final WorkflowRuntimeService workflowRuntimeService;

    public Optional<Task> getTaskById(String id) {
        return taskRepository.findById(id);
    }

    public List<Task> getTasksByAssignee(String assignee) {
        LinkedHashSet<Task> tasks = new LinkedHashSet<>(taskRepository.findByAssignee(assignee));
        tasks.addAll(taskRepository.findByCandidateRole(assignee));
        return tasks.stream().toList();
    }

    public List<Task> getCurrentUserTasks() {
        User user = userService.getCurrentAuthenticatedUser()
                .orElseThrow(() -> new RuntimeException("No hay usuario autenticado"));
        return workflowRuntimeService.getOpenTasksForUser(user);
    }

    public void updateTask(Task task) {
        taskRepository.save(task);
    }

    public List<Task> getNextTasks(String processId) {
        return taskRepository.findByProcessInstanceIdAndStatus(processId, "PENDING");
    }

    public List<Task> getTasksByStatus(String status, Integer limit) {
        return taskRepository.findByStatus(status).stream().limit(limit).toList();
    }
}
