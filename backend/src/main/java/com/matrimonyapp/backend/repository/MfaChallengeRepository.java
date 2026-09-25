package com.matrimonyapp.backend.repository;

import com.matrimonyapp.backend.entity.MfaChallengeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface MfaChallengeRepository extends JpaRepository<MfaChallengeEntity, String> {

    Optional<MfaChallengeEntity> findByIdAndConsumedFalseAndExpiresAtAfter(String id, Instant now);
}
