package com.banco.workflow.config;

import com.banco.workflow.model.FormDefinition;

import java.util.ArrayList;
import java.util.List;

public class PolicyCollaborationMessage {

    private String type;
    private String policyId;
    private String actor;
    private String actorDisplayName;
    private String actorUserId;
    private String ownerUserId;
    private String ownerDisplayName;
    private String empresa;
    private String collaborationMode;
    private boolean canEdit;
    private boolean collaborationEnabled;
    private String policyName;
    private String name;
    private String description;
    private String bpmnXml;
    private List<FormDefinition> forms = new ArrayList<>();
    private List<TaskBindingPayload> taskBindings = new ArrayList<>();
    private long timestamp;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getPolicyId() {
        return policyId;
    }

    public void setPolicyId(String policyId) {
        this.policyId = policyId;
    }

    public String getActor() {
        return actor;
    }

    public void setActor(String actor) {
        this.actor = actor;
    }

    public String getActorDisplayName() {
        return actorDisplayName;
    }

    public void setActorDisplayName(String actorDisplayName) {
        this.actorDisplayName = actorDisplayName;
    }

    public String getActorUserId() {
        return actorUserId;
    }

    public void setActorUserId(String actorUserId) {
        this.actorUserId = actorUserId;
    }

    public String getOwnerUserId() {
        return ownerUserId;
    }

    public void setOwnerUserId(String ownerUserId) {
        this.ownerUserId = ownerUserId;
    }

    public String getOwnerDisplayName() {
        return ownerDisplayName;
    }

    public void setOwnerDisplayName(String ownerDisplayName) {
        this.ownerDisplayName = ownerDisplayName;
    }

    public String getEmpresa() {
        return empresa;
    }

    public void setEmpresa(String empresa) {
        this.empresa = empresa;
    }

    public String getCollaborationMode() {
        return collaborationMode;
    }

    public void setCollaborationMode(String collaborationMode) {
        this.collaborationMode = collaborationMode;
    }

    public boolean isCanEdit() {
        return canEdit;
    }

    public void setCanEdit(boolean canEdit) {
        this.canEdit = canEdit;
    }

    public boolean isCollaborationEnabled() {
        return collaborationEnabled;
    }

    public void setCollaborationEnabled(boolean collaborationEnabled) {
        this.collaborationEnabled = collaborationEnabled;
    }

    public String getPolicyName() {
        return policyName;
    }

    public void setPolicyName(String policyName) {
        this.policyName = policyName;
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

    public String getBpmnXml() {
        return bpmnXml;
    }

    public void setBpmnXml(String bpmnXml) {
        this.bpmnXml = bpmnXml;
    }

    public List<FormDefinition> getForms() {
        return forms;
    }

    public void setForms(List<FormDefinition> forms) {
        this.forms = forms != null ? forms : new ArrayList<>();
    }

    public List<TaskBindingPayload> getTaskBindings() {
        return taskBindings;
    }

    public void setTaskBindings(List<TaskBindingPayload> taskBindings) {
        this.taskBindings = taskBindings != null ? taskBindings : new ArrayList<>();
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public static class TaskBindingPayload {
        private String taskId;
        private String taskName;
        private String departmentRole;
        private String formId;

        public String getTaskId() {
            return taskId;
        }

        public void setTaskId(String taskId) {
            this.taskId = taskId;
        }

        public String getTaskName() {
            return taskName;
        }

        public void setTaskName(String taskName) {
            this.taskName = taskName;
        }

        public String getDepartmentRole() {
            return departmentRole;
        }

        public void setDepartmentRole(String departmentRole) {
            this.departmentRole = departmentRole;
        }

        public String getFormId() {
            return formId;
        }

        public void setFormId(String formId) {
            this.formId = formId;
        }
    }
}
