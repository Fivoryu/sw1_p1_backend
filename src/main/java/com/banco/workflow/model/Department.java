package com.banco.workflow.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "departments")
public class Department {

    @Id
    private String id;
    private String name;
    private String role;
    private String description;
    private boolean active;

    public Department() {
    }

    public Department(String id, String name, String role, String description, boolean active) {
        this.id = id;
        this.name = name;
        this.role = role;
        this.description = description;
        this.active = active;
    }

    public static Builder builder() {
        return new Builder();
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

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public static class Builder {
        private String id;
        private String name;
        private String role;
        private String description;
        private boolean active;

        public Builder id(String id) { this.id = id; return this; }
        public Builder name(String name) { this.name = name; return this; }
        public Builder role(String role) { this.role = role; return this; }
        public Builder description(String description) { this.description = description; return this; }
        public Builder active(boolean active) { this.active = active; return this; }

        public Department build() {
            return new Department(id, name, role, description, active);
        }
    }
}
