package com.aidigital.aionboarding.service.learning.support;

import com.aidigital.aionboarding.domain.common.dictionary.UserRoleCode;
import com.aidigital.aionboarding.domain.common.dictionary.entities.UserRole;
import com.aidigital.aionboarding.domain.roadmap.entities.RoadmapTeamAssignment;
import com.aidigital.aionboarding.domain.user.entities.User;
import com.aidigital.aionboarding.service.common.error.AppException;
import com.aidigital.aionboarding.service.common.security.AppUser;
import com.aidigital.aionboarding.service.permission.services.PermissionService;
import com.aidigital.aionboarding.service.team.services.TeamService;
import com.aidigital.aionboarding.service.user.models.UserRecord;
import org.instancio.Instancio;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LearningAssignmentAccessPolicyTest {

	@Mock
	private TeamService teamService;
	@Mock
	private PermissionService permissionService;

	@InjectMocks
	private LearningAssignmentAccessPolicy policy;

	private AppUser adminActor() {
		return new AppUser(1L, "clerk-admin", "admin@test.com", "Admin", "admin", "Admin", null, null, null);
	}

	private AppUser teamLeadActor() {
		return new AppUser(2L, "clerk-tl", "tl@test.com", "Team Lead", "teamlead", "Team Lead", null, null, null);
	}

	private UserRecord assignableUserRecord(Long id) {
		return new UserRecord(id, "clerk-" + id, "User " + id, "user" + id + "@test.com", "member", null, null, null,
				null, null, null);
	}

	private User userWithRole(Long id, String roleCode) {
		User user = Instancio.create(User.class);
		user.setId(id);
		UserRole role = new UserRole();
		role.setCode(roleCode);
		role.setName(roleCode);
		role.setDisplayOrder(1);
		role.setIsActive(true);
		user.setRole(role);
		return user;
	}

	@Nested
	class RequireAssignableTargets {

		@Test
		void shouldNotThrowWhenAllTargetsAreAssignableTest() {
			// Given:
			AppUser actor = adminActor();
			List<Long> targetUserIds = List.of(20L, 21L);
			when(teamService.getAssignableLearningUsers(actor))
					.thenReturn(List.of(assignableUserRecord(20L), assignableUserRecord(21L)));

			// When-Then:
			policy.requireAssignableTargets(actor, targetUserIds, "forbidden");
		}

		@Test
		void shouldThrowWhenAnyTargetIsNotAssignableTest() {
			// Given: only one of the two selected users is assignable to this actor
			AppUser actor = teamLeadActor();
			List<Long> targetUserIds = List.of(20L, 21L);
			when(teamService.getAssignableLearningUsers(actor)).thenReturn(List.of(assignableUserRecord(20L)));

			// When-Then:
			assertThatThrownBy(() -> policy.requireAssignableTargets(actor, targetUserIds, "forbidden message"))
					.isInstanceOf(AppException.class);
		}
	}

	@Nested
	class RequireManageableTeam {

		@Test
		void shouldNotThrowWhenActorCanManageTeamTest() {
			// Given:
			AppUser actor = adminActor();
			when(permissionService.canManageTeam(actor, 20L)).thenReturn(true);

			// When-Then:
			policy.requireManageableTeam(actor, 20L, "forbidden");
		}

		@Test
		void shouldThrowWhenActorCannotManageTeamTest() {
			// Given:
			AppUser actor = adminActor();
			when(permissionService.canManageTeam(actor, 20L)).thenReturn(false);

			// When-Then:
			assertThatThrownBy(() -> policy.requireManageableTeam(actor, 20L, "forbidden message"))
					.isInstanceOf(AppException.class);
		}
	}

	@Nested
	class RequireCanRevokeTeamAssignment {

		@Test
		void shouldNotThrowWhenAssignmentHasNoAssignedByUserTest() {
			// Given:
			AppUser actor = teamLeadActor();
			RoadmapTeamAssignment assignment = Instancio.create(RoadmapTeamAssignment.class);
			assignment.setAssignedByUser(null);

			// When-Then:
			policy.requireCanRevokeTeamAssignment(actor, assignment);
		}

		@Test
		void shouldNotThrowWhenActorCreatedTheAssignmentTest() {
			// Given:
			AppUser actor = adminActor();
			RoadmapTeamAssignment assignment = Instancio.create(RoadmapTeamAssignment.class);
			assignment.setAssignedByUser(userWithRole(actor.internalId(), UserRoleCode.ADMIN));

			// When-Then:
			policy.requireCanRevokeTeamAssignment(actor, assignment);
		}

		@Test
		void shouldThrowWhenAssignmentWasCreatedByHigherRoleTest() {
			// Given:
			AppUser actor = teamLeadActor();
			RoadmapTeamAssignment assignment = Instancio.create(RoadmapTeamAssignment.class);
			assignment.setAssignedByUser(userWithRole(1L, UserRoleCode.ADMIN));

			// When-Then:
			assertThatThrownBy(() -> policy.requireCanRevokeTeamAssignment(actor, assignment))
					.isInstanceOf(AppException.class);
		}

		@Test
		void shouldNotThrowWhenActorRoleIsHigherThanCreatorRoleTest() {
			// Given:
			AppUser actor = adminActor();
			RoadmapTeamAssignment assignment = Instancio.create(RoadmapTeamAssignment.class);
			assignment.setAssignedByUser(userWithRole(2L, UserRoleCode.TEAMLEAD));

			// When-Then:
			policy.requireCanRevokeTeamAssignment(actor, assignment);
		}
	}
}
