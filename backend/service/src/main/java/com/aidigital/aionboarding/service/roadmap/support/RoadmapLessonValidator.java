package com.aidigital.aionboarding.service.roadmap.support;

import com.aidigital.aionboarding.domain.common.dictionary.LessonPublicationStatusCode;
import com.aidigital.aionboarding.domain.common.dictionary.LessonStatusCode;
import com.aidigital.aionboarding.domain.lesson.entities.Lesson;
import com.aidigital.aionboarding.service.common.error.AppException;
import com.aidigital.aionboarding.service.common.error.ErrorReason;
import com.aidigital.aionboarding.service.lesson.services.entity.LessonEntityService;
import com.aidigital.aionboarding.service.lesson.util.LessonTagUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Validates and normalizes the lesson-related inputs of a roadmap create/update request:
 * lesson-ID normalization, ready+published lesson lookup, and tag merging.
 */
@Component
@RequiredArgsConstructor
public class RoadmapLessonValidator {

	private final LessonEntityService lessonEntityService;
	private final LessonTagUtil lessonTagUtil;

	/**
	 * Normalizes a raw lesson-ID list: {@code null} becomes empty, nulls are filtered out, and
	 * duplicates are removed.
	 *
	 * @param lessonIds the raw lesson IDs from the request
	 * @return the normalized, deduplicated lesson-ID list
	 */
	public List<Long> normalizeLessonIds(List<Long> lessonIds) {
		if (lessonIds == null) {
			return List.of();
		}
		return lessonIds.stream().filter(Objects::nonNull).distinct().toList();
	}

	/**
	 * Loads and validates that every requested lesson exists, is {@code READY}, and is
	 * {@code PUBLISHED}, returning the lessons in the same order as the requested IDs.
	 *
	 * @param lessonIds the normalized lesson IDs to validate
	 * @return the validated lessons, ordered to match {@code lessonIds}
	 * @throws AppException {@link ErrorReason#C002} if the list is empty, any ID does not exist,
	 *                      or any lesson is not ready and published
	 */
	public List<Lesson> validateReadyPublishedLessons(List<Long> lessonIds) {
		if (lessonIds.isEmpty()) {
			throw new AppException(ErrorReason.C002, "Select at least one lesson for the roadmap.");
		}
		List<Lesson> lessons = lessonEntityService.findAllById(lessonIds);
		if (lessons.size() != lessonIds.size()) {
			throw new AppException(ErrorReason.C002, "Roadmaps can include only existing published ready lessons.");
		}
		for (Lesson lesson : lessons) {
			if (!LessonStatusCode.READY.equals(lesson.getStatus().getCode())
					|| !LessonPublicationStatusCode.PUBLISHED.equals(lesson.getPublicationStatus().getCode())) {
				throw new AppException(ErrorReason.C002, "Roadmaps can include only existing published ready lessons" +
						".");
			}
		}
		Map<Long, Lesson> byId = lessons.stream().collect(Collectors.toMap(Lesson::getId, lesson -> lesson));
		return lessonIds.stream().map(byId::get).toList();
	}

	/**
	 * Merges the input tags with every selected lesson's own tags, normalizing the combined list.
	 *
	 * @param inputTags the roadmap's own requested tags
	 * @param lessons   the roadmap's selected lessons, whose tags are folded in as well
	 * @return the normalized, merged tag list
	 */
	public List<String> mergeTags(List<String> inputTags, List<Lesson> lessons) {
		List<Object> all = new ArrayList<>();
		if (inputTags != null) {
			all.addAll(inputTags);
		}
		for (Lesson lesson : lessons) {
			if (lesson.getTags() != null) {
				all.addAll(lesson.getTags());
			}
		}
		return lessonTagUtil.normalizeLessonTagInput(all);
	}
}
