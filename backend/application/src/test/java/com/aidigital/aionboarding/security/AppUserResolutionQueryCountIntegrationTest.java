package com.aidigital.aionboarding.security;

import com.aidigital.aionboarding.domain.user.entities.User;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Regression fixture: an authenticated request whose Clerk-linked fields (id, email, name)
 * are already identical to what's stored
 * must not write a no-op {@code users} UPDATE. Hits an authenticated-but-otherwise-unprotected
 * endpoint twice with identical JWT claims across two SEPARATE HTTP requests (so the
 * request-scoped {@code RequestAuthenticationCache} is cleared in between, exercising
 * {@code UserServiceImpl.resolveOrCreateFromClerk}'s own conditional-save fresh each time) and
 * asserts the second, unchanged request performs zero {@code User} entity updates.
 * <p>
 * Needs {@code @ActiveProfiles("test")} for its {@code app.auth.authorized-parties} override
 * (otherwise {@code AuthStartupValidator} fails fast) — but that profile also disables
 * Liquibase and uses Hibernate {@code ddl-auto=create-drop}, which leaves dictionary tables
 * (e.g. {@code user_roles}) unseeded. First-login provisioning needs the real {@code member}
 * role row, so Liquibase and schema validation are switched back on here, seeding the real
 * dictionary data against the Testcontainers Postgres exactly like the default profile would.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class AppUserResolutionQueryCountIntegrationTest {

	@Container
	@ServiceConnection
	static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

	@DynamicPropertySource
	static void enableHibernateStatisticsAndRealSchema(DynamicPropertyRegistry registry) {
		registry.add("spring.jpa.properties.hibernate.generate_statistics", () -> "true");
		registry.add("spring.liquibase.enabled", () -> "true");
		registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
	}

	@Autowired
	private MockMvc mvc;

	@Autowired
	private EntityManagerFactory entityManagerFactory;

	@MockitoBean
	private JwtDecoder jwtDecoder;

	@Test
	void unchangedAuthenticatedUserPerformsNoUpdateOnSubsequentRequestTest() throws Exception {
		// Given:
		Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
		var jwtPostProcessor = jwt().jwt(builder -> builder
				.subject("user_query_count_baseline")
				.claim("user_id", "user_query_count_baseline")
				.claim("email", "qa-query-count-baseline@aidigital.com")
				.claim("full_name", "Query Count Baseline"));

		// When: first call provisions the user; second (separate) request repeats identical claims.
		mvc.perform(get("/api/v1/permissions").with(jwtPostProcessor)).andExpect(status().isOk());
		statistics.clear();
		mvc.perform(get("/api/v1/permissions").with(jwtPostProcessor)).andExpect(status().isOk());

		// Then: the second request's claims are unchanged from what's stored, so it must not
		// write a no-op UPDATE.
		long userUpdatesOnUnchangedRequest = statistics.getEntityStatistics(User.class.getName()).getUpdateCount();
		assertThat(userUpdatesOnUnchangedRequest)
				.as("an unchanged authenticated user must not be UPDATEd")
				.isZero();
	}
}
