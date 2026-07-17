package com.aidigital.aionboarding.service.lessonactivity.services;

import com.aidigital.aionboarding.domain.lesson.entities.Lesson;
import com.aidigital.aionboarding.service.common.security.AppUser;
import com.aidigital.aionboarding.service.lessonactivity.models.ActivityCompletionResultRecord;
import java.util.List;
import java.util.Map;

/**
 * Persists learner activity progress, quiz attempts, and lesson completion side effects.
 */
public interface LessonActivityProgressService {

    /**
     * Clears stored progress for one activity and unmarks lesson completion when needed.
     *
     * @param viewer authenticated learner
     * @param lessonId lesson identifier
     * @param activityId activity identifier
     */
    void resetProgress(AppUser viewer, Long lessonId, Long activityId);

    /**
     * Marks a flashcards activity complete for the learner.
     *
     * @param viewer authenticated learner
     * @param lesson published ready lesson
     * @param activityId activity identifier
     * @param request flashcards completion payload
     * @return updated progress, activities, enrollment, and completion flags
     */
    ActivityCompletionResultRecord completeFlashcards(
        AppUser viewer,
        Lesson lesson,
        Long activityId,
        Map<String, Object> request
    );

    /**
     * Scores and persists a quiz attempt for the learner.
     *
     * @param viewer authenticated learner
     * @param lesson published ready lesson
     * @param activityId activity identifier
     * @param submittedAnswers learner answers in question order; each entry is the list of
     *     option(s) selected for that question
     * @return updated progress, activities, enrollment, attempt details, and completion flags
     */
    ActivityCompletionResultRecord completeQuiz(
        AppUser viewer,
        Lesson lesson,
        Long activityId,
        List<List<String>> submittedAnswers
    );
}
