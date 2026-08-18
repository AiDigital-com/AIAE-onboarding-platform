package com.aidigital.aionboarding.service.lesson.models;

import java.util.Map;

/**
 * Typed boundary wrapper around the JSON document persisted at
 * {@code Lesson.generationMetadata}.
 * <p>
 * The document's shape is genuinely dynamic: draft/manual creation, each generation attempt,
 * each failure, and each revision cycle contribute a different set of keys over the lesson's
 * lifetime (provider/model/prompt version, prepared-materials and attached-file snapshots,
 * failure timestamps, {@code lastRevision*} fields, the full {@code revisionHistory} array, ...),
 * assembled by {@code GenerationMetadataAssembler}, {@code LessonGenerationMetadataFactory} and
 * {@code LessonRevisionMetadataMapper} in different combinations per call site. A fixed-shape
 * record would either omit fields those assemblers legitimately add, or have to grow every time a
 * new generation/revision flow adds a key. This wrapper gives the JSON document a distinct type
 * at the {@code LessonEntityService} boundary — so its public methods never expose a bare
 * {@code Map<String, Object>} — while {@link #asMap()} is the single, explicit conversion point
 * back to the shape the {@code Lesson} entity's {@code @JdbcTypeCode(SqlTypes.JSON)} column
 * actually persists.
 *
 * @param values the underlying JSON-shaped metadata entries, never {@code null}
 */
public record LessonGenerationMetadata(Map<String, Object> values) {

	/**
	 * Shared empty instance for call sites that have no metadata to attach yet.
	 */
	public static final LessonGenerationMetadata EMPTY = new LessonGenerationMetadata(Map.of());

	/**
	 * Normalizes a {@code null} argument to an empty map so every instance carries a non-null
	 * document, matching {@link #EMPTY}.
	 *
	 * @param values the raw metadata entries, or {@code null} for an empty document
	 */
	public LessonGenerationMetadata(Map<String, Object> values) {
		this.values = values == null ? Map.of() : values;
	}

	/**
	 * Returns the underlying JSON-shaped map, ready to persist on
	 * {@code Lesson.generationMetadata}.
	 *
	 * @return the metadata entries backing this document
	 */
	public Map<String, Object> asMap() {
		return values;
	}
}
