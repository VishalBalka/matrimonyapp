package com.matrimonyapp.backend.repository;

import com.matrimonyapp.backend.entity.ProfileEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProfileRepository extends JpaRepository<ProfileEntity, String> {

    Optional<ProfileEntity> findByUserId(String userId);

    long countByVerificationStatus(String verificationStatus);

    @Query("""
        SELECT p FROM ProfileEntity p
        WHERE p.profileVisibility = 'PUBLIC'
          AND p.profileLocked = false
          AND (:country IS NULL OR LOWER(p.country) LIKE LOWER(CONCAT('%', :country, '%')))
          AND (:state IS NULL OR LOWER(p.stateProvince) LIKE LOWER(CONCAT('%', :state, '%')))
          AND (:city IS NULL OR LOWER(p.city) LIKE LOWER(CONCAT('%', :city, '%')))
          AND (:excludedUserIds IS NULL OR p.userId NOT IN :excludedUserIds)
    """)
    Page<ProfileEntity> searchProfiles(
            @Param("country") String country,
            @Param("state") String state,
            @Param("city") String city,
            @Param("excludedUserIds") List<String> excludedUserIds,
            Pageable pageable
    );
}
