package com.aidigital.aionboarding.service.roadmap.services.impl;

import com.aidigital.aionboarding.domain.grade.entities.Grade;
import com.aidigital.aionboarding.domain.group.entities.Group;
import com.aidigital.aionboarding.domain.roadmap.entities.Roadmap;
import com.aidigital.aionboarding.domain.roadmap.entities.RoadmapGroupAssignment;
import com.aidigital.aionboarding.service.common.error.AppException;
import com.aidigital.aionboarding.service.common.security.AppUser;
import com.aidigital.aionboarding.service.common.time.CurrentTime;
import com.aidigital.aionboarding.service.grade.services.entity.GradeEntityService;
import com.aidigital.aionboarding.service.group.services.entity.GroupEntityService;
import com.aidigital.aionboarding.service.group.services.entity.GroupLeadEntityService;
import com.aidigital.aionboarding.service.group.services.entity.GroupMemberEntityService;
import com.aidigital.aionboarding.service.group.support.GroupAccessPolicy;
import com.aidigital.aionboarding.service.learning.models.RoadmapAssignmentEnrollmentRecord;
import com.aidigital.aionboarding.service.mappers.user.UserRecordMapper;
import com.aidigital.aionboarding.service.permission.services.PermissionService;
import com.aidigital.aionboarding.service.roadmap.models.RoadmapGroupAssignmentPreviewRecord;
import com.aidigital.aionboarding.service.roadmap.models.RoadmapGroupAssignmentRecord;
import com.aidigital.aionboarding.service.roadmap.models.RoadmapGroupAssignmentResultRecord;
import com.aidigital.aionboarding.service.roadmap.services.RoadmapGroupAssignmentSyncService;
import com.aidigital.aionboarding.service.roadmap.services.entity.RoadmapEntityService;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoadmapGroupAssignmentServiceImplTest {

	@Mock
	private RoadmapGroupAssignmentEntityService roadmapGroupAssignmentEntityService;
	@Mock
	private RoadmapGroupAssignmentSyncService roadmapGroupAssignmentSyncService;
	@Mock
	private RoadmapEntityService roadmapEntityService;
	@Mock
	private GroupEntityService groupEntityService;
	@Mock
	private GroupLeadEntityService groupLeadEntityService;
	@Mock
	private GroupMemberEntityService groupMemberEntityService;
	@Mock
	private GradeEntityService gradeEntityService;
	@Mock
	private GroupAccessPolicy groupAccessPolicy;
	@Mock
	private PermissionService permissionService;
	@Mock
	private UserRecordMapper userMapper;
	@Mock
	private UserEntityService userEntityService;
	@Mock
	private CurrentTime currentTime;

	@InjectMocks
	private RoadmapGroupAssignmentServiceImpl service;

	@Nested
	class AssignRoadmapToGroup {

		@Test
		void shouldRejectWhenViewerCannotManageGroupTest() {
			// Given:
			AppUser lead = new AppUser(2L, "clerk-lead", "lead@test.com", "Lead", "teamlead", "Lead", null, null,
					null);
			when(groupAccessPolicy.canManageGroup(lead, 20L)).thenReturn(false);

			// When-Then:
			assertThatThrownBy(() -> service.assignRoadmapToGroup(lead, 10L, 20L, List.of()))
					.isInstanceOf(AppException.class);
			verify(roadmapGroupAssignmentEntityService, never()).save(any());
		}

		@Test
		void shouldRejectWhenGradeIdDoesNotExistTest() {
			// Given:
			AppUser admin = new AppUser(1L, "clerk-admin", "admin@test.com", "Admin", "admin", "Admin", null, null,
					null);
			when(groupAccessPolicy.canManageGroup(admin, 20L)).thenReturn(true);
			Roadmap roadmap = new Roadmap();
			roadmap.setId(10L);
			when(roadmapEntityService.getReference(10L)).thenReturn(roadmap);
			Group group = new Group();
			group.setId(20L);
			when(groupEntityService.findById(20L)).thenReturn(Optional.of(group));
			when(gradeEntityService.findById(99L)).thenReturn(Optional.empty());

			// When-Then:
			assertThatThrownBy(() -> service.assignRoadmapToGroup(admin, 10L, 20L, List.of(99L)))
					.isInstanceOf(AppException.class);
			verify(roadmapGroupAssignmentEntityService, never()).save(any());
		}

		@Test
		void shouldCreateAssignmentAndSyncEnrollmentWhenViewerCanManageGroupTest() {
			// Given:
			AppUser admin = new AppUser(1L, "clerk-admin", "admin@test.com", "Admin", "admin", "Admin", null, null,
					null);
			when(groupAccessPolicy.canManageGroup(admin, 20L)).thenReturn(true);
			Roadmap roadmap = new Roadmap();
			roadmap.setId(10L);
			when(roadmapEntityService.getReference(10L)).thenReturn(roadmap);
			Group group = new Group();
			group.setId(20L);
			group.setName("CS Campaign");
			when(groupEntityService.findById(20L)).thenReturn(Optional.of(group));
			when(roadmapGroupAssignmentEntityService.findByRoadmapIdAndGroupId(10L, 20L)).thenReturn(Optional.empty());
			when(currentTime.utcDateTime()).thenReturn(LocalDateTime.of(2026, 1, 1, 0, 0));
			when(userEntityService.getReference(1L)).thenReturn(null);
			when(roadmapGroupAssignmentEntityService.save(any(RoadmapGroupAssignment.class)))
					.thenAnswer(invocation -> {
						RoadmapGroupAssignment assignment = invocation.getArgument(0);
						assignment.setId(500L);
						return assignment;
					});
			when(groupLeadEntityService.findByGroupIdIn(List.of(20L))).thenReturn(List.of());
			when(groupMemberEntityService.countByGroupId(20L)).thenReturn(3L);
			when(groupMemberEntityService.countMembersWithoutGrade(20L)).thenReturn(0L);
			when(roadmapGroupAssignmentSyncService.syncGroupRoadmapEnrollment(20L, 10L, java.util.Set.of()))
					.thenReturn(List.of(new RoadmapAssignmentEnrollmentRecord(30L, 10L, LocalDateTime.now())));

			// When:
			RoadmapGroupAssignmentResultRecord result = service.assignRoadmapToGroup(admin, 10L, 20L, List.of());

			// Then:
			assertThat(result.ok()).isTrue();
			assertThat(result.assignment().groupName()).isEqualTo("CS Campaign");
			assertThat(result.enrollments()).hasSize(1);
			verify(roadmapGroupAssignmentEntityService).replaceGrades(500L, List.of());
		}
	}

	@Nested
	class UnassignRoadmapFromGroup {

		@Test
		void shouldRejectWhenViewerCannotManageGroupTest() {
			// Given:
			AppUser lead = new AppUser(2L, "clerk-lead", "lead@test.com", "Lead", "teamlead", "Lead", null, null,
					null);
			when(groupAccessPolicy.canManageGroup(lead, 20L)).thenReturn(false);

			// When-Then:
			assertThatThrownBy(() -> service.unassignRoadmapFromGroup(lead, 10L, 20L))
					.isInstanceOf(AppException.class);
			verify(roadmapGroupAssignmentEntityService, never()).deleteByRoadmapIdAndGroupId(anyLong(), anyLong());
		}

		@Test
		void shouldDeleteAssignmentWhenViewerCanManageGroupTest() {
			// Given:
			AppUser admin = new AppUser(1L, "clerk-admin", "admin@test.com", "Admin", "admin", "Admin", null, null,
					null);
			when(groupAccessPolicy.canManageGroup(admin, 20L)).thenReturn(true);

			// When:
			service.unassignRoadmapFromGroup(admin, 10L, 20L);

			// Then:
			verify(roadmapGroupAssignmentEntityService).deleteByRoadmapIdAndGroupId(10L, 20L);
		}
	}

	@Nested
	class PreviewAssignment {

		@Test
		void shouldReturnCountsForGradeFilterTest() {
			// Given:
			AppUser admin = new AppUser(1L, "clerk-admin", "admin@test.com", "Admin", "admin", "Admin", null, null,
					null);
			when(groupAccessPolicy.canManageGroup(admin, 20L)).thenReturn(true);
			Grade grade = new Grade();
			grade.setId(5L);
			when(gradeEntityService.findById(5L)).thenReturn(Optional.of(grade));
			when(groupMemberEntityService.countByGroupId(20L)).thenReturn(10L);
			when(groupMemberEntityService.countMembersWithoutGrade(20L)).thenReturn(2L);
			when(groupMemberEntityService.findByGroupIdAndMemberGradeIdIn(20L, java.util.Set.of(5L)))
					.thenReturn(List.of(new com.aidigital.aionboarding.domain.group.entities.GroupMember()));

			// When:
			RoadmapGroupAssignmentPreviewRecord result = service.previewAssignment(admin, 20L, List.of(5L));

			// Then:
			assertThat(result.groupMembersCount()).isEqualTo(10L);
			assertThat(result.membersMatchedCount()).isEqualTo(1L);
			assertThat(result.membersWithoutGradeCount()).isEqualTo(2L);
		}
	}

	@Nested
	class ListAssignmentsForGroup {

		@Test
		void shouldRejectWhenViewerCannotManageGroupTest() {
			// Given:
			AppUser lead = new AppUser(2L, "clerk-lead", "lead@test.com", "Lead", "teamlead", "Lead", null, null,
					null);
			when(groupAccessPolicy.canManageGroup(lead, 20L)).thenReturn(false);

			// When-Then:
			assertThatThrownBy(() -> service.listAssignmentsForGroup(lead, 20L))
					.isInstanceOf(AppException.class);
			verify(roadmapGroupAssignmentEntityService, never()).findByGroupId(any());
		}

		@Test
		void shouldReturnAssignmentsWithRoadmapTitleTest() {
			// Given:
			AppUser admin = new AppUser(1L, "clerk-admin", "admin@test.com", "Admin", "admin", "Admin", null, null,
					null);
			when(groupAccessPolicy.canManageGroup(admin, 20L)).thenReturn(true);

			Roadmap roadmap = new Roadmap();
			roadmap.setId(10L);
			roadmap.setTitle("Onboarding basics");

			Group group = new Group();
			group.setId(20L);
			group.setName("CS Campaign");

			RoadmapGroupAssignment assignment = new RoadmapGroupAssignment();
			assignment.setId(99L);
			assignment.setRoadmap(roadmap);
			assignment.setGroup(group);
			assignment.setCreatedAt(LocalDateTime.of(2026, 1, 1, 0, 0));

			when(roadmapGroupAssignmentEntityService.findByGroupId(20L)).thenReturn(List.of(assignment));
			when(roadmapGroupAssignmentEntityService.findGradesByAssignmentId(99L)).thenReturn(List.of());
			when(groupLeadEntityService.findByGroupIdIn(List.of(20L))).thenReturn(List.of());
			when(groupMemberEntityService.countByGroupId(20L)).thenReturn(3L);
			when(groupMemberEntityService.countMembersWithoutGrade(20L)).thenReturn(1L);

			// When:
			List<RoadmapGroupAssignmentRecord> result = service.listAssignmentsForGroup(admin, 20L);

			// Then:
			assertThat(result).hasSize(1);
			assertThat(result.get(0).roadmapId()).isEqualTo(10L);
			assertThat(result.get(0).roadmapTitle()).isEqualTo("Onboarding basics");
			assertThat(result.get(0).groupId()).isEqualTo(20L);
		}
	}
}
