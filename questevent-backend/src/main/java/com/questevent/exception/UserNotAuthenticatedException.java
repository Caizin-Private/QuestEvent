package com.questevent.exception;

import org.springframework.security.core.AuthenticationException;

public class UserNotAuthenticatedException extends AuthenticationException {

    public UserNotAuthenticatedException() {
        super("User not logged in");
    }
}
