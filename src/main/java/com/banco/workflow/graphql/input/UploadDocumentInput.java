package com.banco.workflow.graphql.input;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UploadDocumentInput {
    private String processInstanceId;
    private String fileName;
    private String fileData; // Base64 encoded
    private String mimeType;
}
