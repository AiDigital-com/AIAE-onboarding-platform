package com.aidigital.aionboarding.service.roadmap.support;

import com.aidigital.aionboarding.domain.lesson.entities.Lesson;
import com.aidigital.aionboarding.service.common.error.AppException;
import com.aidigital.aionboarding.service.common.error.ErrorReason;
import com.aidigital.aionboarding.service.common.security.AppUser;
import com.aidigital.aionboarding.service.learning.services.LearningEnrollmentService;
import com.aidigital.aionboarding.service.lesson.services.entity.LessonEntityService;
import com.aidigital.aionboarding.service.lesson.support.LessonVisibilityPolicy;
import com.aidigital.aionboarding.service.lesson.util.LessonTagUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Validates and normalizes the lesson-related inputs of a roadmap create/update request:
 * lesson-ID normalization, learnable+visible lesson lookup, and tag merging.
 */
@Component
@RequiredArgsConstructor
public class RoadmapLessonValidator {

	private final LessonEntityService lessonEntityService;
	private final LessonTagUtil lessonTagUtil;
	private final LearningEnrollmentService learningEnrollmentService;
	private final LessonVisibilityPolicy lessonVisibilityPolicy;

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
	 * Loads and validates that every requested lesson exists, is learnable — {@code READY} and
	 * either {@code PUBLISHED} (Public) or {@code PRIVATE} (assigned-only) — and is visible to
	 * {@code actor}, returning the lessons in the same order as the requested IDs. A private
	 * lesson is deliberately allowed when it is learnable and visible to the actor: the roadmap's
	 * own lesson fan-out ({@code LearningEnrollmentService#isLearnable}) already enrolls roadmap
	 * members into private lessons, so rejecting every private lesson here would make that
	 * fan-out unreachable through the API. An archived lesson is still rejected.
	 * <p>
	 * The visibility check ({@link LessonVisibilityPolicy}) exists to close a fan-out leak: lesson
	 * IDs are sequential, and without it a {@code ROADMAPS_MANAGE} holder could add another
	 * author's private lesson to a roadmap by ID — a lesson they cannot see in their own
	 * Library — and then fan it out to every roadmap assignee. The rejection uses the same
	 * "does not exist" message as a missing ID so an invisible lesson never confirms its own
	 * existence to the actor.
	 * <p>
	 * The visibility check is resolved for every requested lesson in one batched call
	 * ({@link LessonVisibilityPolicy#visibleLessonIds}), which itself issues at most one
	 * enrollment query regardless of how many lessons are checked — never once per lesson.
	 *
	 * @param actor     the authenticated user creating or updating the roadmap
	 * @param lessonIds the normalized lesson IDs to validate
	 * @return the validated lessons, ordered to match {@code lessonIds}
	 * @throws AppException {@link ErrorReason#C002} if the list is empty, any ID does not exist,
	 *                      any lesson is not learnable (not ready, or archived), or any lesson is
	 *                      not visible to {@code actor}
	 */
	public List<Lesson> validateReadyPublishedLessons(AppUser actor, List<Long> lessonIds) {
		if (lessonIds.isEmpty()) {
			throw new AppException(ErrorReason.C002, "Select at least one lesson for the roadmap.");
		}
		List<Lesson> lessons = lessonEntityService.findAllById(lessonIds);
		if (lessons.size() != lessonIds.size()) {
			throw new AppException(ErrorReason.C002, "Roadmaps can include only existing, learnable lessons.");
		}
		Set<Long> visibleLessonIds = lessonVisibilityPolicy.visibleLessonIds(actor, lessons);
		for (Lesson lesson : lessons) {
			if (!learningEnrollmentService.isLearnable(lesson) || !visibleLessonIds.contains(lesson.getId())) {
				throw new AppException(ErrorReason.C002, "Roadmaps can include only existing, learnable lessons.");
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
