package com.banco.workflow.service;

import com.banco.workflow.dto.UserDtos;
import com.banco.workflow.dto.RegisterClientRequest;
import com.banco.workflow.model.User;
import com.banco.workflow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private static final Set<String> ALLOWED_ROLES = Set.of(
            "ROLE_ADMIN", "ROLE_REVISOR", "ROLE_GERENTE", "ROLE_CLIENTE",
            "ROLE_CAJA", "ROLE_OPERACIONES", "ROLE_COMPLIANCE",
            "ROLE_RIESGO", "ROLE_PRESTAMOS_VEHICULO", "ROLE_ATENCION_CLIENTE"
    );

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public Optional<User> getUserById(String id) {
        return userRepository.findById(id);
    }

    public Optional<User> getUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public Optional<User> getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public List<User> getUsersByEmpresa(String empresa) {
        return userRepository.findByEmpresa(empresa);
    }

    public List<User> getActiveUsersByEmpresa(String empresa) {
        return userRepository.findByEmpresaAndActive(empresa, true);
    }

    public List<User> getUsersByDepartamento(String empresa, String departamento) {
        return userRepository.findByEmpresaAndDepartamento(empresa, departamento);
    }

    public User assignDepartamento(String userId, String departamento, String actorEmpresa) {
        User user = requireUserInEmpresa(userId, actorEmpresa);
        user.setDepartamento(departamento != null ? departamento.trim() : null);
        user.setUpdatedAt(LocalDateTime.now());
        User saved = userRepository.save(user);
        log.info("Departamento '{}' asignado a usuario '{}'", departamento, user.getUsername());
        return saved;
    }

    public java.util.Map<String, Object> getUserStatistics(String userId) {
        java.util.Map<String, Object> stats = new java.util.HashMap<>();
        stats.put("tasksCompleted", 0);
        stats.put("processesInitiated", 0);
        stats.put("documentsUploaded", 0);
        stats.put("averageCompletionTime", 0);
        return stats;
    }

    public Optional<User> validateCredentials(String username, String password) {
        Optional<User> user = getUserByUsername(username);
        if (user.isPresent() && user.get().isActive()) {
            if (passwordEncoder.matches(password, user.get().getPasswordHash())) {
                return user;
            }
        }
        return Optional.empty();
    }

    public User createUser(UserDtos.CreateUserRequest request, String empresa) {
        validateCreateRequest(request);
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("El nombre de usuario ya existe: " + request.getUsername());
        }
        if (request.getEmail() != null && userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("El email ya está registrado: " + request.getEmail());
        }

        Set<String> roles = request.getRoles() != null && !request.getRoles().isEmpty()
                ? validateRoles(request.getRoles())
                : Set.of("ROLE_REVISOR");

        LocalDateTime now = LocalDateTime.now();
        User user = User.builder()
                .id(UUID.randomUUID().toString())
                .username(request.getUsername().trim().toLowerCase())
                .email(request.getEmail() != null ? request.getEmail().trim().toLowerCase() : null)
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .roles(roles)
                .departamento(request.getDepartamento())
                .empresa(empresa)
                .active(true)
                .createdAt(now)
                .updatedAt(now)
                .build();

        User saved = userRepository.save(user);
        log.info("Usuario creado: {} en empresa {}", saved.getUsername(), empresa);
        return saved;
    }

    /**
     * CU-22: Registro público de cliente.
     * Crea un usuario con rol ROLE_CLIENTE en la empresa indicada.
     */
    public User registerClient(RegisterClientRequest request) {
        if (request.getEmpresa() == null || request.getEmpresa().isBlank()) {
            throw new IllegalArgumentException("El campo 'empresa' es obligatorio");
        }
        UserDtos.CreateUserRequest create = UserDtos.CreateUserRequest.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(request.getPassword())
                .roles(Set.of("ROLE_CLIENTE"))
                .departamento(null)
                .build();
        return createUser(create, request.getEmpresa().trim());
    }

    public User updateUser(String userId, UserDtos.UpdateUserRequest request, String actorEmpresa) {
        User user = requireUserInEmpresa(userId, actorEmpresa);

        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new IllegalArgumentException("El email ya está registrado");
            }
            user.setEmail(request.getEmail().trim().toLowerCase());
        }
        if (request.getRoles() != null && !request.getRoles().isEmpty()) {
            user.setRoles(validateRoles(request.getRoles()));
        }
        if (request.getDepartamento() != null) {
            user.setDepartamento(request.getDepartamento());
        }
        if (request.getActive() != null) {
            user.setActive(request.getActive());
        }
        user.setUpdatedAt(LocalDateTime.now());

        User saved = userRepository.save(user);
        log.info("Usuario actualizado: {}", saved.getUsername());
        return saved;
    }

    public void deactivateUser(String userId, String actorEmpresa) {
        User user = requireUserInEmpresa(userId, actorEmpresa);
        user.setActive(false);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
        log.info("Usuario desactivado: {}", user.getUsername());
    }

    public void changePassword(String userId, UserDtos.ChangePasswordRequest request, String actorUsername) {
        if (request.getNewPassword() == null || request.getNewPassword().length() < 8) {
            throw new IllegalArgumentException("La nueva contraseña debe tener al menos 8 caracteres");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        boolean isSelf = user.getUsername().equals(actorUsername);
        if (isSelf) {
            if (request.getCurrentPassword() == null || !passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
                throw new IllegalArgumentException("La contraseña actual es incorrecta");
            }
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
        log.info("Contraseña cambiada para usuario: {}", user.getUsername());
    }

    public User assignRoles(String userId, Set<String> roles, String actorEmpresa) {
        User user = requireUserInEmpresa(userId, actorEmpresa);
        user.setRoles(validateRoles(roles));
        user.setUpdatedAt(LocalDateTime.now());
        User saved = userRepository.save(user);
        log.info("Roles actualizados para usuario {}: {}", user.getUsername(), roles);
        return saved;
    }

    public Optional<User> getCurrentAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null || "anonymousUser".equals(authentication.getName())) {
            return Optional.empty();
        }
        return userRepository.findByUsername(authentication.getName());
    }

    private User requireUserInEmpresa(String userId, String actorEmpresa) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado: " + userId));
        if (actorEmpresa != null && !actorEmpresa.isBlank()
                && user.getEmpresa() != null && !actorEmpresa.equalsIgnoreCase(user.getEmpresa())) {
            throw new IllegalArgumentException("No puedes modificar usuarios de otra empresa");
        }
        return user;
    }

    private Set<String> validateRoles(Set<String> roles) {
        for (String role : roles) {
            if (!ALLOWED_ROLES.contains(role)) {
                throw new IllegalArgumentException("Rol no permitido: " + role + ". Roles válidos: " + ALLOWED_ROLES);
            }
        }
        return roles;
    }

    private void validateCreateRequest(UserDtos.CreateUserRequest request) {
        if (request.getUsername() == null || request.getUsername().isBlank()) {
            throw new IllegalArgumentException("El nombre de usuario es requerido");
        }
        if (request.getPassword() == null || request.getPassword().length() < 8) {
            throw new IllegalArgumentException("La contraseña debe tener al menos 8 caracteres");
        }
        if (!request.getUsername().matches("^[a-zA-Z0-9_.-]{3,50}$")) {
            throw new IllegalArgumentException("El nombre de usuario solo puede contener letras, números, _, . y - (3-50 caracteres)");
        }
    }
}
