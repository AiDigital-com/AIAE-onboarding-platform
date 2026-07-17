package com.aidigital.aionboarding.service.lessongen.prompt;

import com.aidigital.aionboarding.domain.lesson.entities.Lesson;
import com.aidigital.aionboarding.service.common.error.AppException;
import com.aidigital.aionboarding.service.common.mapping.JsonMapReader;
import com.aidigital.aionboarding.service.common.mapping.TextValueNormalizer;
import com.aidigital.aionboarding.service.lesson.util.OpenAiPromptCacheUtil;
import com.aidigital.aionboarding.service.lessonactivity.models.FlashcardRecord;
import com.aidigital.aionboarding.service.lessonactivity.models.QuizItemRecord;
import com.aidigital.aionboarding.service.lessonactivity.models.UpdateActivityInput;
import com.aidigital.aionboarding.service.lessonactivity.support.QuizQuestionValidator;
import com.aidigital.aionboarding.service.lessongen.model.ActivityRequest;
import com.aidigital.aionboarding.service.lessongen.model.LessonGenPrompt;
import org.instancio.Instancio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.instancio.Select.field;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ActivityPromptBuilderTest {

	@Mock
	private OpenAiPromptCacheUtil mockCacheUtil;

	@Mock
	private TextValueNormalizer textValueNormalizer;

	@Mock
	private JsonMapReader jsonMapReader;

	@Mock
	private QuizQuestionValidator quizQuestionValidator;

	@InjectMocks
	private ActivityPromptBuilder builder;

	@Nested
	class NormalizeActivityRequest {

		@Test
		void shouldDefaultToQuizForUnknownTypeTest() {
			// Given:
			Map<String, Object> input = new LinkedHashMap<>();
			input.put("type", "unknown");
			input.put("count", 5);
			when(textValueNormalizer.trimmed("unknown")).thenReturn("unknown");

			// When:
			ActivityRequest result = builder.normalizeActivityRequest(input);

			// Then:
			assertThat(result.type()).isEqualTo("quiz");
		}

		@Test
		void shouldAcceptQuizTypeTest() {
			// Given:
			Map<String, Object> input = new LinkedHashMap<>();
			input.put("type", "quiz");
			input.put("count", 10);
			when(textValueNormalizer.trimmed("quiz")).thenReturn("quiz");

			// When:
			ActivityRequest result = builder.normalizeActivityRequest(input);

			// Then:
			assertThat(result.type()).isEqualTo("quiz");
			assertThat(result.count()).isEqualTo(10);
		}

		@Test
		void shouldAcceptFlashcardsTypeTest() {
			// Given:
			Map<String, Object> input = new LinkedHashMap<>();
			input.put("type", "flashcards");
			input.put("count", 10);
			when(textValueNormalizer.trimmed("flashcards")).thenReturn("flashcards");

			// When:
			ActivityRequest result = builder.normalizeActivityRequest(input);

			// Then:
			assertThat(result.type()).isEqualTo("flashcards");
			assertThat(result.count()).isEqualTo(10);
		}

		@Test
		void shouldClampCountToMaxTest() {
			// Given:
			Map<String, Object> input = new LinkedHashMap<>();
			input.put("type", "quiz");
			input.put("count", 50);
			when(textValueNormalizer.trimmed("quiz")).thenReturn("quiz");

			// When:
			ActivityRequest result = builder.normalizeActivityRequest(input);

			// Then:
			assertThat(result.count()).isEqualTo(20);
		}

		@Test
		void shouldClampCountToMinTest() {
			// Given:
			Map<String, Object> input = new LinkedHashMap<>();
			input.put("type", "flashcards");
			input.put("count", 1);
			when(textValueNormalizer.trimmed("flashcards")).thenReturn("flashcards");

			// When:
			ActivityRequest result = builder.normalizeActivityRequest(input);

			// Then:
			assertThat(result.count()).isEqualTo(5);
		}

		@Test
		void shouldDefaultCountWhenMissingTest() {
			// Given:
			Map<String, Object> input = new LinkedHashMap<>();
			input.put("type", "quiz");
			when(textValueNormalizer.trimmed("quiz")).thenReturn("quiz");

			// When:
			ActivityRequest result = builder.normalizeActivityRequest(input);

			// Then:
			assertThat(result.count()).isEqualTo(8);
		}

		@Test
		void shouldHandleNonNumericCountTest() {
			// Given:
			Map<String, Object> input = new LinkedHashMap<>();
			input.put("type", "quiz");
			input.put("count", "abc");
			when(textValueNormalizer.trimmed("quiz")).thenReturn("quiz");

			// When:
			ActivityRequest result = builder.normalizeActivityRequest(input);

			// Then:
			assertThat(result.count()).isEqualTo(8);
		}
	}

	@Nested
	class BuildLessonActivityPrompt {

		@BeforeEach
		void setUp() {
			when(mockCacheUtil.build(any(), any(Object[].class))).thenReturn("test-cache-key");
		}

		@Test
		void shouldBuildQuizPromptTest() {
			// Given:
			Lesson lesson = Instancio.of(Lesson.class)
					.set(field(Lesson::getId), 1L)
					.set(field(Lesson::getTitle), "Test Lesson")
					.set(field(Lesson::getContentHtml), "<p>Content text</p>")
					.set(field(Lesson::getContentMarkdown), "Content text")
					.create();
			when(textValueNormalizer.firstNonBlankTrimmed("<p>Content text</p>", "Content text"))
					.thenReturn("Content text");
			when(textValueNormalizer.firstNonBlankTrimmed("Test Lesson", "Untitled lesson"))
					.thenReturn("Test Lesson");

			// When:
			LessonGenPrompt result = builder.buildLessonActivityPrompt(lesson, "quiz", 3);

			// Then:
			assertThat(result.version()).isEqualTo("lesson-activity-v2-flashcard-front-prompts");
			assertThat(result.instructions()).contains("quiz questions");
			assertThat(result.instructions()).contains("single_choice");
			assertThat(result.input()).contains("Lesson title:");
			assertThat(result.input()).contains("Test Lesson");
		}

		@Test
		void shouldBuildFlashcardsPromptTest() {
			// Given:
			Lesson lesson = Instancio.of(Lesson.class)
					.set(field(Lesson::getId), 1L)
					.set(field(Lesson::getTitle), "Test Lesson")
					.set(field(Lesson::getContentHtml), "<p>Content text</p>")
					.set(field(Lesson::getContentMarkdown), "Content text")
					.create();
			when(textValueNormalizer.firstNonBlankTrimmed("<p>Content text</p>", "Content text"))
					.thenReturn("Content text");
			when(textValueNormalizer.firstNonBlankTrimmed("Test Lesson", "Untitled lesson"))
					.thenReturn("Test Lesson");

			// When:
			LessonGenPrompt result = builder.buildLessonActivityPrompt(lesson, "flashcards", 5);

			// Then:
			assertThat(result.instructions()).contains("flashcards");
			assertThat(result.instructions()).contains("Front must be a complete study prompt");
			assertThat(result.instructions()).doesNotContain("single_choice");
		}

		@Test
		void shouldFallBackToQuizForUnknownTypeTest() {
			// Given:
			Lesson lesson = Instancio.of(Lesson.class)
					.set(field(Lesson::getId), 1L)
					.set(field(Lesson::getTitle), "Test Lesson")
					.set(field(Lesson::getContentHtml), "<p>Content text</p>")
					.set(field(Lesson::getContentMarkdown), "Content text")
					.create();
			when(textValueNormalizer.firstNonBlankTrimmed("<p>Content text</p>", "Content text"))
					.thenReturn("Content text");
			when(textValueNormalizer.firstNonBlankTrimmed("Test Lesson", "Untitled lesson"))
					.thenReturn("Test Lesson");

			// When:
			LessonGenPrompt result = builder.buildLessonActivityPrompt(lesson, "unknown", 3);

			// Then:
			assertThat(result.instructions()).contains("quiz questions");
		}

		@Test
		void shouldUseMarkdownWhenHtmlIsBlankTest() {
			// Given:
			Lesson lesson = Instancio.of(Lesson.class)
					.set(field(Lesson::getId), 1L)
					.set(field(Lesson::getTitle), "Test Lesson")
					.set(field(Lesson::getContentHtml), "")
					.set(field(Lesson::getContentMarkdown), "# Markdown content")
					.create();
			when(textValueNormalizer.firstNonBlankTrimmed("", "# Markdown content"))
					.thenReturn("Markdown content");
			when(textValueNormalizer.firstNonBlankTrimmed("Test Lesson", "Untitled lesson"))
					.thenReturn("Test Lesson");

			// When:
			LessonGenPrompt result = builder.buildLessonActivityPrompt(lesson, "quiz", 3);

			// Then:
			assertThat(result.input()).contains("Markdown content");
		}
	}

	@Nested
	class NormalizeGeneratedActivityPayload {

		@Test
		void shouldThrowForNullPayloadTest() {
			// When-Then:
			assertThatThrownBy(() -> builder.normalizeGeneratedActivityPayload(null,
					Instancio.of(ActivityRequest.class)
							.set(field("type"), "quiz")
							.set(field("count"), 3)
							.set(field("limits"), null)
							.create()))
					.isInstanceOf(AppException.class)
					.hasMessageContaining("OpenAI returned an invalid activity");
		}

		@Test
		void shouldThrowForEmptyPayloadTest() {
			// When-Then:
			assertThatThrownBy(() -> builder.normalizeGeneratedActivityPayload(Map.of(),
					Instancio.of(ActivityRequest.class)
							.set(field("type"), "quiz")
							.set(field("count"), 3)
							.set(field("limits"), null)
							.create()))
					.isInstanceOf(AppException.class)
					.hasMessageContaining("OpenAI returned an invalid activity");
		}

		@Test
		void shouldRouteToFlashcardsNormalizerWhenTypeIsFlashcardTest() {
			// Given:
			Map<String, Object> rawPayload = new LinkedHashMap<>();
			rawPayload.put("type", "flashcards");
			rawPayload.put("title", "Flashcards set");
			Map<String, Object> card = new LinkedHashMap<>();
			card.put("front", "What is Java?");
			card.put("back", "A programming language");
			card.put("explanation", "Java is widely used.");
			rawPayload.put("cards", List.of(card));
			when(jsonMapReader.mapList(List.of(card))).thenReturn(List.of(card));
			when(textValueNormalizer.firstNonBlankTrimmed("Flashcards set", "Lesson flashcards"))
					.thenReturn("Flashcards set");

			// When:
			Map<String, Object> result = builder.normalizeGeneratedActivityPayload(rawPayload,
					Instancio.of(ActivityRequest.class)
							.set(field("type"), "flashcards")
							.set(field("count"), 10)
							.set(field("limits"), null)
							.create());

			// Then:
			assertThat(result.get("type")).isEqualTo("flashcards");
			@SuppressWarnings("unchecked")
			List<Map<String, Object>> cards = (List<Map<String, Object>>) result.get("cards");
			assertThat(cards).hasSize(1);
		}

		@Test
		void shouldRouteToQuizNormalizerWhenTypeIsQuizTest() {
			// Given:
			Map<String, Object> singleChoiceItem = new LinkedHashMap<>();
			singleChoiceItem.put("type", "single_choice");
			singleChoiceItem.put("question", "What is the capital of France?");
			singleChoiceItem.put("options", List.of("Paris", "Berlin", "Madrid"));
			singleChoiceItem.put("correctAnswer", "Paris");
			singleChoiceItem.put("explanation", "Paris is the capital.");
			Map<String, Object> rawPayload = new LinkedHashMap<>();
			rawPayload.put("type", "quiz");
			rawPayload.put("title", "Quiz");
			rawPayload.put("items", List.of(singleChoiceItem));
			when(jsonMapReader.mapList(List.of(singleChoiceItem))).thenReturn(List.of(singleChoiceItem));
			when(jsonMapReader.stringList(List.of("Paris", "Berlin", "Madrid")))
					.thenReturn(List.of("Paris", "Berlin", "Madrid"));
			when(jsonMapReader.stringList((Object) null)).thenReturn(List.of());
			when(quizQuestionValidator.normalize(
					"single_choice",
					"What is the capital of France?",
					List.of("Paris", "Berlin", "Madrid"),
					"Paris",
					List.of(),
					"Paris is the capital."))
					.thenReturn(singleChoiceItem);
			when(textValueNormalizer.firstNonBlankTrimmed("Quiz", "Lesson quiz"))
					.thenReturn("Quiz");

			// When:
			Map<String, Object> result = builder.normalizeGeneratedActivityPayload(rawPayload,
					Instancio.of(ActivityRequest.class)
							.set(field("type"), "quiz")
							.set(field("count"), 10)
							.set(field("limits"), null)
							.create());

			// Then:
			assertThat(result.get("type")).isEqualTo("quiz");
		}
	}

	@Nested
	class NormalizeQuizPayload {

		@Test
		void shouldKeepMultipleChoiceQuestionWithCorrectAnswersArrayTest() {
			// Given:
			Map<String, Object> singleChoiceItem = new LinkedHashMap<>();
			singleChoiceItem.put("type", "single_choice");
			singleChoiceItem.put("question", "What is the capital of France?");
			singleChoiceItem.put("options", List.of("Paris", "Berlin", "Madrid"));
			singleChoiceItem.put("correctAnswer", "Paris");
			singleChoiceItem.put("explanation", "Paris is the capital.");
			Map<String, Object> multipleChoiceItem = new LinkedHashMap<>();
			multipleChoiceItem.put("type", "multiple_choice");
			multipleChoiceItem.put("question", "Which are primary colors?");
			multipleChoiceItem.put("options", List.of("Red", "Blue", "Green", "Yellow"));
			multipleChoiceItem.put("correctAnswers", List.of("Red", "Blue", "Yellow"));
			multipleChoiceItem.put("explanation", "Red, blue, and yellow are primary colors.");
			Map<String, Object> rawPayload = new LinkedHashMap<>();
			rawPayload.put("title", "Lesson quiz");
			rawPayload.put("items", List.of(singleChoiceItem, multipleChoiceItem));
			Map<String, Object> normalizedMultipleChoiceItem = new LinkedHashMap<>();
			normalizedMultipleChoiceItem.put("type", "multiple_choice");
			normalizedMultipleChoiceItem.put("question", "Which are primary colors?");
			normalizedMultipleChoiceItem.put("options", List.of("Red", "Blue", "Green", "Yellow"));
			normalizedMultipleChoiceItem.put("correctAnswer", "Red");
			normalizedMultipleChoiceItem.put("correctAnswers", List.of("Red", "Blue", "Yellow"));
			normalizedMultipleChoiceItem.put("explanation", "Red, blue, and yellow are primary colors.");
			when(jsonMapReader.mapList(List.of(singleChoiceItem, multipleChoiceItem)))
					.thenReturn(List.of(singleChoiceItem, multipleChoiceItem));
			when(jsonMapReader.stringList(List.of("Paris", "Berlin", "Madrid")))
					.thenReturn(List.of("Paris", "Berlin", "Madrid"));
			when(jsonMapReader.stringList(List.of("Red", "Blue", "Green", "Yellow")))
					.thenReturn(List.of("Red", "Blue", "Green", "Yellow"));
			when(jsonMapReader.stringList(List.of("Red", "Blue", "Yellow")))
					.thenReturn(List.of("Red", "Blue", "Yellow"));
			when(jsonMapReader.stringList((Object) null)).thenReturn(List.of());
			when(quizQuestionValidator.normalize(
					"single_choice",
					"What is the capital of France?",
					List.of("Paris", "Berlin", "Madrid"),
					"Paris",
					List.of(),
					"Paris is the capital."))
					.thenReturn(singleChoiceItem);
			when(quizQuestionValidator.normalize(
					"multiple_choice",
					"Which are primary colors?",
					List.of("Red", "Blue", "Green", "Yellow"),
					"",
					List.of("Red", "Blue", "Yellow"),
					"Red, blue, and yellow are primary colors."))
					.thenReturn(normalizedMultipleChoiceItem);
			when(textValueNormalizer.firstNonBlankTrimmed("Lesson quiz", "Lesson quiz"))
					.thenReturn("Lesson quiz");

			// When:
			Map<String, Object> normalized = builder.normalizeQuizPayload(rawPayload, 2);

			// Then:
			@SuppressWarnings("unchecked")
			List<Map<String, Object>> items = (List<Map<String, Object>>) normalized.get("items");
			assertThat(items).hasSize(2);
			assertThat(items.get(1).get("type")).isEqualTo("multiple_choice");
			assertThat(items.get(1).get("correctAnswers")).isEqualTo(List.of("Red", "Blue", "Yellow"));
		}

		@Test
		void shouldReturnRequestedCountWhenAllQuestionsAreValidTest() {
			// Given:
			Map<String, Object> singleChoiceItem = new LinkedHashMap<>();
			singleChoiceItem.put("type", "single_choice");
			singleChoiceItem.put("question", "What is the capital of France?");
			singleChoiceItem.put("options", List.of("Paris", "Berlin", "Madrid"));
			singleChoiceItem.put("correctAnswer", "Paris");
			singleChoiceItem.put("explanation", "Paris is the capital.");
			Map<String, Object> multipleChoiceItem = new LinkedHashMap<>();
			multipleChoiceItem.put("type", "multiple_choice");
			multipleChoiceItem.put("question", "Which are primary colors?");
			multipleChoiceItem.put("options", List.of("Red", "Blue", "Green", "Yellow"));
			multipleChoiceItem.put("correctAnswers", List.of("Red", "Blue", "Yellow"));
			multipleChoiceItem.put("explanation", "Red, blue, and yellow are primary colors.");
			List<Map<String, Object>> items = List.of(
					singleChoiceItem, multipleChoiceItem, singleChoiceItem, multipleChoiceItem);
			Map<String, Object> rawPayload = new LinkedHashMap<>();
			rawPayload.put("title", "Lesson quiz");
			rawPayload.put("items", items);
			when(jsonMapReader.mapList(items)).thenReturn(items);
			when(jsonMapReader.stringList(List.of("Paris", "Berlin", "Madrid")))
					.thenReturn(List.of("Paris", "Berlin", "Madrid"));
			when(jsonMapReader.stringList(List.of("Red", "Blue", "Green", "Yellow")))
					.thenReturn(List.of("Red", "Blue", "Green", "Yellow"));
			when(jsonMapReader.stringList(List.of("Red", "Blue", "Yellow")))
					.thenReturn(List.of("Red", "Blue", "Yellow"));
			when(jsonMapReader.stringList((Object) null)).thenReturn(List.of());
			when(quizQuestionValidator.normalize(
					"single_choice",
					"What is the capital of France?",
					List.of("Paris", "Berlin", "Madrid"),
					"Paris",
					List.of(),
					"Paris is the capital."))
					.thenReturn(singleChoiceItem);
			when(quizQuestionValidator.normalize(
					"multiple_choice",
					"Which are primary colors?",
					List.of("Red", "Blue", "Green", "Yellow"),
					"",
					List.of("Red", "Blue", "Yellow"),
					"Red, blue, and yellow are primary colors."))
					.thenReturn(multipleChoiceItem);
			when(textValueNormalizer.firstNonBlankTrimmed("Lesson quiz", "Lesson quiz"))
					.thenReturn("Lesson quiz");

			// When:
			Map<String, Object> normalized = builder.normalizeQuizPayload(rawPayload, 4);

			// Then:
			@SuppressWarnings("unchecked")
			List<Map<String, Object>> normalizedItems = (List<Map<String, Object>>) normalized.get("items");
			assertThat(normalizedItems).hasSize(4);
		}
	}

	@Nested
	class NormalizeQuizUpdatePayload {

		@Test
		void shouldThrowWhenNoItemsTest() {
			// Given:
			UpdateActivityInput input = Instancio.of(UpdateActivityInput.class)
					.set(field("title"), "Quiz")
					.set(field("items"), List.of())
					.set(field("cards"), null)
					.create();

			// When-Then:
			assertThatThrownBy(() -> builder.normalizeQuizUpdatePayload(input))
					.isInstanceOf(AppException.class)
					.hasMessageContaining("Add at least one valid quiz question");
		}

		@Test
		void shouldNormalizeSingleChoiceItemTest() {
			// Given:
			Map<String, Object> normalizedItem = new LinkedHashMap<>();
			normalizedItem.put("type", "single_choice");
			normalizedItem.put("question", "What is 2+2?");
			normalizedItem.put("options", List.of("3", "4", "5"));
			normalizedItem.put("correctAnswer", "4");
			normalizedItem.put("explanation", "Correct");
			QuizItemRecord item = Instancio.of(QuizItemRecord.class)
					.set(field("type"), "single_choice")
					.set(field("question"), "What is 2+2?")
					.set(field("options"), List.of("3", "4", "5"))
					.set(field("correctAnswer"), "4")
					.set(field("correctAnswers"), null)
					.set(field("explanation"), "Correct")
					.create();
			UpdateActivityInput input = Instancio.of(UpdateActivityInput.class)
					.set(field("title"), "My Quiz")
					.set(field("items"), List.of(item))
					.set(field("cards"), null)
					.create();
			when(quizQuestionValidator.normalize(
					"single_choice", "What is 2+2?", List.of("3", "4", "5"), "4", null, "Correct"))
					.thenReturn(normalizedItem);
			when(textValueNormalizer.firstNonBlankTrimmed("My Quiz", "Lesson quiz"))
					.thenReturn("My Quiz");

			// When:
			Map<String, Object> result = builder.normalizeQuizUpdatePayload(input);

			// Then:
			assertThat(result.get("type")).isEqualTo("quiz");
			assertThat(result.get("title")).isEqualTo("My Quiz");
			@SuppressWarnings("unchecked")
			List<Map<String, Object>> resultItems = (List<Map<String, Object>>) result.get("items");
			assertThat(resultItems).hasSize(1);
		}

		@Test
		void shouldThrowOnInvalidQuestionTest() {
			// Given:
			QuizItemRecord item = Instancio.of(QuizItemRecord.class)
					.set(field("type"), "single_choice")
					.set(field("question"), "")
					.set(field("options"), List.of())
					.set(field("correctAnswer"), "")
					.set(field("correctAnswers"), null)
					.set(field("explanation"), "")
					.create();
			UpdateActivityInput input = Instancio.of(UpdateActivityInput.class)
					.set(field("title"), null)
					.set(field("items"), List.of(item))
					.set(field("cards"), null)
					.create();
			when(quizQuestionValidator.normalize("single_choice", "", List.of(), "", null, ""))
					.thenReturn(null);
			when(quizQuestionValidator.describeFailure("single_choice", "", List.of(), "", null))
					.thenReturn("add a question");

			// When-Then:
			assertThatThrownBy(() -> builder.normalizeQuizUpdatePayload(input))
					.isInstanceOf(AppException.class)
					.hasMessageContaining("Question 1");
		}
	}

	@Nested
	class NormalizeFlashcardsUpdatePayload {

		@Test
		void shouldThrowWhenNoCardsTest() {
			// Given:
			UpdateActivityInput input = Instancio.of(UpdateActivityInput.class)
					.set(field("title"), null)
					.set(field("items"), null)
					.set(field("cards"), List.of())
					.create();

			// When-Then:
			assertThatThrownBy(() -> builder.normalizeFlashcardsUpdatePayload(input))
					.isInstanceOf(AppException.class)
					.hasMessageContaining("Add at least one valid flashcard");
		}

		@Test
		void shouldThrowWhenFrontIsBlankTest() {
			// Given:
			FlashcardRecord card = Instancio.of(FlashcardRecord.class)
					.set(field("front"), "")
					.set(field("back"), "Back")
					.set(field("explanation"), null)
					.create();
			UpdateActivityInput input = Instancio.of(UpdateActivityInput.class)
					.set(field("title"), null)
					.set(field("items"), null)
					.set(field("cards"), List.of(card))
					.create();

			// When-Then:
			assertThatThrownBy(() -> builder.normalizeFlashcardsUpdatePayload(input))
					.isInstanceOf(AppException.class)
					.hasMessageContaining("Card 1");
		}

		@Test
		void shouldNormalizeValidFlashcardsTest() {
			// Given:
			FlashcardRecord card1 = Instancio.of(FlashcardRecord.class)
					.set(field("front"), "What is AI?")
					.set(field("back"), "Artificial Intelligence")
					.set(field("explanation"), "Good question")
					.create();
			FlashcardRecord card2 = Instancio.of(FlashcardRecord.class)
					.set(field("front"), "What is ML?")
					.set(field("back"), "Machine Learning")
					.set(field("explanation"), "")
					.create();
			UpdateActivityInput input = Instancio.of(UpdateActivityInput.class)
					.set(field("title"), "My Cards")
					.set(field("items"), null)
					.set(field("cards"), List.of(card1, card2))
					.create();
			when(textValueNormalizer.firstNonBlankTrimmed("My Cards", "Lesson flashcards"))
					.thenReturn("My Cards");

			// When:
			Map<String, Object> result = builder.normalizeFlashcardsUpdatePayload(input);

			// Then:
			assertThat(result.get("type")).isEqualTo("flashcards");
			@SuppressWarnings("unchecked")
			List<Map<String, Object>> cards = (List<Map<String, Object>>) result.get("cards");
			assertThat(cards).hasSize(2);
			assertThat(cards.get(0).get("front")).isEqualTo("What is AI?");
		}
	}

	@Nested
	class GetActivityShape {

		@Test
		void shouldReturnQuizShapeTest() {
			// Given:
			String type = "quiz";

			// When:
			String shape = builder.getActivityShape(type);

			// Then:
			assertThat(shape).contains("\"type\": \"quiz\"");
			assertThat(shape).contains("single_choice");
			assertThat(shape).contains("multiple_choice");
		}

		@Test
		void shouldReturnFlashcardsShapeTest() {
			// Given:
			String type = "flashcards";

			// When:
			String shape = builder.getActivityShape(type);

			// Then:
			assertThat(shape).contains("\"type\": \"flashcards\"");
			assertThat(shape).contains("\"cards\"");
		}
	}

	@Nested
	class StripHtml {

		@Test
		void shouldReturnEmptyForNullTest() {
			// Given:
			String input = null;

			// When:
			String result = builder.stripHtml(input);

			// Then:
			assertThat(result).isEmpty();
		}

		@Test
		void shouldRemoveTagsTest() {
			// Given:
			String input = "<p>Hello <b>World</b></p>";

			// When:
			String result = builder.stripHtml(input);

			// Then:
			assertThat(result).isEqualTo("Hello World");
		}

		@Test
		void shouldRemoveStyleAndScriptBlocksTest() {
			// Given:
			String input = "<style>body{}</style><p>Text</p><script>alert()</script>";

			// When:
			String result = builder.stripHtml(input);

			// Then:
			assertThat(result).isEqualTo("Text");
		}

		@Test
		void shouldNormalizeWhitespaceTest() {
			// Given:
			String input = "<p>Hello   World\n\nTest</p>";

			// When:
			String result = builder.stripHtml(input);

			// Then:
			assertThat(result).isEqualTo("Hello World Test");
		}
	}

	@Nested
	class NormalizeString {

		@Test
		void shouldReturnEmptyForNullTest() {
			// Given:
			String input = null;

			// When:
			String result = builder.normalizeString(input);

			// Then:
			assertThat(result).isEmpty();
		}

		@Test
		void shouldReturnEmptyForNonStringTest() {
			// Given:
			Object input = 42;

			// When:
			String result = builder.normalizeString(input);

			// Then:
			assertThat(result).isEmpty();
		}

		@Test
		void shouldTrimStringTest() {
			// Given:
			String input = "  hello  ";

			// When:
			String result = builder.normalizeString(input);

			// Then:
			assertThat(result).isEqualTo("hello");
		}
	}

	@Nested
	class ClampCount {

		@Test
		void shouldReturnDefaultWhenCountIsNullTest() {
			// Given:
			String type = "quiz";
			Object count = null;

			// When:
			int result = builder.clampCount(type, count);

			// Then:
			assertThat(result).isEqualTo(8);
		}

		@Test
		void shouldReturnDefaultWhenCountIsInvalidTest() {
			// Given:
			String type = "flashcards";
			Object count = "not-a-number";

			// When:
			int result = builder.clampCount(type, count);

			// Then:
			assertThat(result).isEqualTo(12);
		}

		@Test
		void shouldClampBelowMinimumTest() {
			// Given:
			String type = "quiz";
			Object count = 1;

			// When:
			int result = builder.clampCount(type, count);

			// Then:
			assertThat(result).isEqualTo(3);
		}

		@Test
		void shouldClampAboveMaximumTest() {
			// Given:
			String type = "flashcards";
			Object count = 100;

			// When:
			int result = builder.clampCount(type, count);

			// Then:
			assertThat(result).isEqualTo(40);
		}

		@Test
		void shouldReturnValidCountTest() {
			// Given:
			String type = "quiz";
			Object count = 10;

			// When:
			int result = builder.clampCount(type, count);

			// Then:
			assertThat(result).isEqualTo(10);
		}
	}

	@Nested
	class NormalizeFlashcardsPayload {

		@Test
		void shouldSkipCardsWithEmptyFrontTest() {
			// Given:
			Map<String, Object> rawPayload = new LinkedHashMap<>();
			Map<String, Object> badCard = new LinkedHashMap<>();
			badCard.put("front", "");
			badCard.put("back", "Some back");
			Map<String, Object> goodCard = new LinkedHashMap<>();
			goodCard.put("front", "Valid front");
			goodCard.put("back", "Valid back");
			rawPayload.put("cards", List.of(badCard, goodCard));
			when(jsonMapReader.mapList(List.of(badCard, goodCard)))
					.thenReturn(List.of(badCard, goodCard));
			when(textValueNormalizer.firstNonBlankTrimmed("", "Lesson flashcards"))
					.thenReturn("Lesson flashcards");

			// When:
			Map<String, Object> result = builder.normalizeFlashcardsPayload(rawPayload, 10);

			// Then:
			@SuppressWarnings("unchecked")
			List<Map<String, Object>> cards = (List<Map<String, Object>>) result.get("cards");
			assertThat(cards).hasSize(1);
		}

		@Test
		void shouldTruncateToExpectedCountTest() {
			// Given:
			Map<String, Object> rawPayload = new LinkedHashMap<>();
			Map<String, Object> card = new LinkedHashMap<>();
			card.put("front", "F");
			card.put("back", "B");
			List<Map<String, Object>> cards = List.of(card, card, card, card, card);
			rawPayload.put("cards", cards);
			when(jsonMapReader.mapList(cards)).thenReturn(cards);
			when(textValueNormalizer.firstNonBlankTrimmed("", "Lesson flashcards"))
					.thenReturn("Lesson flashcards");

			// When:
			Map<String, Object> result = builder.normalizeFlashcardsPayload(rawPayload, 2);

			// Then:
			@SuppressWarnings("unchecked")
			List<Map<String, Object>> resultCards = (List<Map<String, Object>>) result.get("cards");
			assertThat(resultCards).hasSize(2);
		}
	}
}
