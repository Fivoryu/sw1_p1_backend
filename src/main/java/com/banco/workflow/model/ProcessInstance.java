package com.banco.workflow.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Document(collection = "process_instances")
public class ProcessInstance {

    @Id
    private String id;
    private String policyId;
    private String policyName;
    private int policyVersion;
    private String tenantEmpresa;
    private String temporalProcessInstanceId;
    private String status;
    private Map<String, Object> variables;
    private List<String> activeNodeIds = new ArrayList<>();
    private String initiatedByUserId;
    private LocalDateTime initiatedAt;
    private LocalDateTime completedAt;
    private String result;
    private List<HistoryEntry> history;
    private List<CompletedForm> completedForms = new ArrayList<>();

    public ProcessInstance() {
    }

    public ProcessInstance(String id, String policyId, String policyName, int policyVersion,
                           String tenantEmpresa, String temporalProcessInstanceId, String status,
                           Map<String, Object> variables, List<String> activeNodeIds, String initiatedByUserId,
                           LocalDateTime initiatedAt, LocalDateTime completedAt, String result,
                           List<HistoryEntry> history, List<CompletedForm> completedForms) {
        this.id = id;
        this.policyId = policyId;
        this.policyName = policyName;
        this.policyVersion = policyVersion;
        this.tenantEmpresa = tenantEmpresa;
        this.temporalProcessInstanceId = temporalProcessInstanceId;
        this.status = status;
        this.variables = variables;
        this.activeNodeIds = activeNodeIds != null ? activeNodeIds : new ArrayList<>();
        this.initiatedByUserId = initiatedByUserId;
        this.initiatedAt = initiatedAt;
        this.completedAt = completedAt;
        this.result = result;
        this.history = history;
        this.completedForms = completedForms != null ? completedForms : new ArrayList<>();
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getPolicyId() { return policyId; }
    public void setPolicyId(String policyId) { this.policyId = policyId; }
    public String getPolicyName() { return policyName; }
    public void setPolicyName(String policyName) { this.policyName = policyName; }
    public int getPolicyVersion() { return policyVersion; }
    public void setPolicyVersion(int policyVersion) { this.policyVersion = policyVersion; }
    public String getTenantEmpresa() { return tenantEmpresa; }
    public void setTenantEmpresa(String tenantEmpresa) { this.tenantEmpresa = tenantEmpresa; }
    public String getTemporalProcessInstanceId() { return temporalProcessInstanceId; }
    public void setTemporalProcessInstanceId(String temporalProcessInstanceId) { this.temporalProcessInstanceId = temporalProcessInstanceId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Map<String, Object> getVariables() { return variables; }
    public void setVariables(Map<String, Object> variables) { this.variables = variables; }
    public List<String> getActiveNodeIds() { return activeNodeIds; }
    public void setActiveNodeIds(List<String> activeNodeIds) { this.activeNodeIds = activeNodeIds; }
    public String getInitiatedByUserId() { return initiatedByUserId; }
    public void setInitiatedByUserId(String initiatedByUserId) { this.initiatedByUserId = initiatedByUserId; }
    public LocalDateTime getInitiatedAt() { return initiatedAt; }
    public void setInitiatedAt(LocalDateTime initiatedAt) { this.initiatedAt = initiatedAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
    public String getResult() { return result; }
    public void setResult(String result) { this.result = result; }
    public List<HistoryEntry> getHistory() { return history; }
    public void setHistory(List<HistoryEntry> history) { this.history = history; }
    public List<CompletedForm> getCompletedForms() { return completedForms; }
    public void setCompletedForms(List<CompletedForm> completedForms) { this.completedForms = completedForms; }

    public static class Builder {
        private String id;
        private String policyId;
        private String policyName;
        private int policyVersion;
        private String tenantEmpresa;
        private String temporalProcessInstanceId;
        private String status;
        private Map<String, Object> variables;
        private List<String> activeNodeIds = new ArrayList<>();
        private String initiatedByUserId;
        private LocalDateTime initiatedAt;
        private LocalDateTime completedAt;
        private String result;
        private List<HistoryEntry> history;
        private List<CompletedForm> completedForms = new ArrayList<>();

        public Builder id(String id) { this.id = id; return this; }
        public Builder policyId(String policyId) { this.policyId = policyId; return this; }
        public Builder policyName(String policyName) { this.policyName = policyName; return this; }
        public Builder policyVersion(int policyVersion) { this.policyVersion = policyVersion; return this; }
        public Builder tenantEmpresa(String tenantEmpresa) { this.tenantEmpresa = tenantEmpresa; return this; }
        public Builder temporalProcessInstanceId(String temporalProcessInstanceId) { this.temporalProcessInstanceId = temporalProcessInstanceId; return this; }
        public Builder status(String status) { this.status = status; return this; }
        public Builder variables(Map<String, Object> variables) { this.variables = variables; return this; }
        public Builder activeNodeIds(List<String> activeNodeIds) { this.activeNodeIds = activeNodeIds; return this; }
        public Builder initiatedByUserId(String initiatedByUserId) { this.initiatedByUserId = initiatedByUserId; return this; }
        public Builder initiatedAt(LocalDateTime initiatedAt) { this.initiatedAt = initiatedAt; return this; }
        public Builder completedAt(LocalDateTime completedAt) { this.completedAt = completedAt; return this; }
        public Builder result(String result) { this.result = result; return this; }
        public Builder history(List<HistoryEntry> history) { this.history = history; return this; }
        public Builder completedForms(List<CompletedForm> completedForms) { this.completedForms = completedForms; return this; }

        public ProcessInstance build() {
            return new ProcessInstance(id, policyId, policyName, policyVersion, tenantEmpresa,
                    temporalProcessInstanceId, status, variables, activeNodeIds, initiatedByUserId,
                    initiatedAt, completedAt, result, history, completedForms);
        }
    }

    public static class CompletedForm {
        private String formId;
        private String taskId;
        private String nodeId;
        private String completedByUserId;
        private LocalDateTime completedAt;
        private Map<String, Object> values;

        public CompletedForm() {
        }

        public CompletedForm(String formId, String taskId, String nodeId, String completedByUserId,
                             LocalDateTime completedAt, Map<String, Object> values) {
            this.formId = formId;
            this.taskId = taskId;
            this.nodeId = nodeId;
            this.completedByUserId = completedByUserId;
            this.completedAt = completedAt;
            this.values = values;
        }

        public String getFormId() { return formId; }
        public void setFormId(String formId) { this.formId = formId; }
        public String getTaskId() { return taskId; }
        public void setTaskId(String taskId) { this.taskId = taskId; }
        public String getNodeId() { return nodeId; }
        public void setNodeId(String nodeId) { this.nodeId = nodeId; }
        public String getCompletedByUserId() { return completedByUserId; }
        public void setCompletedByUserId(String completedByUserId) { this.completedByUserId = completedByUserId; }
        public LocalDateTime getCompletedAt() { return completedAt; }
        public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
        public Map<String, Object> getValues() { return values; }
        public void setValues(Map<String, Object> values) { this.values = values; }
    }

    public static class HistoryEntry {
        private String id;
        private String processInstanceId;
        private String nodeId;
        private String nodeType;
        private String nodeName;
        private String taskId;
        private String taskName;
        private String assignedRole;
        private String completedByUserId;
        private LocalDateTime timestamp;
        private String status;
        private Map<String, Object> taskData;
        private Map<String, Object> taskResult;
        private String errorMessage;

        public HistoryEntry() {
        }

        public HistoryEntry(String id, String processInstanceId, String nodeId, String nodeType, String nodeName,
                            String taskId, String taskName, String assignedRole, String completedByUserId,
                            LocalDateTime timestamp, String status, Map<String, Object> taskData,
                            Map<String, Object> taskResult, String errorMessage) {
            this.id = id;
            this.processInstanceId = processInstanceId;
            this.nodeId = nodeId;
            this.nodeType = nodeType;
            this.nodeName = nodeName;
            this.taskId = taskId;
            this.taskName = taskName;
            this.assignedRole = assignedRole;
            this.completedByUserId = completedByUserId;
            this.timestamp = timestamp;
            this.status = status;
            this.taskData = taskData;
            this.taskResult = taskResult;
            this.errorMessage = errorMessage;
        }

        public static HistoryEntryBuilder builder() {
            return new HistoryEntryBuilder();
        }

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getProcessInstanceId() { return processInstanceId; }
        public void setProcessInstanceId(String processInstanceId) { this.processInstanceId = processInstanceId; }
        public String getNodeId() { return nodeId; }
        public void setNodeId(String nodeId) { this.nodeId = nodeId; }
        public String getNodeType() { return nodeType; }
        public void setNodeType(String nodeType) { this.nodeType = nodeType; }
        public String getNodeName() { return nodeName; }
        public void setNodeName(String nodeName) { this.nodeName = nodeName; }
        public String getTaskId() { return taskId; }
        public void setTaskId(String taskId) { this.taskId = taskId; }
        public String getTaskName() { return taskName; }
        public void setTaskName(String taskName) { this.taskName = taskName; }
        public String getAssignedRole() { return assignedRole; }
        public void setAssignedRole(String assignedRole) { this.assignedRole = assignedRole; }
        public String getCompletedByUserId() { return completedByUserId; }
        public void setCompletedByUserId(String completedByUserId) { this.completedByUserId = completedByUserId; }
        public LocalDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public Map<String, Object> getTaskData() { return taskData; }
        public void setTaskData(Map<String, Object> taskData) { this.taskData = taskData; }
        public Map<String, Object> getTaskResult() { return taskResult; }
        public void setTaskResult(Map<String, Object> taskResult) { this.taskResult = taskResult; }
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

        public static class HistoryEntryBuilder {
            private String id;
            private String processInstanceId;
            private String nodeId;
            private String nodeType;
            private String nodeName;
            private String taskId;
            private String taskName;
            private String assignedRole;
            private String completedByUserId;
            private LocalDateTime timestamp;
            private String status;
            private Map<String, Object> taskData;
            private Map<String, Object> taskResult;
            private String errorMessage;

            public HistoryEntryBuilder id(String id) { this.id = id; return this; }
            public HistoryEntryBuilder processInstanceId(String processInstanceId) { this.processInstanceId = processInstanceId; return this; }
            public HistoryEntryBuilder nodeId(String nodeId) { this.nodeId = nodeId; return this; }
            public HistoryEntryBuilder nodeType(String nodeType) { this.nodeType = nodeType; return this; }
            public HistoryEntryBuilder nodeName(String nodeName) { this.nodeName = nodeName; return this; }
            public HistoryEntryBuilder taskId(String taskId) { this.taskId = taskId; return this; }
            public HistoryEntryBuilder taskName(String taskName) { this.taskName = taskName; return this; }
            public HistoryEntryBuilder assignedRole(String assignedRole) { this.assignedRole = assignedRole; return this; }
            public HistoryEntryBuilder completedByUserId(String completedByUserId) { this.completedByUserId = completedByUserId; return this; }
            public HistoryEntryBuilder timestamp(LocalDateTime timestamp) { this.timestamp = timestamp; return this; }
            public HistoryEntryBuilder status(String status) { this.status = status; return this; }
            public HistoryEntryBuilder taskData(Map<String, Object> taskData) { this.taskData = taskData; return this; }
            public HistoryEntryBuilder taskResult(Map<String, Object> taskResult) { this.taskResult = taskResult; return this; }
            public HistoryEntryBuilder errorMessage(String errorMessage) { this.errorMessage = errorMessage; return this; }

            public HistoryEntry build() {
                return new HistoryEntry(id, processInstanceId, nodeId, nodeType, nodeName, taskId, taskName,
                        assignedRole, completedByUserId, timestamp, status, taskData, taskResult, errorMessage);
            }
        }
    }
}
