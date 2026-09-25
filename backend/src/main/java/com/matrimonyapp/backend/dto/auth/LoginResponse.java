package com.matrimonyapp.backend.dto.auth;

public class LoginResponse {

    private String token;
    private String userId;
    private String displayName;
    private String email;
    private String expiresAt;
    private boolean mfaRequired;
    private String mfaChallengeId;
    private String role;

    public LoginResponse() {
    }

    public LoginResponse(String token, String userId, String displayName, String email, String expiresAt, boolean mfaRequired, String mfaChallengeId, String role) {
        this.token = token;
        this.userId = userId;
        this.displayName = displayName;
        this.email = email;
        this.expiresAt = expiresAt;
        this.mfaRequired = mfaRequired;
        this.mfaChallengeId = mfaChallengeId;
        this.role = role;
    }

    public static LoginResponse mfaChallenge(String userId, String email, String mfaChallengeId) {
        return new LoginResponse("", userId, "", email, "", true, mfaChallengeId, "");
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
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

    public String getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(String expiresAt) {
        this.expiresAt = expiresAt;
    }

    public boolean isMfaRequired() {
        return mfaRequired;
    }

    public void setMfaRequired(boolean mfaRequired) {
        this.mfaRequired = mfaRequired;
    }

    public String getMfaChallengeId() {
        return mfaChallengeId;
    }

    public void setMfaChallengeId(String mfaChallengeId) {
        this.mfaChallengeId = mfaChallengeId;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}
