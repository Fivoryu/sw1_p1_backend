package com.banco.workflow.service;

import com.banco.workflow.model.DepartmentDefinition;
import com.banco.workflow.model.FormDefinition;
import com.banco.workflow.model.Policy;
import com.banco.workflow.model.ProcessInstance;
import com.banco.workflow.model.User;
import com.banco.workflow.model.WorkflowDefinition;
import com.banco.workflow.repository.DocumentUploadRepository;
import com.banco.workflow.repository.PolicyRepository;
import com.banco.workflow.repository.ProcessInstanceRepository;
import com.banco.workflow.repository.TaskRepository;
import com.banco.workflow.repository.WorkflowDefinitionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PolicyService {
    public static final String COLLABORATION_PRIVATE = "PRIVATE";
    public static final String COLLABORATION_READ_ONLY = "READ_ONLY";
    public static final String COLLABORATION_EDIT_SHARED = "EDIT_SHARED";
    public static final String UML_VERSION_25 = "2.5";
    public static final String DIAGRAM_NOTATION_UML_ACTIVITY = "BPMN_EXECUTABLE_WITH_UML_ACTIVITY_VIEW";

    private final PolicyRepository policyRepository;
    private final BpmnParserService parserService;
    private final UserService userService;
    private final WorkflowDefinitionService workflowDefinitionService;
    private final DepartmentService departmentService;
    private final ProcessInstanceRepository processInstanceRepository;
    private final TaskRepository taskRepository;
    private final DocumentUploadRepository documentUploadRepository;
    private final WorkflowDefinitionRepository workflowDefinitionRepository;

    public Policy createPolicy(String name, String description, String bpmnXml,
                               Map<String, Object> umlActivityJson,
                               String umlVersion,
                               String diagramNotation,
                               List<DepartmentDefinition> departments,
                               List<FormDefinition> forms,
                               Boolean collaborationEnabled,
                               String collaborationMode) throws Exception {
        User actor = requireAuthenticatedUser();
        String resolvedName = resolvePolicyName(name);
        Map<String, Object> resolvedUmlActivity = resolveUmlActivity(umlActivityJson, null);
        String resolvedXml = generateTechnicalBpmnXml(resolvedUmlActivity);
        Map<String, com.banco.workflow.model.BpmnNode> nodes = tryParseDraftGraph(resolvedXml);
        if (!nodes.isEmpty()) {
            validateTaskConfiguration(nodes, departments, forms);
        }

        int version = policyRepository.findAll().stream()
                .filter(policy -> resolvedName.equalsIgnoreCase(policy.getName()))
                .mapToInt(Policy::getVersion)
                .max()
                .orElse(0) + 1;
        String resolvedMode = normalizeCollaborationMode(collaborationEnabled, collaborationMode);
        LocalDateTime now = LocalDateTime.now();

        Policy policy = Policy.builder()
                .id(UUID.randomUUID().toString())
                .name(resolvedName)
                .description(description)
                .bpmnXml(resolvedXml)
                .umlActivityJson(resolvedUmlActivity)
                .umlVersion(resolveUmlVersion(umlVersion))
                .diagramNotation(resolveDiagramNotation(diagramNotation))
                .version(version)
                .status("DRAFT")
                .graph(nodes)
                .departments(safeDepartments(departments))
                .forms(safeForms(forms))
                .createdByUserId(actor.getId())
                .ownerUserId(actor.getId())
                .tenantEmpresa(actor.getEmpresa())
                .collaborationEnabled(Boolean.TRUE.equals(collaborationEnabled))
                .collaborationMode(resolvedMode)
                .lastEditedByUserId(actor.getId())
                .lastAutoSavedAt(now)
                .createdAt(now)
                .updatedAt(now)
                .active(false)
                .build();

        Policy saved = policyRepository.save(policy);
        log.info("Política creada {} v{}", saved.getName(), saved.getVersion());
        return saved;
    }

    public Optional<Policy> getPolicyById(String id) {
        User actor = requireAuthenticatedUser();
        return policyRepository.findById(id)
                .filter(policy -> canViewPolicy(policy, actor));
    }

    public List<Policy> getAllPolicies() {
        User actor = requireAuthenticatedUser();
        return policyRepository.findAll().stream()
                .filter(policy -> canViewPolicy(policy, actor))
                .toList();
    }

    public List<Policy> getActivePolicies() {
        return policyRepository.findAll().stream()
                .filter(policy -> "PUBLISHED".equals(policy.getStatus()) || policy.isActive())
                .toList();
    }

    public List<Policy> getPublishedPolicies() {
        return policyRepository.findByStatus("PUBLISHED");
    }

    public Policy updatePolicy(String id, String name, String description, String bpmnXml,
                               Map<String, Object> umlActivityJson,
                               String umlVersion,
                               String diagramNotation,
                               List<DepartmentDefinition> departments,
                               List<FormDefinition> forms,
                               Boolean collaborationEnabled,
                               String collaborationMode) throws Exception {
        User actor = requireAuthenticatedUser();
        Policy existing = policyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Política no encontrada: " + id));
        assertCanEditPolicy(existing, actor);

        String resolvedName = resolvePolicyName(name, existing.getName());
        Map<String, Object> resolvedUmlActivity = resolveUmlActivity(umlActivityJson, existing.getUmlActivityJson());
        String resolvedXml = generateTechnicalBpmnXml(resolvedUmlActivity);
        Map<String, com.banco.workflow.model.BpmnNode> nodes = tryParseDraftGraph(resolvedXml);
        if (!nodes.isEmpty()) {
            validateTaskConfiguration(nodes, departments, forms);
        }

        existing.setName(resolvedName);
        existing.setDescription(description);
        existing.setBpmnXml(resolvedXml);
        existing.setUmlActivityJson(resolvedUmlActivity);
        existing.setUmlVersion(resolveUmlVersion(umlVersion));
        existing.setDiagramNotation(resolveDiagramNotation(diagramNotation));
        existing.setGraph(nodes);
        existing.setDepartments(safeDepartments(departments));
        existing.setForms(safeForms(forms));
        existing.setCollaborationEnabled(Boolean.TRUE.equals(collaborationEnabled));
        existing.setCollaborationMode(normalizeCollaborationMode(collaborationEnabled, collaborationMode));
        existing.setLastEditedByUserId(actor.getId());
        existing.setLastAutoSavedAt(LocalDateTime.now());
        existing.setUpdatedAt(LocalDateTime.now());
        if ("PUBLISHED".equals(existing.getStatus())) {
            existing.setStatus("DRAFT");
            existing.setActive(false);
        }
        return policyRepository.save(existing);
    }

    public void deactivatePolicy(String id) {
        User actor = requireAuthenticatedUser();
        Policy policy = policyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Política no encontrada: " + id));
        assertCanEditPolicy(policy, actor);
        policy.setActive(false);
        policy.setStatus("ARCHIVED");
        policy.setUpdatedAt(LocalDateTime.now());
        policyRepository.save(policy);
    }

    public void publishPolicy(String id) throws Exception {
        User actor = requireAuthenticatedUser();
        Policy policy = policyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Política no encontrada: " + id));
        assertCanEditPolicy(policy, actor);
        if (policy.getUmlActivityJson() != null && !policy.getUmlActivityJson().isEmpty()) {
            policy.setBpmnXml(generateTechnicalBpmnXml(resolveUmlActivity(policy.getUmlActivityJson(), null)));
        }
        String xml = policy.getBpmnXml() != null && !policy.getBpmnXml().isBlank() ? policy.getBpmnXml() : defaultDraftBpmnXml();
        Map<String, com.banco.workflow.model.BpmnNode> nodes = parseAndValidateGraph(xml);
        validateTaskConfiguration(nodes, policy.getDepartments(), policy.getForms());
        policy.setGraph(nodes);
        WorkflowDefinition definition = workflowDefinitionService.compileAndPublish(policy);
        if (!"VALID".equals(definition.getValidationStatus())) {
            throw new IllegalArgumentException("La política no es publicable: " + String.join(", ", definition.getValidationErrors()));
        }
        policy.setActive(true);
        policy.setStatus("PUBLISHED");
        policy.setUpdatedAt(LocalDateTime.now());
        policyRepository.save(policy);
    }

    public void deprecatePolicy(String id) {
        User actor = requireAuthenticatedUser();
        Policy policy = policyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Política no encontrada: " + id));
        assertCanEditPolicy(policy, actor);
        policy.setActive(false);
        policy.setStatus("DEPRECATED");
        policy.setUpdatedAt(LocalDateTime.now());
        policyRepository.save(policy);
    }

    /**
     * Borra permanentemente la política y dependencias: instancias de trámite, tareas, adjuntos
     * y versiones de {@link WorkflowDefinition} publicadas para esa política.
     * Los formularios incrustados en el documento de política se eliminan con el documento.
     * No elimina el catálogo reutilizable {@code forms} (DynamicForm) por ser por empresa.
     */
    public void permanentlyDeletePolicy(String id) {
        User actor = requireAuthenticatedUser();
        Policy policy = policyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Política no encontrada: " + id));
        if (!canViewPolicy(policy, actor)) {
            throw new RuntimeException("No tienes permiso para ver o eliminar esta política");
        }
        assertIsPolicyOwnerOrAdmin(actor, policy);

        java.util.List<ProcessInstance> instances = processInstanceRepository.findByPolicyId(id);
        for (ProcessInstance instance : instances) {
            if (instance.getId() == null) {
                continue;
            }
            taskRepository.deleteByProcessInstanceId(instance.getId());
            documentUploadRepository.deleteByProcessInstanceId(instance.getId());
        }
        processInstanceRepository.deleteAll(instances);
        workflowDefinitionRepository.deleteByPolicyId(id);
        policyRepository.deleteById(id);
        log.info("Política {} eliminada en cascada por usuario {}", id, actor.getId());
    }

    /**
     * El dueño de la política o un {@code ROLE_ADMIN} de la misma empresa pueden eliminar de forma definitiva.
     * Un colaborador con edición compartida no basta, para evitar borrados accidentales.
     */
    private void assertIsPolicyOwnerOrAdmin(User actor, Policy policy) {
        if (actor.getId() != null && actor.getId().equals(policy.getOwnerUserId())) {
            return;
        }
        if (isAdminRole(actor) && canViewPolicy(policy, actor)) {
            return;
        }
        throw new RuntimeException("Solo el dueño de la política o un administrador de la misma empresa puede eliminarla");
    }

    private boolean isAdminRole(User user) {
        if (user.getRoles() == null) {
            return false;
        }
        return user.getRoles().stream().anyMatch(r -> "ROLE_ADMIN".equalsIgnoreCase(r) || "ADMIN".equalsIgnoreCase(r));
    }

    private java.util.Map<String, com.banco.workflow.model.BpmnNode> parseAndValidateGraph(String bpmnXml) throws Exception {
        var nodes = parserService.parseProcessDefinition(bpmnXml);
        var errors = parserService.validateTopology(nodes);
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException("BPMN inválido: " + String.join(", ", errors));
        }
        return nodes;
    }

    public Optional<WorkflowDefinition> getPublishedDefinition(String policyId) {
        Policy policy = policyRepository.findById(policyId)
                .orElseThrow(() -> new RuntimeException("Política no encontrada: " + policyId));
        return workflowDefinitionService.getDefinitionByPolicy(policyId, policy.getVersion());
    }

    private void validateTaskConfiguration(
            java.util.Map<String, com.banco.workflow.model.BpmnNode> nodes,
            List<DepartmentDefinition> departments,
            List<FormDefinition> forms
    ) {
        if (nodes == null || nodes.isEmpty()) {
            throw new IllegalArgumentException("La política no tiene tareas ni nodos configurados");
        }

        Set<String> validRoles = new HashSet<>();
        if (departments != null) {
            departments.stream()
                    .map(DepartmentDefinition::getRole)
                    .filter(role -> role != null && !role.isBlank())
                    .forEach(validRoles::add);
        }
        if (validRoles.isEmpty()) {
            departmentService.getActiveDepartments().stream()
                    .map(com.banco.workflow.model.Department::getRole)
                    .filter(role -> role != null && !role.isBlank())
                    .forEach(validRoles::add);
        }

        Set<String> validForms = new HashSet<>();
        if (forms != null) {
            forms.stream()
                    .map(FormDefinition::getId)
                    .filter(formId -> formId != null && !formId.isBlank())
                    .forEach(validForms::add);
        }

        nodes.values().stream()
                .filter(node -> "UserTask".equals(node.getType()))
                .forEach(node -> {
                    if (node.getAssignedRole() != null && !node.getAssignedRole().isBlank() && !validRoles.contains(node.getAssignedRole())) {
                        throw new IllegalArgumentException("La tarea " + node.getName() + " referencia un departamento/rol no definido: " + node.getAssignedRole());
                    }
                    if (node.getFormId() != null && !node.getFormId().isBlank() && !validForms.contains(node.getFormId())) {
                        throw new IllegalArgumentException("La tarea " + node.getName() + " referencia un formulario no definido: " + node.getFormId());
                    }
                });
    }

    private User requireAuthenticatedUser() {
        return userService.getCurrentAuthenticatedUser()
                .orElseThrow(() -> new RuntimeException("No hay usuario autenticado"));
    }

    private boolean canViewPolicy(Policy policy, User actor) {
        if (policy.getTenantEmpresa() == null || actor.getEmpresa() == null) {
            return true;
        }
        return policy.getTenantEmpresa().equalsIgnoreCase(actor.getEmpresa());
    }

    public boolean canEditPolicy(Policy policy, User actor) {
        if (!canViewPolicy(policy, actor)) {
            return false;
        }
        if (actor.getId() != null && actor.getId().equals(policy.getOwnerUserId())) {
            return true;
        }
        return policy.isCollaborationEnabled()
                && COLLABORATION_EDIT_SHARED.equals(policy.getCollaborationMode());
    }

    private void assertCanEditPolicy(Policy policy, User actor) {
        if (!canEditPolicy(policy, actor)) {
            throw new RuntimeException("No tienes permiso de edición sobre esta política");
        }
    }

    private String resolvePolicyName(String requestedName) {
        if (requestedName != null && !requestedName.isBlank()) {
            return requestedName.trim();
        }
        return nextUntitledName();
    }

    private String resolvePolicyName(String requestedName, String fallbackName) {
        if (requestedName != null && !requestedName.isBlank()) {
            return requestedName.trim();
        }
        if (fallbackName != null && !fallbackName.isBlank()) {
            return fallbackName;
        }
        return nextUntitledName();
    }

    private String nextUntitledName() {
        int nextIndex = policyRepository.findAll().stream()
                .map(Policy::getName)
                .filter(name -> name != null && name.startsWith("Sin nombre"))
                .mapToInt(name -> {
                    String suffix = name.replace("Sin nombre", "").trim();
                    try {
                        return Integer.parseInt(suffix);
                    } catch (NumberFormatException ignored) {
                        return 0;
                    }
                })
                .max()
                .orElse(0) + 1;
        return "Sin nombre " + nextIndex;
    }

    private String normalizeCollaborationMode(Boolean enabled, String requestedMode) {
        if (!Boolean.TRUE.equals(enabled)) {
            return COLLABORATION_PRIVATE;
        }
        if (COLLABORATION_EDIT_SHARED.equals(requestedMode)) {
            return COLLABORATION_EDIT_SHARED;
        }
        return COLLABORATION_READ_ONLY;
    }

    private String resolveUmlVersion(String requestedVersion) {
        if (requestedVersion != null && !requestedVersion.isBlank()) {
            return requestedVersion.trim();
        }
        return UML_VERSION_25;
    }

    private String resolveDiagramNotation(String requestedNotation) {
        if (requestedNotation != null && !requestedNotation.isBlank()) {
            return requestedNotation.trim();
        }
        return DIAGRAM_NOTATION_UML_ACTIVITY;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> resolveUmlActivity(Map<String, Object> requestedUml, Map<String, Object> fallbackUml) {
        Map<String, Object> resolved = requestedUml != null && !requestedUml.isEmpty()
                ? requestedUml
                : fallbackUml;
        if (resolved == null || resolved.isEmpty()) {
            return defaultDraftUmlActivityJson();
        }
        Object nodes = resolved.get("nodes");
        Object partitions = resolved.get("partitions");
        Object metadata = resolved.get("metadata");
        if (!(nodes instanceof List<?>) || !(partitions instanceof List<?>)) {
            throw new IllegalArgumentException("El diagrama UML 2.5 debe incluir nodes y partitions");
        }
        if (((List<?>) partitions).isEmpty()) {
            throw new IllegalArgumentException("El diagrama UML 2.5 debe incluir al menos una particion/calle");
        }
        if (!(metadata instanceof Map<?, ?> metadataMap)) {
            throw new IllegalArgumentException("El diagrama UML 2.5 debe incluir metadata");
        }
        Object umlVersion = metadataMap.get("umlVersion");
        Object diagramType = metadataMap.get("diagramType");
        if (!UML_VERSION_25.equals(String.valueOf(umlVersion))) {
            throw new IllegalArgumentException("El diagrama debe usar UML 2.5");
        }
        if (!"ActivityDiagram".equals(String.valueOf(diagramType))) {
            throw new IllegalArgumentException("El diagrama debe ser ActivityDiagram");
        }
        long initialCount = ((List<?>) nodes).stream()
                .filter(Map.class::isInstance)
                .map(item -> (Map<String, Object>) item)
                .filter(item -> "INITIAL".equals(String.valueOf(item.get("type"))))
                .count();
        if (initialCount != 1) {
            throw new IllegalArgumentException("El diagrama UML 2.5 debe tener exactamente un nodo inicial");
        }
        boolean hasFinal = ((List<?>) nodes).stream()
                .filter(Map.class::isInstance)
                .map(item -> (Map<String, Object>) item)
                .anyMatch(item -> "ACTIVITY_FINAL".equals(String.valueOf(item.get("type"))));
        if (!hasFinal) {
            throw new IllegalArgumentException("El diagrama UML 2.5 debe tener al menos un nodo final");
        }
        return resolved;
    }

    @SuppressWarnings("unchecked")
    private String generateTechnicalBpmnXml(Map<String, Object> umlActivity) {
        List<Map<String, Object>> nodes = ((List<?>) umlActivity.getOrDefault("nodes", List.of())).stream()
                .filter(Map.class::isInstance)
                .map(item -> (Map<String, Object>) item)
                .filter(node -> !"NOTE".equals(String.valueOf(node.get("type"))))
                .toList();
        List<Map<String, Object>> edges = ((List<?>) umlActivity.getOrDefault("edges", List.of())).stream()
                .filter(Map.class::isInstance)
                .map(item -> (Map<String, Object>) item)
                .filter(edge -> !"Annotation".equals(String.valueOf(edge.get("type"))))
                .toList();

        StringBuilder processItems = new StringBuilder();
        for (Map<String, Object> node : nodes) {
            processItems.append(renderTechnicalBpmnNode(node)).append('\n');
        }

        StringBuilder flowItems = new StringBuilder();
        Set<String> nodeIds = nodes.stream()
                .map(node -> String.valueOf(node.get("id")))
                .collect(java.util.stream.Collectors.toSet());
        int generatedFlowIndex = 1;
        for (Map<String, Object> edge : edges) {
            String source = String.valueOf(edge.getOrDefault("source", ""));
            String target = String.valueOf(edge.getOrDefault("target", ""));
            if (!nodeIds.contains(source) || !nodeIds.contains(target)) {
                continue;
            }
            String id = String.valueOf(edge.getOrDefault("id", "UmlFlow_" + generatedFlowIndex++));
            String guard = edge.get("guard") != null ? " name=\"" + escapeXml(String.valueOf(edge.get("guard"))) + "\"" : "";
            flowItems.append("    <bpmn:sequenceFlow id=\"")
                    .append(escapeXml(id))
                    .append("\" sourceRef=\"")
                    .append(escapeXml(source))
                    .append("\" targetRef=\"")
                    .append(escapeXml(target))
                    .append("\"")
                    .append(guard)
                    .append(" />\n");
        }

        if (flowItems.length() == 0 && nodes.size() > 1) {
            for (int index = 0; index < nodes.size() - 1; index++) {
                flowItems.append("    <bpmn:sequenceFlow id=\"UmlAutoFlow_")
                        .append(index + 1)
                        .append("\" sourceRef=\"")
                        .append(escapeXml(String.valueOf(nodes.get(index).get("id"))))
                        .append("\" targetRef=\"")
                        .append(escapeXml(String.valueOf(nodes.get(index + 1).get("id"))))
                        .append("\" />\n");
            }
        }

        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <bpmn:definitions xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL"
                                  xmlns:bpmndi="http://www.omg.org/spec/BPMN/20100524/DI"
                                  xmlns:dc="http://www.omg.org/spec/DD/20100524/DC"
                                  xmlns:di="http://www.omg.org/spec/DD/20100524/DI"
                                  id="Definitions_UmlTechnical"
                                  targetNamespace="https://workflow-cloud.local/uml-technical-bpmn">
                  <bpmn:process id="Process_UmlTechnical" name="Derivado tecnico desde UML 2.5" isExecutable="true">
                %s%s  </bpmn:process>
                  <bpmndi:BPMNDiagram id="BPMNDiagram_UmlTechnical">
                    <bpmndi:BPMNPlane id="BPMNPlane_UmlTechnical" bpmnElement="Process_UmlTechnical" />
                  </bpmndi:BPMNDiagram>
                </bpmn:definitions>
                """.formatted(processItems, flowItems);
    }

    private String renderTechnicalBpmnNode(Map<String, Object> node) {
        String id = escapeXml(String.valueOf(node.getOrDefault("id", UUID.randomUUID().toString())));
        String name = escapeXml(String.valueOf(node.getOrDefault("label", id)));
        String type = String.valueOf(node.getOrDefault("type", "ACTION"));
        return switch (type) {
            case "INITIAL" -> "    <bpmn:startEvent id=\"" + id + "\" name=\"" + name + "\" />";
            case "ACTIVITY_FINAL" -> "    <bpmn:endEvent id=\"" + id + "\" name=\"" + name + "\" />";
            case "DECISION", "MERGE" -> "    <bpmn:exclusiveGateway id=\"" + id + "\" name=\"" + name + "\" />";
            case "FORK", "JOIN" -> "    <bpmn:parallelGateway id=\"" + id + "\" name=\"" + name + "\" />";
            default -> "    <bpmn:userTask id=\"" + id + "\" name=\"" + name + "\" />";
        };
    }

    private String escapeXml(String value) {
        return value
                .replace("&", "&amp;")
                .replace("\"", "&quot;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    private Map<String, Object> defaultDraftUmlActivityJson() {
        return Map.of(
                "nodes", List.of(
                        Map.of("id", "uml_initial", "type", "INITIAL", "label", "Inicio", "partition", "partition_negocio", "umlElement", "InitialNode"),
                        Map.of("id", "uml_final", "type", "ACTIVITY_FINAL", "label", "Fin", "partition", "partition_negocio", "umlElement", "ActivityFinalNode")
                ),
                "edges", List.of(Map.of("id", "uml_edge_1", "source", "uml_initial", "target", "uml_final", "type", "ControlFlow")),
                "partitions", List.of(Map.of("id", "partition_negocio", "name", "Negocio", "umlElement", "ActivityPartition")),
                "metadata", Map.of("umlVersion", UML_VERSION_25, "diagramType", "ActivityDiagram", "partitionElement", "ActivityPartition")
        );
    }

    private Map<String, com.banco.workflow.model.BpmnNode> tryParseDraftGraph(String bpmnXml) {
        if (bpmnXml == null || bpmnXml.isBlank()) {
            return Collections.emptyMap();
        }
        try {
            return parserService.parseProcessDefinition(bpmnXml);
        } catch (Exception ignored) {
            return Collections.emptyMap();
        }
    }

    private List<DepartmentDefinition> safeDepartments(List<DepartmentDefinition> departments) {
        return departments != null ? departments : List.of();
    }

    private List<FormDefinition> safeForms(List<FormDefinition> forms) {
        return forms != null ? forms : List.of();
    }

    private String defaultDraftBpmnXml() {
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <bpmn:definitions xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL"
                                  xmlns:bpmndi="http://www.omg.org/spec/BPMN/20100524/DI"
                                  xmlns:dc="http://www.omg.org/spec/DD/20100524/DC"
                                  xmlns:di="http://www.omg.org/spec/DD/20100524/DI"
                                  id="Definitions_Draft"
                                  targetNamespace="http://bpmn.io/schema/bpmn">
                  <bpmn:process id="Process_Draft" isExecutable="true" />
                  <bpmndi:BPMNDiagram id="BPMNDiagram_Draft">
                    <bpmndi:BPMNPlane id="BPMNPlane_Draft" bpmnElement="Process_Draft" />
                  </bpmndi:BPMNDiagram>
                </bpmn:definitions>
                """;
    }
}
