package com.banco.workflow.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final PolicyCollaborationWebSocketHandler policyCollaborationWebSocketHandler;
    private final TaskQueueWebSocketHandler taskQueueWebSocketHandler;

    public WebSocketConfig(
            PolicyCollaborationWebSocketHandler policyCollaborationWebSocketHandler,
            TaskQueueWebSocketHandler taskQueueWebSocketHandler
    ) {
        this.policyCollaborationWebSocketHandler = policyCollaborationWebSocketHandler;
        this.taskQueueWebSocketHandler = taskQueueWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(policyCollaborationWebSocketHandler, "/ws/policies/*")
                .setAllowedOriginPatterns("*");
        registry.addHandler(taskQueueWebSocketHandler, "/ws/operator/roles/*")
                .setAllowedOriginPatterns("*");
    }
}
