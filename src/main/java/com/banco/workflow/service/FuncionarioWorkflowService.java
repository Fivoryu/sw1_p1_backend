package com.banco.workflow.service;

import com.banco.workflow.config.TaskQueueWebSocketHandler;
import com.banco.workflow.dto.FuncionarioWorkflowDtos;
import com.banco.workflow.model.BpmnNode;
import com.banco.workflow.model.DocumentUpload;
import com.banco.workflow.model.FormDefinition;
import com.banco.workflow.model.Policy;
import com.banco.workflow.model.ProcessInstance;
import com.banco.workflow.model.Task;
import com.banco.workflow.model.User;
import com.banco.workflow.model.WorkflowDefinition;
import com.banco.workflow.repository.DocumentUploadRepository;
import com.banco.workflow.repository.PolicyRepository;
import com.banco.workflow.repository.ProcessInstanceRepository;
import com.banco.workflow.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class FuncionarioWorkflowService {

    private final TaskRepository taskRepository;
    private final ProcessInstanceRepository processInstanceRepository;
    private final PolicyRepository policyRepository;
    private final WorkflowDefinitionService workflowDefinitionService;
    private final WorkflowRuntimeService workflowRuntimeService;
    private final FormValidationService formValidationService;
    private final UserService userService;
    private final TaskQueueWebSocketHandler taskQueueWebSocketHandler;
    private final DocumentUploadRepository documentUploadRepository;
    private final TaskAssignmentService taskAssignmentService;
    private final WorkflowHistoryService workflowHistoryService;

    public List<FuncionarioWorkflowDtos.TareaDTO> getBandejaActual() {
        User user = getAuthenticatedUser();
        LinkedHashSet<Task> tasks = new LinkedHashSet<>();
        String department = user.getDepartamento();
        String userEmpresa = user.getEmpresa();

        tasks.addAll(taskRepository.findByAssigneeAndStatus(user.getUsername(), "IN_PROGRESS"));
        tasks.addAll(taskRepository.findByAssigneeAndStatus(user.getUsername(), "PENDING"));

        if (department != null && !department.isBlank()) {
            tasks.addAll(taskRepository.findByDepartmentAssignedAndStatus(department, "PENDING"));
            tasks.addAll(taskRepository.findByDepartmentAssignedAndStatus(department, "IN_PROGRESS"));
        }

        for (String role : user.getRoles()) {
            tasks.addAll(taskRepository.findByCandidateRoleAndStatus(role, "PENDING"));
            tasks.addAll(taskRepository.findByCandidateRoleAndStatus(role, "IN_PROGRESS"));
        }

        return tasks.stream()
                .filter(task -> taskBelongsToTenant(task, userEmpresa))
                .filter(task -> {
                    boolean belongsToDepartment = department != null && department.equalsIgnoreCase(task.getDepartmentAssigned());
                    boolean belongsToRole = task.getCandidateRole() != null && user.getRoles().contains(task.getCandidateRole());
                    boolean belongsToAssignee = user.getUsername().equals(task.getAssignee());
                    boolean isVisible = belongsToDepartment || belongsToRole || belongsToAssignee;
                    boolean available = task.getAssignee() == null || user.getUsername().equals(task.getAssignee());
                    return isVisible && available;
                })
                .sorted(Comparator.comparing(Task::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(FuncionarioWorkflowDtos::fromTask)
                .toList();
    }

    public FuncionarioWorkflowDtos.TareaDetalleDTO getDetalleTarea(String tareaId) {
        User user = getAuthenticatedUser();
        Task task = loadTask(tareaId);
        validateTaskAccess(task, user);
        return buildTaskDetail(task, user);
    }

    public FuncionarioWorkflowDtos.TareaDetalleDTO reclamarTarea(String tareaId) {
        User user = getAuthenticatedUser();
        Task task = loadTask(tareaId);
        validateTaskAccess(task, user);

        if (!"PENDING".equals(task.getStatus()) && !"IN_PROGRESS".equals(task.getStatus())) {
            throw new IllegalArgumentException("La tarea no está disponible para ser reclamada");
        }

        if (task.getAssignee() != null && !user.getUsername().equals(task.getAssignee())) {
            throw new IllegalArgumentException("La tarea ya fue reclamada por otro funcionario");
        }

        task.setAssignee(user.getUsername());
        task.setStatus("IN_PROGRESS");
        taskRepository.save(task);
        broadcastTaskUpdate(task, "TASK_CLAIMED");

        return buildTaskDetail(task, user);
    }

    public FuncionarioWorkflowDtos.GuardadoBorradorDTO guardarBorrador(String tareaId, Map<String, Object> valoresFormulario) {
        User user = getAuthenticatedUser();
        Task task = loadTask(tareaId);
        validateTaskAccess(task, user);
        claimIfNeeded(task, user);

        task.setFormData(valoresFormulario != null ? valoresFormulario : Map.of());
        task.setVariables(valoresFormulario != null ? valoresFormulario : Map.of());
        taskRepository.save(task);

        ProcessInstance instance = loadInstance(task.getProcessInstanceId());
        workflowHistoryService.record(instance, task.getNodeId(), "DRAFT_SAVED", task.getNodeType(), user.getId(), castMap(task.getFormData()), null);
        processInstanceRepository.save(instance);

        return FuncionarioWorkflowDtos.GuardadoBorradorDTO.builder()
                .success(true)
                .message("Borrador guardado correctamente")
                .detalle(buildTaskDetail(task, user))
                .build();
    }

    public FuncionarioWorkflowDtos.CorreccionDTO solicitarCorreccion(String tareaId, String motivo) {
        return solicitarCorreccion(tareaId, motivo, null);
    }

    public List<FuncionarioWorkflowDtos.CorrectionTargetDTO> listCorrectionTargets(String tareaId) {
        User user = getAuthenticatedUser();
        Task task = loadTask(tareaId);
        validateTaskAccess(task, user);

        ProcessInstance instance = loadInstance(task.getProcessInstanceId());
        Policy policy = loadPolicy(instance.getPolicyId());
        WorkflowDefinition definition = loadDefinition(policy);

        List<String> candidateNodeIds = resolvePreviousHumanNodes(instance, task, definition);
        return candidateNodeIds.stream()
                .map(definition.getGraph()::get)
                .filter(Objects::nonNull)
                .map(node -> FuncionarioWorkflowDtos.CorrectionTargetDTO.builder()
                        .nodeId(node.getId())
                        .nodeName(node.getName())
                        .nodeType(node.getType())
                        .build())
                .toList();
    }

    public FuncionarioWorkflowDtos.CorreccionDTO solicitarCorreccion(String tareaId, String motivo, String targetNodeId) {
        if (motivo == null || motivo.isBlank()) {
            throw new IllegalArgumentException("Debes indicar el motivo de la corrección");
        }

        User user = getAuthenticatedUser();
        Task task = loadTask(tareaId);
        validateTaskAccess(task, user);
        claimIfNeeded(task, user);

        ProcessInstance instance = loadInstance(task.getProcessInstanceId());
        Policy policy = loadPolicy(instance.getPolicyId());
        WorkflowDefinition definition = loadDefinition(policy);

        String previousNodeId = resolveTargetCorrectionNode(instance, task, definition, targetNodeId);
        if (previousNodeId == null) {
            throw new IllegalArgumentException("No existe un paso previo válido para devolver el trámite");
        }

        BpmnNode previousNode = definition.getGraph().get(previousNodeId);
        if (previousNode == null) {
            throw new IllegalArgumentException("No se encontró el nodo previo en la definición del workflow");
        }

        task.setStatus("RETURNED");
        task.setRejectionReason(motivo);
        task.setCompletedAt(LocalDateTime.now());
        taskRepository.save(task);

        instance.setStatus("IN_PROGRESS");
        instance.setActiveNodeIds(new ArrayList<>(List.of(previousNodeId)));
        workflowHistoryService.record(
                instance,
                task.getNodeId(),
                "RETURNED_FOR_CORRECTION",
                task.getNodeType(),
                user.getId(),
                Map.of("motivo", motivo, "previousNodeId", previousNodeId),
                null
        );
        processInstanceRepository.save(instance);

        Task returnedTask = taskAssignmentService.createHumanTask(instance, previousNode);
        returnedTask.setStatus("PENDING");
        taskRepository.save(returnedTask);
        broadcastTaskUpdate(returnedTask, "TASK_RETURNED");

        return FuncionarioWorkflowDtos.CorreccionDTO.builder()
                .success(true)
                .message("El trámite fue devuelto al paso anterior para corrección")
                .instanciaId(instance.getId())
                .nodoDevueltoId(previousNode.getId())
                .nodoDevueltoNombre(previousNode.getName())
                .build();
    }

    public FuncionarioWorkflowDtos.ResultadoDerivacionDTO completarTarea(String tareaId, Map<String, Object> valoresFormulario) {
        User user = getAuthenticatedUser();
        Task task = loadTask(tareaId);
        validateTaskAccess(task, user);
        claimIfNeeded(task, user);

        ProcessInstance instance = loadInstance(task.getProcessInstanceId());
        Policy policy = loadPolicy(instance.getPolicyId());
        WorkflowDefinition definition = loadDefinition(policy);
        FormDefinition form = resolveForm(policy, task.getFormId());

        Map<String, String> validationErrors = formValidationService.validate(form, valoresFormulario);
        if (!validationErrors.isEmpty()) {
            throw new IllegalArgumentException("Formulario inválido: " + String.join(" | ", validationErrors.values()));
        }

        if (instance.getCompletedForms() == null) {
            instance.setCompletedForms(new ArrayList<>());
        }

        instance.getCompletedForms().add(new ProcessInstance.CompletedForm(
                task.getFormId(),
                task.getId(),
                task.getNodeId(),
                user.getId(),
                LocalDateTime.now(),
                valoresFormulario
        ));

        task.setFormData(valoresFormulario);
        taskRepository.save(task);

        ProcessInstance updated = workflowRuntimeService.completeTask(definition, instance, task, valoresFormulario, user);
        broadcastTaskUpdate(task, "TASK_COMPLETED");

        List<FuncionarioWorkflowDtos.TareaDTO> nuevasTareas = taskRepository
                .findByProcessInstanceId(updated.getId()).stream()
                .filter(candidate -> "PENDING".equals(candidate.getStatus()) || "IN_PROGRESS".equals(candidate.getStatus()))
                .map(FuncionarioWorkflowDtos::fromTask)
                .toList();

        return FuncionarioWorkflowDtos.ResultadoDerivacionDTO.builder()
                .success(true)
                .message("Tarea completada y trámite derivado correctamente")
                .estadoInstancia(updated.getStatus())
                .nuevasTareas(nuevasTareas)
                .activeNodeIds(updated.getActiveNodeIds())
                .contextoDatos(updated.getVariables())
                .build();
    }

    public FuncionarioWorkflowDtos.HistorialTramiteDTO getHistorial(String instanciaId) {
        ProcessInstance instance = loadInstance(instanciaId);
        Policy policy = loadPolicy(instance.getPolicyId());
        List<FuncionarioWorkflowDtos.DocumentoResumenDTO> documentos = documentUploadRepository.findByProcessInstanceId(instanciaId)
                .stream()
                .map(FuncionarioWorkflowDtos::fromDocument)
                .toList();

        List<FuncionarioWorkflowDtos.FormularioCompletoDTO> forms = (instance.getCompletedForms() != null
                ? instance.getCompletedForms() : List.<ProcessInstance.CompletedForm>of()).stream()
                .map(form -> FuncionarioWorkflowDtos.FormularioCompletoDTO.builder()
                        .formularioId(form.getFormId())
                        .tareaId(form.getTaskId())
                        .nodoId(form.getNodeId())
                        .usuarioId(form.getCompletedByUserId())
                        .fecha(form.getCompletedAt())
                        .estado("COMPLETED")
                        .valores(form.getValues())
                        .build())
                .toList();

        return FuncionarioWorkflowDtos.HistorialTramiteDTO.builder()
                .instanciaId(instance.getId())
                .policyName(policy.getName())
                .estado(instance.getStatus())
                .historialNodos(instance.getHistory())
                .formulariosCompletados(forms)
                .documentos(documentos)
                .build();
    }

    public List<FuncionarioWorkflowDtos.BuscarTramiteDTO> buscarTramites(
            String workflowInstanceId,
            String clienteDni,
            String estado,
            String fechaDesde,
            String fechaHasta
    ) {
        User user = getAuthenticatedUser();
        LocalDateTime from = parseDateStart(fechaDesde);
        LocalDateTime to = parseDateEnd(fechaHasta);
        String userEmpresa = user.getEmpresa();

        return processInstanceRepository.findAll().stream()
                .filter(instance -> matchesTenant(instance, userEmpresa))
                .filter(instance -> isVisibleToUser(instance, user))
                .filter(instance -> workflowInstanceId == null || workflowInstanceId.isBlank() || instance.getId().toLowerCase().contains(workflowInstanceId.toLowerCase()))
                .filter(instance -> estado == null || estado.isBlank() || estado.equalsIgnoreCase(instance.getStatus()))
                .filter(instance -> {
                    String instanceDni = extractCustomerDni(instance);
                    return clienteDni == null || clienteDni.isBlank() || (instanceDni != null && instanceDni.toLowerCase().contains(clienteDni.toLowerCase()));
                })
                .filter(instance -> from == null || (instance.getInitiatedAt() != null && !instance.getInitiatedAt().isBefore(from)))
                .filter(instance -> to == null || (instance.getInitiatedAt() != null && !instance.getInitiatedAt().isAfter(to)))
                .sorted(Comparator.comparing(ProcessInstance::getInitiatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(instance -> FuncionarioWorkflowDtos.BuscarTramiteDTO.builder()
                        .instanciaId(instance.getId())
                        .policyName(instance.getPolicyName())
                        .estado(instance.getStatus())
                        .clienteNombre(extractCustomerName(instance))
                        .clienteDni(extractCustomerDni(instance))
                        .departamentoActual(resolveCurrentDepartment(instance))
                        .fechaInicio(instance.getInitiatedAt())
                        .ultimaActualizacion(resolveLastUpdate(instance))
                        .build())
                .toList();
    }

    private FuncionarioWorkflowDtos.TareaDetalleDTO buildTaskDetail(Task task, User user) {
        ProcessInstance instance = loadInstance(task.getProcessInstanceId());
        Policy policy = loadPolicy(instance.getPolicyId());
        WorkflowDefinition definition = loadDefinition(policy);
        FormDefinition form = resolveForm(policy, task.getFormId());
        boolean claimedByCurrentUser = user.getUsername().equals(task.getAssignee());
        boolean blockedByOtherUser = task.getAssignee() != null && !claimedByCurrentUser;
        boolean canClaim = "PENDING".equals(task.getStatus()) && task.getAssignee() == null;
        boolean canEdit = !blockedByOtherUser;

        List<FuncionarioWorkflowDtos.FormularioCompletoDTO> previousForms = (instance.getCompletedForms() != null
                ? instance.getCompletedForms() : List.<ProcessInstance.CompletedForm>of()).stream()
                .map(formEntry -> FuncionarioWorkflowDtos.FormularioCompletoDTO.builder()
                        .formularioId(formEntry.getFormId())
                        .tareaId(formEntry.getTaskId())
                        .nodoId(formEntry.getNodeId())
                        .usuarioId(formEntry.getCompletedByUserId())
                        .fecha(formEntry.getCompletedAt())
                        .estado("COMPLETED")
                        .valores(formEntry.getValues())
                        .build())
                .toList();

        List<FuncionarioWorkflowDtos.DocumentoResumenDTO> documents = documentUploadRepository.findByProcessInstanceId(instance.getId())
                .stream()
                .map(FuncionarioWorkflowDtos::fromDocument)
                .toList();

        return FuncionarioWorkflowDtos.TareaDetalleDTO.builder()
                .tarea(FuncionarioWorkflowDtos.fromTask(task))
                .tramite(instance)
                .formulario(form)
                .workflowGraph(definition.getGraph())
                .datosActuales(instance.getVariables())
                .borradorActual(castMap(task.getFormData()))
                .formulariosPrevios(previousForms)
                .documentos(documents)
                .activeNodeIds(instance.getActiveNodeIds())
                .canClaim(canClaim)
                .canEdit(canEdit && !blockedByOtherUser)
                .claimedByCurrentUser(claimedByCurrentUser)
                .blockedByOtherUser(blockedByOtherUser)
                .accessState(resolveAccessState(task, claimedByCurrentUser, blockedByOtherUser))
                .build();
    }

    private String resolveAccessState(Task task, boolean claimedByCurrentUser, boolean blockedByOtherUser) {
        if (blockedByOtherUser) {
            return "BLOCKED_BY_OTHER";
        }
        if ("PENDING".equals(task.getStatus()) && task.getAssignee() == null) {
            return "PENDING_CLAIM";
        }
        if ("IN_PROGRESS".equals(task.getStatus()) && claimedByCurrentUser) {
            return "IN_PROGRESS_BY_YOU";
        }
        return "AVAILABLE";
    }

    private void claimIfNeeded(Task task, User user) {
        if (task.getAssignee() == null) {
            task.setAssignee(user.getUsername());
            task.setStatus("IN_PROGRESS");
        }
        if (!user.getUsername().equals(task.getAssignee())) {
            throw new IllegalArgumentException("La tarea está bloqueada por otro funcionario");
        }
    }

    private String resolvePreviousHumanNode(ProcessInstance instance, Task currentTask) {
        if (instance.getHistory() == null) {
            return null;
        }

        return instance.getHistory().stream()
                .filter(entry -> entry.getNodeId() != null)
                .filter(entry -> !"PROCESS_STARTED".equals(entry.getStatus()))
                .filter(entry -> entry.getNodeId() != null && !entry.getNodeId().equals(currentTask.getNodeId()))
                .map(ProcessInstance.HistoryEntry::getNodeId)
                .reduce((first, second) -> second)
                .orElse(null);
    }

    private String resolveTargetCorrectionNode(
            ProcessInstance instance,
            Task currentTask,
            WorkflowDefinition definition,
            String targetNodeId
    ) {
        List<String> targets = resolvePreviousHumanNodes(instance, currentTask, definition);
        if (targets.isEmpty()) {
            return null;
        }
        if (targetNodeId == null || targetNodeId.isBlank()) {
            return targets.get(0);
        }
        String requested = targetNodeId.trim();
        if (!targets.contains(requested)) {
            throw new IllegalArgumentException("El nodo destino no es válido para corrección en este trámite");
        }
        return requested;
    }

    private List<String> resolvePreviousHumanNodes(ProcessInstance instance, Task currentTask, WorkflowDefinition definition) {
        if (instance.getHistory() == null || definition.getGraph() == null) {
            return List.of();
        }
        // Orden descendente por historial (últimos primero), únicos, solo UserTask, excluye nodo actual.
        List<String> ordered = instance.getHistory().stream()
                .map(ProcessInstance.HistoryEntry::getNodeId)
                .filter(Objects::nonNull)
                .filter(nodeId -> !nodeId.equals(currentTask.getNodeId()))
                .toList();

        List<String> targets = new ArrayList<>();
        for (int i = ordered.size() - 1; i >= 0; i--) {
            // history suele estar ordenado asc; invertimos recorriendo desde el final
            String nodeId = ordered.get(i);
            if (targets.contains(nodeId)) {
                continue;
            }
            BpmnNode node = definition.getGraph().get(nodeId);
            if (node == null) {
                continue;
            }
            if (node.getType() != null && node.getType().toLowerCase().contains("usertask")) {
                targets.add(nodeId);
            }
        }
        // targets quedó con últimos primero por el recorrido; ya sirve como lista de selección.
        return targets;
    }

    private boolean isVisibleToUser(ProcessInstance instance, User user) {
        String username = user.getUsername();
        String department = user.getDepartamento();

        boolean visibleByTask = taskRepository.findByProcessInstanceId(instance.getId()).stream().anyMatch(task ->
                Objects.equals(task.getAssignee(), username)
                        || (department != null && department.equalsIgnoreCase(task.getDepartmentAssigned()))
                        || (task.getCandidateRole() != null && user.getRoles().contains(task.getCandidateRole()))
        );

        boolean visibleByHistory = instance.getHistory() != null && instance.getHistory().stream().anyMatch(entry ->
                Objects.equals(entry.getCompletedByUserId(), user.getId())
                        || (entry.getAssignedRole() != null && user.getRoles().contains(entry.getAssignedRole()))
        );

        return visibleByTask || visibleByHistory;
    }

    private String extractCustomerName(ProcessInstance instance) {
        Object direct = instance.getVariables() != null ? instance.getVariables().get("clienteNombre") : null;
        if (direct != null) {
            return String.valueOf(direct);
        }
        return taskRepository.findByProcessInstanceId(instance.getId()).stream()
                .map(Task::getCustomerName)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse("Cliente");
    }

    private String extractCustomerDni(ProcessInstance instance) {
        Object direct = instance.getVariables() != null ? instance.getVariables().get("clienteDni") : null;
        if (direct != null) {
            return String.valueOf(direct);
        }
        return taskRepository.findByProcessInstanceId(instance.getId()).stream()
                .map(Task::getCustomerDni)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    private String resolveCurrentDepartment(ProcessInstance instance) {
        return taskRepository.findByProcessInstanceId(instance.getId()).stream()
                .filter(task -> "PENDING".equals(task.getStatus()) || "IN_PROGRESS".equals(task.getStatus()))
                .map(Task::getDepartmentAssigned)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse("Sin departamento activo");
    }

    private LocalDateTime resolveLastUpdate(ProcessInstance instance) {
        if (instance.getHistory() == null || instance.getHistory().isEmpty()) {
            return instance.getInitiatedAt();
        }
        return instance.getHistory().stream()
                .map(ProcessInstance.HistoryEntry::getTimestamp)
                .filter(Objects::nonNull)
                .max(LocalDateTime::compareTo)
                .orElse(instance.getInitiatedAt());
    }

    private LocalDateTime parseDateStart(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return LocalDate.parse(value).atStartOfDay();
    }

    private LocalDateTime parseDateEnd(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return LocalDate.parse(value).atTime(23, 59, 59);
    }

    private Map<String, Object> castMap(Object raw) {
        if (raw instanceof Map<?, ?> map) {
            return map.entrySet().stream()
                    .filter(entry -> entry.getKey() instanceof String)
                    .collect(java.util.stream.Collectors.toMap(
                            entry -> (String) entry.getKey(),
                            entry -> entry.getValue()
                    ));
        }
        return Map.of();
    }

    private boolean matchesTenant(ProcessInstance instance, String userEmpresa) {
        if (userEmpresa == null || userEmpresa.isBlank()) {
            return true;
        }
        String instanceEmpresa = instance.getTenantEmpresa();
        if (instanceEmpresa == null || instanceEmpresa.isBlank()) {
            return true;
        }
        return userEmpresa.equalsIgnoreCase(instanceEmpresa);
    }

    private boolean taskBelongsToTenant(Task task, String userEmpresa) {
        if (userEmpresa == null || userEmpresa.isBlank()) {
            return true;
        }
        ProcessInstance instance = processInstanceRepository.findById(task.getProcessInstanceId()).orElse(null);
        if (instance == null) {
            return false;
        }
        return matchesTenant(instance, userEmpresa);
    }

    private User getAuthenticatedUser() {
        return userService.getCurrentAuthenticatedUser()
                .orElseThrow(() -> new RuntimeException("No hay usuario autenticado"));
    }

    private Task loadTask(String taskId) {
        return taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Tarea no encontrada"));
    }

    private ProcessInstance loadInstance(String instanceId) {
        return processInstanceRepository.findById(instanceId)
                .orElseThrow(() -> new RuntimeException("Trámite no encontrado"));
    }

    private Policy loadPolicy(String policyId) {
        return policyRepository.findById(policyId)
                .orElseThrow(() -> new RuntimeException("Política no encontrada"));
    }

    private WorkflowDefinition loadDefinition(Policy policy) {
        return workflowDefinitionService.getDefinitionByPolicy(policy.getId(), policy.getVersion())
                .orElseThrow(() -> new RuntimeException("La política no tiene definición publicada"));
    }

    private FormDefinition resolveForm(Policy policy, String formId) {
        if (formId == null || policy.getForms() == null) {
            return null;
        }
        return policy.getForms().stream()
                .filter(form -> formId.equals(form.getId()))
                .findFirst()
                .orElse(null);
    }

    private void validateTaskAccess(Task task, User user) {
        boolean matchesDepartment = user.getDepartamento() != null
                && user.getDepartamento().equalsIgnoreCase(task.getDepartmentAssigned());
        boolean matchesRole = task.getCandidateRole() != null && user.getRoles().contains(task.getCandidateRole());
        boolean matchesAssignee = task.getAssignee() != null && user.getUsername().equals(task.getAssignee());
        if (!matchesDepartment && !matchesRole && !matchesAssignee) {
            throw new IllegalArgumentException("La tarea no pertenece al departamento del funcionario autenticado");
        }
    }

    private void broadcastTaskUpdate(Task task, String type) {
        String role = task.getCandidateRole() != null ? task.getCandidateRole() : task.getDepartmentAssigned();
        if (role != null && !role.isBlank()) {
            taskQueueWebSocketHandler.broadcastToRole(role,
                    "{\"type\":\"" + type + "\",\"taskId\":\"" + task.getId() + "\"}");
        }
    }
}
