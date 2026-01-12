package com.questevent.exception;

public class SubmissionAlreadyExistsException extends RuntimeException {
    public SubmissionAlreadyExistsException(String message) {
        super(message);
    }
}
