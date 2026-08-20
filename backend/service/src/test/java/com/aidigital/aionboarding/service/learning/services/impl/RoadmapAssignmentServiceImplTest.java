package com.aidigital.aionboarding.service.learning.services.impl;

import com.aidigital.aionboarding.domain.learning.entities.UserRoadmap;
import com.aidigital.aionboarding.domain.user.entities.User;
import com.aidigital.aionboarding.service.common.error.AppException;
import com.aidigital.aionboarding.service.common.security.AppUser;
import com.aidigital.aionboarding.service.learning.models.LearningAssigneeRecord;
import com.aidigital.aionboarding.service.learning.models.RoadmapAssignmentEnrollmentRecord;
import com.aidigital.aionboarding.service.learning.models.RoadmapAssignmentResultRecord;
import com.aidigital.aionboarding.service.learning.models.RoadmapEnrollmentRecord;
import com.aidigital.aionboarding.service.learning.models.RoadmapEnrollmentResultRecord;
import com.aidigital.aionboarding.service.learning.services.RoadmapEnrollmentService;
import com.aidigital.aionboarding.service.learning.services.entity.LearningEnrollmentEntityService;
import com.aidigital.aionboarding.service.learning.support.LearningAssignmentAccessPolicy;
import com.aidigital.aionboarding.service.learning.support.LearningEnrollmentSupport;
import com.aidigital.aionboarding.service.permission.PermissionKeys;
import com.aidigital.aionboarding.service.permission.services.PermissionService;
import com.aidigital.aionboarding.service.roadmap.services.entity.RoadmapEntityService;
import com.aidigital.aionboarding.service.roadmap.support.RoadmapAccessPolicy;
import org.instancio.Instancio;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoadmapAssignmentServiceImplTest {

	@Mock
	private PermissionService permissionService;
	@Mock
	private LearningEnrollmentEntityService learningEnrollmentEntityService;
	@Mock
	private RoadmapEnrollmentService roadmapEnrollmentService;
	@Mock
	private LearningEnrollmentSupport learningEnrollmentSupport;
	@Mock
	private RoadmapEntityService roadmapEntityService;
	@Mock
	private LearningAssignmentAccessPolicy learningAssignmentAccessPolicy;
	@Mock
	private RoadmapAccessPolicy roadmapAccessPolicy;

	@InjectMocks
	private RoadmapAssignmentServiceImpl service;

	private AppUser adminActor() {
		return new AppUser(1L, "clerk-admin", "admin@test.com", "Admin", "admin", "Admin", null, null, null);
	}

	private UserRoadmap userRoadmap(Long userId, Long roadmapId, User user, LocalDateTime enrolledAt) {
		UserRoadmap userRoadmap = new UserRoadmap();
		UserRoadmap.UserRoadmapId id = new UserRoadmap.UserRoadmapId();
		id.setUserId(userId);
		id.setRoadmapId(roadmapId);
		userRoadmap.setId(id);
		userRoadmap.setUser(user);
		userRoadmap.setEnrolledAt(enrolledAt);
		return userRoadmap;
	}

	@Nested
	class AssignRoadmap {

		@Test
		void shouldEnrollAllManageableTargetsTest() {
			// Given:
			AppUser actor = adminActor();
			Long roadmapId = 10L;
			List<Long> userIds = List.of(20L, 21L);
			UserRoadmap firstRow = Instancio.create(UserRoadmap.class);
			UserRoadmap secondRow = Instancio.create(UserRoadmap.class);
			RoadmapAssignmentEnrollmentRecord firstRecord = Instancio.create(RoadmapAssignmentEnrollmentRecord.class);
			RoadmapAssignmentEnrollmentRecord secondRecord = Instancio.create(RoadmapAssignmentEnrollmentRecord.class);

			when(learningEnrollmentSupport.normalizeUserIds(userIds)).thenReturn(userIds);
			when(roadmapEnrollmentService.enrollUsersInRoadmap(userIds, roadmapId)).thenReturn(List.of(firstRow, secondRow));
			when(learningEnrollmentSupport.toRoadmapAssignmentEnrollment(firstRow, 20L)).thenReturn(firstRecord);
			when(learningEnrollmentSupport.toRoadmapAssignmentEnrollment(secondRow, 21L)).thenReturn(secondRecord);

			// When:
			RoadmapAssignmentResultRecord result = service.assignRoadmap(actor, roadmapId, userIds);

			// Then:
			assertThat(result.ok()).isTrue();
			assertThat(result.enrollments()).containsExactly(firstRecord, secondRecord);
			verify(permissionService).requirePermission(actor, PermissionKeys.LEARNING_ASSIGN);
			verify(learningAssignmentAccessPolicy).requireAssignableTargets(actor, userIds, "You can assign roadmaps only to manageable users.");
		}

		@Test
		void shouldThrowWhenNoUserIdsSelectedTest() {
			// Given:
			AppUser actor = adminActor();
			when(learningEnrollmentSupport.normalizeUserIds(List.of())).thenReturn(List.of());

			// When-Then:
			assertThatThrownBy(() -> service.assignRoadmap(actor, 10L, List.of()))
					.isInstanceOf(AppException.class);
			verifyNoInteractions(learningAssignmentAccessPolicy);
		}
	}

	@Nested
	class ListRoadmapAssignees {

		@Test
		void listRoadmapAssigneesShouldReturnOnlyManageableEnrolledLearnersTest() {
			// Given:
			AppUser actor = adminActor();
			Long roadmapId = 10L;
			User user = Instancio.create(User.class);
			LocalDateTime enrolledAt = LocalDateTime.of(2026, 1, 1, 0, 0);
			UserRoadmap enrollment = userRoadmap(20L, roadmapId, user, enrolledAt);

			when(learningEnrollmentEntityService.findByRoadmapIdWithUser(roadmapId)).thenReturn(List.of(enrollment));
			when(learningAssignmentAccessPolicy.assignableUserIds(actor)).thenReturn(java.util.Set.of(20L));

			// When:
			List<LearningAssigneeRecord> result = service.listRoadmapAssignees(actor, roadmapId);

			// Then:
			assertThat(result).containsExactly(
					new LearningAssigneeRecord(20L, user.getName(), user.getEmail(), enrolledAt, null)
			);
			verify(permissionService).requirePermission(actor, PermissionKeys.LEARNING_ASSIGN);
			verify(roadmapEntityService).getReference(roadmapId);
		}

		@Test
		void listRoadmapAssigneesShouldExcludeEnrolleesTheActorCannotManageTest() {
			// Given: an enrollee outside the actor's manageable set (e.g. a team lead with no
			// connection to this roadmap's enrollee) must never appear in the roster — this is the
			// specific leak this test guards against.
			AppUser actor = adminActor();
			Long roadmapId = 10L;
			User manageableUser = Instancio.create(User.class);
			User unmanageableUser = Instancio.create(User.class);
			LocalDateTime enrolledAt = LocalDateTime.of(2026, 1, 1, 0, 0);
			UserRoadmap manageableEnrollment = userRoadmap(20L, roadmapId, manageableUser, enrolledAt);
			UserRoadmap unmanageableEnrollment = userRoadmap(21L, roadmapId, unmanageableUser, enrolledAt);

			when(learningEnrollmentEntityService.findByRoadmapIdWithUser(roadmapId))
					.thenReturn(List.of(manageableEnrollment, unmanageableEnrollment));
			when(learningAssignmentAccessPolicy.assignableUserIds(actor)).thenReturn(java.util.Set.of(20L));

			// When:
			List<LearningAssigneeRecord> result = service.listRoadmapAssignees(actor, roadmapId);

			// Then:
			assertThat(result).extracting(LearningAssigneeRecord::userId).containsExactly(20L);
		}

		@Test
		void listRoadmapAssigneesShouldReturnEmptyWhenActorMayManageNoOneTest() {
			// Given: an actor (e.g. a plain member holding learning.assign via an override) with
			// no manageable learners at all
			AppUser actor = adminActor();
			Long roadmapId = 10L;
			UserRoadmap enrollment = userRoadmap(20L, roadmapId, Instancio.create(User.class),
					LocalDateTime.of(2026, 1, 1, 0, 0));
			when(learningEnrollmentEntityService.findByRoadmapIdWithUser(roadmapId)).thenReturn(List.of(enrollment));
			when(learningAssignmentAccessPolicy.assignableUserIds(actor)).thenReturn(java.util.Set.of());

			// When:
			List<LearningAssigneeRecord> result = service.listRoadmapAssignees(actor, roadmapId);

			// Then:
			assertThat(result).isEmpty();
		}
	}

	@Nested
	class RevokeRoadmapAssignments {

		@Test
		void revokeRoadmapAssignmentsShouldUnenrollUsersWhenAllTargetsAreAssignableTest() {
			// Given:
			AppUser actor = adminActor();
			Long roadmapId = 10L;
			List<Long> userIds = List.of(20L, 21L);
			when(learningEnrollmentSupport.normalizeUserIds(userIds)).thenReturn(userIds);

			// When:
			service.revokeRoadmapAssignments(actor, roadmapId, userIds);

			// Then:
			verify(permissionService).requirePermission(actor, PermissionKeys.LEARNING_ASSIGN);
			verify(roadmapEntityService).getReference(roadmapId);
			verify(roadmapEnrollmentService).unenrollUsersFromRoadmap(userIds, roadmapId);
		}

		@Test
		void revokeRoadmapAssignmentsShouldThrowAndUnenrollNoOneWhenAnyTargetIsNotManageableTest() {
			// Given:
			AppUser actor = adminActor();
			Long roadmapId = 10L;
			List<Long> userIds = List.of(20L, 21L);
			when(learningEnrollmentSupport.normalizeUserIds(userIds)).thenReturn(userIds);
			doThrow(new AppException(com.aidigital.aionboarding.service.common.error.ErrorReason.C004, "forbidden"))
					.when(learningAssignmentAccessPolicy).requireAssignableTargets(actor, userIds,
							"You can revoke roadmap assignments only for manageable users.");

			// When-Then:
			assertThatThrownBy(() -> service.revokeRoadmapAssignments(actor, roadmapId, userIds))
					.isInstanceOf(AppException.class);
			verify(roadmapEnrollmentService, never()).unenrollUsersFromRoadmap(any(), any());
		}
	}

	@Nested
	class EnrollRoadmap {

		private AppUser memberActor() {
			return new AppUser(3L, "clerk-member", "member@test.com", "Member", "member", "Member", null, null, null);
		}

		@Test
		void shouldEnrollWhenRoadmapAccessPolicyDeemsTheActorSelfEnrollableTest() {
			// Given: admin, or any actor RoadmapAccessPolicy has already cleared
			AppUser user = adminActor();
			Long roadmapId = 10L;
			UserRoadmap enrollment = Instancio.create(UserRoadmap.class);
			RoadmapEnrollmentRecord enrollmentRecord = Instancio.create(RoadmapEnrollmentRecord.class);
			when(roadmapEnrollmentService.enrollUserInRoadmap(user.internalId(), roadmapId)).thenReturn(enrollment);
			when(learningEnrollmentSupport.toRoadmapEnrollment(enrollment)).thenReturn(enrollmentRecord);

			// When:
			RoadmapEnrollmentResultRecord result = service.enrollRoadmap(user, roadmapId);

			// Then:
			assertThat(result.ok()).isTrue();
			assertThat(result.enrollment()).isSameAs(enrollmentRecord);
			verify(permissionService).requirePermission(user, PermissionKeys.LEARNING_ENROLL);
			verify(roadmapAccessPolicy).requireSelfEnrollable(user, roadmapId);
		}

		@Test
		void shouldRejectWithC001AndNeverCallEnrollmentOrFanOutWhenActorIsNotSelfEnrollableTest() {
			// Given: pins the actual defect — a Member holding only learning.enroll, with no
			// enrollment and no manage relationship to this roadmap, must never reach
			// roadmapEnrollmentService.enrollUserInRoadmap, which is what drives the private-lesson
			// fan-out (RoadmapEnrollmentServiceImpl.fanOutRoadmapLessons uses isLearnable, which
			// includes private lessons since Phase C).
			AppUser user = memberActor();
			Long roadmapId = 10L;
			doThrow(new AppException(com.aidigital.aionboarding.service.common.error.ErrorReason.C001, roadmapId))
					.when(roadmapAccessPolicy).requireSelfEnrollable(user, roadmapId);

			// When-Then:
			AppException thrown = org.junit.jupiter.api.Assertions.assertThrows(AppException.class,
					() -> service.enrollRoadmap(user, roadmapId));
			assertThat(thrown.getCode()).isEqualTo(com.aidigital.aionboarding.service.common.error.ErrorReason.C001.name());
			verifyNoInteractions(roadmapEnrollmentService);
		}
	}

	@Nested
	class UnenrollRoadmap {

		@Test
		void shouldUnenrollAuthenticatedLearnerTest() {
			// Given:
			AppUser user = adminActor();
			Long roadmapId = 10L;

			// When:
			service.unenrollRoadmap(user, roadmapId);

			// Then:
			verify(permissionService).requirePermission(user, PermissionKeys.LEARNING_ENROLL);
			verify(roadmapEnrollmentService).unenrollUserFromRoadmap(user.internalId(), roadmapId);
		}
	}
}
