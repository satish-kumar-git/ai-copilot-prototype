package com.aiprototype.engine.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.List;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationErrors(
            MethodArgumentNotValidException ex, HttpServletRequest request) {

        String uri = request.getRequestURI();
        List<String> details = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .toList();

        log.warn("Validation failed [path={}]: {}", uri, details);

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.builder()
                        .status(400)
                        .error("VALIDATION_ERROR")
                        .message("Request validation failed")
                        .path(uri)
                        .timestamp(LocalDateTime.now())
                        .details(details)
                        .build());
    }

    @ExceptionHandler(InvalidRequirementException.class)
    public ResponseEntity<ErrorResponse> handleInvalidRequirement(
            InvalidRequirementException ex, HttpServletRequest request) {

        String uri = request.getRequestURI();
        log.warn("Invalid requirement [path={}]: {}", uri, ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(ErrorResponse.builder()
                        .status(422)
                        .error("INVALID_REQUIREMENT")
                        .message(ex.getMessage())
                        .path(uri)
                        .timestamp(LocalDateTime.now())
                        .build());
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleUnsupportedMediaType(
            HttpMediaTypeNotSupportedException ex, HttpServletRequest request) {

        String uri = request.getRequestURI();
        log.warn("Unsupported media type [path={}]: {}", uri, ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .body(ErrorResponse.builder()
                        .status(415)
                        .error("UNSUPPORTED_MEDIA_TYPE")
                        .message("Content-Type must be application/json")
                        .path(uri)
                        .timestamp(LocalDateTime.now())
                        .build());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(
            Exception ex, HttpServletRequest request) {

        String uri = request.getRequestURI();
        log.error("Unexpected error [path={}]", uri, ex);

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.builder()
                        .status(500)
                        .error("INTERNAL_ERROR")
                        .message("An unexpected error occurred")
                        .path(uri)
                        .timestamp(LocalDateTime.now())
                        .build());
    }
}
