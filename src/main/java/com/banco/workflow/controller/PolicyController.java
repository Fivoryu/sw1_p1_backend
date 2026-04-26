package com.banco.workflow.controller;

import com.banco.workflow.dto.ApiResponse;
import com.banco.workflow.model.Policy;
import com.banco.workflow.model.WorkflowDefinition;
import com.banco.workflow.service.PolicyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/policies")
@RequiredArgsConstructor
@Slf4j
public class PolicyController {

    private final PolicyService policyService;

    /**
     * POST /v1/policies - Crear nueva política
     */
    @PostMapping
    public ResponseEntity<Policy> createPolicy(
            @RequestBody ApiResponse.PolicyRequest request) {
        try {
            Policy policy = policyService.createPolicy(
                    request.getName(),
                    request.getDescription(),
                    request.getBpmnXml(),
                    request.getDepartments(),
                    request.getForms(),
                    request.getCollaborationEnabled(),
                    request.getCollaborationMode()
            );

            return ResponseEntity.ok(policy);
        } catch (Exception e) {
            log.error("Error creando política", e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * GET /v1/policies - Listar políticas para backoffice
     */
    @GetMapping
    public ResponseEntity<List<Policy>> listPolicies() {
        return ResponseEntity.ok(policyService.getAllPolicies());
    }

    /**
     * GET /v1/policies/:id - Obtener policy por ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<Policy> getPolicy(@PathVariable String id) {
        return policyService.getPolicyById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * PUT /v1/policies/:id - Actualizar policy (nueva versión)
     */
    @PutMapping("/{id}")
    public ResponseEntity<Policy> updatePolicy(
            @PathVariable String id,
            @RequestBody ApiResponse.PolicyRequest request) {
        try {
            Policy policy = policyService.updatePolicy(
                    id,
                    request.getName(),
                    request.getDescription(),
                    request.getBpmnXml(),
                    request.getDepartments(),
                    request.getForms(),
                    request.getCollaborationEnabled(),
                    request.getCollaborationMode()
            );

            return ResponseEntity.ok(policy);
        } catch (Exception e) {
            log.error("Error actualizando política", e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * DELETE /v1/policies/:id - Desactivar policy
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivatePolicy(@PathVariable String id) {
        try {
            policyService.deactivatePolicy(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{id}/publish")
    public ResponseEntity<Void> publishPolicy(@PathVariable String id) {
        try {
            policyService.publishPolicy(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{id}/archive")
    public ResponseEntity<Void> archivePolicy(@PathVariable String id) {
        try {
            policyService.deactivatePolicy(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{id}/definition")
    public ResponseEntity<WorkflowDefinition> getWorkflowDefinition(@PathVariable String id) {
        return policyService.getPublishedDefinition(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
