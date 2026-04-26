package com.banco.workflow.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

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
    
    private DynamicFormField[] fields;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DynamicFormField {
        private String name;
        private String type;  // text, number, boolean, textarea, select, date
        private String label;
        private boolean required;
        private String defaultValue;
        private String pattern;  // Regex para validación
        private String[] options;  // Para select
    }
}
