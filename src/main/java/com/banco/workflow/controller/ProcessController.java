package com.banco.workflow.controller;

import com.banco.workflow.dto.ApiResponse;
import com.banco.workflow.model.ProcessInstance;
import com.banco.workflow.service.ProcessInstanceService;
import com.banco.workflow.service.TemporalWorkflowService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/processes")
@RequiredArgsConstructor
@Slf4j
public class ProcessController {

    private final TemporalWorkflowService workflowService;
    private final ProcessInstanceService processInstanceService;

    /**
     * POST /v1/processes - Iniciar nueva instancia de workflow
     */
    @PostMapping
    public ResponseEntity<ApiResponse.ProcessStartResponse> startProcess(
            @RequestBody ApiResponse.ProcessStartRequest request) {
        try {
            ProcessInstance instance = workflowService.startWorkflow(
                    request.getPolicyId(),
                    request.getVariables()
            );

            return ResponseEntity.ok(ApiResponse.ProcessStartResponse.builder()
                    .id(instance.getId())
                    .temporalProcessInstanceId(instance.getTemporalProcessInstanceId())
                    .status(instance.getStatus())
                    .build());
        } catch (Exception e) {
            log.error("Error iniciando workflow", e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * GET /v1/processes/:id - Obtener estado de instancia
     */
    @GetMapping("/{id}")
    public ResponseEntity<ProcessInstance> getProcessStatus(@PathVariable String id) {
        try {
            return ResponseEntity.ok(workflowService.getInstanceStatus(id));
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * GET /v1/processes/:id/history - Obtener historial
     */
    @GetMapping("/{id}/history")
    public ResponseEntity<List<ProcessInstance.HistoryEntry>> getHistory(@PathVariable String id) {
        try {
            return ResponseEntity.ok(workflowService.getInstanceHistory(id));
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * GET /v1/processes/status/:status - Listar por estado
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<List<ProcessInstance>> getInstancesByStatus(@PathVariable String status) {
        return ResponseEntity.ok(workflowService.getInstancesByStatus(status));
    }

    /**
     * GET /v1/processes/policy/:policyId - Listar por política
     */
    @GetMapping("/policy/{policyId}")
    public ResponseEntity<List<ProcessInstance>> getInstancesByPolicy(@PathVariable String policyId) {
        return ResponseEntity.ok(workflowService.getInstancesByPolicy(policyId));
    }

    @GetMapping("/my")
    public ResponseEntity<List<ProcessInstance>> getCurrentUserProcesses() {
        return ResponseEntity.ok(processInstanceService.getCurrentUserProcesses());
    }
}
