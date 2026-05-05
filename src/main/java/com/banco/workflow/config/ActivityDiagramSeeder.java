package com.banco.workflow.config;

import com.banco.workflow.model.DepartmentDefinition;
import com.banco.workflow.model.FormDefinition;
import com.banco.workflow.model.Policy;
import com.banco.workflow.model.User;
import com.banco.workflow.repository.DepartmentRepository;
import com.banco.workflow.repository.PolicyRepository;
import com.banco.workflow.repository.UserRepository;
import com.banco.workflow.service.WorkflowDefinitionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@Order(2)
@RequiredArgsConstructor
@Slf4j
public class ActivityDiagramSeeder implements CommandLineRunner {

    private static final String POLICY_NAME = "Demo UML 2.5 Actividad Completa";

    private final PolicyRepository policyRepository;
    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;
    private final WorkflowDefinitionService workflowDefinitionService;

    @Override
    public void run(String... args) {
        try {
            User admin = userRepository.findByUsername("admin")
                    .orElseThrow(() -> new IllegalStateException("No existe usuario admin para ActivityDiagramSeeder"));
            List<DepartmentDefinition> departments = departmentRepository.findByActiveTrue().stream()
                    .map(department -> new DepartmentDefinition(
                            department.getId(),
                            department.getName(),
                            department.getRole(),
                            department.getDescription()
                    ))
                    .toList();

            LocalDateTime now = LocalDateTime.now();
            Policy policy = policyRepository.findByNameAndVersion(POLICY_NAME, 1).orElseGet(Policy::new);
            if (policy.getCreatedAt() == null) {
                policy.setCreatedAt(now);
                policy.setVersion(1);
                policy.setActive(true);
            }

            policy.setName(POLICY_NAME);
            policy.setDescription("Política demo para validar un ActivityDiagram UML 2.5 completo con calles horizontales.");
            policy.setBpmnXml(activityDiagramTechnicalBpmn());
            policy.setUmlActivityJson(activityDiagramJson());
            policy.setUmlVersion("2.5");
            policy.setDiagramNotation("BPMN_EXECUTABLE_WITH_UML_ACTIVITY_VIEW");
            policy.setStatus("PUBLISHED");
            policy.setForms(forms());
            policy.setDepartments(departments);
            policy.setCreatedByUserId(admin.getId());
            policy.setOwnerUserId(admin.getId());
            policy.setTenantEmpresa("Workflow Cloud");
            policy.setCollaborationEnabled(false);
            policy.setCollaborationMode("PRIVATE");
            policy.setLastEditedByUserId(admin.getId());
            policy.setLastAutoSavedAt(now);
            policy.setUpdatedAt(now);

            policy = policyRepository.save(policy);
            var definition = workflowDefinitionService.compileAndPublish(policy);
            policy.setGraph(definition.getGraph());
            policyRepository.save(policy);
            log.info("✅ ActivityDiagram UML 2.5 demo sincronizado: {}", POLICY_NAME);
        } catch (Exception e) {
            log.warn("No se pudo sincronizar ActivityDiagramSeeder: {}", e.getMessage());
        }
    }

    private Map<String, Object> activityDiagramJson() {
        return Map.of(
                "partitions", List.of(
                        partition("partition_cliente", "Cliente"),
                        partition("partition_caja", "Caja"),
                        partition("partition_compliance", "Compliance"),
                        partition("partition_riesgo", "Riesgo"),
                        partition("partition_operaciones", "Operaciones")
                ),
                "nodes", List.of(
                        node("uml_start", "INITIAL", "Inicio", "partition_cliente", "InitialNode", 190, 58),
                        node("uml_recepcionar", "ACTION", "Recepcionar solicitud", "partition_caja", "Action", 330, 198),
                        node("uml_documento", "OBJECT_NODE", "Formulario y documentos", "partition_caja", "ObjectNode", 545, 190),
                        node("uml_decision_docs", "DECISION", "¿Documentación completa?", "partition_caja", "DecisionNode", 760, 188),
                        node("uml_solicitar_correccion", "SEND_SIGNAL", "Solicitar corrección", "partition_cliente", "SendSignalAction", 965, 48),
                        node("uml_recibir_correccion", "ACCEPT_SIGNAL", "Recibir subsanación", "partition_cliente", "AcceptEventAction", 1160, 48),
                        node("uml_formalizar", "ACTION", "Formalizar cuenta", "partition_caja", "Action", 965, 198),
                        node("uml_merge_docs", "MERGE", "Unir revisión documental", "partition_caja", "MergeNode", 1170, 188),
                        node("uml_fork", "FORK", "Dividir validaciones", "partition_caja", "ForkNode", 1340, 168),
                        node("uml_revisar_cumplimiento", "ACTION", "Revisar cumplimiento", "partition_compliance", "Action", 1485, 348),
                        node("uml_evaluar_riesgo", "ACTION", "Evaluar riesgo", "partition_riesgo", "Action", 1485, 498),
                        node("uml_join", "JOIN", "Sincronizar validaciones", "partition_operaciones", "JoinNode", 1725, 620),
                        node("uml_alta_producto", "ACTION", "Dar alta al producto", "partition_operaciones", "Action", 1870, 648),
                        node("uml_notificar", "SEND_SIGNAL", "Notificar activación", "partition_cliente", "SendSignalAction", 2090, 48),
                        node("uml_note", "NOTE", "Validar KYC antes del alta operativa", "partition_compliance", "Comment", 1850, 342),
                        node("uml_final", "ACTIVITY_FINAL", "Fin", "partition_cliente", "ActivityFinalNode", 2290, 68)
                ),
                "edges", List.of(
                        edge("edge_1", "uml_start", "uml_recepcionar", "ControlFlow", null),
                        edge("edge_2", "uml_recepcionar", "uml_documento", "ObjectFlow", null),
                        edge("edge_3", "uml_documento", "uml_decision_docs", "ControlFlow", null),
                        edge("edge_4", "uml_decision_docs", "uml_solicitar_correccion", "ControlFlow", "[documentación incompleta]"),
                        edge("edge_5", "uml_solicitar_correccion", "uml_recibir_correccion", "ControlFlow", null),
                        edge("edge_6", "uml_recibir_correccion", "uml_merge_docs", "ControlFlow", null),
                        edge("edge_7", "uml_decision_docs", "uml_formalizar", "ControlFlow", "[documentación completa]"),
                        edge("edge_8", "uml_formalizar", "uml_merge_docs", "ControlFlow", null),
                        edge("edge_9", "uml_merge_docs", "uml_fork", "ControlFlow", null),
                        edge("edge_10", "uml_fork", "uml_revisar_cumplimiento", "ControlFlow", null),
                        edge("edge_11", "uml_fork", "uml_evaluar_riesgo", "ControlFlow", null),
                        edge("edge_12", "uml_revisar_cumplimiento", "uml_join", "ControlFlow", null),
                        edge("edge_13", "uml_evaluar_riesgo", "uml_join", "ControlFlow", null),
                        edge("edge_14", "uml_join", "uml_alta_producto", "ControlFlow", null),
                        edge("edge_15", "uml_alta_producto", "uml_notificar", "ControlFlow", null),
                        edge("edge_16", "uml_notificar", "uml_final", "ControlFlow", null),
                        edge("edge_17", "uml_revisar_cumplimiento", "uml_note", "Annotation", null)
                ),
                "metadata", Map.of(
                        "umlVersion", "2.5",
                        "diagramType", "ActivityDiagram",
                        "partitionElement", "ActivityPartition",
                        "orientation", "horizontal",
                        "canonical", true,
                        "seed", "ActivityDiagramSeeder"
                )
        );
    }

    private Map<String, Object> partition(String id, String name) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", id);
        map.put("name", name);
        map.put("umlElement", "ActivityPartition");
        return map;
    }

    private Map<String, Object> node(String id, String type, String label, String partition, String umlElement, int x, int y) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", id);
        map.put("type", type);
        map.put("label", label);
        map.put("partition", partition);
        map.put("umlElement", umlElement);
        map.put("x", x);
        map.put("y", y);
        return map;
    }

    private Map<String, Object> edge(String id, String source, String target, String type, String guard) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", id);
        map.put("source", source);
        map.put("target", target);
        map.put("type", type);
        if (guard != null) {
            map.put("guard", guard);
        }
        return map;
    }

    private List<FormDefinition> forms() {
        return List.of(new FormDefinition(
                "form_demo_uml_actividad",
                "Formulario demo UML",
                "Datos mínimos para probar la política UML 2.5.",
                List.of(
                        new FormDefinition.FormFieldDefinition("documento", "documento", "Documento de identidad", "text", true, List.of()),
                        new FormDefinition.FormFieldDefinition("observaciones", "observaciones", "Observaciones", "textarea", false, List.of())
                )
        ));
    }

    private String activityDiagramTechnicalBpmn() {
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <bpmn:definitions xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL"
                                  xmlns:bpmndi="http://www.omg.org/spec/BPMN/20100524/DI"
                                  xmlns:dc="http://www.omg.org/spec/DD/20100524/DC"
                                  xmlns:di="http://www.omg.org/spec/DD/20100524/DI"
                                  id="Definitions_UmlActivitySeed"
                                  targetNamespace="https://workflow-cloud.local/uml-activity-seed">
                  <bpmn:process id="Process_UmlActivitySeed" name="Demo UML 2.5 Actividad Completa" isExecutable="true">
                    <bpmn:startEvent id="uml_start" name="Inicio" />
                    <bpmn:userTask id="uml_recepcionar" name="Recepcionar solicitud" data-role="ROLE_CAJA" data-form-id="form_demo_uml_actividad" />
                    <bpmn:userTask id="uml_documento" name="Formulario y documentos" data-role="ROLE_CAJA" />
                    <bpmn:exclusiveGateway id="uml_decision_docs" name="¿Documentación completa?" />
                    <bpmn:userTask id="uml_solicitar_correccion" name="Solicitar corrección" data-role="ROLE_ATENCION_CLIENTE" />
                    <bpmn:userTask id="uml_recibir_correccion" name="Recibir subsanación" data-role="ROLE_ATENCION_CLIENTE" />
                    <bpmn:userTask id="uml_formalizar" name="Formalizar cuenta" data-role="ROLE_CAJA" />
                    <bpmn:exclusiveGateway id="uml_merge_docs" name="Unir revisión documental" />
                    <bpmn:parallelGateway id="uml_fork" name="Dividir validaciones" />
                    <bpmn:userTask id="uml_revisar_cumplimiento" name="Revisar cumplimiento" data-role="ROLE_COMPLIANCE" />
                    <bpmn:userTask id="uml_evaluar_riesgo" name="Evaluar riesgo" data-role="ROLE_RIESGO" />
                    <bpmn:parallelGateway id="uml_join" name="Sincronizar validaciones" />
                    <bpmn:userTask id="uml_alta_producto" name="Dar alta al producto" data-role="ROLE_OPERACIONES" />
                    <bpmn:userTask id="uml_notificar" name="Notificar activación" data-role="ROLE_ATENCION_CLIENTE" />
                    <bpmn:endEvent id="uml_final" name="Fin" />
                    <bpmn:sequenceFlow id="flow_1" sourceRef="uml_start" targetRef="uml_recepcionar" />
                    <bpmn:sequenceFlow id="flow_2" sourceRef="uml_recepcionar" targetRef="uml_documento" />
                    <bpmn:sequenceFlow id="flow_3" sourceRef="uml_documento" targetRef="uml_decision_docs" />
                    <bpmn:sequenceFlow id="flow_4" name="[documentación incompleta]" sourceRef="uml_decision_docs" targetRef="uml_solicitar_correccion" />
                    <bpmn:sequenceFlow id="flow_5" sourceRef="uml_solicitar_correccion" targetRef="uml_recibir_correccion" />
                    <bpmn:sequenceFlow id="flow_6" sourceRef="uml_recibir_correccion" targetRef="uml_merge_docs" />
                    <bpmn:sequenceFlow id="flow_7" name="[documentación completa]" sourceRef="uml_decision_docs" targetRef="uml_formalizar" />
                    <bpmn:sequenceFlow id="flow_8" sourceRef="uml_formalizar" targetRef="uml_merge_docs" />
                    <bpmn:sequenceFlow id="flow_9" sourceRef="uml_merge_docs" targetRef="uml_fork" />
                    <bpmn:sequenceFlow id="flow_10" sourceRef="uml_fork" targetRef="uml_revisar_cumplimiento" />
                    <bpmn:sequenceFlow id="flow_11" sourceRef="uml_fork" targetRef="uml_evaluar_riesgo" />
                    <bpmn:sequenceFlow id="flow_12" sourceRef="uml_revisar_cumplimiento" targetRef="uml_join" />
                    <bpmn:sequenceFlow id="flow_13" sourceRef="uml_evaluar_riesgo" targetRef="uml_join" />
                    <bpmn:sequenceFlow id="flow_14" sourceRef="uml_join" targetRef="uml_alta_producto" />
                    <bpmn:sequenceFlow id="flow_15" sourceRef="uml_alta_producto" targetRef="uml_notificar" />
                    <bpmn:sequenceFlow id="flow_16" sourceRef="uml_notificar" targetRef="uml_final" />
                  </bpmn:process>
                  <bpmndi:BPMNDiagram id="BPMNDiagram_UmlActivitySeed">
                    <bpmndi:BPMNPlane id="BPMNPlane_UmlActivitySeed" bpmnElement="Process_UmlActivitySeed" />
                  </bpmndi:BPMNDiagram>
                </bpmn:definitions>
                """;
    }
}
