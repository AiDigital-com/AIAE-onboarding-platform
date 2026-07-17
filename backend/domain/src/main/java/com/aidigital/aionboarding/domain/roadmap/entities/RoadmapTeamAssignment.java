package com.aidigital.aionboarding.domain.roadmap.entities;

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

@Entity
@Table(name = "roadmap_team_assignments")
@Getter
@Setter
@NoArgsConstructor
public class RoadmapTeamAssignment extends IdAwareEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "roadmap_id", nullable = false)
	private Roadmap roadmap;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "lead_user_id", nullable = false)
	private User leadUser;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "assigned_by_user_id")
	private User assignedByUser;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;

	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;
}
