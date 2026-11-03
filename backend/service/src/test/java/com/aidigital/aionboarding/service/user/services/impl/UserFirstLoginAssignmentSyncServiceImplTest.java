package com.aidigital.aionboarding.service.user.services.impl;

import com.aidigital.aionboarding.service.roadmap.services.RoadmapAutoAssignmentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserFirstLoginAssignmentSyncServiceImplTest {

	@Mock
	private RoadmapAutoAssignmentService roadmapAutoAssignmentService;

	@InjectMocks
	private UserFirstLoginAssignmentSyncServiceImpl service;

	@Test
	void shouldDelegateANewUserToTheRoadmapAutoAssignmentRuleTest() {
		// When:
		service.onUserCreated(42L);

		// Then:
		verify(roadmapAutoAssignmentService).enrollNewUser(42L);
	}
}
