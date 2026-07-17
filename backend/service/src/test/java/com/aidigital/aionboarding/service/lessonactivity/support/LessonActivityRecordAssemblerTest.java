package com.aidigital.aionboarding.service.lessonactivity.support;

import com.aidigital.aionboarding.domain.common.dictionary.ActivityProgressStatusCode;
import com.aidigital.aionboarding.domain.common.dictionary.ActivityTypeCode;
import com.aidigital.aionboarding.domain.common.dictionary.LessonPublicationStatusCode;
import com.aidigital.aionboarding.domain.common.dictionary.LessonStatusCode;
import com.aidigital.aionboarding.domain.common.dictionary.entities.ActivityProgressStatus;
import com.aidigital.aionboarding.domain.common.dictionary.entities.ActivityType;
import com.aidigital.aionboarding.domain.common.dictionary.entities.LessonPublicationStatus;
import com.aidigital.aionboarding.domain.common.dictionary.entities.LessonStatus;
import com.aidigital.aionboarding.domain.learning.entities.UserLesson;
import com.aidigital.aionboarding.domain.lesson.entities.Lesson;
import com.aidigital.aionboarding.domain.lessonactivity.entities.LessonActivity;
import com.aidigital.aionboarding.domain.lessonactivity.entities.UserLessonActivityAttempt;
import com.aidigital.aionboarding.domain.lessonactivity.entities.UserLessonActivityProgress;
import com.aidigital.aionboarding.domain.user.entities.User;
import com.aidigital.aionboarding.service.learning.models.LessonEnrollmentRecord;
import com.aidigital.aionboarding.service.lessonactivity.models.ActivityAttemptRecord;
import com.aidigital.aionboarding.service.lessonactivity.models.ActivityProgressRecord;
import com.aidigital.aionboarding.service.lessonactivity.models.ActivityProgressViewRecord;
import com.aidigital.aionboarding.service.lessonactivity.models.ActivityPromptRecord;
import com.aidigital.aionboarding.service.lessonactivity.models.LessonActivityRecord;
import com.aidigital.aionboarding.service.lessonactivity.models.LessonWithActivitiesRecord;
import com.aidigital.aionboarding.service.lessonactivity.models.QuizAnswerResultRecord;
import com.aidigital.aionboarding.service.lessongen.model.LessonGenPrompt;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class LessonActivityRecordAssemblerTest {

	private final LessonActivityRecordAssembler assembler = new LessonActivityRecordAssembler();

	private ActivityType activityType(String code) {
		ActivityType type = new ActivityType();
		type.setCode(code);
		return type;
	}

	private ActivityProgressStatus progressStatus(String code) {
		ActivityProgressStatus status = new ActivityProgressStatus();
		status.setCode(code);
		return status;
	}

	private LessonStatus lessonStatus(String code) {
		LessonStatus status = new LessonStatus();
		status.setCode(code);
		return status;
	}

	private LessonPublicationStatus publicationStatus(String code) {
		LessonPublicationStatus status = new LessonPublicationStatus();
		status.setCode(code);
		return status;
	}

	private Lesson lessonWithId(Long id) {
		Lesson lesson = new Lesson();
		lesson.setId(id);
		return lesson;
	}

	private User userWithId(Long id) {
		User user = new User();
		user.setId(id);
		return user;
	}

	@Nested
	class ToActivityRecord {

		@Test
		void shouldMapAllActivityFieldsAndNullProgressWhenProgressIsNullTest() {
			// Given: a fully populated activity and no progress record
			LessonActivity activity = new LessonActivity();
			activity.setId(1L);
			activity.setLesson(lessonWithId(10L));
			activity.setType(activityType(ActivityTypeCode.QUIZ));
			activity.setTitle("Quiz title");
			activity.setItemCount(5);
			activity.setPayload(Map.of("items", List.of()));
			activity.setGenerationMetadata(Map.of("model", "gpt-5"));
			activity.setCreatedBy("author-1");
			LocalDateTime createdAt = LocalDateTime.of(2026, 1, 1, 10, 0);
			activity.setCreatedAt(createdAt);

			// When:
			LessonActivityRecord result = assembler.toActivityRecord(activity, null);

			// Then:
			assertThat(result.id()).isEqualTo(1L);
			assertThat(result.lessonId()).isEqualTo(10L);
			assertThat(result.type()).isEqualTo(ActivityTypeCode.QUIZ);
			assertThat(result.title()).isEqualTo("Quiz title");
			assertThat(result.itemCount()).isEqualTo(5);
			assertThat(result.payload()).isEqualTo(Map.of("items", List.of()));
			assertThat(result.generationMetadata()).isEqualTo(Map.of("model", "gpt-5"));
			assertThat(result.createdBy()).isEqualTo("author-1");
			assertThat(result.createdAt()).isEqualTo(createdAt);
			assertThat(result.progress()).isNull();
		}

		@Test
		void shouldIncludeProgressViewRecordWhenProgressIsProvidedTest() {
			// Given: an activity plus a completed progress record
			LessonActivity activity = new LessonActivity();
			activity.setId(2L);
			activity.setLesson(lessonWithId(20L));
			activity.setType(activityType(ActivityTypeCode.FLASHCARDS));
			activity.setTitle("Flashcards title");
			activity.setItemCount(3);
			activity.setPayload(Map.of());
			activity.setGenerationMetadata(Map.of());
			activity.setCreatedBy("author-2");
			activity.setCreatedAt(LocalDateTime.of(2026, 2, 1, 9, 0));

			UserLessonActivityProgress progress = new UserLessonActivityProgress();
			progress.setStatus(progressStatus(ActivityProgressStatusCode.COMPLETED));
			progress.setScore(new BigDecimal("85"));
			progress.setMetadata(Map.of("attempts", 1));
			LocalDateTime startedAt = LocalDateTime.of(2026, 2, 1, 9, 5);
			LocalDateTime completedAt = LocalDateTime.of(2026, 2, 1, 9, 10);
			progress.setStartedAt(startedAt);
			progress.setCompletedAt(completedAt);

			// When:
			LessonActivityRecord result = assembler.toActivityRecord(activity, progress);

			// Then:
			assertThat(result.progress()).isEqualTo(
					new ActivityProgressViewRecord(
							ActivityProgressStatusCode.COMPLETED, 85, Map.of("attempts", 1), startedAt, completedAt,
							true));
		}
	}

	@Nested
	class ToProgressViewRecord {

		@Test
		void shouldMapNullScoreAndNotCompletedWhenScoreAndCompletedAtAreNullTest() {
			// Given: progress that has not been completed and carries no score
			UserLessonActivityProgress progress = new UserLessonActivityProgress();
			progress.setStatus(progressStatus(ActivityProgressStatusCode.IN_PROGRESS));
			progress.setScore(null);
			progress.setMetadata(Map.of());
			progress.setStartedAt(LocalDateTime.of(2026, 3, 1, 8, 0));
			progress.setCompletedAt(null);

			// When:
			ActivityProgressViewRecord result = assembler.toProgressViewRecord(progress);

			// Then:
			assertThat(result.status()).isEqualTo(ActivityProgressStatusCode.IN_PROGRESS);
			assertThat(result.score()).isNull();
			assertThat(result.completedAt()).isNull();
			assertThat(result.isCompleted()).isFalse();
		}

		@Test
		void shouldRoundScoreUpHalfUpAndMarkCompletedWhenCompletedAtIsPresentTest() {
			// Given: a score exactly on the HALF_UP rounding boundary
			UserLessonActivityProgress progress = new UserLessonActivityProgress();
			progress.setStatus(progressStatus(ActivityProgressStatusCode.COMPLETED));
			progress.setScore(new BigDecimal("79.5"));
			progress.setMetadata(Map.of());
			progress.setStartedAt(LocalDateTime.of(2026, 3, 1, 8, 0));
			progress.setCompletedAt(LocalDateTime.of(2026, 3, 1, 8, 30));

			// When:
			ActivityProgressViewRecord result = assembler.toProgressViewRecord(progress);

			// Then:
			assertThat(result.score()).isEqualTo(80);
			assertThat(result.isCompleted()).isTrue();
		}

		@Test
		void shouldRoundScoreDownWhenFractionIsBelowHalfTest() {
			// Given: a score just below the HALF_UP rounding boundary
			UserLessonActivityProgress progress = new UserLessonActivityProgress();
			progress.setStatus(progressStatus(ActivityProgressStatusCode.COMPLETED));
			progress.setScore(new BigDecimal("79.49"));
			progress.setMetadata(Map.of());
			progress.setStartedAt(LocalDateTime.of(2026, 3, 1, 8, 0));
			progress.setCompletedAt(LocalDateTime.of(2026, 3, 1, 8, 30));

			// When:
			ActivityProgressViewRecord result = assembler.toProgressViewRecord(progress);

			// Then:
			assertThat(result.score()).isEqualTo(79);
		}
	}

	@Nested
	class ToProgressRecord {

		@Test
		void shouldMapNullScoreTest() {
			// Given:
			UserLessonActivityProgress progress = new UserLessonActivityProgress();
			progress.setActivity(activityWithId(11L));
			progress.setLesson(lessonWithId(21L));
			progress.setStatus(progressStatus(ActivityProgressStatusCode.NOT_STARTED));
			progress.setScore(null);
			progress.setCompletedAt(null);
			progress.setMetadata(Map.of());

			// When:
			ActivityProgressRecord result = assembler.toProgressRecord(progress);

			// Then:
			assertThat(result.activityId()).isEqualTo(11L);
			assertThat(result.lessonId()).isEqualTo(21L);
			assertThat(result.status()).isEqualTo(ActivityProgressStatusCode.NOT_STARTED);
			assertThat(result.score()).isNull();
			assertThat(result.completedAt()).isNull();
		}

		@Test
		void shouldRoundNonNullScoreTest() {
			// Given:
			UserLessonActivityProgress progress = new UserLessonActivityProgress();
			progress.setActivity(activityWithId(12L));
			progress.setLesson(lessonWithId(22L));
			progress.setStatus(progressStatus(ActivityProgressStatusCode.COMPLETED));
			progress.setScore(new BigDecimal("90.6"));
			LocalDateTime completedAt = LocalDateTime.of(2026, 4, 1, 12, 0);
			progress.setCompletedAt(completedAt);
			progress.setMetadata(Map.of("tries", 2));

			// When:
			ActivityProgressRecord result = assembler.toProgressRecord(progress);

			// Then:
			assertThat(result.score()).isEqualTo(91);
			assertThat(result.completedAt()).isEqualTo(completedAt);
			assertThat(result.metadata()).isEqualTo(Map.of("tries", 2));
		}

		private LessonActivity activityWithId(Long id) {
			LessonActivity activity = new LessonActivity();
			activity.setId(id);
			return activity;
		}
	}

	@Nested
	class ToAttemptRecordWithExplicitGradingArgs {

		@Test
		void shouldUseSuppliedGradingArgumentsRatherThanEntityOwnFieldsTest() {
			// Given: the entity carries different score/passed/count values than the explicit
			// grading arguments, proving the overload trusts the arguments, not the entity
			UserLessonActivityAttempt attemptEntity = new UserLessonActivityAttempt();
			attemptEntity.setId(30L);
			attemptEntity.setUser(userWithId(3L));
			attemptEntity.setLesson(lessonWithId(31L));
			attemptEntity.setActivity(new LessonActivity());
			attemptEntity.getActivity().setId(32L);
			attemptEntity.setType(activityType(ActivityTypeCode.QUIZ));
			attemptEntity.setAttemptNumber(2);
			attemptEntity.setScore(new BigDecimal("10"));
			attemptEntity.setPassed(false);
			attemptEntity.setCorrectCount(1);
			attemptEntity.setTotalCount(1);
			attemptEntity.setMetadata(Map.of("source", "entity"));
			LocalDateTime createdAt = LocalDateTime.of(2026, 5, 1, 14, 0);
			attemptEntity.setCreatedAt(createdAt);

			List<QuizAnswerResultRecord> results = List.of(
					new QuizAnswerResultRecord("single_choice", "Q1", List.of("A", "B"), List.of("A"), List.of("A"),
							true, "Because"));
			List<List<String>> submittedAnswers = List.of(List.of("A"));

			// When:
			ActivityAttemptRecord result = assembler.toAttemptRecord(
					attemptEntity, 95, true, 4, 5, results, submittedAnswers);

			// Then: identifying fields come from the entity, grading fields come from the arguments
			assertThat(result.id()).isEqualTo(30L);
			assertThat(result.userId()).isEqualTo(3L);
			assertThat(result.lessonId()).isEqualTo(31L);
			assertThat(result.activityId()).isEqualTo(32L);
			assertThat(result.type()).isEqualTo(ActivityTypeCode.QUIZ);
			assertThat(result.attemptNumber()).isEqualTo(2);
			assertThat(result.score()).isEqualTo(95);
			assertThat(result.passed()).isTrue();
			assertThat(result.correctCount()).isEqualTo(4);
			assertThat(result.totalCount()).isEqualTo(5);
			assertThat(result.submittedAnswers()).isEqualTo(submittedAnswers);
			assertThat(result.results()).isEqualTo(results);
			assertThat(result.metadata()).isEqualTo(Map.of("source", "entity"));
			assertThat(result.createdAt()).isEqualTo(createdAt);
		}
	}

	@Nested
	class ToAttemptRecordFromEntity {

		@Test
		void shouldDefaultScoreToZeroAndPassedToFalseWhenBothAreNullTest() {
			// Given: an attempt persisted before scoring completed
			UserLessonActivityAttempt attempt = new UserLessonActivityAttempt();
			attempt.setId(40L);
			attempt.setUser(userWithId(4L));
			attempt.setLesson(lessonWithId(41L));
			attempt.setActivity(new LessonActivity());
			attempt.getActivity().setId(42L);
			attempt.setType(activityType(ActivityTypeCode.QUIZ));
			attempt.setAttemptNumber(1);
			attempt.setScore(null);
			attempt.setPassed(null);
			attempt.setCorrectCount(0);
			attempt.setTotalCount(5);
			attempt.setSubmittedAnswers(null);
			attempt.setResults(null);
			attempt.setMetadata(Map.of());
			attempt.setCreatedAt(LocalDateTime.of(2026, 6, 1, 10, 0));

			// When:
			ActivityAttemptRecord result = assembler.toAttemptRecord(attempt);

			// Then:
			assertThat(result.score()).isZero();
			assertThat(result.passed()).isFalse();
			assertThat(result.submittedAnswers()).isEmpty();
			assertThat(result.results()).isEmpty();
		}

		@Test
		void shouldRoundScoreAndMapPassedTrueTest() {
			// Given:
			UserLessonActivityAttempt attempt = new UserLessonActivityAttempt();
			attempt.setId(43L);
			attempt.setUser(userWithId(5L));
			attempt.setLesson(lessonWithId(44L));
			attempt.setActivity(new LessonActivity());
			attempt.getActivity().setId(45L);
			attempt.setType(activityType(ActivityTypeCode.QUIZ));
			attempt.setAttemptNumber(3);
			attempt.setScore(new BigDecimal("87.6"));
			attempt.setPassed(Boolean.TRUE);
			attempt.setCorrectCount(4);
			attempt.setTotalCount(5);
			attempt.setSubmittedAnswers(List.of());
			attempt.setResults(List.of());
			attempt.setMetadata(Map.of());
			attempt.setCreatedAt(LocalDateTime.of(2026, 6, 2, 10, 0));

			// When:
			ActivityAttemptRecord result = assembler.toAttemptRecord(attempt);

			// Then:
			assertThat(result.score()).isEqualTo(88);
			assertThat(result.passed()).isTrue();
		}

		@Test
		void shouldMapPassedFalseWhenExplicitlyFalseTest() {
			// Given:
			UserLessonActivityAttempt attempt = new UserLessonActivityAttempt();
			attempt.setId(46L);
			attempt.setUser(userWithId(6L));
			attempt.setLesson(lessonWithId(47L));
			attempt.setActivity(new LessonActivity());
			attempt.getActivity().setId(48L);
			attempt.setType(activityType(ActivityTypeCode.QUIZ));
			attempt.setAttemptNumber(1);
			attempt.setScore(new BigDecimal("30"));
			attempt.setPassed(Boolean.FALSE);
			attempt.setCorrectCount(1);
			attempt.setTotalCount(5);
			attempt.setSubmittedAnswers(List.of());
			attempt.setResults(List.of());
			attempt.setMetadata(Map.of());
			attempt.setCreatedAt(LocalDateTime.of(2026, 6, 3, 10, 0));

			// When:
			ActivityAttemptRecord result = assembler.toAttemptRecord(attempt);

			// Then:
			assertThat(result.passed()).isFalse();
		}

		@Test
		void shouldConvertSubmittedAnswersAndResultsThroughTheHelpersTest() {
			// Given: raw JSONB-shaped values, mixing well-formed and malformed elements
			UserLessonActivityAttempt attempt = new UserLessonActivityAttempt();
			attempt.setId(49L);
			attempt.setUser(userWithId(7L));
			attempt.setLesson(lessonWithId(50L));
			attempt.setActivity(new LessonActivity());
			attempt.getActivity().setId(51L);
			attempt.setType(activityType(ActivityTypeCode.QUIZ));
			attempt.setAttemptNumber(1);
			attempt.setScore(new BigDecimal("100"));
			attempt.setPassed(Boolean.TRUE);
			attempt.setCorrectCount(2);
			attempt.setTotalCount(2);
			attempt.setSubmittedAnswers(List.of(List.of("Paris"), "not-a-list"));

			Map<String, Object> resultMap = new LinkedHashMap<>();
			resultMap.put("type", "single_choice");
			resultMap.put("question", "What is the capital of France?");
			resultMap.put("options", List.of("Paris", "Berlin"));
			resultMap.put("selectedAnswers", List.of("Paris"));
			resultMap.put("correctAnswers", List.of("Paris"));
			resultMap.put("isCorrect", true);
			resultMap.put("explanation", "Paris is the capital.");
			attempt.setResults(List.of(resultMap, "not-a-map"));
			attempt.setMetadata(Map.of());
			attempt.setCreatedAt(LocalDateTime.of(2026, 6, 4, 10, 0));

			// When:
			ActivityAttemptRecord result = assembler.toAttemptRecord(attempt);

			// Then: malformed elements are dropped, well-formed ones are converted
			assertThat(result.submittedAnswers()).containsExactly(List.of("Paris"), List.of());
			assertThat(result.results()).containsExactly(
					new QuizAnswerResultRecord(
							"single_choice", "What is the capital of France?",
							List.of("Paris", "Berlin"), List.of("Paris"), List.of("Paris"), true, "Paris is the " +
							"capital."));
		}
	}

	@Nested
	class ToPromptRecord {

		@Test
		void shouldMapAllFieldsFromLessonGenPromptTest() {
			// Given:
			LessonGenPrompt prompt = new LessonGenPrompt("v1", "cache-key", "instructions text", "input text");

			// When:
			ActivityPromptRecord result = assembler.toPromptRecord(prompt);

			// Then:
			assertThat(result).isEqualTo(new ActivityPromptRecord("v1", "cache-key", "instructions text", "input " +
					"text"));
		}
	}

	@Nested
	class ToLessonWithActivitiesRecord {

		@Test
		void shouldMapLessonFieldsAndActivitiesListTest() {
			// Given:
			Lesson lesson = new Lesson();
			lesson.setId(60L);
			lesson.setTitle("Onboarding basics");
			lesson.setDescription("An intro lesson");
			lesson.setStatus(lessonStatus(LessonStatusCode.READY));
			lesson.setPublicationStatus(publicationStatus(LessonPublicationStatusCode.PUBLISHED));
			lesson.setContentMarkdown("# Welcome");
			lesson.setContentHtml("<h1>Welcome</h1>");

			LessonActivityRecord activityRecord = new LessonActivityRecord(
					1L, 60L, ActivityTypeCode.QUIZ, "Quiz", 1, Map.of(), Map.of(), "author", null, null);

			// When:
			LessonWithActivitiesRecord result = assembler.toLessonWithActivitiesRecord(lesson,
					List.of(activityRecord));

			// Then:
			assertThat(result.id()).isEqualTo(60L);
			assertThat(result.title()).isEqualTo("Onboarding basics");
			assertThat(result.description()).isEqualTo("An intro lesson");
			assertThat(result.status()).isEqualTo(LessonStatusCode.READY);
			assertThat(result.publicationStatus()).isEqualTo(LessonPublicationStatusCode.PUBLISHED);
			assertThat(result.contentMarkdown()).isEqualTo("# Welcome");
			assertThat(result.contentHtml()).isEqualTo("<h1>Welcome</h1>");
			assertThat(result.activities()).containsExactly(activityRecord);
		}

		@Test
		void shouldPreserveEmptyActivitiesListTest() {
			// Given:
			Lesson lesson = new Lesson();
			lesson.setId(61L);
			lesson.setTitle("Empty lesson");
			lesson.setDescription("No activities yet");
			lesson.setStatus(lessonStatus(LessonStatusCode.DRAFT));
			lesson.setPublicationStatus(publicationStatus(LessonPublicationStatusCode.PRIVATE));
			lesson.setContentMarkdown("");
			lesson.setContentHtml("");

			// When:
			LessonWithActivitiesRecord result = assembler.toLessonWithActivitiesRecord(lesson, List.of());

			// Then:
			assertThat(result.activities()).isEmpty();
		}
	}

	@Nested
	class ToEnrollmentRecord {

		@Test
		void shouldReturnNullWhenEnrollmentIsNullTest() {
			// When:
			LessonEnrollmentRecord result = assembler.toEnrollmentRecord(null);

			// Then:
			assertThat(result).isNull();
		}

		@Test
		void shouldMapEnrollmentAsNotCompletedWhenCompletedAtIsNullTest() {
			// Given:
			UserLesson enrollment = new UserLesson();
			enrollment.setLesson(lessonWithId(70L));
			LocalDateTime enrolledAt = LocalDateTime.of(2026, 1, 10, 9, 0);
			enrollment.setEnrolledAt(enrolledAt);
			enrollment.setCompletedAt(null);

			// When:
			LessonEnrollmentRecord result = assembler.toEnrollmentRecord(enrollment);

			// Then:
			assertThat(result.lessonId()).isEqualTo(70L);
			assertThat(result.enrolledAt()).isEqualTo(enrolledAt);
			assertThat(result.completedAt()).isNull();
			assertThat(result.isCompleted()).isFalse();
		}

		@Test
		void shouldMapEnrollmentAsCompletedWhenCompletedAtIsPresentTest() {
			// Given:
			UserLesson enrollment = new UserLesson();
			enrollment.setLesson(lessonWithId(71L));
			enrollment.setEnrolledAt(LocalDateTime.of(2026, 1, 10, 9, 0));
			LocalDateTime completedAt = LocalDateTime.of(2026, 1, 12, 9, 0);
			enrollment.setCompletedAt(completedAt);

			// When:
			LessonEnrollmentRecord result = assembler.toEnrollmentRecord(enrollment);

			// Then:
			assertThat(result.completedAt()).isEqualTo(completedAt);
			assertThat(result.isCompleted()).isTrue();
		}
	}

	@Nested
	class ToQuizAnswerResults {

		@Test
		void shouldReturnEmptyListWhenResultsIsNullTest() {
			// When:
			List<QuizAnswerResultRecord> result = assembler.toQuizAnswerResults(null);

			// Then:
			assertThat(result).isEmpty();
		}

		@Test
		void shouldReturnEmptyListWhenResultsIsEmptyTest() {
			// When:
			List<QuizAnswerResultRecord> result = assembler.toQuizAnswerResults(List.of());

			// Then:
			assertThat(result).isEmpty();
		}

		@Test
		void shouldFilterOutNonMapElementsAndMapRemainingOnesTest() {
			// Given: a mix of a well-formed Map result and a malformed non-Map entry
			Map<String, Object> mapResult = new LinkedHashMap<>();
			mapResult.put("type", "true_false");
			mapResult.put("question", "Is the sky blue?");
			mapResult.put("options", List.of("True", "False"));
			mapResult.put("selectedAnswers", List.of("True"));
			mapResult.put("correctAnswers", List.of("True"));
			mapResult.put("isCorrect", true);
			mapResult.put("explanation", "Yes.");
			List<Object> rawResults = Arrays.asList(mapResult, "not-a-map", 42);

			// When:
			List<QuizAnswerResultRecord> result = assembler.toQuizAnswerResults(rawResults);

			// Then:
			assertThat(result).containsExactly(
					new QuizAnswerResultRecord(
							"true_false", "Is the sky blue?", List.of("True", "False"), List.of("True"), List.of("True"
					), true, "Yes."));
		}
	}

	@Nested
	class ToQuizAnswerResult {

		@Test
		void shouldPreserveKnownQuestionTypesTest() {
			// Given-When-Then: each recognized type value round-trips unchanged
			assertThat(assembler.toQuizAnswerResult(resultMapWithType("single_choice")).type()).isEqualTo(
					"single_choice");
			assertThat(assembler.toQuizAnswerResult(resultMapWithType("multiple_choice")).type()).isEqualTo(
					"multiple_choice");
			assertThat(assembler.toQuizAnswerResult(resultMapWithType("true_false")).type()).isEqualTo("true_false");
			assertThat(assembler.toQuizAnswerResult(resultMapWithType("fill_in_blanks_with_options")).type())
					.isEqualTo("fill_in_blanks_with_options");
		}

		@Test
		void shouldDefaultToMultipleChoiceWhenTypeIsUnrecognizedOrMissingTest() {
			// Given: an unrecognized type value and a map missing the type key entirely
			Map<String, Object> unrecognized = resultMapWithType("essay");
			Map<String, Object> missing = new LinkedHashMap<>();

			// When-Then:
			assertThat(assembler.toQuizAnswerResult(unrecognized).type()).isEqualTo("multiple_choice");
			assertThat(assembler.toQuizAnswerResult(missing).type()).isEqualTo("multiple_choice");
		}

		@Test
		void shouldMapIsCorrectTrueWhenFlagIsExplicitlyTrueTest() {
			// Given:
			Map<String, Object> map = resultMapWithType("single_choice");
			map.put("isCorrect", true);

			// When-Then:
			assertThat(assembler.toQuizAnswerResult(map).isCorrect()).isTrue();
		}

		@Test
		void shouldMapIsCorrectFalseWhenFlagIsMissingOrFalseTest() {
			// Given:
			Map<String, Object> missingFlag = new LinkedHashMap<>();
			Map<String, Object> falseFlag = resultMapWithType("single_choice");
			falseFlag.put("isCorrect", false);

			// When-Then:
			assertThat(assembler.toQuizAnswerResult(missingFlag).isCorrect()).isFalse();
			assertThat(assembler.toQuizAnswerResult(falseFlag).isCorrect()).isFalse();
		}

		@Test
		void shouldDefaultToEmptyStringsAndListsWhenOptionalFieldsAreMissingTest() {
			// Given: only the type key is present
			Map<String, Object> map = new LinkedHashMap<>();
			map.put("type", "single_choice");

			// When:
			QuizAnswerResultRecord result = assembler.toQuizAnswerResult(map);

			// Then:
			assertThat(result.question()).isEmpty();
			assertThat(result.explanation()).isEmpty();
			assertThat(result.options()).isEmpty();
			assertThat(result.selectedAnswers()).isEmpty();
			assertThat(result.correctAnswers()).isEmpty();
		}

		private Map<String, Object> resultMapWithType(String type) {
			Map<String, Object> map = new LinkedHashMap<>();
			map.put("type", type);
			map.put("question", "Question text");
			map.put("options", List.of("A", "B"));
			map.put("selectedAnswers", List.of("A"));
			map.put("correctAnswers", List.of("A"));
			map.put("isCorrect", true);
			map.put("explanation", "Explanation text");
			return map;
		}
	}

	@Nested
	class ToQuizAnswerResultMap {

		@Test
		void shouldProduceMapWithAllRecordFieldsTest() {
			// Given:
			QuizAnswerResultRecord record = new QuizAnswerResultRecord(
					"multiple_choice", "Which are primary colors?",
					List.of("Red", "Blue", "Green"), List.of("Red", "Blue"), List.of("Red", "Blue"), true, "Correct.");

			// When:
			Map<String, Object> result = assembler.toQuizAnswerResultMap(record);

			// Then:
			assertThat(result)
					.containsEntry("type", "multiple_choice")
					.containsEntry("question", "Which are primary colors?")
					.containsEntry("options", List.of("Red", "Blue", "Green"))
					.containsEntry("selectedAnswers", List.of("Red", "Blue"))
					.containsEntry("correctAnswers", List.of("Red", "Blue"))
					.containsEntry("isCorrect", true)
					.containsEntry("explanation", "Correct.");
		}
	}

	@Nested
	class IsActivityPassed {

		@Test
		void shouldReturnFalseWhenProgressIsNullTest() {
			// Given:
			LessonActivityRecord activity = new LessonActivityRecord(
					1L, 10L, ActivityTypeCode.QUIZ, "Quiz", 1, Map.of(), Map.of(), "author", null, null);

			// When-Then:
			assertThat(assembler.isActivityPassed(activity)).isFalse();
		}

		@Test
		void shouldReturnFalseWhenProgressIsNotCompletedTest() {
			// Given:
			ActivityProgressViewRecord progress = new ActivityProgressViewRecord(
					ActivityProgressStatusCode.IN_PROGRESS, 90, Map.of(), LocalDateTime.now(), null, false);
			LessonActivityRecord activity = new LessonActivityRecord(
					1L, 10L, ActivityTypeCode.QUIZ, "Quiz", 1, Map.of(), Map.of(), "author", null, progress);

			// When-Then:
			assertThat(assembler.isActivityPassed(activity)).isFalse();
		}

		@Test
		void shouldReturnTrueForQuizWithScoreAtOrAboveEightyTest() {
			// Given: a quiz completed with the exact passing threshold score
			ActivityProgressViewRecord progress = new ActivityProgressViewRecord(
					ActivityProgressStatusCode.COMPLETED, 80, Map.of(), LocalDateTime.now(), LocalDateTime.now(),
					true);
			LessonActivityRecord activity = new LessonActivityRecord(
					1L, 10L, ActivityTypeCode.QUIZ, "Quiz", 1, Map.of(), Map.of(), "author", null, progress);

			// When-Then:
			assertThat(assembler.isActivityPassed(activity)).isTrue();
		}

		@Test
		void shouldReturnFalseForQuizWithScoreBelowEightyTest() {
			// Given:
			ActivityProgressViewRecord progress = new ActivityProgressViewRecord(
					ActivityProgressStatusCode.COMPLETED, 79, Map.of(), LocalDateTime.now(), LocalDateTime.now(),
					true);
			LessonActivityRecord activity = new LessonActivityRecord(
					1L, 10L, ActivityTypeCode.QUIZ, "Quiz", 1, Map.of(), Map.of(), "author", null, progress);

			// When-Then:
			assertThat(assembler.isActivityPassed(activity)).isFalse();
		}

		@Test
		void shouldReturnFalseForCompletedQuizWithNullScoreTest() {
			// Given:
			ActivityProgressViewRecord progress = new ActivityProgressViewRecord(
					ActivityProgressStatusCode.COMPLETED, null, Map.of(), LocalDateTime.now(), LocalDateTime.now(),
					true);
			LessonActivityRecord activity = new LessonActivityRecord(
					1L, 10L, ActivityTypeCode.QUIZ, "Quiz", 1, Map.of(), Map.of(), "author", null, progress);

			// When-Then:
			assertThat(assembler.isActivityPassed(activity)).isFalse();
		}

		@Test
		void shouldReturnTrueForCompletedNonQuizActivityRegardlessOfScoreTest() {
			// Given: flashcards have no scoring concept — completion alone is a pass
			ActivityProgressViewRecord progress = new ActivityProgressViewRecord(
					ActivityProgressStatusCode.COMPLETED, null, Map.of(), LocalDateTime.now(), LocalDateTime.now(),
					true);
			LessonActivityRecord activity = new LessonActivityRecord(
					1L, 10L, ActivityTypeCode.FLASHCARDS, "Flashcards", 1, Map.of(), Map.of(), "author", null,
					progress);

			// When-Then:
			assertThat(assembler.isActivityPassed(activity)).isTrue();
		}
	}

	@Nested
	class StringList {

		@Test
		void shouldReturnEmptyListWhenValueIsNullTest() {
			// When-Then:
			assertThat(assembler.stringList(null)).isEmpty();
		}

		@Test
		void shouldReturnEmptyListWhenValueIsNotAListTest() {
			// When-Then:
			assertThat(assembler.stringList("not a list")).isEmpty();
			assertThat(assembler.stringList(Map.of("a", "b"))).isEmpty();
		}

		@Test
		void shouldCollectOnlyStringElementsAndDropOthersTest() {
			// Given: a list mixing strings with non-string and null elements
			List<Object> value = Arrays.asList("a", 1, null, "b", true);

			// When:
			List<String> result = assembler.stringList(value);

			// Then:
			assertThat(result).containsExactly("a", "b");
		}
	}

	@Nested
	class StringListList {

		@Test
		void shouldReturnEmptyListWhenValueIsNullTest() {
			// When-Then:
			assertThat(assembler.stringListList(null)).isEmpty();
		}

		@Test
		void shouldReturnEmptyListWhenValueIsNotAListTest() {
			// When-Then:
			assertThat(assembler.stringListList("not a list")).isEmpty();
		}

		@Test
		void shouldConvertNestedListsAndTreatNonListElementsAsEmptyListsTest() {
			// Given: one proper nested list of strings, one non-list element
			List<Object> value = Arrays.asList(List.of("Paris", "Berlin"), "not-a-list");

			// When:
			List<List<String>> result = assembler.stringListList(value);

			// Then:
			assertThat(result).containsExactly(List.of("Paris", "Berlin"), List.of());
		}
	}

	@Nested
	class StringVal {

		@Test
		void shouldReturnEmptyStringWhenValueIsNullTest() {
			// When-Then:
			assertThat(assembler.stringVal(null)).isEmpty();
		}

		@Test
		void shouldReturnValueUnchangedWhenAlreadyAStringTest() {
			// When-Then:
			assertThat(assembler.stringVal("hello")).isEqualTo("hello");
		}

		@Test
		void shouldReturnStringRepresentationForNonStringValuesTest() {
			// When-Then:
			assertThat(assembler.stringVal(42)).isEqualTo("42");
			assertThat(assembler.stringVal(true)).isEqualTo("true");
		}
	}
}
