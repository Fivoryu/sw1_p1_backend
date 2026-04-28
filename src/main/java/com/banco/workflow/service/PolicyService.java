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
                               List<DepartmentDefinition> departments,
                               List<FormDefinition> forms,
                               Boolean collaborationEnabled,
                               String collaborationMode) throws Exception {
        User actor = requireAuthenticatedUser();
        String resolvedName = resolvePolicyName(name);
        String resolvedXml = bpmnXml != null && !bpmnXml.isBlank() ? bpmnXml : defaultDraftBpmnXml();
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
                               List<DepartmentDefinition> departments,
                               List<FormDefinition> forms,
                               Boolean collaborationEnabled,
                               String collaborationMode) throws Exception {
        User actor = requireAuthenticatedUser();
        Policy existing = policyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Política no encontrada: " + id));
        assertCanEditPolicy(existing, actor);

        String resolvedName = resolvePolicyName(name, existing.getName());
        String resolvedXml = bpmnXml != null && !bpmnXml.isBlank() ? bpmnXml : existing.getBpmnXml();
        Map<String, com.banco.workflow.model.BpmnNode> nodes = tryParseDraftGraph(resolvedXml);
        if (!nodes.isEmpty()) {
            validateTaskConfiguration(nodes, departments, forms);
        }

        existing.setName(resolvedName);
        existing.setDescription(description);
        existing.setBpmnXml(resolvedXml);
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
