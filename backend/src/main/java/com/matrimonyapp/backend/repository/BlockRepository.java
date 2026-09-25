package com.matrimonyapp.backend.repository;

import com.matrimonyapp.backend.entity.BlockEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BlockRepository extends JpaRepository<BlockEntity, String> {

    Optional<BlockEntity> findByBlockerUserIdAndBlockedUserId(String blockerUserId, String blockedUserId);

    boolean existsByBlockerUserIdAndBlockedUserId(String blockerUserId, String blockedUserId);

    void deleteByBlockerUserIdAndBlockedUserId(String blockerUserId, String blockedUserId);

    @Query("SELECT b.blockedUserId FROM BlockEntity b WHERE b.blockerUserId = :userId")
    List<String> findBlockedUserIds(@Param("userId") String userId);

    @Query("SELECT b.blockerUserId FROM BlockEntity b WHERE b.blockedUserId = :userId")
    List<String> findBlockerUserIds(@Param("userId") String userId);
}
