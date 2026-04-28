package com.banco.workflow.controller;

import com.banco.workflow.dto.LoginRequest;
import com.banco.workflow.dto.LoginResponse;
import com.banco.workflow.dto.PasswordRecoveryDtos;
import com.banco.workflow.dto.RegisterClientRequest;
import com.banco.workflow.model.User;
import com.banco.workflow.service.AuthService;
import com.banco.workflow.service.PasswordRecoveryService;
import com.banco.workflow.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/v1/auth")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*", maxAge = 3600)
public class AuthController {

    private final UserService userService;
    private final AuthService authService;
    private final PasswordRecoveryService passwordRecoveryService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        log.info("Intento de login para usuario: {}", loginRequest.getUsername());

        if (loginRequest.getUsername() == null || loginRequest.getUsername().isBlank() ||
                loginRequest.getPassword() == null || loginRequest.getPassword().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Usuario y contraseña son requeridos"));
        }

        Optional<User> user = userService.validateCredentials(loginRequest.getUsername(), loginRequest.getPassword());
        if (user.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Usuario o contraseña inválidos"));
        }

        User authenticatedUser = user.get();
        String token = authService.generateToken(authenticatedUser);

        LoginResponse response = LoginResponse.builder()
                .token(token)
                .user(LoginResponse.UserDto.builder()
                        .id(authenticatedUser.getId())
                        .username(authenticatedUser.getUsername())
                        .email(authenticatedUser.getEmail())
                        .departamento(authenticatedUser.getDepartamento())
                        .empresa(authenticatedUser.getEmpresa())
                        .roles(authenticatedUser.getRoles())
                        .build())
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * CU-22: Registro público de cliente.
     * Devuelve token JWT para iniciar sesión inmediatamente.
     */
    @PostMapping("/register-client")
    public ResponseEntity<?> registerClient(@RequestBody RegisterClientRequest request) {
        User created = userService.registerClient(request);
        String token = authService.generateToken(created);
        LoginResponse response = LoginResponse.builder()
                .token(token)
                .user(LoginResponse.UserDto.builder()
                        .id(created.getId())
                        .username(created.getUsername())
                        .email(created.getEmail())
                        .departamento(created.getDepartamento())
                        .empresa(created.getEmpresa())
                        .roles(created.getRoles())
                        .build())
                .build();
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestHeader(value = "Authorization", required = false) String token) {
        return ResponseEntity.ok(Map.of("message", "Logout exitoso"));
    }

    /**
     * CU-03: Recuperar contraseña (demo).
     * En lugar de enviar correo, devuelve un resetToken utilizable para /password/reset.
     */
    @PostMapping("/password/forgot")
    public ResponseEntity<PasswordRecoveryDtos.ForgotPasswordResponse> forgotPassword(
            @RequestBody PasswordRecoveryDtos.ForgotPasswordRequest request) {
        return ResponseEntity.ok(passwordRecoveryService.forgot(request));
    }

    /**
     * CU-03: Confirmar recuperación de contraseña con token.
     */
    @PostMapping("/password/reset")
    public ResponseEntity<?> resetPassword(@RequestBody PasswordRecoveryDtos.ResetPasswordRequest request) {
        passwordRecoveryService.reset(request);
        return ResponseEntity.ok(Map.of("message", "Contraseña actualizada"));
    }

    @GetMapping("/verify")
    public ResponseEntity<?> verify(@RequestHeader("Authorization") String token) {
        if (token == null || !token.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Token inválido"));
        }

        String jwtToken = token.substring(7);
        String userId = authService.getUserIdFromToken(jwtToken);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Token expirado o inválido"));
        }

        return userService.getUserById(userId)
                .<ResponseEntity<?>>map(user -> ResponseEntity.ok(Map.of(
                        "valid", true,
                        "userId", user.getId(),
                        "username", user.getUsername(),
                        "roles", user.getRoles()
                )))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Usuario no encontrado")));
    }

    @GetMapping("/me")
    public ResponseEntity<?> me() {
        return userService.getCurrentAuthenticatedUser()
                .<ResponseEntity<?>>map(user -> ResponseEntity.ok(Map.of(
                        "id", user.getId(),
                        "username", user.getUsername(),
                        "email", user.getEmail(),
                        "departamento", user.getDepartamento(),
                        "empresa", user.getEmpresa(),
                        "roles", user.getRoles()
                )))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "No autenticado")));
    }
}
