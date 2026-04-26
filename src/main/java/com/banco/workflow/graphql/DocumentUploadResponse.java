package com.banco.workflow.graphql;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.banco.workflow.model.DocumentUpload;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DocumentUploadResponse {
    private boolean success;
    private String message;
    private DocumentUpload document;
}
