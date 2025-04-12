package com.example.personal_finance_tracker.app.exceptions;

import org.springframework.security.core.AuthenticationException;

/**
 * Custom exception for JWT authentication errors.
 * Extends Spring Security's AuthenticationException to maintain compatibility with security framework.
 */
public class JwtAuthenticationException extends AuthenticationException {

    /**
     * Constructs a JwtAuthenticationException with the specified message.
     *
     * @param msg the detail message
     */
    public JwtAuthenticationException(String msg) {
        super(msg);
    }

    /**
     * Constructs a JwtAuthenticationException with the specified message and root cause.
     *
     * @param msg the detail message
     * @param cause the root cause
     */
    public JwtAuthenticationException(String msg, Throwable cause) {
        super(msg, cause);
    }
}