package com.aidigital.aionboarding.roadmap;

import com.aidigital.aionboarding.domain.common.dictionary.UserRoleCode;
import com.aidigital.aionboarding.domain.common.dictionary.entities.UserRole;
import com.aidigital.aionboarding.domain.common.dictionary.repositories.UserRoleRepository;
import com.aidigital.aionboarding.domain.roadmap.entities.Roadmap;
import com.aidigital.aionboarding.domain.roadmap.entities.RoadmapTeamAssignment;
import com.aidigital.aionboarding.domain.roadmap.repositories.RoadmapRepository;
import com.aidigital.aionboarding.domain.roadmap.repositories.RoadmapTeamAssignmentRepository;
import com.aidigital.aionboarding.domain.user.entities.User;
import com.aidigital.aionboarding.domain.user.repositories.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifies {@link RoadmapTeamAssignment} persistence against real PostgreSQL: the
 * {@code fk_roadmap_team_assignments_*} foreign keys, the
 * {@code uq_roadmap_team_assignments_roadmap_lead} unique constraint, and the
 * {@code findByRoadmapId} JOIN FETCH query — none of which H2 exercises the same way.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers(disabledWithoutDocker = true)
class RoadmapTeamAssignmentRepositoryIntegrationTest {

	@Container
	@ServiceConnection
	static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

	@Autowired
	private RoadmapTeamAssignmentRepository roadmapTeamAssignmentRepository;

	@Autowired
	private RoadmapRepository roadmapRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private UserRoleRepository userRoleRepository;

	@Test
	void saveShouldPersistAssignmentAndFindByRoadmapIdShouldFetchLeadUserTest() {
		// Given:
		UserRole role = userRoleRepository.findByCode(UserRoleCode.MEMBER).orElseThrow();
		User lead = userRepository.save(user(role, "lead@test.com"));
		User admin = userRepository.save(user(role, "admin@test.com"));
		Roadmap roadmap = roadmapRepository.save(roadmap());

		RoadmapTeamAssignment assignment = new RoadmapTeamAssignment();
		assignment.setRoadmap(roadmap);
		assignment.setLeadUser(lead);
		assignment.setAssignedByUser(admin);
		LocalDateTime now = LocalDateTime.of(2026, 1, 1, 0, 0);
		assignment.setCreatedAt(now);
		assignment.setUpdatedAt(now);

		// When:
		RoadmapTeamAssignment saved = roadmapTeamAssignmentRepository.save(assignment);
		List<RoadmapTeamAssignment> found = roadmapTeamAssignmentRepository.findByRoadmapId(roadmap.getId());

		// Then:
		assertThat(saved.getId()).isNotNull();
		assertThat(found).hasSize(1);
		assertThat(found.get(0).getLeadUser().getEmail()).isEqualTo("lead@test.com");
	}

	@Test
	void saveShouldViolateUniqueConstraintWhenRoadmapAlreadyAssignedToSameTeamTest() {
		// Given:
		UserRole role = userRoleRepository.findByCode(UserRoleCode.MEMBER).orElseThrow();
		User lead = userRepository.save(user(role, "lead2@test.com"));
		Roadmap roadmap = roadmapRepository.save(roadmap());
		LocalDateTime now = LocalDateTime.of(2026, 1, 1, 0, 0);

		RoadmapTeamAssignment first = new RoadmapTeamAssignment();
		first.setRoadmap(roadmap);
		first.setLeadUser(lead);
		first.setCreatedAt(now);
		first.setUpdatedAt(now);
		roadmapTeamAssignmentRepository.saveAndFlush(first);

		RoadmapTeamAssignment duplicate = new RoadmapTeamAssignment();
		duplicate.setRoadmap(roadmap);
		duplicate.setLeadUser(lead);
		duplicate.setCreatedAt(now);
		duplicate.setUpdatedAt(now);

		// When-Then:
		assertThatThrownBy(() -> roadmapTeamAssignmentRepository.saveAndFlush(duplicate))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	private User user(UserRole role, String email) {
		User user = new User();
		user.setName(email);
		user.setEmail(email);
		user.setRole(role);
		LocalDateTime now = LocalDateTime.of(2026, 1, 1, 0, 0);
		user.setCreatedAt(now);
		user.setUpdatedAt(now);
		return user;
	}

	private Roadmap roadmap() {
		Roadmap roadmap = new Roadmap();
		roadmap.setTitle("Onboarding roadmap");
		roadmap.setDescription("description");
		roadmap.setTags(List.of());
		roadmap.setCreatedBy("tester");
		LocalDateTime now = LocalDateTime.of(2026, 1, 1, 0, 0);
		roadmap.setCreatedAt(now);
		roadmap.setUpdatedAt(now);
		return roadmap;
	}
}
