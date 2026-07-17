package com.aidigital.aionboarding.controllers;

import com.aidigital.aionboarding.api.v1.model.CountResponseV1;
import com.aidigital.aionboarding.api.v1.model.SearchRoadmapsV1;
import com.aidigital.aionboarding.mappers.learning.LearningApiMapper;
import com.aidigital.aionboarding.mappers.roadmap.RoadmapApiMapper;
import com.aidigital.aionboarding.mappers.roadmap.RoadmapGroupAssignmentApiMapper;
import com.aidigital.aionboarding.service.common.security.AppUser;
import com.aidigital.aionboarding.service.learning.services.LearningService;
import com.aidigital.aionboarding.service.roadmap.models.RoadmapListQuery;
import com.aidigital.aionboarding.service.roadmap.services.RoadmapGroupAssignmentService;
import com.aidigital.aionboarding.service.roadmap.services.RoadmapService;
import com.aidigital.aionboarding.support.ApiResponses;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoadmapsControllerTest {

	@Mock
	private CurrentUserSupport currentUser;
	@Mock
	private RoadmapService roadmapService;
	@Mock
	private LearningService learningService;
	@Mock
	private RoadmapGroupAssignmentService roadmapGroupAssignmentService;
	@Mock
	private RoadmapApiMapper roadmapApiMapper;
	@Mock
	private LearningApiMapper learningApiMapper;
	@Mock
	private RoadmapGroupAssignmentApiMapper roadmapGroupAssignmentApiMapper;
	@Mock
	private ApiResponses apiResponses;

	@InjectMocks
	private RoadmapsController controller;

	@Test
	void countRoadmapsShouldDelegateToServiceAndWrapTheTotalTest() {
		// Given:
		AppUser viewer = new AppUser(1L, "clerk-1", "viewer@test.com", "Viewer", "member", "Viewer", null, null, null);
		SearchRoadmapsV1 request = mock(SearchRoadmapsV1.class);
		RoadmapListQuery query = mock(RoadmapListQuery.class);
		CountResponseV1 expectedBody = mock(CountResponseV1.class);
		when(currentUser.requireUser()).thenReturn(viewer);
		when(roadmapApiMapper.toRoadmapListQuery(request)).thenReturn(query);
		when(roadmapService.countRoadmaps(viewer, query)).thenReturn(3L);
		when(roadmapApiMapper.toCountResponseV1(3L)).thenReturn(expectedBody);

		// When:
		ResponseEntity<CountResponseV1> response = controller.countRoadmaps(request);

		// Then:
		assertThat(response.getBody()).isSameAs(expectedBody);
	}
}
