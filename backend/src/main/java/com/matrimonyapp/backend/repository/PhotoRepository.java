package com.matrimonyapp.backend.repository;

import com.matrimonyapp.backend.entity.PhotoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PhotoRepository extends JpaRepository<PhotoEntity, String> {

    Optional<PhotoEntity> findByProfileId(String profileId);

    Optional<PhotoEntity> findByUserId(String userId);

    void deleteByProfileId(String profileId);
}
