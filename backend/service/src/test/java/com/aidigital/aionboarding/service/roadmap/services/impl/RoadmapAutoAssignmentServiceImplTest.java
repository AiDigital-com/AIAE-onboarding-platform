package com.aidigital.aionboarding.service.roadmap.services.impl;

import com.aidigital.aionboarding.domain.roadmap.entities.Roadmap;
import com.aidigital.aionboarding.service.learning.services.RoadmapEnrollmentService;
import com.aidigital.aionboarding.service.roadmap.services.entity.RoadmapEntityService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoadmapAutoAssignmentServiceImplTest {

	@Mock
	private RoadmapEntityService roadmapEntityService;
	@Mock
	private RoadmapEnrollmentService roadmapEnrollmentService;

	@InjectMocks
	private RoadmapAutoAssignmentServiceImpl service;

	private Roadmap roadmap(Long id) {
		Roadmap roadmap = new Roadmap();
		roadmap.setId(id);
		roadmap.setAutoAssignNewUsers(true);
		return roadmap;
	}

	@Test
	void shouldEnrollTheNewUserInEveryAutoAssignedRoadmapAsAutoAssignedTest() {
		// Given:
		when(roadmapEntityService.findAutoAssignedToNewUsers()).thenReturn(List.of(roadmap(100L), roadmap(101L)));

		// When:
		service.enrollNewUser(7L);

		// Then: enrollments are flagged so the assignment UI can label them "New user"
		verify(roadmapEnrollmentService).enrollUsersInRoadmapAsAutoAssigned(List.of(7L), 100L);
		verify(roadmapEnrollmentService).enrollUsersInRoadmapAsAutoAssigned(List.of(7L), 101L);
	}

	@Test
	void shouldDoNothingWhenNoRoadmapIsAutoAssignedTest() {
		// Given:
		when(roadmapEntityService.findAutoAssignedToNewUsers()).thenReturn(List.of());

		// When:
		service.enrollNewUser(7L);

		// Then:
		verifyNoInteractions(roadmapEnrollmentService);
	}
}
