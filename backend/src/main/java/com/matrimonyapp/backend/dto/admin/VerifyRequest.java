package com.matrimonyapp.backend.dto.admin;

public class VerifyRequest {

    private boolean verified;

    public VerifyRequest() {
    }

    public VerifyRequest(boolean verified) {
        this.verified = verified;
    }

    public boolean isVerified() {
        return verified;
    }

    public void setVerified(boolean verified) {
        this.verified = verified;
    }
}
