package com.matrimonyapp.backend.dto.auth;

public class OtpVerifyResponse {

    private boolean verified;
    private String verificationToken;
    private String message;

    public OtpVerifyResponse() {
    }

    public OtpVerifyResponse(boolean verified, String verificationToken, String message) {
        this.verified = verified;
        this.verificationToken = verificationToken;
        this.message = message;
    }

    public boolean isVerified() {
        return verified;
    }

    public void setVerified(boolean verified) {
        this.verified = verified;
    }

    public String getVerificationToken() {
        return verificationToken;
    }

    public void setVerificationToken(String verificationToken) {
        this.verificationToken = verificationToken;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
