package com.banco.workflow.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class WorkflowAiConfig {

    @Bean
    public RestClient workflowAiRestClient(
            RestClient.Builder builder,
            @Value("${workflow.ai.base-url:http://localhost:8090}") String baseUrl) {
        return builder.baseUrl(baseUrl).build();
    }
}
