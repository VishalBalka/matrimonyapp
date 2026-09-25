package com.matrimonyapp.backend.dto.security;

import jakarta.validation.constraints.NotBlank;

public class CodeRequest {

    @NotBlank(message = "Code is required.")
    private String code;

    public CodeRequest() {
    }

    public CodeRequest(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}
