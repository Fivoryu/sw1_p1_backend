package com.banco.workflow.model;

import java.util.ArrayList;
import java.util.List;

public class FormDefinition {

    private String id;
    private String name;
    private String description;
    private List<FormFieldDefinition> fields = new ArrayList<>();

    public FormDefinition() {
    }

    public FormDefinition(String id, String name, String description, List<FormFieldDefinition> fields) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.fields = fields != null ? fields : new ArrayList<>();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<FormFieldDefinition> getFields() {
        return fields;
    }

    public void setFields(List<FormFieldDefinition> fields) {
        this.fields = fields;
    }

    public static class FormFieldDefinition {
        private String id;
        private String name;
        private String label;
        private String type;
        private boolean required;
        private List<FormFieldOption> options = new ArrayList<>();

        public FormFieldDefinition() {
        }

        public FormFieldDefinition(String id, String name, String label, String type, boolean required, List<FormFieldOption> options) {
            this.id = id;
            this.name = name;
            this.label = label;
            this.type = type;
            this.required = required;
            this.options = options != null ? options : new ArrayList<>();
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public boolean isRequired() {
            return required;
        }

        public void setRequired(boolean required) {
            this.required = required;
        }

        public List<FormFieldOption> getOptions() {
            return options;
        }

        public void setOptions(List<FormFieldOption> options) {
            this.options = options;
        }
    }

    public static class FormFieldOption {
        private String label;
        private String value;

        public FormFieldOption() {
        }

        public FormFieldOption(String label, String value) {
            this.label = label;
            this.value = value;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }
}
