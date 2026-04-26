package com.banco.workflow.repository;

import com.banco.workflow.model.Department;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DepartmentRepository extends MongoRepository<Department, String> {
    List<Department> findByActiveTrue();
    Optional<Department> findByRole(String role);
}
