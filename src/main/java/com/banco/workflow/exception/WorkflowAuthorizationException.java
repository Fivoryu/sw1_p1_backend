package com.banco.workflow.exception;

public class WorkflowAuthorizationException extends RuntimeException {
    public WorkflowAuthorizationException(String message) {
        super(message);
    }
}
