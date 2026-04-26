package com.banco.workflow.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BpmnNode {
    
    private String id;
    private String name;
    private String type;  // StartEvent, EndEvent, UserTask, ExclusiveGateway, ParallelGateway
    
    private String assignedRole;  // Para UserTask
    private String formId;  // Para UserTask
    
    private Map<String, String> properties;  // Propiedades adicionales
    
    private List<BpmnTransition> outgoingTransitions;
    private List<BpmnTransition> incomingTransitions;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BpmnTransition {
        private String id;
        private String fromNodeId;
        private String toNodeId;
        private String conditionExpression;  // SpEL: "${variables.aprobado == true}"
        private boolean defaultFlow;
    }
}
