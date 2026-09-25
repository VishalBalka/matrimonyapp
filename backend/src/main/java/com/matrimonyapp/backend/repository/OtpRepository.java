package com.matrimonyapp.backend.repository;

import com.matrimonyapp.backend.entity.OtpEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface OtpRepository extends JpaRepository<OtpEntity, String> {

    Optional<OtpEntity> findTopByEmailIgnoreCaseAndPurposeAndConsumedFalseAndExpiresAtAfterOrderByCreatedAtDesc(
            String email,
            String purpose,
            Instant now
    );

    List<OtpEntity> findByEmailIgnoreCaseAndPurposeAndCreatedAtAfter(
            String email,
            String purpose,
            Instant since
    );
}
