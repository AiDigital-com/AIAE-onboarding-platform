package com.aidigital.aionboarding.service.teachervideo.prompt;

import com.aidigital.aionboarding.service.common.mapping.TextValueNormalizer;
import com.aidigital.aionboarding.service.lesson.util.LessonTextUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TeacherVideoPromptBuilderTest {

	@Mock
	private LessonTextUtil lessonTextUtil;
	@Mock
	private TextValueNormalizer textValueNormalizer;

	@InjectMocks
	private TeacherVideoPromptBuilder builder;

	@Test
	void shouldBuildPromptWithSourceNamesAndDescriptionTest() {
		// Given:
		Map<String, Object> lesson = Map.of(
				"title", "Lesson title",
				"description", "Lesson description",
				"contentHtml", "<p>content</p>",
				"generationMetadata", Map.of(
						"preparedMaterials", Map.of(
								"sourceReferences", List.of(
										Map.of("title", "Source one"),
										Map.of("title", "  "),
										Map.of("title", "Source two")
								)
						)
				)
		);
		when(textValueNormalizer.raw(any())).thenAnswer(invocation -> {
			Object value = invocation.getArgument(0);
			return value == null ? "" : String.valueOf(value);
		});
		when(textValueNormalizer.firstNonBlankRaw(anyString(), anyString())).thenAnswer(invocation -> {
			for (Object value : invocation.getArguments()) {
				String s = String.valueOf(value);
				if (!s.isBlank()) {
					return s;
				}
			}
			return "";
		});
		when(lessonTextUtil.compactText(anyString())).thenReturn("Compacted content");

		// When:
		String result = builder.buildTeacherVideoPrompt(lesson);

		// Then:
		assertThat(result).contains("Lesson title");
		assertThat(result).contains("Lesson description");
		assertThat(result).contains("Compacted content");
		assertThat(result).contains("Source one");
		assertThat(result).contains("Source two");
		assertThat(result).contains("Target duration: 45-60 seconds");
	}

	@Test
	void shouldBuildPromptWithoutDescriptionWhenBlankTest() {
		// Given:
		Map<String, Object> lesson = Map.of(
				"title", "Title only",
				"description", "   ",
				"contentMarkdown", "markdown content",
				"generationMetadata", Map.of()
		);
		when(textValueNormalizer.raw(any())).thenAnswer(invocation -> {
			Object value = invocation.getArgument(0);
			return value == null ? "" : String.valueOf(value);
		});
		when(textValueNormalizer.firstNonBlankRaw(anyString(), anyString())).thenAnswer(invocation -> {
			for (Object value : invocation.getArguments()) {
				String s = String.valueOf(value);
				if (!s.isBlank()) {
					return s;
				}
			}
			return "";
		});
		when(lessonTextUtil.compactText(anyString())).thenReturn("Compacted markdown");

		// When:
		String result = builder.buildTeacherVideoPrompt(lesson);

		// Then:
		assertThat(result).contains("Title only");
		assertThat(result).contains("Compacted markdown");
		assertThat(result).doesNotContain("Lesson description:");
	}

	@Test
	void shouldReturnDurationLimitSecondsTest() {
		// When:
		int result = builder.durationLimitSeconds();

		// Then:
		assertThat(result).isEqualTo(60);
	}

	@Test
	void shouldCompactTextBelowThresholdTest() {
		// Given:
		when(lessonTextUtil.compactText("short")).thenReturn("short");

		// When:
		String result = builder.truncateText("short");

		// Then:
		assertThat(result).isEqualTo("short");
	}

	@Test
	void shouldCompactTextAboveThresholdTest() {
		// Given:
		String longText = "x".repeat(7000);
		when(lessonTextUtil.compactText(longText)).thenReturn(longText);

		// When:
		String result = builder.truncateText(longText);

		// Then:
		assertThat(result).hasSize(6003);
		assertThat(result).endsWith("...");
	}
}
