package com.banco.workflow.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Catálogo de formularios dinámicos reutilizables (CU-10).
 * Cada formulario pertenece a una empresa (tenantEmpresa) y puede
 * asociarse a tareas humanas de cualquier política de esa empresa.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "forms")
public class DynamicForm {

    @Id
    private String id;

    private String name;
    private String title;
    private String description;
    private String tenantEmpresa;
    private String createdByUserId;
    private boolean active;

    private List<DynamicFormField> fields;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DynamicFormField {
        private String id;
        private String name;
        private String type;       // text, number, boolean, textarea, select, date, file
        private String label;
        private boolean required;
        private String defaultValue;
        private String placeholder;
        private String pattern;    // Regex de validación
        private Integer minLength;
        private Integer maxLength;
        private List<FieldOption> options;  // Para tipo select

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        @Builder
        public static class FieldOption {
            private String label;
            private String value;
        }
    }
}
