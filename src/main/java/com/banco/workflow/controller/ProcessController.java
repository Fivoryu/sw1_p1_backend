package com.banco.workflow.controller;

import com.banco.workflow.dto.ApiResponse;
import com.banco.workflow.model.ProcessInstance;
import com.banco.workflow.service.ProcessInstanceService;
import com.banco.workflow.service.TemporalWorkflowService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * CU-23: Endpoint de inicio de trámite (consumido por Flutter y web).
 * Cada operación respeta tenantEmpresa del iniciador.
 */
@RestController
@RequestMapping("/v1/processes")
@RequiredArgsConstructor
@Slf4j
public class ProcessController {

    private final TemporalWorkflowService workflowService;
    private final ProcessInstanceService processInstanceService;

    /**
     * CU-23: POST /v1/processes — Iniciar nueva instancia de workflow.
     * Accesible por ADMIN, CLIENTE y funcionarios.
     */
    @PreAuthorize("isAuthenticated()")
    @PostMapping
    public ResponseEntity<ApiResponse.ProcessStartResponse> startProcess(
            @RequestBody ApiResponse.ProcessStartRequest request) throws Exception {
        ProcessInstance instance = workflowService.startWorkflow(
                request.getPolicyId(),
                request.getVariables()
        );
        return ResponseEntity.ok(ApiResponse.ProcessStartResponse.builder()
                .id(instance.getId())
                .temporalProcessInstanceId(instance.getTemporalProcessInstanceId())
                .status(instance.getStatus())
                .build());
    }

    /**
     * GET /v1/processes/:id — Obtener estado de instancia.
     */
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{id}")
    public ResponseEntity<ProcessInstance> getProcessStatus(@PathVariable String id) throws Exception {
        return ResponseEntity.ok(workflowService.getInstanceStatus(id));
    }

    /**
     * GET /v1/processes/:id/history — Obtener historial de la instancia.
     */
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{id}/history")
    public ResponseEntity<List<ProcessInstance.HistoryEntry>> getHistory(@PathVariable String id) throws Exception {
        return ResponseEntity.ok(workflowService.getInstanceHistory(id));
    }

    /**
     * GET /v1/processes/status/:status — Listar instancias por estado (solo admins).
     */
    @PreAuthorize("hasAnyRole('ADMIN','GERENTE')")
    @GetMapping("/status/{status}")
    public ResponseEntity<List<ProcessInstance>> getInstancesByStatus(@PathVariable String status) {
        return ResponseEntity.ok(workflowService.getInstancesByStatus(status));
    }

    /**
     * GET /v1/processes/policy/:policyId — Listar instancias por política (solo admins).
     */
    @PreAuthorize("hasAnyRole('ADMIN','GERENTE')")
    @GetMapping("/policy/{policyId}")
    public ResponseEntity<List<ProcessInstance>> getInstancesByPolicy(@PathVariable String policyId) {
        return ResponseEntity.ok(workflowService.getInstancesByPolicy(policyId));
    }

    /**
     * GET /v1/processes/my — Trámites iniciados por el usuario autenticado.
     */
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/my")
    public ResponseEntity<List<ProcessInstance>> getCurrentUserProcesses() {
        return ResponseEntity.ok(processInstanceService.getCurrentUserProcesses());
    }
}
