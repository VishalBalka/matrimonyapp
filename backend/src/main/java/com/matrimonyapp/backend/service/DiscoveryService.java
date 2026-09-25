package com.matrimonyapp.backend.service;

import com.matrimonyapp.backend.dto.discovery.ProfileDetailDto;
import com.matrimonyapp.backend.dto.discovery.ProfileSearchItemDto;
import com.matrimonyapp.backend.dto.discovery.ProfileSearchResponse;
import com.matrimonyapp.backend.entity.PhotoEntity;
import com.matrimonyapp.backend.entity.ProfileEntity;
import com.matrimonyapp.backend.exception.AppException;
import com.matrimonyapp.backend.exception.ErrorCode;
import com.matrimonyapp.backend.repository.BlockRepository;
import com.matrimonyapp.backend.repository.ProfileRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class DiscoveryService {

    private final ProfileRepository profileRepository;
    private final BlockRepository blockRepository;
    private final PhotoService photoService;

    public DiscoveryService(
            ProfileRepository profileRepository,
            BlockRepository blockRepository,
            PhotoService photoService
    ) {
        this.profileRepository = profileRepository;
        this.blockRepository = blockRepository;
        this.photoService = photoService;
    }

    @Transactional(readOnly = true)
    public ProfileSearchResponse searchProfiles(
            String callerUserId,
            String country,
            String state,
            String city,
            Integer minAge,
            Integer maxAge,
            int page,
            int pageSize
    ) {
        int safePage = Math.max(0, page);
        int safePageSize = Math.min(50, Math.max(1, pageSize));

        // Find users blocked by caller or who have blocked caller
        Set<String> excludedUserIds = new HashSet<>();
        if (callerUserId != null) {
            excludedUserIds.add(callerUserId); // Exclude self
            excludedUserIds.addAll(blockRepository.findBlockedUserIds(callerUserId));
            excludedUserIds.addAll(blockRepository.findBlockerUserIds(callerUserId));
        }

        List<String> excludedList = excludedUserIds.isEmpty() ? null : new ArrayList<>(excludedUserIds);

        Pageable pageable = PageRequest.of(safePage, safePageSize);
        Page<ProfileEntity> resultPage = profileRepository.searchProfiles(
                cleanQuery(country),
                cleanQuery(state),
                cleanQuery(city),
                excludedList,
                pageable
        );

        int min = minAge != null ? minAge : 18;
        int max = maxAge != null ? maxAge : 100;

        List<ProfileSearchItemDto> items = resultPage.getContent().stream()
                .filter(p -> {
                    int age = calculateAge(p.getDateOfBirth());
                    return age >= min && age <= max;
                })
                .map(this::toSearchItemDto)
                .collect(Collectors.toList());

        return new ProfileSearchResponse(
                items,
                resultPage.getNumber(),
                resultPage.getSize(),
                resultPage.getTotalElements(),
                resultPage.getTotalPages()
        );
    }

    @Transactional(readOnly = true)
    public ProfileDetailDto getProfileDetail(String callerUserId, String profileId) {
        ProfileEntity profile = profileRepository.findById(profileId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Profile not found."));

        if (!"PUBLIC".equalsIgnoreCase(profile.getProfileVisibility()) && !profile.getUserId().equals(callerUserId)) {
            throw new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Profile not found or set to private.");
        }

        if (profile.isProfileLocked() && !profile.getUserId().equals(callerUserId)) {
            throw new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Profile is unavailable.");
        }

        // Check if mutual block exists
        if (callerUserId != null && !callerUserId.equals(profile.getUserId())) {
            if (blockRepository.existsByBlockerUserIdAndBlockedUserId(callerUserId, profile.getUserId()) ||
                blockRepository.existsByBlockerUserIdAndBlockedUserId(profile.getUserId(), callerUserId)) {
                throw new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Profile not found.");
            }
        }

        return toDetailDto(profile);
    }

    @Transactional(readOnly = true)
    public PhotoEntity getProfilePhoto(String callerUserId, String profileId) {
        ProfileEntity profile = profileRepository.findById(profileId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Profile not found."));

        if (callerUserId != null && !callerUserId.equals(profile.getUserId())) {
            if (blockRepository.existsByBlockerUserIdAndBlockedUserId(callerUserId, profile.getUserId()) ||
                blockRepository.existsByBlockerUserIdAndBlockedUserId(profile.getUserId(), callerUserId)) {
                throw new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Photo not available.");
            }
        }

        return photoService.getPhotoByProfileId(profileId);
    }

    private ProfileSearchItemDto toSearchItemDto(ProfileEntity entity) {
        ProfileSearchItemDto dto = new ProfileSearchItemDto();
        dto.setProfileId(entity.getId());
        dto.setDisplayName(entity.getDisplayName());
        dto.setAge(calculateAge(entity.getDateOfBirth()));
        dto.setCountry(entity.getCountry());
        dto.setStateProvince(entity.getStateProvince());
        dto.setCity(entity.getCity());
        dto.setProfession(entity.getProfession());
        dto.setSkills(entity.getSkills());
        dto.setSalaryRange(entity.isShowSalary() ? entity.getSalaryRange() : null);
        dto.setLinkedinUrl(entity.isShowSocial() ? entity.getLinkedinUrl() : null);
        dto.setInstagramUrl(entity.isShowSocial() ? entity.getInstagramUrl() : null);
        dto.setFacebookUrl(entity.isShowSocial() ? entity.getFacebookUrl() : null);
        dto.setWebsiteUrl(entity.isShowSocial() ? entity.getWebsiteUrl() : null);
        dto.setVerified("VERIFIED".equalsIgnoreCase(entity.getVerificationStatus()));
        dto.setPhotoAvailable(entity.isPhotoAvailable());
        dto.setPhotoUrl(entity.isPhotoAvailable() ? "/api/v1/profiles/" + entity.getId() + "/photo" : null);
        return dto;
    }

    private ProfileDetailDto toDetailDto(ProfileEntity entity) {
        ProfileDetailDto dto = new ProfileDetailDto();
        dto.setProfileId(entity.getId());
        dto.setDisplayName(entity.getDisplayName());
        dto.setAge(calculateAge(entity.getDateOfBirth()));
        dto.setCountry(entity.getCountry());
        dto.setStateProvince(entity.getStateProvince());
        dto.setCity(entity.getCity());
        dto.setProfession(entity.getProfession());
        dto.setSkills(entity.getSkills());
        dto.setSalaryRange(entity.isShowSalary() ? entity.getSalaryRange() : null);
        dto.setLinkedinUrl(entity.isShowSocial() ? entity.getLinkedinUrl() : null);
        dto.setInstagramUrl(entity.isShowSocial() ? entity.getInstagramUrl() : null);
        dto.setFacebookUrl(entity.isShowSocial() ? entity.getFacebookUrl() : null);
        dto.setWebsiteUrl(entity.isShowSocial() ? entity.getWebsiteUrl() : null);
        dto.setVerified("VERIFIED".equalsIgnoreCase(entity.getVerificationStatus()));
        dto.setPhotoAvailable(entity.isPhotoAvailable());
        dto.setPhotoUrl(entity.isPhotoAvailable() ? "/api/v1/profiles/" + entity.getId() + "/photo" : null);
        return dto;
    }

    private int calculateAge(String dob) {
        if (dob == null || dob.isBlank()) {
            return 25; // Sensible fallback
        }
        try {
            LocalDate birthDate = LocalDate.parse(dob.trim(), DateTimeFormatter.ISO_LOCAL_DATE);
            return Period.between(birthDate, LocalDate.now()).getYears();
        } catch (Exception e) {
            return 25;
        }
    }

    private String cleanQuery(String q) {
        return (q != null && !q.trim().isEmpty()) ? q.trim() : null;
    }
}
