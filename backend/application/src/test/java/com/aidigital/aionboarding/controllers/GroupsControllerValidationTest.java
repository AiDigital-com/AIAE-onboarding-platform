package com.aidigital.aionboarding.controllers;

import com.aidigital.aionboarding.error.mapper.GlobalExceptionResponseHelperImpl;
import com.aidigital.aionboarding.mappers.group.GroupApiMapper;
import com.aidigital.aionboarding.mappers.roadmap.RoadmapGroupAssignmentApiMapper;
import com.aidigital.aionboarding.mappers.user.UserApiMapper;
import com.aidigital.aionboarding.service.common.security.RequestAuthenticationCache;
import com.aidigital.aionboarding.service.common.time.CurrentTimeImpl;
import com.aidigital.aionboarding.service.group.services.GroupMembershipService;
import com.aidigital.aionboarding.service.group.services.GroupService;
import com.aidigital.aionboarding.service.roadmap.services.RoadmapGroupAssignmentService;
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

@WebMvcTest(controllers = GroupsController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({
		GlobalExceptionResponseHelperImpl.class,
		CurrentTimeImpl.class,
		GroupsControllerValidationTest.MeterRegistryTestConfig.class
})
class GroupsControllerValidationTest {

	@Autowired
	private MockMvc mvc;

	@MockitoBean
	private CurrentUserSupport currentUser;
	@MockitoBean
	private GroupService groupService;
	@MockitoBean
	private GroupMembershipService groupMembershipService;
	@MockitoBean
	private RoadmapGroupAssignmentService roadmapGroupAssignmentService;
	@MockitoBean
	private GroupApiMapper groupApiMapper;
	@MockitoBean
	private UserApiMapper userApiMapper;
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
	void shouldRejectListGroupsWithOversizedPageSizeTest() throws Exception {
		// Given/When: size violates the generated @Max(100) on the query parameter
		var result = mvc.perform(get("/api/v1/groups").param("size", "101"));

		// Then:
		result.andExpect(status().isBadRequest());
	}

	@Test
	void shouldRejectCreateGroupWithMissingNameTest() throws Exception {
		// Given: name is required (@NotNull) and omitted
		String body = "{}";

		// When:
		var result = mvc.perform(post("/api/v1/groups").contentType(APPLICATION_JSON).content(body));

		// Then:
		result.andExpect(status().isBadRequest());
	}

	@Test
	void shouldRejectGetGroupWithNonPositiveIdTest() throws Exception {
		// Given/When: id violates the generated @Min(1) on the path variable
		var result = mvc.perform(get("/api/v1/groups/0"));

		// Then:
		result.andExpect(status().isBadRequest());
	}

	@Test
	void shouldRejectUpdateGroupWithTooShortNameTest() throws Exception {
		// Given: name violates the generated @Size(min = 3, max = 100)
		String body = "{\"name\":\"ab\"}";

		// When:
		var result = mvc.perform(put("/api/v1/groups/1").contentType(APPLICATION_JSON).content(body));

		// Then:
		result.andExpect(status().isBadRequest());
	}

	@Test
	void shouldRejectDeleteGroupWithNonPositiveIdTest() throws Exception {
		// Given/When: id violates the generated @Min(1) on the path variable
		var result = mvc.perform(delete("/api/v1/groups/0"));

		// Then:
		result.andExpect(status().isBadRequest());
	}

	@Test
	void shouldRejectListGroupMembersWithNonPositiveIdTest() throws Exception {
		// Given/When: id violates the generated @Min(1) on the path variable
		var result = mvc.perform(get("/api/v1/groups/0/members"));

		// Then:
		result.andExpect(status().isBadRequest());
	}

	@Test
	void shouldRejectAddGroupMemberWithMissingMemberUserIdTest() throws Exception {
		// Given: memberUserId is required (@NotNull) and omitted
		String body = "{}";

		// When:
		var result = mvc.perform(post("/api/v1/groups/1/members").contentType(APPLICATION_JSON).content(body));

		// Then:
		result.andExpect(status().isBadRequest());
	}

	@Test
	void shouldRejectRemoveGroupMemberWithNonPositiveUserIdTest() throws Exception {
		// Given/When: userId violates the generated @Min(1) on the path variable
		var result = mvc.perform(delete("/api/v1/groups/1/members/0"));

		// Then:
		result.andExpect(status().isBadRequest());
	}

	@Test
	void shouldRejectListGroupRoadmapAssignmentsWithNonPositiveIdTest() throws Exception {
		// Given/When: id violates the generated @Min(1) on the path variable
		var result = mvc.perform(get("/api/v1/groups/0/roadmap-assignments"));

		// Then:
		result.andExpect(status().isBadRequest());
	}

	@Test
	void shouldRejectListGroupCandidateUsersWithOversizedSearchTest() throws Exception {
		// Given/When: search violates the generated @Size(max = 200) on the query parameter
		var result = mvc.perform(get("/api/v1/groups/1/candidate-users").param("search", "a".repeat(201)));

		// Then:
		result.andExpect(status().isBadRequest());
	}

	@Test
	void shouldRejectAddGroupLeadWithMissingLeadUserIdTest() throws Exception {
		// Given: leadUserId is required (@NotNull) and omitted
		String body = "{}";

		// When:
		var result = mvc.perform(post("/api/v1/groups/1/leads").contentType(APPLICATION_JSON).content(body));

		// Then:
		result.andExpect(status().isBadRequest());
	}

	@Test
	void shouldRejectRemoveGroupLeadWithNonPositiveUserIdTest() throws Exception {
		// Given/When: userId violates the generated @Min(1) on the path variable
		var result = mvc.perform(delete("/api/v1/groups/1/leads/0"));

		// Then:
		result.andExpect(status().isBadRequest());
	}
}
