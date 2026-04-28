package com.banco.workflow.repository;

import com.banco.workflow.model.DynamicForm;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DynamicFormRepository extends MongoRepository<DynamicForm, String> {
    Optional<DynamicForm> findByName(String name);
    List<DynamicForm> findByTenantEmpresa(String tenantEmpresa);
    List<DynamicForm> findByTenantEmpresaAndActive(String tenantEmpresa, boolean active);
    boolean existsByNameAndTenantEmpresa(String name, String tenantEmpresa);
}
