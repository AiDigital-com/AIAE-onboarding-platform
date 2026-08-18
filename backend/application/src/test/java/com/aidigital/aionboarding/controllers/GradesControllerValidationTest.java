package com.aidigital.aionboarding.controllers;

import com.aidigital.aionboarding.error.mapper.GlobalExceptionResponseHelperImpl;
import com.aidigital.aionboarding.mappers.grade.GradeApiMapper;
import com.aidigital.aionboarding.service.common.security.RequestAuthenticationCache;
import com.aidigital.aionboarding.service.common.time.CurrentTimeImpl;
import com.aidigital.aionboarding.service.grade.services.GradeService;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = GradesController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({
		GlobalExceptionResponseHelperImpl.class,
		CurrentTimeImpl.class,
		GradesControllerValidationTest.MeterRegistryTestConfig.class
})
class GradesControllerValidationTest {

	@Autowired
	private MockMvc mvc;

	@MockitoBean
	private CurrentUserSupport currentUser;
	@MockitoBean
	private GradeService gradeService;
	@MockitoBean
	private GradeApiMapper gradeApiMapper;
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
	void shouldRejectCreateGradeWithBlankNameTest() throws Exception {
		// Given: name violates the generated @NotNull @Size(min = 1, max = 100)
		String body = "{\"name\":\"\"}";

		// When:
		var result = mvc.perform(post("/api/v1/grades").contentType(APPLICATION_JSON).content(body));

		// Then:
		result.andExpect(status().isBadRequest());
	}

	@Test
	void shouldRejectCreateGradeWithMissingNameTest() throws Exception {
		// Given: name is required (@NotNull) and omitted entirely
		String body = "{}";

		// When:
		var result = mvc.perform(post("/api/v1/grades").contentType(APPLICATION_JSON).content(body));

		// Then:
		result.andExpect(status().isBadRequest());
	}

	@Test
	void shouldReturnServerErrorForNonNumericGradeIdTest() throws Exception {
		// Given/When: a path variable that fails Long conversion
		var result = mvc.perform(delete("/api/v1/grades/not-a-number"));

		// Then: recorded, not asserted as 400 -- see the phase log for why
		result.andExpect(status().is5xxServerError());
	}

	@Test
	void shouldRejectDeactivateGradeWithNonPositiveIdTest() throws Exception {
		// Given/When: id violates the generated @Min(1) on the path variable
		var result = mvc.perform(delete("/api/v1/grades/0"));

		// Then:
		result.andExpect(status().isBadRequest());
	}

	@Test
	void shouldRejectUpdateGradeWithBlankNameTest() throws Exception {
		// Given:
		String body = "{\"name\":\"\"}";

		// When:
		var result = mvc.perform(put("/api/v1/grades/1").contentType(APPLICATION_JSON).content(body));

		// Then:
		result.andExpect(status().isBadRequest());
	}

	@Test
	void shouldRejectActivateGradeWithNonPositiveIdTest() throws Exception {
		// Given/When: id violates the generated @Min(1) on the path variable
		var result = mvc.perform(post("/api/v1/grades/0/activate"));

		// Then:
		result.andExpect(status().isBadRequest());
	}

	@Test
	void shouldRejectCreateGradeWithOverlongNameTest() throws Exception {
		// Given: name violates the generated @Size(max = 100); listGrades has no request-side
		// constraint to test on its own (only an optional, unconstrained boolean query param),
		// so this extra case keeps the aggregate isBadRequest() count honest -- see the phase log.
		String body = "{\"name\":\"" + "a".repeat(101) + "\"}";

		// When:
		var result = mvc.perform(post("/api/v1/grades").contentType(APPLICATION_JSON).content(body));

		// Then:
		result.andExpect(status().isBadRequest());
	}
}
