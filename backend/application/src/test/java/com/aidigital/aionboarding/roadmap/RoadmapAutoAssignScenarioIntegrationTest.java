package com.aidigital.aionboarding.roadmap;

import com.aidigital.aionboarding.domain.common.dictionary.UserRoleCode;
import com.aidigital.aionboarding.domain.common.dictionary.entities.UserRole;
import com.aidigital.aionboarding.domain.common.dictionary.repositories.UserRoleRepository;
import com.aidigital.aionboarding.domain.roadmap.entities.Roadmap;
import com.aidigital.aionboarding.domain.roadmap.repositories.RoadmapRepository;
import com.aidigital.aionboarding.domain.user.entities.User;
import com.aidigital.aionboarding.domain.user.repositories.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.testcontainers.containers.PostgreSQLContainer;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end scenarios for "Automatically assign to new users" (PAC_084) over the real HTTP
 * layer, security chain and PostgreSQL schema: the admin-only toggle, first-login enrollment
 * of a brand-new user, the assignee summary on the roadmap card, the library filter and the
 * "how did this person get the roadmap" sources in the assign dialog.
 * <p>
 * Like {@code AppUserResolutionQueryCountIntegrationTest},
 * Liquibase and schema validation are switched back on because first-login provisioning needs
 * the real {@code user_role} dictionary rows. The database is a Testcontainers Postgres by
 * default; set {@code SCENARIO_DB_URL} (plus {@code SCENARIO_DB_USER} / {@code SCENARIO_DB_PASSWORD})
 * to run the same scenarios against an existing empty local database instead.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@EnabledIf("hasDatabase")
class RoadmapAutoAssignScenarioIntegrationTest {

	private static final String AUTO_ASSIGN_PATH = "/api/v1/roadmaps/{id}/auto-assign";
	private static final String ASSIGNEES_PATH = "/api/v1/roadmaps/{id}/assignees";
	private static final String ASSIGNMENTS_PATH = "/api/v1/roadmaps/{id}/assignments";
	private static final String REVOKE_PATH = "/api/v1/roadmaps/{id}/assignments/revoke";
	private static final String GROUP_ASSIGNMENTS_PATH = "/api/v1/roadmaps/{id}/group-assignments";
	private static final String SEARCH_PATH = "/api/v1/roadmaps/search";
	private static final String FIRST_LOGIN_PATH = "/api/v1/permissions";
	private static final int PREVIEW_SIZE = 4;

	private static final String LOCAL_DB_URL = System.getenv("SCENARIO_DB_URL");
	private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

	/** Enables the class when a local database is configured or a Testcontainers Postgres can start. */
	static boolean hasDatabase() {
		if (LOCAL_DB_URL != null) {
			return true;
		}
		try {
			POSTGRES.start();
			return true;
		} catch (RuntimeException dockerUnavailable) {
			return false;
		}
	}

	@DynamicPropertySource
	static void useRealSchema(DynamicPropertyRegistry registry) {
		if (LOCAL_DB_URL != null) {
			registry.add("spring.datasource.url", () -> LOCAL_DB_URL);
			registry.add("spring.datasource.username", () -> System.getenv().getOrDefault("SCENARIO_DB_USER", "app"));
			registry.add("spring.datasource.password", () -> System.getenv().getOrDefault("SCENARIO_DB_PASSWORD", "app"));
		} else {
			registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
			registry.add("spring.datasource.username", POSTGRES::getUsername);
			registry.add("spring.datasource.password", POSTGRES::getPassword);
		}
		registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
		registry.add("spring.liquibase.enabled", () -> "true");
		registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
	}

	@Autowired
	private MockMvc mvc;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private UserRoleRepository userRoleRepository;

	@Autowired
	private RoadmapRepository roadmapRepository;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	private JwtDecoder jwtDecoder;

	/** One admin, one teamlead who authored the roadmap, one plain member, one roadmap. */
	private record Scenario(User admin, User teamlead, User member, Roadmap roadmap) {
	}

	private Scenario seed() {
		User admin = userRepository.save(user(UserRoleCode.ADMIN, "admin"));
		User teamlead = userRepository.save(user(UserRoleCode.TEAMLEAD, "lead"));
		User member = userRepository.save(user(UserRoleCode.MEMBER, "member"));
		return new Scenario(admin, teamlead, member, roadmapRepository.save(roadmap(teamlead)));
	}

	@Nested
	class Toggle {

		@Test
		void shouldLetAdminTurnAutoAssignOnAndOffTest() throws Exception {
			// Given:
			Scenario s = seed();

			// When: admin turns the rule on
			mvc.perform(put(AUTO_ASSIGN_PATH, s.roadmap().getId()).with(as(s.admin()))
							.contentType(MediaType.APPLICATION_JSON).content("{\"enabled\":true}"))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.roadmap.autoAssignNewUsers").value(true))
					.andExpect(jsonPath("$.roadmap.assigneeCount").value(0));

			// Then: it is persisted and the library filter finds the roadmap
			assertThat(roadmapRepository.findById(s.roadmap().getId()).orElseThrow().isAutoAssignNewUsers()).isTrue();
			mvc.perform(post(SEARCH_PATH).with(as(s.admin()))
							.contentType(MediaType.APPLICATION_JSON).content("{\"autoAssignNewUsers\":true}"))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.roadmaps[?(@.id == " + s.roadmap().getId() + ")]").exists());

			// When: admin turns it off again
			mvc.perform(put(AUTO_ASSIGN_PATH, s.roadmap().getId()).with(as(s.admin()))
							.contentType(MediaType.APPLICATION_JSON).content("{\"enabled\":false}"))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.roadmap.autoAssignNewUsers").value(false));

			// Then: the filter no longer matches
			mvc.perform(post(SEARCH_PATH).with(as(s.admin()))
							.contentType(MediaType.APPLICATION_JSON).content("{\"autoAssignNewUsers\":true}"))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.roadmaps[?(@.id == " + s.roadmap().getId() + ")]").doesNotExist());
		}

		@Test
		void shouldRejectTheToggleForATeamleadWhoOwnsTheRoadmapTest() throws Exception {
			// Given:
			Scenario s = seed();

			// the teamlead is the roadmap author, so they may manage it but are not admin

			// When / Then:
			mvc.perform(put(AUTO_ASSIGN_PATH, s.roadmap().getId()).with(as(s.teamlead()))
							.contentType(MediaType.APPLICATION_JSON).content("{\"enabled\":true}"))
					.andExpect(status().isForbidden());
			assertThat(roadmapRepository.findById(s.roadmap().getId()).orElseThrow().isAutoAssignNewUsers()).isFalse();
		}

		@Test
		void shouldRejectTheToggleForAMemberTest() throws Exception {
			// Given:
			Scenario s = seed();

			// When / Then:
			mvc.perform(put(AUTO_ASSIGN_PATH, s.roadmap().getId()).with(as(s.member()))
							.contentType(MediaType.APPLICATION_JSON).content("{\"enabled\":true}"))
					.andExpect(status().isForbidden());
		}
	}

	@Nested
	class FirstLogin {

		@Test
		void shouldEnrollABrandNewUserOnFirstLoginAndShowThemAsAutoAssignedTest() throws Exception {
			// Given: the rule is on
			Scenario s = seed();
			enableAutoAssign(s);

			// When: an unknown Clerk identity hits the API for the first time
			String newClerkId = "user_new_" + UUID.randomUUID();
			String newEmail = newClerkId + "@aidigital.com";
			mvc.perform(get(FIRST_LOGIN_PATH).with(asNewUser(newClerkId, newEmail, "New Hire")))
					.andExpect(status().isOk());

			// Then: the user exists and holds the roadmap through the new-user rule
			User newHire = userRepository.findAll().stream()
					.filter(u -> newClerkId.equals(u.getClerkUserId())).findFirst().orElseThrow();
			mvc.perform(get(ASSIGNEES_PATH, s.roadmap().getId()).with(as(s.admin())))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.assignees", hasSize(1)))
					.andExpect(jsonPath("$.assignees[0].userId").value(newHire.getId().intValue()))
					.andExpect(jsonPath("$.assignees[0].autoAssigned").value(true))
					.andExpect(jsonPath("$.assignees[0].viaGroupNames", hasSize(0)));

			// Then: the new hire sees it in "assigned to me", and the card summary counts them
			mvc.perform(post(SEARCH_PATH).with(asNewUser(newClerkId, newEmail, "New Hire"))
							.contentType(MediaType.APPLICATION_JSON).content("{\"assignedToMe\":true}"))
					.andExpect(status().isOk())
					.andExpect(jsonPath(roadmapInList(s) + ".isEnrolled").value(true))
					.andExpect(jsonPath(roadmapInList(s) + ".assigneeCount").value(contains(nullValue())));
			mvc.perform(post(SEARCH_PATH).with(as(s.admin()))
							.contentType(MediaType.APPLICATION_JSON).content("{}"))
					.andExpect(status().isOk())
					.andExpect(jsonPath(roadmapInList(s) + ".assigneeCount").value(1))
					.andExpect(jsonPath(roadmapInList(s) + ".assigneePreview[0].userId").value(newHire.getId().intValue()));
		}

		@Test
		void shouldNotEnrollTheSameUserTwiceOnRepeatedLoginsTest() throws Exception {
			// Given:
			Scenario s = seed();
			enableAutoAssign(s);
			String clerkId = "user_repeat_" + UUID.randomUUID();
			RequestPostProcessor hire = asNewUser(clerkId, clerkId + "@aidigital.com", "Repeat Hire");

			// When: the same identity logs in three times
			for (int i = 0; i < 3; i++) {
				mvc.perform(get(FIRST_LOGIN_PATH).with(hire)).andExpect(status().isOk());
			}

			// Then:
			mvc.perform(get(ASSIGNEES_PATH, s.roadmap().getId()).with(as(s.admin())))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.assignees", hasSize(1)));
		}

		@Test
		void shouldNotEnrollANewUserWhenTheRuleIsOffAndNotTouchExistingUsersWhenTurnedOnTest() throws Exception {
			// Given: the rule is off and an existing member already logged in before
			Scenario s = seed();
			mvc.perform(get(FIRST_LOGIN_PATH).with(as(s.member()))).andExpect(status().isOk());

			// When: a new user logs in while the rule is off, then the rule is turned on
			String clerkId = "user_off_" + UUID.randomUUID();
			mvc.perform(get(FIRST_LOGIN_PATH).with(asNewUser(clerkId, clerkId + "@aidigital.com", "Off Hire")))
					.andExpect(status().isOk());
			enableAutoAssign(s);
			mvc.perform(get(FIRST_LOGIN_PATH).with(as(s.member()))).andExpect(status().isOk());

			// Then: neither the earlier new user nor the existing member got the roadmap
			mvc.perform(get(ASSIGNEES_PATH, s.roadmap().getId()).with(as(s.admin())))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.assignees", hasSize(0)));
		}
	}

	@Nested
	class AssigneeSources {

		@Test
		void shouldLabelDirectTeamAndAutoAssignedPeopleAndCapThePreviewTest() throws Exception {
			// Given: the rule is on; a Design team holds the member; four extra people exist
			Scenario s = seed();
			enableAutoAssign(s);
			String teamName = "Design " + UUID.randomUUID();
			long groupId = createGroup(s, teamName);
			addGroupMember(s, groupId, s.member().getId());
			List<User> extras = List.of(
					userRepository.save(user(UserRoleCode.MEMBER, "extra1")),
					userRepository.save(user(UserRoleCode.MEMBER, "extra2")),
					userRepository.save(user(UserRoleCode.MEMBER, "extra3")),
					userRepository.save(user(UserRoleCode.MEMBER, "extra4")));

			// When: the team is assigned, the extras are assigned directly, a new hire logs in
			mvc.perform(post(GROUP_ASSIGNMENTS_PATH, s.roadmap().getId()).with(as(s.admin()))
							.contentType(MediaType.APPLICATION_JSON).content("{\"groupId\":" + groupId + "}"))
					.andExpect(status().isOk());
			mvc.perform(post(ASSIGNMENTS_PATH, s.roadmap().getId()).with(as(s.admin()))
							.contentType(MediaType.APPLICATION_JSON)
							.content("{\"userIds\":[" + ids(extras) + "]}"))
					.andExpect(status().isOk());
			String clerkId = "user_src_" + UUID.randomUUID();
			String hireEmail = clerkId + "@aidigital.com";
			mvc.perform(get(FIRST_LOGIN_PATH).with(asNewUser(clerkId, hireEmail, "Src Hire")))
					.andExpect(status().isOk());

			// Then: each person carries the right source
			String body = mvc.perform(get(ASSIGNEES_PATH, s.roadmap().getId()).with(as(s.admin())))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.assignees", hasSize(6)))
					.andReturn().getResponse().getContentAsString();
			JsonNode memberRow = assigneeRow(body, s.member().getId());
			JsonNode extraRow = assigneeRow(body, extras.get(0).getId());
			JsonNode hireRow = assigneeRowByEmail(body, hireEmail);
			assertThat(memberRow.get("autoAssigned").asBoolean()).isFalse();
			assertThat(names(memberRow.get("viaGroupNames"))).containsExactly(teamName);
			assertThat(extraRow.get("autoAssigned").asBoolean()).isFalse();
			assertThat(names(extraRow.get("viaGroupNames"))).isEmpty();
			assertThat(hireRow.get("autoAssigned").asBoolean()).isTrue();
			assertThat(names(hireRow.get("viaGroupNames"))).isEmpty();

			// Then: the card shows the total but previews only four avatars
			mvc.perform(post(SEARCH_PATH).with(as(s.admin()))
							.contentType(MediaType.APPLICATION_JSON).content("{}"))
					.andExpect(status().isOk())
					.andExpect(jsonPath(roadmapInList(s) + ".assigneeCount").value(6))
					.andExpect(jsonPath(roadmapInList(s) + ".assigneePreview[*]", hasSize(PREVIEW_SIZE)));

			// When: one direct assignee is revoked
			mvc.perform(post(REVOKE_PATH, s.roadmap().getId()).with(as(s.admin()))
							.contentType(MediaType.APPLICATION_JSON)
							.content("{\"userIds\":[" + extras.get(0).getId() + "]}"))
					.andExpect(status().isOk());

			// Then:
			mvc.perform(get(ASSIGNEES_PATH, s.roadmap().getId()).with(as(s.admin())))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.assignees", hasSize(5)))
					.andExpect(jsonPath("$.assignees[?(@.userId == " + extras.get(0).getId() + ")]").doesNotExist());
		}

		@Test
		void shouldHideTheAssigneeListAndCountFromAMemberTest() throws Exception {
			// Given: the rule is on and the member holds the roadmap directly, so they can see it
			Scenario s = seed();
			enableAutoAssign(s);
			mvc.perform(post(ASSIGNMENTS_PATH, s.roadmap().getId()).with(as(s.admin()))
							.contentType(MediaType.APPLICATION_JSON)
							.content("{\"userIds\":[" + s.member().getId() + "]}"))
					.andExpect(status().isOk());

			// When / Then: a member has no learning.assign permission and sees no assignee summary
			mvc.perform(get(ASSIGNEES_PATH, s.roadmap().getId()).with(as(s.member())))
					.andExpect(status().isForbidden());
			mvc.perform(post(SEARCH_PATH).with(as(s.member()))
							.contentType(MediaType.APPLICATION_JSON).content("{}"))
					.andExpect(status().isOk())
					.andExpect(jsonPath(roadmapInList(s) + ".autoAssignNewUsers").value(true))
					.andExpect(jsonPath(roadmapInList(s) + ".viewerCanManage").value(false))
					.andExpect(jsonPath(roadmapInList(s) + ".assigneeCount").value(contains(nullValue())));
		}
	}

	private void enableAutoAssign(Scenario s) throws Exception {
		mvc.perform(put(AUTO_ASSIGN_PATH, s.roadmap().getId()).with(as(s.admin()))
						.contentType(MediaType.APPLICATION_JSON).content("{\"enabled\":true}"))
				.andExpect(status().isOk());
	}

	private long createGroup(Scenario s, String name) throws Exception {
		String body = mvc.perform(post("/api/v1/groups").with(as(s.admin()))
						.contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"" + name + "\"}"))
				.andExpect(status().is2xxSuccessful())
				.andReturn().getResponse().getContentAsString();
		return objectMapper.readTree(body).get("group").get("id").asLong();
	}

	private void addGroupMember(Scenario s, long groupId, Long userId) throws Exception {
		mvc.perform(post("/api/v1/groups/{id}/members", groupId).with(as(s.admin()))
						.contentType(MediaType.APPLICATION_JSON).content("{\"memberUserId\":" + userId + "}"))
				.andExpect(status().isOk());
	}

	private JsonNode assigneeRow(String body, Long userId) throws Exception {
		for (JsonNode row : objectMapper.readTree(body).get("assignees")) {
			if (row.get("userId").asLong() == userId) {
				return row;
			}
		}
		throw new AssertionError("No assignee row for user " + userId);
	}

	private JsonNode assigneeRowByEmail(String body, String email) throws Exception {
		for (JsonNode row : objectMapper.readTree(body).get("assignees")) {
			if (email.equals(row.get("email").asText())) {
				return row;
			}
		}
		throw new AssertionError("No assignee row for " + email);
	}

	private static List<String> names(JsonNode array) {
		List<String> names = new ArrayList<>();
		array.forEach(node -> names.add(node.asText()));
		return names;
	}

	private static String roadmapInList(Scenario s) {
		return "$.roadmaps[?(@.id == " + s.roadmap().getId() + ")]";
	}

	private static String ids(List<User> users) {
		return String.join(",", users.stream().map(u -> String.valueOf(u.getId())).toList());
	}

	private static RequestPostProcessor as(User user) {
		return asNewUser(user.getClerkUserId(), user.getEmail(), user.getName());
	}

	private static RequestPostProcessor asNewUser(String clerkUserId, String email, String fullName) {
		return jwt().jwt(builder -> builder
				.subject(clerkUserId)
				.claim("user_id", clerkUserId)
				.claim("email", email)
				.claim("full_name", fullName));
	}

	private User user(String roleCode, String handle) {
		UserRole role = userRoleRepository.findByCode(roleCode).orElseThrow();
		String unique = handle + "-" + UUID.randomUUID();
		User user = new User();
		user.setClerkUserId("user_" + unique);
		user.setName(handle);
		user.setEmail(unique + "@aidigital.com");
		user.setRole(role);
		LocalDateTime now = LocalDateTime.of(2026, 1, 1, 0, 0);
		user.setCreatedAt(now);
		user.setUpdatedAt(now);
		return user;
	}

	private Roadmap roadmap(User author) {
		Roadmap entity = new Roadmap();
		entity.setTitle("Company Essentials " + UUID.randomUUID());
		entity.setDescription("description");
		entity.setTags(List.of());
		entity.setAuthorUser(author);
		entity.setCreatedBy(author.getName());
		LocalDateTime now = LocalDateTime.of(2026, 1, 1, 0, 0);
		entity.setCreatedAt(now);
		entity.setUpdatedAt(now);
		return entity;
	}
}
