package com.banco.workflow.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Document(collection = "notifications")
public class Notification {

    @Id
    private String id;

    private String userId;

    private String title;
    private String body;

    private String type;

    private boolean read;

    private LocalDateTime createdAt;

    private LocalDateTime readAt;

    private String relatedId;
    private String metadata;

    private String priority;
    private String actionUrl;
}
