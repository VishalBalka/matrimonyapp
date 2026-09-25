package com.matrimonyapp.backend.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class OtpVerifyRequest {

    @NotBlank(message = "Email is required.")
    @Email(message = "Please enter a valid email address.")
    private String email;

    @NotBlank(message = "Verification code is required.")
    @Pattern(regexp = "^[0-9]{6}$", message = "Verification code must be exactly 6 digits.")
    private String code;

    @NotBlank(message = "Purpose is required.")
    private String purpose = "REGISTRATION_VERIFICATION";

    public OtpVerifyRequest() {
    }

    public OtpVerifyRequest(String email, String code, String purpose) {
        this.email = email;
        this.code = code;
        this.purpose = purpose;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getPurpose() {
        return purpose;
    }

    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }
}
