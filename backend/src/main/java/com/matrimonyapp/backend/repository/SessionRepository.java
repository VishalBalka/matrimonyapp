package com.matrimonyapp.backend.repository;

import com.matrimonyapp.backend.entity.SessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SessionRepository extends JpaRepository<SessionEntity, String> {

    Optional<SessionEntity> findByTokenHash(String tokenHash);

    @Modifying
    @Query("UPDATE SessionEntity s SET s.revoked = true WHERE s.userId = :userId")
    void revokeAllByUserId(@Param("userId") String userId);

    @Modifying
    @Query("UPDATE SessionEntity s SET s.revoked = true WHERE s.tokenHash = :tokenHash")
    void revokeByTokenHash(@Param("tokenHash") String tokenHash);
}
