package com.banco.workflow.model;

public class DepartmentDefinition {

    private String id;
    private String name;
    private String role;
    private String description;

    public DepartmentDefinition() {
    }

    public DepartmentDefinition(String id, String name, String role, String description) {
        this.id = id;
        this.name = name;
        this.role = role;
        this.description = description;
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
}
