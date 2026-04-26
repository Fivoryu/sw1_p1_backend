package com.banco.workflow.service;

import com.banco.workflow.model.FormDefinition;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class FormValidationService {

    public Map<String, String> validate(FormDefinition formDefinition, Map<String, Object> values) {
        Map<String, String> errors = new HashMap<>();
        if (formDefinition == null) {
            return errors;
        }

        Map<String, Object> safeValues = values != null ? values : Map.of();
        for (FormDefinition.FormFieldDefinition field : formDefinition.getFields()) {
            Object value = safeValues.get(field.getName());
            if (field.isRequired() && isBlank(value)) {
                errors.put(field.getName(), "El campo " + field.getLabel() + " es obligatorio");
                continue;
            }

            if (value != null && !isBlank(value) && "select".equals(field.getType())) {
                List<String> options = field.getOptions().stream()
                        .map(FormDefinition.FormFieldOption::getValue)
                        .toList();
                if (!options.isEmpty() && !options.contains(String.valueOf(value))) {
                    errors.put(field.getName(), "El valor del campo " + field.getLabel() + " no es válido");
                }
            }
        }

        return errors;
    }

    private boolean isBlank(Object value) {
        return value == null || String.valueOf(value).isBlank();
    }
}
