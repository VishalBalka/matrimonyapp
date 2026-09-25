package com.matrimonyapp.backend.dto.security;

public class SecurityStateResponse {

    private boolean mfaEnabled;
    private String visibility;
    private boolean profileLocked;
    private boolean showPhone;
    private boolean showSalary;
    private boolean showSocial;

    public SecurityStateResponse() {
    }

    public SecurityStateResponse(boolean mfaEnabled, String visibility, boolean profileLocked, boolean showPhone, boolean showSalary, boolean showSocial) {
        this.mfaEnabled = mfaEnabled;
        this.visibility = visibility;
        this.profileLocked = profileLocked;
        this.showPhone = showPhone;
        this.showSalary = showSalary;
        this.showSocial = showSocial;
    }

    public boolean isMfaEnabled() {
        return mfaEnabled;
    }

    public void setMfaEnabled(boolean mfaEnabled) {
        this.mfaEnabled = mfaEnabled;
    }

    public String getVisibility() {
        return visibility;
    }

    public void setVisibility(String visibility) {
        this.visibility = visibility;
    }

    public boolean isProfileLocked() {
        return profileLocked;
    }

    public void setProfileLocked(boolean profileLocked) {
        this.profileLocked = profileLocked;
    }

    public boolean isShowPhone() {
        return showPhone;
    }

    public void setShowPhone(boolean showPhone) {
        this.showPhone = showPhone;
    }

    public boolean isShowSalary() {
        return showSalary;
    }

    public void setShowSalary(boolean showSalary) {
        this.showSalary = showSalary;
    }

    public boolean isShowSocial() {
        return showSocial;
    }

    public void setShowSocial(boolean showSocial) {
        this.showSocial = showSocial;
    }
}
