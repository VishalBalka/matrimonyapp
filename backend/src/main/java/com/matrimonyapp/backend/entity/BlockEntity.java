package com.matrimonyapp.backend.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "blocks", uniqueConstraints = {
    @UniqueConstraint(name = "uq_blocks", columnNames = {"blocker_user_id", "blocked_user_id"})
})
public class BlockEntity {

    @Id
    @Column(name = "id", length = 64)
    private String id;

    @Column(name = "blocker_user_id", nullable = false, length = 64)
    private String blockerUserId;

    @Column(name = "blocked_user_id", nullable = false, length = 64)
    private String blockedUserId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public BlockEntity() {
    }

    public BlockEntity(String id, String blockerUserId, String blockedUserId) {
        this.id = id;
        this.blockerUserId = blockerUserId;
        this.blockedUserId = blockedUserId;
        this.createdAt = Instant.now();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getBlockerUserId() {
        return blockerUserId;
    }

    public void setBlockerUserId(String blockerUserId) {
        this.blockerUserId = blockerUserId;
    }

    public String getBlockedUserId() {
        return blockedUserId;
    }

    public void setBlockedUserId(String blockedUserId) {
        this.blockedUserId = blockedUserId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
