package com.banco.workflow.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterClientRequest {
    private String username;
    private String email;
    private String password;
    /**
     * Tenant/empresa en la que se crea el cliente.
     */
    private String empresa;
}

