package com.matrimonyapp.backend.service;

import com.matrimonyapp.backend.dto.profile.ProfileResponse;
import com.matrimonyapp.backend.entity.PhotoEntity;
import com.matrimonyapp.backend.entity.ProfileEntity;
import com.matrimonyapp.backend.exception.AppException;
import com.matrimonyapp.backend.exception.ErrorCode;
import com.matrimonyapp.backend.repository.PhotoRepository;
import com.matrimonyapp.backend.repository.ProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@Service
public class PhotoService {

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB

    private final PhotoRepository photoRepository;
    private final ProfileRepository profileRepository;
    private final ProfileService profileService;
    private final AuditService auditService;

    public PhotoService(
            PhotoRepository photoRepository,
            ProfileRepository profileRepository,
            ProfileService profileService,
            AuditService auditService
    ) {
        this.photoRepository = photoRepository;
        this.profileRepository = profileRepository;
        this.profileService = profileService;
        this.auditService = auditService;
    }

    @Transactional
    public ProfileResponse uploadPhoto(String userId, MultipartFile file, String clientIp, String requestId) {
        if (file == null || file.isEmpty()) {
            throw new AppException(ErrorCode.VALIDATION_FAILED, "Please select an image file to upload.");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new AppException(ErrorCode.VALIDATION_FAILED, "Image size must not exceed 10MB.");
        }

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new AppException(ErrorCode.BAD_REQUEST, "Failed to read uploaded photo file.");
        }

        String mimeType = detectMimeType(bytes);
        if (mimeType == null) {
            throw new AppException(ErrorCode.VALIDATION_FAILED, "Only JPEG and PNG image formats are supported.");
        }

        ProfileEntity profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Profile not found."));

        // Delete any existing photo for this profile
        photoRepository.deleteByProfileId(profile.getId());

        String photoId = "photo-" + UUID.randomUUID().toString().substring(0, 10);
        PhotoEntity photo = new PhotoEntity(
                photoId,
                userId,
                profile.getId(),
                mimeType,
                file.getSize(),
                bytes
        );
        photoRepository.save(photo);

        profile.setPhotoAvailable(true);
        ProfileEntity savedProfile = profileRepository.save(profile);

        auditService.record(userId, "PHOTO_UPLOADED", "Uploaded profile photo: " + photoId, clientIp, requestId);
        return profileService.toProfileResponse(savedProfile);
    }

    @Transactional(readOnly = true)
    public PhotoEntity getPhotoByUserId(String userId) {
        return photoRepository.findByUserId(userId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "No photo found for user."));
    }

    @Transactional(readOnly = true)
    public PhotoEntity getPhotoByProfileId(String profileId) {
        return photoRepository.findByProfileId(profileId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "No photo found for this profile."));
    }

    @Transactional
    public ProfileResponse deletePhoto(String userId, String clientIp, String requestId) {
        ProfileEntity profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Profile not found."));

        photoRepository.deleteByProfileId(profile.getId());
        profile.setPhotoAvailable(false);
        ProfileEntity savedProfile = profileRepository.save(profile);

        auditService.record(userId, "PHOTO_DELETED", "Deleted profile photo", clientIp, requestId);
        return profileService.toProfileResponse(savedProfile);
    }

    private String detectMimeType(byte[] bytes) {
        if (bytes == null || bytes.length < 8) {
            return null;
        }

        // JPEG Magic Bytes: FF D8 FF
        if ((bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8 && (bytes[2] & 0xFF) == 0xFF) {
            return "image/jpeg";
        }

        // PNG Magic Bytes: 89 50 4E 47 0D 0A 1A 0A
        if ((bytes[0] & 0xFF) == 0x89 && (bytes[1] & 0xFF) == 0x50 &&
            (bytes[2] & 0xFF) == 0x4E && (bytes[3] & 0xFF) == 0x47 &&
            (bytes[4] & 0xFF) == 0x0D && (bytes[5] & 0xFF) == 0x0A &&
            (bytes[6] & 0xFF) == 0x1A && (bytes[7] & 0xFF) == 0x0A) {
            return "image/png";
        }

        return null;
    }
}
