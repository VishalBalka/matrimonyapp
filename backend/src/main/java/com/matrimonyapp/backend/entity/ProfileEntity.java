package com.matrimonyapp.backend.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "profiles")
public class ProfileEntity {

    @Id
    @Column(name = "id", length = 64)
    private String id;

    @Column(name = "user_id", nullable = false, unique = true, length = 64)
    private String userId;

    @Column(name = "display_name", nullable = false, length = 150)
    private String displayName;

    @Column(name = "date_of_birth", nullable = false, length = 30)
    private String dateOfBirth;

    @Column(name = "gender", nullable = false, length = 50)
    private String gender;

    @Column(name = "country", length = 100)
    private String country;

    @Column(name = "state_province", length = 100)
    private String stateProvince;

    @Column(name = "city", nullable = false, length = 100)
    private String city;

    @Column(name = "bio", nullable = false, columnDefinition = "TEXT")
    private String bio = "";

    @Column(name = "phone_number", length = 50)
    private String phoneNumber;

    @Column(name = "photo_available", nullable = false)
    private boolean photoAvailable = false;

    @Column(name = "profession", length = 150)
    private String profession;

    @Column(name = "employer", length = 150)
    private String employer;

    @Column(name = "salary_range", length = 100)
    private String salaryRange;

    @Column(name = "education", length = 200)
    private String education;

    @Column(name = "skills", columnDefinition = "TEXT")
    private String skills;

    @Column(name = "linkedin_url", length = 255)
    private String linkedinUrl;

    @Column(name = "instagram_url", length = 255)
    private String instagramUrl;

    @Column(name = "facebook_url", length = 255)
    private String facebookUrl;

    @Column(name = "website_url", length = 255)
    private String websiteUrl;

    @Column(name = "profile_visibility", nullable = false, length = 50)
    private String profileVisibility = "PUBLIC";

    @Column(name = "show_phone", nullable = false)
    private boolean showPhone = false;

    @Column(name = "show_salary", nullable = false)
    private boolean showSalary = false;

    @Column(name = "show_social", nullable = false)
    private boolean showSocial = false;

    @Column(name = "background_check_status", nullable = false, length = 50)
    private String backgroundCheckStatus = "UNREQUESTED";

    @Column(name = "verification_status", nullable = false, length = 50)
    private String verificationStatus = "UNVERIFIED";

    @Column(name = "verified_at")
    private Instant verifiedAt;

    @Column(name = "profile_locked", nullable = false)
    private boolean profileLocked = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public ProfileEntity() {
    }

    @PreUpdate
    public void onPreUpdate() {
        this.updatedAt = Instant.now();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public String getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(String dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getStateProvince() {
        return stateProvince;
    }

    public void setStateProvince(String stateProvince) {
        this.stateProvince = stateProvince;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public boolean isPhotoAvailable() {
        return photoAvailable;
    }

    public void setPhotoAvailable(boolean photoAvailable) {
        this.photoAvailable = photoAvailable;
    }

    public String getProfession() {
        return profession;
    }

    public void setProfession(String profession) {
        this.profession = profession;
    }

    public String getEmployer() {
        return employer;
    }

    public void setEmployer(String employer) {
        this.employer = employer;
    }

    public String getSalaryRange() {
        return salaryRange;
    }

    public void setSalaryRange(String salaryRange) {
        this.salaryRange = salaryRange;
    }

    public String getEducation() {
        return education;
    }

    public void setEducation(String education) {
        this.education = education;
    }

    public String getSkills() {
        return skills;
    }

    public void setSkills(String skills) {
        this.skills = skills;
    }

    public String getLinkedinUrl() {
        return linkedinUrl;
    }

    public void setLinkedinUrl(String linkedinUrl) {
        this.linkedinUrl = linkedinUrl;
    }

    public String getInstagramUrl() {
        return instagramUrl;
    }

    public void setInstagramUrl(String instagramUrl) {
        this.instagramUrl = instagramUrl;
    }

    public String getFacebookUrl() {
        return facebookUrl;
    }

    public void setFacebookUrl(String facebookUrl) {
        this.facebookUrl = facebookUrl;
    }

    public String getWebsiteUrl() {
        return websiteUrl;
    }

    public void setWebsiteUrl(String websiteUrl) {
        this.websiteUrl = websiteUrl;
    }

    public String getProfileVisibility() {
        return profileVisibility;
    }

    public void setProfileVisibility(String profileVisibility) {
        this.profileVisibility = profileVisibility;
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

    public String getBackgroundCheckStatus() {
        return backgroundCheckStatus;
    }

    public void setBackgroundCheckStatus(String backgroundCheckStatus) {
        this.backgroundCheckStatus = backgroundCheckStatus;
    }

    public String getVerificationStatus() {
        return verificationStatus;
    }

    public void setVerificationStatus(String verificationStatus) {
        this.verificationStatus = verificationStatus;
    }

    public Instant getVerifiedAt() {
        return verifiedAt;
    }

    public void setVerifiedAt(Instant verifiedAt) {
        this.verifiedAt = verifiedAt;
    }

    public boolean isProfileLocked() {
        return profileLocked;
    }

    public void setProfileLocked(boolean profileLocked) {
        this.profileLocked = profileLocked;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
