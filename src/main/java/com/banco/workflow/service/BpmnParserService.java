package com.banco.workflow.service;

import com.banco.workflow.model.BpmnNode;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class BpmnParserService {

    public Map<String, BpmnNode> parseProcessDefinition(String bpmnXml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(false);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new InputSource(new StringReader(bpmnXml)));

        Map<String, BpmnNode> nodes = new HashMap<>();
        Map<String, List<BpmnNode.BpmnTransition>> transitionMap = new HashMap<>();

        // Parse todos los nodos (tasks, gateways, events)
        parseNodes(doc, nodes);

        // Parse todas las transiciones
        parseTransitions(doc, transitionMap);

        // Asociar transiciones con nodos
        nodes.forEach((nodeId, node) -> {
            List<BpmnNode.BpmnTransition> outgoing = transitionMap.getOrDefault("out_" + nodeId, new ArrayList<>());
            List<BpmnNode.BpmnTransition> incoming = transitionMap.getOrDefault("in_" + nodeId, new ArrayList<>());
            node.setOutgoingTransitions(outgoing);
            node.setIncomingTransitions(incoming);
        });

        return nodes;
    }

    private void parseNodes(Document doc, Map<String, BpmnNode> nodes) {
        // StartEvent
        NodeList startEvents = doc.getElementsByTagName("bpmn:startEvent");
        for (int i = 0; i < startEvents.getLength(); i++) {
            Element el = (Element) startEvents.item(i);
            nodes.put(el.getAttribute("id"), BpmnNode.builder()
                    .id(el.getAttribute("id"))
                    .name(el.getAttribute("name"))
                    .type("StartEvent")
                    .properties(Map.of("gatewayDirection", safeAttribute(el, "gatewayDirection")))
                    .build());
        }

        // EndEvent
        NodeList endEvents = doc.getElementsByTagName("bpmn:endEvent");
        for (int i = 0; i < endEvents.getLength(); i++) {
            Element el = (Element) endEvents.item(i);
            nodes.put(el.getAttribute("id"), BpmnNode.builder()
                    .id(el.getAttribute("id"))
                    .name(el.getAttribute("name"))
                    .type("EndEvent")
                    .properties(Map.of("gatewayDirection", safeAttribute(el, "gatewayDirection")))
                    .build());
        }

        // UserTask
        NodeList userTasks = doc.getElementsByTagName("bpmn:userTask");
        for (int i = 0; i < userTasks.getLength(); i++) {
            Element el = (Element) userTasks.item(i);
            BpmnNode node = BpmnNode.builder()
                    .id(el.getAttribute("id"))
                    .name(el.getAttribute("name"))
                    .type("UserTask")
                    .assignedRole(extractAssignedRole(el))
                    .formId(extractFormId(el))
                    .properties(extractNodeProperties(el))
                    .build();
            nodes.put(el.getAttribute("id"), node);
        }

        // ExclusiveGateway (XOR)
        NodeList exclusiveGateways = doc.getElementsByTagName("bpmn:exclusiveGateway");
        for (int i = 0; i < exclusiveGateways.getLength(); i++) {
            Element el = (Element) exclusiveGateways.item(i);
            nodes.put(el.getAttribute("id"), BpmnNode.builder()
                    .id(el.getAttribute("id"))
                    .name(el.getAttribute("name"))
                    .type("ExclusiveGateway")
                    .properties(extractNodeProperties(el))
                    .build());
        }

        // ParallelGateway (AND)
        NodeList parallelGateways = doc.getElementsByTagName("bpmn:parallelGateway");
        for (int i = 0; i < parallelGateways.getLength(); i++) {
            Element el = (Element) parallelGateways.item(i);
            nodes.put(el.getAttribute("id"), BpmnNode.builder()
                    .id(el.getAttribute("id"))
                    .name(el.getAttribute("name"))
                    .type("ParallelGateway")
                    .properties(extractNodeProperties(el))
                    .build());
        }
    }

    private Map<String, String> extractNodeProperties(Element element) {
        Map<String, String> properties = new HashMap<>();

        if (element.hasAttribute("gatewayDirection")) {
            properties.put("gatewayDirection", element.getAttribute("gatewayDirection"));
        }

        if (element.getElementsByTagName("bpmn:standardLoopCharacteristics").getLength() > 0) {
            properties.put("loopType", "STANDARD");
        }

        if (element.getElementsByTagName("bpmn:multiInstanceLoopCharacteristics").getLength() > 0) {
            properties.put("loopType", "MULTI_INSTANCE");
        }

        return properties;
    }

    private String extractAssignedRole(Element el) {
        String[] candidateAttributes = {
                "camunda:candidateGroups",
                "camunda:assignee",
                "candidateGroups",
                "assignee",
                "data-role",
                "role"
        };

        for (String attribute : candidateAttributes) {
            String value = el.getAttribute(attribute);
            if (value != null && !value.isBlank()) {
                return value;
            }
        }

        return null;
    }

    private String extractFormId(Element el) {
        String[] candidateAttributes = {
                "camunda:formKey",
                "formKey",
                "data-form-id",
                "formId"
        };

        for (String attribute : candidateAttributes) {
            String value = el.getAttribute(attribute);
            if (value != null && !value.isBlank()) {
                return value;
            }
        }

        return null;
    }

    private void parseTransitions(Document doc, Map<String, List<BpmnNode.BpmnTransition>> transitionMap) {
        NodeList sequences = doc.getElementsByTagName("bpmn:sequenceFlow");
        for (int i = 0; i < sequences.getLength(); i++) {
            Element el = (Element) sequences.item(i);
            String id = el.getAttribute("id");
            String from = el.getAttribute("sourceRef");
            String to = el.getAttribute("targetRef");

            // Obtener condición si existe
            String condition = "";
            NodeList conditions = el.getElementsByTagName("bpmn:conditionExpression");
            if (conditions.getLength() > 0) {
                condition = conditions.item(0).getTextContent();
            }

            boolean defaultFlow = false;
            Element sourceElement = findElementById(doc, from);
            if (sourceElement != null && id.equals(sourceElement.getAttribute("default"))) {
                defaultFlow = true;
            }

            BpmnNode.BpmnTransition transition = BpmnNode.BpmnTransition.builder()
                    .id(id)
                    .fromNodeId(from)
                    .toNodeId(to)
                    .conditionExpression(condition)
                    .defaultFlow(defaultFlow)
                    .build();

            // Add to outgoing
            transitionMap.computeIfAbsent("out_" + from, k -> new ArrayList<>()).add(transition);
            // Add to incoming
            transitionMap.computeIfAbsent("in_" + to, k -> new ArrayList<>()).add(transition);
        }
    }

    public List<String> validateTopology(Map<String, BpmnNode> nodes) throws Exception {
        List<String> errors = new ArrayList<>();

        // Check: solo 1 StartEvent
        long startEvents = nodes.values().stream()
                .filter(n -> "StartEvent".equals(n.getType()))
                .count();
        if (startEvents != 1) {
            errors.add("Debe haber exactamente 1 nodo de Inicio");
        }

        // Check: al menos 1 EndEvent
        long endEvents = nodes.values().stream()
                .filter(n -> "EndEvent".equals(n.getType()))
                .count();
        if (endEvents < 1) {
            errors.add("Debe haber al menos 1 nodo de Fin");
        }

        // Check: todos los nodos conectados
        BpmnNode startNode = nodes.values().stream()
                .filter(n -> "StartEvent".equals(n.getType()))
                .findFirst()
                .orElse(null);

        if (startNode != null) {
            Set<String> reachable = new HashSet<>();
            dfs(startNode, reachable, nodes);

            Set<String> unreachable = nodes.keySet().stream()
                    .filter(id -> !reachable.contains(id))
                    .collect(Collectors.toSet());

            if (!unreachable.isEmpty()) {
                errors.add("Nodos sin conexión: " + unreachable);
            }
        }

        for (BpmnNode node : nodes.values()) {
            List<BpmnNode.BpmnTransition> incoming = node.getIncomingTransitions() != null
                    ? node.getIncomingTransitions() : List.of();
            List<BpmnNode.BpmnTransition> outgoing = node.getOutgoingTransitions() != null
                    ? node.getOutgoingTransitions() : List.of();

            if ("StartEvent".equals(node.getType()) && !incoming.isEmpty()) {
                errors.add("El nodo inicial no puede tener entradas: " + node.getId());
            }

            if ("EndEvent".equals(node.getType()) && !outgoing.isEmpty()) {
                errors.add("El nodo final no puede tener salidas: " + node.getId());
            }

            if (!"StartEvent".equals(node.getType()) && incoming.isEmpty()) {
                errors.add("El nodo debe tener al menos una entrada: " + node.getId());
            }

            if (!"EndEvent".equals(node.getType()) && outgoing.isEmpty()) {
                errors.add("El nodo debe tener al menos una salida: " + node.getId());
            }

            if ("UserTask".equals(node.getType()) && (node.getAssignedRole() == null || node.getAssignedRole().isBlank())) {
                errors.add("La tarea humana debe tener un rol asignado: " + node.getId());
            }

            if ("ExclusiveGateway".equals(node.getType())) {
                validateExclusiveGateway(node, errors);
            }

            if ("ParallelGateway".equals(node.getType())) {
                validateParallelGateway(node, errors);
            }
        }

        return errors;
    }

    private void validateExclusiveGateway(BpmnNode node, List<String> errors) {
        List<BpmnNode.BpmnTransition> outgoing = node.getOutgoingTransitions() != null
                ? node.getOutgoingTransitions() : List.of();
        if (outgoing.size() < 2) {
            errors.add("El gateway XOR debe tener al menos dos salidas: " + node.getId());
            return;
        }

        boolean hasDefault = outgoing.stream().anyMatch(BpmnNode.BpmnTransition::isDefaultFlow);
        boolean allConditional = outgoing.stream()
                .allMatch(transition -> transition.getConditionExpression() != null && !transition.getConditionExpression().isBlank());
        if (!hasDefault && !allConditional) {
            errors.add("El gateway XOR debe tener flujo por defecto o condiciones en todas sus salidas: " + node.getId());
        }
    }

    private void validateParallelGateway(BpmnNode node, List<String> errors) {
        int incoming = node.getIncomingTransitions() != null ? node.getIncomingTransitions().size() : 0;
        int outgoing = node.getOutgoingTransitions() != null ? node.getOutgoingTransitions().size() : 0;
        if (incoming <= 1 && outgoing <= 1) {
            errors.add("El gateway AND debe ser split o join real: " + node.getId());
        }
        if (incoming > 1 && outgoing > 1) {
            errors.add("El gateway AND no debe mezclar join y split en el mismo nodo: " + node.getId());
        }
    }

    private Element findElementById(Document doc, String elementId) {
        NodeList all = doc.getElementsByTagName("*");
        for (int i = 0; i < all.getLength(); i++) {
            Element candidate = (Element) all.item(i);
            if (elementId.equals(candidate.getAttribute("id"))) {
                return candidate;
            }
        }
        return null;
    }

    private String safeAttribute(Element element, String name) {
        return element.hasAttribute(name) ? element.getAttribute(name) : "";
    }

    private void dfs(BpmnNode node, Set<String> visited, Map<String, BpmnNode> allNodes) {
        if (visited.contains(node.getId())) return;
        visited.add(node.getId());

        if (node.getOutgoingTransitions() != null) {
            for (BpmnNode.BpmnTransition transition : node.getOutgoingTransitions()) {
                BpmnNode nextNode = allNodes.get(transition.getToNodeId());
                if (nextNode != null) {
                    dfs(nextNode, visited, allNodes);
                }
            }
        }
    }
}
