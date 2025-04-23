package com.example.personal_finance_tracker.app.exceptions;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import jakarta.validation.ConstraintViolationException;
import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    private ResponseEntity<Object> buildErrorResponse(String message, String error, HttpStatus status) {
        Map<String, Object> body = new HashMap<>();
        body.put("message", message);
        body.put("error", error);
        return new ResponseEntity<>(body, status);
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<Object> handleOptimisticLockingFailure(ObjectOptimisticLockingFailureException ex) {
        return buildErrorResponse(
            "The data was modified by another user. Please refresh and try again.",
            "Concurrent Modification Error",
            HttpStatus.CONFLICT
        );
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Object> handleResourceNotFound(ResourceNotFoundException ex) {
        return buildErrorResponse(ex.getMessage(), "Resource Not Found", HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<Object> handleValidation(ValidationException ex) {
        return buildErrorResponse(ex.getMessage(), "Validation Error", HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(FileProcessingException.class)
    public ResponseEntity<Object> handleFileProcessing(FileProcessingException ex) {
        return buildErrorResponse(ex.getMessage(), "File Processing Error", HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(JwtAuthenticationException.class)
    public ResponseEntity<Object> handleJwtAuthentication(JwtAuthenticationException ex) {
        return buildErrorResponse(ex.getMessage(), "Authentication Error", HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler({BadCredentialsException.class, UsernameNotFoundException.class})
    public ResponseEntity<Object> handleAuthenticationFailure(Exception ex) {
        String message = ex.getMessage() != null ? ex.getMessage() : "Invalid username or password";
        return buildErrorResponse(message, "Authentication Error", HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(LockedException.class)
    public ResponseEntity<Object> handleAccountLocked(LockedException ex) {
        return buildErrorResponse("Account is locked", "Authentication Error", HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler({MalformedJwtException.class, SignatureException.class})
    public ResponseEntity<Object> handleInvalidToken(Exception ex) {
        return buildErrorResponse("Invalid token", "Authentication Error", HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(ExpiredJwtException.class)
    public ResponseEntity<Object> handleExpiredToken(ExpiredJwtException ex) {
        return buildErrorResponse("Token has expired", "Authentication Error", HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Object> handleAccessDenied(AccessDeniedException ex) {
        return buildErrorResponse("Access denied", "Authorization Error", HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Object> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException ex) {
        return buildErrorResponse("File size exceeds maximum allowed size", "File Upload Error", HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        Map<String, Object> body = new HashMap<>();
        Map<String, String> errors = new HashMap<>();

        ex.getBindingResult().getFieldErrors().forEach(error -> 
            errors.put(error.getField(), error.getDefaultMessage())
        );

        body.put("message", "Validation failed");
        body.put("error", "Validation Error");
        body.put("details", errors);

        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Object> handleConstraintViolation(ConstraintViolationException ex) {
        Map<String, Object> body = new HashMap<>();
        Map<String, String> errors = new HashMap<>();

        ex.getConstraintViolations().forEach(violation -> {
            String path = violation.getPropertyPath().toString();
            String field = path.substring(path.lastIndexOf('.') + 1);
            errors.put(field, violation.getMessage());
        });

        body.put("message", "Validation failed");
        body.put("error", "Validation Error");
        body.put("details", errors);

        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleGenericException(Exception ex) {
        return buildErrorResponse(
            "An unexpected error occurred",
            "Internal Server Error",
            HttpStatus.INTERNAL_SERVER_ERROR
        );
    }
}