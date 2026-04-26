package com.banco.workflow.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class TaskQueueWebSocketHandler extends TextWebSocketHandler {

    private final Map<String, Set<WebSocketSession>> roleRooms = new ConcurrentHashMap<>();
    private final Map<String, String> sessionToRole = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String role = extractLastSegment(session.getUri());
        if (role == null || role.isBlank()) {
            closeQuietly(session, CloseStatus.BAD_DATA);
            return;
        }
        sessionToRole.put(session.getId(), role);
        roleRooms.computeIfAbsent(role, ignored -> ConcurrentHashMap.newKeySet()).add(session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String role = sessionToRole.remove(session.getId());
        if (role == null) {
            return;
        }
        Set<WebSocketSession> sessions = roleRooms.get(role);
        if (sessions != null) {
            sessions.remove(session);
            if (sessions.isEmpty()) {
                roleRooms.remove(role);
            }
        }
    }

    public void broadcastToRole(String role, String payload) {
        for (WebSocketSession session : roleRooms.getOrDefault(role, Set.of())) {
            if (!session.isOpen()) {
                continue;
            }
            try {
                session.sendMessage(new TextMessage(payload));
            } catch (IOException e) {
                log.debug("No se pudo enviar actualización de bandeja al rol {}: {}", role, e.getMessage());
            }
        }
    }

    private String extractLastSegment(URI uri) {
        if (uri == null || uri.getPath() == null) {
            return null;
        }
        String[] segments = uri.getPath().split("/");
        return segments.length == 0 ? null : segments[segments.length - 1];
    }

    private void closeQuietly(WebSocketSession session, CloseStatus status) {
        try {
            session.close(status);
        } catch (IOException ignored) {
            log.debug("No se pudo cerrar sesión websocket {}", session.getId());
        }
    }
}
