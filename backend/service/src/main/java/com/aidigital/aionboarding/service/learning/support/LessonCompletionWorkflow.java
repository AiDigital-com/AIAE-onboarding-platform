package com.aidigital.aionboarding.service.learning.support;

import com.aidigital.aionboarding.domain.learning.entities.UserLesson;
import com.aidigital.aionboarding.domain.lesson.entities.Lesson;
import com.aidigital.aionboarding.service.common.error.AppException;
import com.aidigital.aionboarding.service.common.error.ErrorReason;
import com.aidigital.aionboarding.service.learning.models.CompletedRoadmapRecord;
import com.aidigital.aionboarding.service.learning.models.LessonEnrollmentResultRecord;
import com.aidigital.aionboarding.service.learning.services.LearningEnrollmentService;
import com.aidigital.aionboarding.service.learning.services.RoadmapEnrollmentSyncService;
import com.aidigital.aionboarding.service.learning.services.entity.LearningEnrollmentEntityService;
import com.aidigital.aionboarding.service.common.time.CurrentTime;
import com.aidigital.aionboarding.service.lesson.services.entity.LessonEntityService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Marks a learner's lesson enrollment complete or incomplete and, when marking complete,
 * determines which roadmaps became complete as a result.
 */
@Component
@RequiredArgsConstructor
public class LessonCompletionWorkflow {

    private final LessonEntityService lessonEntityService;
    private final LearningEnrollmentEntityService learningEnrollmentEntityService;
    private final LearningEnrollmentService learningEnrollmentService;
    private final RoadmapEnrollmentSyncService roadmapEnrollmentSyncService;
    private final LearningEnrollmentSupport learningEnrollmentSupport;
    private final LearningActivityCompletionPolicy learningActivityCompletionPolicy;
    private final CurrentTime currentTime;

    /**
     * Marks a lesson complete or incomplete for a learner, and when marking complete, ensures
     * every lesson activity has been passed first.
     *
     * @param userId    learner internal id
     * @param lessonId  lesson identifier
     * @param completed {@code true} to mark complete, {@code false} to clear completion
     * @return updated enrollment and any roadmaps newly completed by this change
     * @throws AppException with reason {@code C001} when the lesson is not enrollable or the
     *                       learner has no matching enrollment
     */
    public LessonEnrollmentResultRecord setLessonCompletion(Long userId, Long lessonId, boolean completed) {
        Lesson lesson = lessonEntityService.getReference(lessonId);
        if (!learningEnrollmentService.isLearnable(lesson)) {
            throw new AppException(ErrorReason.C001, lessonId);
        }

        UserLesson.UserLessonId id = learningEnrollmentSupport.userLessonId(userId, lessonId);
        UserLesson enrollment = learningEnrollmentEntityService.findUserLessonById(id)
            .orElseThrow(() -> new AppException(ErrorReason.C001, "Lesson is not in My Lessons."));

        if (completed) {
            learningActivityCompletionPolicy.ensureAllActivitiesPassed(userId, lessonId);
            enrollment.setCompletedAt(currentTime.utcDateTime());
        } else {
            enrollment.setCompletedAt(null);
        }
        learningEnrollmentEntityService.save(enrollment);

        List<CompletedRoadmapRecord> completedRoadmaps = enrollment.getCompletedAt() != null
            ? roadmapEnrollmentSyncService.getCompletedRoadmapsForUserLesson(userId, lessonId)
            : List.of();
        return new LessonEnrollmentResultRecord(
            true,
            learningEnrollmentSupport.toLessonEnrollment(enrollment),
            completedRoadmaps
        );
    }
}
