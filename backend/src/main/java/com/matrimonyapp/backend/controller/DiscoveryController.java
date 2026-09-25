package com.matrimonyapp.backend.controller;

import com.matrimonyapp.backend.dto.discovery.ProfileDetailDto;
import com.matrimonyapp.backend.dto.discovery.ProfileSearchResponse;
import com.matrimonyapp.backend.entity.PhotoEntity;
import com.matrimonyapp.backend.security.UserPrincipal;
import com.matrimonyapp.backend.service.DiscoveryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/profiles")
@Tag(name = "Discovery & Matching", description = "Query discoverable matrimonial profiles, filter matches, and view detailed profiles")
public class DiscoveryController {

    private final DiscoveryService discoveryService;

    public DiscoveryController(DiscoveryService discoveryService) {
        this.discoveryService = discoveryService;
    }

    @GetMapping("/search")
    @Operation(summary = "Search profiles", description = "Paginated query for discoverable profiles with location and age filtering")
    public ResponseEntity<ProfileSearchResponse> search(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(value = "country", required = false) String country,
            @RequestParam(value = "state", required = false) String state,
            @RequestParam(value = "city", required = false) String city,
            @RequestParam(value = "minAge", required = false) Integer minAge,
            @RequestParam(value = "maxAge", required = false) Integer maxAge,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "pageSize", defaultValue = "20") int pageSize
    ) {
        String callerUserId = principal != null ? principal.getUserId() : null;
        ProfileSearchResponse response = discoveryService.searchProfiles(
                callerUserId,
                country,
                state,
                city,
                minAge,
                maxAge,
                page,
                pageSize
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get profile details", description = "Fetch public details of a match candidate, respecting privacy and blocking rules")
    public ResponseEntity<ProfileDetailDto> getProfile(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable("id") String profileId
    ) {
        String callerUserId = principal != null ? principal.getUserId() : null;
        ProfileDetailDto response = discoveryService.getProfileDetail(callerUserId, profileId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/photo")
    @Operation(summary = "Get candidate photo", description = "Stream photo image for a match candidate")
    public ResponseEntity<byte[]> getPhoto(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable("id") String profileId
    ) {
        String callerUserId = principal != null ? principal.getUserId() : null;
        PhotoEntity photo = discoveryService.getProfilePhoto(callerUserId, profileId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, photo.getContentType())
                .body(photo.getFileData());
    }
}
