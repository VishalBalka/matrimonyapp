package com.matrimonyapp.backend.dto.security;

public class PrivacyUpdateRequest {

    private String visibility = "PUBLIC";
    private boolean profileLocked = false;
    private boolean showPhone = false;
    private boolean showSalary = false;
    private boolean showSocial = false;

    public PrivacyUpdateRequest() {
    }

    public PrivacyUpdateRequest(String visibility, boolean profileLocked, boolean showPhone, boolean showSalary, boolean showSocial) {
        this.visibility = visibility;
        this.profileLocked = profileLocked;
        this.showPhone = showPhone;
        this.showSalary = showSalary;
        this.showSocial = showSocial;
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
