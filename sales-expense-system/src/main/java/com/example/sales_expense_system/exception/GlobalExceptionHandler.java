package com.example.sales_expense_system.exception;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.example.sales_expense_system.dto.response.ApiErrorResponse;

import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {

        Map<String, String> validationErrors = new LinkedHashMap<>();

        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            validationErrors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }

        ApiErrorResponse response = new ApiErrorResponse();
        response.setTimestamp(LocalDateTime.now());
        response.setStatus(HttpStatus.BAD_REQUEST.value());
        response.setError("Validation Failed");
        response.setMessage("Request validation failed");
        response.setPath(request.getRequestURI());
        response.setValidationErrors(validationErrors);

        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex,
            HttpServletRequest request) {

        ApiErrorResponse response = buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                "Malformed JSON request",
                request.getRequestURI()
        );

        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex,
            HttpServletRequest request) {

        ApiErrorResponse response = buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                "Invalid request parameter: " + ex.getName(),
                request.getRequestURI()
        );

        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleDataIntegrityViolation(
            DataIntegrityViolationException ex,
            HttpServletRequest request) {

        ApiErrorResponse response = buildErrorResponse(
                HttpStatus.CONFLICT,
                "Database constraint violation",
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    @ExceptionHandler(AuthorizationDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAccessDenied(
            AuthorizationDeniedException ex,
            HttpServletRequest request) {

        ApiErrorResponse response = buildErrorResponse(
                HttpStatus.FORBIDDEN,
                "Access denied",
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNoResourceFound(
            NoResourceFoundException ex,
            HttpServletRequest request) {

        ApiErrorResponse response = buildErrorResponse(
                HttpStatus.NOT_FOUND,
                "Resource not found",
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiErrorResponse> handleApiException(
            ApiException ex,
            HttpServletRequest request) {
            
        ApiErrorResponse response = buildErrorResponse(
                ex.getStatus(),
                ex.getMessage(),
                request.getRequestURI()
        );
            
        return ResponseEntity.status(ex.getStatus()).body(response);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiErrorResponse> handleRuntimeException(
            RuntimeException ex,
            HttpServletRequest request) {

        HttpStatus status = resolveRuntimeStatus(ex.getMessage());

        ApiErrorResponse response = buildErrorResponse(
                status,
                ex.getMessage() != null ? ex.getMessage() : "Unexpected error occurred",
                request.getRequestURI()
        );

        return ResponseEntity.status(status).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGeneralException(
            Exception ex,
            HttpServletRequest request) {

        ApiErrorResponse response = buildErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected internal server error occurred",
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    private ApiErrorResponse buildErrorResponse(HttpStatusCode status, String message, String path) {
        ApiErrorResponse response = new ApiErrorResponse();
        response.setTimestamp(LocalDateTime.now());
        response.setStatus(status.value());
        response.setError(resolveErrorLabel(status.value()));
        response.setMessage(message);
        response.setPath(path);
        response.setValidationErrors(null);
        return response;
    }

    private String resolveErrorLabel(int status) {
        return switch (status) {
            case 400 -> "Bad Request";
            case 401 -> "Unauthorized";
            case 403 -> "Forbidden";
            case 404 -> "Not Found";
            case 409 -> "Conflict";
            case 500 -> "Internal Server Error";
            default -> "Error";
        };
    }

    private HttpStatus resolveRuntimeStatus(String message) {
        if (message == null) {
            return HttpStatus.BAD_REQUEST;
        }

        String lower = message.toLowerCase();

        if (lower.contains("not found")) {
            return HttpStatus.NOT_FOUND;
        }

        if (lower.contains("already exists")
                || lower.contains("already locked")
                || lower.contains("already unlocked")
                || lower.contains("already voided")
                || lower.contains("overlaps")
                || lower.contains("duplicate")
                || lower.contains("constraint")) {
            return HttpStatus.CONFLICT;
        }

        if (lower.contains("access denied")
                || lower.contains("permission")) {
            return HttpStatus.FORBIDDEN;
        }

        return HttpStatus.BAD_REQUEST;
    }
}