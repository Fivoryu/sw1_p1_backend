package com.banco.workflow.graphql;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.banco.workflow.model.User;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AuthResponse {
    private boolean success;
    private String message;
    private String token;
    private User user;
}
