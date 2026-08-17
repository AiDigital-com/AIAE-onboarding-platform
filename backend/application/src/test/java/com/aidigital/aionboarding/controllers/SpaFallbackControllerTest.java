package com.aidigital.aionboarding.controllers;

import com.aidigital.aionboarding.error.mapper.GlobalExceptionResponseHelperImpl;
import com.aidigital.aionboarding.service.common.security.RequestAuthenticationCache;
import com.aidigital.aionboarding.service.common.time.CurrentTimeImpl;
import com.aidigital.aionboarding.web.SpaFallbackController;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.method.HandlerMethod;

import java.io.UnsupportedEncodingException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * MockMvc-level tests for {@link SpaFallbackController}'s routing patterns:
 * which paths are forwarded to the built SPA shell and which are left for
 * other handlers (API, actuator, docs, static assets, or plain 404).
 * {@link SpaFallbackController#INDEX} resolves from the {@code static/index.html}
 * fixture on the test classpath (see {@code src/test/resources/static/index.html}),
 * so the 200 branch of {@code indexHtml()} is exercised end to end; the
 * "frontend not built yet" 404 branch depends on that classpath resource
 * being absent and is not reachable from a test that must not touch
 * production code to swap it out.
 */
@WebMvcTest(controllers = SpaFallbackController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({
		GlobalExceptionResponseHelperImpl.class,
		CurrentTimeImpl.class,
		SpaFallbackControllerTest.MeterRegistryTestConfig.class
})
class SpaFallbackControllerTest {

	@Autowired
	private MockMvc mvc;

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
	void shouldServeSpaShellForRootPathTest() throws Exception {
		// When:
		MvcResult result = mvc.perform(get("/")).andExpect(status().isOk()).andReturn();

		// Then:
		assertSpaShellResponse(result.getResponse());
	}

	@Test
	void shouldServeSpaShellForExplicitIndexHtmlPathTest() throws Exception {
		// When:
		MvcResult result = mvc.perform(get("/index.html")).andExpect(status().isOk()).andReturn();

		// Then:
		assertSpaShellResponse(result.getResponse());
	}

	@Test
	void shouldServeSpaShellForSingleSegmentClientRouteTest() throws Exception {
		// When:
		MvcResult result = mvc.perform(get("/login")).andExpect(status().isOk()).andReturn();

		// Then:
		assertSpaShellResponse(result.getResponse());
	}

	@Test
	void shouldServeSpaShellForNestedClientRouteTest() throws Exception {
		// When:
		MvcResult result = mvc.perform(get("/reports/123/edit")).andExpect(status().isOk()).andReturn();

		// Then:
		assertSpaShellResponse(result.getResponse());
	}

	@Test
	void shouldNotInterceptApiPathTest() throws Exception {
		// When / Then: leading "api" segment is excluded by both the single-segment
		// and nested @GetMapping patterns, so no SPA mapping matches
		assertNotHandledBySpaFallback(mvc.perform(get("/api/v1/users/me")).andReturn());
	}

	@Test
	void shouldNotInterceptActuatorPathTest() throws Exception {
		// When / Then: leading "actuator" segment is excluded
		assertNotHandledBySpaFallback(mvc.perform(get("/actuator/health")).andReturn());
	}

	@Test
	void shouldNotInterceptSwaggerUiPathTest() throws Exception {
		// When / Then: leading "swagger-ui" segment is excluded
		assertNotHandledBySpaFallback(mvc.perform(get("/swagger-ui/index.html")).andReturn());
	}

	@Test
	void shouldNotInterceptOpenApiDocsPathTest() throws Exception {
		// When / Then: leading "v3" segment is excluded
		assertNotHandledBySpaFallback(mvc.perform(get("/v3/api-docs")).andReturn());
	}

	@Test
	void shouldNotInterceptStaticAssetPathTest() throws Exception {
		// When / Then: leading "assets" segment is excluded
		assertNotHandledBySpaFallback(mvc.perform(get("/assets/app.abc123.js")).andReturn());
	}

	@Test
	void shouldNotInterceptSingleSegmentPathWithFileExtensionTest() throws Exception {
		// When / Then: the single-segment pattern requires no "." in the segment,
		// so a bare file-like path (no built-in mapping for it either) is not forwarded
		assertNotHandledBySpaFallback(mvc.perform(get("/logo.png")).andReturn());
	}

	/**
	 * Asserts the path was not claimed by {@link SpaFallbackController}.
	 *
	 * <p>The exact status is deliberately not asserted. In this {@code @WebMvcTest}
	 * slice no other handler is registered, so an unmatched path surfaces as a 500
	 * from {@code GlobalExceptionHandler} rather than the 404 the full application
	 * returns. That status is a property of the slice, not of the controller. What
	 * this controller is responsible for — and all these tests need to prove — is
	 * that its patterns do not match the path.
	 */
	void assertNotHandledBySpaFallback(MvcResult result) {
		Object handler = result.getHandler();
		boolean handledBySpaFallback = handler instanceof HandlerMethod handlerMethod
				&& handlerMethod.getBeanType().equals(SpaFallbackController.class);
		assertThat(handledBySpaFallback).isFalse();
	}

	void assertSpaShellResponse(MockHttpServletResponse response) throws UnsupportedEncodingException {
		assertThat(response.getContentType()).isEqualTo(MediaType.TEXT_HTML_VALUE);
		assertThat(response.getHeader(HttpHeaders.CACHE_CONTROL)).contains("no-cache").contains("must-revalidate");
		assertThat(response.getContentAsString()).contains("<div id=\"root\"></div>");
	}
}
