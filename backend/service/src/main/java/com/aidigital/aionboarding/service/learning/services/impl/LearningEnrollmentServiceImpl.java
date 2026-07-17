package com.aidigital.aionboarding.service.learning.services.impl;

import com.aidigital.aionboarding.domain.common.dictionary.LessonPublicationStatusCode;
import com.aidigital.aionboarding.domain.common.dictionary.LessonStatusCode;
import com.aidigital.aionboarding.domain.learning.entities.UserLesson;
import com.aidigital.aionboarding.domain.learning.entities.UserRoadmap;
import com.aidigital.aionboarding.domain.learning.repositories.MyLessonSummaryProjection;
import com.aidigital.aionboarding.domain.lesson.entities.Lesson;
import com.aidigital.aionboarding.domain.roadmap.entities.Roadmap;
import com.aidigital.aionboarding.domain.roadmap.entities.RoadmapLesson;
import com.aidigital.aionboarding.service.common.error.AppException;
import com.aidigital.aionboarding.service.common.error.ErrorReason;
import com.aidigital.aionboarding.service.common.security.AppUser;
import com.aidigital.aionboarding.service.common.time.CurrentTime;
import com.aidigital.aionboarding.service.learning.models.LessonEnrollmentRecord;
import com.aidigital.aionboarding.service.learning.models.MyLessonSummaryRecord;
import com.aidigital.aionboarding.service.learning.services.LearningEnrollmentService;
import com.aidigital.aionboarding.service.learning.services.entity.LearningEnrollmentEntityService;
import com.aidigital.aionboarding.service.learning.support.LearningEnrollmentSupport;
import com.aidigital.aionboarding.service.lesson.services.entity.LessonEntityService;
import com.aidigital.aionboarding.service.lessonactivity.models.LessonActivityCountsRecord;
import com.aidigital.aionboarding.service.lessonactivity.services.LessonActivityAssemblyService;
import com.aidigital.aionboarding.service.roadmap.services.entity.RoadmapEntityService;
import com.aidigital.aionboarding.service.user.services.entity.UserEntityService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LearningEnrollmentServiceImpl implements LearningEnrollmentService {

	private final LessonEntityService lessonEntityService;
	private final RoadmapEntityService roadmapEntityService;
	private final LearningEnrollmentEntityService learningEnrollmentEntityService;
	private final UserEntityService userEntityService;
	private final LearningEnrollmentSupport enrollmentSupport;
	private final LessonActivityAssemblyService lessonActivityAssemblyService;
	private final CurrentTime currentTime;

	@Override
	@Transactional(readOnly = true)
	public Page<MyLessonSummaryRecord> getMyLessons(AppUser viewer, Pageable pageable) {
		Page<MyLessonSummaryProjection> page =
				learningEnrollmentEntityService.findMyLessonsPage(viewer.internalId(), pageable);
		List<Long> lessonIds = page.getContent().stream().map(MyLessonSummaryProjection::lessonId).toList();
		Map<Long, LessonActivityCountsRecord> activityCountsByLessonId =
				lessonActivityAssemblyService.countActivitiesByLessonIds(lessonIds);
		Set<Long> lessonIdsWithTeacherVideo = lessonEntityService.findIdsWithTeacherVideoIn(lessonIds);

		return page.map(summary -> new MyLessonSummaryRecord(
				summary.lessonId(),
				summary.title(),
				summary.description(),
				summary.statusCode(),
				summary.publicationStatusCode(),
				summary.coverImageStorageKey(),
				summary.coverImageOriginalName(),
				summary.coverImageMimeType(),
				summary.contentHtmlPreview(),
				summary.contentMarkdownPreview(),
				lessonIdsWithTeacherVideo.contains(summary.lessonId()),
				summary.tags(),
				summary.createdBy(),
				summary.createdAt(),
				summary.updatedAt(),
				new LessonEnrollmentRecord(
						summary.lessonId(),
						summary.enrolledAt(),
						summary.completedAt(),
						summary.completedAt() != null
				),
				activityCountsByLessonId.getOrDefault(summary.lessonId(), new LessonActivityCountsRecord(0, 0))
		));
	}

	@Override
	@Transactional(readOnly = true)
	public LessonEnrollmentRecord getLessonEnrollment(AppUser viewer, Long lessonId) {
		return findLessonEnrollment(viewer, lessonId)
				.orElseThrow(() -> new AppException(ErrorReason.C001, "Enrollment not found."));
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<LessonEnrollmentRecord> findLessonEnrollment(AppUser viewer, Long lessonId) {
		lessonEntityService.getReference(lessonId);
		UserLesson.UserLessonId id = enrollmentSupport.userLessonId(viewer.internalId(), lessonId);
		return learningEnrollmentEntityService.findUserLessonById(id).map(enrollmentSupport::toLessonEnrollment);
	}

	@Override
	public Lesson requireEnrollableLesson(Long lessonId) {
		Lesson lesson = lessonEntityService.getReference(lessonId);
		if (!isEnrollable(lesson)) {
			throw new AppException(ErrorReason.C001, "Lesson was not found or is not ready yet.");
		}
		return lesson;
	}

	@Override
	public boolean isEnrollable(Lesson lesson) {
		return LessonStatusCode.READY.equals(lesson.getStatus().getCode())
				&& LessonPublicationStatusCode.PUBLISHED.equals(lesson.getPublicationStatus().getCode());
	}

	@Override
	public UserLesson enrollUserInLesson(Long userId, Lesson lesson, LocalDateTime enrolledAt,
										 boolean updateExisting) {
		return enrollUsersInLesson(List.of(userId), lesson, enrolledAt, updateExisting).get(0);
	}

	@Override
	public List<UserLesson> enrollUsersInLesson(
			Collection<Long> userIds,
			Lesson lesson,
			LocalDateTime enrolledAt,
			boolean updateExisting
	) {
		List<Long> targetUserIds = normalizedUserIds(userIds);
		if (targetUserIds.isEmpty()) {
			return List.of();
		}
		Map<EnrollmentKey, UserLesson> existingByKey = userLessonsByKey(targetUserIds, List.of(lesson.getId()));
		Map<Long, UserLesson> resultByUserId = new HashMap<>();
		List<UserLesson> toSave = new ArrayList<>();
		for (Long userId : targetUserIds) {
			EnrollmentKey key = new EnrollmentKey(userId, lesson.getId());
			UserLesson existing = existingByKey.get(key);
			if (existing != null) {
				if (updateExisting) {
					existing.setEnrolledAt(enrolledAt);
					toSave.add(existing);
				}
				resultByUserId.put(userId, existing);
				continue;
			}
			UserLesson row = new UserLesson();
			row.setId(enrollmentSupport.userLessonId(userId, lesson.getId()));
			row.setUser(userEntityService.getReference(userId));
			row.setLesson(lesson);
			row.setEnrolledAt(enrolledAt);
			toSave.add(row);
			resultByUserId.put(userId, row);
		}
		learningEnrollmentEntityService.saveAllUserLessons(toSave);
		return targetUserIds.stream()
				.map(resultByUserId::get)
				.toList();
	}

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
	public void unenrollUserFromLesson(Long userId, Long lessonId) {
		UserLesson.UserLessonId id = enrollmentSupport.userLessonId(userId, lessonId);
		learningEnrollmentEntityService.findUserLessonById(id).ifPresent(learningEnrollmentEntityService::delete);
	}

	@Override
	public void unenrollUsersFromLesson(Collection<Long> userIds, Long lessonId) {
		List<Long> targetUserIds = normalizedUserIds(userIds);
		if (targetUserIds.isEmpty()) {
			return;
		}
		learningEnrollmentEntityService.deleteUserLessonsByUserIdsAndLessonId(targetUserIds, lessonId);
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
				.filter(roadmapLesson -> isEnrollable(roadmapLesson.getLesson()))
				.toList();
		if (enrollableLessons.isEmpty()) {
			return;
		}
		List<Long> lessonIds = enrollableLessons.stream()
				.map(row -> row.getLesson().getId())
				.toList();
		Map<EnrollmentKey, UserLesson> existingByKey = userLessonsByKey(targetUserIds, lessonIds);
		LocalDateTime base = currentTime.utcDateTime();
		List<UserLesson> toSave = new ArrayList<>();
		for (Long userId : targetUserIds) {
			for (RoadmapLesson roadmapLesson : enrollableLessons) {
				Lesson lesson = roadmapLesson.getLesson();
				EnrollmentKey key = new EnrollmentKey(userId, lesson.getId());
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

	@Override
	@Transactional(readOnly = true)
	public Set<Long> getEnrolledLessonIds(Long userId, Collection<Long> lessonIds) {
		return learningEnrollmentEntityService.findEnrolledLessonIds(userId, lessonIds);
	}

	Map<EnrollmentKey, UserLesson> userLessonsByKey(Collection<Long> userIds, Collection<Long> lessonIds) {
		return learningEnrollmentEntityService.findUserLessonsByUserIdsAndLessonIds(userIds, lessonIds).stream()
				.collect(Collectors.toMap(
						row -> new EnrollmentKey(row.getId().getUserId(), row.getId().getLessonId()),
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

	record EnrollmentKey(Long userId, Long lessonId) {

	}

}
