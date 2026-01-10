package com.questevent.exception;

public class ActivityAlreadyCompletedException extends RuntimeException {
    public ActivityAlreadyCompletedException(String message) {
        super(message);
    }
}
