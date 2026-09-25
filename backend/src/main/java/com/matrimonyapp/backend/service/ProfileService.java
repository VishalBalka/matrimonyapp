package com.matrimonyapp.backend.service;

import com.matrimonyapp.backend.dto.profile.ProfileResponse;
import com.matrimonyapp.backend.dto.profile.ProfileUpdateRequest;
import com.matrimonyapp.backend.entity.ProfileEntity;
import com.matrimonyapp.backend.exception.AppException;
import com.matrimonyapp.backend.exception.ErrorCode;
import com.matrimonyapp.backend.repository.ProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProfileService {

    private final ProfileRepository profileRepository;
    private final AuditService auditService;

    public ProfileService(ProfileRepository profileRepository, AuditService auditService) {
        this.profileRepository = profileRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public ProfileResponse getOwnerProfile(String userId) {
        ProfileEntity profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Profile not found for this account."));
        return toProfileResponse(profile);
    }

    @Transactional
    public ProfileResponse updateProfile(String userId, ProfileUpdateRequest request, String clientIp, String requestId) {
        ProfileEntity profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Profile not found for this account."));

        if (profile.isProfileLocked()) {
            throw new AppException(ErrorCode.ACCESS_DENIED, "Profile is locked by administrative policy and cannot be edited.");
        }

        profile.setDisplayName(request.getDisplayName().trim());
        profile.setDateOfBirth(request.getDateOfBirth().trim());
        profile.setGender(request.getGender().trim());
        profile.setCountry(request.getCountry());
        profile.setStateProvince(request.getStateProvince());
        profile.setCity(request.getCity().trim());
        profile.setBio(request.getBio() != null ? request.getBio().trim() : "");
        profile.setPhoneNumber(request.getPhoneNumber());
        profile.setProfession(request.getProfession());
        profile.setEmployer(request.getEmployer());
        profile.setSalaryRange(request.getSalaryRange());
        profile.setEducation(request.getEducation());
        profile.setSkills(request.getSkills());
        profile.setLinkedinUrl(request.getLinkedinUrl());
        profile.setInstagramUrl(request.getInstagramUrl());
        profile.setFacebookUrl(request.getFacebookUrl());
        profile.setWebsiteUrl(request.getWebsiteUrl());
        profile.setProfileVisibility(request.getProfileVisibility() != null ? request.getProfileVisibility() : "PUBLIC");
        profile.setShowPhone(request.isShowPhone());
        profile.setShowSalary(request.isShowSalary());
        profile.setShowSocial(request.isShowSocial());
        profile.setProfileLocked(request.isProfileLocked());

        ProfileEntity saved = profileRepository.save(profile);
        auditService.record(userId, "PROFILE_UPDATED", "Updated personal profile information", clientIp, requestId);

        return toProfileResponse(saved);
    }

    public ProfileResponse toProfileResponse(ProfileEntity entity) {
        ProfileResponse dto = new ProfileResponse();
        dto.setProfileId(entity.getId());
        dto.setUserId(entity.getUserId());
        dto.setDisplayName(entity.getDisplayName());
        dto.setDateOfBirth(entity.getDateOfBirth());
        dto.setGender(entity.getGender());
        dto.setCountry(entity.getCountry());
        dto.setStateProvince(entity.getStateProvince());
        dto.setCity(entity.getCity());
        dto.setBio(entity.getBio());
        dto.setPhoneNumber(entity.getPhoneNumber());
        dto.setPhotoAvailable(entity.isPhotoAvailable());
        dto.setProfession(entity.getProfession());
        dto.setEmployer(entity.getEmployer());
        dto.setSalaryRange(entity.getSalaryRange());
        dto.setEducation(entity.getEducation());
        dto.setSkills(entity.getSkills());
        dto.setLinkedinUrl(entity.getLinkedinUrl());
        dto.setInstagramUrl(entity.getInstagramUrl());
        dto.setFacebookUrl(entity.getFacebookUrl());
        dto.setWebsiteUrl(entity.getWebsiteUrl());
        dto.setProfileVisibility(entity.getProfileVisibility());
        dto.setShowPhone(entity.isShowPhone());
        dto.setShowSalary(entity.isShowSalary());
        dto.setShowSocial(entity.isShowSocial());
        dto.setBackgroundCheckStatus(entity.getBackgroundCheckStatus());
        dto.setVerificationStatus(entity.getVerificationStatus());
        dto.setVerifiedAt(entity.getVerifiedAt() != null ? entity.getVerifiedAt().toString() : null);
        dto.setProfileLocked(entity.isProfileLocked());
        return dto;
    }
}
