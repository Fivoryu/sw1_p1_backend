package com.banco.workflow.service;

import com.banco.workflow.model.DynamicForm;
import com.banco.workflow.model.User;
import com.banco.workflow.repository.DynamicFormRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * CU-10: Gestión del catálogo de formularios dinámicos por empresa.
 * Los formularios son reutilizables entre políticas del mismo tenant.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DynamicFormService {

    private final DynamicFormRepository dynamicFormRepository;
    private final UserService userService;

    public List<DynamicForm> getFormsByEmpresa(String empresa) {
        return dynamicFormRepository.findByTenantEmpresaAndActive(empresa, true);
    }

    public Optional<DynamicForm> getFormById(String id, String empresa) {
        return dynamicFormRepository.findById(id)
                .filter(f -> empresa == null || empresa.equalsIgnoreCase(f.getTenantEmpresa()));
    }

    public DynamicForm createForm(DynamicForm form) {
        User actor = requireActor();
        validateForm(form);

        if (dynamicFormRepository.existsByNameAndTenantEmpresa(form.getName().trim(), actor.getEmpresa())) {
            throw new IllegalArgumentException("Ya existe un formulario con ese nombre en tu empresa: " + form.getName());
        }

        validateFields(form);

        LocalDateTime now = LocalDateTime.now();
        form.setId(UUID.randomUUID().toString());
        form.setName(form.getName().trim());
        form.setTenantEmpresa(actor.getEmpresa());
        form.setCreatedByUserId(actor.getId());
        form.setActive(true);
        form.setCreatedAt(now);
        form.setUpdatedAt(now);

        DynamicForm saved = dynamicFormRepository.save(form);
        log.info("Formulario creado: {} en empresa {}", saved.getName(), saved.getTenantEmpresa());
        return saved;
    }

    public DynamicForm updateForm(String id, DynamicForm updatedData) {
        User actor = requireActor();
        DynamicForm existing = requireFormInEmpresa(id, actor.getEmpresa());

        if (updatedData.getName() != null && !updatedData.getName().trim().equals(existing.getName())) {
            if (dynamicFormRepository.existsByNameAndTenantEmpresa(updatedData.getName().trim(), actor.getEmpresa())) {
                throw new IllegalArgumentException("Ya existe un formulario con ese nombre: " + updatedData.getName());
            }
            existing.setName(updatedData.getName().trim());
        }
        if (updatedData.getTitle() != null) {
            existing.setTitle(updatedData.getTitle());
        }
        if (updatedData.getDescription() != null) {
            existing.setDescription(updatedData.getDescription());
        }
        if (updatedData.getFields() != null) {
            validateFieldsRaw(updatedData.getFields());
            existing.setFields(updatedData.getFields());
        }
        existing.setUpdatedAt(LocalDateTime.now());

        DynamicForm saved = dynamicFormRepository.save(existing);
        log.info("Formulario actualizado: {}", saved.getName());
        return saved;
    }

    public void deleteForm(String id) {
        User actor = requireActor();
        DynamicForm form = requireFormInEmpresa(id, actor.getEmpresa());
        form.setActive(false);
        form.setUpdatedAt(LocalDateTime.now());
        dynamicFormRepository.save(form);
        log.info("Formulario desactivado: {}", form.getName());
    }

    /**
     * CU-11: Crea un formulario a partir de campos ya parseados por el servicio OCR (Agente 5).
     * El endpoint Python envía la estructura detectada; este método la persiste como DynamicForm.
     */
    public DynamicForm createFromOcr(String formName, String description,
                                     List<DynamicForm.DynamicFormField> detectedFields) {
        User actor = requireActor();
        if (formName == null || formName.isBlank()) {
            throw new IllegalArgumentException("El nombre del formulario OCR es obligatorio");
        }
        if (detectedFields == null || detectedFields.isEmpty()) {
            throw new IllegalArgumentException("El servicio OCR no detectó campos en la imagen");
        }

        assignFieldIds(detectedFields);

        DynamicForm form = DynamicForm.builder()
                .id(UUID.randomUUID().toString())
                .name(formName.trim())
                .title(formName.trim())
                .description(description != null ? description : "Formulario generado por OCR")
                .tenantEmpresa(actor.getEmpresa())
                .createdByUserId(actor.getId())
                .active(true)
                .fields(detectedFields)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        DynamicForm saved = dynamicFormRepository.save(form);
        log.info("Formulario creado desde OCR: {} con {} campos", saved.getName(), detectedFields.size());
        return saved;
    }

    private DynamicForm requireFormInEmpresa(String id, String empresa) {
        DynamicForm form = dynamicFormRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Formulario no encontrado: " + id));
        if (empresa != null && !empresa.isBlank()
                && form.getTenantEmpresa() != null
                && !empresa.equalsIgnoreCase(form.getTenantEmpresa())) {
            throw new IllegalArgumentException("No puedes modificar formularios de otra empresa");
        }
        return form;
    }

    private User requireActor() {
        return userService.getCurrentAuthenticatedUser()
                .orElseThrow(() -> new RuntimeException("No hay usuario autenticado"));
    }

    private void validateForm(DynamicForm form) {
        if (form.getName() == null || form.getName().isBlank()) {
            throw new IllegalArgumentException("El nombre del formulario es obligatorio");
        }
        if (form.getFields() == null || form.getFields().isEmpty()) {
            throw new IllegalArgumentException("El formulario debe tener al menos un campo");
        }
    }

    private void validateFields(DynamicForm form) {
        validateFieldsRaw(form.getFields());
    }

    private void validateFieldsRaw(List<DynamicForm.DynamicFormField> fields) {
        if (fields == null) return;
        var validTypes = java.util.Set.of("text", "number", "boolean", "textarea", "select", "date", "file");
        for (DynamicForm.DynamicFormField field : fields) {
            if (field.getName() == null || field.getName().isBlank()) {
                throw new IllegalArgumentException("Cada campo debe tener un nombre (name)");
            }
            if (field.getLabel() == null || field.getLabel().isBlank()) {
                throw new IllegalArgumentException("El campo '" + field.getName() + "' debe tener una etiqueta (label)");
            }
            if (field.getType() == null || !validTypes.contains(field.getType())) {
                throw new IllegalArgumentException("El tipo '" + field.getType() + "' del campo '" + field.getName()
                        + "' no es válido. Tipos permitidos: " + validTypes);
            }
            if ("select".equals(field.getType()) && (field.getOptions() == null || field.getOptions().isEmpty())) {
                throw new IllegalArgumentException("El campo tipo 'select' '" + field.getName() + "' debe tener opciones");
            }
            if (field.getId() == null || field.getId().isBlank()) {
                field.setId(UUID.randomUUID().toString());
            }
        }
    }

    private void assignFieldIds(List<DynamicForm.DynamicFormField> fields) {
        for (DynamicForm.DynamicFormField field : fields) {
            if (field.getId() == null || field.getId().isBlank()) {
                field.setId(UUID.randomUUID().toString());
            }
        }
    }
}
