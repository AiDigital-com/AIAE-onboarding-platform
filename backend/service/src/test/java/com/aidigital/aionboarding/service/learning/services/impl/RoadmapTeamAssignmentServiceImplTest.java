package com.aidigital.aionboarding.service.learning.services.impl;

import com.aidigital.aionboarding.domain.roadmap.entities.Roadmap;
import com.aidigital.aionboarding.domain.roadmap.entities.RoadmapTeamAssignment;
import com.aidigital.aionboarding.domain.user.entities.User;
import com.aidigital.aionboarding.service.common.error.AppException;
import com.aidigital.aionboarding.service.common.security.AppUser;
import com.aidigital.aionboarding.service.learning.models.RoadmapAssignmentEnrollmentRecord;
import com.aidigital.aionboarding.service.learning.models.RoadmapTeamAssignmentRecord;
import com.aidigital.aionboarding.service.learning.models.RoadmapTeamAssignmentResultRecord;
import com.aidigital.aionboarding.service.learning.services.RoadmapEnrollmentService;
import com.aidigital.aionboarding.service.learning.support.LearningAssignmentAccessPolicy;
import com.aidigital.aionboarding.service.learning.support.LearningEnrollmentSupport;
import com.aidigital.aionboarding.service.learning.support.RoadmapTeamAssignmentWorkflow;
import com.aidigital.aionboarding.service.permission.services.PermissionService;
import com.aidigital.aionboarding.service.roadmap.services.entity.RoadmapEntityService;
import com.aidigital.aionboarding.service.roadmap.services.entity.RoadmapTeamAssignmentEntityService;
import org.instancio.Instancio;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoadmapTeamAssignmentServiceImplTest {

	@Mock
	private PermissionService permissionService;
	@Mock
	private LearningAssignmentAccessPolicy learningAssignmentAccessPolicy;
	@Mock
	private RoadmapEntityService roadmapEntityService;
	@Mock
	private RoadmapTeamAssignmentEntityService roadmapTeamAssignmentEntityService;
	@Mock
	private RoadmapTeamAssignmentWorkflow roadmapTeamAssignmentWorkflow;
	@Mock
	private LearningEnrollmentSupport learningEnrollmentSupport;
	@Mock
	private RoadmapEnrollmentService roadmapEnrollmentService;

	@InjectMocks
	private RoadmapTeamAssignmentServiceImpl service;

	private AppUser adminActor() {
		return new AppUser(1L, "clerk-admin", "admin@test.com", "Admin", "admin", "Admin", null, null, null);
	}

	@Nested
	class AssignRoadmapToGroup {

		@Test
		void assignRoadmapToGroupShouldCreateAssignmentWhenNoneExistsTest() {
			// Given:
			AppUser actor = adminActor();
			Long roadmapId = 10L;
			Long leadUserId = 20L;
			Roadmap roadmap = Instancio.create(Roadmap.class);
			RoadmapTeamAssignment createdAssignment = Instancio.create(RoadmapTeamAssignment.class);
			RoadmapAssignmentEnrollmentRecord enrollmentRecord = Instancio.create(RoadmapAssignmentEnrollmentRecord.class);
			RoadmapTeamAssignmentRecord assignmentRecord = Instancio.create(RoadmapTeamAssignmentRecord.class);

			when(roadmapEntityService.getReference(roadmapId)).thenReturn(roadmap);
			when(roadmapTeamAssignmentEntityService.findByRoadmapIdAndLeadUserId(roadmapId, leadUserId))
					.thenReturn(Optional.empty());
			when(roadmapTeamAssignmentWorkflow.createRoadmapTeamAssignment(actor, roadmap, leadUserId))
					.thenReturn(createdAssignment);
			when(roadmapTeamAssignmentWorkflow.syncGroupRoadmapEnrollment(leadUserId, roadmapId))
					.thenReturn(List.of(enrollmentRecord));
			when(learningEnrollmentSupport.toRoadmapTeamAssignment(createdAssignment)).thenReturn(assignmentRecord);

			// When:
			RoadmapTeamAssignmentResultRecord result = service.assignRoadmapToGroup(actor, roadmapId, leadUserId);

			// Then:
			assertThat(result.ok()).isTrue();
			assertThat(result.assignment()).isSameAs(assignmentRecord);
			assertThat(result.enrollments()).containsExactly(enrollmentRecord);
			verify(learningAssignmentAccessPolicy).requireManageableTeam(actor, leadUserId, "You can assign roadmaps only to your own team.");
			verify(roadmapTeamAssignmentWorkflow).createRoadmapTeamAssignment(actor, roadmap, leadUserId);
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

			when(roadmapEntityService.getReference(roadmapId)).thenReturn(roadmap);
			when(roadmapTeamAssignmentEntityService.findByRoadmapIdAndLeadUserId(roadmapId, leadUserId))
					.thenReturn(Optional.of(existingAssignment));
			when(roadmapTeamAssignmentWorkflow.syncGroupRoadmapEnrollment(leadUserId, roadmapId)).thenReturn(List.of());
			when(learningEnrollmentSupport.toRoadmapTeamAssignment(existingAssignment)).thenReturn(assignmentRecord);

			// When:
			RoadmapTeamAssignmentResultRecord result = service.assignRoadmapToGroup(actor, roadmapId, leadUserId);

			// Then:
			assertThat(result.assignment()).isSameAs(assignmentRecord);
			verify(roadmapTeamAssignmentWorkflow, never()).createRoadmapTeamAssignment(any(), any(), any());
		}

		@Test
		void assignRoadmapToGroupShouldThrowWhenActorCannotManageTeamTest() {
			// Given:
			AppUser actor = adminActor();
			doThrow(new AppException(com.aidigital.aionboarding.service.common.error.ErrorReason.C004, "forbidden"))
					.when(learningAssignmentAccessPolicy)
					.requireManageableTeam(actor, 20L, "You can assign roadmaps only to your own team.");

			// When-Then:
			assertThatThrownBy(() -> service.assignRoadmapToGroup(actor, 10L, 20L))
					.isInstanceOf(AppException.class);
			verifyNoInteractions(roadmapTeamAssignmentEntityService);
		}
	}

	@Nested
	class UnassignRoadmapFromGroup {

		@Test
		void unassignRoadmapFromGroupShouldDeleteAssignmentTest() {
			// Given:
			AppUser actor = adminActor();
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
			doThrow(new AppException(com.aidigital.aionboarding.service.common.error.ErrorReason.C004, "forbidden"))
					.when(learningAssignmentAccessPolicy)
					.requireManageableTeam(actor, 20L, "You can unassign roadmaps only from your own team.");

			// When-Then:
			assertThatThrownBy(() -> service.unassignRoadmapFromGroup(actor, 10L, 20L))
					.isInstanceOf(AppException.class);
			verify(roadmapTeamAssignmentEntityService, never()).deleteByRoadmapIdAndLeadUserId(any(), any());
		}

		@Test
		void unassignRoadmapFromGroupShouldCheckRevokePermissionWhenAssignmentExistsTest() {
			// Given:
			AppUser actor = adminActor();
			RoadmapTeamAssignment assignment = Instancio.create(RoadmapTeamAssignment.class);
			when(roadmapTeamAssignmentEntityService.findByRoadmapIdAndLeadUserId(10L, 20L))
					.thenReturn(Optional.of(assignment));

			// When:
			service.unassignRoadmapFromGroup(actor, 10L, 20L);

			// Then:
			verify(learningAssignmentAccessPolicy).requireCanRevokeTeamAssignment(actor, assignment);
			verify(roadmapTeamAssignmentEntityService).deleteByRoadmapIdAndLeadUserId(10L, 20L);
		}
	}

	@Nested
	class GetRoadmapTeamAssignments {

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
	class SyncNewTeamMemberEnrollments {

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
			verify(roadmapEnrollmentService).enrollUsersInRoadmap(List.of(memberUserId), firstRoadmap.getId());
			verify(roadmapEnrollmentService).enrollUsersInRoadmap(List.of(memberUserId), secondRoadmap.getId());
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
			verifyNoInteractions(roadmapEnrollmentService);
		}
	}
}
