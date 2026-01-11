package com.questevent.exception;

public class DuplicateActivitySubmissionException extends RuntimeException {
    public DuplicateActivitySubmissionException(String message) {
        super(message);
    }
}
