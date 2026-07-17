package com.aidigital.aionboarding.service.teachervideo.prompt;

import com.aidigital.aionboarding.service.common.mapping.TextValueNormalizer;
import com.aidigital.aionboarding.service.lesson.util.LessonTextUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Verbatim port of {@code heygen.js} {@code buildTeacherVideoPrompt}.
 */
@Component
@RequiredArgsConstructor
public class TeacherVideoPromptBuilder {

	private static final int TEACHER_VIDEO_MIN_SECONDS = 45;
	private static final int TEACHER_VIDEO_MAX_SECONDS = 60;
	private static final int TEACHER_VIDEO_CONTEXT_LIMIT = 6000;

	private final LessonTextUtil lessonTextUtil;
	private final TextValueNormalizer textValueNormalizer;

	/**
	 * Builds the HeyGen prompt for a lesson teacher-video generation request.
	 *
	 * @param lesson lesson detail map
	 * @return prompt text for teacher-video generation
	 */
	@SuppressWarnings("unchecked")
	public String buildTeacherVideoPrompt(Map<String, Object> lesson) {
		String lessonText = truncateText(textValueNormalizer.firstNonBlankRaw(
				textValueNormalizer.raw(lesson.get("contentHtml")),
				textValueNormalizer.raw(lesson.get("contentMarkdown"))));

		List<String> sourceNames = List.of();
		Object generationMetadata = lesson.get("generationMetadata");
		if (generationMetadata instanceof Map<?, ?> metadata) {
			Object preparedMaterials = metadata.get("preparedMaterials");
			if (preparedMaterials instanceof Map<?, ?> prepared) {
				Object sourceReferences = prepared.get("sourceReferences");
				if (sourceReferences instanceof List<?> references) {
					sourceNames = references.stream()
							.filter(Map.class::isInstance)
							.map(item -> textValueNormalizer.raw(((Map<String, Object>) item).get("title")))
							.filter(title -> !title.isBlank())
							.limit(5)
							.toList();
				}
			}
		}

		List<String> parts = new java.util.ArrayList<>();
		parts.add("Create a concise talking-head teacher video for an employee onboarding lesson.");
		parts.add("Target duration: " + TEACHER_VIDEO_MIN_SECONDS + "-" + TEACHER_VIDEO_MAX_SECONDS
				+ " seconds. Do not exceed " + TEACHER_VIDEO_MAX_SECONDS + " seconds.");
		parts.add("Format: 16:9 landscape video, full frame, clean corporate training style.");
		parts.add("Use the selected avatar as the only primary visual subject.");
		parts.add("Crop and scale the avatar naturally as a chest-up presenter who fills the landscape frame. Do not " +
				"place the avatar inside a square photo frame.");
		parts.add("Do not use black bars, pillarboxing, letterboxing, black background panels, or a boxed portrait " +
				"layout.");
		parts.add("Do not add subtitles or captions. The video should not display the spoken narration as text.");
		parts.add("In any scene where the avatar face is visible, do not add any visual elements at all: no text, " +
				"subtitles, captions, title cards, lower thirds, labels, icons, cards, charts, diagrams, stickers, " +
				"animations, or graphics.");
		parts.add("Speaking-avatar scenes must contain only the avatar and a clean background. Keep the avatar face " +
				"and upper body clearly visible.");
		parts.add("You may use separate cutaway scenes without the avatar for diagrams, process flows, simple " +
				"animations, title cards, or media-style inserts when they help explain the topic.");
		parts.add("All text or graphics must appear only in those separate no-avatar cutaway scenes.");
		parts.add("Cutaway scenes can contain more visual information than presenter shots, but should still stay " +
				"clean, readable, and focused.");
		parts.add("Visual elements must use distinct readable colors and strong contrast against the background. If " +
				"contrast is uncertain, place text on an opaque light or dark panel.");
		parts.add("Prefer a few concise key-point overlays instead of transcript-style text.");
		parts.add("Use a bright neutral office or soft studio background behind the presenter.");
		parts.add("The final video should look like a professional instructor speaking directly to camera, not a slide" +
				" deck or marketing explainer.");
		parts.add("The teacher should speak in a clear, practical, corporate training tone.");
		parts.add("Write the narration as a tight 120-145 word script.");
		parts.add("Cover only the most important learning points: what the lesson is about, 3-4 key ideas, and one " +
				"practical takeaway.");
		parts.add("Do not mention that this script was generated from source material.");
		parts.add("Use simple spoken English unless the lesson content is clearly in another language.");
		parts.add("");
		parts.add("Lesson title: " + textValueNormalizer.firstNonBlankRaw(
				textValueNormalizer.raw(lesson.get("title")), "Untitled lesson"));
		if (!textValueNormalizer.raw(lesson.get("description")).isBlank()) {
			parts.add("Lesson description: " + lesson.get("description"));
		}
		if (!sourceNames.isEmpty()) {
			parts.add("Source material names: " + String.join(", ", sourceNames));
		}
		parts.add("");
		parts.add("Lesson content:");
		parts.add(lessonText.isBlank() ? "No lesson content available." : lessonText);
		return parts.stream().filter(part -> part != null && !part.isBlank()).reduce((a, b) -> a + "\n" + b).orElse(
				"");
	}

	/**
	 * Returns the maximum requested teacher-video duration in seconds.
	 *
	 * @return maximum duration in seconds
	 */
	public int durationLimitSeconds() {
		return TEACHER_VIDEO_MAX_SECONDS;
	}

	/**
	 * Compacts and truncates lesson content to the teacher-video prompt context limit.
	 *
	 * @param value raw lesson content
	 * @return compacted prompt context
	 */
	String truncateText(String value) {
		String text = lessonTextUtil.compactText(value);
		if (text.length() <= TEACHER_VIDEO_CONTEXT_LIMIT) {
			return text;
		}
		return text.substring(0, TEACHER_VIDEO_CONTEXT_LIMIT).trim() + "...";
	}

}
