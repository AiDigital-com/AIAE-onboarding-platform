package com.aidigital.aionboarding.controllers;

import com.aidigital.aionboarding.error.mapper.GlobalExceptionResponseHelperImpl;
import com.aidigital.aionboarding.mappers.material.MaterialApiMapper;
import com.aidigital.aionboarding.service.common.security.RequestAuthenticationCache;
import com.aidigital.aionboarding.service.common.time.CurrentTimeImpl;
import com.aidigital.aionboarding.service.material.services.MaterialService;
import com.aidigital.aionboarding.service.storage.StorageService;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = MaterialsController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({
		GlobalExceptionResponseHelperImpl.class,
		CurrentTimeImpl.class,
		MaterialsControllerValidationTest.MeterRegistryTestConfig.class
})
class MaterialsControllerValidationTest {

	@Autowired
	private MockMvc mvc;

	@MockitoBean
	private CurrentUserSupport currentUser;
	@MockitoBean
	private MaterialService materialService;
	@MockitoBean
	private StorageService storageService;
	@MockitoBean
	private MaterialApiMapper materialApiMapper;
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
	void shouldRejectCreateMaterialWithMissingTitleTest() throws Exception {
		// Given: title is required (@NotNull) and omitted
		String body = "{}";

		// When:
		var result = mvc.perform(post("/api/v1/materials").contentType(APPLICATION_JSON).content(body));

		// Then:
		result.andExpect(status().isBadRequest());
	}

	@Test
	void shouldRejectSearchMaterialsWithOversizedPageSizeTest() throws Exception {
		// Given: size violates the generated @Max(100)
		String body = "{\"size\":101}";

		// When:
		var result = mvc.perform(post("/api/v1/materials/search").contentType(APPLICATION_JSON).content(body));

		// Then:
		result.andExpect(status().isBadRequest());
	}

	@Test
	void shouldRejectCountMaterialsWithOversizedPageSizeTest() throws Exception {
		// Given: size violates the generated @Max(100)
		String body = "{\"size\":101}";

		// When:
		var result = mvc.perform(post("/api/v1/materials/count").contentType(APPLICATION_JSON).content(body));

		// Then:
		result.andExpect(status().isBadRequest());
	}

	@Test
	void shouldRejectGetMaterialWithNonPositiveIdTest() throws Exception {
		// Given/When: id violates the generated @Min(1) on the path variable
		var result = mvc.perform(get("/api/v1/materials/0"));

		// Then:
		result.andExpect(status().isBadRequest());
	}

	@Test
	void shouldRejectUpdateMaterialWithNonPositiveIdTest() throws Exception {
		// Given/When: id violates the generated @Min(1) on the path variable
		var result = mvc.perform(put("/api/v1/materials/0").contentType(APPLICATION_JSON).content("{}"));

		// Then:
		result.andExpect(status().isBadRequest());
	}

	@Test
	void shouldRejectDeleteMaterialWithNonPositiveIdTest() throws Exception {
		// Given/When: id violates the generated @Min(1) on the path variable
		var result = mvc.perform(delete("/api/v1/materials/0"));

		// Then:
		result.andExpect(status().isBadRequest());
	}

	@Test
	void shouldRejectCreateMaterialUploadUrlWithMissingFileNameTest() throws Exception {
		// Given: fileName is required (@NotNull) and omitted
		String body = "{\"contentType\":\"application/pdf\",\"size\":10}";

		// When:
		var result = mvc.perform(post("/api/v1/materials/upload-url").contentType(APPLICATION_JSON).content(body));

		// Then:
		result.andExpect(status().isBadRequest());
	}

	@Test
	void shouldRejectCreateMaterialUploadUrlWithDisallowedContentTypeTest() throws Exception {
		// Given: contentType violates the generated @Pattern derived from UploadPurpose's allowlist
		String body = "{\"fileName\":\"a.docx\",\"contentType\":\"application/msword\",\"size\":10}";

		// When:
		var result = mvc.perform(post("/api/v1/materials/upload-url").contentType(APPLICATION_JSON).content(body));

		// Then:
		result.andExpect(status().isBadRequest());
	}

	@Test
	void shouldRejectUploadMaterialFileWithEmptyFileTest() throws Exception {
		// Given: an empty file, rejected by MaterialsController#validateUploadedFile (C002 -> 400)
		MockMultipartFile emptyFile = new MockMultipartFile("file", "doc.pdf", "application/pdf", new byte[0]);

		// When:
		var result = mvc.perform(multipart("/api/v1/materials/upload-file").file(emptyFile));

		// Then:
		result.andExpect(status().isBadRequest());
	}
}
