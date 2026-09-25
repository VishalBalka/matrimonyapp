package com.matrimonyapp.backend.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "Input syntax or constraint violation."),
    BAD_REQUEST(HttpStatus.BAD_REQUEST, "Malformed JSON or invalid parameter syntax."),
    OTP_INVALID(HttpStatus.BAD_REQUEST, "Invalid verification code."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "Missing or invalid authentication token."),
    AUTH_INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "Unable to authenticate with the supplied credentials."),
    SESSION_EXPIRED(HttpStatus.UNAUTHORIZED, "Your session has expired. Please sign in again."),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "Insufficient role or unauthorized resource access."),
    ACCOUNT_LOCKED(HttpStatus.FORBIDDEN, "Account has been locked by administrator or rate protection."),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "Requested resource does not exist."),
    CONFLICT(HttpStatus.CONFLICT, "Resource already exists with the supplied identifier."),
    OTP_EXPIRED(HttpStatus.GONE, "Verification code has expired."),
    OTP_TOO_MANY_ATTEMPTS(HttpStatus.TOO_MANY_REQUESTS, "Exceeded maximum attempts for OTP code."),
    RATE_LIMITED(HttpStatus.TOO_MANY_REQUESTS, "Too many attempts. Please try again later."),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "An internal server error occurred.");

    private final HttpStatus httpStatus;
    private final String defaultMessage;

    ErrorCode(HttpStatus httpStatus, String defaultMessage) {
        this.httpStatus = httpStatus;
        this.defaultMessage = defaultMessage;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public String getDefaultMessage() {
        return defaultMessage;
    }
}
