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
@Document(collection = "document_uploads")
public class DocumentUpload {

    @Id
    private String id;

    private String processInstanceId;
    private String taskId;
    private String formFieldKey;

    private String fileName;
    private String fileType;

    private String url;

    private String mimeType;

    private Long fileSize;

    private String uploadedBy;

    private LocalDateTime uploadedAt;

    private String status;

    private String documentType;

    private String validationResult;
    private String ocrStatus;

    private String validationNotes;

    private String storageLocation;
    private String rejectionReason;
}
