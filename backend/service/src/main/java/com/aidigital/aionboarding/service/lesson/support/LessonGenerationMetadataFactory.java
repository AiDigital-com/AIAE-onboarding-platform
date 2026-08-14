package com.aidigital.aionboarding.service.lesson.support;

import com.aidigital.aionboarding.service.lesson.models.CreateLessonInput;
import com.aidigital.aionboarding.service.lesson.models.LessonGenerationMetadata;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Builds the initial {@link LessonGenerationMetadata} document for a freshly created lesson
 * (draft or manual) — the one shape {@code LessonEntityService} itself assembles rather than
 * merely carrying through from a caller. Kept as its own collaborator, not a method on
 * {@code LessonEntityService}, so the JSON/map assembly this needs stays isolated at the
 * boundary converter rather than inside the service's own class.
 */
@Component
public class LessonGenerationMetadataFactory {

	private static final String META_STEP = "step";

	private static final String META_MODE = "mode";

	private static final String META_DESIRED_FORMAT = "desiredFormat";

	private static final String META_DEPTH = "depth";

	private static final String META_TONE = "tone";

	/**
	 * Builds the initial generation metadata document for a new lesson.
	 *
	 * @param input the lesson creation input
	 * @param step  the creation step label (e.g., {@code "draft"} or {@code "manual"})
	 * @param mode  the resolved creation mode name
	 * @param extra additional entries to merge in, or {@link LessonGenerationMetadata#EMPTY}
	 * @return the initial generation metadata document
	 */
	public LessonGenerationMetadata forCreation(
			CreateLessonInput input,
			String step,
			String mode,
			LessonGenerationMetadata extra
	) {
		Map<String, Object> meta = new HashMap<>();
		meta.put(META_STEP, step);
		meta.put(META_MODE, mode);
		meta.put(META_DESIRED_FORMAT, input.desiredFormat());
		meta.put(META_DEPTH, input.depth());
		meta.put(META_TONE, input.tone());
		if (extra != null) {
			meta.putAll(extra.asMap());
		}
		return new LessonGenerationMetadata(meta);
	}
}
