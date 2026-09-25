package com.matrimonyapp.backend.dto.security;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ReportRequest {

    @NotBlank(message = "Reason is required.")
    @Size(max = 100)
    private String reason;

    @Size(max = 2000)
    private String details;

    public ReportRequest() {
    }

    public ReportRequest(String reason, String details) {
        this.reason = reason;
        this.details = details;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }
}
