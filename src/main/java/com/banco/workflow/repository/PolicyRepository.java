package com.banco.workflow.repository;

import com.banco.workflow.model.Policy;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PolicyRepository extends MongoRepository<Policy, String> {
    List<Policy> findByActive(boolean active);
    List<Policy> findByStatus(String status);
    Optional<Policy> findByNameAndVersion(String name, int version);
}
