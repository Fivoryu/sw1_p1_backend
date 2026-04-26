package com.banco.workflow.service;

import com.banco.workflow.model.User;
import com.banco.workflow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.Optional;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Obtiene un usuario por ID
     */
    public Optional<User> getUserById(String id) {
        return userRepository.findById(id);
    }

    /**
     * Obtiene un usuario por nombre de usuario
     */
    public Optional<User> getUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    /**
     * Obtiene un usuario por email
     */
    public Optional<User> getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    /**
     * Valida las credenciales de un usuario
     * @param username nombre de usuario
     * @param password contraseña en texto plano
     * @return Usuario si las credenciales son válidas, Optional.empty() en caso contrario
     */
    public Optional<User> validateCredentials(String username, String password) {
        Optional<User> user = getUserByUsername(username);
        
        if (user.isPresent() && user.get().isActive()) {
            User foundUser = user.get();
            // Verificar que la contraseña coincida con el hash
            if (passwordEncoder.matches(password, foundUser.getPasswordHash())) {
                return user;
            }
        }
        
        return Optional.empty();
    }

    /**
     * Obtiene estadísticas de un usuario
     */
    public Map<String, Object> getUserStatistics(String userId) {
        Map<String, Object> stats = new HashMap<>();
        stats.put("tasksCompleted", 0);
        stats.put("processesInitiated", 0);
        stats.put("documentsUploaded", 0);
        stats.put("averageCompletionTime", 0);
        return stats;
    }

    public Optional<User> getCurrentAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null || "anonymousUser".equals(authentication.getName())) {
            return Optional.empty();
        }
        return userRepository.findByUsername(authentication.getName());
    }
}
