package com.aidigital.aionboarding.service.roadmap.services.entity;

import com.aidigital.aionboarding.domain.roadmap.entities.RoadmapTeamAssignment;
import com.aidigital.aionboarding.domain.roadmap.repositories.RoadmapTeamAssignmentRepository;
import org.instancio.Instancio;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoadmapTeamAssignmentEntityServiceTest {

	@Mock
	private RoadmapTeamAssignmentRepository roadmapTeamAssignmentRepository;

	@InjectMocks
	private RoadmapTeamAssignmentEntityService service;

	@Test
	void findByRoadmapIdShouldDelegateToRepositoryTest() {
		// Given:
		Long roadmapId = 10L;
		List<RoadmapTeamAssignment> assignments = List.of(Instancio.create(RoadmapTeamAssignment.class));
		when(roadmapTeamAssignmentRepository.findByRoadmapId(roadmapId)).thenReturn(assignments);

		// When:
		List<RoadmapTeamAssignment> result = service.findByRoadmapId(roadmapId);

		// Then:
		assertThat(result).isSameAs(assignments);
	}

	@Test
	void findByLeadUserIdShouldDelegateToRepositoryTest() {
		// Given:
		Long leadUserId = 20L;
		List<RoadmapTeamAssignment> assignments = List.of(Instancio.create(RoadmapTeamAssignment.class));
		when(roadmapTeamAssignmentRepository.findByLeadUserId(leadUserId)).thenReturn(assignments);

		// When:
		List<RoadmapTeamAssignment> result = service.findByLeadUserId(leadUserId);

		// Then:
		assertThat(result).isSameAs(assignments);
	}

	@Test
	void findByRoadmapIdAndLeadUserIdShouldDelegateToRepositoryTest() {
		// Given:
		Long roadmapId = 10L;
		Long leadUserId = 20L;
		RoadmapTeamAssignment assignment = Instancio.create(RoadmapTeamAssignment.class);
		when(roadmapTeamAssignmentRepository.findByRoadmapIdAndLeadUserId(roadmapId, leadUserId))
				.thenReturn(Optional.of(assignment));

		// When:
		Optional<RoadmapTeamAssignment> result = service.findByRoadmapIdAndLeadUserId(roadmapId, leadUserId);

		// Then:
		assertThat(result).contains(assignment);
	}

	@Test
	void saveShouldDelegateToRepositoryTest() {
		// Given:
		RoadmapTeamAssignment assignment = Instancio.create(RoadmapTeamAssignment.class);
		when(roadmapTeamAssignmentRepository.save(assignment)).thenReturn(assignment);

		// When:
		RoadmapTeamAssignment result = service.save(assignment);

		// Then:
		assertThat(result).isSameAs(assignment);
	}

	@Test
	void deleteByRoadmapIdAndLeadUserIdShouldDelegateToRepositoryTest() {
		// Given:
		Long roadmapId = 10L;
		Long leadUserId = 20L;

		// When:
		service.deleteByRoadmapIdAndLeadUserId(roadmapId, leadUserId);

		// Then:
		verify(roadmapTeamAssignmentRepository).deleteByRoadmapIdAndLeadUserId(roadmapId, leadUserId);
	}
}
