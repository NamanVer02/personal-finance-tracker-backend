package com.example.personal_finance_tracker.app.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Component
public class AuthEntryPointJwt implements AuthenticationEntryPoint {

    private static final Logger logger = LoggerFactory.getLogger(AuthEntryPointJwt.class);
    private static final String MESSAGE = "message";

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException {
        logger.error("Unauthorized error: {}", authException.getMessage(), authException);

        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

        final Map<String, Object> body = new HashMap<>();
        body.put("status", HttpServletResponse.SC_UNAUTHORIZED);
        body.put("error", "Unauthorized");
        
        // Provide more specific error messages based on exception type
        if (authException instanceof com.example.personal_finance_tracker.app.exceptions.JwtAuthenticationException) {
            body.put(MESSAGE, "JWT authentication failed: " + authException.getMessage());
        } else if (authException instanceof org.springframework.security.authentication.BadCredentialsException) {
            body.put(MESSAGE, "Invalid username or password");
        } else if (authException instanceof org.springframework.security.authentication.LockedException) {
            body.put(MESSAGE, "Account is locked. Please try again later");
        } else if (authException instanceof org.springframework.security.authentication.DisabledException) {
            body.put(MESSAGE, "Account is disabled. Please contact support");
        } else if (authException instanceof org.springframework.security.authentication.AccountExpiredException) {
            body.put(MESSAGE, "Account has expired. Please contact support");
        } else if (authException instanceof org.springframework.security.authentication.CredentialsExpiredException) {
            body.put(MESSAGE, "Credentials have expired. Please reset your password");
        } else {
            body.put(MESSAGE, authException.getMessage());
        }
        
        body.put("path", request.getServletPath());
        body.put("timestamp", new java.util.Date().toString());

        final ObjectMapper mapper = new ObjectMapper();
        mapper.writeValue(response.getOutputStream(), body);
    }
}
