package com.banco.workflow.repository;

import com.banco.workflow.model.WorkflowDefinition;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WorkflowDefinitionRepository extends MongoRepository<WorkflowDefinition, String> {
    Optional<WorkflowDefinition> findByPolicyIdAndPolicyVersion(String policyId, int policyVersion);
    List<WorkflowDefinition> findByPolicyIdOrderByPolicyVersionDesc(String policyId);

    void deleteByPolicyId(String policyId);
}
