package com.banco.workflow.controller;

import com.banco.workflow.model.DynamicForm;
import com.banco.workflow.service.DynamicFormService;
import com.banco.workflow.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * CU-10: Gestionar Formularios Dinámicos.
 * CU-11: Receptor de formulario desde OCR (payload parseado por workflow-ai-service).
 *
 * Todos los endpoints filtran por tenantEmpresa del actor autenticado.
 */
@RestController
@RequestMapping("/v1/forms")
@RequiredArgsConstructor
@Slf4j
public class DynamicFormController {

    private final DynamicFormService dynamicFormService;
    private final UserService userService;

    /**
     * CU-10: Listar formularios activos de la empresa del usuario autenticado.
     * Accesible por admin y funcionarios (para ver formularios disponibles).
     */
    @PreAuthorize("hasAnyRole('ADMIN','REVISOR','GERENTE')")
    @GetMapping
    public ResponseEntity<List<DynamicForm>> listForms() {
        String empresa = requireEmpresa();
        return ResponseEntity.ok(dynamicFormService.getFormsByEmpresa(empresa));
    }

    /**
     * CU-10: Obtener un formulario por ID.
     */
    @PreAuthorize("hasAnyRole('ADMIN','REVISOR','GERENTE')")
    @GetMapping("/{id}")
    public ResponseEntity<DynamicForm> getForm(@PathVariable String id) {
        String empresa = requireEmpresa();
        return dynamicFormService.getFormById(id, empresa)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * CU-10: Crear formulario dinámico.
     * Solo admins pueden crear formularios del catálogo.
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<DynamicForm> createForm(@RequestBody DynamicForm form) {
        DynamicForm created = dynamicFormService.createForm(form);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * CU-10: Actualizar un formulario existente.
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<DynamicForm> updateForm(
            @PathVariable String id,
            @RequestBody DynamicForm form) {
        return ResponseEntity.ok(dynamicFormService.updateForm(id, form));
    }

    /**
     * CU-10: Desactivar (eliminar lógicamente) un formulario.
     */
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteForm(@PathVariable String id) {
        dynamicFormService.deleteForm(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * CU-11: Recibir campos detectados por el servicio OCR (workflow-ai-service).
     *
     * El Agente 5 procesa la imagen y envía:
     * {
     *   "formName": "Solicitud de Préstamo",
     *   "description": "Detectado desde imagen",
     *   "fields": [ { "name": "nombre", "label": "Nombre", "type": "text", "required": true } ]
     * }
     *
     * Este endpoint persiste el formulario ya parseado en el catálogo de la empresa.
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/from-ocr")
    public ResponseEntity<DynamicForm> createFromOcr(@RequestBody Map<String, Object> payload) {
        String formName = String.valueOf(payload.getOrDefault("formName", ""));
        String description = String.valueOf(payload.getOrDefault("description", ""));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rawFields = (List<Map<String, Object>>) payload.get("fields");
        if (rawFields == null || rawFields.isEmpty()) {
            throw new IllegalArgumentException("El payload de OCR no contiene campos detectados");
        }

        List<DynamicForm.DynamicFormField> fields = rawFields.stream()
                .map(this::mapToField)
                .toList();

        DynamicForm created = dynamicFormService.createFromOcr(formName, description, fields);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    private DynamicForm.DynamicFormField mapToField(Map<String, Object> raw) {
        DynamicForm.DynamicFormField field = new DynamicForm.DynamicFormField();
        field.setName(String.valueOf(raw.getOrDefault("name", "")));
        field.setLabel(String.valueOf(raw.getOrDefault("label", raw.getOrDefault("name", ""))));
        field.setType(String.valueOf(raw.getOrDefault("type", "text")));
        field.setRequired(Boolean.parseBoolean(String.valueOf(raw.getOrDefault("required", false))));
        field.setPlaceholder(raw.containsKey("placeholder") ? String.valueOf(raw.get("placeholder")) : null);
        field.setDefaultValue(raw.containsKey("defaultValue") ? String.valueOf(raw.get("defaultValue")) : null);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rawOptions = (List<Map<String, Object>>) raw.get("options");
        if (rawOptions != null) {
            List<DynamicForm.DynamicFormField.FieldOption> options = rawOptions.stream()
                    .map(o -> new DynamicForm.DynamicFormField.FieldOption(
                            String.valueOf(o.getOrDefault("label", "")),
                            String.valueOf(o.getOrDefault("value", ""))
                    ))
                    .toList();
            field.setOptions(options);
        }
        return field;
    }

    private String requireEmpresa() {
        return userService.getCurrentAuthenticatedUser()
                .map(u -> u.getEmpresa())
                .orElseThrow(() -> new RuntimeException("No hay usuario autenticado"));
    }
}
