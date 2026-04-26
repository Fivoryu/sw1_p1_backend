package com.banco.workflow.service;

import com.banco.workflow.model.ProcessInstance;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;

@Service
public class WorkflowHistoryService {

    public void record(
            ProcessInstance instance,
            String nodeId,
            String status,
            String nodeType,
            String completedByUserId,
            Map<String, Object> payload,
            String errorMessage
    ) {
        if (instance.getHistory() == null) {
            instance.setHistory(new ArrayList<>());
        }

        ProcessInstance.HistoryEntry entry = new ProcessInstance.HistoryEntry();
        entry.setId(UUID.randomUUID().toString());
        entry.setProcessInstanceId(instance.getId());
        entry.setNodeId(nodeId);
        entry.setNodeType(nodeType);
        entry.setNodeName(nodeId == null ? "Proceso" : nodeId);
        entry.setTaskId(nodeId);
        entry.setTaskName(nodeId == null ? "Proceso" : nodeId);
        entry.setCompletedByUserId(completedByUserId);
        entry.setTimestamp(LocalDateTime.now());
        entry.setStatus(status);
        entry.setTaskData(payload);
        entry.setTaskResult(payload);
        entry.setErrorMessage(errorMessage);
        instance.getHistory().add(entry);
    }
}
