package com.example.personal_finance_tracker.app.exceptions;

import org.springframework.security.core.AuthenticationException;


public class JwtAuthenticationException extends AuthenticationException {

    public JwtAuthenticationException(String msg) {
        super(msg);
    }
    public JwtAuthenticationException(String msg, Throwable cause) {
        super(msg, cause);
    }

}