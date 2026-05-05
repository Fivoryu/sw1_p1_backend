package com.banco.workflow.controller;

import com.banco.workflow.dto.ApiResponse;
import com.banco.workflow.model.Policy;
import com.banco.workflow.model.WorkflowDefinition;
import com.banco.workflow.service.PolicyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<Policy> createPolicy(
            @RequestBody ApiResponse.PolicyRequest request) {
        try {
            Policy policy = policyService.createPolicy(
                    request.getName(),
                    request.getDescription(),
                    request.getBpmnXml(),
                    request.getUmlActivityJson(),
                    request.getUmlVersion(),
                    request.getDiagramNotation(),
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
    @PreAuthorize("hasAnyRole('ADMIN','REVISOR','GERENTE')")
    @GetMapping
    public ResponseEntity<List<Policy>> listPolicies() {
        return ResponseEntity.ok(policyService.getAllPolicies());
    }

    /**
     * GET /v1/policies/:id - Obtener policy por ID
     */
    @PreAuthorize("hasAnyRole('ADMIN','REVISOR','GERENTE')")
    @GetMapping("/{id}")
    public ResponseEntity<Policy> getPolicy(@PathVariable String id) {
        return policyService.getPolicyById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * PUT /v1/policies/:id - Actualizar policy (nueva versión)
     */
    @PreAuthorize("hasRole('ADMIN')")
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
                    request.getUmlActivityJson(),
                    request.getUmlVersion(),
                    request.getDiagramNotation(),
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
     * DELETE /v1/policies/:id - Borrar policy y dependencias (instancias, tareas, adjuntos, definiciones publicadas).
     * Para archivar sin borrar use POST /v1/policies/{id}/archive
     */
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePolicy(@PathVariable String id) {
        try {
            policyService.permanentlyDeletePolicy(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            String msg = e.getMessage() != null ? e.getMessage() : "";
            if (msg.contains("no encontrada")) {
                return ResponseEntity.notFound().build();
            }
            if (msg.contains("permiso") || msg.contains("dueño")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            if (msg.contains("ver o eliminar")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            return ResponseEntity.badRequest().build();
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
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

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{id}/archive")
    public ResponseEntity<Void> archivePolicy(@PathVariable String id) {
        try {
            policyService.deactivatePolicy(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PreAuthorize("hasAnyRole('ADMIN','REVISOR','GERENTE')")
    @GetMapping("/{id}/definition")
    public ResponseEntity<WorkflowDefinition> getWorkflowDefinition(@PathVariable String id) {
        return policyService.getPublishedDefinition(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
