package com.banco.workflow.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "users")
public class User {
    
    @Id
    private String id;
    
    private String username;
    private String email;
    private String passwordHash;
    
    private Set<String> roles;  // ROLE_ADMIN, ROLE_REVISOR, ROLE_GERENTE, etc
    
    private String departamento;
    private String empresa;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean active;
}
