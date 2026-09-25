package com.matrimonyapp.backend.exception;

import com.matrimonyapp.backend.dto.error.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(AppException.class)
    public ResponseEntity<ApiErrorResponse> handleAppException(AppException ex, HttpServletRequest request) {
        String requestId = getOrCreateRequestId(request);
        log.warn("Application exception [{}]: {} (Path: {})", requestId, ex.getMessage(), request.getRequestURI());

        ApiErrorResponse response = new ApiErrorResponse(
                ex.getErrorCode().name(),
                ex.getMessage(),
                ex.getStatus().value(),
                request.getRequestURI(),
                requestId,
                ex.getFieldErrors()
        );
        return new ResponseEntity<>(response, ex.getStatus());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidationException(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String requestId = getOrCreateRequestId(request);
        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }

        String message = fieldErrors.isEmpty() ? "Request validation failed." :
                fieldErrors.values().iterator().next();

        ApiErrorResponse response = new ApiErrorResponse(
                ErrorCode.VALIDATION_FAILED.name(),
                message,
                HttpStatus.BAD_REQUEST.value(),
                request.getRequestURI(),
                requestId,
                fieldErrors
        );
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiErrorResponse> handleBadCredentials(BadCredentialsException ex, HttpServletRequest request) {
        String requestId = getOrCreateRequestId(request);
        ApiErrorResponse response = new ApiErrorResponse(
                ErrorCode.AUTH_INVALID_CREDENTIALS.name(),
                "Invalid email or password.",
                HttpStatus.UNAUTHORIZED.value(),
                request.getRequestURI(),
                requestId,
                null
        );
        return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiErrorResponse> handleAuthenticationException(AuthenticationException ex, HttpServletRequest request) {
        String requestId = getOrCreateRequestId(request);
        ApiErrorResponse response = new ApiErrorResponse(
                ErrorCode.UNAUTHORIZED.name(),
                "Authentication required or token expired.",
                HttpStatus.UNAUTHORIZED.value(),
                request.getRequestURI(),
                requestId,
                null
        );
        return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        String requestId = getOrCreateRequestId(request);
        ApiErrorResponse response = new ApiErrorResponse(
                ErrorCode.ACCESS_DENIED.name(),
                "Access denied. Insufficient permissions.",
                HttpStatus.FORBIDDEN.value(),
                request.getRequestURI(),
                requestId,
                null
        );
        return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleMalformedJson(HttpMessageNotReadableException ex, HttpServletRequest request) {
        String requestId = getOrCreateRequestId(request);
        ApiErrorResponse response = new ApiErrorResponse(
                ErrorCode.BAD_REQUEST.name(),
                "Malformed request body.",
                HttpStatus.BAD_REQUEST.value(),
                request.getRequestURI(),
                requestId,
                null
        );
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiErrorResponse> handleMaxUploadSize(MaxUploadSizeExceededException ex, HttpServletRequest request) {
        String requestId = getOrCreateRequestId(request);
        ApiErrorResponse response = new ApiErrorResponse(
                ErrorCode.VALIDATION_FAILED.name(),
                "File size exceeds maximum allowed upload limit (10MB).",
                HttpStatus.BAD_REQUEST.value(),
                request.getRequestURI(),
                requestId,
                null
        );
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
        String requestId = getOrCreateRequestId(request);
        ApiErrorResponse response = new ApiErrorResponse(
                ErrorCode.BAD_REQUEST.name(),
                "HTTP method " + ex.getMethod() + " is not supported for this endpoint.",
                HttpStatus.METHOD_NOT_ALLOWED.value(),
                request.getRequestURI(),
                requestId,
                null
        );
        return new ResponseEntity<>(response, HttpStatus.METHOD_NOT_ALLOWED);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGenericException(Exception ex, HttpServletRequest request) {
        String requestId = getOrCreateRequestId(request);
        log.error("Unhandled server exception [{}]: ", requestId, ex);

        ApiErrorResponse response = new ApiErrorResponse(
                ErrorCode.INTERNAL_ERROR.name(),
                "An unexpected error occurred. Please try again later.",
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                request.getRequestURI(),
                requestId,
                null
        );
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private String getOrCreateRequestId(HttpServletRequest request) {
        Object attr = request.getAttribute("X-Request-Id");
        if (attr != null) {
            return attr.toString();
        }
        String header = request.getHeader("X-Request-Id");
        if (header != null && !header.isBlank()) {
            return header.trim();
        }
        String generated = "req-" + UUID.randomUUID().toString().substring(0, 8);
        request.setAttribute("X-Request-Id", generated);
        return generated;
    }
}
