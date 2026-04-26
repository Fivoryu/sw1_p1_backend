package com.banco.workflow.controller;

import com.banco.workflow.dto.ApiResponse;
import com.banco.workflow.model.Task;
import com.banco.workflow.service.TaskService;
import com.banco.workflow.service.TemporalWorkflowService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/tasks")
@RequiredArgsConstructor
@Slf4j
public class TaskController {

    private final TemporalWorkflowService workflowService;
    private final TaskService taskService;

    @GetMapping("/my")
    public ResponseEntity<List<Task>> getMyTasks() {
        return ResponseEntity.ok(taskService.getCurrentUserTasks());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Task> getTask(@PathVariable String id) {
        return taskService.getTaskById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * POST /v1/tasks/:taskId/complete - Completar una tarea
     */
    @PostMapping("/{instanceId}/{taskId}/complete")
    public ResponseEntity<Void> completeTask(
            @PathVariable String instanceId,
            @PathVariable String taskId,
            @RequestBody ApiResponse.TaskCompleteRequest request) {
        try {
            workflowService.completeTask(instanceId, taskId, request.getVariables());
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("Error completando tarea", e);
            return ResponseEntity.badRequest().build();
        }
    }
}
