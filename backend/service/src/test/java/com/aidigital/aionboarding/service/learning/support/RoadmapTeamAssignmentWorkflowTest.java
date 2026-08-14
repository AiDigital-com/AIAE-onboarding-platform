package com.aidigital.aionboarding.service.learning.support;

import com.aidigital.aionboarding.domain.learning.entities.UserRoadmap;
import com.aidigital.aionboarding.domain.roadmap.entities.Roadmap;
import com.aidigital.aionboarding.domain.roadmap.entities.RoadmapTeamAssignment;
import com.aidigital.aionboarding.domain.team.entities.TeamMember;
import com.aidigital.aionboarding.domain.user.entities.User;
import com.aidigital.aionboarding.service.common.security.AppUser;
import com.aidigital.aionboarding.service.common.time.CurrentTime;
import com.aidigital.aionboarding.service.common.time.CurrentTimeImpl;
import com.aidigital.aionboarding.service.learning.models.RoadmapAssignmentEnrollmentRecord;
import com.aidigital.aionboarding.service.learning.services.RoadmapEnrollmentService;
import com.aidigital.aionboarding.service.roadmap.services.entity.RoadmapTeamAssignmentEntityService;
import com.aidigital.aionboarding.service.team.services.entity.TeamEntityService;
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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoadmapTeamAssignmentWorkflowTest {

	@Mock
	private UserEntityService userEntityService;
	@Mock
	private TeamEntityService teamEntityService;
	@Mock
	private RoadmapTeamAssignmentEntityService roadmapTeamAssignmentEntityService;
	@Mock
	private RoadmapEnrollmentService roadmapEnrollmentService;
	@Mock
	private LearningEnrollmentSupport learningEnrollmentSupport;
	@Spy
	private CurrentTime currentTime = new CurrentTimeImpl();

	@InjectMocks
	private RoadmapTeamAssignmentWorkflow workflow;

	private AppUser adminActor() {
		return new AppUser(1L, "clerk-admin", "admin@test.com", "Admin", "admin", "Admin", null, null, null);
	}

	private TeamMember teamMember(Long leadUserId, Long memberUserId) {
		TeamMember member = Instancio.create(TeamMember.class);
		TeamMember.TeamMemberId id = new TeamMember.TeamMemberId();
		id.setLeadUserId(leadUserId);
		id.setMemberUserId(memberUserId);
		member.setId(id);
		return member;
	}

	@Nested
	class CreateRoadmapTeamAssignment {

		@Test
		void shouldPersistAssignmentWithRoadmapLeadAndAssignerTest() {
			// Given:
			AppUser actor = adminActor();
			Roadmap roadmap = Instancio.create(Roadmap.class);
			Long leadUserId = 20L;
			User leadUser = Instancio.create(User.class);
			User actorUser = Instancio.create(User.class);
			RoadmapTeamAssignment savedAssignment = Instancio.create(RoadmapTeamAssignment.class);

			when(userEntityService.getReference(leadUserId)).thenReturn(leadUser);
			when(userEntityService.getReference(actor.internalId())).thenReturn(actorUser);
			when(roadmapTeamAssignmentEntityService.save(any())).thenReturn(savedAssignment);

			// When:
			RoadmapTeamAssignment result = workflow.createRoadmapTeamAssignment(actor, roadmap, leadUserId);

			// Then:
			assertThat(result).isSameAs(savedAssignment);
			ArgumentCaptor<RoadmapTeamAssignment> captor = ArgumentCaptor.forClass(RoadmapTeamAssignment.class);
			org.mockito.Mockito.verify(roadmapTeamAssignmentEntityService).save(captor.capture());
			assertThat(captor.getValue().getRoadmap()).isSameAs(roadmap);
			assertThat(captor.getValue().getLeadUser()).isSameAs(leadUser);
			assertThat(captor.getValue().getAssignedByUser()).isSameAs(actorUser);
			assertThat(captor.getValue().getCreatedAt()).isNotNull();
			assertThat(captor.getValue().getUpdatedAt()).isNotNull();
		}
	}

	@Nested
	class SyncGroupRoadmapEnrollment {

		@Test
		void shouldEnrollLeadAndCurrentMembersTest() {
			// Given:
			Long leadUserId = 20L;
			Long roadmapId = 10L;
			TeamMember member = teamMember(leadUserId, 30L);
			UserRoadmap leadEnrollmentRow = Instancio.create(UserRoadmap.class);
			UserRoadmap memberEnrollmentRow = Instancio.create(UserRoadmap.class);
			RoadmapAssignmentEnrollmentRecord leadEnrollmentRecord = Instancio.create(RoadmapAssignmentEnrollmentRecord.class);
			RoadmapAssignmentEnrollmentRecord memberEnrollmentRecord = Instancio.create(RoadmapAssignmentEnrollmentRecord.class);

			when(teamEntityService.findByLeadUserIdWithMember(leadUserId)).thenReturn(List.of(member));
			when(roadmapEnrollmentService.enrollUsersInRoadmap(List.of(20L, 30L), roadmapId))
					.thenReturn(List.of(leadEnrollmentRow, memberEnrollmentRow));
			when(learningEnrollmentSupport.toRoadmapAssignmentEnrollment(leadEnrollmentRow, 20L))
					.thenReturn(leadEnrollmentRecord);
			when(learningEnrollmentSupport.toRoadmapAssignmentEnrollment(memberEnrollmentRow, 30L))
					.thenReturn(memberEnrollmentRecord);

			// When:
			List<RoadmapAssignmentEnrollmentRecord> result = workflow.syncGroupRoadmapEnrollment(leadUserId, roadmapId);

			// Then:
			assertThat(result).containsExactly(leadEnrollmentRecord, memberEnrollmentRecord);
		}

		@Test
		void shouldEnrollOnlyLeadWhenTeamHasNoOtherMembersTest() {
			// Given:
			Long leadUserId = 20L;
			Long roadmapId = 10L;
			UserRoadmap leadEnrollmentRow = Instancio.create(UserRoadmap.class);
			RoadmapAssignmentEnrollmentRecord leadEnrollmentRecord = Instancio.create(RoadmapAssignmentEnrollmentRecord.class);

			when(teamEntityService.findByLeadUserIdWithMember(leadUserId)).thenReturn(List.of());
			when(roadmapEnrollmentService.enrollUsersInRoadmap(List.of(leadUserId), roadmapId))
					.thenReturn(List.of(leadEnrollmentRow));
			when(learningEnrollmentSupport.toRoadmapAssignmentEnrollment(leadEnrollmentRow, leadUserId))
					.thenReturn(leadEnrollmentRecord);

			// When:
			List<RoadmapAssignmentEnrollmentRecord> result = workflow.syncGroupRoadmapEnrollment(leadUserId, roadmapId);

			// Then:
			assertThat(result).containsExactly(leadEnrollmentRecord);
		}
	}
}
