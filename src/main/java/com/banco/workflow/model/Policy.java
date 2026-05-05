package com.banco.workflow.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Document(collection = "policies")
public class Policy {

    @Id
    private String id;
    private String name;
    private String description;
    private int version;
    private String bpmnXml;
    private Map<String, Object> umlActivityJson;
    private String umlVersion;
    private String diagramNotation;
    private String status;
    private Map<String, BpmnNode> graph;
    private List<DepartmentDefinition> departments;
    private List<FormDefinition> forms;
    private String createdByUserId;
    private String ownerUserId;
    private String tenantEmpresa;
    private boolean collaborationEnabled;
    private String collaborationMode;
    private String lastEditedByUserId;
    private LocalDateTime lastAutoSavedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean active;

    public Policy() {
    }

    public Policy(String id, String name, String description, int version, String bpmnXml,
                  Map<String, Object> umlActivityJson, String umlVersion, String diagramNotation, String status,
                  Map<String, BpmnNode> graph, List<DepartmentDefinition> departments, List<FormDefinition> forms,
                  String createdByUserId, String ownerUserId, String tenantEmpresa, boolean collaborationEnabled,
                  String collaborationMode, String lastEditedByUserId, LocalDateTime lastAutoSavedAt, LocalDateTime createdAt,
                  LocalDateTime updatedAt, boolean active) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.version = version;
        this.bpmnXml = bpmnXml;
        this.umlActivityJson = umlActivityJson;
        this.umlVersion = umlVersion;
        this.diagramNotation = diagramNotation;
        this.status = status;
        this.graph = graph;
        this.departments = departments;
        this.forms = forms;
        this.createdByUserId = createdByUserId;
        this.ownerUserId = ownerUserId;
        this.tenantEmpresa = tenantEmpresa;
        this.collaborationEnabled = collaborationEnabled;
        this.collaborationMode = collaborationMode;
        this.lastEditedByUserId = lastEditedByUserId;
        this.lastAutoSavedAt = lastAutoSavedAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public String getBpmnXml() {
        return bpmnXml;
    }

    public void setBpmnXml(String bpmnXml) {
        this.bpmnXml = bpmnXml;
    }

    public Map<String, Object> getUmlActivityJson() {
        return umlActivityJson;
    }

    public void setUmlActivityJson(Map<String, Object> umlActivityJson) {
        this.umlActivityJson = umlActivityJson;
    }

    public String getUmlVersion() {
        return umlVersion;
    }

    public void setUmlVersion(String umlVersion) {
        this.umlVersion = umlVersion;
    }

    public String getDiagramNotation() {
        return diagramNotation;
    }

    public void setDiagramNotation(String diagramNotation) {
        this.diagramNotation = diagramNotation;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Map<String, BpmnNode> getGraph() {
        return graph;
    }

    public void setGraph(Map<String, BpmnNode> graph) {
        this.graph = graph;
    }

    public List<DepartmentDefinition> getDepartments() {
        return departments;
    }

    public void setDepartments(List<DepartmentDefinition> departments) {
        this.departments = departments;
    }

    public List<FormDefinition> getForms() {
        return forms;
    }

    public void setForms(List<FormDefinition> forms) {
        this.forms = forms;
    }

    public String getCreatedByUserId() {
        return createdByUserId;
    }

    public void setCreatedByUserId(String createdByUserId) {
        this.createdByUserId = createdByUserId;
    }

    public String getOwnerUserId() {
        return ownerUserId;
    }

    public void setOwnerUserId(String ownerUserId) {
        this.ownerUserId = ownerUserId;
    }

    public String getTenantEmpresa() {
        return tenantEmpresa;
    }

    public void setTenantEmpresa(String tenantEmpresa) {
        this.tenantEmpresa = tenantEmpresa;
    }

    public boolean isCollaborationEnabled() {
        return collaborationEnabled;
    }

    public void setCollaborationEnabled(boolean collaborationEnabled) {
        this.collaborationEnabled = collaborationEnabled;
    }

    public String getCollaborationMode() {
        return collaborationMode;
    }

    public void setCollaborationMode(String collaborationMode) {
        this.collaborationMode = collaborationMode;
    }

    public String getLastEditedByUserId() {
        return lastEditedByUserId;
    }

    public void setLastEditedByUserId(String lastEditedByUserId) {
        this.lastEditedByUserId = lastEditedByUserId;
    }

    public LocalDateTime getLastAutoSavedAt() {
        return lastAutoSavedAt;
    }

    public void setLastAutoSavedAt(LocalDateTime lastAutoSavedAt) {
        this.lastAutoSavedAt = lastAutoSavedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
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
        private String description;
        private int version;
        private String bpmnXml;
        private Map<String, Object> umlActivityJson;
        private String umlVersion;
        private String diagramNotation;
        private String status;
        private Map<String, BpmnNode> graph;
        private List<DepartmentDefinition> departments;
        private List<FormDefinition> forms;
        private String createdByUserId;
        private String ownerUserId;
        private String tenantEmpresa;
        private boolean collaborationEnabled;
        private String collaborationMode;
        private String lastEditedByUserId;
        private LocalDateTime lastAutoSavedAt;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private boolean active;

        public Builder id(String id) { this.id = id; return this; }
        public Builder name(String name) { this.name = name; return this; }
        public Builder description(String description) { this.description = description; return this; }
        public Builder version(int version) { this.version = version; return this; }
        public Builder bpmnXml(String bpmnXml) { this.bpmnXml = bpmnXml; return this; }
        public Builder umlActivityJson(Map<String, Object> umlActivityJson) { this.umlActivityJson = umlActivityJson; return this; }
        public Builder umlVersion(String umlVersion) { this.umlVersion = umlVersion; return this; }
        public Builder diagramNotation(String diagramNotation) { this.diagramNotation = diagramNotation; return this; }
        public Builder status(String status) { this.status = status; return this; }
        public Builder graph(Map<String, BpmnNode> graph) { this.graph = graph; return this; }
        public Builder departments(List<DepartmentDefinition> departments) { this.departments = departments; return this; }
        public Builder forms(List<FormDefinition> forms) { this.forms = forms; return this; }
        public Builder createdByUserId(String createdByUserId) { this.createdByUserId = createdByUserId; return this; }
        public Builder ownerUserId(String ownerUserId) { this.ownerUserId = ownerUserId; return this; }
        public Builder tenantEmpresa(String tenantEmpresa) { this.tenantEmpresa = tenantEmpresa; return this; }
        public Builder collaborationEnabled(boolean collaborationEnabled) { this.collaborationEnabled = collaborationEnabled; return this; }
        public Builder collaborationMode(String collaborationMode) { this.collaborationMode = collaborationMode; return this; }
        public Builder lastEditedByUserId(String lastEditedByUserId) { this.lastEditedByUserId = lastEditedByUserId; return this; }
        public Builder lastAutoSavedAt(LocalDateTime lastAutoSavedAt) { this.lastAutoSavedAt = lastAutoSavedAt; return this; }
        public Builder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public Builder updatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }
        public Builder active(boolean active) { this.active = active; return this; }

        public Policy build() {
            return new Policy(id, name, description, version, bpmnXml, umlActivityJson, umlVersion, diagramNotation, status, graph, departments, forms, createdByUserId,
                    ownerUserId, tenantEmpresa, collaborationEnabled, collaborationMode, lastEditedByUserId, lastAutoSavedAt,
                    createdAt, updatedAt, active);
        }
    }
}
