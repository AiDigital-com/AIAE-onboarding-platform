package com.aidigital.aionboarding.service.learning.services.impl;

import com.aidigital.aionboarding.domain.common.dictionary.UserRoleCode;
import com.aidigital.aionboarding.domain.learning.entities.UserLesson;
import com.aidigital.aionboarding.domain.lesson.entities.Lesson;
import com.aidigital.aionboarding.domain.user.entities.User;
import com.aidigital.aionboarding.service.common.error.AppException;
import com.aidigital.aionboarding.service.common.security.AppUser;
import com.aidigital.aionboarding.service.common.time.CurrentTime;
import com.aidigital.aionboarding.service.common.time.CurrentTimeImpl;
import com.aidigital.aionboarding.service.learning.models.LearningAssigneeRecord;
import com.aidigital.aionboarding.service.learning.services.LearningEnrollmentService;
import com.aidigital.aionboarding.service.learning.services.entity.LearningEnrollmentEntityService;
import com.aidigital.aionboarding.service.learning.support.LearningAssignmentAccessPolicy;
import com.aidigital.aionboarding.service.learning.support.LearningEnrollmentSupport;
import com.aidigital.aionboarding.service.learning.support.LessonCompletionWorkflow;
import com.aidigital.aionboarding.service.permission.PermissionKeys;
import com.aidigital.aionboarding.service.permission.services.PermissionService;
import org.instancio.Instancio;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LearningServiceImplTest {

	@Mock
	private LearningEnrollmentEntityService learningEnrollmentEntityService;
	@Mock
	private PermissionService permissionService;
	@Mock
	private LearningEnrollmentService learningEnrollmentService;
	@Mock
	private LearningEnrollmentSupport learningEnrollmentSupport;
	@Mock
	private LearningAssignmentAccessPolicy learningAssignmentAccessPolicy;
	@Mock
	private LessonCompletionWorkflow lessonCompletionWorkflow;

	@Spy
	private CurrentTime currentTime = new CurrentTimeImpl();

	@InjectMocks
	private LearningServiceImpl service;

	private AppUser adminActor() {
		return new AppUser(1L, "clerk-admin", "admin@test.com", "Admin", "admin", "Admin", null, null, null);
	}

	private User userWithRole(Long id, String roleCode) {
		User user = Instancio.create(User.class);
		user.setId(id);
		com.aidigital.aionboarding.domain.common.dictionary.entities.UserRole role =
				new com.aidigital.aionboarding.domain.common.dictionary.entities.UserRole();
		role.setCode(roleCode);
		role.setName(roleCode);
		role.setDisplayOrder(1);
		role.setIsActive(true);
		user.setRole(role);
		return user;
	}

	private UserLesson userLesson(Long userId, Long lessonId, User user, LocalDateTime enrolledAt,
								  LocalDateTime completedAt) {
		UserLesson userLesson = new UserLesson();
		UserLesson.UserLessonId id = new UserLesson.UserLessonId();
		id.setUserId(userId);
		id.setLessonId(lessonId);
		userLesson.setId(id);
		userLesson.setUser(user);
		userLesson.setEnrolledAt(enrolledAt);
		userLesson.setCompletedAt(completedAt);
		return userLesson;
	}

	@Nested
	class ListLessonAssigneesTests {

		@Test
		void listLessonAssigneesShouldReturnEnrolledLearnersNewestFirstTest() {
			// Given:
			AppUser actor = adminActor();
			Long lessonId = 10L;
			Lesson lesson = Instancio.create(Lesson.class);
			User user = userWithRole(20L, UserRoleCode.MEMBER);
			LocalDateTime enrolledAt = LocalDateTime.of(2026, 1, 1, 0, 0);
			UserLesson enrollment = userLesson(20L, lessonId, user, enrolledAt, null);

			when(learningEnrollmentService.requireLearnableLesson(lessonId)).thenReturn(lesson);
			when(learningEnrollmentEntityService.findByLessonIdWithUser(lessonId)).thenReturn(List.of(enrollment));
			when(learningAssignmentAccessPolicy.assignableUserIds(actor)).thenReturn(java.util.Set.of(20L));

			// When:
			List<LearningAssigneeRecord> result = service.listLessonAssignees(actor, lessonId);

			// Then:
			assertThat(result).containsExactly(
					new LearningAssigneeRecord(20L, user.getName(), user.getEmail(), enrolledAt, false)
			);
			verify(permissionService).requirePermission(actor, PermissionKeys.LEARNING_ASSIGN);
		}

		@Test
		void listLessonAssigneesShouldMarkCompletedLearnersTest() {
			// Given:
			AppUser actor = adminActor();
			Long lessonId = 10L;
			Lesson lesson = Instancio.create(Lesson.class);
			User user = userWithRole(20L, UserRoleCode.MEMBER);
			LocalDateTime enrolledAt = LocalDateTime.of(2026, 1, 1, 0, 0);
			LocalDateTime completedAt = LocalDateTime.of(2026, 1, 2, 0, 0);
			UserLesson enrollment = userLesson(20L, lessonId, user, enrolledAt, completedAt);

			when(learningEnrollmentService.requireLearnableLesson(lessonId)).thenReturn(lesson);
			when(learningEnrollmentEntityService.findByLessonIdWithUser(lessonId)).thenReturn(List.of(enrollment));
			when(learningAssignmentAccessPolicy.assignableUserIds(actor)).thenReturn(java.util.Set.of(20L));

			// When:
			List<LearningAssigneeRecord> result = service.listLessonAssignees(actor, lessonId);

			// Then:
			assertThat(result).containsExactly(
					new LearningAssigneeRecord(20L, user.getName(), user.getEmail(), enrolledAt, true)
			);
		}

		@Test
		void listLessonAssigneesShouldExcludeEnrolleesTheActorCannotManageTest() {
			// Given: an enrollee outside the actor's manageable set (e.g. a team lead with no
			// connection to this enrollee) must never appear in the roster.
			AppUser actor = adminActor();
			Long lessonId = 10L;
			Lesson lesson = Instancio.create(Lesson.class);
			User manageableUser = userWithRole(20L, UserRoleCode.MEMBER);
			User unmanageableUser = userWithRole(21L, UserRoleCode.MEMBER);
			LocalDateTime enrolledAt = LocalDateTime.of(2026, 1, 1, 0, 0);
			UserLesson manageableEnrollment = userLesson(20L, lessonId, manageableUser, enrolledAt, null);
			UserLesson unmanageableEnrollment = userLesson(21L, lessonId, unmanageableUser, enrolledAt, null);

			when(learningEnrollmentService.requireLearnableLesson(lessonId)).thenReturn(lesson);
			when(learningEnrollmentEntityService.findByLessonIdWithUser(lessonId))
					.thenReturn(List.of(manageableEnrollment, unmanageableEnrollment));
			when(learningAssignmentAccessPolicy.assignableUserIds(actor)).thenReturn(java.util.Set.of(20L));

			// When:
			List<LearningAssigneeRecord> result = service.listLessonAssignees(actor, lessonId);

			// Then:
			assertThat(result).extracting(LearningAssigneeRecord::userId).containsExactly(20L);
		}

		@Test
		void listLessonAssigneesShouldReturnEmptyWhenActorMayManageNoOneTest() {
			// Given:
			AppUser actor = adminActor();
			Long lessonId = 10L;
			Lesson lesson = Instancio.create(Lesson.class);
			UserLesson enrollment = userLesson(20L, lessonId, userWithRole(20L, UserRoleCode.MEMBER),
					LocalDateTime.of(2026, 1, 1, 0, 0), null);

			when(learningEnrollmentService.requireLearnableLesson(lessonId)).thenReturn(lesson);
			when(learningEnrollmentEntityService.findByLessonIdWithUser(lessonId)).thenReturn(List.of(enrollment));
			when(learningAssignmentAccessPolicy.assignableUserIds(actor)).thenReturn(java.util.Set.of());

			// When:
			List<LearningAssigneeRecord> result = service.listLessonAssignees(actor, lessonId);

			// Then:
			assertThat(result).isEmpty();
		}
	}

	@Nested
	class RevokeLessonAssignmentTests {

		@Test
		void revokeLessonAssignmentsShouldUnenrollUsersWhenAllTargetsAreAssignableTest() {
			// Given:
			AppUser actor = adminActor();
			Long lessonId = 10L;
			List<Long> userIds = List.of(20L, 21L);
			Lesson lesson = Instancio.create(Lesson.class);

			when(learningEnrollmentSupport.normalizeUserIds(userIds)).thenReturn(userIds);
			when(learningEnrollmentService.requireLearnableLesson(lessonId)).thenReturn(lesson);

			// When:
			service.revokeLessonAssignments(actor, lessonId, userIds);

			// Then:
			verify(permissionService).requirePermission(actor, PermissionKeys.LEARNING_ASSIGN);
			verify(learningAssignmentAccessPolicy).requireAssignableTargets(
					actor, userIds, "You can revoke lesson assignments only for manageable users.");
			verify(learningEnrollmentService).unenrollUsersFromLesson(userIds, lessonId);
		}

		@Test
		void revokeLessonAssignmentsShouldThrowAndUnenrollNoOneWhenAnyTargetIsNotManageableTest() {
			// Given: only one of the two selected users is assignable to this actor
			AppUser actor = adminActor();
			Long lessonId = 10L;
			List<Long> userIds = List.of(20L, 21L);

			when(learningEnrollmentSupport.normalizeUserIds(userIds)).thenReturn(userIds);
			doThrow(new AppException(com.aidigital.aionboarding.service.common.error.ErrorReason.C004, "forbidden"))
					.when(learningAssignmentAccessPolicy).requireAssignableTargets(
							actor, userIds, "You can revoke lesson assignments only for manageable users.");

			// When-Then: the whole bulk request is rejected, not just the unmanageable target
			assertThatThrownBy(() -> service.revokeLessonAssignments(actor, lessonId, userIds))
					.isInstanceOf(AppException.class);
			verify(learningEnrollmentService, never()).unenrollUsersFromLesson(any(), any());
		}
	}

	@Nested
	class AssignLessonTests {

		@Test
		void assignLessonShouldEnrollAllManageableTargetsTest() {
			// Given:
			AppUser actor = adminActor();
			Long lessonId = 10L;
			List<Long> userIds = List.of(20L, 21L);
			Lesson lesson = Instancio.create(Lesson.class);
			UserLesson firstRow = Instancio.create(UserLesson.class);
			UserLesson secondRow = Instancio.create(UserLesson.class);
			com.aidigital.aionboarding.service.learning.models.LessonAssignmentEnrollmentRecord firstRecord =
					Instancio.create(com.aidigital.aionboarding.service.learning.models.LessonAssignmentEnrollmentRecord.class);
			com.aidigital.aionboarding.service.learning.models.LessonAssignmentEnrollmentRecord secondRecord =
					Instancio.create(com.aidigital.aionboarding.service.learning.models.LessonAssignmentEnrollmentRecord.class);

			when(learningEnrollmentSupport.normalizeUserIds(userIds)).thenReturn(userIds);
			when(learningEnrollmentService.requireLearnableLesson(lessonId)).thenReturn(lesson);
			when(learningEnrollmentService.enrollUsersInLesson(eq(userIds), eq(lesson), any(), eq(false)))
					.thenReturn(List.of(firstRow, secondRow));
			when(learningEnrollmentSupport.toLessonAssignmentEnrollment(firstRow, 20L)).thenReturn(firstRecord);
			when(learningEnrollmentSupport.toLessonAssignmentEnrollment(secondRow, 21L)).thenReturn(secondRecord);

			// When:
			com.aidigital.aionboarding.service.learning.models.LessonAssignmentResultRecord result =
					service.assignLesson(actor, lessonId, userIds);

			// Then:
			assertThat(result.ok()).isTrue();
			assertThat(result.enrollments()).containsExactly(firstRecord, secondRecord);
			verify(learningAssignmentAccessPolicy).requireAssignableTargets(
					actor, userIds, "You can assign lessons only to manageable users.");
		}

		@Test
		void assignLessonShouldThrowWhenNoUserIdsSelectedTest() {
			// Given:
			AppUser actor = adminActor();
			when(learningEnrollmentSupport.normalizeUserIds(List.of())).thenReturn(List.of());

			// When-Then:
			assertThatThrownBy(() -> service.assignLesson(actor, 10L, List.of()))
					.isInstanceOf(AppException.class);
		}
	}

	@Nested
	class EnrollLessonTests {

		@Test
		void shouldEnrollAuthenticatedLearnerTest() {
			// Given:
			AppUser user = adminActor();
			Long lessonId = 10L;
			Lesson lesson = Instancio.create(Lesson.class);
			UserLesson enrollment = Instancio.create(UserLesson.class);
			com.aidigital.aionboarding.service.learning.models.LessonEnrollmentRecord enrollmentRecord =
					Instancio.create(com.aidigital.aionboarding.service.learning.models.LessonEnrollmentRecord.class);
			when(learningEnrollmentService.requireSelfEnrollableLesson(lessonId)).thenReturn(lesson);
			when(learningEnrollmentService.enrollUserInLesson(eq(user.internalId()), eq(lesson), any(), eq(false)))
					.thenReturn(enrollment);
			when(learningEnrollmentSupport.toLessonEnrollment(enrollment)).thenReturn(enrollmentRecord);

			// When:
			com.aidigital.aionboarding.service.learning.models.LessonEnrollmentResultRecord result =
					service.enrollLesson(user, lessonId);

			// Then:
			assertThat(result.ok()).isTrue();
			assertThat(result.enrollment()).isSameAs(enrollmentRecord);
			assertThat(result.completedRoadmaps()).isEmpty();
			verify(permissionService).requirePermission(user, PermissionKeys.LEARNING_ENROLL);
		}
	}

	@Nested
	class UnenrollLessonTests {

		@Test
		void shouldUnenrollAuthenticatedLearnerTest() {
			// Given:
			AppUser user = adminActor();
			Long lessonId = 10L;

			// When:
			service.unenrollLesson(user, lessonId);

			// Then:
			verify(permissionService).requirePermission(user, PermissionKeys.LEARNING_ENROLL);
			verify(learningEnrollmentService).unenrollUserFromLesson(user.internalId(), lessonId);
		}
	}

	@Nested
	class SetLessonCompletionTests {

		@Test
		void shouldRequirePermissionAndDelegateToWorkflowTest() {
			// Given:
			AppUser user = adminActor();
			Long lessonId = 10L;
			com.aidigital.aionboarding.service.learning.models.LessonEnrollmentResultRecord workflowResult =
					Instancio.create(com.aidigital.aionboarding.service.learning.models.LessonEnrollmentResultRecord.class);
			when(lessonCompletionWorkflow.setLessonCompletion(user.internalId(), lessonId, true)).thenReturn(workflowResult);

			// When:
			com.aidigital.aionboarding.service.learning.models.LessonEnrollmentResultRecord result =
					service.setLessonCompletion(user, lessonId, true);

			// Then:
			assertThat(result).isSameAs(workflowResult);
			verify(permissionService).requirePermission(user, PermissionKeys.LEARNING_COMPLETE);
		}
	}
}
