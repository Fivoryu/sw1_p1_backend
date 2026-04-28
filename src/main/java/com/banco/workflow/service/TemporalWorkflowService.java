package com.banco.workflow.service;

import com.banco.workflow.model.Policy;
import com.banco.workflow.model.ProcessInstance;
import com.banco.workflow.model.Task;
import com.banco.workflow.model.User;
import com.banco.workflow.model.WorkflowDefinition;
import com.banco.workflow.repository.PolicyRepository;
import com.banco.workflow.repository.ProcessInstanceRepository;
import com.banco.workflow.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class TemporalWorkflowService {

    private final ProcessInstanceRepository instanceRepository;
    private final PolicyRepository policyRepository;
    private final TaskRepository taskRepository;
    private final WorkflowRuntimeService workflowRuntimeService;
    private final UserService userService;
    private final WorkflowDefinitionService workflowDefinitionService;

    public ProcessInstance startWorkflow(String policyId, Map<String, Object> variables) throws Exception {
        User initiator = userService.getCurrentAuthenticatedUser()
                .orElseThrow(() -> new RuntimeException("No hay usuario autenticado"));
        Policy policy = policyRepository.findById(policyId)
                .orElseThrow(() -> new RuntimeException("Política no encontrada: " + policyId));
        if (!"PUBLISHED".equals(policy.getStatus())) {
            throw new IllegalArgumentException("La política no está publicada");
        }
        if (policy.getTenantEmpresa() != null && initiator.getEmpresa() != null
                && !policy.getTenantEmpresa().equalsIgnoreCase(initiator.getEmpresa())) {
            throw new IllegalArgumentException("No puedes iniciar trámites de otra empresa");
        }
        WorkflowDefinition definition = workflowDefinitionService.getDefinitionByPolicy(policy.getId(), policy.getVersion())
                .orElseThrow(() -> new RuntimeException("La política no tiene una definición ejecutable publicada"));
        return workflowRuntimeService.startProcess(policy, definition, variables, initiator);
    }

    public ProcessInstance getInstanceStatus(String instanceId) throws Exception {
        return instanceRepository.findById(instanceId)
                .orElseThrow(() -> new RuntimeException("Instancia no encontrada: " + instanceId));
    }

    public ProcessInstance completeTask(String instanceId, String taskId, Map<String, Object> taskData) throws Exception {
        ProcessInstance instance = instanceRepository.findById(instanceId)
                .orElseThrow(() -> new RuntimeException("Instancia no encontrada: " + instanceId));
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Tarea no encontrada: " + taskId));
        Policy policy = policyRepository.findById(instance.getPolicyId())
                .orElseThrow(() -> new RuntimeException("Política no encontrada: " + instance.getPolicyId()));
        WorkflowDefinition definition = workflowDefinitionService.getDefinitionByPolicy(policy.getId(), policy.getVersion())
                .orElseThrow(() -> new RuntimeException("La política no tiene una definición ejecutable publicada"));
        User completedBy = userService.getCurrentAuthenticatedUser()
                .orElseThrow(() -> new RuntimeException("No hay usuario autenticado"));
        return workflowRuntimeService.completeTask(definition, instance, task, taskData, completedBy);
    }

    public List<ProcessInstance.HistoryEntry> getInstanceHistory(String instanceId) throws Exception {
        ProcessInstance instance = getInstanceStatus(instanceId);
        return instance.getHistory() != null ? instance.getHistory() : List.of();
    }

    public List<ProcessInstance> getInstancesByStatus(String status) {
        return instanceRepository.findByStatus(status);
    }

    public List<ProcessInstance> getInstancesByPolicy(String policyId) {
        return instanceRepository.findByPolicyId(policyId);
    }
}
