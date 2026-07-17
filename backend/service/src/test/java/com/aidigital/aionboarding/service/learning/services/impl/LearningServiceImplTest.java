package com.aidigital.aionboarding.service.learning.services.impl;

import com.aidigital.aionboarding.domain.common.dictionary.UserRoleCode;
import com.aidigital.aionboarding.domain.common.dictionary.entities.UserRole;
import com.aidigital.aionboarding.domain.learning.entities.UserLesson;
import com.aidigital.aionboarding.domain.learning.entities.UserRoadmap;
import com.aidigital.aionboarding.domain.lesson.entities.Lesson;
import com.aidigital.aionboarding.domain.roadmap.entities.Roadmap;
import com.aidigital.aionboarding.domain.roadmap.entities.RoadmapTeamAssignment;
import com.aidigital.aionboarding.domain.team.entities.TeamMember;
import com.aidigital.aionboarding.domain.user.entities.User;
import com.aidigital.aionboarding.service.common.error.AppException;
import com.aidigital.aionboarding.service.common.security.AppUser;
import com.aidigital.aionboarding.service.common.time.CurrentTime;
import com.aidigital.aionboarding.service.learning.models.LearningAssigneeRecord;
import com.aidigital.aionboarding.service.learning.models.RoadmapAssignmentEnrollmentRecord;
import com.aidigital.aionboarding.service.learning.models.RoadmapTeamAssignmentRecord;
import com.aidigital.aionboarding.service.learning.models.RoadmapTeamAssignmentResultRecord;
import com.aidigital.aionboarding.service.learning.services.LearningEnrollmentService;
import com.aidigital.aionboarding.service.learning.services.RoadmapEnrollmentSyncService;
import com.aidigital.aionboarding.service.learning.services.entity.LearningEnrollmentEntityService;
import com.aidigital.aionboarding.service.learning.support.LearningActivityCompletionPolicy;
import com.aidigital.aionboarding.service.learning.support.LearningEnrollmentSupport;
import com.aidigital.aionboarding.service.lesson.services.entity.LessonEntityService;
import com.aidigital.aionboarding.service.permission.PermissionKeys;
import com.aidigital.aionboarding.service.permission.services.PermissionService;
import com.aidigital.aionboarding.service.roadmap.services.entity.RoadmapEntityService;
import com.aidigital.aionboarding.service.roadmap.services.entity.RoadmapTeamAssignmentEntityService;
import com.aidigital.aionboarding.service.team.services.TeamService;
import com.aidigital.aionboarding.service.team.services.entity.TeamEntityService;
import com.aidigital.aionboarding.service.user.models.UserRecord;
import com.aidigital.aionboarding.service.user.services.entity.UserEntityService;
import org.instancio.Instancio;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LearningServiceImplTest {

	@Mock
	private LessonEntityService lessonEntityService;
	@Mock
	private LearningEnrollmentEntityService learningEnrollmentEntityService;
	@Mock
	private PermissionService permissionService;
	@Mock
	private TeamService teamService;
	@Mock
	private TeamEntityService teamEntityService;
	@Mock
	private RoadmapEntityService roadmapEntityService;
	@Mock
	private RoadmapTeamAssignmentEntityService roadmapTeamAssignmentEntityService;
	@Mock
	private UserEntityService userEntityService;
	@Mock
	private LearningEnrollmentService learningEnrollmentService;
	@Mock
	private RoadmapEnrollmentSyncService roadmapEnrollmentSyncService;
	@Mock
	private LearningEnrollmentSupport learningEnrollmentSupport;
	@Mock
	private LearningActivityCompletionPolicy learningActivityCompletionPolicy;

	@Spy
	private CurrentTime currentTime = new CurrentTime();

	@InjectMocks
	private LearningServiceImpl service;

	private AppUser adminActor() {
		return new AppUser(1L, "clerk-admin", "admin@test.com", "Admin", "admin", "Admin", null, null, null);
	}

	private AppUser teamLeadActor() {
		return new AppUser(2L, "clerk-tl", "tl@test.com", "Team Lead", "teamlead", "Team Lead", null, null, null);
	}

	private TeamMember teamMember(Long leadUserId, Long memberUserId) {
		TeamMember member = Instancio.create(TeamMember.class);
		TeamMember.TeamMemberId id = new TeamMember.TeamMemberId();
		id.setLeadUserId(leadUserId);
		id.setMemberUserId(memberUserId);
		member.setId(id);
		return member;
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
	class AssignRoadmapToGroupTests {

		@Test
		void assignRoadmapToGroupShouldCreateAssignmentAndEnrollLeadAndCurrentMembersWhenNoneExistsTest() {
			// Given:
			AppUser actor = adminActor();
			Long roadmapId = 10L;
			Long leadUserId = 20L;
			Roadmap roadmap = Instancio.create(Roadmap.class);
			User leadUser = Instancio.create(User.class);
			User actorUser = Instancio.create(User.class);
			RoadmapTeamAssignment savedAssignment = Instancio.create(RoadmapTeamAssignment.class);
			TeamMember member = teamMember(leadUserId, 30L);
			UserRoadmap leadEnrollmentRow = Instancio.create(UserRoadmap.class);
			UserRoadmap memberEnrollmentRow = Instancio.create(UserRoadmap.class);
			RoadmapAssignmentEnrollmentRecord leadEnrollmentRecord =
					Instancio.create(RoadmapAssignmentEnrollmentRecord.class);
			RoadmapAssignmentEnrollmentRecord memberEnrollmentRecord =
					Instancio.create(RoadmapAssignmentEnrollmentRecord.class);
			RoadmapTeamAssignmentRecord assignmentRecord = Instancio.create(RoadmapTeamAssignmentRecord.class);

			when(permissionService.canManageTeam(actor, leadUserId)).thenReturn(true);
			when(roadmapEntityService.getReference(roadmapId)).thenReturn(roadmap);
			when(roadmapTeamAssignmentEntityService.findByRoadmapIdAndLeadUserId(roadmapId, leadUserId))
					.thenReturn(Optional.empty());
			when(userEntityService.getReference(leadUserId)).thenReturn(leadUser);
			when(userEntityService.getReference(actor.internalId())).thenReturn(actorUser);
			when(roadmapTeamAssignmentEntityService.save(org.mockito.ArgumentMatchers.any())).thenReturn(savedAssignment);
			when(teamEntityService.findByLeadUserIdWithMember(leadUserId)).thenReturn(List.of(member));
			when(learningEnrollmentService.enrollUsersInRoadmap(List.of(20L, 30L), roadmapId))
					.thenReturn(List.of(leadEnrollmentRow, memberEnrollmentRow));
			when(learningEnrollmentSupport.toRoadmapAssignmentEnrollment(leadEnrollmentRow, 20L))
					.thenReturn(leadEnrollmentRecord);
			when(learningEnrollmentSupport.toRoadmapAssignmentEnrollment(memberEnrollmentRow, 30L))
					.thenReturn(memberEnrollmentRecord);
			when(learningEnrollmentSupport.toRoadmapTeamAssignment(savedAssignment)).thenReturn(assignmentRecord);

			// When:
			RoadmapTeamAssignmentResultRecord result = service.assignRoadmapToGroup(actor, roadmapId, leadUserId);

			// Then:
			assertThat(result.ok()).isTrue();
			assertThat(result.assignment()).isSameAs(assignmentRecord);
			assertThat(result.enrollments()).containsExactly(leadEnrollmentRecord, memberEnrollmentRecord);
			ArgumentCaptor<RoadmapTeamAssignment> captor = ArgumentCaptor.forClass(RoadmapTeamAssignment.class);
			verify(roadmapTeamAssignmentEntityService).save(captor.capture());
			assertThat(captor.getValue().getRoadmap()).isSameAs(roadmap);
			assertThat(captor.getValue().getLeadUser()).isSameAs(leadUser);
			assertThat(captor.getValue().getAssignedByUser()).isSameAs(actorUser);
		}

		@Test
		void assignRoadmapToGroupShouldReuseExistingAssignmentWithoutDuplicatingWhenAlreadyAssignedTest() {
			// Given:
			AppUser actor = adminActor();
			Long roadmapId = 10L;
			Long leadUserId = 20L;
			Roadmap roadmap = Instancio.create(Roadmap.class);
			RoadmapTeamAssignment existingAssignment = Instancio.create(RoadmapTeamAssignment.class);
			RoadmapTeamAssignmentRecord assignmentRecord = Instancio.create(RoadmapTeamAssignmentRecord.class);
			UserRoadmap leadEnrollmentRow = Instancio.create(UserRoadmap.class);
			RoadmapAssignmentEnrollmentRecord leadEnrollmentRecord =
					Instancio.create(RoadmapAssignmentEnrollmentRecord.class);

			when(permissionService.canManageTeam(actor, leadUserId)).thenReturn(true);
			when(roadmapEntityService.getReference(roadmapId)).thenReturn(roadmap);
			when(roadmapTeamAssignmentEntityService.findByRoadmapIdAndLeadUserId(roadmapId, leadUserId))
					.thenReturn(Optional.of(existingAssignment));
			when(teamEntityService.findByLeadUserIdWithMember(leadUserId)).thenReturn(List.of());
			when(learningEnrollmentService.enrollUsersInRoadmap(List.of(20L), roadmapId)).thenReturn(List.of(leadEnrollmentRow));
			when(learningEnrollmentSupport.toRoadmapAssignmentEnrollment(leadEnrollmentRow, 20L))
					.thenReturn(leadEnrollmentRecord);
			when(learningEnrollmentSupport.toRoadmapTeamAssignment(existingAssignment)).thenReturn(assignmentRecord);

			// When:
			RoadmapTeamAssignmentResultRecord result = service.assignRoadmapToGroup(actor, roadmapId, leadUserId);

			// Then:
			assertThat(result.assignment()).isSameAs(assignmentRecord);
			assertThat(result.enrollments()).containsExactly(leadEnrollmentRecord);
			verify(roadmapTeamAssignmentEntityService, never()).save(org.mockito.ArgumentMatchers.any());
		}

		@Test
		void assignRoadmapToGroupShouldThrowWhenActorCannotManageTeamTest() {
			// Given:
			AppUser actor = adminActor();
			when(permissionService.canManageTeam(actor, 20L)).thenReturn(false);

			// When-Then:
			assertThatThrownBy(() -> service.assignRoadmapToGroup(actor, 10L, 20L))
					.isInstanceOf(AppException.class);
			verifyNoInteractions(roadmapTeamAssignmentEntityService);
		}
	}

	@Nested
	class UnassignRoadmapFromGroupTests {

		@Test
		void unassignRoadmapFromGroupShouldDeleteAssignmentWhenActorCanManageTeamTest() {
			// Given:
			AppUser actor = adminActor();
			when(permissionService.canManageTeam(actor, 20L)).thenReturn(true);
			when(roadmapTeamAssignmentEntityService.findByRoadmapIdAndLeadUserId(10L, 20L))
					.thenReturn(Optional.empty());

			// When:
			service.unassignRoadmapFromGroup(actor, 10L, 20L);

			// Then:
			verify(roadmapTeamAssignmentEntityService).deleteByRoadmapIdAndLeadUserId(10L, 20L);
		}

		@Test
		void unassignRoadmapFromGroupShouldThrowWhenActorCannotManageTeamTest() {
			// Given:
			AppUser actor = adminActor();
			when(permissionService.canManageTeam(actor, 20L)).thenReturn(false);

			// When-Then:
			assertThatThrownBy(() -> service.unassignRoadmapFromGroup(actor, 10L, 20L))
					.isInstanceOf(AppException.class);
			verify(roadmapTeamAssignmentEntityService, never()).deleteByRoadmapIdAndLeadUserId(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyLong());
		}

		@Test
		void unassignRoadmapFromGroupShouldThrowWhenAssignmentWasCreatedByHigherRoleTest() {
			// Given:
			AppUser actor = teamLeadActor();
			RoadmapTeamAssignment assignment = Instancio.create(RoadmapTeamAssignment.class);
			assignment.setAssignedByUser(userWithRole(1L, UserRoleCode.ADMIN));

			when(permissionService.canManageTeam(actor, 20L)).thenReturn(true);
			when(roadmapTeamAssignmentEntityService.findByRoadmapIdAndLeadUserId(10L, 20L))
					.thenReturn(Optional.of(assignment));

			// When-Then:
			assertThatThrownBy(() -> service.unassignRoadmapFromGroup(actor, 10L, 20L))
					.isInstanceOf(AppException.class);
			verify(roadmapTeamAssignmentEntityService, never()).deleteByRoadmapIdAndLeadUserId(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyLong());
		}

		@Test
		void unassignRoadmapFromGroupShouldDeleteAssignmentWhenActorRoleIsHigherThanCreatorRoleTest() {
			// Given:
			AppUser actor = adminActor();
			RoadmapTeamAssignment assignment = Instancio.create(RoadmapTeamAssignment.class);
			assignment.setAssignedByUser(userWithRole(2L, UserRoleCode.TEAMLEAD));

			when(permissionService.canManageTeam(actor, 20L)).thenReturn(true);
			when(roadmapTeamAssignmentEntityService.findByRoadmapIdAndLeadUserId(10L, 20L))
					.thenReturn(Optional.of(assignment));

			// When:
			service.unassignRoadmapFromGroup(actor, 10L, 20L);

			// Then:
			verify(roadmapTeamAssignmentEntityService).deleteByRoadmapIdAndLeadUserId(10L, 20L);
		}
	}

	@Nested
	class GetRoadmapTeamAssignmentsTests {

		@Test
		void getRoadmapTeamAssignmentsShouldReturnOnlyAssignmentsViewerCanManageTest() {
			// Given:
			AppUser viewer = adminActor();
			Long roadmapId = 10L;
			RoadmapTeamAssignment visibleAssignment = Instancio.create(RoadmapTeamAssignment.class);
			RoadmapTeamAssignment hiddenAssignment = Instancio.create(RoadmapTeamAssignment.class);
			User visibleLead = Instancio.create(User.class);
			User hiddenLead = Instancio.create(User.class);
			visibleAssignment.setLeadUser(visibleLead);
			hiddenAssignment.setLeadUser(hiddenLead);
			RoadmapTeamAssignmentRecord visibleRecord = Instancio.create(RoadmapTeamAssignmentRecord.class);

			when(roadmapTeamAssignmentEntityService.findByRoadmapId(roadmapId))
					.thenReturn(List.of(visibleAssignment, hiddenAssignment));
			when(permissionService.canManageTeam(viewer, visibleLead.getId())).thenReturn(true);
			when(permissionService.canManageTeam(viewer, hiddenLead.getId())).thenReturn(false);
			when(learningEnrollmentSupport.toRoadmapTeamAssignment(visibleAssignment)).thenReturn(visibleRecord);

			// When:
			List<RoadmapTeamAssignmentRecord> result = service.getRoadmapTeamAssignments(viewer, roadmapId);

			// Then:
			assertThat(result).containsExactly(visibleRecord);
		}
	}

	@Nested
	class SyncNewTeamMemberEnrollmentsTests {

		@Test
		void syncNewTeamMemberEnrollmentsShouldEnrollMemberIntoEveryStandingAssignedRoadmapTest() {
			// Given:
			Long leadUserId = 20L;
			Long memberUserId = 30L;
			RoadmapTeamAssignment firstAssignment = Instancio.create(RoadmapTeamAssignment.class);
			RoadmapTeamAssignment secondAssignment = Instancio.create(RoadmapTeamAssignment.class);
			Roadmap firstRoadmap = Instancio.create(Roadmap.class);
			Roadmap secondRoadmap = Instancio.create(Roadmap.class);
			firstAssignment.setRoadmap(firstRoadmap);
			secondAssignment.setRoadmap(secondRoadmap);

			when(roadmapTeamAssignmentEntityService.findByLeadUserId(leadUserId))
					.thenReturn(List.of(firstAssignment, secondAssignment));

			// When:
			service.syncNewTeamMemberEnrollments(leadUserId, memberUserId);

			// Then:
			verify(learningEnrollmentService).enrollUsersInRoadmap(List.of(memberUserId), firstRoadmap.getId());
			verify(learningEnrollmentService).enrollUsersInRoadmap(List.of(memberUserId), secondRoadmap.getId());
		}

		@Test
		void syncNewTeamMemberEnrollmentsShouldDoNothingWhenTeamHasNoStandingAssignmentsTest() {
			// Given:
			Long leadUserId = 20L;
			Long memberUserId = 30L;
			when(roadmapTeamAssignmentEntityService.findByLeadUserId(leadUserId)).thenReturn(List.of());

			// When:
			service.syncNewTeamMemberEnrollments(leadUserId, memberUserId);

			// Then:
			verifyNoInteractions(learningEnrollmentService);
		}
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

	private UserRecord assignableUserRecord(Long id) {
		return new UserRecord(id, "clerk-" + id, "User " + id, "user" + id + "@test.com", "member", null, null, null,
				null, null, null);
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

			when(learningEnrollmentService.requireEnrollableLesson(lessonId)).thenReturn(lesson);
			when(learningEnrollmentEntityService.findByLessonIdWithUser(lessonId)).thenReturn(List.of(enrollment));

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

			when(learningEnrollmentService.requireEnrollableLesson(lessonId)).thenReturn(lesson);
			when(learningEnrollmentEntityService.findByLessonIdWithUser(lessonId)).thenReturn(List.of(enrollment));

			// When:
			List<LearningAssigneeRecord> result = service.listLessonAssignees(actor, lessonId);

			// Then:
			assertThat(result).containsExactly(
					new LearningAssigneeRecord(20L, user.getName(), user.getEmail(), enrolledAt, true)
			);
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
			when(teamService.getAssignableLearningUsers(actor))
					.thenReturn(List.of(assignableUserRecord(20L), assignableUserRecord(21L)));
			when(learningEnrollmentService.requireEnrollableLesson(lessonId)).thenReturn(lesson);

			// When:
			service.revokeLessonAssignments(actor, lessonId, userIds);

			// Then:
			verify(permissionService).requirePermission(actor, PermissionKeys.LEARNING_ASSIGN);
			verify(learningEnrollmentService).unenrollUsersFromLesson(userIds, lessonId);
		}

		@Test
		void revokeLessonAssignmentsShouldThrowAndUnenrollNoOneWhenAnyTargetIsNotManageableTest() {
			// Given: only one of the two selected users is assignable to this actor
			AppUser actor = teamLeadActor();
			Long lessonId = 10L;
			List<Long> userIds = List.of(20L, 21L);

			when(learningEnrollmentSupport.normalizeUserIds(userIds)).thenReturn(userIds);
			when(teamService.getAssignableLearningUsers(actor)).thenReturn(List.of(assignableUserRecord(20L)));

			// When-Then: the whole bulk request is rejected, not just the unmanageable target
			assertThatThrownBy(() -> service.revokeLessonAssignments(actor, lessonId, userIds))
					.isInstanceOf(AppException.class);
			verify(learningEnrollmentService, never()).unenrollUsersFromLesson(any(), any());
		}
	}

	@Nested
	class ListRoadmapAssigneesTests {

		@Test
		void listRoadmapAssigneesShouldReturnEnrolledLearnersTest() {
			// Given:
			AppUser actor = adminActor();
			Long roadmapId = 10L;
			User user = userWithRole(20L, UserRoleCode.MEMBER);
			LocalDateTime enrolledAt = LocalDateTime.of(2026, 1, 1, 0, 0);
			UserRoadmap enrollment = userRoadmap(20L, roadmapId, user, enrolledAt);

			when(learningEnrollmentEntityService.findByRoadmapIdWithUser(roadmapId)).thenReturn(List.of(enrollment));

			// When:
			List<LearningAssigneeRecord> result = service.listRoadmapAssignees(actor, roadmapId);

			// Then:
			assertThat(result).containsExactly(
					new LearningAssigneeRecord(20L, user.getName(), user.getEmail(), enrolledAt, null)
			);
			verify(permissionService).requirePermission(actor, PermissionKeys.LEARNING_ASSIGN);
			verify(roadmapEntityService).getReference(roadmapId);
		}
	}

	@Nested
	class RevokeRoadmapAssignmentTests {

		@Test
		void revokeRoadmapAssignmentsShouldUnenrollUsersWhenAllTargetsAreAssignableTest() {
			// Given:
			AppUser actor = adminActor();
			Long roadmapId = 10L;
			List<Long> userIds = List.of(20L, 21L);

			when(learningEnrollmentSupport.normalizeUserIds(userIds)).thenReturn(userIds);
			when(teamService.getAssignableLearningUsers(actor))
					.thenReturn(List.of(assignableUserRecord(20L), assignableUserRecord(21L)));

			// When:
			service.revokeRoadmapAssignments(actor, roadmapId, userIds);

			// Then:
			verify(permissionService).requirePermission(actor, PermissionKeys.LEARNING_ASSIGN);
			verify(roadmapEntityService).getReference(roadmapId);
			verify(learningEnrollmentService).unenrollUsersFromRoadmap(userIds, roadmapId);
		}

		@Test
		void revokeRoadmapAssignmentsShouldThrowAndUnenrollNoOneWhenAnyTargetIsNotManageableTest() {
			// Given: only one of the two selected users is assignable to this actor
			AppUser actor = teamLeadActor();
			Long roadmapId = 10L;
			List<Long> userIds = List.of(20L, 21L);

			when(learningEnrollmentSupport.normalizeUserIds(userIds)).thenReturn(userIds);
			when(teamService.getAssignableLearningUsers(actor)).thenReturn(List.of(assignableUserRecord(20L)));

			// When-Then: the whole bulk request is rejected, not just the unmanageable target
			assertThatThrownBy(() -> service.revokeRoadmapAssignments(actor, roadmapId, userIds))
					.isInstanceOf(AppException.class);
			verify(learningEnrollmentService, never()).unenrollUsersFromRoadmap(any(), any());
		}
	}
}
