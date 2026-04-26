package com.banco.workflow.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Document(collection = "tasks")
public class Task {

    @Id
    private String id;
    private String processInstanceId;
    private String nodeId;
    private String nodeName;
    private String nodeType;
    private String assignee;
    private String candidateRole;
    private String departmentAssigned;
    private String formId;
    private String status;
    private String title;
    private String description;
    private String priority;
    private LocalDateTime createdAt;
    private LocalDateTime dueDate;
    private LocalDateTime completedAt;
    private Object result;
    private String taskType;
    private Object formData;
    private Object variables;
    private List<String> requiredDocuments;
    private String nextTaskId;
    private String rejectionReason;
    private String customerName;
    private String customerDni;

    public Task() {
    }

    public Task(String id, String processInstanceId, String nodeId, String nodeName, String nodeType, String assignee,
                String candidateRole, String departmentAssigned, String formId, String status, String title, String description, String priority,
                LocalDateTime createdAt, LocalDateTime dueDate, LocalDateTime completedAt, Object result,
                String taskType, Object formData, Object variables, List<String> requiredDocuments,
                String nextTaskId, String rejectionReason, String customerName, String customerDni) {
        this.id = id;
        this.processInstanceId = processInstanceId;
        this.nodeId = nodeId;
        this.nodeName = nodeName;
        this.nodeType = nodeType;
        this.assignee = assignee;
        this.candidateRole = candidateRole;
        this.departmentAssigned = departmentAssigned;
        this.formId = formId;
        this.status = status;
        this.title = title;
        this.description = description;
        this.priority = priority;
        this.createdAt = createdAt;
        this.dueDate = dueDate;
        this.completedAt = completedAt;
        this.result = result;
        this.taskType = taskType;
        this.formData = formData;
        this.variables = variables;
        this.requiredDocuments = requiredDocuments;
        this.nextTaskId = nextTaskId;
        this.rejectionReason = rejectionReason;
        this.customerName = customerName;
        this.customerDni = customerDni;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getProcessInstanceId() { return processInstanceId; }
    public void setProcessInstanceId(String processInstanceId) { this.processInstanceId = processInstanceId; }
    public String getNodeId() { return nodeId; }
    public void setNodeId(String nodeId) { this.nodeId = nodeId; }
    public String getNodeName() { return nodeName; }
    public void setNodeName(String nodeName) { this.nodeName = nodeName; }
    public String getNodeType() { return nodeType; }
    public void setNodeType(String nodeType) { this.nodeType = nodeType; }
    public String getAssignee() { return assignee; }
    public void setAssignee(String assignee) { this.assignee = assignee; }
    public String getCandidateRole() { return candidateRole; }
    public void setCandidateRole(String candidateRole) { this.candidateRole = candidateRole; }
    public String getDepartmentAssigned() { return departmentAssigned; }
    public void setDepartmentAssigned(String departmentAssigned) { this.departmentAssigned = departmentAssigned; }
    public String getFormId() { return formId; }
    public void setFormId(String formId) { this.formId = formId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getDueDate() { return dueDate; }
    public void setDueDate(LocalDateTime dueDate) { this.dueDate = dueDate; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
    public Object getResult() { return result; }
    public void setResult(Object result) { this.result = result; }
    public String getTaskType() { return taskType; }
    public void setTaskType(String taskType) { this.taskType = taskType; }
    public Object getFormData() { return formData; }
    public void setFormData(Object formData) { this.formData = formData; }
    public Object getVariables() { return variables; }
    public void setVariables(Object variables) { this.variables = variables; }
    public List<String> getRequiredDocuments() { return requiredDocuments; }
    public void setRequiredDocuments(List<String> requiredDocuments) { this.requiredDocuments = requiredDocuments; }
    public String getNextTaskId() { return nextTaskId; }
    public void setNextTaskId(String nextTaskId) { this.nextTaskId = nextTaskId; }
    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public String getCustomerDni() { return customerDni; }
    public void setCustomerDni(String customerDni) { this.customerDni = customerDni; }

    public static class Builder {
        private String id;
        private String processInstanceId;
        private String nodeId;
        private String nodeName;
        private String nodeType;
        private String assignee;
        private String candidateRole;
        private String departmentAssigned;
        private String formId;
        private String status;
        private String title;
        private String description;
        private String priority;
        private LocalDateTime createdAt;
        private LocalDateTime dueDate;
        private LocalDateTime completedAt;
        private Object result;
        private String taskType;
        private Object formData;
        private Object variables;
        private List<String> requiredDocuments;
        private String nextTaskId;
        private String rejectionReason;
        private String customerName;
        private String customerDni;

        public Builder id(String id) { this.id = id; return this; }
        public Builder processInstanceId(String processInstanceId) { this.processInstanceId = processInstanceId; return this; }
        public Builder nodeId(String nodeId) { this.nodeId = nodeId; return this; }
        public Builder nodeName(String nodeName) { this.nodeName = nodeName; return this; }
        public Builder nodeType(String nodeType) { this.nodeType = nodeType; return this; }
        public Builder assignee(String assignee) { this.assignee = assignee; return this; }
        public Builder candidateRole(String candidateRole) { this.candidateRole = candidateRole; return this; }
        public Builder departmentAssigned(String departmentAssigned) { this.departmentAssigned = departmentAssigned; return this; }
        public Builder formId(String formId) { this.formId = formId; return this; }
        public Builder status(String status) { this.status = status; return this; }
        public Builder title(String title) { this.title = title; return this; }
        public Builder description(String description) { this.description = description; return this; }
        public Builder priority(String priority) { this.priority = priority; return this; }
        public Builder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public Builder dueDate(LocalDateTime dueDate) { this.dueDate = dueDate; return this; }
        public Builder completedAt(LocalDateTime completedAt) { this.completedAt = completedAt; return this; }
        public Builder result(Object result) { this.result = result; return this; }
        public Builder taskType(String taskType) { this.taskType = taskType; return this; }
        public Builder formData(Object formData) { this.formData = formData; return this; }
        public Builder variables(Object variables) { this.variables = variables; return this; }
        public Builder requiredDocuments(List<String> requiredDocuments) { this.requiredDocuments = requiredDocuments; return this; }
        public Builder nextTaskId(String nextTaskId) { this.nextTaskId = nextTaskId; return this; }
        public Builder rejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; return this; }
        public Builder customerName(String customerName) { this.customerName = customerName; return this; }
        public Builder customerDni(String customerDni) { this.customerDni = customerDni; return this; }

        public Task build() {
            return new Task(id, processInstanceId, nodeId, nodeName, nodeType, assignee, candidateRole, departmentAssigned, formId, status, title,
                    description, priority, createdAt, dueDate, completedAt, result, taskType, formData, variables,
                    requiredDocuments, nextTaskId, rejectionReason, customerName, customerDni);
        }
    }
}
