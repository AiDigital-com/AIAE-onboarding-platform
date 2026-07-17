package com.aidigital.aionboarding.domain.storage.entities;

import com.aidigital.aionboarding.domain.common.entities.IdAwareEntity;
import com.aidigital.aionboarding.domain.user.entities.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Tracks a presigned PUT that was issued to a user but not yet confirmed as registered onto a
 * lesson/material entity, so registration can validate ownership and upload completion, and
 * abandoned (never-registered) uploads can be swept from object storage.
 */
@Entity
@Table(name = "pending_uploads")
@Getter
@Setter
@NoArgsConstructor
public class PendingUpload extends IdAwareEntity {

    @Column(name = "storage_key", nullable = false, unique = true)
    private String storageKey;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_user_id", nullable = false)
    private User ownerUser;

    @Column(name = "expected_content_type", nullable = false)
    private String expectedContentType;

    @Column(name = "expected_size_bytes", nullable = false)
    private long expectedSizeBytes;

    @Column(name = "confirmed", nullable = false)
    private boolean confirmed;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;
}
