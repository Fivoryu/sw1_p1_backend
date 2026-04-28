package com.banco.workflow.service;

import com.banco.workflow.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class WorkflowAiService {

    private final RestClient workflowAiRestClient;
    private final UserService userService;

    public Map<String, Object> health() {
        return get("/health");
    }

    public Map<String, Object> generateDiagram(Map<String, Object> request) {
        return post("/diagrams/generate", withTenant(request));
    }

    public Map<String, Object> extractForm(Map<String, Object> request) {
        return post("/ocr/form", withTenant(request));
    }

    public Map<String, Object> extractDocument(Map<String, Object> request) {
        return post("/ocr/document", withTenant(request));
    }

    public Map<String, Object> analyzeBottleneck(Map<String, Object> request) {
        Map<String, Object> payload = withTenant(request);
        Object workflow = payload.get("workflow");
        if (workflow instanceof Map<?, ?> workflowMap) {
            Map<String, Object> enrichedWorkflow = new LinkedHashMap<>();
            workflowMap.forEach((key, value) -> enrichedWorkflow.put(String.valueOf(key), value));
            enrichedWorkflow.put("tenant_empresa", currentEmpresa());
            payload.put("workflow", enrichedWorkflow);
        }
        return post("/simulation/bottleneck", payload);
    }

    private Map<String, Object> withTenant(Map<String, Object> request) {
        Map<String, Object> payload = new LinkedHashMap<>();
        if (request != null) {
            payload.putAll(request);
        }
        payload.put("tenant_empresa", currentEmpresa());
        return payload;
    }

    private String currentEmpresa() {
        return userService.getCurrentAuthenticatedUser()
                .map(User::getEmpresa)
                .filter(empresa -> !empresa.isBlank())
                .orElseThrow(() -> new ResponseStatusException(HttpStatusCode.valueOf(401), "No hay usuario autenticado"));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> get(String path) {
        try {
            return workflowAiRestClient.get()
                    .uri(path)
                    .retrieve()
                    .body(Map.class);
        } catch (Exception ex) {
            log.warn("No se pudo contactar workflow-ai-service en {}", path, ex);
            throw new ResponseStatusException(HttpStatusCode.valueOf(502), "Servicio IA no disponible");
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> post(String path, Map<String, Object> payload) {
        try {
            return workflowAiRestClient.post()
                    .uri(path)
                    .body(payload)
                    .retrieve()
                    .body(Map.class);
        } catch (Exception ex) {
            log.warn("Error al invocar workflow-ai-service en {}", path, ex);
            throw new ResponseStatusException(HttpStatusCode.valueOf(502), "Servicio IA no disponible");
        }
    }
}
