package com.banco.workflow.controller;

import com.banco.workflow.dto.UserDtos;
import com.banco.workflow.model.User;
import com.banco.workflow.service.DepartmentService;
import com.banco.workflow.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * CU-04: Gestionar Usuarios
 * CU-05: Gestionar Roles y Permisos
 *
 * Todos los endpoints filtran por tenantEmpresa del admin autenticado.
 * Un admin solo puede ver/modificar usuarios de su misma empresa.
 */
@RestController
@RequestMapping("/v1/users")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
public class UserController {

    private final UserService userService;
    private final DepartmentService departmentService;

    /**
     * CU-04: Listar usuarios de la empresa del admin autenticado.
     */
    @GetMapping
    public ResponseEntity<List<UserDtos.UserResponse>> listUsers() {
        User actor = requireCurrentUser();
        List<UserDtos.UserResponse> users = userService.getUsersByEmpresa(actor.getEmpresa())
                .stream()
                .map(UserDtos.UserResponse::from)
                .toList();
        return ResponseEntity.ok(users);
    }

    /**
     * CU-04: Obtener un usuario por ID (solo de la misma empresa).
     */
    @GetMapping("/{id}")
    public ResponseEntity<UserDtos.UserResponse> getUser(@PathVariable String id) {
        User actor = requireCurrentUser();
        User user = userService.getUserById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        assertSameEmpresa(user, actor);
        return ResponseEntity.ok(UserDtos.UserResponse.from(user));
    }

    /**
     * CU-04: Crear usuario en la empresa del admin autenticado.
     */
    @PostMapping
    public ResponseEntity<UserDtos.UserResponse> createUser(@RequestBody UserDtos.CreateUserRequest request) {
        User actor = requireCurrentUser();
        User created = userService.createUser(request, actor.getEmpresa());
        return ResponseEntity.status(HttpStatus.CREATED).body(UserDtos.UserResponse.from(created));
    }

    /**
     * CU-04: Actualizar datos de un usuario.
     */
    @PutMapping("/{id}")
    public ResponseEntity<UserDtos.UserResponse> updateUser(
            @PathVariable String id,
            @RequestBody UserDtos.UpdateUserRequest request) {
        User actor = requireCurrentUser();
        User updated = userService.updateUser(id, request, actor.getEmpresa());
        return ResponseEntity.ok(UserDtos.UserResponse.from(updated));
    }

    /**
     * CU-04: Desactivar un usuario (soft delete).
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivateUser(@PathVariable String id) {
        User actor = requireCurrentUser();
        userService.deactivateUser(id, actor.getEmpresa());
        return ResponseEntity.noContent().build();
    }

    /**
     * CU-04: Cambio de contraseña por el admin o el propio usuario.
     * El admin puede cambiar sin requerir contraseña actual.
     * El propio usuario debe enviar su contraseña actual.
     */
    @PatchMapping("/{id}/password")
    @PreAuthorize("hasRole('ADMIN') or #id == authentication.details")
    public ResponseEntity<Void> changePassword(
            @PathVariable String id,
            @RequestBody UserDtos.ChangePasswordRequest request) {
        User actor = requireCurrentUser();
        userService.changePassword(id, request, actor.getUsername());
        return ResponseEntity.noContent().build();
    }

    /**
     * CU-05: Asignar roles a un usuario.
     * Solo el admin puede asignar roles.
     */
    @PutMapping("/{id}/roles")
    public ResponseEntity<UserDtos.UserResponse> assignRoles(
            @PathVariable String id,
            @RequestBody UserDtos.AssignRolesRequest request) {
        if (request.getRoles() == null || request.getRoles().isEmpty()) {
            throw new IllegalArgumentException("Debes especificar al menos un rol");
        }
        User actor = requireCurrentUser();
        User updated = userService.assignRoles(id, request.getRoles(), actor.getEmpresa());
        return ResponseEntity.ok(UserDtos.UserResponse.from(updated));
    }

    /**
     * CU-06: Listar usuarios de un departamento específico.
     * Útil para que el admin vea quién está en cada área.
     */
    @GetMapping("/by-departamento/{departamento}")
    public ResponseEntity<List<UserDtos.UserResponse>> getUsersByDepartamento(
            @PathVariable String departamento) {
        User actor = requireCurrentUser();
        List<UserDtos.UserResponse> users = userService
                .getUsersByDepartamento(actor.getEmpresa(), departamento)
                .stream()
                .map(UserDtos.UserResponse::from)
                .toList();
        return ResponseEntity.ok(users);
    }

    /**
     * CU-06: Asignar departamento a un usuario funcionario.
     * Body: { "departamento": "Caja" }
     * Valida que el departamento exista en el catálogo de la empresa antes de asignar.
     */
    @PatchMapping("/{id}/departamento")
    public ResponseEntity<UserDtos.UserResponse> assignDepartamento(
            @PathVariable String id,
            @RequestBody Map<String, String> body) {
        String departamento = body.get("departamento");
        if (departamento == null || departamento.isBlank()) {
            throw new IllegalArgumentException("El campo 'departamento' es obligatorio");
        }
        boolean exists = departmentService.getActiveDepartments().stream()
                .anyMatch(d -> d.getName().equalsIgnoreCase(departamento.trim()));
        if (!exists) {
            throw new IllegalArgumentException(
                    "El departamento '" + departamento + "' no existe. Créalo primero en el catálogo.");
        }
        User actor = requireCurrentUser();
        User updated = userService.assignDepartamento(id, departamento.trim(), actor.getEmpresa());
        return ResponseEntity.ok(UserDtos.UserResponse.from(updated));
    }

    /**
     * CU-05: Listar roles disponibles en el sistema.
     */
    @GetMapping("/roles/available")
    public ResponseEntity<Set<String>> listAvailableRoles() {
        return ResponseEntity.ok(Set.of(
                "ROLE_ADMIN",
                "ROLE_REVISOR",
                "ROLE_GERENTE",
                "ROLE_CLIENTE",
                "ROLE_CAJA",
                "ROLE_OPERACIONES",
                "ROLE_COMPLIANCE",
                "ROLE_RIESGO",
                "ROLE_PRESTAMOS_VEHICULO",
                "ROLE_ATENCION_CLIENTE"
        ));
    }

    private User requireCurrentUser() {
        return userService.getCurrentAuthenticatedUser()
                .orElseThrow(() -> new RuntimeException("No hay usuario autenticado"));
    }

    private void assertSameEmpresa(User target, User actor) {
        if (actor.getEmpresa() != null && target.getEmpresa() != null
                && !actor.getEmpresa().equalsIgnoreCase(target.getEmpresa())) {
            throw new IllegalArgumentException("No puedes acceder a usuarios de otra empresa");
        }
    }
}
