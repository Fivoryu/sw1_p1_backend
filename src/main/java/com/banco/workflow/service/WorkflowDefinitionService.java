package com.banco.workflow.service;

import com.banco.workflow.model.Policy;
import com.banco.workflow.model.WorkflowDefinition;
import com.banco.workflow.repository.WorkflowDefinitionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WorkflowDefinitionService {

    private final BpmnParserService bpmnParserService;
    private final WorkflowDefinitionRepository workflowDefinitionRepository;

    public WorkflowDefinition compileDraftDefinition(Policy policy) throws Exception {
        return buildDefinition(policy, null);
    }

    public WorkflowDefinition compileAndPublish(Policy policy) throws Exception {
        WorkflowDefinition definition = buildDefinition(policy, LocalDateTime.now());
        Optional<WorkflowDefinition> existing = workflowDefinitionRepository.findByPolicyIdAndPolicyVersion(
                policy.getId(),
                policy.getVersion()
        );
        if (existing.isPresent()) {
            definition.setId(existing.get().getId());
        }
        return workflowDefinitionRepository.save(definition);
    }

    public Optional<WorkflowDefinition> getDefinitionByPolicy(String policyId, int version) {
        return workflowDefinitionRepository.findByPolicyIdAndPolicyVersion(policyId, version);
    }

    public List<WorkflowDefinition> getDefinitionsByPolicy(String policyId) {
        return workflowDefinitionRepository.findByPolicyIdOrderByPolicyVersionDesc(policyId);
    }

    private WorkflowDefinition buildDefinition(Policy policy, LocalDateTime publishedAt) throws Exception {
        var graph = bpmnParserService.parseProcessDefinition(policy.getBpmnXml());
        var validationErrors = bpmnParserService.validateTopology(graph);

        if (!validationErrors.isEmpty() && publishedAt != null) {
            throw new IllegalArgumentException("BPMN inválido: " + String.join(", ", validationErrors));
        }

        return WorkflowDefinition.builder()
                .id(UUID.randomUUID().toString())
                .policyId(policy.getId())
                .policyName(policy.getName())
                .policyVersion(policy.getVersion())
                .validationStatus(validationErrors.isEmpty() ? "VALID" : "INVALID")
                .validationErrors(validationErrors)
                .graph(graph)
                .compiledAt(LocalDateTime.now())
                .publishedAt(publishedAt)
                .build();
    }
}
