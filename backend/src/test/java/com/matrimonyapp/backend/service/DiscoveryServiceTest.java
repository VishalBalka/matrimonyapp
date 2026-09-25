package com.matrimonyapp.backend.service;

import com.matrimonyapp.backend.dto.discovery.ProfileDetailDto;
import com.matrimonyapp.backend.entity.ProfileEntity;
import com.matrimonyapp.backend.exception.AppException;
import com.matrimonyapp.backend.exception.ErrorCode;
import com.matrimonyapp.backend.repository.BlockRepository;
import com.matrimonyapp.backend.repository.ProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class DiscoveryServiceTest {

    private ProfileRepository profileRepository;
    private BlockRepository blockRepository;
    private PhotoService photoService;
    private DiscoveryService discoveryService;

    @BeforeEach
    void setUp() {
        profileRepository = Mockito.mock(ProfileRepository.class);
        blockRepository = Mockito.mock(BlockRepository.class);
        photoService = Mockito.mock(PhotoService.class);
        discoveryService = new DiscoveryService(profileRepository, blockRepository, photoService);
    }

    @Test
    void getProfileDetail_privateProfile_hiddenFromOtherUsers() {
        ProfileEntity profile = new ProfileEntity();
        profile.setId("p1");
        profile.setUserId("user-owner");
        profile.setProfileVisibility("PRIVATE");

        when(profileRepository.findById("p1")).thenReturn(Optional.of(profile));

        AppException ex = assertThrows(AppException.class, () -> discoveryService.getProfileDetail("other-user", "p1"));
        assertEquals(ErrorCode.RESOURCE_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void getProfileDetail_blockedUser_returnsNotFound() {
        ProfileEntity profile = new ProfileEntity();
        profile.setId("p1");
        profile.setUserId("user-owner");
        profile.setProfileVisibility("PUBLIC");

        when(profileRepository.findById("p1")).thenReturn(Optional.of(profile));
        when(blockRepository.existsByBlockerUserIdAndBlockedUserId("user-owner", "blocked-caller")).thenReturn(true);

        AppException ex = assertThrows(AppException.class, () -> discoveryService.getProfileDetail("blocked-caller", "p1"));
        assertEquals(ErrorCode.RESOURCE_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void getProfileDetail_privacyFlagsRespected() {
        ProfileEntity profile = new ProfileEntity();
        profile.setId("p1");
        profile.setUserId("user-owner");
        profile.setDisplayName("Test Profile");
        profile.setDateOfBirth("1995-01-01");
        profile.setCity("Mumbai");
        profile.setProfileVisibility("PUBLIC");
        profile.setSalaryRange("₹40-50 LPA");
        profile.setShowSalary(false); // Hidden!
        profile.setPhoneNumber("+91 99999 88888");
        profile.setShowPhone(false);  // Hidden!
        profile.setLinkedinUrl("https://linkedin.com/in/secret");
        profile.setShowSocial(false); // Hidden!

        when(profileRepository.findById("p1")).thenReturn(Optional.of(profile));
        when(blockRepository.existsByBlockerUserIdAndBlockedUserId("caller", "user-owner")).thenReturn(false);

        ProfileDetailDto detail = discoveryService.getProfileDetail("caller", "p1");
        assertNotNull(detail);
        assertNull(detail.getSalaryRange(), "Salary must be hidden when showSalary is false");
        assertNull(detail.getLinkedinUrl(), "Social links must be hidden when showSocial is false");
    }
}
