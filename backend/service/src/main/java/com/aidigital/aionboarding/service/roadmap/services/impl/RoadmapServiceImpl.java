package com.aidigital.aionboarding.service.roadmap.services.impl;

import com.aidigital.aionboarding.domain.learning.entities.UserRoadmap;
import com.aidigital.aionboarding.domain.lesson.entities.Lesson;
import com.aidigital.aionboarding.domain.roadmap.entities.Roadmap;
import com.aidigital.aionboarding.domain.roadmap.entities.RoadmapLesson;
import com.aidigital.aionboarding.service.common.error.AppException;
import com.aidigital.aionboarding.service.common.error.ErrorReason;
import com.aidigital.aionboarding.service.common.security.AppUser;
import com.aidigital.aionboarding.service.common.time.CurrentTime;
import com.aidigital.aionboarding.service.permission.PermissionKeys;
import com.aidigital.aionboarding.service.permission.services.PermissionService;
import com.aidigital.aionboarding.service.roadmap.models.CreateRoadmapInput;
import com.aidigital.aionboarding.service.roadmap.models.RoadmapListQuery;
import com.aidigital.aionboarding.service.roadmap.models.RoadmapRecord;
import com.aidigital.aionboarding.service.roadmap.models.UpdateRoadmapInput;
import com.aidigital.aionboarding.service.roadmap.services.RoadmapService;
import com.aidigital.aionboarding.service.roadmap.services.entity.RoadmapEntityService;
import com.aidigital.aionboarding.service.roadmap.support.RoadmapAccessPolicy;
import com.aidigital.aionboarding.service.roadmap.support.RoadmapEnrollmentFanOutSupport;
import com.aidigital.aionboarding.service.roadmap.support.RoadmapLessonValidator;
import com.aidigital.aionboarding.service.roadmap.support.RoadmapRecordAssembler;
import com.aidigital.aionboarding.service.user.services.entity.UserEntityService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Thin orchestrator for roadmap CRUD, delegating permission/ownership checks, lesson validation,
 * record assembly, and cross-aggregate enrollment fan-out to dedicated collaborators.
 */
@Service
@RequiredArgsConstructor
public class RoadmapServiceImpl implements RoadmapService {

	private final RoadmapEntityService roadmapEntityService;
	private final RoadmapAccessPolicy roadmapAccessPolicy;
	private final RoadmapLessonValidator roadmapLessonValidator;
	private final RoadmapRecordAssembler roadmapRecordAssembler;
	private final RoadmapEnrollmentFanOutSupport roadmapEnrollmentFanOutSupport;
	private final PermissionService permissionService;
	private final UserEntityService userEntityService;
	private final CurrentTime currentTime;

	/**
	 * {@inheritDoc}
	 */
	@Override
	@Transactional(readOnly = true)
	public Page<RoadmapRecord> getAllRoadmaps(AppUser viewer, RoadmapListQuery query, int page, int size) {
		Page<Roadmap> roadmapsPage = roadmapEntityService.search(query, viewer.internalId(), page, size);
		List<Roadmap> roadmaps = roadmapsPage.getContent();
		if (roadmaps.isEmpty()) {
			return new PageImpl<>(List.of(), roadmapsPage.getPageable(), roadmapsPage.getTotalElements());
		}
		Set<Long> manageableIds = roadmapAccessPolicy.getManageableRoadmapIds(viewer, roadmaps);
		Map<Long, UserRoadmap> enrollmentsByRoadmapId =
				roadmapEnrollmentFanOutSupport.getViewerEnrollmentsByRoadmapId(viewer, roadmaps);

		// Single query for all roadmap lessons across the page's roadmaps (with lesson+status eager)
		List<Long> roadmapIds = roadmaps.stream().map(Roadmap::getId).toList();
		List<RoadmapLesson> allRoadmapLessons = roadmapEntityService.findAllByRoadmapIdsWithLessons(roadmapIds);
		Map<Long, List<RoadmapLesson>> lessonsByRoadmapId = allRoadmapLessons.stream()
				.collect(Collectors.groupingBy(rl -> rl.getId().getRoadmapId()));

		// Single query for all viewer lesson completions across the page's roadmaps
		List<Long> allLessonIds = allRoadmapLessons.stream()
				.map(rl -> rl.getId().getLessonId())
				.distinct()
				.toList();
		Map<Long, Boolean> allCompletions = roadmapRecordAssembler.lessonCompletionMap(viewer, allLessonIds);

		return roadmapsPage.map(roadmap -> roadmapRecordAssembler.toRecord(
				roadmap,
				manageableIds,
				enrollmentsByRoadmapId.get(roadmap.getId()),
				lessonsByRoadmapId.getOrDefault(roadmap.getId(), List.of()),
				allCompletions));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	@Transactional(readOnly = true)
	public long countRoadmaps(AppUser viewer, RoadmapListQuery query) {
		return roadmapEntityService.countRoadmaps(query, viewer.internalId());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	@Transactional
	public RoadmapRecord createRoadmap(AppUser viewer, CreateRoadmapInput input) {
		permissionService.requirePermission(viewer, PermissionKeys.ROADMAPS_CREATE);
		String title = input.title() == null ? "" : input.title();
		if (title.isBlank()) {
			throw new AppException(ErrorReason.C002, "Title is required.");
		}
		List<Long> lessonIds = roadmapLessonValidator.normalizeLessonIds(input.lessonIds());
		List<Lesson> lessons = roadmapLessonValidator.validateReadyPublishedLessons(lessonIds);
		List<String> tags = roadmapLessonValidator.mergeTags(input.tags(), lessons);

		Roadmap roadmap = new Roadmap();
		roadmap.setTitle(title);
		roadmap.setDescription(stringVal(input.description()));
		roadmap.setTags(tags);
		roadmap.setCreatedBy(viewer.name());
		roadmap.setAuthorUser(userEntityService.getReference(viewer.internalId()));
		LocalDateTime now = currentTime.utcDateTime();
		roadmap.setCreatedAt(now);
		roadmap.setUpdatedAt(now);
		Roadmap saved = roadmapEntityService.save(roadmap);

		saveRoadmapLessons(saved, lessons);
		return roadmapRecordAssembler.toRecord(saved, viewer, Set.of(saved.getId()), null,
				roadmapEntityService.findByIdRoadmapIdOrderBySortOrderAsc(saved.getId()));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	@Transactional
	public RoadmapRecord updateRoadmap(AppUser viewer, Long id, UpdateRoadmapInput input) {
		Roadmap roadmap = roadmapAccessPolicy.requireManageable(viewer, id);
		if (input.title().isPresent()) {
			roadmap.setTitle(stringVal(input.title().get()));
		}
		if (input.description().isPresent()) {
			roadmap.setDescription(stringVal(input.description().get()));
		}
		if (input.lessonIds().isPresent()) {
			List<Long> lessonIds = roadmapLessonValidator.normalizeLessonIds(input.lessonIds().get());
			List<Lesson> lessons = roadmapLessonValidator.validateReadyPublishedLessons(lessonIds);
			List<String> tags = roadmapLessonValidator.mergeTags(input.tags().orElse(List.of()), lessons);
			roadmap.setTags(tags);
			roadmapEntityService.deleteByIdRoadmapId(id);
			saveRoadmapLessons(roadmap, lessons);
			roadmapEnrollmentFanOutSupport.fanOutLessonsToEnrolledUsers(id, lessons);
		} else if (input.tags().isPresent()) {
			roadmap.setTags(roadmapLessonValidator.mergeTags(input.tags().get(), List.of()));
		}
		roadmap.setUpdatedAt(currentTime.utcDateTime());
		Roadmap saved = roadmapEntityService.save(roadmap);
		return roadmapRecordAssembler.toRecord(saved, viewer, Set.of(saved.getId()), null,
				roadmapEntityService.findByIdRoadmapIdOrderBySortOrderAsc(saved.getId()));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	@Transactional
	public void deleteRoadmap(AppUser viewer, Long id) {
		Roadmap roadmap = roadmapAccessPolicy.requireManageable(viewer, id);
		roadmapEntityService.delete(roadmap);
	}

	/**
	 * Persists the roadmap-lesson join rows for a roadmap's selected lessons, assigning
	 * sequential zero-indexed sort order matching list order.
	 *
	 * @param roadmap the roadmap the lessons belong to
	 * @param lessons the selected lessons, in display order
	 */
	void saveRoadmapLessons(Roadmap roadmap, List<Lesson> lessons) {
		LocalDateTime timestamp = currentTime.utcDateTime();
		for (int i = 0; i < lessons.size(); i++) {
			Lesson lesson = lessons.get(i);
			RoadmapLesson row = new RoadmapLesson();
			RoadmapLesson.RoadmapLessonId rowId = new RoadmapLesson.RoadmapLessonId();
			rowId.setRoadmapId(roadmap.getId());
			rowId.setLessonId(lesson.getId());
			row.setId(rowId);
			row.setRoadmap(roadmap);
			row.setLesson(lesson);
			row.setSortOrder(i);
			row.setCreatedAt(timestamp);
			roadmapEntityService.saveRoadmapLesson(row);
		}
	}

	String stringVal(String value) {
		return value == null ? "" : value;
	}

}
