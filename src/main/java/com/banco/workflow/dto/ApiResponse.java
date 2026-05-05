package com.banco.workflow.dto;

import com.banco.workflow.model.DepartmentDefinition;
import com.banco.workflow.model.FormDefinition;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

public class ApiResponse {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PolicyRequest {
        private String name;
        private String description;
        private String bpmnXml;
        private Map<String, Object> umlActivityJson;
        private String umlVersion;
        private String diagramNotation;
        private java.util.List<DepartmentDefinition> departments;
        private java.util.List<FormDefinition> forms;
        private Boolean collaborationEnabled;
        private String collaborationMode;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PolicyResponse {
        private String id;
        private String name;
        private int version;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ProcessStartRequest {
        private String policyId;
        private Map<String, Object> variables;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ProcessStartResponse {
        private String id;
        private String temporalProcessInstanceId;
        private String status;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TaskResponse {
        private String id;
        private String processInstanceId;
        private String taskId;
        private String taskName;
        private String assignedRole;
        private String formId;
        private Map<String, Object> contextData;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TaskCompleteRequest {
        private Map<String, Object> variables;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ErrorResponse {
        private int status;
        private String message;
        private LocalDateTime timestamp;
    }
}
