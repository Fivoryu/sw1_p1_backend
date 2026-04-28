package com.banco.workflow.repository;

import com.banco.workflow.model.ProcessInstance;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProcessInstanceRepository extends MongoRepository<ProcessInstance, String> {
    List<ProcessInstance> findByStatus(String status);
    List<ProcessInstance> findByPolicyId(String policyId);
    List<ProcessInstance> findByInitiatedByUserId(String initiatedByUserId);
    List<ProcessInstance> findByTenantEmpresa(String tenantEmpresa);
    List<ProcessInstance> findByTenantEmpresaAndStatus(String tenantEmpresa, String status);
    Optional<ProcessInstance> findByTemporalProcessInstanceId(String temporalId);
}
