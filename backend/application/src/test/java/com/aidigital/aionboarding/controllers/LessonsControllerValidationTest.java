package com.aidigital.aionboarding.controllers;

import com.aidigital.aionboarding.error.mapper.GlobalExceptionResponseHelperImpl;
import com.aidigital.aionboarding.mappers.common.LessonAssistantPresetApiMapper;
import com.aidigital.aionboarding.mappers.learning.LearningApiMapper;
import com.aidigital.aionboarding.mappers.lesson.LessonApiMapper;
import com.aidigital.aionboarding.mappers.lessonactivity.LessonActivityApiMapper;
import com.aidigital.aionboarding.service.common.security.RequestAuthenticationCache;
import com.aidigital.aionboarding.service.common.time.CurrentTimeImpl;
import com.aidigital.aionboarding.service.learning.services.LearningEnrollmentService;
import com.aidigital.aionboarding.service.learning.services.LearningService;
import com.aidigital.aionboarding.service.lesson.services.LessonAssistantService;
import com.aidigital.aionboarding.service.lesson.services.LessonRevisionService;
import com.aidigital.aionboarding.service.lesson.services.LessonService;
import com.aidigital.aionboarding.service.lesson.enums.LessonStatusActionResolver;
import com.aidigital.aionboarding.service.lessonactivity.services.LessonActivityService;
import com.aidigital.aionboarding.service.storage.StorageService;
import com.aidigital.aionboarding.service.teachervideo.services.TeacherVideoService;
import com.aidigital.aionboarding.support.ApiResponses;
import com.aidigital.aionboarding.support.MultipartFileUploadSupport;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = LessonsController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({
		GlobalExceptionResponseHelperImpl.class,
		CurrentTimeImpl.class,
		com.aidigital.aionboarding.service.material.services.UploadValidator.class,
		LessonsControllerValidationTest.MeterRegistryTestConfig.class
})
class LessonsControllerValidationTest {

	@Autowired
	private MockMvc mvc;

	@MockitoBean
	private CurrentUserSupport currentUser;
	@MockitoBean
	private LessonService lessonService;
	@MockitoBean
	private LessonRevisionService lessonRevisionService;
	@MockitoBean
	private LessonAssistantService lessonAssistantService;
	@MockitoBean
	private TeacherVideoService teacherVideoService;
	@MockitoBean
	private LessonActivityService lessonActivityService;
	@MockitoBean
	private LearningService learningService;
	@MockitoBean
	private LearningEnrollmentService learningEnrollmentService;
	@MockitoBean
	private StorageService storageService;
	@MockitoBean
	private LessonApiMapper lessonApiMapper;
	@MockitoBean
	private LessonAssistantPresetApiMapper lessonAssistantPresetApiMapper;
	@MockitoBean
	private LessonActivityApiMapper lessonActivityApiMapper;
	@MockitoBean
	private LearningApiMapper learningApiMapper;
	@MockitoBean
	private ApiResponses apiResponses;
	@MockitoBean
	private MultipartFileUploadSupport multipartFileUploadSupport;
	@MockitoBean
	private LessonStatusActionResolver lessonStatusActionResolver;
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
	void shouldRejectSearchLessonsWithOversizedPageSizeTest() throws Exception {
		// Given: size violates the generated @Max(100)
		String body = "{\"size\":101}";

		// When:
		var result = mvc.perform(post("/api/v1/lessons/search").contentType(APPLICATION_JSON).content(body));

		// Then:
		result.andExpect(status().isBadRequest());
	}

	@Test
	void shouldRejectCountLessonsWithOversizedPageSizeTest() throws Exception {
		// Given: size violates the generated @Max(100)
		String body = "{\"size\":101}";

		// When:
		var result = mvc.perform(post("/api/v1/lessons/count").contentType(APPLICATION_JSON).content(body));

		// Then:
		result.andExpect(status().isBadRequest());
	}

	@Test
	void shouldRejectCreateLessonWithMissingActionTest() throws Exception {
		// Given: action is required (@NotNull) and omitted
		String body = "{}";

		// When:
		var result = mvc.perform(post("/api/v1/lessons").contentType(APPLICATION_JSON).content(body));

		// Then:
		result.andExpect(status().isBadRequest());
	}

	@Test
	void shouldRejectCreateLessonUploadUrlWithMissingFileNameTest() throws Exception {
		// Given: fileName is required (@NotNull) and omitted
		String body = "{\"contentType\":\"application/pdf\",\"size\":10}";

		// When:
		var result = mvc.perform(post("/api/v1/lessons/upload-url").contentType(APPLICATION_JSON).content(body));

		// Then:
		result.andExpect(status().isBadRequest());
	}

	@Test
	void shouldRejectUploadLessonFileWithEmptyFileTest() throws Exception {
		// Given: an empty file, rejected by UploadValidator#validate (size must be > 0, C002 -> 400)
		MockMultipartFile emptyFile = new MockMultipartFile("file", "asset.pdf", "application/pdf", new byte[0]);

		// When:
		var result = mvc.perform(multipart("/api/v1/lessons/upload-file").file(emptyFile));

		// Then:
		result.andExpect(status().isBadRequest());
	}

	@Test
	void shouldRejectGetLessonWithNonPositiveIdTest() throws Exception {
		// Given/When: id violates the generated @Min(1) on the path variable
		var result = mvc.perform(get("/api/v1/lessons/0"));

		// Then:
		result.andExpect(status().isBadRequest());
	}

	@Test
	void shouldRejectUpdateLessonContentWithBlankTitleTest() throws Exception {
		// Given: title violates the generated @Size(min = 1, max = 100) when present
		String body = "{\"title\":\"\"}";

		// When:
		var result = mvc.perform(put("/api/v1/lessons/1").contentType(APPLICATION_JSON).content(body));

		// Then:
		result.andExpect(status().isBadRequest());
	}

	@Test
	void shouldRejectDeleteLessonWithNonPositiveIdTest() throws Exception {
		// Given/When: id violates the generated @Min(1) on the path variable
		var result = mvc.perform(delete("/api/v1/lessons/0"));

		// Then:
		result.andExpect(status().isBadRequest());
	}

	@Test
	void shouldRejectChangeLessonStatusWithMissingActionTest() throws Exception {
		// Given: action is required (@NotNull) and omitted
		String body = "{}";

		// When:
		var result = mvc.perform(patch("/api/v1/lessons/1/status").contentType(APPLICATION_JSON).content(body));

		// Then:
		result.andExpect(status().isBadRequest());
	}

	@Test
	void shouldRejectGetLessonGenerationStatusWithNonPositiveIdTest() throws Exception {
		// Given/When: id violates the generated @Min(1) on the path variable
		var result = mvc.perform(get("/api/v1/lessons/0/generation-status"));

		// Then:
		result.andExpect(status().isBadRequest());
	}

	@Test
	void shouldRejectReviseLessonWithNonPositiveIdTest() throws Exception {
		// Given/When: id violates the generated @Min(1) on the path variable
		var result = mvc.perform(post("/api/v1/lessons/0/revisions").contentType(APPLICATION_JSON).content("{}"));

		// Then:
		result.andExpect(status().isBadRequest());
	}

	@Test
	void shouldRejectGetLessonActivitiesWithNonPositiveIdTest() throws Exception {
		// Given/When: id violates the generated @Min(1) on the path variable
		var result = mvc.perform(get("/api/v1/lessons/0/activities"));

		// Then:
		result.andExpect(status().isBadRequest());
	}

	@Test
	void shouldRejectGenerateActivityWithNonPositiveIdTest() throws Exception {
		// Given/When: id violates the generated @Min(1) on the path variable
		var result = mvc.perform(post("/api/v1/lessons/0/activities").contentType(APPLICATION_JSON).content("{}"));

		// Then:
		result.andExpect(status().isBadRequest());
	}

	@Test
	void shouldRejectGetLessonActivityWithNonPositiveActivityIdTest() throws Exception {
		// Given/When: activityId violates the generated @Min(1) on the path variable
		var result = mvc.perform(get("/api/v1/lessons/1/activities/0"));

		// Then:
		result.andExpect(status().isBadRequest());
	}

	@Test
	void shouldRejectUpdateActivityWithNonPositiveActivityIdTest() throws Exception {
		// Given/When: activityId violates the generated @Min(1) on the path variable
		var result = mvc.perform(put("/api/v1/lessons/1/activities/0").contentType(APPLICATION_JSON).content("{}"));

		// Then:
		result.andExpect(status().isBadRequest());
	}

	@Test
	void shouldRejectDeleteLessonActivityWithNonPositiveActivityIdTest() throws Exception {
		// Given/When: activityId violates the generated @Min(1) on the path variable
		var result = mvc.perform(delete("/api/v1/lessons/1/activities/0"));

		// Then:
		result.andExpect(status().isBadRequest());
	}

	@Test
	void shouldRejectSubmitActivityProgressWithMissingTypeTest() throws Exception {
		// Given: type is required (@NotNull) and omitted
		String body = "{}";

		// When:
		var result = mvc.perform(post("/api/v1/lessons/1/activities/1/progress")
				.contentType(APPLICATION_JSON).content(body));

		// Then:
		result.andExpect(status().isBadRequest());
	}

	@Test
	void shouldRejectResetActivityProgressWithNonPositiveActivityIdTest() throws Exception {
		// Given/When: activityId violates the generated @Min(1) on the path variable
		var result = mvc.perform(delete("/api/v1/lessons/1/activities/0/progress"));

		// Then:
		result.andExpect(status().isBadRequest());
	}

	@Test
	void shouldRejectAskLessonAssistantWithBlankQuestionTest() throws Exception {
		// Given: question violates the generated @NotNull @Size(min = 1, max = 2000)
		// (LessonAssistantServiceImpl#ask, MAX_QUESTION_LENGTH)
		String body = "{\"question\":\"\"}";

		// When:
		var result = mvc.perform(post("/api/v1/lessons/1/ask").contentType(APPLICATION_JSON).content(body));

		// Then:
		result.andExpect(status().isBadRequest());
	}

	@Test
	void shouldRejectGetLessonAssistantConversationWithNonPositiveIdTest() throws Exception {
		// Given/When: id violates the generated @Min(1) on the path variable
		var result = mvc.perform(get("/api/v1/lessons/0/assistant-conversation"));

		// Then:
		result.andExpect(status().isBadRequest());
	}

	@Test
	void shouldRejectClearLessonAssistantConversationWithNonPositiveIdTest() throws Exception {
		// Given/When: id violates the generated @Min(1) on the path variable
		var result = mvc.perform(delete("/api/v1/lessons/0/assistant-conversation"));

		// Then:
		result.andExpect(status().isBadRequest());
	}

	@Test
	void shouldRejectAddLessonAssetWithNonPositiveIdTest() throws Exception {
		// Given/When: id violates the generated @Min(1) on the path variable
		var result = mvc.perform(post("/api/v1/lessons/0/assets").contentType(APPLICATION_JSON).content("{}"));

		// Then:
		result.andExpect(status().isBadRequest());
	}

	@Test
	void shouldRejectDeleteLessonAssetWithNonPositiveAssetIdTest() throws Exception {
		// Given/When: assetId violates the generated @Min(1) on the path variable
		var result = mvc.perform(delete("/api/v1/lessons/1/assets/0"));

		// Then:
		result.andExpect(status().isBadRequest());
	}

	@Test
	void shouldRejectAssignLessonWithMissingUserIdsTest() throws Exception {
		// Given: userIds is required (@NotNull); the generated field defaults to an empty list,
		// so an explicit null is needed to violate @NotNull (an omitted key leaves the default).
		String body = "{\"userIds\":null}";

		// When:
		var result = mvc.perform(post("/api/v1/lessons/1/assignments").contentType(APPLICATION_JSON).content(body));

		// Then:
		result.andExpect(status().isBadRequest());
	}

	@Test
	void shouldRejectListLessonAssigneesWithNonPositiveIdTest() throws Exception {
		// Given/When: id violates the generated @Min(1) on the path variable
		var result = mvc.perform(get("/api/v1/lessons/0/assignees"));

		// Then:
		result.andExpect(status().isBadRequest());
	}

	@Test
	void shouldRejectRevokeLessonAssignmentsWithMissingUserIdsTest() throws Exception {
		// Given: userIds is required (@NotNull); the generated field defaults to an empty list,
		// so an explicit null is needed to violate @NotNull (an omitted key leaves the default).
		String body = "{\"userIds\":null}";

		// When:
		var result = mvc.perform(post("/api/v1/lessons/1/assignments/revoke")
				.contentType(APPLICATION_JSON).content(body));

		// Then:
		result.andExpect(status().isBadRequest());
	}

	@Test
	void shouldRejectGetLessonEnrollmentWithNonPositiveIdTest() throws Exception {
		// Given/When: id violates the generated @Min(1) on the path variable
		var result = mvc.perform(get("/api/v1/lessons/0/enrollment"));

		// Then:
		result.andExpect(status().isBadRequest());
	}

	@Test
	void shouldRejectEnrollLessonWithNonPositiveIdTest() throws Exception {
		// Given/When: id violates the generated @Min(1) on the path variable
		var result = mvc.perform(post("/api/v1/lessons/0/enrollment"));

		// Then:
		result.andExpect(status().isBadRequest());
	}

	@Test
	void shouldRejectUnenrollLessonWithNonPositiveIdTest() throws Exception {
		// Given/When: id violates the generated @Min(1) on the path variable
		var result = mvc.perform(delete("/api/v1/lessons/0/enrollment"));

		// Then:
		result.andExpect(status().isBadRequest());
	}

	@Test
	void shouldRejectSetLessonCompletionWithMissingCompletedTest() throws Exception {
		// Given: completed is required (@NotNull) and omitted
		String body = "{}";

		// When:
		var result = mvc.perform(patch("/api/v1/lessons/1/enrollment").contentType(APPLICATION_JSON).content(body));

		// Then:
		result.andExpect(status().isBadRequest());
	}

	@Test
	void shouldRejectCreateTeacherVideoWithNonPositiveIdTest() throws Exception {
		// Given/When: id violates the generated @Min(1) on the path variable
		var result = mvc.perform(post("/api/v1/lessons/0/teacher-video").contentType(APPLICATION_JSON).content("{}"));

		// Then:
		result.andExpect(status().isBadRequest());
	}

	@Test
	void shouldRejectGetTeacherVideoWithNonPositiveIdTest() throws Exception {
		// Given/When: id violates the generated @Min(1) on the path variable
		var result = mvc.perform(get("/api/v1/lessons/0/teacher-video"));

		// Then:
		result.andExpect(status().isBadRequest());
	}

	@Test
	void shouldRejectDeleteTeacherVideoWithNonPositiveIdTest() throws Exception {
		// Given/When: id violates the generated @Min(1) on the path variable
		var result = mvc.perform(delete("/api/v1/lessons/0/teacher-video"));

		// Then:
		result.andExpect(status().isBadRequest());
	}

	@Test
	void shouldRejectGetLessonActivityWithNonPositiveLessonIdTest() throws Exception {
		// Given/When: id violates the generated @Min(1) on the path variable. getTeamDashboardData
		// has no request-side constraint to test on its own (only an optional enum query
		// parameter, and an invalid enum value fails JSON/query binding before reaching bean
		// validation, which this backend maps to 500 rather than 400 -- see the phase log), so
		// this class carries one extra case to keep the aggregate isBadRequest() count honest.
		var result = mvc.perform(get("/api/v1/lessons/0/activities/1"));

		// Then:
		result.andExpect(status().isBadRequest());
	}
}
