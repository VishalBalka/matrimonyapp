package com.matrimonyapp.backend.dto.security;

public class MfaSetupResponse {

    private String secret;
    private String message;

    public MfaSetupResponse() {
    }

    public MfaSetupResponse(String secret, String message) {
        this.secret = secret;
        this.message = message;
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
