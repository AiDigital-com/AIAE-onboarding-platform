package com.aidigital.aionboarding.service.lesson.services;

import com.aidigital.aionboarding.domain.lesson.entities.Lesson;
import com.aidigital.aionboarding.service.common.security.AppUser;
import com.aidigital.aionboarding.service.lesson.models.LessonRoadmapContextRecord;

/**
 * Resolves the enrolled-roadmap navigation context (title, position, previous/next lesson)
 * for a learner viewing a lesson.
 */
public interface LessonRoadmapContextService {

    /**
     * Builds the roadmap context for an enrolled learner viewing a lesson.
     *
     * @param viewer the authenticated user
     * @param lesson the lesson being viewed
     * @return populated context record, or {@code null} when the viewer has no applicable roadmap context
     */
    LessonRoadmapContextRecord buildContext(AppUser viewer, Lesson lesson);
}
