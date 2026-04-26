package com.banco.workflow.service;

import com.banco.workflow.model.BpmnNode;
import com.banco.workflow.model.Policy;
import com.banco.workflow.model.ProcessInstance;
import com.banco.workflow.model.Task;
import com.banco.workflow.model.User;
import com.banco.workflow.model.WorkflowDefinition;
import com.banco.workflow.repository.ProcessInstanceRepository;
import com.banco.workflow.repository.TaskRepository;
import org.springframework.context.expression.MapAccessor;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class WorkflowRuntimeService {

    private static final String JOIN_PREFIX = "__join__";

    private final ProcessInstanceRepository processInstanceRepository;
    private final TaskRepository taskRepository;
    private final TaskAssignmentService taskAssignmentService;
    private final WorkflowHistoryService workflowHistoryService;
    private final ExpressionParser expressionParser = new SpelExpressionParser();

    public WorkflowRuntimeService(
            ProcessInstanceRepository processInstanceRepository,
            TaskRepository taskRepository,
            TaskAssignmentService taskAssignmentService,
            WorkflowHistoryService workflowHistoryService
    ) {
        this.processInstanceRepository = processInstanceRepository;
        this.taskRepository = taskRepository;
        this.taskAssignmentService = taskAssignmentService;
        this.workflowHistoryService = workflowHistoryService;
    }

    public ProcessInstance startProcess(Policy policy, WorkflowDefinition definition, Map<String, Object> inputVariables, User initiator) {
        Map<String, Object> variables = inputVariables != null ? new java.util.HashMap<>(inputVariables) : new java.util.HashMap<>();
        variables.putIfAbsent("initiatedByUserId", initiator.getId());
        variables.putIfAbsent("initiatedByUsername", initiator.getUsername());

        ProcessInstance instance = new ProcessInstance();
        instance.setId(UUID.randomUUID().toString());
        instance.setPolicyId(policy.getId());
        instance.setPolicyName(policy.getName());
        instance.setPolicyVersion(policy.getVersion());
        instance.setTemporalProcessInstanceId("engine-" + UUID.randomUUID());
        instance.setStatus("RUNNING");
        instance.setVariables(variables);
        instance.setActiveNodeIds(new ArrayList<>());
        instance.setInitiatedByUserId(initiator.getId());
        instance.setInitiatedAt(LocalDateTime.now());
        instance.setHistory(new ArrayList<>());

        processInstanceRepository.save(instance);
        workflowHistoryService.record(instance, null, "PROCESS_STARTED", "SYSTEM", initiator.getId(), variables, null);
        advanceInstance(instance, definition, findStartNode(definition));
        return processInstanceRepository.save(instance);
    }

    public ProcessInstance completeTask(WorkflowDefinition definition, ProcessInstance instance, Task task, Map<String, Object> payload, User completedBy) {
        Map<String, Object> variables = instance.getVariables();
        if (variables == null) {
            variables = new java.util.HashMap<>();
            instance.setVariables(variables);
        }
        if (payload != null) {
            variables.putAll(payload);
            variables.put(task.getNodeId() + "Result", payload);
        }

        task.setStatus("COMPLETED");
        task.setCompletedAt(LocalDateTime.now());
        task.setResult(payload);
        taskRepository.save(task);

        instance.getActiveNodeIds().remove(task.getNodeId());
        workflowHistoryService.record(instance, task.getNodeId(), "TASK_COMPLETED", task.getNodeType(), completedBy.getId(), payload, null);

        BpmnNode node = definition.getGraph().get(task.getNodeId());
        if (node != null && node.getOutgoingTransitions() != null) {
            for (BpmnNode.BpmnTransition transition : node.getOutgoingTransitions()) {
                advanceInstance(instance, definition, transition.getToNodeId());
            }
        }

        if (instance.getActiveNodeIds().isEmpty() && "RUNNING".equals(instance.getStatus())) {
            instance.setStatus("COMPLETED");
            instance.setCompletedAt(LocalDateTime.now());
            workflowHistoryService.record(instance, null, "PROCESS_COMPLETED", "EndEvent", completedBy.getId(), payload, null);
        }

        return processInstanceRepository.save(instance);
    }

    public List<Task> getOpenTasksForUser(User user) {
        return taskAssignmentService.getOpenTasksForUser(user);
    }

    private void advanceInstance(ProcessInstance instance, WorkflowDefinition definition, String nodeId) {
        if (nodeId == null || definition.getGraph() == null) {
            return;
        }

        BpmnNode node = definition.getGraph().get(nodeId);
        if (node == null) {
            workflowHistoryService.record(instance, nodeId, "NODE_MISSING", "Unknown", "SYSTEM", null, "Nodo no encontrado en el grafo");
            return;
        }

        switch (node.getType()) {
            case "StartEvent" -> followFirstTransition(instance, definition, node);
            case "EndEvent" -> {
                instance.setStatus("COMPLETED");
                instance.setCompletedAt(LocalDateTime.now());
                workflowHistoryService.record(instance, nodeId, "END_REACHED", node.getType(), "SYSTEM", null, null);
            }
            case "UserTask" -> createTask(instance, node);
            case "ExclusiveGateway" -> {
                String nextNodeId = evaluateExclusiveGateway(instance.getVariables(), node.getOutgoingTransitions());
                workflowHistoryService.record(instance, nodeId, "GATEWAY_EVALUATED", node.getType(), "SYSTEM", instance.getVariables(), null);
                advanceInstance(instance, definition, nextNodeId);
            }
            case "ParallelGateway" -> {
                if (isJoin(node)) {
                    int expected = size(node.getIncomingTransitions());
                    int arrived = incrementJoinCounter(instance, node.getId());
                    if (arrived >= expected) {
                        instance.getVariables().remove(JOIN_PREFIX + node.getId());
                        followFirstTransition(instance, definition, node);
                    }
                } else {
                    workflowHistoryService.record(instance, nodeId, "PARALLEL_SPLIT", node.getType(), "SYSTEM", instance.getVariables(), null);
                    for (BpmnNode.BpmnTransition transition : safeTransitions(node.getOutgoingTransitions())) {
                        advanceInstance(instance, definition, transition.getToNodeId());
                    }
                }
            }
            default -> followFirstTransition(instance, definition, node);
        }
    }

    private void followFirstTransition(ProcessInstance instance, WorkflowDefinition definition, BpmnNode node) {
        List<BpmnNode.BpmnTransition> transitions = safeTransitions(node.getOutgoingTransitions());
        if (!transitions.isEmpty()) {
            advanceInstance(instance, definition, transitions.get(0).getToNodeId());
        }
    }

    private void createTask(ProcessInstance instance, BpmnNode node) {
        if (instance.getActiveNodeIds().contains(node.getId())) {
            return;
        }

        Task task = taskAssignmentService.createHumanTask(instance, node);
        instance.getActiveNodeIds().add(node.getId());
        workflowHistoryService.record(instance, node.getId(), "TASK_CREATED", node.getType(), "SYSTEM", instance.getVariables(), null);
    }

    private String evaluateExclusiveGateway(Map<String, Object> variables, List<BpmnNode.BpmnTransition> transitions) {
        for (BpmnNode.BpmnTransition transition : safeTransitions(transitions)) {
            if (transition.isDefaultFlow()) {
                continue;
            }

            if (evaluateCondition(transition.getConditionExpression(), variables)) {
                return transition.getToNodeId();
            }
        }

        return safeTransitions(transitions).stream()
                .filter(BpmnNode.BpmnTransition::isDefaultFlow)
                .map(BpmnNode.BpmnTransition::getToNodeId)
                .findFirst()
                .orElseGet(() -> safeTransitions(transitions).stream()
                        .findFirst()
                        .map(BpmnNode.BpmnTransition::getToNodeId)
                        .orElse(null));
    }

    private boolean evaluateCondition(String rawExpression, Map<String, Object> variables) {
        if (rawExpression == null || rawExpression.isBlank()) {
            return false;
        }

        String expression = rawExpression.trim();
        if (expression.startsWith("${") && expression.endsWith("}")) {
            expression = expression.substring(2, expression.length() - 1);
        }

        StandardEvaluationContext context = new StandardEvaluationContext();
        context.addPropertyAccessor(new MapAccessor());
        context.setVariable("variables", variables);
        context.setRootObject(variables);
        Boolean value = expressionParser.parseExpression(expression).getValue(context, Boolean.class);
        return Boolean.TRUE.equals(value);
    }

    private int incrementJoinCounter(ProcessInstance instance, String joinNodeId) {
        Object current = instance.getVariables().get(JOIN_PREFIX + joinNodeId);
        int count = current instanceof Number number ? number.intValue() : 0;
        count++;
        instance.getVariables().put(JOIN_PREFIX + joinNodeId, count);
        return count;
    }

    private boolean isJoin(BpmnNode node) {
        return size(node.getIncomingTransitions()) > 1;
    }

    private int size(Collection<?> collection) {
        return collection == null ? 0 : collection.size();
    }

    private List<BpmnNode.BpmnTransition> safeTransitions(List<BpmnNode.BpmnTransition> transitions) {
        return transitions == null ? List.of() : transitions;
    }

    private String findStartNode(WorkflowDefinition definition) {
        return definition.getGraph().values().stream()
                .filter(node -> "StartEvent".equals(node.getType()))
                .map(BpmnNode::getId)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("La política no tiene nodo inicial"));
    }
}
