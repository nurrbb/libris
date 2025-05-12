package com.nurbb.libris.exception.handler;

import com.nurbb.libris.exception.NotFoundException;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(NotFoundException ex) {
        log.warn("NotFoundException -> {}", ex.getMessage());
        return buildError(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(BadCredentialsException ex) {
        log.warn("BadCredentialsException -> {}", ex.getMessage());
        return buildError(HttpStatus.UNAUTHORIZED, "Invalid e-mail or password");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("IllegalArgumentException -> {}", ex.getMessage());
        return buildError(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Object> handleAccessDenied(AccessDeniedException ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.FORBIDDEN.value());
        body.put("message", ex.getMessage());

        return new ResponseEntity<>(body, HttpStatus.FORBIDDEN);
    }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        List<String> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> fieldError.getField() + " " + fieldError.getDefaultMessage())
                .toList();

        log.warn("Validation Error -> {}", errors);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                new ErrorResponse(LocalDateTime.now(), 400, "Bad Request", errors.toString())
        );
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ErrorResponse> handleHandlerMethodValidation(HandlerMethodValidationException ex) {
        log.warn("HandlerMethodValidationException -> {}", ex.getMessage());
        return buildError(HttpStatus.BAD_REQUEST, "Validation failure");
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex) {
        log.warn("ConstraintViolationException -> {}", ex.getMessage());
        return buildError(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleAllUnhandled(Exception ex) {
        log.error("Unhandled Exception -> {}", ex.getMessage(), ex);
        return buildError(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred.");
    }

    private ResponseEntity<ErrorResponse> buildError(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(
                new ErrorResponse(LocalDateTime.now(), status.value(), status.getReasonPhrase(), message)
        );
    }

    @ExceptionHandler(com.nurbb.libris.exception.InvalidRequestException.class)
    public ResponseEntity<ErrorResponse> handleInvalidRequest(com.nurbb.libris.exception.InvalidRequestException ex) {
        log.warn("InvalidRequestException -> {}", ex.getMessage());
        return buildError(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(com.nurbb.libris.exception.QuotasFullException.class)
    public ResponseEntity<ErrorResponse> handleQuotaExceeded(com.nurbb.libris.exception.QuotasFullException ex) {
        log.warn("QuotasFullException -> {}", ex.getMessage());
        return buildError(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

}
