package com.banco.workflow.graphql.input;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CompleteTaskInput {
    private String taskId;
    private Map<String, Object> result;
    private String comments;
}
