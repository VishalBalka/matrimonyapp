package com.matrimonyapp.backend.dto.error;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiErrorResponse {

    private String code;
    private String message;
    private int status;
    private String timestamp;
    private String path;
    private String requestId;
    private Map<String, String> fieldErrors;
    private ErrorDetail error;

    public ApiErrorResponse() {
        this.timestamp = Instant.now().toString();
    }

    public ApiErrorResponse(String code, String message, int status, String path, String requestId, Map<String, String> fieldErrors) {
        this.code = code;
        this.message = message;
        this.status = status;
        this.timestamp = Instant.now().toString();
        this.path = path;
        this.requestId = requestId;
        this.fieldErrors = (fieldErrors != null && !fieldErrors.isEmpty()) ? fieldErrors : null;
        this.error = new ErrorDetail(code, message, requestId);
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public Map<String, String> getFieldErrors() {
        return fieldErrors;
    }

    public void setFieldErrors(Map<String, String> fieldErrors) {
        this.fieldErrors = fieldErrors;
    }

    public ErrorDetail getError() {
        return error;
    }

    public void setError(ErrorDetail error) {
        this.error = error;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ErrorDetail {
        private String code;
        private String message;
        private String requestId;

        public ErrorDetail() {
        }

        public ErrorDetail(String code, String message, String requestId) {
            this.code = code;
            this.message = message;
            this.requestId = requestId;
        }

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public String getRequestId() {
            return requestId;
        }

        public void setRequestId(String requestId) {
            this.requestId = requestId;
        }
    }
}
