package com.matrimonyapp.backend.exception;

import org.springframework.http.HttpStatus;

import java.util.Collections;
import java.util.Map;

public class AppException extends RuntimeException {

    private final ErrorCode errorCode;
    private final HttpStatus status;
    private final Map<String, String> fieldErrors;

    public AppException(ErrorCode errorCode) {
        super(errorCode.getDefaultMessage());
        this.errorCode = errorCode;
        this.status = errorCode.getHttpStatus();
        this.fieldErrors = Collections.emptyMap();
    }

    public AppException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.status = errorCode.getHttpStatus();
        this.fieldErrors = Collections.emptyMap();
    }

    public AppException(ErrorCode errorCode, String message, Map<String, String> fieldErrors) {
        super(message);
        this.errorCode = errorCode;
        this.status = errorCode.getHttpStatus();
        this.fieldErrors = fieldErrors != null ? fieldErrors : Collections.emptyMap();
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public Map<String, String> getFieldErrors() {
        return fieldErrors;
    }
}
