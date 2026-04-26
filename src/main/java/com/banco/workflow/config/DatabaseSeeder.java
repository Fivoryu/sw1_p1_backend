package com.banco.workflow.config;

import com.banco.workflow.model.Department;
import com.banco.workflow.model.DepartmentDefinition;
import com.banco.workflow.model.FormDefinition;
import com.banco.workflow.model.Notification;
import com.banco.workflow.model.Policy;
import com.banco.workflow.model.ProcessInstance;
import com.banco.workflow.model.Task;
import com.banco.workflow.model.User;
import com.banco.workflow.repository.DepartmentRepository;
import com.banco.workflow.repository.NotificationRepository;
import com.banco.workflow.repository.PolicyRepository;
import com.banco.workflow.repository.ProcessInstanceRepository;
import com.banco.workflow.repository.TaskRepository;
import com.banco.workflow.repository.UserRepository;
import com.banco.workflow.service.WorkflowDefinitionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class DatabaseSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final PolicyRepository policyRepository;
    private final ProcessInstanceRepository processInstanceRepository;
    private final TaskRepository taskRepository;
    private final NotificationRepository notificationRepository;
    private final WorkflowDefinitionService workflowDefinitionService;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        log.info("=== Iniciando Database Seeder ===");
        try {
            seedDepartments();
            seedUsers();
            seedPolicies();
            seedBulkDemoData();
            log.info("📦 Totales sembrados -> users: {}, policies: {}, process_instances: {}, tasks: {}, notifications: {}",
                    userRepository.count(),
                    policyRepository.count(),
                    processInstanceRepository.count(),
                    taskRepository.count(),
                    notificationRepository.count());
            log.info("=== Database Seeder completado ===");
        } catch (Exception e) {
            log.warn("Error ejecutando Database Seeder (retomará en próximo startup): {}", e.getMessage());
        }
    }

    private void seedDepartments() {
        log.info("Sincronizando catálogo global de departamentos...");

        upsertDepartment("Caja", "ROLE_CAJA", "Recepción, validación inicial y formalización de trámites.");
        upsertDepartment("Operaciones", "ROLE_OPERACIONES", "Ejecución operativa y alta final del producto.");
        upsertDepartment("Compliance", "ROLE_COMPLIANCE", "Validación documental y cumplimiento normativo.");
        upsertDepartment("Riesgo", "ROLE_RIESGO", "Evaluación de riesgo y revisión de excepciones.");
        upsertDepartment("Préstamos Vehículo", "ROLE_PRESTAMOS_VEHICULO", "Atención especializada para créditos vehiculares.");
        upsertDepartment("Atención al Cliente", "ROLE_ATENCION_CLIENTE", "Soporte y seguimiento al cliente.");
    }

    private void seedUsers() {
        log.info("Sincronizando usuarios demo multiempresa...");

        upsertUser("admin", "admin@workflowcloud.com", "admin123",
                Set.of("ROLE_ADMIN", "ROLE_REVISOR", "ROLE_GERENTE"),
                "Administración SaaS", "Workflow Cloud");
        upsertUser("admin_operaciones", "ops@workflowcloud.com", "adminops123",
                Set.of("ROLE_ADMIN"), "Operaciones SaaS", "Workflow Cloud");

        upsertUser("admin_mutual", "admin@mutualandina.com", "mutual123",
                Set.of("ROLE_ADMIN"), "Administración", "Mutual Andina");
        upsertUser("admin_crediauto", "admin@crediauto.com", "crediauto123",
                Set.of("ROLE_ADMIN"), "Administración", "CrediAuto");
        upsertUser("admin_financiaplus", "admin@financiaplus.com", "financia123",
                Set.of("ROLE_ADMIN"), "Administración", "FinanciaPlus");

        upsertUser("revisor_cloud", "revisor@workflowcloud.com", "revisor123",
                Set.of("ROLE_REVISOR", "ROLE_COMPLIANCE"), "Compliance", "Workflow Cloud");
        upsertUser("gerente_cloud", "gerente@workflowcloud.com", "gerente123",
                Set.of("ROLE_GERENTE", "ROLE_OPERACIONES"), "Operaciones", "Workflow Cloud");
        upsertUser("cliente_cloud", "cliente@workflowcloud.com", "cliente123",
                Set.of("ROLE_CLIENTE"), "Externos", "Workflow Cloud");

        upsertUser("funcionario_mutual", "funcionario@mutualandina.com", "funcionario123",
                Set.of("ROLE_REVISOR", "ROLE_CAJA"), "Caja", "Mutual Andina");
        upsertUser("cliente_mutual", "cliente@mutualandina.com", "cliente123",
                Set.of("ROLE_CLIENTE"), "Externos", "Mutual Andina");

        upsertUser("funcionario_auto", "funcionario@crediauto.com", "funcionario123",
                Set.of("ROLE_REVISOR", "ROLE_PRESTAMOS_VEHICULO"), "Préstamos Vehículo", "CrediAuto");
        upsertUser("cliente_auto", "cliente@crediauto.com", "cliente123",
                Set.of("ROLE_CLIENTE"), "Externos", "CrediAuto");

        upsertUser("funcionario_fin", "funcionario@financiaplus.com", "funcionario123",
                Set.of("ROLE_REVISOR", "ROLE_ATENCION_CLIENTE"), "Atención al Cliente", "FinanciaPlus");
        upsertUser("cliente_fin", "cliente@financiaplus.com", "cliente123",
                Set.of("ROLE_CLIENTE"), "Externos", "FinanciaPlus");

        log.info("✅ Usuarios demo sincronizados: 5 admins, funcionarios y clientes de 4 empresas.");
    }

    private void seedPolicies() throws Exception {
        log.info("Sincronizando políticas demo con BPMN y formularios...");

        User adminUser = userRepository.findByUsername("admin")
                .orElseThrow(() -> new IllegalStateException("No se encontró el usuario admin para sembrar políticas"));

        List<DepartmentDefinition> departmentRefs = departmentRepository.findByActiveTrue().stream()
                .map(department -> new DepartmentDefinition(
                        department.getId(),
                        department.getName(),
                        department.getRole(),
                        department.getDescription()
                ))
                .toList();

        upsertPolicy(
                "Apertura de Cuenta Digital",
                "Proceso de alta de cuenta con revisión documental y formalización operativa.",
                aperturaCuentaBpmn(),
                List.of(
                        form("form_apertura_cuenta", "Formulario de apertura", "Captura inicial del cliente",
                                field("nombres", "nombres", "Nombres completos", "text", true),
                                field("documento", "documento", "Documento de identidad", "text", true),
                                field("ingresos", "ingresos", "Ingresos mensuales", "number", true),
                                selectField("tipoCuenta", "tipoCuenta", "Tipo de cuenta", true,
                                        option("Ahorros", "ahorros"),
                                        option("Corriente", "corriente"))
                        ),
                        form("form_revision_cumplimiento", "Revisión de cumplimiento", "Control documental interno",
                                selectField("resultado", "resultado", "Resultado", true,
                                        option("Observado", "observado"),
                                        option("Aprobado", "aprobado")),
                                field("observaciones", "observaciones", "Observaciones", "textarea", false)
                        )
                ),
                departmentRefs,
                adminUser.getId()
        );

        upsertPolicy(
                "Crédito Vehicular SaaS",
                "Solicitud de crédito vehicular con evaluación especializada y derivación automática.",
                creditoVehicularBpmn(),
                List.of(
                        form("form_solicitud_credito", "Solicitud de crédito", "Datos del crédito solicitado",
                                field("monto", "monto", "Monto solicitado", "number", true),
                                field("plazo", "plazo", "Plazo en meses", "number", true),
                                selectField("tipoVehiculo", "tipoVehiculo", "Tipo de vehículo", true,
                                        option("Nuevo", "nuevo"),
                                        option("Usado", "usado"))
                        ),
                        form("form_revision_riesgo", "Evaluación de riesgo", "Análisis del analista",
                                selectField("decision", "decision", "Decisión", true,
                                        option("Enviar a gerente", "gerente"),
                                        option("Aprobar en caja", "caja")),
                                field("comentarioRiesgo", "comentarioRiesgo", "Comentario", "textarea", false)
                        )
                ),
                departmentRefs,
                adminUser.getId()
        );

        upsertPolicy(
                "Actualización Documental",
                "Validación paralela de datos del cliente para actualización de expediente.",
                actualizacionDocumentalBpmn(),
                List.of(
                        form("form_actualizacion", "Actualización documental", "Carga de datos y documentos",
                                field("telefono", "telefono", "Teléfono", "text", true),
                                field("direccion", "direccion", "Dirección", "textarea", true),
                                field("fechaActualizacion", "fechaActualizacion", "Fecha de actualización", "date", true)
                        )
                ),
                departmentRefs,
                adminUser.getId()
        );
    }

    private void seedBulkDemoData() {
        log.info("Sincronizando dataset demo masivo para poblar Mongo Atlas...");

        List<SeedCompany> companies = List.of(
                new SeedCompany("cloud", "Workflow Cloud", "workflowcloud.com"),
                new SeedCompany("mutual", "Mutual Andina", "mutualandina.com"),
                new SeedCompany("auto", "CrediAuto", "crediauto.com"),
                new SeedCompany("fin", "FinanciaPlus", "financiaplus.com")
        );

        List<DepartmentSeed> departmentCatalog = List.of(
                new DepartmentSeed("Caja", "ROLE_CAJA"),
                new DepartmentSeed("Operaciones", "ROLE_OPERACIONES"),
                new DepartmentSeed("Compliance", "ROLE_COMPLIANCE"),
                new DepartmentSeed("Riesgo", "ROLE_RIESGO"),
                new DepartmentSeed("Préstamos Vehículo", "ROLE_PRESTAMOS_VEHICULO"),
                new DepartmentSeed("Atención al Cliente", "ROLE_ATENCION_CLIENTE")
        );

        List<User> clients = new ArrayList<>();
        List<User> officers = new ArrayList<>();

        for (SeedCompany company : companies) {
            for (int i = 1; i <= 25; i++) {
                DepartmentSeed department = departmentCatalog.get((i - 1) % departmentCatalog.size());
                String username = "func_" + company.code() + "_" + formatIndex(i);
                officers.add(upsertUser(
                        username,
                        username + "@" + company.domain(),
                        "demo123",
                        Set.of(department.role(), "ROLE_REVISOR"),
                        department.name(),
                        company.name()
                ));
            }

            for (int i = 1; i <= 200; i++) {
                String username = "cli_" + company.code() + "_" + formatIndex(i);
                clients.add(upsertUser(
                        username,
                        username + "@" + company.domain(),
                        "demo123",
                        Set.of("ROLE_CLIENTE"),
                        "Externos",
                        company.name()
                ));
            }
        }

        Map<String, Policy> policiesByName = new LinkedHashMap<>();
        policyRepository.findAll().forEach(policy -> policiesByName.putIfAbsent(policy.getName(), policy));
        List<Policy> policies = List.of(
                policiesByName.get("Apertura de Cuenta Digital"),
                policiesByName.get("Crédito Vehicular SaaS"),
                policiesByName.get("Actualización Documental")
        ).stream().filter(policy -> policy != null).toList();

        if (policies.isEmpty() || clients.isEmpty()) {
            log.warn("No se generó dataset masivo porque faltan políticas o clientes base");
            return;
        }

        List<Task> seededTasks = new ArrayList<>();
        List<ProcessInstance> seededInstances = new ArrayList<>();
        List<Notification> seededNotifications = new ArrayList<>();

        for (int i = 1; i <= 180; i++) {
            Policy policy = policies.get((i - 1) % policies.size());
            User client = clients.get((i - 1) % clients.size());
            DepartmentSeed department = departmentCatalog.get((i - 1) % departmentCatalog.size());
            boolean completed = i % 5 == 0;
            LocalDateTime startedAt = LocalDateTime.now().minusDays((i % 45) + 1).minusHours(i % 12);
            String processId = "seed-instance-" + formatIndex(i);
            String activeNodeId = resolveActiveNodeId(policy.getName(), completed);
            String formId = firstFormId(policy);
            String currentOfficer = officers.get((i - 1) % officers.size()).getId();

            Map<String, Object> variables = buildVariablePayload(policy.getName(), i, client);
            List<ProcessInstance.HistoryEntry> history = buildHistory(processId, policy.getName(), department.role(), currentOfficer, startedAt, completed);
            List<ProcessInstance.CompletedForm> completedForms = buildCompletedForms(formId, processId, currentOfficer, startedAt, variables);

            ProcessInstance instance = processInstanceRepository.findById(processId).orElseGet(ProcessInstance::new);
            instance.setId(processId);
            instance.setPolicyId(policy.getId());
            instance.setPolicyName(policy.getName());
            instance.setPolicyVersion(policy.getVersion());
            instance.setTemporalProcessInstanceId("temporal-" + processId);
            instance.setStatus(completed ? "COMPLETED" : "IN_PROGRESS");
            instance.setVariables(variables);
            instance.setActiveNodeIds(completed ? List.of("end") : List.of(activeNodeId));
            instance.setInitiatedByUserId(client.getId());
            instance.setInitiatedAt(startedAt);
            instance.setCompletedAt(completed ? startedAt.plusHours(6) : null);
            instance.setResult(completed ? "APROBADO" : null);
            instance.setHistory(history);
            instance.setCompletedForms(completedForms);
            seededInstances.add(instance);

            if (!completed) {
                String taskId = "seed-task-" + formatIndex(i);
                Task task = taskRepository.findById(taskId).orElseGet(Task::new);
                task.setId(taskId);
                task.setProcessInstanceId(processId);
                task.setNodeId(activeNodeId);
                task.setNodeName(resolveNodeName(policy.getName(), activeNodeId));
                task.setNodeType("UserTask");
                task.setAssignee(i % 7 == 0 ? officers.get((i - 1) % officers.size()).getUsername() : null);
                task.setCandidateRole(department.role());
                task.setDepartmentAssigned(department.name());
                task.setFormId(formId);
                task.setStatus(i % 7 == 0 ? "IN_PROGRESS" : "PENDING");
                task.setTitle(task.getNodeName());
                task.setDescription("Tarea demo generada para visualizar una bandeja poblada por departamento.");
                task.setPriority(resolvePriority(i));
                task.setCreatedAt(startedAt.plusHours(1));
                task.setDueDate(startedAt.plusDays((i % 4) + 1));
                task.setCompletedAt(null);
                task.setResult(null);
                task.setTaskType("HUMAN");
                task.setFormData(null);
                task.setVariables(variables);
                task.setRequiredDocuments(List.of("dni", "solicitud"));
                task.setNextTaskId(null);
                task.setRejectionReason(null);
                task.setCustomerName(buildCustomerName(client.getUsername()));
                task.setCustomerDni(buildCustomerDni(i));
                seededTasks.add(task);
            }
        }

        for (int i = 1; i <= 220; i++) {
            User target = i % 3 == 0 ? officers.get((i - 1) % officers.size()) : clients.get((i - 1) % clients.size());
            String notificationId = "seed-notification-" + formatIndex(i);
            Notification notification = notificationRepository.findById(notificationId).orElseGet(Notification::new);
            notification.setId(notificationId);
            notification.setUserId(target.getId());
            notification.setTitle(i % 2 == 0 ? "Actualización de trámite" : "Nueva tarea disponible");
            notification.setBody(i % 2 == 0
                    ? "Tu trámite cambió de estado y ya puedes consultar el seguimiento."
                    : "Existe una tarea demo nueva en la bandeja del departamento.");
            notification.setType(i % 2 == 0 ? "PROCESS_UPDATE" : "TASK_ASSIGNED");
            notification.setRead(i % 4 == 0);
            notification.setCreatedAt(LocalDateTime.now().minusHours(i));
            notification.setReadAt(notification.isRead() ? notification.getCreatedAt().plusMinutes(20) : null);
            notification.setRelatedId("seed-instance-" + formatIndex(((i - 1) % 180) + 1));
            notification.setMetadata("{\"source\":\"seeder\",\"index\":" + i + "}");
            notification.setPriority(i % 5 == 0 ? "HIGH" : "NORMAL");
            notification.setActionUrl(i % 2 == 0 ? "/mobile/processes" : "/operator/tasks");
            seededNotifications.add(notification);
        }

        processInstanceRepository.saveAll(seededInstances);
        taskRepository.saveAll(seededTasks);
        notificationRepository.saveAll(seededNotifications);

        log.info("✅ Dataset masivo sincronizado: {} usuarios demo, {} instancias, {} tareas, {} notificaciones.",
                userRepository.count(),
                seededInstances.size(),
                seededTasks.size(),
                seededNotifications.size());
    }

    private void upsertDepartment(String name, String role, String description) {
        Department department = departmentRepository.findByRole(role).orElseGet(Department::new);
        department.setName(name);
        department.setRole(role);
        department.setDescription(description);
        department.setActive(true);
        departmentRepository.save(department);
    }

    private User upsertUser(String username, String email, String password, Set<String> roles,
                            String departamento, String empresa) {
        Optional<User> existing = userRepository.findByUsername(username);
        LocalDateTime now = LocalDateTime.now();

        User user = existing.orElseGet(User::new);
        if (user.getCreatedAt() == null) {
            user.setCreatedAt(now);
        }

        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setRoles(roles);
        user.setDepartamento(departamento);
        user.setEmpresa(empresa);
        user.setUpdatedAt(now);
        user.setActive(true);

        return userRepository.save(user);
    }

    private void upsertPolicy(String name, String description, String bpmnXml, List<FormDefinition> forms,
                              List<DepartmentDefinition> departments, String createdByUserId) throws Exception {
        LocalDateTime now = LocalDateTime.now();
        Policy policy = policyRepository.findByNameAndVersion(name, 1).orElseGet(Policy::new);

        if (policy.getCreatedAt() == null) {
            policy.setCreatedAt(now);
            policy.setVersion(1);
            policy.setActive(true);
        }

        policy.setName(name);
        policy.setDescription(description);
        policy.setBpmnXml(bpmnXml);
        policy.setStatus("PUBLISHED");
        policy.setForms(forms);
        policy.setDepartments(departments);
        policy.setCreatedByUserId(createdByUserId);
        policy.setOwnerUserId(createdByUserId);
        policy.setTenantEmpresa("Workflow Cloud");
        policy.setCollaborationEnabled(false);
        policy.setCollaborationMode("PRIVATE");
        policy.setLastEditedByUserId(createdByUserId);
        policy.setLastAutoSavedAt(now);
        policy.setUpdatedAt(now);

        policy = policyRepository.save(policy);
        var definition = workflowDefinitionService.compileAndPublish(policy);
        policy.setGraph(definition.getGraph());
        policyRepository.save(policy);
    }

    private FormDefinition form(String id, String name, String description, FormDefinition.FormFieldDefinition... fields) {
        return new FormDefinition(id, name, description, List.of(fields));
    }

    private FormDefinition.FormFieldDefinition field(String id, String name, String label, String type, boolean required) {
        return new FormDefinition.FormFieldDefinition(id, name, label, type, required, List.of());
    }

    private FormDefinition.FormFieldDefinition selectField(String id, String name, String label, boolean required,
                                                           FormDefinition.FormFieldOption... options) {
        return new FormDefinition.FormFieldDefinition(id, name, label, "select", required, List.of(options));
    }

    private FormDefinition.FormFieldOption option(String label, String value) {
        return new FormDefinition.FormFieldOption(label, value);
    }

    private String formatIndex(int index) {
        return String.format("%03d", index);
    }

    private String firstFormId(Policy policy) {
        return policy.getForms() != null && !policy.getForms().isEmpty() ? policy.getForms().get(0).getId() : null;
    }

    private String resolveActiveNodeId(String policyName, boolean completed) {
        if (completed) {
            return "end";
        }
        return switch (policyName) {
            case "Apertura de Cuenta Digital" -> "Task_RevisionCumplimiento";
            case "Crédito Vehicular SaaS" -> "Task_EvaluarRiesgo";
            default -> "Task_RecibirActualizacion";
        };
    }

    private String resolveNodeName(String policyName, String nodeId) {
        return switch (nodeId) {
            case "Task_RevisionCumplimiento" -> "Revisar cumplimiento";
            case "Task_EvaluarRiesgo" -> "Evaluar riesgo";
            case "Task_RecibirActualizacion" -> "Recibir actualización";
            default -> policyName;
        };
    }

    private String resolvePriority(int index) {
        if (index % 11 == 0) {
            return "HIGH";
        }
        if (index % 5 == 0) {
            return "MEDIUM";
        }
        return "NORMAL";
    }

    private Map<String, Object> buildVariablePayload(String policyName, int index, User client) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("clienteUsername", client.getUsername());
        payload.put("clienteEmpresa", client.getEmpresa());
        payload.put("monto", 3000 + (index * 175));
        payload.put("plazo", 12 + (index % 36));
        payload.put("score", 550 + (index % 220));
        payload.put("canal", index % 2 == 0 ? "WEB" : "MOBILE");
        payload.put("estadoDocumental", index % 4 == 0 ? "OBSERVADO" : "VALIDADO");
        payload.put("tipoProceso", policyName);
        return payload;
    }

    private List<ProcessInstance.HistoryEntry> buildHistory(String processId, String policyName, String role,
                                                            String userId, LocalDateTime startedAt, boolean completed) {
        List<ProcessInstance.HistoryEntry> history = new ArrayList<>();
        history.add(ProcessInstance.HistoryEntry.builder()
                .id(processId + "-hist-1")
                .processInstanceId(processId)
                .nodeId("start")
                .nodeType("StartEvent")
                .nodeName("Inicio")
                .timestamp(startedAt)
                .status("COMPLETED")
                .build());

        history.add(ProcessInstance.HistoryEntry.builder()
                .id(processId + "-hist-2")
                .processInstanceId(processId)
                .nodeId("task-intake")
                .nodeType("UserTask")
                .nodeName(policyName)
                .taskId(processId + "-task-intake")
                .taskName("Registro inicial")
                .assignedRole(role)
                .completedByUserId(userId)
                .timestamp(startedAt.plusMinutes(40))
                .status("COMPLETED")
                .build());

        if (completed) {
            history.add(ProcessInstance.HistoryEntry.builder()
                    .id(processId + "-hist-3")
                    .processInstanceId(processId)
                    .nodeId("end")
                    .nodeType("EndEvent")
                    .nodeName("Fin")
                    .timestamp(startedAt.plusHours(6))
                    .status("COMPLETED")
                    .build());
        }

        return history;
    }

    private List<ProcessInstance.CompletedForm> buildCompletedForms(String formId, String processId,
                                                                    String userId, LocalDateTime startedAt,
                                                                    Map<String, Object> variables) {
        if (formId == null) {
            return List.of();
        }
        return List.of(new ProcessInstance.CompletedForm(
                formId,
                processId + "-task-intake",
                "task-intake",
                userId,
                startedAt.plusMinutes(40),
                Map.of(
                        "monto", variables.get("monto"),
                        "plazo", variables.get("plazo"),
                        "canal", variables.get("canal")
                )
        ));
    }

    private String buildCustomerName(String username) {
        String normalized = username.replace("cli_", "Cliente ").replace('_', ' ');
        return normalized.substring(0, 1).toUpperCase() + normalized.substring(1);
    }

    private String buildCustomerDni(int index) {
        return String.format("%08d", 10000000 + index);
    }

    private record SeedCompany(String code, String name, String domain) {
    }

    private record DepartmentSeed(String name, String role) {
    }

    private String aperturaCuentaBpmn() {
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <bpmn:definitions xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL"
                                  xmlns:bpmndi="http://www.omg.org/spec/BPMN/20100524/DI"
                                  xmlns:dc="http://www.omg.org/spec/DD/20100524/DC"
                                  xmlns:di="http://www.omg.org/spec/DD/20100524/DI"
                                  id="Definitions_Apertura"
                                  targetNamespace="http://bpmn.io/schema/bpmn">
                  <bpmn:process id="Process_AperturaCuenta" isExecutable="true">
                    <bpmn:startEvent id="StartEvent_Apertura" name="Inicio"/>
                    <bpmn:userTask id="Task_RecepcionarSolicitud" name="Recepcionar solicitud" data-role="ROLE_CAJA" data-form-id="form_apertura_cuenta"/>
                    <bpmn:exclusiveGateway id="Gateway_Revision" name="¿Documentación completa?" default="Flow_Observado"/>
                    <bpmn:userTask id="Task_RevisionCumplimiento" name="Revisar cumplimiento" data-role="ROLE_COMPLIANCE" data-form-id="form_revision_cumplimiento"/>
                    <bpmn:userTask id="Task_FormalizarCuenta" name="Formalizar cuenta" data-role="ROLE_OPERACIONES"/>
                    <bpmn:endEvent id="EndEvent_Apertura" name="Cuenta creada"/>
                    <bpmn:sequenceFlow id="Flow_1" sourceRef="StartEvent_Apertura" targetRef="Task_RecepcionarSolicitud"/>
                    <bpmn:sequenceFlow id="Flow_2" sourceRef="Task_RecepcionarSolicitud" targetRef="Gateway_Revision"/>
                    <bpmn:sequenceFlow id="Flow_Aprobado" sourceRef="Gateway_Revision" targetRef="Task_RevisionCumplimiento">
                      <bpmn:conditionExpression xsi:type="bpmn:tFormalExpression" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">${resultado == 'aprobado'}</bpmn:conditionExpression>
                    </bpmn:sequenceFlow>
                    <bpmn:sequenceFlow id="Flow_Observado" sourceRef="Gateway_Revision" targetRef="Task_FormalizarCuenta"/>
                    <bpmn:sequenceFlow id="Flow_4" sourceRef="Task_RevisionCumplimiento" targetRef="Task_FormalizarCuenta"/>
                    <bpmn:sequenceFlow id="Flow_5" sourceRef="Task_FormalizarCuenta" targetRef="EndEvent_Apertura"/>
                  </bpmn:process>
                  <bpmndi:BPMNDiagram id="BPMNDiagram_Apertura">
                    <bpmndi:BPMNPlane id="BPMNPlane_Apertura" bpmnElement="Process_AperturaCuenta">
                      <bpmndi:BPMNShape id="Shape_Start_Apertura" bpmnElement="StartEvent_Apertura">
                        <dc:Bounds x="120" y="180" width="36" height="36"/>
                      </bpmndi:BPMNShape>
                      <bpmndi:BPMNShape id="Shape_Task_RecepcionarSolicitud" bpmnElement="Task_RecepcionarSolicitud">
                        <dc:Bounds x="210" y="158" width="160" height="80"/>
                      </bpmndi:BPMNShape>
                      <bpmndi:BPMNShape id="Shape_Gateway_Revision" bpmnElement="Gateway_Revision" isMarkerVisible="true">
                        <dc:Bounds x="430" y="173" width="50" height="50"/>
                      </bpmndi:BPMNShape>
                      <bpmndi:BPMNShape id="Shape_Task_RevisionCumplimiento" bpmnElement="Task_RevisionCumplimiento">
                        <dc:Bounds x="540" y="80" width="170" height="80"/>
                      </bpmndi:BPMNShape>
                      <bpmndi:BPMNShape id="Shape_Task_FormalizarCuenta" bpmnElement="Task_FormalizarCuenta">
                        <dc:Bounds x="540" y="245" width="170" height="80"/>
                      </bpmndi:BPMNShape>
                      <bpmndi:BPMNShape id="Shape_End_Apertura" bpmnElement="EndEvent_Apertura">
                        <dc:Bounds x="790" y="265" width="36" height="36"/>
                      </bpmndi:BPMNShape>
                      <bpmndi:BPMNEdge id="Edge_Flow_1" bpmnElement="Flow_1">
                        <di:waypoint x="156" y="198"/>
                        <di:waypoint x="210" y="198"/>
                      </bpmndi:BPMNEdge>
                      <bpmndi:BPMNEdge id="Edge_Flow_2" bpmnElement="Flow_2">
                        <di:waypoint x="370" y="198"/>
                        <di:waypoint x="430" y="198"/>
                      </bpmndi:BPMNEdge>
                      <bpmndi:BPMNEdge id="Edge_Flow_Aprobado" bpmnElement="Flow_Aprobado">
                        <di:waypoint x="455" y="173"/>
                        <di:waypoint x="455" y="120"/>
                        <di:waypoint x="540" y="120"/>
                      </bpmndi:BPMNEdge>
                      <bpmndi:BPMNEdge id="Edge_Flow_Observado" bpmnElement="Flow_Observado">
                        <di:waypoint x="455" y="223"/>
                        <di:waypoint x="455" y="285"/>
                        <di:waypoint x="540" y="285"/>
                      </bpmndi:BPMNEdge>
                      <bpmndi:BPMNEdge id="Edge_Flow_4" bpmnElement="Flow_4">
                        <di:waypoint x="710" y="120"/>
                        <di:waypoint x="750" y="120"/>
                        <di:waypoint x="750" y="285"/>
                        <di:waypoint x="710" y="285"/>
                      </bpmndi:BPMNEdge>
                      <bpmndi:BPMNEdge id="Edge_Flow_5" bpmnElement="Flow_5">
                        <di:waypoint x="710" y="285"/>
                        <di:waypoint x="790" y="283"/>
                      </bpmndi:BPMNEdge>
                    </bpmndi:BPMNPlane>
                  </bpmndi:BPMNDiagram>
                </bpmn:definitions>
                """;
    }

    private String creditoVehicularBpmn() {
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <bpmn:definitions xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL"
                                  xmlns:bpmndi="http://www.omg.org/spec/BPMN/20100524/DI"
                                  xmlns:dc="http://www.omg.org/spec/DD/20100524/DC"
                                  xmlns:di="http://www.omg.org/spec/DD/20100524/DI"
                                  id="Definitions_Credito"
                                  targetNamespace="http://bpmn.io/schema/bpmn">
                  <bpmn:process id="Process_CreditoVehicular" isExecutable="true">
                    <bpmn:startEvent id="StartEvent_Credito" name="Inicio"/>
                    <bpmn:userTask id="Task_RegistrarSolicitud" name="Registrar solicitud" data-role="ROLE_PRESTAMOS_VEHICULO" data-form-id="form_solicitud_credito"/>
                    <bpmn:userTask id="Task_EvaluarRiesgo" name="Evaluar riesgo" data-role="ROLE_RIESGO" data-form-id="form_revision_riesgo"/>
                    <bpmn:exclusiveGateway id="Gateway_Decision" name="Ruta de aprobación" default="Flow_A_Caja"/>
                    <bpmn:userTask id="Task_Gerencia" name="Aprobación gerencial" data-role="ROLE_GERENTE"/>
                    <bpmn:userTask id="Task_Desembolso" name="Desembolso y firma" data-role="ROLE_CAJA"/>
                    <bpmn:endEvent id="EndEvent_Credito" name="Crédito finalizado"/>
                    <bpmn:sequenceFlow id="Credito_Flow_1" sourceRef="StartEvent_Credito" targetRef="Task_RegistrarSolicitud"/>
                    <bpmn:sequenceFlow id="Credito_Flow_2" sourceRef="Task_RegistrarSolicitud" targetRef="Task_EvaluarRiesgo"/>
                    <bpmn:sequenceFlow id="Credito_Flow_3" sourceRef="Task_EvaluarRiesgo" targetRef="Gateway_Decision"/>
                    <bpmn:sequenceFlow id="Flow_A_Gerencia" sourceRef="Gateway_Decision" targetRef="Task_Gerencia">
                      <bpmn:conditionExpression xsi:type="bpmn:tFormalExpression" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">${decision == 'gerente'}</bpmn:conditionExpression>
                    </bpmn:sequenceFlow>
                    <bpmn:sequenceFlow id="Flow_A_Caja" sourceRef="Gateway_Decision" targetRef="Task_Desembolso"/>
                    <bpmn:sequenceFlow id="Credito_Flow_4" sourceRef="Task_Gerencia" targetRef="Task_Desembolso"/>
                    <bpmn:sequenceFlow id="Credito_Flow_5" sourceRef="Task_Desembolso" targetRef="EndEvent_Credito"/>
                  </bpmn:process>
                  <bpmndi:BPMNDiagram id="BPMNDiagram_Credito">
                    <bpmndi:BPMNPlane id="BPMNPlane_Credito" bpmnElement="Process_CreditoVehicular">
                      <bpmndi:BPMNShape id="Shape_Start_Credito" bpmnElement="StartEvent_Credito">
                        <dc:Bounds x="100" y="160" width="36" height="36"/>
                      </bpmndi:BPMNShape>
                      <bpmndi:BPMNShape id="Shape_Task_RegistrarSolicitud" bpmnElement="Task_RegistrarSolicitud">
                        <dc:Bounds x="190" y="138" width="160" height="80"/>
                      </bpmndi:BPMNShape>
                      <bpmndi:BPMNShape id="Shape_Task_EvaluarRiesgo" bpmnElement="Task_EvaluarRiesgo">
                        <dc:Bounds x="400" y="138" width="160" height="80"/>
                      </bpmndi:BPMNShape>
                      <bpmndi:BPMNShape id="Shape_Gateway_Decision" bpmnElement="Gateway_Decision" isMarkerVisible="true">
                        <dc:Bounds x="620" y="153" width="50" height="50"/>
                      </bpmndi:BPMNShape>
                      <bpmndi:BPMNShape id="Shape_Task_Gerencia" bpmnElement="Task_Gerencia">
                        <dc:Bounds x="730" y="60" width="170" height="80"/>
                      </bpmndi:BPMNShape>
                      <bpmndi:BPMNShape id="Shape_Task_Desembolso" bpmnElement="Task_Desembolso">
                        <dc:Bounds x="730" y="220" width="170" height="80"/>
                      </bpmndi:BPMNShape>
                      <bpmndi:BPMNShape id="Shape_End_Credito" bpmnElement="EndEvent_Credito">
                        <dc:Bounds x="970" y="242" width="36" height="36"/>
                      </bpmndi:BPMNShape>
                      <bpmndi:BPMNEdge id="Edge_Credito_1" bpmnElement="Credito_Flow_1">
                        <di:waypoint x="136" y="178"/>
                        <di:waypoint x="190" y="178"/>
                      </bpmndi:BPMNEdge>
                      <bpmndi:BPMNEdge id="Edge_Credito_2" bpmnElement="Credito_Flow_2">
                        <di:waypoint x="350" y="178"/>
                        <di:waypoint x="400" y="178"/>
                      </bpmndi:BPMNEdge>
                      <bpmndi:BPMNEdge id="Edge_Credito_3" bpmnElement="Credito_Flow_3">
                        <di:waypoint x="560" y="178"/>
                        <di:waypoint x="620" y="178"/>
                      </bpmndi:BPMNEdge>
                      <bpmndi:BPMNEdge id="Edge_Gerencia" bpmnElement="Flow_A_Gerencia">
                        <di:waypoint x="645" y="153"/>
                        <di:waypoint x="645" y="100"/>
                        <di:waypoint x="730" y="100"/>
                      </bpmndi:BPMNEdge>
                      <bpmndi:BPMNEdge id="Edge_Caja" bpmnElement="Flow_A_Caja">
                        <di:waypoint x="645" y="203"/>
                        <di:waypoint x="645" y="260"/>
                        <di:waypoint x="730" y="260"/>
                      </bpmndi:BPMNEdge>
                      <bpmndi:BPMNEdge id="Edge_Credito_4" bpmnElement="Credito_Flow_4">
                        <di:waypoint x="900" y="100"/>
                        <di:waypoint x="935" y="100"/>
                        <di:waypoint x="935" y="260"/>
                        <di:waypoint x="900" y="260"/>
                      </bpmndi:BPMNEdge>
                      <bpmndi:BPMNEdge id="Edge_Credito_5" bpmnElement="Credito_Flow_5">
                        <di:waypoint x="900" y="260"/>
                        <di:waypoint x="970" y="260"/>
                      </bpmndi:BPMNEdge>
                    </bpmndi:BPMNPlane>
                  </bpmndi:BPMNDiagram>
                </bpmn:definitions>
                """;
    }

    private String actualizacionDocumentalBpmn() {
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <bpmn:definitions xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL"
                                  xmlns:bpmndi="http://www.omg.org/spec/BPMN/20100524/DI"
                                  xmlns:dc="http://www.omg.org/spec/DD/20100524/DC"
                                  xmlns:di="http://www.omg.org/spec/DD/20100524/DI"
                                  id="Definitions_Actualizacion"
                                  targetNamespace="http://bpmn.io/schema/bpmn">
                  <bpmn:process id="Process_Actualizacion" isExecutable="true">
                    <bpmn:startEvent id="StartEvent_Actualizacion" name="Inicio"/>
                    <bpmn:userTask id="Task_RecibirActualizacion" name="Recibir actualización" data-role="ROLE_ATENCION_CLIENTE" data-form-id="form_actualizacion"/>
                    <bpmn:parallelGateway id="Gateway_Split" name="Validaciones en paralelo"/>
                    <bpmn:userTask id="Task_ValidarIdentidad" name="Validar identidad" data-role="ROLE_COMPLIANCE"/>
                    <bpmn:userTask id="Task_ValidarDireccion" name="Validar dirección" data-role="ROLE_OPERACIONES"/>
                    <bpmn:parallelGateway id="Gateway_Join" name="Consolidar validación"/>
                    <bpmn:userTask id="Task_CerrarActualizacion" name="Cerrar actualización" data-role="ROLE_ATENCION_CLIENTE"/>
                    <bpmn:endEvent id="EndEvent_Actualizacion" name="Expediente actualizado"/>
                    <bpmn:sequenceFlow id="Act_Flow_1" sourceRef="StartEvent_Actualizacion" targetRef="Task_RecibirActualizacion"/>
                    <bpmn:sequenceFlow id="Act_Flow_2" sourceRef="Task_RecibirActualizacion" targetRef="Gateway_Split"/>
                    <bpmn:sequenceFlow id="Act_Flow_3" sourceRef="Gateway_Split" targetRef="Task_ValidarIdentidad"/>
                    <bpmn:sequenceFlow id="Act_Flow_4" sourceRef="Gateway_Split" targetRef="Task_ValidarDireccion"/>
                    <bpmn:sequenceFlow id="Act_Flow_5" sourceRef="Task_ValidarIdentidad" targetRef="Gateway_Join"/>
                    <bpmn:sequenceFlow id="Act_Flow_6" sourceRef="Task_ValidarDireccion" targetRef="Gateway_Join"/>
                    <bpmn:sequenceFlow id="Act_Flow_7" sourceRef="Gateway_Join" targetRef="Task_CerrarActualizacion"/>
                    <bpmn:sequenceFlow id="Act_Flow_8" sourceRef="Task_CerrarActualizacion" targetRef="EndEvent_Actualizacion"/>
                  </bpmn:process>
                  <bpmndi:BPMNDiagram id="BPMNDiagram_Actualizacion">
                    <bpmndi:BPMNPlane id="BPMNPlane_Actualizacion" bpmnElement="Process_Actualizacion">
                      <bpmndi:BPMNShape id="Shape_Start_Actualizacion" bpmnElement="StartEvent_Actualizacion">
                        <dc:Bounds x="90" y="190" width="36" height="36"/>
                      </bpmndi:BPMNShape>
                      <bpmndi:BPMNShape id="Shape_Task_RecibirActualizacion" bpmnElement="Task_RecibirActualizacion">
                        <dc:Bounds x="170" y="168" width="170" height="80"/>
                      </bpmndi:BPMNShape>
                      <bpmndi:BPMNShape id="Shape_Gateway_Split" bpmnElement="Gateway_Split">
                        <dc:Bounds x="390" y="183" width="50" height="50"/>
                      </bpmndi:BPMNShape>
                      <bpmndi:BPMNShape id="Shape_Task_ValidarIdentidad" bpmnElement="Task_ValidarIdentidad">
                        <dc:Bounds x="500" y="90" width="170" height="80"/>
                      </bpmndi:BPMNShape>
                      <bpmndi:BPMNShape id="Shape_Task_ValidarDireccion" bpmnElement="Task_ValidarDireccion">
                        <dc:Bounds x="500" y="260" width="170" height="80"/>
                      </bpmndi:BPMNShape>
                      <bpmndi:BPMNShape id="Shape_Gateway_Join" bpmnElement="Gateway_Join">
                        <dc:Bounds x="730" y="183" width="50" height="50"/>
                      </bpmndi:BPMNShape>
                      <bpmndi:BPMNShape id="Shape_Task_CerrarActualizacion" bpmnElement="Task_CerrarActualizacion">
                        <dc:Bounds x="840" y="168" width="170" height="80"/>
                      </bpmndi:BPMNShape>
                      <bpmndi:BPMNShape id="Shape_End_Actualizacion" bpmnElement="EndEvent_Actualizacion">
                        <dc:Bounds x="1080" y="190" width="36" height="36"/>
                      </bpmndi:BPMNShape>
                      <bpmndi:BPMNEdge id="Edge_Act_1" bpmnElement="Act_Flow_1">
                        <di:waypoint x="126" y="208"/>
                        <di:waypoint x="170" y="208"/>
                      </bpmndi:BPMNEdge>
                      <bpmndi:BPMNEdge id="Edge_Act_2" bpmnElement="Act_Flow_2">
                        <di:waypoint x="340" y="208"/>
                        <di:waypoint x="390" y="208"/>
                      </bpmndi:BPMNEdge>
                      <bpmndi:BPMNEdge id="Edge_Act_3" bpmnElement="Act_Flow_3">
                        <di:waypoint x="415" y="183"/>
                        <di:waypoint x="415" y="130"/>
                        <di:waypoint x="500" y="130"/>
                      </bpmndi:BPMNEdge>
                      <bpmndi:BPMNEdge id="Edge_Act_4" bpmnElement="Act_Flow_4">
                        <di:waypoint x="415" y="233"/>
                        <di:waypoint x="415" y="300"/>
                        <di:waypoint x="500" y="300"/>
                      </bpmndi:BPMNEdge>
                      <bpmndi:BPMNEdge id="Edge_Act_5" bpmnElement="Act_Flow_5">
                        <di:waypoint x="670" y="130"/>
                        <di:waypoint x="755" y="130"/>
                        <di:waypoint x="755" y="183"/>
                      </bpmndi:BPMNEdge>
                      <bpmndi:BPMNEdge id="Edge_Act_6" bpmnElement="Act_Flow_6">
                        <di:waypoint x="670" y="300"/>
                        <di:waypoint x="755" y="300"/>
                        <di:waypoint x="755" y="233"/>
                      </bpmndi:BPMNEdge>
                      <bpmndi:BPMNEdge id="Edge_Act_7" bpmnElement="Act_Flow_7">
                        <di:waypoint x="780" y="208"/>
                        <di:waypoint x="840" y="208"/>
                      </bpmndi:BPMNEdge>
                      <bpmndi:BPMNEdge id="Edge_Act_8" bpmnElement="Act_Flow_8">
                        <di:waypoint x="1010" y="208"/>
                        <di:waypoint x="1080" y="208"/>
                      </bpmndi:BPMNEdge>
                    </bpmndi:BPMNPlane>
                  </bpmndi:BPMNDiagram>
                </bpmn:definitions>
                """;
    }
}
