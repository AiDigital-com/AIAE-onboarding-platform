package com.aidigital.aionboarding.service.learning.support;

import com.aidigital.aionboarding.domain.common.dictionary.ActivityTypeCode;
import com.aidigital.aionboarding.domain.lessonactivity.entities.LessonActivity;
import com.aidigital.aionboarding.domain.lessonactivity.entities.UserLessonActivityProgress;
import com.aidigital.aionboarding.domain.lessonactivity.repositories.LessonActivityRepository;
import com.aidigital.aionboarding.domain.lessonactivity.repositories.UserLessonActivityProgressRepository;
import com.aidigital.aionboarding.service.common.error.AppException;
import com.aidigital.aionboarding.service.common.error.ErrorReason;
import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LearningActivityCompletionPolicy {

    private static final int QUIZ_PASS_SCORE = 80;

    private final LessonActivityRepository lessonActivityRepository;
    private final UserLessonActivityProgressRepository activityProgressRepository;

    public void ensureAllActivitiesPassed(Long userId, Long lessonId) {
        List<LessonActivity> activities = lessonActivityRepository.findByLessonIdOrderByCreatedAtAsc(lessonId);
        if (activities.isEmpty()) {
            return;
        }
        for (LessonActivity activity : activities) {
            if (!isActivityPassed(activity, findProgress(userId, activity.getId()))) {
                throw new AppException(
                    ErrorReason.C002,
                    "Complete all lesson activities before marking this lesson complete."
                );
            }
        }
    }

    UserLessonActivityProgress findProgress(Long userId, Long activityId) {
        UserLessonActivityProgress.UserLessonActivityProgressId id =
            new UserLessonActivityProgress.UserLessonActivityProgressId();
        id.setUserId(userId);
        id.setActivityId(activityId);
        return activityProgressRepository.findById(id).orElse(null);
    }

    boolean isActivityPassed(LessonActivity activity, UserLessonActivityProgress progress) {
        if (progress == null || progress.getCompletedAt() == null) {
            return false;
        }
        if (ActivityTypeCode.QUIZ.equals(activity.getType().getCode())) {
            BigDecimal score = progress.getScore();
            return score != null && score.intValue() >= QUIZ_PASS_SCORE;
        }
        return true;
    }
}
