package com.aidigital.aionboarding.service.lessonactivity.support;

import com.aidigital.aionboarding.domain.lesson.entities.Lesson;
import com.aidigital.aionboarding.domain.lessonactivity.entities.LessonActivity;
import com.aidigital.aionboarding.domain.lessonactivity.entities.UserLessonActivityAttempt;
import com.aidigital.aionboarding.domain.lessonactivity.entities.UserLessonActivityProgress;
import com.aidigital.aionboarding.domain.learning.entities.UserLesson;
import com.aidigital.aionboarding.service.learning.models.LessonEnrollmentRecord;
import com.aidigital.aionboarding.service.lessonactivity.enums.QuizQuestionType;
import com.aidigital.aionboarding.service.lessongen.model.LessonGenPrompt;
import com.aidigital.aionboarding.service.lessonactivity.models.ActivityAttemptRecord;
import com.aidigital.aionboarding.service.lessonactivity.models.ActivityProgressRecord;
import com.aidigital.aionboarding.service.lessonactivity.models.ActivityProgressViewRecord;
import com.aidigital.aionboarding.service.lessonactivity.models.ActivityPromptRecord;
import com.aidigital.aionboarding.service.lessonactivity.models.LessonActivityRecord;
import com.aidigital.aionboarding.service.lessonactivity.models.LessonWithActivitiesRecord;
import com.aidigital.aionboarding.service.lessonactivity.models.QuizAnswerResultRecord;
import org.springframework.stereotype.Component;

import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class LessonActivityRecordAssembler {

    public LessonActivityRecord toActivityRecord(LessonActivity activity, UserLessonActivityProgress progress) {
        return new LessonActivityRecord(
            activity.getId(),
            activity.getLesson().getId(),
            activity.getType().getCode(),
            activity.getTitle(),
            activity.getItemCount(),
            activity.getPayload(),
            activity.getGenerationMetadata(),
            activity.getCreatedBy(),
            activity.getCreatedAt(),
            progress == null ? null : toProgressViewRecord(progress)
        );
    }

    public ActivityProgressViewRecord toProgressViewRecord(UserLessonActivityProgress progress) {
        return new ActivityProgressViewRecord(
            progress.getStatus().getCode(),
            progress.getScore() == null ? null : progress.getScore().setScale(0, RoundingMode.HALF_UP).intValue(),
            progress.getMetadata(),
            progress.getStartedAt(),
            progress.getCompletedAt(),
            progress.getCompletedAt() != null
        );
    }

    public ActivityProgressRecord toProgressRecord(UserLessonActivityProgress progress) {
        return new ActivityProgressRecord(
            progress.getActivity().getId(),
            progress.getLesson().getId(),
            progress.getStatus().getCode(),
            progress.getScore() == null ? null : progress.getScore().setScale(0, RoundingMode.HALF_UP).intValue(),
            progress.getCompletedAt(),
            progress.getMetadata()
        );
    }

    public ActivityAttemptRecord toAttemptRecord(
        UserLessonActivityAttempt attemptEntity,
        int score,
        boolean passed,
        int correctCount,
        int totalCount,
        List<QuizAnswerResultRecord> results,
        List<List<String>> submittedAnswers
    ) {
        return new ActivityAttemptRecord(
            attemptEntity.getId(),
            attemptEntity.getUser().getId(),
            attemptEntity.getLesson().getId(),
            attemptEntity.getActivity().getId(),
            attemptEntity.getType().getCode(),
            attemptEntity.getAttemptNumber(),
            score,
            passed,
            correctCount,
            totalCount,
            submittedAnswers,
            results,
            attemptEntity.getMetadata(),
            attemptEntity.getCreatedAt()
        );
    }

    public ActivityAttemptRecord toAttemptRecord(UserLessonActivityAttempt attempt) {
        return new ActivityAttemptRecord(
            attempt.getId(),
            attempt.getUser().getId(),
            attempt.getLesson().getId(),
            attempt.getActivity().getId(),
            attempt.getType().getCode(),
            attempt.getAttemptNumber(),
            attempt.getScore() == null ? 0 : attempt.getScore().setScale(0, RoundingMode.HALF_UP).intValue(),
            Boolean.TRUE.equals(attempt.getPassed()),
            attempt.getCorrectCount(),
            attempt.getTotalCount(),
            stringListList(attempt.getSubmittedAnswers()),
            toQuizAnswerResults(attempt.getResults()),
            attempt.getMetadata(),
            attempt.getCreatedAt()
        );
    }

    public ActivityPromptRecord toPromptRecord(LessonGenPrompt prompt) {
        return new ActivityPromptRecord(
            prompt.version(),
            prompt.cacheKey(),
            prompt.instructions(),
            prompt.input()
        );
    }

    public LessonWithActivitiesRecord toLessonWithActivitiesRecord(Lesson lesson, List<LessonActivityRecord> activities) {
        return new LessonWithActivitiesRecord(
            lesson.getId(),
            lesson.getTitle(),
            lesson.getDescription(),
            lesson.getStatus().getCode(),
            lesson.getPublicationStatus().getCode(),
            lesson.getContentMarkdown(),
            lesson.getContentHtml(),
            activities
        );
    }

    public LessonEnrollmentRecord toEnrollmentRecord(UserLesson enrollment) {
        if (enrollment == null) {
            return null;
        }
        return new LessonEnrollmentRecord(
            enrollment.getLesson().getId(),
            enrollment.getEnrolledAt(),
            enrollment.getCompletedAt(),
            enrollment.getCompletedAt() != null
        );
    }

    public List<QuizAnswerResultRecord> toQuizAnswerResults(List<Object> results) {
        if (results == null) {
            return List.of();
        }
        return results.stream()
            .filter(result -> result instanceof Map)
            .map(result -> toQuizAnswerResult((Map<String, Object>) result))
            .toList();
    }

    public QuizAnswerResultRecord toQuizAnswerResult(Map<String, Object> result) {
        return new QuizAnswerResultRecord(
            QuizQuestionType.fromValue(stringVal(result.get("type"))).value(),
            stringVal(result.get("question")),
            stringList(result.get("options")),
            stringList(result.get("selectedAnswers")),
            stringList(result.get("correctAnswers")),
            Boolean.TRUE.equals(result.get("isCorrect")),
            stringVal(result.get("explanation"))
        );
    }

    public Map<String, Object> toQuizAnswerResultMap(QuizAnswerResultRecord result) {
        Map<String, Object> map = new HashMap<>();
        map.put("type", result.type());
        map.put("question", result.question());
        map.put("options", result.options());
        map.put("selectedAnswers", result.selectedAnswers());
        map.put("correctAnswers", result.correctAnswers());
        map.put("isCorrect", result.isCorrect());
        map.put("explanation", result.explanation());
        return map;
    }

    public boolean isActivityPassed(LessonActivityRecord activity) {
        String type = activity.type();
        ActivityProgressViewRecord progress = activity.progress();
        if (progress == null || progress.completedAt() == null) {
            return false;
        }
        if ("quiz".equals(type)) {
            Integer score = progress.score();
            return score != null && score >= 80;
        }
        return true;
    }

    @SuppressWarnings("unchecked")
    List<String> stringList(Object value) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        List<String> strings = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof String stringValue) {
                strings.add(stringValue);
            }
        }
        return strings;
    }

    /**
     * Converts a raw JSONB value back into a list of string lists, one per quiz question.
     *
     * @param value raw persisted value
     * @return normalized list of string lists; non-list elements are dropped
     */
    List<List<String>> stringListList(Object value) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        List<List<String>> result = new ArrayList<>();
        for (Object item : list) {
            result.add(stringList(item));
        }
        return result;
    }

    String stringVal(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
