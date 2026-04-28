package com.banco.workflow.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public class PasswordRecoveryDtos {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ForgotPasswordRequest {
        /**
         * Puede ser username o email.
         */
        private String identifier;
        /**
         * En SaaS multiempresa, ayuda a evitar ambigüedades cuando aplica.
         * Si el usuario se encuentra unívocamente por username/email, se ignora.
         */
        private String empresa;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ForgotPasswordResponse {
        private boolean success;
        /**
         * Para entorno demo: se devuelve el token para completar el flujo sin correo real.
         */
        private String resetToken;
        private String message;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResetPasswordRequest {
        private String token;
        private String newPassword;
    }
}

