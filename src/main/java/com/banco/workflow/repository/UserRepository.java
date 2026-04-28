package com.banco.workflow.repository;

import com.banco.workflow.model.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends MongoRepository<User, String> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    List<User> findByEmpresa(String empresa);
    List<User> findByEmpresaAndActive(String empresa, boolean active);
    List<User> findByEmpresaAndDepartamento(String empresa, String departamento);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
}
