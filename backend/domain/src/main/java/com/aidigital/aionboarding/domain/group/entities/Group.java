package com.aidigital.aionboarding.domain.group.entities;

import com.aidigital.aionboarding.domain.common.entities.IdAwareEntity;
import com.aidigital.aionboarding.domain.user.entities.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * An explicit, independently named organizational group. Leads and members are modeled as
 * separate join entities ({@link GroupLead}, {@link GroupMember}) rather than as collections here,
 * so a user can lead or belong to more than one group.
 */
@Entity
@Table(name = "groups")
@Getter
@Setter
@NoArgsConstructor
public class Group extends IdAwareEntity {

    @Column(nullable = false)
    private String name;

    /** Lower-cased, trimmed {@link #name}, used for case-insensitive uniqueness and search. */
    @Column(name = "normalized_name", nullable = false)
    private String normalizedName;

    @Column(nullable = false)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id")
    private User createdByUser;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Optimistic-locking version, so two leads editing the same group's name/description
     * concurrently cannot silently overwrite one another.
     */
    @Version
    @Column(nullable = false)
    private Long version;
}
