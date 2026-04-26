package com.banco.workflow.config;

import com.banco.workflow.model.Policy;
import com.banco.workflow.model.User;
import com.banco.workflow.repository.PolicyRepository;
import com.banco.workflow.repository.UserRepository;
import com.banco.workflow.service.AuthService;
import com.banco.workflow.service.PolicyService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
@RequiredArgsConstructor
public class PolicyCollaborationWebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper;
    private final AuthService authService;
    private final UserRepository userRepository;
    private final PolicyRepository policyRepository;
    private final PolicyService policyService;

    private final Map<String, Set<WebSocketSession>> rooms = new ConcurrentHashMap<>();
    private final Map<String, SessionContext> sessionContexts = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        try {
            String policyId = extractPolicyId(session.getUri());
            String token = extractQueryValue(session.getUri(), "token");
            if (policyId == null || policyId.isBlank() || token == null || token.isBlank()) {
                closeQuietly(session, CloseStatus.BAD_DATA);
                return;
            }

            if (!authService.isTokenValid(token)) {
                closeQuietly(session, CloseStatus.NOT_ACCEPTABLE.withReason("Token inválido"));
                return;
            }

            Policy policy = policyRepository.findById(policyId).orElse(null);
            if (policy == null) {
                closeQuietly(session, CloseStatus.NOT_ACCEPTABLE.withReason("Política inexistente"));
                return;
            }

            String userId = authService.getUserIdFromToken(token);
            String username = authService.getUsernameFromToken(token);
            String empresa = authService.getEmpresaFromToken(token);
            Set<String> roles = authService.getRolesFromToken(token);
            Optional<User> actorOptional = userRepository.findById(userId);
            if (actorOptional.isEmpty() || !roles.contains("ROLE_ADMIN")) {
                closeQuietly(session, CloseStatus.NOT_ACCEPTABLE.withReason("Acceso no autorizado"));
                return;
            }

            User actor = actorOptional.get();
            if (policy.getTenantEmpresa() != null && empresa != null
                    && !policy.getTenantEmpresa().equalsIgnoreCase(empresa)) {
                closeQuietly(session, CloseStatus.NOT_ACCEPTABLE.withReason("Empresa no autorizada"));
                return;
            }

            boolean isOwner = actor.getId() != null && actor.getId().equals(policy.getOwnerUserId());
            if (!isOwner && !policy.isCollaborationEnabled()) {
                closeQuietly(session, CloseStatus.NOT_ACCEPTABLE.withReason("La política es privada"));
                return;
            }

            boolean canEdit = policyService.canEditPolicy(policy, actor);
            SessionContext context = new SessionContext(policyId, actor, canEdit);
            sessionContexts.put(session.getId(), context);
            rooms.computeIfAbsent(policyId, key -> ConcurrentHashMap.newKeySet()).add(session);

            log.info("Sesión de colaboración conectada: session={}, policy={}, actor={}", session.getId(), policyId, username);
            sendPresence(session, policy, actor, canEdit, "PRESENCE");
            broadcastPresence(policy, actor, "PRESENCE");
        } catch (Exception e) {
            log.warn("No se pudo establecer sesión colaborativa: {}", e.getMessage());
            closeQuietly(session, CloseStatus.SERVER_ERROR);
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        SessionContext context = sessionContexts.get(session.getId());
        if (context == null) {
            return;
        }

        Policy policy = policyRepository.findById(context.policyId()).orElse(null);
        if (policy == null) {
            closeQuietly(session, CloseStatus.NOT_ACCEPTABLE.withReason("Política inexistente"));
            return;
        }

        PolicyCollaborationMessage payload = objectMapper.readValue(message.getPayload(), PolicyCollaborationMessage.class);
        String type = payload.getType() != null ? payload.getType() : "STATE_SYNC";
        boolean editEvent = "STATE_SYNC".equals(type) || "MODE_CHANGED".equals(type);
        if (editEvent && !context.canEdit()) {
            sendError(session, policy, context.actor(), "No tienes permiso de edición sobre esta política");
            return;
        }

        enrichPayload(payload, policy, context.actor(), context.canEdit());
        if ("MODE_CHANGED".equals(type)) {
            policy.setCollaborationEnabled(payload.isCollaborationEnabled());
            policy.setCollaborationMode(payload.getCollaborationMode());
            policy.setLastEditedByUserId(context.actor().getId());
            policy.setUpdatedAt(LocalDateTime.now());
            policyRepository.save(policy);
        }

        broadcast(policy.getId(), payload, session.getId());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        SessionContext context = sessionContexts.remove(session.getId());
        if (context == null) {
            return;
        }

        Set<WebSocketSession> sessions = rooms.get(context.policyId());
        if (sessions != null) {
            sessions.remove(session);
            if (sessions.isEmpty()) {
                rooms.remove(context.policyId());
            }
        }

        Policy policy = policyRepository.findById(context.policyId()).orElse(null);
        if (policy != null) {
            broadcastPresence(policy, context.actor(), "PRESENCE");
        }

        log.info("Sesión de colaboración cerrada: session={}, policy={}, status={}", session.getId(), context.policyId(), status);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        SessionContext context = sessionContexts.get(session.getId());
        log.warn("Error de transporte en colaboración policy {}: {}", context != null ? context.policyId() : "?", exception.getMessage());
        if (session.isOpen()) {
            session.close(CloseStatus.SERVER_ERROR);
        }
    }

    private void broadcastPresence(Policy policy, User actor, String type) {
        try {
            PolicyCollaborationMessage message = buildPresenceMessage(policy, actor, type);
            broadcast(policy.getId(), message, null);
        } catch (Exception e) {
            log.debug("No se pudo emitir presencia para policy {}: {}", policy.getId(), e.getMessage());
        }
    }

    private void sendPresence(WebSocketSession session, Policy policy, User actor, boolean canEdit, String type) throws IOException {
        PolicyCollaborationMessage message = buildPresenceMessage(policy, actor, type);
        message.setCanEdit(canEdit);
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(message)));
    }

    private PolicyCollaborationMessage buildPresenceMessage(Policy policy, User actor, String type) {
        PolicyCollaborationMessage message = new PolicyCollaborationMessage();
        enrichPayload(message, policy, actor, policyService.canEditPolicy(policy, actor));
        message.setType(type);
        return message;
    }

    private void enrichPayload(PolicyCollaborationMessage payload, Policy policy, User actor, boolean canEdit) {
        payload.setPolicyId(policy.getId());
        payload.setPolicyName(policy.getName());
        payload.setOwnerUserId(policy.getOwnerUserId());
        payload.setOwnerDisplayName(resolveOwnerDisplayName(policy));
        payload.setEmpresa(policy.getTenantEmpresa());
        payload.setCollaborationEnabled(policy.isCollaborationEnabled());
        payload.setCollaborationMode(policy.getCollaborationMode());
        payload.setActorUserId(actor.getId());
        payload.setActor(actor.getUsername());
        payload.setActorDisplayName(actor.getUsername());
        payload.setCanEdit(canEdit);
        payload.setTimestamp(System.currentTimeMillis());
    }

    private String resolveOwnerDisplayName(Policy policy) {
        if (policy.getOwnerUserId() == null) {
            return "Administrador";
        }
        return userRepository.findById(policy.getOwnerUserId())
                .map(User::getUsername)
                .orElse("Administrador");
    }

    private void sendError(WebSocketSession session, Policy policy, User actor, String error) throws IOException {
        PolicyCollaborationMessage payload = new PolicyCollaborationMessage();
        enrichPayload(payload, policy, actor, false);
        payload.setType("ERROR");
        payload.setDescription(error);
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(payload)));
    }

    private void broadcast(String policyId, PolicyCollaborationMessage payload, String excludedSessionId) throws IOException {
        String serialized = objectMapper.writeValueAsString(payload);
        for (WebSocketSession peer : rooms.getOrDefault(policyId, Set.of())) {
            if (!peer.isOpen()) {
                continue;
            }
            if (excludedSessionId != null && peer.getId().equals(excludedSessionId)) {
                continue;
            }
            peer.sendMessage(new TextMessage(serialized));
        }
    }

    private String extractPolicyId(URI uri) {
        if (uri == null) {
            return null;
        }
        String path = uri.getPath();
        if (path == null || path.isBlank()) {
            return null;
        }
        String[] segments = path.split("/");
        return segments.length == 0 ? null : segments[segments.length - 1];
    }

    private String extractQueryValue(URI uri, String key) {
        if (uri == null || uri.getQuery() == null) {
            return null;
        }
        Map<String, String> params = new HashMap<>();
        for (String pair : uri.getQuery().split("&")) {
            String[] parts = pair.split("=", 2);
            if (parts.length == 2) {
                params.put(parts[0], URLDecoder.decode(parts[1], StandardCharsets.UTF_8));
            }
        }
        return params.get(key);
    }

    private void closeQuietly(WebSocketSession session, CloseStatus status) {
        try {
            session.close(status);
        } catch (IOException ignored) {
            log.debug("No se pudo cerrar la sesión websocket {}", session.getId());
        }
    }

    private record SessionContext(String policyId, User actor, boolean canEdit) {
    }
}
