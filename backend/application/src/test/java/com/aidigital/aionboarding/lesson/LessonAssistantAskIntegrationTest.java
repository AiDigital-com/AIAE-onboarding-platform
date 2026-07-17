package com.aidigital.aionboarding.lesson;

import com.aidigital.aionboarding.domain.common.dictionary.LessonContentFormatCode;
import com.aidigital.aionboarding.domain.common.dictionary.LessonPublicationStatusCode;
import com.aidigital.aionboarding.domain.common.dictionary.LessonStatusCode;
import com.aidigital.aionboarding.domain.common.dictionary.entities.LessonContentFormat;
import com.aidigital.aionboarding.domain.common.dictionary.entities.LessonPublicationStatus;
import com.aidigital.aionboarding.domain.common.dictionary.entities.LessonStatus;
import com.aidigital.aionboarding.domain.common.dictionary.repositories.LessonContentFormatRepository;
import com.aidigital.aionboarding.domain.common.dictionary.repositories.LessonPublicationStatusRepository;
import com.aidigital.aionboarding.domain.common.dictionary.repositories.LessonStatusRepository;
import com.aidigital.aionboarding.domain.learning.entities.UserLesson;
import com.aidigital.aionboarding.domain.learning.repositories.UserLessonRepository;
import com.aidigital.aionboarding.domain.lesson.entities.Lesson;
import com.aidigital.aionboarding.domain.lesson.repositories.LessonRepository;
import com.aidigital.aionboarding.domain.user.entities.User;
import com.aidigital.aionboarding.domain.user.repositories.UserRepository;
import com.aidigital.aionboarding.service.lessongen.model.GeneratedContentResult;
import com.aidigital.aionboarding.service.lessongen.services.LessonGenService;
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
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Regression fixture for the LazyInitializationException reported in production after
 * removing {@code @Transactional} from {@code askLessonAssistant}: with no ambient
 * transaction, {@code LessonAssistantServiceImpl.ask()} must load the lesson with
 * status/publicationStatus/createdByUser already eagerly initialised (via
 * {@code LessonEntityService#findByIdWithFetches}, not {@code getReference}), or accessing those
 * lazy associations throws once the loading session has closed. Runs against real PostgreSQL
 * with real Hibernate proxies — a Mockito-based unit test cannot reproduce this failure mode.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class LessonAssistantAskIntegrationTest {

	@Container
	@ServiceConnection
	static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

	@DynamicPropertySource
	static void enableRealSchema(DynamicPropertyRegistry registry) {
		registry.add("spring.liquibase.enabled", () -> "true");
		registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
	}

	@Autowired
	private MockMvc mvc;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private LessonRepository lessonRepository;

	@Autowired
	private UserLessonRepository userLessonRepository;

	@Autowired
	private LessonStatusRepository lessonStatusRepository;

	@Autowired
	private LessonPublicationStatusRepository lessonPublicationStatusRepository;

	@Autowired
	private LessonContentFormatRepository lessonContentFormatRepository;

	@Autowired
	private PlatformTransactionManager transactionManager;

	@MockitoBean
	private JwtDecoder jwtDecoder;

	@MockitoBean
	private LessonGenService lessonGenService;

	@Test
	void askLessonAssistantShouldReturnAnswerWithoutLazyInitializationExceptionTest() throws Exception {
		// Given: a JWT-provisioned learner enrolled in a real READY/PUBLISHED lesson
		var jwtPostProcessor = jwt().jwt(builder -> builder
				.subject("user_ask_lesson_assistant")
				.claim("user_id", "user_ask_lesson_assistant")
				.claim("email", "qa-ask-lesson-assistant@aidigital.com")
				.claim("full_name", "Ask Lesson Assistant"));
		mvc.perform(get("/api/v1/permissions").with(jwtPostProcessor)).andExpect(status().isOk());

		Lesson lesson = new TransactionTemplate(transactionManager).execute(status -> {
			User learner = userRepository.findByClerkUserId("user_ask_lesson_assistant").orElseThrow();
			Lesson savedLesson = lessonRepository.save(readyPublishedLesson());
			userLessonRepository.save(enrollment(learner, savedLesson));
			return savedLesson;
		});

		when(lessonGenService.generateLessonContent(any()))
				.thenReturn(new GeneratedContentResult("The lesson covers onboarding basics.", Map.of()));

		// When-Then: the real Hibernate session must resolve status/publicationStatus without
		// an ambient transaction; a regression here throws 500 (LazyInitializationException)
		mvc.perform(post("/api/v1/lessons/{id}/ask", lesson.getId())
						.with(jwtPostProcessor)
						.contentType("application/json")
						.content("{\"question\":\"What is this lesson about?\"}"))
				.andExpect(status().isOk())
				.andExpect(content().string(org.hamcrest.Matchers.containsString("onboarding basics")));
	}

	private Lesson readyPublishedLesson() {
		LessonStatus status = lessonStatusRepository.findByCode(LessonStatusCode.READY).orElseThrow();
		LessonPublicationStatus publicationStatus =
				lessonPublicationStatusRepository.findByCode(LessonPublicationStatusCode.PUBLISHED).orElseThrow();
		LessonContentFormat contentFormat =
				lessonContentFormatRepository.findByCode(LessonContentFormatCode.MARKDOWN).orElseThrow();
		LocalDateTime now = LocalDateTime.of(2026, 1, 1, 0, 0);

		Lesson lesson = new Lesson();
		lesson.setTitle("Onboarding basics");
		lesson.setDescription("description");
		lesson.setStatus(status);
		lesson.setUserInstructions("");
		lesson.setDepth("standard");
		lesson.setTone("clear");
		lesson.setDesiredFormat("structured theoretical lesson");
		lesson.setContentFormat(contentFormat);
		lesson.setContentMarkdown("content");
		lesson.setContentHtml("<p>content</p>");
		lesson.setCoverImageStorageKey("");
		lesson.setCoverImageOriginalName("");
		lesson.setCoverImageMimeType("");
		lesson.setGenerationMetadata(Map.of());
		lesson.setRevisionHistory(List.of());
		lesson.setErrorMessage("");
		lesson.setPublicationStatus(publicationStatus);
		lesson.setCreatedBy("tester");
		lesson.setTags(List.of());
		lesson.setCreatedAt(now);
		lesson.setUpdatedAt(now);
		return lesson;
	}

	private UserLesson enrollment(User user, Lesson lesson) {
		UserLesson userLesson = new UserLesson();
		UserLesson.UserLessonId id = new UserLesson.UserLessonId();
		id.setUserId(user.getId());
		id.setLessonId(lesson.getId());
		userLesson.setId(id);
		userLesson.setUser(user);
		userLesson.setLesson(lesson);
		userLesson.setEnrolledAt(LocalDateTime.of(2026, 1, 1, 0, 0));
		return userLesson;
	}
}
