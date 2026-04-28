package com.banco.workflow.service;

import com.banco.workflow.dto.PasswordRecoveryDtos;
import com.banco.workflow.model.PasswordResetToken;
import com.banco.workflow.model.User;
import com.banco.workflow.repository.PasswordResetTokenRepository;
import com.banco.workflow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PasswordRecoveryService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;

    public PasswordRecoveryDtos.ForgotPasswordResponse forgot(PasswordRecoveryDtos.ForgotPasswordRequest request) {
        String identifier = request.getIdentifier() != null ? request.getIdentifier().trim().toLowerCase() : "";
        if (identifier.isBlank()) {
            throw new IllegalArgumentException("El campo 'identifier' es obligatorio (username o email)");
        }

        Optional<User> userOpt = identifier.contains("@")
                ? userRepository.findByEmail(identifier)
                : userRepository.findByUsername(identifier);

        // Respuesta genérica (anti user-enumeration); para demo devolvemos token si existe user.
        if (userOpt.isEmpty()) {
            return PasswordRecoveryDtos.ForgotPasswordResponse.builder()
                    .success(true)
                    .resetToken(null)
                    .message("Si el usuario existe, se generó un token de recuperación")
                    .build();
        }

        User user = userOpt.get();
        if (request.getEmpresa() != null && !request.getEmpresa().isBlank()
                && user.getEmpresa() != null
                && !request.getEmpresa().trim().equalsIgnoreCase(user.getEmpresa())) {
            // Misma respuesta genérica
            return PasswordRecoveryDtos.ForgotPasswordResponse.builder()
                    .success(true)
                    .resetToken(null)
                    .message("Si el usuario existe, se generó un token de recuperación")
                    .build();
        }

        String rawToken = UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", "");
        String tokenHash = sha256(rawToken);
        LocalDateTime now = LocalDateTime.now();
        PasswordResetToken token = PasswordResetToken.builder()
                .id(UUID.randomUUID().toString())
                .userId(user.getId())
                .empresa(user.getEmpresa())
                .tokenHash(tokenHash)
                .createdAt(now)
                .expiresAt(now.plusMinutes(30))
                .build();
        tokenRepository.save(token);

        return PasswordRecoveryDtos.ForgotPasswordResponse.builder()
                .success(true)
                .resetToken(rawToken)
                .message("Token de recuperación generado (demo). Úsalo en /v1/auth/password/reset")
                .build();
    }

    public void reset(PasswordRecoveryDtos.ResetPasswordRequest request) {
        String token = request.getToken() != null ? request.getToken().trim() : "";
        if (token.isBlank()) {
            throw new IllegalArgumentException("El campo 'token' es obligatorio");
        }
        if (request.getNewPassword() == null || request.getNewPassword().length() < 8) {
            throw new IllegalArgumentException("La nueva contraseña debe tener al menos 8 caracteres");
        }

        String tokenHash = sha256(token);
        PasswordResetToken resetToken = tokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new IllegalArgumentException("Token inválido o expirado"));

        if (resetToken.getUsedAt() != null) {
            throw new IllegalArgumentException("El token ya fue utilizado");
        }
        if (resetToken.getExpiresAt() != null && resetToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Token expirado");
        }

        User user = userRepository.findById(resetToken.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        resetToken.setUsedAt(LocalDateTime.now());
        tokenRepository.save(resetToken);
    }

    private static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new RuntimeException("No se pudo hashear token", e);
        }
    }
}

