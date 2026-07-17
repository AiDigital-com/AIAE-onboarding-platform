package com.aidigital.aionboarding.learning;

import com.aidigital.aionboarding.domain.common.dictionary.ActivityTypeCode;
import com.aidigital.aionboarding.domain.common.dictionary.LessonContentFormatCode;
import com.aidigital.aionboarding.domain.common.dictionary.LessonPublicationStatusCode;
import com.aidigital.aionboarding.domain.common.dictionary.LessonStatusCode;
import com.aidigital.aionboarding.domain.common.dictionary.UserRoleCode;
import com.aidigital.aionboarding.domain.common.dictionary.entities.ActivityType;
import com.aidigital.aionboarding.domain.common.dictionary.entities.LessonContentFormat;
import com.aidigital.aionboarding.domain.common.dictionary.entities.LessonPublicationStatus;
import com.aidigital.aionboarding.domain.common.dictionary.entities.LessonStatus;
import com.aidigital.aionboarding.domain.common.dictionary.entities.UserRole;
import com.aidigital.aionboarding.domain.common.dictionary.repositories.ActivityTypeRepository;
import com.aidigital.aionboarding.domain.common.dictionary.repositories.LessonContentFormatRepository;
import com.aidigital.aionboarding.domain.common.dictionary.repositories.LessonPublicationStatusRepository;
import com.aidigital.aionboarding.domain.common.dictionary.repositories.LessonStatusRepository;
import com.aidigital.aionboarding.domain.common.dictionary.repositories.UserRoleRepository;
import com.aidigital.aionboarding.domain.learning.entities.UserLesson;
import com.aidigital.aionboarding.domain.learning.repositories.MyLessonSummaryProjection;
import com.aidigital.aionboarding.domain.learning.repositories.UserLessonRepository;
import com.aidigital.aionboarding.domain.lesson.entities.Lesson;
import com.aidigital.aionboarding.domain.lesson.repositories.LessonRepository;
import com.aidigital.aionboarding.domain.lessonactivity.entities.LessonActivity;
import com.aidigital.aionboarding.domain.lessonactivity.repositories.LessonActivityRepository;
import com.aidigital.aionboarding.domain.lessonactivity.repositories.LessonActivityTypeCountProjection;
import com.aidigital.aionboarding.domain.user.entities.User;
import com.aidigital.aionboarding.domain.user.repositories.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the bounded "My Lessons" summary queries against real
 * PostgreSQL: {@link UserLessonRepository#findMyLessonsPage} must return a lean projection
 * (content truncated to a short preview, never the full body), filtered to published lessons,
 * ordered incomplete-first then newest-enrolled-first, and paged; {@link
 * LessonRepository#findIdsWithTeacherVideoIn} must resolve the {@code jsonb_extract_path_text}
 * teacher-video flag; {@link LessonActivityRepository#countByLessonIdsGroupedByType} must count
 * activities per lesson/type without loading their JSONB payload.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers(disabledWithoutDocker = true)
class MyLessonsSummaryRepositoryIntegrationTest {

	@Container
	@ServiceConnection
	static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private UserRoleRepository userRoleRepository;

	@Autowired
	private LessonRepository lessonRepository;

	@Autowired
	private LessonStatusRepository lessonStatusRepository;

	@Autowired
	private LessonPublicationStatusRepository lessonPublicationStatusRepository;

	@Autowired
	private LessonContentFormatRepository lessonContentFormatRepository;

	@Autowired
	private UserLessonRepository userLessonRepository;

	@Autowired
	private LessonActivityRepository lessonActivityRepository;

	@Autowired
	private ActivityTypeRepository activityTypeRepository;

	@Test
	void findMyLessonsPageShouldReturnOnlyPublishedLessonsAsTruncatedIncompleteFirstOrderedSummariesTest() {
		// Given: learner enrolled in a published lesson with long content, an incomplete
		// published lesson enrolled earlier, and a lesson that is not published
		UserRole role = userRoleRepository.findByCode(UserRoleCode.MEMBER).orElseThrow();
		User learner = userRepository.save(user(role, "Learner", "learner@test.com"));
		String longHtml = "<p>" + "x".repeat(600) + "</p>";
		String longMarkdown = "m".repeat(600);
		Lesson completedLesson = lessonRepository.save(
				lesson("Completed Lesson", LessonPublicationStatusCode.PUBLISHED, longHtml, longMarkdown));
		Lesson incompleteLesson = lessonRepository.save(
				lesson("Incomplete Lesson", LessonPublicationStatusCode.PUBLISHED, "<p>short</p>", "short"));
		Lesson privateLesson = lessonRepository.save(
				lesson("Private Lesson", LessonPublicationStatusCode.PRIVATE, "<p>hidden</p>", "hidden"));

		LocalDateTime older = LocalDateTime.of(2026, 1, 1, 0, 0);
		LocalDateTime newer = LocalDateTime.of(2026, 1, 5, 0, 0);
		userLessonRepository.save(completedUserLesson(learner, completedLesson, older, older.plusDays(1)));
		userLessonRepository.save(incompleteUserLesson(learner, incompleteLesson, newer));
		userLessonRepository.save(incompleteUserLesson(learner, privateLesson, newer));

		// When:
		Page<MyLessonSummaryProjection> result =
				userLessonRepository.findMyLessonsPage(learner.getId(), PageRequest.of(0, 20));

		// Then: private lesson excluded; incomplete lesson (any enrolledAt) sorts before completed
		assertThat(result.getContent()).extracting(MyLessonSummaryProjection::lessonId)
				.containsExactly(incompleteLesson.getId(), completedLesson.getId());
		assertThat(result.getTotalElements()).isEqualTo(2);

		MyLessonSummaryProjection completedSummary = result.getContent().stream()
				.filter(s -> s.lessonId().equals(completedLesson.getId()))
				.findFirst().orElseThrow();
		assertThat(completedSummary.contentHtmlPreview()).hasSize(500);
		assertThat(completedSummary.contentMarkdownPreview()).hasSize(500);
		assertThat(completedSummary.completedAt()).isEqualTo(older.plusDays(1));
	}

	@Test
	void findMyLessonsPageShouldRespectPageSizeAndPageIndexTest() {
		// Given: three published enrollments
		UserRole role = userRoleRepository.findByCode(UserRoleCode.MEMBER).orElseThrow();
		User learner = userRepository.save(user(role, "Learner2", "learner2@test.com"));
		for (int i = 0; i < 3; i++) {
			Lesson lesson = lessonRepository.save(
					lesson("Lesson " + i, LessonPublicationStatusCode.PUBLISHED, "<p>c</p>", "c"));
			userLessonRepository.save(incompleteUserLesson(learner, lesson, LocalDateTime.of(2026, 1, 1 + i, 0, 0)));
		}

		// When: requesting a page of size 2
		Page<MyLessonSummaryProjection> firstPage =
				userLessonRepository.findMyLessonsPage(learner.getId(), PageRequest.of(0, 2));

		// Then:
		assertThat(firstPage.getContent()).hasSize(2);
		assertThat(firstPage.getTotalElements()).isEqualTo(3);
		assertThat(firstPage.hasNext()).isTrue();
	}

	@Test
	void findIdsWithTeacherVideoInShouldReturnOnlyLessonsWithATeacherVideoUrlTest() {
		// Given:
		Lesson withVideo = lessonRepository.save(lessonWithGenerationMetadata(
				"With Video", Map.of("teacherVideo", Map.of("videoUrl", "https://example.com/video.mp4"))));
		Lesson withoutVideo = lessonRepository.save(lessonWithGenerationMetadata("Without Video", Map.of()));
		Lesson withEmptyTeacherVideoObject = lessonRepository.save(
				lessonWithGenerationMetadata("Empty Teacher Video", Map.of("teacherVideo", Map.of())));

		// When:
		var result = lessonRepository.findIdsWithTeacherVideoIn(
				List.of(withVideo.getId(), withoutVideo.getId(), withEmptyTeacherVideoObject.getId()));

		// Then:
		assertThat(result).containsExactly(withVideo.getId());
	}

	@Test
	void countByLessonIdsGroupedByTypeShouldCountActivitiesPerLessonAndTypeTest() {
		// Given: lessonA has 2 flashcard activities and 1 quiz; lessonB has 1 quiz only
		ActivityType flashcards = activityTypeRepository.findByCode(ActivityTypeCode.FLASHCARDS).orElseThrow();
		ActivityType quiz = activityTypeRepository.findByCode(ActivityTypeCode.QUIZ).orElseThrow();
		Lesson lessonA = lessonRepository.save(lesson("Lesson A", LessonPublicationStatusCode.PUBLISHED, "<p>a</p>",
				"a"));
		Lesson lessonB = lessonRepository.save(lesson("Lesson B", LessonPublicationStatusCode.PUBLISHED, "<p>b</p>",
				"b"));
		lessonActivityRepository.save(activity(lessonA, flashcards, "Set 1"));
		lessonActivityRepository.save(activity(lessonA, flashcards, "Set 2"));
		lessonActivityRepository.save(activity(lessonA, quiz, "Quiz A"));
		lessonActivityRepository.save(activity(lessonB, quiz, "Quiz B"));

		// When:
		List<LessonActivityTypeCountProjection> result = lessonActivityRepository.countByLessonIdsGroupedByType(
				List.of(lessonA.getId(), lessonB.getId()));

		// Then:
		assertThat(result)
				.extracting(
						LessonActivityTypeCountProjection::getLessonId,
						LessonActivityTypeCountProjection::getTypeCode,
						LessonActivityTypeCountProjection::getActivityCount
				)
				.containsExactlyInAnyOrder(
						org.assertj.core.groups.Tuple.tuple(lessonA.getId(), ActivityTypeCode.FLASHCARDS, 2L),
						org.assertj.core.groups.Tuple.tuple(lessonA.getId(), ActivityTypeCode.QUIZ, 1L),
						org.assertj.core.groups.Tuple.tuple(lessonB.getId(), ActivityTypeCode.QUIZ, 1L)
				);
	}

	private User user(UserRole role, String name, String email) {
		User user = new User();
		user.setName(name);
		user.setEmail(email);
		user.setRole(role);
		LocalDateTime now = LocalDateTime.of(2026, 1, 1, 0, 0);
		user.setCreatedAt(now);
		user.setUpdatedAt(now);
		return user;
	}

	private Lesson lesson(String title, String publicationStatusCode, String contentHtml, String contentMarkdown) {
		LessonStatus status = lessonStatusRepository.findByCode(LessonStatusCode.READY).orElseThrow();
		LessonPublicationStatus publicationStatus =
				lessonPublicationStatusRepository.findByCode(publicationStatusCode).orElseThrow();
		LessonContentFormat contentFormat =
				lessonContentFormatRepository.findByCode(LessonContentFormatCode.MARKDOWN).orElseThrow();
		LocalDateTime now = LocalDateTime.of(2026, 1, 1, 0, 0);

		Lesson lesson = new Lesson();
		lesson.setTitle(title);
		lesson.setDescription("description");
		lesson.setStatus(status);
		lesson.setUserInstructions("");
		lesson.setDepth("standard");
		lesson.setTone("clear");
		lesson.setDesiredFormat("structured theoretical lesson");
		lesson.setContentFormat(contentFormat);
		lesson.setContentMarkdown(contentMarkdown);
		lesson.setContentHtml(contentHtml);
		lesson.setCoverImageStorageKey("");
		lesson.setCoverImageOriginalName("");
		lesson.setCoverImageMimeType("");
		lesson.setGenerationMetadata(Map.of());
		lesson.setRevisionHistory(List.of());
		lesson.setErrorMessage("");
		lesson.setPublicationStatus(publicationStatus);
		lesson.setCreatedBy("tester");
		lesson.setTags(List.of("tag1"));
		lesson.setCreatedAt(now);
		lesson.setUpdatedAt(now);
		return lesson;
	}

	private Lesson lessonWithGenerationMetadata(String title, Map<String, Object> generationMetadata) {
		Lesson lesson = lesson(title, LessonPublicationStatusCode.PUBLISHED, "<p>c</p>", "c");
		lesson.setGenerationMetadata(generationMetadata);
		return lesson;
	}

	private UserLesson incompleteUserLesson(User user, Lesson lesson, LocalDateTime enrolledAt) {
		UserLesson userLesson = new UserLesson();
		UserLesson.UserLessonId id = new UserLesson.UserLessonId();
		id.setUserId(user.getId());
		id.setLessonId(lesson.getId());
		userLesson.setId(id);
		userLesson.setUser(user);
		userLesson.setLesson(lesson);
		userLesson.setEnrolledAt(enrolledAt);
		return userLesson;
	}

	private UserLesson completedUserLesson(User user, Lesson lesson, LocalDateTime enrolledAt,
										   LocalDateTime completedAt) {
		UserLesson userLesson = incompleteUserLesson(user, lesson, enrolledAt);
		userLesson.setCompletedAt(completedAt);
		return userLesson;
	}

	private LessonActivity activity(Lesson lesson, ActivityType type, String title) {
		LessonActivity activity = new LessonActivity();
		activity.setLesson(lesson);
		activity.setType(type);
		activity.setTitle(title);
		activity.setItemCount(1);
		activity.setPayload(Map.of());
		activity.setGenerationMetadata(Map.of());
		activity.setCreatedBy("tester");
		activity.setCreatedAt(LocalDateTime.of(2026, 1, 1, 0, 0));
		return activity;
	}
}
