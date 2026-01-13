package com.questevent.exception;

import org.springframework.security.core.AuthenticationException;

public class UnsupportedPrincipalException extends AuthenticationException {
    public UnsupportedPrincipalException(String principalType) {
        super("Unsupported principal type: " + principalType);
    }
}

