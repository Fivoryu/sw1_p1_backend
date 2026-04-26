package com.banco.workflow.graphql;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.banco.workflow.model.Task;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CompleteTaskResponse {
    private boolean success;
    private String message;
    private Task task;
    private List<Task> nextTasks;
}
