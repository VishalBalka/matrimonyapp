package com.matrimonyapp.backend.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class MfaVerifyRequest {

    @NotBlank(message = "MFA challenge identifier is required.")
    private String challengeId;

    @NotBlank(message = "Verification code is required.")
    @Pattern(regexp = "^[0-9]{6}$", message = "Verification code must be exactly 6 digits.")
    private String code;

    public MfaVerifyRequest() {
    }

    public MfaVerifyRequest(String challengeId, String code) {
        this.challengeId = challengeId;
        this.code = code;
    }

    public String getChallengeId() {
        return challengeId;
    }

    public void setChallengeId(String challengeId) {
        this.challengeId = challengeId;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}
