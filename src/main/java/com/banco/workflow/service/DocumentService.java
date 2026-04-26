package com.banco.workflow.service;

import com.banco.workflow.model.DocumentUpload;
import com.banco.workflow.repository.DocumentUploadRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.io.IOException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentService {

    private final DocumentUploadRepository documentUploadRepository;

    public Optional<DocumentUpload> getDocumentById(String id) {
        return documentUploadRepository.findById(id);
    }

    public List<DocumentUpload> getDocumentsByProcessId(String processInstanceId) {
        return documentUploadRepository.findByProcessInstanceId(processInstanceId);
    }

    public List<DocumentUpload> getDocumentsByTaskId(String taskId) {
        return documentUploadRepository.findByTaskId(taskId);
    }

    public DocumentUpload saveDocument(String processInstanceId, String fileName, byte[] fileBytes, String mimeType) throws IOException {
        return saveDocument(processInstanceId, null, null, fileName, fileBytes, mimeType);
    }

    public DocumentUpload saveDocument(String processInstanceId, String taskId, String formFieldKey, String fileName, byte[] fileBytes, String mimeType) throws IOException {
        String url = "/api/v1/documents/" + UUID.randomUUID() + "/" + fileName;
        DocumentUpload documentUpload = DocumentUpload.builder()
                .id(UUID.randomUUID().toString())
                .processInstanceId(processInstanceId)
                .taskId(taskId)
                .formFieldKey(formFieldKey)
                .fileName(fileName)
                .fileType(resolveFileType(fileName))
                .url(url)
                .mimeType(mimeType)
                .fileSize((long) fileBytes.length)
                .uploadedBy("system")
                .uploadedAt(LocalDateTime.now())
                .status("UPLOADED")
                .documentType("GENERAL")
                .validationResult("PENDING")
                .ocrStatus("NOT_REQUESTED")
                .storageLocation("memory")
                .build();
        return documentUploadRepository.save(documentUpload);
    }

    public void validateDocument(String documentId) {
        DocumentUpload doc = documentUploadRepository.findById(documentId).orElse(null);
        if (doc != null) {
            doc.setStatus("VALIDATED");
            doc.setValidationResult("APPROVED");
            documentUploadRepository.save(doc);
        }
    }

    public void markPendingValidation(String documentId) {
        DocumentUpload doc = documentUploadRepository.findById(documentId).orElse(null);
        if (doc != null) {
            doc.setStatus("PENDING_VALIDATION");
            doc.setValidationResult("PENDING");
            documentUploadRepository.save(doc);
        }
    }

    public void rejectDocument(String documentId, String reason) {
        DocumentUpload doc = documentUploadRepository.findById(documentId).orElse(null);
        if (doc != null) {
            doc.setStatus("REJECTED");
            doc.setValidationResult("REJECTED");
            doc.setRejectionReason(reason);
            documentUploadRepository.save(doc);
        }
    }

    private String resolveFileType(String fileName) {
        int index = fileName.lastIndexOf('.');
        return index > -1 ? fileName.substring(index + 1).toUpperCase() : "BIN";
    }
}
