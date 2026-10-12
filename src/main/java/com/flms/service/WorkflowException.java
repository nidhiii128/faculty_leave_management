package com.flms.service;

/** A request that breaks a business rule (wrong owner, wrong status, bad dates). Shown to the user. */
public class WorkflowException extends RuntimeException {
    public WorkflowException(String message) {
        super(message);
    }
}
