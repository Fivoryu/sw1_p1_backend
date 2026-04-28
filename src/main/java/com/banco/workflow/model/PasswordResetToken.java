package com.banco.workflow.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "password_reset_tokens")
public class PasswordResetToken {

    @Id
    private String id;

    private String userId;
    private String empresa;

    /**
     * Hash del token (nunca guardar token plano en DB).
     */
    @Indexed(unique = true)
    private String tokenHash;

    @Indexed(expireAfterSeconds = 0)
    private LocalDateTime expiresAt;

    private LocalDateTime usedAt;
    private LocalDateTime createdAt;
}

