package com.questevent.exception;

public class UserNotRegisteredForActivityException extends RuntimeException {
    public UserNotRegisteredForActivityException(String message) {
        super(message);
    }
}
