package com.technotes.notes.exception;

import com.technotes.notes.dto.common.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleResourceNotFound(
            ResourceNotFoundException ex,
            HttpServletRequest request) {

        return buildResponse(
                HttpStatus.NOT_FOUND,
                "RESOURCE_NOT_FOUND",
                ex.getMessage(),
                request.getRequestURI()
        );
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ApiError> handleDuplicateResource(
            DuplicateResourceException ex,
            HttpServletRequest request) {

        return buildResponse(
                HttpStatus.CONFLICT,
                "DUPLICATE_RESOURCE",
                ex.getMessage(),
                request.getRequestURI()
        );
    }

    @ExceptionHandler(InvalidStateException.class)
    public ResponseEntity<ApiError> handleInvalidState(
            InvalidStateException ex,
            HttpServletRequest request) {

        return buildResponse(
                HttpStatus.CONFLICT,
                "INVALID_STATE",
                ex.getMessage(),
                request.getRequestURI()
        );
    }

    @ExceptionHandler(MissingIfMatchException.class)
    public ResponseEntity<ApiError> handleMissingIfMatch(
            MissingIfMatchException ex,
            HttpServletRequest request) {

        return buildResponse(
                HttpStatus.PRECONDITION_REQUIRED,
                "MISSING_IF_MATCH",
                ex.getMessage(),
                request.getRequestURI()
        );
    }

    @ExceptionHandler(StaleVersionException.class)
    public ResponseEntity<ApiError> handleStaleVersion(
            StaleVersionException ex,
            HttpServletRequest request) {

        return buildResponse(
                HttpStatus.PRECONDITION_FAILED,
                "STALE_VERSION",
                ex.getMessage(),
                request.getRequestURI()
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {

        String message = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error ->
                        error.getField()
                                + ": "
                                + error.getDefaultMessage()
                )
                .collect(Collectors.joining(", "));

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "VALIDATION_FAILED",
                message,
                request.getRequestURI()
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArgument(
            IllegalArgumentException ex,
            HttpServletRequest request) {

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "INVALID_REQUEST",
                ex.getMessage(),
                request.getRequestURI()
        );
    }

    private ResponseEntity<ApiError> buildResponse(
            HttpStatus status,
            String code,
            String message,
            String path) {

        ApiError error = new ApiError();

        error.setTimestamp(Instant.now());
        error.setStatus(status.value());
        error.setCode(code);
        error.setMessage(message);
        error.setPath(path);

        return ResponseEntity
                .status(status)
                .body(error);
    }
}