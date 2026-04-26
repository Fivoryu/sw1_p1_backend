package com.banco.workflow.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Document(collection = "workflow_definitions")
public class WorkflowDefinition {

    @Id
    private String id;
    private String policyId;
    private String policyName;
    private int policyVersion;
    private String validationStatus;
    private List<String> validationErrors = new ArrayList<>();
    private Map<String, BpmnNode> graph;
    private LocalDateTime compiledAt;
    private LocalDateTime publishedAt;

    public WorkflowDefinition() {
    }

    public WorkflowDefinition(String id, String policyId, String policyName, int policyVersion, String validationStatus,
                              List<String> validationErrors, Map<String, BpmnNode> graph, LocalDateTime compiledAt,
                              LocalDateTime publishedAt) {
        this.id = id;
        this.policyId = policyId;
        this.policyName = policyName;
        this.policyVersion = policyVersion;
        this.validationStatus = validationStatus;
        this.validationErrors = validationErrors != null ? validationErrors : new ArrayList<>();
        this.graph = graph;
        this.compiledAt = compiledAt;
        this.publishedAt = publishedAt;
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

    public String getPolicyId() {
        return policyId;
    }

    public void setPolicyId(String policyId) {
        this.policyId = policyId;
    }

    public String getPolicyName() {
        return policyName;
    }

    public void setPolicyName(String policyName) {
        this.policyName = policyName;
    }

    public int getPolicyVersion() {
        return policyVersion;
    }

    public void setPolicyVersion(int policyVersion) {
        this.policyVersion = policyVersion;
    }

    public String getValidationStatus() {
        return validationStatus;
    }

    public void setValidationStatus(String validationStatus) {
        this.validationStatus = validationStatus;
    }

    public List<String> getValidationErrors() {
        return validationErrors;
    }

    public void setValidationErrors(List<String> validationErrors) {
        this.validationErrors = validationErrors;
    }

    public Map<String, BpmnNode> getGraph() {
        return graph;
    }

    public void setGraph(Map<String, BpmnNode> graph) {
        this.graph = graph;
    }

    public LocalDateTime getCompiledAt() {
        return compiledAt;
    }

    public void setCompiledAt(LocalDateTime compiledAt) {
        this.compiledAt = compiledAt;
    }

    public LocalDateTime getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(LocalDateTime publishedAt) {
        this.publishedAt = publishedAt;
    }

    public static class Builder {
        private String id;
        private String policyId;
        private String policyName;
        private int policyVersion;
        private String validationStatus;
        private List<String> validationErrors = new ArrayList<>();
        private Map<String, BpmnNode> graph;
        private LocalDateTime compiledAt;
        private LocalDateTime publishedAt;

        public Builder id(String id) { this.id = id; return this; }
        public Builder policyId(String policyId) { this.policyId = policyId; return this; }
        public Builder policyName(String policyName) { this.policyName = policyName; return this; }
        public Builder policyVersion(int policyVersion) { this.policyVersion = policyVersion; return this; }
        public Builder validationStatus(String validationStatus) { this.validationStatus = validationStatus; return this; }
        public Builder validationErrors(List<String> validationErrors) { this.validationErrors = validationErrors; return this; }
        public Builder graph(Map<String, BpmnNode> graph) { this.graph = graph; return this; }
        public Builder compiledAt(LocalDateTime compiledAt) { this.compiledAt = compiledAt; return this; }
        public Builder publishedAt(LocalDateTime publishedAt) { this.publishedAt = publishedAt; return this; }

        public WorkflowDefinition build() {
            return new WorkflowDefinition(id, policyId, policyName, policyVersion, validationStatus, validationErrors,
                    graph, compiledAt, publishedAt);
        }
    }
}
