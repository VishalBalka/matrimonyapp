package com.matrimonyapp.backend.dto.admin;

import jakarta.validation.constraints.NotBlank;

public class StatusRequest {

    @NotBlank(message = "Status is required.")
    private String status;

    public StatusRequest() {
    }

    public StatusRequest(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
