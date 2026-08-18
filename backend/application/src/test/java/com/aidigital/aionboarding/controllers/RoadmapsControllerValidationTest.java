package com.aidigital.aionboarding.controllers;

import com.aidigital.aionboarding.error.mapper.GlobalExceptionResponseHelperImpl;
import com.aidigital.aionboarding.mappers.learning.LearningApiMapper;
import com.aidigital.aionboarding.mappers.roadmap.RoadmapApiMapper;
import com.aidigital.aionboarding.mappers.roadmap.RoadmapGroupAssignmentApiMapper;
import com.aidigital.aionboarding.service.common.security.RequestAuthenticationCache;
import com.aidigital.aionboarding.service.common.time.CurrentTimeImpl;
import com.aidigital.aionboarding.service.learning.services.RoadmapAssignmentService;
import com.aidigital.aionboarding.service.roadmap.services.RoadmapGroupAssignmentService;
import com.aidigital.aionboarding.service.roadmap.services.RoadmapService;
import com.aidigital.aionboarding.support.ApiResponses;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = RoadmapsController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({
		GlobalExceptionResponseHelperImpl.class,
		CurrentTimeImpl.class,
		RoadmapsControllerValidationTest.MeterRegistryTestConfig.class
})
class RoadmapsControllerValidationTest {

	@Autowired
	private MockMvc mvc;

	@MockitoBean
	private CurrentUserSupport currentUser;
	@MockitoBean
	private RoadmapService roadmapService;
	@MockitoBean
	private RoadmapAssignmentService roadmapAssignmentService;
	@MockitoBean
	private RoadmapGroupAssignmentService roadmapGroupAssignmentService;
	@MockitoBean
	private RoadmapApiMapper roadmapApiMapper;
	@MockitoBean
	private LearningApiMapper learningApiMapper;
	@MockitoBean
	private RoadmapGroupAssignmentApiMapper roadmapGroupAssignmentApiMapper;
	@MockitoBean
	private ApiResponses apiResponses;
	@MockitoBean
	private RequestAuthenticationCache requestAuthenticationCache;

	@TestConfiguration
	static class MeterRegistryTestConfig {

		@Bean
		MeterRegistry meterRegistry() {
			return new SimpleMeterRegistry();
		}
	}

	@Test
	void shouldRejectSearchRoadmapsWithOversizedPageSizeTest() throws Exception {
		// Given: size violates the generated @Max(100)
		String body = "{\"size\":101}";

		// When:
		var result = mvc.perform(post("/api/v1/roadmaps/search").contentType(APPLICATION_JSON).content(body));

		// Then:
		result.andExpect(status().isBadRequest());
	}

	@Test
	void shouldRejectCountRoadmapsWithOversizedPageSizeTest() throws Exception {
		// Given: size violates the generated @Max(100)
		String body = "{\"size\":101}";

		// When:
		var result = mvc.perform(post("/api/v1/roadmaps/count").contentType(APPLICATION_JSON).content(body));

		// Then:
		result.andExpect(status().isBadRequest());
	}

	@Test
	void shouldRejectCreateRoadmapWithBlankTitleTest() throws Exception {
		// Given: title violates the generated @NotNull @Size(min = 1) (RoadmapServiceImpl#createRoadmap)
		String body = "{\"title\":\"\",\"lessonIds\":[1]}";

		// When:
		var result = mvc.perform(post("/api/v1/roadmaps").contentType(APPLICATION_JSON).content(body));

		// Then:
		result.andExpect(status().isBadRequest());
	}

	@Test
	void shouldRejectCreateRoadmapWithMissingLessonIdsTest() throws Exception {
		// Given: lessonIds is required (@NotNull); the generated field defaults to an empty
		// list, so an explicit null is needed to violate @NotNull (an omitted key leaves the
		// default).
		String body = "{\"title\":\"Onboarding\",\"lessonIds\":null}";

		// When:
		var result = mvc.perform(post("/api/v1/roadmaps").contentType(APPLICATION_JSON).content(body));

		// Then:
		result.andExpect(status().isBadRequest());
	}

	@Test
	void shouldRejectUpdateRoadmapWithNonPositiveIdTest() throws Exception {
		// Given/When: id violates the generated @Min(1) on the path variable
		var result = mvc.perform(put("/api/v1/roadmaps/0").contentType(APPLICATION_JSON).content("{}"));

		// Then:
		result.andExpect(status().isBadRequest());
	}

	@Test
	void shouldRejectDeleteRoadmapWithNonPositiveIdTest() throws Exception {
		// Given/When: id violates the generated @Min(1) on the path variable
		var result = mvc.perform(delete("/api/v1/roadmaps/0"));

		// Then:
		result.andExpect(status().isBadRequest());
	}

	@Test
	void shouldRejectAssignRoadmapWithMissingUserIdsTest() throws Exception {
		// Given: userIds is required (@NotNull); the generated field defaults to an empty list,
		// so an explicit null is needed to violate @NotNull (an omitted key leaves the default).
		String body = "{\"userIds\":null}";

		// When:
		var result = mvc.perform(post("/api/v1/roadmaps/1/assignments").contentType(APPLICATION_JSON).content(body));

		// Then:
		result.andExpect(status().isBadRequest());
	}

	@Test
	void shouldRejectListRoadmapAssigneesWithNonPositiveIdTest() throws Exception {
		// Given/When: id violates the generated @Min(1) on the path variable
		var result = mvc.perform(get("/api/v1/roadmaps/0/assignees"));

		// Then:
		result.andExpect(status().isBadRequest());
	}

	@Test
	void shouldRejectRevokeRoadmapAssignmentsWithMissingUserIdsTest() throws Exception {
		// Given: userIds is required (@NotNull); the generated field defaults to an empty list,
		// so an explicit null is needed to violate @NotNull (an omitted key leaves the default).
		String body = "{\"userIds\":null}";

		// When:
		var result = mvc.perform(post("/api/v1/roadmaps/1/assignments/revoke")
				.contentType(APPLICATION_JSON).content(body));

		// Then:
		result.andExpect(status().isBadRequest());
	}

	@Test
	void shouldRejectEnrollRoadmapWithNonPositiveIdTest() throws Exception {
		// Given/When: id violates the generated @Min(1) on the path variable
		var result = mvc.perform(post("/api/v1/roadmaps/0/enrollment"));

		// Then:
		result.andExpect(status().isBadRequest());
	}

	@Test
	void shouldRejectUnenrollRoadmapWithNonPositiveIdTest() throws Exception {
		// Given/When: id violates the generated @Min(1) on the path variable
		var result = mvc.perform(delete("/api/v1/roadmaps/0/enrollment"));

		// Then:
		result.andExpect(status().isBadRequest());
	}

	@Test
	void shouldRejectListRoadmapTeamAssignmentsWithNonPositiveIdTest() throws Exception {
		// Given/When: id violates the generated @Min(1) on the path variable
		var result = mvc.perform(get("/api/v1/roadmaps/0/team-assignments"));

		// Then:
		result.andExpect(status().isBadRequest());
	}

	@Test
	void shouldRejectAssignRoadmapToTeamWithMissingLeadUserIdTest() throws Exception {
		// Given: leadUserId is required (@NotNull) and omitted
		String body = "{}";

		// When:
		var result = mvc.perform(post("/api/v1/roadmaps/1/team-assignments")
				.contentType(APPLICATION_JSON).content(body));

		// Then:
		result.andExpect(status().isBadRequest());
	}

	@Test
	void shouldRejectUnassignRoadmapFromTeamWithNonPositiveLeadUserIdTest() throws Exception {
		// Given/When: leadUserId violates the generated @Min(1) on the path variable
		var result = mvc.perform(delete("/api/v1/roadmaps/1/team-assignments/0"));

		// Then:
		result.andExpect(status().isBadRequest());
	}

	@Test
	void shouldRejectListRoadmapGroupAssignmentsWithNonPositiveIdTest() throws Exception {
		// Given/When: id violates the generated @Min(1) on the path variable
		var result = mvc.perform(get("/api/v1/roadmaps/0/group-assignments"));

		// Then:
		result.andExpect(status().isBadRequest());
	}

	@Test
	void shouldRejectAssignRoadmapToGroupWithMissingGroupIdTest() throws Exception {
		// Given: groupId is required (@NotNull) and omitted
		String body = "{}";

		// When:
		var result = mvc.perform(post("/api/v1/roadmaps/1/group-assignments")
				.contentType(APPLICATION_JSON).content(body));

		// Then:
		result.andExpect(status().isBadRequest());
	}

	@Test
	void shouldRejectPreviewRoadmapGroupAssignmentWithMissingGroupIdTest() throws Exception {
		// Given: groupId is required (@NotNull) and omitted
		String body = "{}";

		// When:
		var result = mvc.perform(post("/api/v1/roadmaps/1/group-assignments/preview")
				.contentType(APPLICATION_JSON).content(body));

		// Then:
		result.andExpect(status().isBadRequest());
	}

	@Test
	void shouldRejectUnassignRoadmapFromGroupWithNonPositiveGroupIdTest() throws Exception {
		// Given/When: groupId violates the generated @Min(1) on the path variable
		var result = mvc.perform(delete("/api/v1/roadmaps/1/group-assignments/0"));

		// Then:
		result.andExpect(status().isBadRequest());
	}
}
