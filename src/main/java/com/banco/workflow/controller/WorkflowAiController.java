package com.banco.workflow.controller;

import com.banco.workflow.service.WorkflowAiService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/v1/ai")
@RequiredArgsConstructor
public class WorkflowAiController {

    private final WorkflowAiService workflowAiService;

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(workflowAiService.health());
    }

    /**
     * CU-12: Generar diagrama BPMN base desde lenguaje natural.
     * URL efectiva: POST /api/v1/ai/diagrams/generate.
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/diagrams/generate")
    public ResponseEntity<Map<String, Object>> generateDiagram(@RequestBody Map<String, Object> request) {
        return ResponseEntity.ok(workflowAiService.generateDiagram(request));
    }

    /**
     * CU-11: Convertir OCR de imagen de formulario en FormDefinition editable.
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/ocr/form")
    public ResponseEntity<Map<String, Object>> extractForm(@RequestBody Map<String, Object> request) {
        return ResponseEntity.ok(workflowAiService.extractForm(request));
    }

    /**
     * CU-19: Extraer datos estructurados de documento cargado por funcionario.
     */
    @PreAuthorize("hasAnyRole('ADMIN','REVISOR','GERENTE')")
    @PostMapping("/ocr/document")
    public ResponseEntity<Map<String, Object>> extractDocument(@RequestBody Map<String, Object> request) {
        return ResponseEntity.ok(workflowAiService.extractDocument(request));
    }

    /**
     * CU-13: Analizar cuellos de botella de un WorkflowDefinition y tareas.
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/simulation/bottleneck")
    public ResponseEntity<Map<String, Object>> analyzeBottleneck(@RequestBody Map<String, Object> request) {
        return ResponseEntity.ok(workflowAiService.analyzeBottleneck(request));
    }
}
