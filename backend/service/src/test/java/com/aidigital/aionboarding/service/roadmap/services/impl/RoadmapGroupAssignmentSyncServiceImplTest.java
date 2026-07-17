package com.aidigital.aionboarding.service.roadmap.services.impl;

import com.aidigital.aionboarding.domain.grade.entities.Grade;
import com.aidigital.aionboarding.domain.group.entities.GroupMember;
import com.aidigital.aionboarding.domain.learning.entities.UserRoadmap;
import com.aidigital.aionboarding.domain.roadmap.entities.Roadmap;
import com.aidigital.aionboarding.domain.roadmap.entities.RoadmapGroupAssignment;
import com.aidigital.aionboarding.domain.roadmap.entities.RoadmapGroupAssignmentGrade;
import com.aidigital.aionboarding.domain.user.entities.User;
import com.aidigital.aionboarding.service.group.services.entity.GroupMemberEntityService;
import com.aidigital.aionboarding.service.learning.models.RoadmapAssignmentEnrollmentRecord;
import com.aidigital.aionboarding.service.learning.services.LearningEnrollmentService;
import com.aidigital.aionboarding.service.learning.support.LearningEnrollmentSupport;
import com.aidigital.aionboarding.service.roadmap.services.entity.RoadmapGroupAssignmentEntityService;
import com.aidigital.aionboarding.service.user.services.entity.UserEntityService;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoadmapGroupAssignmentSyncServiceImplTest {

	@Mock
	private RoadmapGroupAssignmentEntityService roadmapGroupAssignmentEntityService;
	@Mock
	private GroupMemberEntityService groupMemberEntityService;
	@Mock
	private UserEntityService userEntityService;
	@Mock
	private LearningEnrollmentService learningEnrollmentService;
	@Mock
	private LearningEnrollmentSupport learningEnrollmentSupport;

	@InjectMocks
	private RoadmapGroupAssignmentSyncServiceImpl service;

	private GroupMember member(Long groupId, Long userId) {
		GroupMember groupMember = new GroupMember();
		GroupMember.GroupMemberId id = new GroupMember.GroupMemberId();
		id.setGroupId(groupId);
		id.setMemberUserId(userId);
		groupMember.setId(id);
		return groupMember;
	}

	private RoadmapGroupAssignment assignment(Long assignmentId, Long roadmapId, Long groupId) {
		RoadmapGroupAssignment assignment = new RoadmapGroupAssignment();
		assignment.setId(assignmentId);
		Roadmap roadmap = new Roadmap();
		roadmap.setId(roadmapId);
		assignment.setRoadmap(roadmap);
		return assignment;
	}

	private RoadmapGroupAssignmentGrade gradeFilterRow(Long gradeId) {
		RoadmapGroupAssignmentGrade row = new RoadmapGroupAssignmentGrade();
		Grade grade = new Grade();
		grade.setId(gradeId);
		row.setGrade(grade);
		return row;
	}

	private User userWithGrade(Long userId, Long gradeId) {
		User user = new User();
		user.setId(userId);
		if (gradeId != null) {
			Grade grade = new Grade();
			grade.setId(gradeId);
			user.setGrade(grade);
		}
		return user;
	}

	@Nested
	class SyncGroupRoadmapEnrollment {

		@Test
		void shouldEnrollEveryMemberWhenNoGradeFilterTest() {
			// Given:
			when(groupMemberEntityService.findByGroupId(1L)).thenReturn(List.of(member(1L, 10L), member(1L, 11L)));
			UserRoadmap row1 = new UserRoadmap();
			UserRoadmap row2 = new UserRoadmap();
			when(learningEnrollmentService.enrollUsersInRoadmap(List.of(10L, 11L), 100L)).thenReturn(List.of(row1,
					row2));
			when(learningEnrollmentSupport.toRoadmapAssignmentEnrollment(row1, 10L))
					.thenReturn(new RoadmapAssignmentEnrollmentRecord(10L, 100L, LocalDateTime.now()));
			when(learningEnrollmentSupport.toRoadmapAssignmentEnrollment(row2, 11L))
					.thenReturn(new RoadmapAssignmentEnrollmentRecord(11L, 100L, LocalDateTime.now()));

			// When:
			List<RoadmapAssignmentEnrollmentRecord> result = service.syncGroupRoadmapEnrollment(1L, 100L, Set.of());

			// Then:
			assertThat(result).hasSize(2);
			verify(groupMemberEntityService, never()).findByGroupIdAndMemberGradeIdIn(anyLong(), eq(Set.of()));
		}

		@Test
		void shouldEnrollOnlyMembersMatchingGradeFilterTest() {
			// Given:
			when(groupMemberEntityService.findByGroupIdAndMemberGradeIdIn(1L, Set.of(5L)))
					.thenReturn(List.of(member(1L, 10L)));
			UserRoadmap row = new UserRoadmap();
			when(learningEnrollmentService.enrollUsersInRoadmap(List.of(10L), 100L)).thenReturn(List.of(row));
			when(learningEnrollmentSupport.toRoadmapAssignmentEnrollment(row, 10L))
					.thenReturn(new RoadmapAssignmentEnrollmentRecord(10L, 100L, LocalDateTime.now()));

			// When:
			List<RoadmapAssignmentEnrollmentRecord> result = service.syncGroupRoadmapEnrollment(1L, 100L, Set.of(5L));

			// Then:
			assertThat(result).hasSize(1);
			verify(groupMemberEntityService, never()).findByGroupId(1L);
		}
	}

	@Nested
	class SyncNewGroupMember {

		@Test
		void shouldEnrollNewMemberIntoUnfilteredAssignmentTest() {
			// Given:
			RoadmapGroupAssignment assignment = assignment(1L, 100L, 1L);
			when(roadmapGroupAssignmentEntityService.findByGroupIdIn(List.of(1L))).thenReturn(List.of(assignment));
			when(roadmapGroupAssignmentEntityService.findGradesByAssignmentId(1L)).thenReturn(List.of());
			when(userEntityService.findById(10L)).thenReturn(Optional.of(userWithGrade(10L, null)));
			UserRoadmap row = new UserRoadmap();
			when(learningEnrollmentService.enrollUsersInRoadmap(List.of(10L), 100L)).thenReturn(List.of(row));
			when(learningEnrollmentSupport.toRoadmapAssignmentEnrollment(row, 10L))
					.thenReturn(new RoadmapAssignmentEnrollmentRecord(10L, 100L, LocalDateTime.now()));

			// When:
			service.syncNewGroupMember(1L, 10L);

			// Then:
			verify(learningEnrollmentService).enrollUsersInRoadmap(List.of(10L), 100L);
		}

		@Test
		void shouldEnrollNewMemberWhenGradeMatchesFilterTest() {
			// Given:
			RoadmapGroupAssignment assignment = assignment(1L, 100L, 1L);
			when(roadmapGroupAssignmentEntityService.findByGroupIdIn(List.of(1L))).thenReturn(List.of(assignment));
			when(roadmapGroupAssignmentEntityService.findGradesByAssignmentId(1L)).thenReturn(List.of(gradeFilterRow(5L)));
			when(userEntityService.findById(10L)).thenReturn(Optional.of(userWithGrade(10L, 5L)));
			UserRoadmap row = new UserRoadmap();
			when(learningEnrollmentService.enrollUsersInRoadmap(List.of(10L), 100L)).thenReturn(List.of(row));
			when(learningEnrollmentSupport.toRoadmapAssignmentEnrollment(row, 10L))
					.thenReturn(new RoadmapAssignmentEnrollmentRecord(10L, 100L, LocalDateTime.now()));

			// When:
			service.syncNewGroupMember(1L, 10L);

			// Then:
			verify(learningEnrollmentService).enrollUsersInRoadmap(List.of(10L), 100L);
		}

		@Test
		void shouldSkipNewMemberWhenGradeDoesNotMatchFilterTest() {
			// Given:
			RoadmapGroupAssignment assignment = assignment(1L, 100L, 1L);
			when(roadmapGroupAssignmentEntityService.findByGroupIdIn(List.of(1L))).thenReturn(List.of(assignment));
			when(roadmapGroupAssignmentEntityService.findGradesByAssignmentId(1L)).thenReturn(List.of(gradeFilterRow(5L)));
			when(userEntityService.findById(10L)).thenReturn(Optional.of(userWithGrade(10L, 6L)));

			// When:
			service.syncNewGroupMember(1L, 10L);

			// Then:
			verify(learningEnrollmentService, never()).enrollUsersInRoadmap(List.of(10L), 100L);
		}

		@Test
		void shouldSkipNewMemberWithNoGradeWhenFilterIsSetTest() {
			// Given:
			RoadmapGroupAssignment assignment = assignment(1L, 100L, 1L);
			when(roadmapGroupAssignmentEntityService.findByGroupIdIn(List.of(1L))).thenReturn(List.of(assignment));
			when(roadmapGroupAssignmentEntityService.findGradesByAssignmentId(1L)).thenReturn(List.of(gradeFilterRow(5L)));
			when(userEntityService.findById(10L)).thenReturn(Optional.of(userWithGrade(10L, null)));

			// When:
			service.syncNewGroupMember(1L, 10L);

			// Then:
			verify(learningEnrollmentService, never()).enrollUsersInRoadmap(List.of(10L), 100L);
		}

		@Test
		void shouldDoNothingWhenGroupHasNoStandingAssignmentsTest() {
			// Given:
			when(roadmapGroupAssignmentEntityService.findByGroupIdIn(List.of(1L))).thenReturn(List.of());

			// When:
			service.syncNewGroupMember(1L, 10L);

			// Then:
			verify(userEntityService, never()).findById(10L);
		}
	}

	@Nested
	class SyncUserGradeChange {

		@Test
		void shouldEnrollWhenNewGradeMatchesAssignmentFilterTest() {
			// Given:
			when(groupMemberEntityService.findGroupIdsByMemberUserId(10L)).thenReturn(Set.of(1L));
			RoadmapGroupAssignment assignment = assignment(1L, 100L, 1L);
			when(roadmapGroupAssignmentEntityService.findByGroupIdIn(Set.of(1L))).thenReturn(List.of(assignment));
			when(roadmapGroupAssignmentEntityService.findGradesByAssignmentId(1L)).thenReturn(List.of(gradeFilterRow(5L)));
			UserRoadmap row = new UserRoadmap();
			when(learningEnrollmentService.enrollUsersInRoadmap(List.of(10L), 100L)).thenReturn(List.of(row));
			when(learningEnrollmentSupport.toRoadmapAssignmentEnrollment(row, 10L))
					.thenReturn(new RoadmapAssignmentEnrollmentRecord(10L, 100L, LocalDateTime.now()));

			// When:
			service.syncUserGradeChange(10L, 5L);

			// Then:
			verify(learningEnrollmentService).enrollUsersInRoadmap(List.of(10L), 100L);
		}

		@Test
		void shouldEnrollForUnfilteredAssignmentsRegardlessOfNewGradeTest() {
			// Given:
			when(groupMemberEntityService.findGroupIdsByMemberUserId(10L)).thenReturn(Set.of(1L));
			RoadmapGroupAssignment assignment = assignment(1L, 100L, 1L);
			when(roadmapGroupAssignmentEntityService.findByGroupIdIn(Set.of(1L))).thenReturn(List.of(assignment));
			when(roadmapGroupAssignmentEntityService.findGradesByAssignmentId(1L)).thenReturn(List.of());
			UserRoadmap row = new UserRoadmap();
			when(learningEnrollmentService.enrollUsersInRoadmap(List.of(10L), 100L)).thenReturn(List.of(row));
			when(learningEnrollmentSupport.toRoadmapAssignmentEnrollment(row, 10L))
					.thenReturn(new RoadmapAssignmentEnrollmentRecord(10L, 100L, LocalDateTime.now()));

			// When:
			service.syncUserGradeChange(10L, null);

			// Then:
			verify(learningEnrollmentService).enrollUsersInRoadmap(List.of(10L), 100L);
		}

		@Test
		void shouldNotEnrollWhenNewGradeDoesNotMatchFilterTest() {
			// Given:
			when(groupMemberEntityService.findGroupIdsByMemberUserId(10L)).thenReturn(Set.of(1L));
			RoadmapGroupAssignment assignment = assignment(1L, 100L, 1L);
			when(roadmapGroupAssignmentEntityService.findByGroupIdIn(Set.of(1L))).thenReturn(List.of(assignment));
			when(roadmapGroupAssignmentEntityService.findGradesByAssignmentId(1L)).thenReturn(List.of(gradeFilterRow(5L)));

			// When:
			service.syncUserGradeChange(10L, 6L);

			// Then:
			verify(learningEnrollmentService, never()).enrollUsersInRoadmap(List.of(10L), 100L);
		}

		@Test
		void shouldDoNothingWhenUserBelongsToNoGroupsTest() {
			// Given:
			when(groupMemberEntityService.findGroupIdsByMemberUserId(10L)).thenReturn(Set.of());

			// When:
			service.syncUserGradeChange(10L, 5L);

			// Then:
			verify(roadmapGroupAssignmentEntityService, never()).findByGroupIdIn(Set.of());
		}
	}
}
