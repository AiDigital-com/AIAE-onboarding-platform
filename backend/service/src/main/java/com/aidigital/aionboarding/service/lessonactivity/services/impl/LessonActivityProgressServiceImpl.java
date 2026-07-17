package com.aidigital.aionboarding.service.lessonactivity.services.impl;

import com.aidigital.aionboarding.domain.common.dictionary.ActivityProgressStatusCode;
import com.aidigital.aionboarding.domain.common.dictionary.ActivityTypeCode;
import com.aidigital.aionboarding.domain.learning.entities.UserLesson;
import com.aidigital.aionboarding.domain.lesson.entities.Lesson;
import com.aidigital.aionboarding.domain.lessonactivity.entities.LessonActivity;
import com.aidigital.aionboarding.domain.lessonactivity.entities.UserLessonActivityAttempt;
import com.aidigital.aionboarding.domain.lessonactivity.entities.UserLessonActivityProgress;
import com.aidigital.aionboarding.service.common.error.AppException;
import com.aidigital.aionboarding.service.common.error.ErrorReason;
import com.aidigital.aionboarding.service.common.security.AppUser;
import com.aidigital.aionboarding.service.common.time.CurrentTime;
import com.aidigital.aionboarding.service.learning.models.LessonEnrollmentRecord;
import com.aidigital.aionboarding.service.lessonactivity.models.ActivityAttemptRecord;
import com.aidigital.aionboarding.service.lessonactivity.models.ActivityCompletionResultRecord;
import com.aidigital.aionboarding.service.lessonactivity.models.ActivityProgressRecord;
import com.aidigital.aionboarding.service.lessonactivity.models.LessonActivityRecord;
import com.aidigital.aionboarding.service.lessonactivity.models.QuizGradingResultRecord;
import com.aidigital.aionboarding.service.lessonactivity.services.LessonActivityAssemblyService;
import com.aidigital.aionboarding.service.lessonactivity.services.LessonActivityGradingService;
import com.aidigital.aionboarding.service.lessonactivity.services.LessonActivityProgressService;
import com.aidigital.aionboarding.service.lessonactivity.support.LessonActivityAccessPolicy;
import com.aidigital.aionboarding.service.lessonactivity.support.LessonActivityPayloadAssembler;
import com.aidigital.aionboarding.service.lessonactivity.support.LessonActivityProgressPersistence;
import com.aidigital.aionboarding.service.lessonactivity.support.LessonActivityRecordAssembler;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class LessonActivityProgressServiceImpl implements LessonActivityProgressService {

	private final LessonActivityPersistenceHelper lessonActivityPersistenceHelper;
	private final LessonActivityAccessPolicy accessPolicy;
	private final LessonActivityGradingService gradingService;
	private final LessonActivityAssemblyService assemblyService;
	private final LessonActivityPayloadAssembler payloadAssembler;
	private final LessonActivityRecordAssembler lessonActivityMapper;
	private final LessonActivityProgressPersistence progressPersistence;
	private final CurrentTime currentTime;

	@Override
	@Transactional
	public void resetProgress(AppUser viewer, Long lessonId, Long activityId) {
		LessonActivity activity = requireActivity(lessonId, activityId);
		UserLessonActivityProgress progress = progressPersistence.loadOrCreateProgress(
				viewer.internalId(),
				activity,
				lessonId
		);
		progress.setStatus(accessPolicy.progressStatus(ActivityProgressStatusCode.NOT_STARTED));
		progress.setScore(null);
		progress.setMetadata(new HashMap<>());
		progress.setCompletedAt(null);
		progress.setUpdatedAt(currentTime.utcDateTime());
		progressPersistence.progressRepository().save(progress);
		progressPersistence.clearLessonCompletion(viewer.internalId(), lessonId);
	}

	@Override
	@Transactional
	public ActivityCompletionResultRecord completeFlashcards(
			AppUser viewer,
			Lesson lesson,
			Long activityId,
			Map<String, Object> request
	) {
		LessonActivity activity = requireFlashcardsActivity(lesson.getId(), activityId);
		Map<String, Object> metadata = new HashMap<>();
		metadata.put("reviewedCards", payloadAssembler.parseInt(request.get("reviewedCards"), 0));
		metadata.put("completedFrom", "flashcards-player");

		UserLessonActivityProgress progress = progressPersistence.loadOrCreateProgress(
				viewer.internalId(),
				activity,
				lesson.getId()
		);
		progress.setStatus(accessPolicy.progressStatus(ActivityProgressStatusCode.COMPLETED));
		progress.setScore(null);
		progress.setMetadata(metadata);
		progress.setCompletedAt(progress.getCompletedAt() == null ? currentTime.utcDateTime() :
				progress.getCompletedAt());
		progress.setUpdatedAt(currentTime.utcDateTime());
		progressPersistence.progressRepository().save(progress);

		return buildCompletionResult(viewer, lesson, lessonActivityMapper.toProgressRecord(progress), null);
	}

	@Override
	@Transactional
	public ActivityCompletionResultRecord completeQuiz(
			AppUser viewer,
			Lesson lesson,
			Long activityId,
			List<List<String>> submittedAnswers
	) {
		LessonActivity activity = requireQuizActivity(lesson.getId(), activityId);
		QuizGradingResultRecord attempt = gradingService.gradeQuiz(activity.getPayload(), submittedAnswers);
		if (attempt.totalCount() == 0) {
			throw new AppException(ErrorReason.C002, "This quiz has no questions.");
		}

		Map<String, Object> metadata = buildQuizMetadata(attempt);
		UserLessonActivityAttempt attemptEntity = saveQuizAttempt(
				viewer,
				lesson,
				activity,
				submittedAnswers,
				attempt,
				metadata
		);

		UserLessonActivityProgress progress = progressPersistence.loadOrCreateProgress(
				viewer.internalId(),
				activity,
				lesson.getId()
		);
		progress.setStatus(accessPolicy.progressStatus(attempt.passed()
				? ActivityProgressStatusCode.COMPLETED
				: ActivityProgressStatusCode.FAILED));
		progress.setScore(BigDecimal.valueOf(attempt.score()));
		progress.setMetadata(metadata);
		progress.setCompletedAt(attempt.passed() ? currentTime.utcDateTime() : null);
		progress.setUpdatedAt(currentTime.utcDateTime());
		progressPersistence.progressRepository().save(progress);

		if (!attempt.passed()) {
			progressPersistence.clearLessonCompletion(viewer.internalId(), lesson.getId());
		}

		ActivityAttemptRecord attemptRecord = lessonActivityMapper.toAttemptRecord(
				attemptEntity,
				attempt.score(),
				attempt.passed(),
				attempt.correctCount(),
				attempt.totalCount(),
				attempt.results(),
				submittedAnswers
		);
		return buildCompletionResult(viewer, lesson, lessonActivityMapper.toProgressRecord(progress), attemptRecord);
	}

	/**
	 * Builds the response after an activity changes progress and refreshes lesson completion state.
	 */
	ActivityCompletionResultRecord buildCompletionResult(
			AppUser viewer,
			Lesson lesson,
			ActivityProgressRecord progress,
			ActivityAttemptRecord attempt
	) {
		Long lessonId = lesson.getId();
		Long lessonCreatedByUserId = lesson.getCreatedByUser() == null ? null : lesson.getCreatedByUser().getId();
		List<LessonActivityRecord> activities = accessPolicy.redactQuizAnswersUnlessManager(
				assemblyService.getLessonActivitiesForUser(lessonId, viewer.internalId()),
				viewer,
				lessonCreatedByUserId
		);
		boolean lessonCompleted = !activities.isEmpty()
				&& activities.stream().allMatch(lessonActivityMapper::isActivityPassed);
		LessonEnrollmentRecord enrollment = lessonCompleted
				? setLessonCompletionForUser(viewer.internalId(), lessonId, true)
				: getLessonEnrollmentForUser(viewer.internalId(), lessonId);

		return new ActivityCompletionResultRecord(progress, activities, lessonCompleted, enrollment, attempt);
	}

	/**
	 * Updates a user's lesson completion timestamp after verifying activity completion.
	 */
	LessonEnrollmentRecord setLessonCompletionForUser(Long userId, Long lessonId, boolean isCompleted) {
		if (isCompleted) {
			List<LessonActivityRecord> activities = assemblyService.getLessonActivitiesForUser(lessonId, userId);
			if (!activities.isEmpty() && activities.stream().anyMatch(activity -> !lessonActivityMapper.isActivityPassed(activity))) {
				throw new AppException(
						ErrorReason.C002,
						"Complete all lesson activities before marking this lesson complete."
				);
			}
		}

		UserLesson enrollment = progressPersistence.findUserLesson(userId, lessonId)
				.orElse(null);
		if (enrollment == null) {
			return null;
		}
		enrollment.setCompletedAt(isCompleted ? currentTime.utcDateTime() : null);
		progressPersistence.saveUserLesson(enrollment);
		return getLessonEnrollmentForUser(userId, lessonId);
	}

	/**
	 * Returns the current lesson enrollment record for the user when one exists.
	 */
	LessonEnrollmentRecord getLessonEnrollmentForUser(Long userId, Long lessonId) {
		return progressPersistence.findUserLesson(userId, lessonId)
				.map(lessonActivityMapper::toEnrollmentRecord)
				.orElse(null);
	}

	/**
	 * Loads an activity for the lesson or lets the persistence helper raise the domain error.
	 */
	LessonActivity requireActivity(Long lessonId, Long activityId) {
		return lessonActivityPersistenceHelper.findByLessonIdAndId(lessonId, activityId);
	}

	/**
	 * Loads a flashcards activity and rejects non-flashcards payloads.
	 */
	LessonActivity requireFlashcardsActivity(Long lessonId, Long activityId) {
		LessonActivity activity = requireActivity(lessonId, activityId);
		if (!ActivityTypeCode.FLASHCARDS.equals(activity.getType().getCode())) {
			throw new AppException(ErrorReason.C002, "Only flashcard activities can be completed with this action.");
		}
		return activity;
	}

	/**
	 * Loads a quiz activity and rejects non-quiz payloads.
	 */
	LessonActivity requireQuizActivity(Long lessonId, Long activityId) {
		LessonActivity activity = requireActivity(lessonId, activityId);
		if (!ActivityTypeCode.QUIZ.equals(activity.getType().getCode())) {
			throw new AppException(ErrorReason.C002, "Only quiz activities can be completed with this action.");
		}
		return activity;
	}

	/**
	 * Builds the metadata stored with quiz progress and attempts.
	 */
	Map<String, Object> buildQuizMetadata(QuizGradingResultRecord attempt) {
		Map<String, Object> metadata = new HashMap<>();
		metadata.put("submittedAt", currentTime.instantString());
		metadata.put("completedFrom", "quiz-player");
		metadata.put("correctCount", attempt.correctCount());
		metadata.put("totalCount", attempt.totalCount());
		metadata.put("passed", attempt.passed());
		metadata.put("results", attempt.results().stream().map(lessonActivityMapper::toQuizAnswerResultMap).toList());
		return metadata;
	}

	/**
	 * Persists one quiz attempt for the submitted answer set, retrying once if a concurrent
	 * submission for the same (user, activity) already claimed the computed attempt number.
	 */
	UserLessonActivityAttempt saveQuizAttempt(
			AppUser viewer,
			Lesson lesson,
			LessonActivity activity,
			List<List<String>> submittedAnswers,
			QuizGradingResultRecord attempt,
			Map<String, Object> metadata
	) {
		UserLessonActivityAttempt attemptEntity = new UserLessonActivityAttempt();
		attemptEntity.setUser(progressPersistence.getUserReference(viewer.internalId()));
		attemptEntity.setActivity(activity);
		attemptEntity.setLesson(lesson);
		attemptEntity.setType(activity.getType());
		attemptEntity.setScore(BigDecimal.valueOf(attempt.score()));
		attemptEntity.setPassed(attempt.passed());
		attemptEntity.setCorrectCount(attempt.correctCount());
		attemptEntity.setTotalCount(attempt.totalCount());
		attemptEntity.setSubmittedAnswers(new ArrayList<>(submittedAnswers));
		attemptEntity.setResults(new ArrayList<>(attempt.results().stream().map(lessonActivityMapper::toQuizAnswerResultMap).toList()));
		attemptEntity.setMetadata(metadata);
		attemptEntity.setCreatedAt(currentTime.utcDateTime());

		try {
			return progressPersistence.insertAttemptWithNextNumber(attemptEntity);
		} catch (DataIntegrityViolationException raceLost) {
			return progressPersistence.insertAttemptWithNextNumber(attemptEntity);
		}
	}
}
