package com.aidigital.aionboarding.service.user.services.impl;

import com.aidigital.aionboarding.service.roadmap.services.RoadmapGroupAssignmentSyncService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class UserGradeAssignmentSyncServiceImplTest {

	@Mock
	private RoadmapGroupAssignmentSyncService roadmapGroupAssignmentSyncService;

	@InjectMocks
	private UserGradeAssignmentSyncServiceImpl service;

	@Test
	void shouldDelegateToRoadmapGroupAssignmentSyncServiceTest() {
		// Given:
		Long userId = 10L;
		Long newGradeId = 5L;

		// When:
		service.onGradeChanged(userId, newGradeId);

		// Then:
		verify(roadmapGroupAssignmentSyncService).syncUserGradeChange(eq(userId), eq(newGradeId));
		verifyNoMoreInteractions(roadmapGroupAssignmentSyncService);
	}

	@Test
	void shouldPassThroughExactUserIdAndGradeIdArgumentsTest() {
		// Given:
		Long userId = 42L;
		Long newGradeId = 7L;
		ArgumentCaptor<Long> userIdCaptor = ArgumentCaptor.forClass(Long.class);
		ArgumentCaptor<Long> gradeIdCaptor = ArgumentCaptor.forClass(Long.class);

		// When:
		service.onGradeChanged(userId, newGradeId);

		// Then:
		verify(roadmapGroupAssignmentSyncService).syncUserGradeChange(userIdCaptor.capture(), gradeIdCaptor.capture());
		assertThat(userIdCaptor.getValue()).isEqualTo(42L);
		assertThat(gradeIdCaptor.getValue()).isEqualTo(7L);
	}

	@Test
	void shouldDelegateWithNullGradeIdWhenGradeIsClearedTest() {
		// Given:
		Long userId = 10L;

		// When:
		service.onGradeChanged(userId, null);

		// Then:
		verify(roadmapGroupAssignmentSyncService).syncUserGradeChange(eq(userId), isNull());
		verifyNoMoreInteractions(roadmapGroupAssignmentSyncService);
	}
}
