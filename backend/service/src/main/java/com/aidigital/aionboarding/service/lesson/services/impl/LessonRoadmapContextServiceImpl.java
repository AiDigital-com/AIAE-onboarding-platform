package com.aidigital.aionboarding.service.lesson.services.impl;

import com.aidigital.aionboarding.domain.lesson.entities.Lesson;
import com.aidigital.aionboarding.domain.roadmap.entities.RoadmapLesson;
import com.aidigital.aionboarding.service.common.security.AppUser;
import com.aidigital.aionboarding.service.learning.services.entity.LearningEnrollmentEntityService;
import com.aidigital.aionboarding.service.lesson.models.LessonRoadmapContextRecord;
import com.aidigital.aionboarding.service.lesson.models.MatchedRoadmapRecord;
import com.aidigital.aionboarding.service.lesson.services.LessonRoadmapContextService;
import com.aidigital.aionboarding.service.roadmap.services.entity.RoadmapEntityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LessonRoadmapContextServiceImpl implements LessonRoadmapContextService {

	private final LearningEnrollmentEntityService learningEnrollmentEntityService;
	private final RoadmapEntityService roadmapEntityService;

	/**
	 * Builds the roadmap context for a viewer reading a lesson, so Previous/Next lesson
	 * navigation works for any viewer personally enrolled in a roadmap that contains the
	 * lesson — regardless of role. Does not fall back to roadmaps the viewer has no
	 * enrollment in, since that would expose Previous/Next navigation into content the
	 * viewer was never assigned.
	 *
	 * <p>Returns {@code null} when the viewer has no enrolled roadmap containing this lesson
	 * (e.g. the lesson was assigned to them standalone).
	 *
	 * <p>This method is NOT annotated {@code @Transactional}. Each entity-service call
	 * opens its own short transaction. This preserves the no-ambient-TX invariant
	 * established in Plan 04-01 for the {@code getLesson()} call chain.
	 *
	 * @param viewer the authenticated user
	 * @param lesson the lesson being viewed
	 * @return populated context record, or {@code null}
	 */
	@Override
	public LessonRoadmapContextRecord buildContext(AppUser viewer, Lesson lesson) {
		MatchedRoadmapRecord matched = matchEnrolledRoadmap(viewer, lesson);
		if (matched == null) {
			return null;
		}

		int index = -1;
		for (int i = 0; i < matched.lessons().size(); i++) {
			if (lesson.getId().equals(matched.lessons().get(i).getId().getLessonId())) {
				index = i;
				break;
			}
		}

		if (index < 0) {
			return null;
		}

		// Load roadmap title via entity service — NOT via roadmapLesson.getRoadmap().getTitle()
		// (that LAZY proxy throws LazyInitializationException outside a transaction)
		String roadmapTitle = roadmapEntityService.findById(matched.roadmapId())
				.map(r -> r.getTitle())
				.orElse("");

		Long previousLessonId = index > 0 ? matched.lessons().get(index - 1).getId().getLessonId() : null;
		Long nextLessonId = index < matched.lessons().size() - 1
				? matched.lessons().get(index + 1).getId().getLessonId()
				: null;

		return new LessonRoadmapContextRecord(
				matched.roadmapId(),
				roadmapTitle,
				index + 1,
				matched.lessons().size(),
				previousLessonId,
				nextLessonId
		);
	}

	/**
	 * Finds the first roadmap (in enrollment order) the viewer is personally enrolled in whose
	 * roadmap-lesson rows contain the given lesson.
	 *
	 * @param viewer the authenticated user
	 * @param lesson the lesson being viewed
	 * @return the matched roadmap ID and its ordered lesson rows, or {@code null} if the viewer
	 * has no enrolled roadmap containing this lesson
	 */
	MatchedRoadmapRecord matchEnrolledRoadmap(AppUser viewer, Lesson lesson) {
		var userRoadmaps = learningEnrollmentEntityService.findUserRoadmapsByUserId(viewer.internalId());
		if (userRoadmaps.isEmpty()) {
			return null;
		}

		var roadmapIds = userRoadmaps.stream()
				.map(ur -> ur.getId().getRoadmapId())
				.toList();

		// Batch-load all roadmap lessons — avoids N+1 per roadmap
		var allRoadmapLessons = roadmapEntityService.findAllByRoadmapIdsWithLessons(roadmapIds);
		return findMatchedRoadmap(roadmapIds, allRoadmapLessons, lesson);
	}

	/**
	 * Finds the first enrolled roadmap (in enrollment order) whose roadmap-lesson rows contain
	 * the given lesson.
	 *
	 * @param roadmapIds        the viewer's enrolled roadmap primary keys, in enrollment order
	 * @param allRoadmapLessons every roadmap-lesson row across the viewer's enrolled roadmaps
	 * @param lesson            the lesson being viewed
	 * @return the matched roadmap ID and its ordered lesson rows, or {@code null} if no roadmap contains the lesson
	 */
	MatchedRoadmapRecord findMatchedRoadmap(List<Long> roadmapIds, List<RoadmapLesson> allRoadmapLessons,
											Lesson lesson) {
		// Group by roadmapId, preserving sort order returned by the query
		Map<Long, List<RoadmapLesson>> grouped = allRoadmapLessons.stream()
				.collect(Collectors.groupingBy(
						rl -> rl.getId().getRoadmapId(),
						Collectors.toList()
				));

		for (Long roadmapId : roadmapIds) {
			List<RoadmapLesson> lessons = grouped.get(roadmapId);
			if (lessons != null && lessons.stream().anyMatch(rl -> lesson.getId().equals(rl.getId().getLessonId()))) {
				return new MatchedRoadmapRecord(roadmapId, lessons);
			}
		}
		return null;
	}
}
