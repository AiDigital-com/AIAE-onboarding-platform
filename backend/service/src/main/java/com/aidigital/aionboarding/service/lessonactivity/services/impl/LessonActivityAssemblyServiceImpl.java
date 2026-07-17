package com.aidigital.aionboarding.service.lessonactivity.services.impl;

import com.aidigital.aionboarding.domain.common.dictionary.ActivityTypeCode;
import com.aidigital.aionboarding.domain.lessonactivity.entities.LessonActivity;
import com.aidigital.aionboarding.domain.lessonactivity.entities.UserLessonActivityProgress;
import com.aidigital.aionboarding.domain.lessonactivity.repositories.LessonActivityTypeCountProjection;
import com.aidigital.aionboarding.service.lessonactivity.models.ActivityAttemptRecord;
import com.aidigital.aionboarding.service.lessonactivity.models.LessonActivityCountsRecord;
import com.aidigital.aionboarding.service.lessonactivity.models.LessonActivityRecord;
import com.aidigital.aionboarding.service.lessonactivity.services.LessonActivityAssemblyService;
import com.aidigital.aionboarding.service.lessonactivity.support.LessonActivityProgressPersistence;
import com.aidigital.aionboarding.service.lessonactivity.support.LessonActivityRecordAssembler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LessonActivityAssemblyServiceImpl implements LessonActivityAssemblyService {

	private final LessonActivityPersistenceHelper lessonActivityPersistenceHelper;
	private final LessonActivityProgressPersistence progressPersistence;
	private final LessonActivityRecordAssembler lessonActivityMapper;

	@Override
	public List<LessonActivityRecord> getLessonActivities(Long lessonId) {
		return sortActivities(lessonActivityPersistenceHelper.findByLessonIdOrderByCreatedAtAsc(lessonId)).stream()
				.map(activity -> lessonActivityMapper.toActivityRecord(activity, null))
				.toList();
	}

	@Override
	public List<LessonActivityRecord> getLessonActivitiesForUser(Long lessonId, Long userId) {
		List<LessonActivity> activities =
				sortActivities(lessonActivityPersistenceHelper.findByLessonIdOrderByCreatedAtAsc(lessonId));
		Map<Long, UserLessonActivityProgress> progressByActivityId = progressPersistence
				.findByUserIdAndLessonId(userId, lessonId)
				.stream()
				.collect(Collectors.toMap(progress -> progress.getActivity().getId(), Function.identity()));

		return activities.stream()
				.map(activity -> lessonActivityMapper.toActivityRecord(activity,
						progressByActivityId.get(activity.getId())))
				.toList();
	}

	@Override
	public Map<Long, List<LessonActivityRecord>> getLessonActivitiesForUserByLessonIds(Long userId,
																					   Collection<Long> lessonIds) {
		if (lessonIds == null || lessonIds.isEmpty()) {
			return Map.of();
		}
		List<LessonActivity> activities =
				lessonActivityPersistenceHelper.findByLessonIdsOrderByCreatedAtAsc(lessonIds);
		Map<Long, UserLessonActivityProgress> progressByActivityId = progressPersistence
				.findByUserIdAndLessonIds(userId, lessonIds)
				.stream()
				.collect(Collectors.toMap(progress -> progress.getActivity().getId(), Function.identity()));

		Map<Long, List<LessonActivityRecord>> activitiesByLessonId = new HashMap<>();
		activities.stream()
				.collect(Collectors.groupingBy(activity -> activity.getLesson().getId(), LinkedHashMap::new,
						Collectors.toList()))
				.forEach((lessonId, lessonActivities) -> activitiesByLessonId.put(
						lessonId,
						sortActivities(lessonActivities).stream()
								.map(activity -> lessonActivityMapper.toActivityRecord(activity,
										progressByActivityId.get(activity.getId())))
								.toList()
				));
		return activitiesByLessonId;
	}

	@Override
	public Map<Long, LessonActivityCountsRecord> countActivitiesByLessonIds(Collection<Long> lessonIds) {
		if (lessonIds == null || lessonIds.isEmpty()) {
			return Map.of();
		}
		Map<Long, Map<String, Long>> countsByLessonIdAndType = new LinkedHashMap<>();
		for (LessonActivityTypeCountProjection row :
				lessonActivityPersistenceHelper.countByLessonIdsGroupedByType(lessonIds)) {
			countsByLessonIdAndType
					.computeIfAbsent(row.getLessonId(), key -> new HashMap<>())
					.put(row.getTypeCode(), row.getActivityCount());
		}
		Map<Long, LessonActivityCountsRecord> result = new LinkedHashMap<>();
		countsByLessonIdAndType.forEach((lessonId, countsByType) -> result.put(lessonId,
				new LessonActivityCountsRecord(
				countsByType.getOrDefault(ActivityTypeCode.FLASHCARDS, 0L).intValue(),
				countsByType.getOrDefault(ActivityTypeCode.QUIZ, 0L).intValue()
		)));
		return result;
	}

	@Override
	public LessonActivityRecord getLessonActivity(Long lessonId, Long activityId, Long userId) {
		return lessonActivityPersistenceHelper.findOptionalByLessonIdAndId(lessonId, activityId)
				.map(activity -> lessonActivityMapper.toActivityRecord(
						activity,
						progressPersistence.findByUserIdAndActivityId(userId, activityId).orElse(null)
				))
				.orElse(null);
	}

	@Override
	public List<ActivityAttemptRecord> getAttemptsForActivity(Long lessonId, Long activityId, Long userId) {
		return progressPersistence.attemptRepository()
				.findByUserIdAndLessonIdAndActivityIdOrderByAttemptNumberDesc(userId, lessonId, activityId)
				.stream()
				.map(lessonActivityMapper::toAttemptRecord)
				.toList();
	}

	List<LessonActivity> sortActivities(List<LessonActivity> activities) {
		return activities.stream()
				.sorted(Comparator
						.comparingInt((LessonActivity activity) -> activityTypeSortOrder(activity.getType().getCode()))
						.thenComparing(LessonActivity::getCreatedAt))
				.toList();
	}

	int activityTypeSortOrder(String type) {
		if (ActivityTypeCode.FLASHCARDS.equals(type)) {
			return 0;
		}
		if (ActivityTypeCode.QUIZ.equals(type)) {
			return 1;
		}
		return 2;
	}
}
