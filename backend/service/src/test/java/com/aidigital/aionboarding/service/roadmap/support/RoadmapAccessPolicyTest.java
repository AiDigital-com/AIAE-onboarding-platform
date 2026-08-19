package com.aidigital.aionboarding.service.roadmap.support;

import com.aidigital.aionboarding.domain.learning.entities.UserRoadmap;
import com.aidigital.aionboarding.domain.roadmap.entities.Roadmap;
import com.aidigital.aionboarding.domain.user.entities.User;
import com.aidigital.aionboarding.service.common.error.AppException;
import com.aidigital.aionboarding.service.common.error.ErrorReason;
import com.aidigital.aionboarding.service.common.security.AppUser;
import com.aidigital.aionboarding.service.learning.services.entity.LearningEnrollmentEntityService;
import com.aidigital.aionboarding.service.learning.support.LearningEnrollmentSupport;
import com.aidigital.aionboarding.service.permission.PermissionKeys;
import com.aidigital.aionboarding.service.permission.services.PermissionService;
import com.aidigital.aionboarding.service.roadmap.services.entity.RoadmapEntityService;
import com.aidigital.aionboarding.service.team.services.entity.TeamEntityService;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoadmapAccessPolicyTest {

	@Mock
	private RoadmapEntityService roadmapEntityService;
	@Mock
	private PermissionService permissionService;
	@Mock
	private TeamEntityService teamEntityService;
	@Mock
	private LearningEnrollmentEntityService learningEnrollmentEntityService;
	@Mock
	private LearningEnrollmentSupport learningEnrollmentSupport;

	@InjectMocks
	private RoadmapAccessPolicy policy;

	private AppUser adminActor() {
		return new AppUser(1L, "clerk-admin", "admin@test.com", "Admin", "admin", "Admin", null, null, null);
	}

	private AppUser teamLeadActor() {
		return new AppUser(6L, "clerk-6", "lead@test.com", "Lead", "teamlead", "Lead", null, null, null);
	}

	private AppUser memberActor() {
		return new AppUser(9L, "clerk-9", "member@test.com", "Member", "member", "Member", null, null, null);
	}

	private UserRoadmap.UserRoadmapId userRoadmapId(Long userId, Long roadmapId) {
		UserRoadmap.UserRoadmapId id = new UserRoadmap.UserRoadmapId();
		id.setUserId(userId);
		id.setRoadmapId(roadmapId);
		return id;
	}

	@Test
	void shouldReturnRoadmapWhenViewerCanManageTest() {
		// Given:
		AppUser viewer = new AppUser(1L, "clerk-1", "admin@test.com", "Admin", "admin", "Admin", null, null, null);
		Roadmap roadmap = roadmapWithAuthor(2L);
		when(roadmapEntityService.getReference(10L)).thenReturn(roadmap);
		when(permissionService.canManageRoadmap(eq(viewer), eq(2L))).thenReturn(true);

		// When:
		Roadmap result = policy.requireManageable(viewer, 10L);

		// Then:
		assertThat(result).isSameAs(roadmap);
		verify(permissionService).requirePermission(viewer, PermissionKeys.ROADMAPS_MANAGE);
	}

	@Test
	void shouldThrowWhenRoadmapNotFoundTest() {
		// Given:
		AppUser viewer = new AppUser(1L, "clerk-1", "admin@test.com", "Admin", "admin", "Admin", null, null, null);
		when(roadmapEntityService.getReference(10L)).thenThrow(new AppException(com.aidigital.aionboarding.service.common.error.ErrorReason.C001, "10"));

		// When-Then:
		assertThatThrownBy(() -> policy.requireManageable(viewer, 10L))
				.isInstanceOf(AppException.class);
	}

	@Test
	void shouldThrowWhenViewerCannotManageRoadmapTest() {
		// Given:
		AppUser viewer = new AppUser(2L, "clerk-2", "member@test.com", "Member", "member", "Member", null, null, null);
		Roadmap roadmap = roadmapWithAuthor(3L);
		when(roadmapEntityService.getReference(10L)).thenReturn(roadmap);
		when(permissionService.canManageRoadmap(eq(viewer), eq(3L))).thenReturn(false);

		// When-Then:
		assertThatThrownBy(() -> policy.requireManageable(viewer, 10L))
				.isInstanceOf(AppException.class);
	}

	@Nested
	class RequireSelfEnrollable {

		@Test
		void shouldThrowC001WhenMemberHoldsNoEnrollmentAndCannotManageTest() {
			// Given: a Member with no enrollment and no manage relationship to this roadmap
			AppUser actor = memberActor();
			Long roadmapId = 10L;
			Roadmap roadmap = roadmapWithAuthor(3L);
			UserRoadmap.UserRoadmapId enrollmentId = userRoadmapId(actor.internalId(), roadmapId);
			when(roadmapEntityService.getReference(roadmapId)).thenReturn(roadmap);
			when(learningEnrollmentSupport.userRoadmapId(actor.internalId(), roadmapId)).thenReturn(enrollmentId);
			when(learningEnrollmentEntityService.findUserRoadmapById(enrollmentId)).thenReturn(Optional.empty());

			// When-Then: C001, never C004 — must not disclose that the roadmap exists
			AppException thrown = org.junit.jupiter.api.Assertions.assertThrows(AppException.class,
					() -> policy.requireSelfEnrollable(actor, roadmapId));
			assertThat(thrown.getCode()).isEqualTo(ErrorReason.C001.name());
		}

		@Test
		void shouldSucceedIdempotentlyWhenMemberAlreadyHoldsAnEnrollmentTest() {
			// Given: a Member already enrolled in this roadmap, regardless of manageability
			AppUser actor = memberActor();
			Long roadmapId = 10L;
			Roadmap roadmap = roadmapWithAuthor(3L);
			UserRoadmap.UserRoadmapId enrollmentId = userRoadmapId(actor.internalId(), roadmapId);
			UserRoadmap existingEnrollment = new UserRoadmap();
			when(roadmapEntityService.getReference(roadmapId)).thenReturn(roadmap);
			when(learningEnrollmentSupport.userRoadmapId(actor.internalId(), roadmapId)).thenReturn(enrollmentId);
			when(learningEnrollmentEntityService.findUserRoadmapById(enrollmentId)).thenReturn(Optional.of(existingEnrollment));

			// When:
			Roadmap result = policy.requireSelfEnrollable(actor, roadmapId);

			// Then: no permission/manageability check needed once already enrolled
			assertThat(result).isSameAs(roadmap);
			verify(permissionService, never()).userHasPermission(eq(actor), eq(PermissionKeys.ROADMAPS_MANAGE));
		}

		@Test
		void shouldSucceedWhenTeamLeadSelfEnrollsInARoadmapAuthoredByTheirOwnTeamMemberTest() {
			// Given: no enrollment yet, but the roadmap's author is the lead's own team member
			AppUser actor = teamLeadActor();
			Long roadmapId = 10L;
			Roadmap roadmap = roadmapWithAuthor(7L);
			UserRoadmap.UserRoadmapId enrollmentId = userRoadmapId(actor.internalId(), roadmapId);
			when(roadmapEntityService.getReference(roadmapId)).thenReturn(roadmap);
			when(learningEnrollmentSupport.userRoadmapId(actor.internalId(), roadmapId)).thenReturn(enrollmentId);
			when(learningEnrollmentEntityService.findUserRoadmapById(enrollmentId)).thenReturn(Optional.empty());
			when(permissionService.userHasPermission(actor, PermissionKeys.ROADMAPS_MANAGE)).thenReturn(true);
			when(permissionService.canManageRoadmap(actor, 7L)).thenReturn(true);

			// When:
			Roadmap result = policy.requireSelfEnrollable(actor, roadmapId);

			// Then:
			assertThat(result).isSameAs(roadmap);
		}

		@Test
		void shouldThrowC001WhenTeamLeadSelfEnrollsInARoadmapAuthoredByAnUnrelatedUserTest() {
			// Given: no enrollment, and the roadmap's author is not this lead's team member
			AppUser actor = teamLeadActor();
			Long roadmapId = 10L;
			Roadmap roadmap = roadmapWithAuthor(99L);
			UserRoadmap.UserRoadmapId enrollmentId = userRoadmapId(actor.internalId(), roadmapId);
			when(roadmapEntityService.getReference(roadmapId)).thenReturn(roadmap);
			when(learningEnrollmentSupport.userRoadmapId(actor.internalId(), roadmapId)).thenReturn(enrollmentId);
			when(learningEnrollmentEntityService.findUserRoadmapById(enrollmentId)).thenReturn(Optional.empty());
			when(permissionService.userHasPermission(actor, PermissionKeys.ROADMAPS_MANAGE)).thenReturn(true);
			when(permissionService.canManageRoadmap(actor, 99L)).thenReturn(false);

			// When-Then:
			AppException thrown = org.junit.jupiter.api.Assertions.assertThrows(AppException.class,
					() -> policy.requireSelfEnrollable(actor, roadmapId));
			assertThat(thrown.getCode()).isEqualTo(ErrorReason.C001.name());
		}

		@Test
		void shouldSucceedForAdminRegardlessOfEnrollmentOrAuthorshipTest() {
			// Given: an admin, no enrollment, roadmap authored by an unrelated user
			AppUser actor = adminActor();
			Long roadmapId = 10L;
			Roadmap roadmap = roadmapWithAuthor(42L);
			UserRoadmap.UserRoadmapId enrollmentId = userRoadmapId(actor.internalId(), roadmapId);
			when(roadmapEntityService.getReference(roadmapId)).thenReturn(roadmap);
			when(learningEnrollmentSupport.userRoadmapId(actor.internalId(), roadmapId)).thenReturn(enrollmentId);
			when(learningEnrollmentEntityService.findUserRoadmapById(enrollmentId)).thenReturn(Optional.empty());

			// When:
			Roadmap result = policy.requireSelfEnrollable(actor, roadmapId);

			// Then: admin short-circuits before any permission/authorship check
			assertThat(result).isSameAs(roadmap);
			verify(permissionService, never()).userHasPermission(eq(actor), eq(PermissionKeys.ROADMAPS_MANAGE));
			verifyNoInteractions(teamEntityService);
		}
	}

	@Test
	void shouldReturnNullManageableIdsForAdminTest() {
		// Given:
		AppUser admin = new AppUser(1L, "clerk-1", "admin@test.com", "Admin", "admin", "Admin", null, null, null);

		// When:
		Set<Long> result = policy.getManageableRoadmapIds(admin, List.of(roadmapWithAuthor(2L)));

		// Then:
		assertThat(result).isNull();
	}

	@Test
	void shouldIncludeOwnRoadmapForNonAdminOwnerTest() {
		// Given:
		AppUser owner = new AppUser(5L, "clerk-5", "owner@test.com", "Owner", "member", "Owner", null, null, null);
		Roadmap roadmap = roadmapWithAuthor(5L);

		// When:
		Set<Long> result = policy.getManageableRoadmapIds(owner, List.of(roadmap));

		// Then:
		assertThat(result).containsExactly(100L);
	}

	@Test
	void shouldIncludeTeamMemberRoadmapsForTeamLeadTest() {
		// Given:
		AppUser lead = new AppUser(6L, "clerk-6", "lead@test.com", "Lead", "teamlead", "Lead", null, null, null);
		Roadmap memberRoadmap = roadmapWithAuthor(7L);
		Roadmap otherRoadmap = roadmapWithAuthor(8L);
		when(teamEntityService.findMemberUserIdsByLeadUserIdAndMemberUserIds(eq(6L), eq(Set.of(7L, 8L))))
				.thenReturn(Set.of(7L));

		// When:
		Set<Long> result = policy.getManageableRoadmapIds(lead, List.of(memberRoadmap, otherRoadmap));

		// Then:
		assertThat(result).containsExactly(100L);
	}

	@Test
	void shouldReturnEmptySetForNonOwnerNonTeamLeadTest() {
		// Given:
		AppUser member = new AppUser(9L, "clerk-9", "member@test.com", "Member", "member", "Member", null, null, null);
		Roadmap roadmap = roadmapWithAuthor(10L);

		// When:
		Set<Long> result = policy.getManageableRoadmapIds(member, List.of(roadmap));

		// Then:
		assertThat(result).isEmpty();
		verifyNoInteractions(teamEntityService);
	}

	private Roadmap roadmapWithAuthor(Long authorId) {
		Roadmap roadmap = new Roadmap();
		roadmap.setId(100L);
		User author = new User();
		author.setId(authorId);
		roadmap.setAuthorUser(author);
		return roadmap;
	}
}
