package com.aidigital.aionboarding.service.roadmap.services.impl;

import com.aidigital.aionboarding.domain.group.entities.Group;
import com.aidigital.aionboarding.domain.roadmap.entities.Roadmap;
import com.aidigital.aionboarding.domain.roadmap.entities.RoadmapGroupAssignment;
import com.aidigital.aionboarding.service.common.error.AppException;
import com.aidigital.aionboarding.service.common.security.AppUser;
import com.aidigital.aionboarding.service.group.services.entity.GroupEntityService;
import com.aidigital.aionboarding.service.group.support.GroupAccessPolicy;
import com.aidigital.aionboarding.service.learning.models.RoadmapAssignmentEnrollmentRecord;
import com.aidigital.aionboarding.service.permission.services.PermissionService;
import com.aidigital.aionboarding.service.roadmap.models.RoadmapGroupAssignmentPreviewRecord;
import com.aidigital.aionboarding.service.roadmap.models.RoadmapGroupAssignmentRecord;
import com.aidigital.aionboarding.service.roadmap.models.RoadmapGroupAssignmentResultRecord;
import com.aidigital.aionboarding.service.roadmap.services.RoadmapGroupAssignmentSyncService;
import com.aidigital.aionboarding.service.roadmap.services.entity.RoadmapEntityService;
import com.aidigital.aionboarding.service.roadmap.services.entity.RoadmapGroupAssignmentEntityService;
import com.aidigital.aionboarding.service.roadmap.support.RoadmapGroupAssignmentRecordAssembler;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
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
	private GroupAccessPolicy groupAccessPolicy;
	@Mock
	private PermissionService permissionService;
	@Mock
	private RoadmapGroupAssignmentRecordAssembler roadmapGroupAssignmentRecordAssembler;

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
			when(roadmapGroupAssignmentRecordAssembler.requireExistingGrades(List.of(99L)))
					.thenThrow(new AppException(com.aidigital.aionboarding.service.common.error.ErrorReason.C001,
							"Grade not found: 99"));

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
			when(groupEntityService.findById(20L)).thenReturn(Optional.of(group));
			when(roadmapGroupAssignmentEntityService.findByRoadmapIdAndGroupId(10L, 20L)).thenReturn(Optional.empty());
			RoadmapGroupAssignment createdAssignment = new RoadmapGroupAssignment();
			createdAssignment.setId(500L);
			when(roadmapGroupAssignmentRecordAssembler.createAssignment(admin, roadmap, group))
					.thenReturn(createdAssignment);
			// toGradeRows is left unstubbed: Mockito's default answer for a List-returning method
			// is an empty list, which is exactly what an empty grade filter should produce here.
			RoadmapGroupAssignmentRecord assignmentRecord = new RoadmapGroupAssignmentRecord(
					500L, 10L, "Onboarding", 20L, "CS Campaign", List.of(), List.of(), 3L, 0L, null, null,
					LocalDateTime.now());
			when(roadmapGroupAssignmentRecordAssembler.toRecord(createdAssignment, Map.of()))
					.thenReturn(assignmentRecord);
			when(roadmapGroupAssignmentSyncService.syncGroupRoadmapEnrollment(20L, 10L, java.util.Set.of()))
					.thenReturn(List.of(new RoadmapAssignmentEnrollmentRecord(30L, 10L, LocalDateTime.now())));

			// When:
			RoadmapGroupAssignmentResultRecord result = service.assignRoadmapToGroup(admin, 10L, 20L, List.of());

			// Then:
			assertThat(result.ok()).isTrue();
			assertThat(result.assignment().groupName()).isEqualTo("CS Campaign");
			assertThat(result.enrollments()).hasSize(1);
			verify(roadmapGroupAssignmentRecordAssembler).touchUpdatedAt(createdAssignment);
			verify(roadmapGroupAssignmentEntityService).save(createdAssignment);
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
		void shouldRejectWhenViewerCannotManageGroupTest() {
			// Given:
			AppUser lead = new AppUser(2L, "clerk-lead", "lead@test.com", "Lead", "teamlead", "Lead", null, null,
					null);
			when(groupAccessPolicy.canManageGroup(lead, 20L)).thenReturn(false);

			// When-Then:
			assertThatThrownBy(() -> service.previewAssignment(lead, 20L, List.of()))
					.isInstanceOf(AppException.class);
		}

		@Test
		void shouldDelegateToAssemblerTest() {
			// Given:
			AppUser admin = new AppUser(1L, "clerk-admin", "admin@test.com", "Admin", "admin", "Admin", null, null,
					null);
			when(groupAccessPolicy.canManageGroup(admin, 20L)).thenReturn(true);
			RoadmapGroupAssignmentPreviewRecord previewRecord = new RoadmapGroupAssignmentPreviewRecord(10L, 5L, 2L);
			when(roadmapGroupAssignmentRecordAssembler.previewAssignment(20L, List.of(5L))).thenReturn(previewRecord);

			// When:
			RoadmapGroupAssignmentPreviewRecord result = service.previewAssignment(admin, 20L, List.of(5L));

			// Then:
			assertThat(result).isSameAs(previewRecord);
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
		void shouldReturnAssembledRecordsTest() {
			// Given:
			AppUser admin = new AppUser(1L, "clerk-admin", "admin@test.com", "Admin", "admin", "Admin", null, null,
					null);
			when(groupAccessPolicy.canManageGroup(admin, 20L)).thenReturn(true);

			RoadmapGroupAssignment assignment = new RoadmapGroupAssignment();
			assignment.setId(99L);
			when(roadmapGroupAssignmentEntityService.findByGroupId(20L)).thenReturn(List.of(assignment));
			when(roadmapGroupAssignmentRecordAssembler.gradesOf(assignment)).thenReturn(Map.of());
			RoadmapGroupAssignmentRecord assignmentRecord = new RoadmapGroupAssignmentRecord(
					99L, 10L, "Onboarding basics", 20L, "CS Campaign", List.of(), List.of(), 3L, 1L, null, null,
					LocalDateTime.now());
			when(roadmapGroupAssignmentRecordAssembler.toRecord(assignment, Map.of())).thenReturn(assignmentRecord);

			// When:
			List<RoadmapGroupAssignmentRecord> result = service.listAssignmentsForGroup(admin, 20L);

			// Then:
			assertThat(result).containsExactly(assignmentRecord);
		}
	}
}
