package com.aidigital.aionboarding.service.learning.services.impl;

import com.aidigital.aionboarding.domain.learning.entities.UserLesson;
import com.aidigital.aionboarding.domain.learning.entities.UserRoadmap;
import com.aidigital.aionboarding.domain.roadmap.entities.Roadmap;
import com.aidigital.aionboarding.domain.roadmap.entities.RoadmapLesson;
import com.aidigital.aionboarding.domain.lesson.entities.Lesson;
import com.aidigital.aionboarding.service.learning.models.UserLessonEnrollmentKey;
import com.aidigital.aionboarding.service.common.time.CurrentTime;
import com.aidigital.aionboarding.service.learning.services.LearningEnrollmentService;
import com.aidigital.aionboarding.service.learning.services.RoadmapEnrollmentService;
import com.aidigital.aionboarding.service.learning.services.entity.LearningEnrollmentEntityService;
import com.aidigital.aionboarding.service.learning.support.LearningEnrollmentSupport;
import com.aidigital.aionboarding.service.roadmap.services.entity.RoadmapEntityService;
import com.aidigital.aionboarding.service.user.services.entity.UserEntityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Enrolls and unenrolls learners in roadmaps, fanning out roadmap membership into per-lesson
 * enrollment rows so a roadmap assignment actually grants access to its lessons.
 */
@Service
@RequiredArgsConstructor
public class RoadmapEnrollmentServiceImpl implements RoadmapEnrollmentService {

	private final RoadmapEntityService roadmapEntityService;
	private final LearningEnrollmentEntityService learningEnrollmentEntityService;
	private final UserEntityService userEntityService;
	private final LearningEnrollmentSupport enrollmentSupport;
	private final CurrentTime currentTime;
	private final LearningEnrollmentService learningEnrollmentService;

	@Override
	public UserRoadmap enrollUserInRoadmap(Long userId, Long roadmapId) {
		return enrollUsersInRoadmap(List.of(userId), roadmapId).get(0);
	}

	@Override
	public List<UserRoadmap> enrollUsersInRoadmap(Collection<Long> userIds, Long roadmapId) {
		List<Long> targetUserIds = normalizedUserIds(userIds);
		if (targetUserIds.isEmpty()) {
			return List.of();
		}
		Roadmap roadmap = roadmapEntityService.getReference(roadmapId);
		Map<Long, UserRoadmap> existingByUserId = learningEnrollmentEntityService
				.findUserRoadmapsByUserIdsAndRoadmapId(targetUserIds, roadmap.getId())
				.stream()
				.collect(Collectors.toMap(row -> row.getId().getUserId(), Function.identity()));

		LocalDateTime enrolledAt = currentTime.utcDateTime();
		Map<Long, UserRoadmap> resultByUserId = new HashMap<>(existingByUserId);
		List<UserRoadmap> toSave = new ArrayList<>();
		for (Long userId : targetUserIds) {
			if (resultByUserId.containsKey(userId)) {
				continue;
			}
			UserRoadmap row = new UserRoadmap();
			row.setId(enrollmentSupport.userRoadmapId(userId, roadmap.getId()));
			row.setUser(userEntityService.getReference(userId));
			row.setRoadmap(roadmap);
			row.setEnrolledAt(enrolledAt);
			toSave.add(row);
			resultByUserId.put(userId, row);
		}
		learningEnrollmentEntityService.saveAllUserRoadmaps(toSave);

		fanOutRoadmapLessons(targetUserIds, roadmap.getId(), true);
		return targetUserIds.stream()
				.map(resultByUserId::get)
				.toList();
	}

	@Override
	public void unenrollUserFromRoadmap(Long userId, Long roadmapId) {
		unenrollUsersFromRoadmap(List.of(userId), roadmapId);
	}

	/**
	 * Removes several users' roadmap enrollments and the per-lesson enrollments
	 * {@link #fanOutRoadmapLessons} originally created for this roadmap, so revoking a roadmap
	 * actually removes the learner's access to its lessons. A lesson is left alone for a user
	 * when another roadmap that user is still enrolled in also contains it, so revoking one
	 * roadmap never silently breaks access granted by a different assignment. Both deletes are
	 * one set-based statement each, regardless of user or roadmap-lesson count.
	 *
	 * @param userIds   learners whose roadmap enrollment is being revoked
	 * @param roadmapId the roadmap being revoked
	 */
	@Override
	public void unenrollUsersFromRoadmap(Collection<Long> userIds, Long roadmapId) {
		List<Long> targetUserIds = normalizedUserIds(userIds);
		if (targetUserIds.isEmpty()) {
			return;
		}
		learningEnrollmentEntityService.deleteUserRoadmapsByUserIdsAndRoadmapId(targetUserIds, roadmapId);
		learningEnrollmentEntityService.deleteRoadmapDerivedLessonEnrollments(targetUserIds, roadmapId);
	}

	void fanOutRoadmapLessons(Long userId, Long roadmapId, boolean updateExistingEnrollment) {
		fanOutRoadmapLessons(List.of(userId), roadmapId, updateExistingEnrollment);
	}

	void fanOutRoadmapLessons(Collection<Long> userIds, Long roadmapId, boolean updateExistingEnrollment) {
		List<Long> targetUserIds = normalizedUserIds(userIds);
		if (targetUserIds.isEmpty()) {
			return;
		}
		List<RoadmapLesson> roadmapLessons = roadmapEntityService.findByIdRoadmapIdOrderBySortOrderAsc(roadmapId);
		List<RoadmapLesson> enrollableLessons = roadmapLessons.stream()
				.filter(roadmapLesson -> learningEnrollmentService.isLearnable(roadmapLesson.getLesson()))
				.toList();
		if (enrollableLessons.isEmpty()) {
			return;
		}
		List<Long> lessonIds = enrollableLessons.stream()
				.map(row -> row.getLesson().getId())
				.toList();
		Map<UserLessonEnrollmentKey, UserLesson> existingByKey = userLessonsByKey(targetUserIds, lessonIds);
		LocalDateTime base = currentTime.utcDateTime();
		List<UserLesson> toSave = new ArrayList<>();
		for (Long userId : targetUserIds) {
			for (RoadmapLesson roadmapLesson : enrollableLessons) {
				Lesson lesson = roadmapLesson.getLesson();
				UserLessonEnrollmentKey key = new UserLessonEnrollmentKey(userId, lesson.getId());
				LocalDateTime enrolledAt = base.minusNanos(roadmapLesson.getSortOrder() * 1_000_000L);
				UserLesson existing = existingByKey.get(key);
				if (existing != null) {
					if (updateExistingEnrollment) {
						existing.setEnrolledAt(enrolledAt);
						toSave.add(existing);
					}
					continue;
				}
				UserLesson row = new UserLesson();
				row.setId(enrollmentSupport.userLessonId(userId, lesson.getId()));
				row.setUser(userEntityService.getReference(userId));
				row.setLesson(lesson);
				row.setEnrolledAt(enrolledAt);
				toSave.add(row);
			}
		}
		learningEnrollmentEntityService.saveAllUserLessons(toSave);
	}

	Map<UserLessonEnrollmentKey, UserLesson> userLessonsByKey(Collection<Long> userIds, Collection<Long> lessonIds) {
		return learningEnrollmentEntityService.findUserLessonsByUserIdsAndLessonIds(userIds, lessonIds).stream()
				.collect(Collectors.toMap(
						row -> new UserLessonEnrollmentKey(row.getId().getUserId(), row.getId().getLessonId()),
						Function.identity()
				));
	}

	List<Long> normalizedUserIds(Collection<Long> userIds) {
		if (userIds == null || userIds.isEmpty()) {
			return List.of();
		}
		LinkedHashSet<Long> unique = new LinkedHashSet<>();
		for (Long userId : userIds) {
			if (userId != null) {
				unique.add(userId);
			}
		}
		return List.copyOf(unique);
	}

}
