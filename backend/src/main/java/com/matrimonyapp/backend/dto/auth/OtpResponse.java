package com.matrimonyapp.backend.dto.auth;

public class OtpResponse {

    private String message;
    private int cooldownSeconds;

    public OtpResponse() {
    }

    public OtpResponse(String message, int cooldownSeconds) {
        this.message = message;
        this.cooldownSeconds = cooldownSeconds;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getCooldownSeconds() {
        return cooldownSeconds;
    }

    public void setCooldownSeconds(int cooldownSeconds) {
        this.cooldownSeconds = cooldownSeconds;
    }
}
