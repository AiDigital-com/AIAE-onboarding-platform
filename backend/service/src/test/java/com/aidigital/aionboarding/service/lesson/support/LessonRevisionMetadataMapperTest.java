package com.aidigital.aionboarding.service.lesson.support;

import com.aidigital.aionboarding.domain.lesson.entities.Lesson;
import com.aidigital.aionboarding.service.lesson.models.LessonRevisionPromptRecord;
import com.aidigital.aionboarding.service.lesson.models.RevisionBriefRecord;
import com.aidigital.aionboarding.service.lesson.models.RevisionHistoryEntryRecord;
import com.aidigital.aionboarding.service.lesson.models.RevisionProviderMetadataRecord;
import com.aidigital.aionboarding.service.lesson.util.LessonContentUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LessonRevisionMetadataMapperTest {

	@Mock
	private LessonRecordAssembler lessonRecordAssembler;
	@Mock
	private LessonContentUtil lessonContentUtil;
	@Mock
	private LessonHtmlSanitizer lessonHtmlSanitizer;

	@InjectMocks
	private LessonRevisionMetadataMapper mapper;

	@Test
	void applyRevisedContentShouldPersistTheSanitizerOutputRatherThanRawWriterHtmlTest() {
		// Given: the writer AI returned HTML containing a script tag the sanitizer would strip.
		Lesson lesson = new Lesson();
		lesson.setTitle("Old title");
		lesson.setContentHtml("<h1>Old</h1>");
		lesson.setContentMarkdown("# Old");
		String rawRevisedHtml = "<h1>New</h1><script>evil()</script>";
		String sanitizedHtml = "<h1>New</h1>";
		when(lessonContentUtil.looksLikeHtml(rawRevisedHtml)).thenReturn(true);
		when(lessonHtmlSanitizer.sanitize(rawRevisedHtml)).thenReturn(sanitizedHtml);
		when(lessonContentUtil.extractHtmlTitle(sanitizedHtml)).thenReturn("New");

		// When:
		mapper.applyRevisedContent(lesson, rawRevisedHtml);

		// Then:
		assertThat(lesson.getContentHtml()).isEqualTo(sanitizedHtml);
		assertThat(lesson.getTitle()).isEqualTo("New");
	}

	@Test
	void applyRevisedContentShouldSanitizeMarkdownConvertedHtmlTooTest() {
		// Given: the writer AI returned markdown, which is converted to HTML before sanitizing.
		Lesson lesson = new Lesson();
		lesson.setTitle("Old title");
		String rawRevisedMarkdown = "# New\n\n<script>evil()</script>";
		String convertedHtml = "<h1>New</h1><script>evil()</script>";
		String sanitizedHtml = "<h1>New</h1>";
		when(lessonContentUtil.looksLikeHtml(rawRevisedMarkdown)).thenReturn(false);
		when(lessonContentUtil.markdownToHtml(rawRevisedMarkdown)).thenReturn(convertedHtml);
		when(lessonHtmlSanitizer.sanitize(convertedHtml)).thenReturn(sanitizedHtml);
		when(lessonContentUtil.extractHtmlTitle(sanitizedHtml)).thenReturn("New");

		// When:
		mapper.applyRevisedContent(lesson, rawRevisedMarkdown);

		// Then:
		assertThat(lesson.getContentHtml()).isEqualTo(sanitizedHtml);
		assertThat(lesson.getContentMarkdown()).isEqualTo(rawRevisedMarkdown);
	}

	@Test
	void applyRevisedContentShouldKeepOldTitleWhenSanitizedHtmlHasNoTitleTest() {
		// Given:
		Lesson lesson = new Lesson();
		lesson.setTitle("Old title");
		String rawRevisedHtml = "<p>Body only</p>";
		String sanitizedHtml = "<p>Body only</p>";
		when(lessonContentUtil.looksLikeHtml(rawRevisedHtml)).thenReturn(true);
		when(lessonHtmlSanitizer.sanitize(rawRevisedHtml)).thenReturn(sanitizedHtml);
		when(lessonContentUtil.extractHtmlTitle(sanitizedHtml)).thenReturn("");

		// When:
		mapper.applyRevisedContent(lesson, rawRevisedHtml);

		// Then:
		assertThat(lesson.getContentHtml()).isEqualTo(sanitizedHtml);
		assertThat(lesson.getTitle()).isEqualTo("Old title");
	}

	@Test
	void applyRevisedContentShouldPersistEmptyMarkdownWhenHtmlInputLacksMarkdownSourceTest() {
		// Given:
		Lesson lesson = new Lesson();
		lesson.setContentMarkdown(null);
		String rawRevisedHtml = "<h1>New</h1>";
		String sanitizedHtml = "<h1>New</h1>";
		when(lessonContentUtil.looksLikeHtml(rawRevisedHtml)).thenReturn(true);
		when(lessonHtmlSanitizer.sanitize(rawRevisedHtml)).thenReturn(sanitizedHtml);
		when(lessonContentUtil.extractHtmlTitle(sanitizedHtml)).thenReturn("New");

		// When:
		mapper.applyRevisedContent(lesson, rawRevisedHtml);

		// Then:
		assertThat(lesson.getContentMarkdown()).isEmpty();
	}

	@Test
	void toMapShouldConvertRevisionPromptRecordTest() {
		// Given:
		LessonRevisionPromptRecord prompt = new LessonRevisionPromptRecord("v1", "cache", "instructions", "input");

		// When:
		Map<String, Object> map = mapper.toMap(prompt);

		// Then:
		assertThat(map).containsExactly(
				entry("version", "v1"),
				entry("cacheKey", "cache"),
				entry("instructions", "instructions"),
				entry("input", "input"));
	}

	@Test
	void toMapShouldConvertProviderMetadataWithRawOutputWhenPresentTest() {
		// Given:
		RevisionProviderMetadataRecord metadata = new RevisionProviderMetadataRecord(
				"openai", "gpt-4o", "v1", "cache", "raw-output");

		// When:
		Map<String, Object> map = mapper.toMap(metadata);

		// Then:
		assertThat(map).containsEntry("provider", "openai");
		assertThat(map).containsEntry("rawOutput", "raw-output");
	}

	@Test
	void toMapShouldOmitRawOutputWhenNullTest() {
		// Given:
		RevisionProviderMetadataRecord metadata = new RevisionProviderMetadataRecord(
				"openai", "gpt-4o", "v1", "cache", null);

		// When:
		Map<String, Object> map = mapper.toMap(metadata);

		// Then:
		assertThat(map).containsEntry("provider", "openai");
		assertThat(map).doesNotContainKey("rawOutput");
	}

	@Test
	void toMapShouldConvertRevisionHistoryEntryRecordTest() {
		// Given:
		LessonRevisionPromptRecord prompt = new LessonRevisionPromptRecord("v1", "cache", "instructions", "input");
		RevisionProviderMetadataRecord provider = new RevisionProviderMetadataRecord(
				"openai", "gpt-4o", "v1", "cache", "raw");
		RevisionBriefRecord brief = new RevisionBriefRecord(
				"substantial", "intent", List.of("edit"), List.of("preserve"), List.of("risk"));
		Map<String, Object> briefMap = Map.of(
				"changeScope", "substantial",
				"userIntent", "intent",
				"editInstructions", List.of("edit"),
				"preserveRules", List.of("preserve"),
				"riskNotes", List.of("risk"));
		RevisionHistoryEntryRecord entry = new RevisionHistoryEntryRecord(
				"2026-01-01T00:00:00Z", "request", List.of("opt"), brief, prompt, prompt, provider, provider);
		when(lessonRecordAssembler.toRevisionBriefMap(brief)).thenReturn(briefMap);

		// When:
		Map<String, Object> map = mapper.toMap(entry);

		// Then:
		assertThat(map).containsEntry("revisedAt", "2026-01-01T00:00:00Z");
		assertThat(map).containsEntry("revisionRequest", "request");
		assertThat(map).containsEntry("selectedOptions", List.of("opt"));
		assertThat(map).containsEntry("revisionBrief", briefMap);
		assertThat(map).containsKey("plannerPrompt");
		assertThat(map).containsKey("writerPrompt");
		assertThat(map).containsKey("planner");
		assertThat(map).containsKey("writer");
	}

	@Test
	void mergeRevisionEntryShouldInitializeMetadataWhenAbsentTest() {
		// Given:
		Lesson lesson = new Lesson();
		lesson.setGenerationMetadata(null);
		LessonRevisionPromptRecord prompt = new LessonRevisionPromptRecord("v1", "cache", "instructions", "input");
		RevisionProviderMetadataRecord provider = new RevisionProviderMetadataRecord(
				"openai", "gpt-4o", "v1", "cache", null);
		RevisionBriefRecord brief = new RevisionBriefRecord("substantial", "intent", List.of(), List.of(), List.of());
		RevisionHistoryEntryRecord entry = new RevisionHistoryEntryRecord(
				"2026-01-01T00:00:00Z", "request", List.of(), brief, prompt, prompt, provider, provider);
		Map<String, Object> briefMap = new LinkedHashMap<>(Map.of(
				"changeScope", "substantial", "userIntent", "intent", "editInstructions", List.of(),
				"preserveRules", List.of(), "riskNotes", List.of()));
		when(lessonRecordAssembler.toRevisionBriefMap(brief)).thenReturn(briefMap);

		// When:
		Map<String, Object> metadata = mapper.mergeRevisionEntry(lesson, entry);

		// Then:
		assertThat(metadata).containsKey("revisionHistory");
		assertThat((List<?>) metadata.get("revisionHistory")).hasSize(1);
		assertThat(metadata.get("lastRevisionScope")).isEqualTo("substantial");
	}

	@Test
	void mergeRevisionEntryShouldKeepLastNineExistingEntriesAndAppendNewOneTest() {
		// Given:
		Lesson lesson = new Lesson();
		List<Object> existingHistory = new java.util.ArrayList<>();
		for (int i = 0; i < 12; i++) {
			existingHistory.add(Map.of("index", i));
		}
		lesson.setGenerationMetadata(new LinkedHashMap<>(Map.of("revisionHistory", existingHistory)));
		LessonRevisionPromptRecord prompt = new LessonRevisionPromptRecord("v1", "cache", "instructions", "input");
		RevisionProviderMetadataRecord provider = new RevisionProviderMetadataRecord(
				"openai", "gpt-4o", "v1", "cache", null);
		RevisionBriefRecord brief = new RevisionBriefRecord("targeted", "intent", List.of(), List.of(), List.of());
		RevisionHistoryEntryRecord entry = new RevisionHistoryEntryRecord(
				"2026-01-01T00:00:00Z", "request", List.of(), brief, prompt, prompt, provider, provider);
		Map<String, Object> briefMap = new LinkedHashMap<>(Map.of(
				"changeScope", "targeted", "userIntent", "intent", "editInstructions", List.of(),
				"preserveRules", List.of(), "riskNotes", List.of()));
		when(lessonRecordAssembler.toRevisionBriefMap(brief)).thenReturn(briefMap);

		// When:
		Map<String, Object> metadata = mapper.mergeRevisionEntry(lesson, entry);

		// Then:
		List<?> history = (List<?>) metadata.get("revisionHistory");
		assertThat(history).hasSize(10);
		assertThat(((Map<?, ?>) history.get(0)).get("index")).isEqualTo(3);
		assertThat(((Map<?, ?>) history.get(9)).get("revisionBrief")).isEqualTo(briefMap);
	}

	@Test
	void mergeRevisionEntryShouldFallbackToRecordChangeScopeWhenBriefMapNotAMapTest() {
		// Given:
		Lesson lesson = new Lesson();
		lesson.setGenerationMetadata(new LinkedHashMap<>());
		LessonRevisionPromptRecord prompt = new LessonRevisionPromptRecord("v1", "cache", "instructions", "input");
		RevisionProviderMetadataRecord provider = new RevisionProviderMetadataRecord(
				"openai", "gpt-4o", "v1", "cache", null);
		RevisionBriefRecord brief = new RevisionBriefRecord("near-complete", "intent", List.of(), List.of(),
				List.of());
		RevisionHistoryEntryRecord entry = new RevisionHistoryEntryRecord(
				"2026-01-01T00:00:00Z", "request", List.of(), brief, prompt, prompt, provider, provider);
		when(lessonRecordAssembler.toRevisionBriefMap(brief)).thenReturn(Map.of("changeScope", "from-map"));

		// When:
		Map<String, Object> metadata = mapper.mergeRevisionEntry(lesson, entry);

		// Then:
		assertThat(metadata.get("lastRevisionScope")).isEqualTo("from-map");
	}

	@Test
	void buildRevisionBriefShouldTrimAndMapAllFieldsTest() {
		// Given:
		Map<String, Object> briefMap = new LinkedHashMap<>();
		briefMap.put("changeScope", "targeted ");
		briefMap.put("userIntent", " Fix typos ");
		briefMap.put("editInstructions", List.of("edit 1", "  ", "edit 2"));
		briefMap.put("preserveRules", List.of("preserve"));
		briefMap.put("riskNotes", List.of("risk"));

		// When:
		RevisionBriefRecord result = mapper.buildRevisionBrief(briefMap);

		// Then:
		assertThat(result.changeScope()).isEqualTo("targeted");
		assertThat(result.userIntent()).isEqualTo("Fix typos");
		assertThat(result.editInstructions()).containsExactly("edit 1", "edit 2");
		assertThat(result.preserveRules()).containsExactly("preserve");
		assertThat(result.riskNotes()).containsExactly("risk");
	}

	@Test
	void buildProviderMetadataShouldMapAllFieldsAndTreatNullAsEmptyTest() {
		// Given:
		Map<String, Object> meta = new LinkedHashMap<>();
		meta.put("provider", "openai");
		meta.put("model", "gpt-4o");
		meta.put("promptVersion", "v1");
		meta.put("promptCacheKey", "cache");
		meta.put("rawOutput", "raw");

		// When:
		RevisionProviderMetadataRecord result = mapper.buildProviderMetadata(meta);

		// Then:
		assertThat(result.provider()).isEqualTo("openai");
		assertThat(result.rawOutput()).isEqualTo("raw");
	}

	private static Map.Entry<String, Object> entry(String key, Object value) {
		return Map.entry(key, value);
	}
}
