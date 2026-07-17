package com.aidigital.aionboarding.domain.roadmap.entities;

import com.aidigital.aionboarding.domain.common.entities.IdAwareEntity;
import com.aidigital.aionboarding.domain.group.entities.Group;
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
 * A roadmap's standing assignment to a {@link Group}, optionally narrowed by
 * {@link RoadmapGroupAssignmentGrade} rows. One row per (roadmap, group) pair; the grade filter
 * is a dynamic, standing rule re-evaluated whenever group membership or a member's grade changes.
 */
@Entity
@Table(name = "roadmap_group_assignments")
@Getter
@Setter
@NoArgsConstructor
public class RoadmapGroupAssignment extends IdAwareEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "roadmap_id", nullable = false)
    private Roadmap roadmap;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_id", nullable = false)
    private Group group;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_by_user_id")
    private User assignedByUser;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Optimistic-locking version. Since {@code assignRoadmapToGroup} always saves this row
     * before replacing its {@link RoadmapGroupAssignmentGrade} child rows, this single version
     * check also guards the child-row replace: a losing concurrent writer fails here first and
     * never reaches the child replace.
     */
    @Version
    @Column(nullable = false)
    private Long version;
}
