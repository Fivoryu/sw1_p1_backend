package com.banco.workflow.repository;

import com.banco.workflow.model.DocumentUpload;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentUploadRepository extends MongoRepository<DocumentUpload, String> {
    List<DocumentUpload> findByProcessInstanceId(String processInstanceId);
    List<DocumentUpload> findByTaskId(String taskId);
}
