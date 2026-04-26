package com.banco.workflow.repository;

import com.banco.workflow.model.DynamicForm;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DynamicFormRepository extends MongoRepository<DynamicForm, String> {
    Optional<DynamicForm> findByName(String name);
}
