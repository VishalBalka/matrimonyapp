package com.matrimonyapp.backend.dto.auth;

public class MeResponse {

    private String userId;
    private String displayName;
    private String email;
    private String role;
    private boolean mfaEnabled;

    public MeResponse() {
    }

    public MeResponse(String userId, String displayName, String email, String role, boolean mfaEnabled) {
        this.userId = userId;
        this.displayName = displayName;
        this.email = email;
        this.role = role;
        this.mfaEnabled = mfaEnabled;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public boolean isMfaEnabled() {
        return mfaEnabled;
    }

    public void setMfaEnabled(boolean mfaEnabled) {
        this.mfaEnabled = mfaEnabled;
    }
}
