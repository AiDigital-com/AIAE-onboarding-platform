package com.aidigital.aionboarding.service.lesson.support;

import com.aidigital.aionboarding.domain.common.dictionary.LessonPublicationStatusCode;
import com.aidigital.aionboarding.domain.common.dictionary.LessonStatusCode;
import com.aidigital.aionboarding.domain.lesson.entities.Lesson;
import org.springframework.stereotype.Component;

/**
 * Stateless predicate for whether a lesson is "learnable" — {@code READY} and either
 * {@code PUBLISHED} (Public) or {@code PRIVATE} (assigned-only). Used by assignment, roadmap
 * inclusion, activity submission/reset, the lesson assistant, roadmap fan-out, and completion.
 * <p>
 * This is deliberately a standalone collaborator that depends only on the {@link Lesson} entity
 * and dictionary code constants, rather than being expressed through
 * {@link com.aidigital.aionboarding.service.learning.services.LearningEnrollmentService} at every
 * call site. {@link com.aidigital.aionboarding.service.lessonactivity.support.LessonActivityAccessPolicy}
 * needs this predicate but is already a transitive dependency of
 * {@code LearningEnrollmentServiceImpl} (via {@code LessonActivityAssemblyService} →
 * {@code LessonActivityProgressPersistence}), so depending on the service interface from that
 * policy would close a Spring bean circular dependency. {@code LearningEnrollmentServiceImpl}
 * delegates its own {@code isLearnable} to this same policy, so there remains exactly one
 * implementation of the rule regardless of which side reaches it.
 */
@Component
public class LessonLearnabilityPolicy {

    /**
     * Checks whether a lesson is ready and either published (Public) or private
     * (assigned-only). A still-generating lesson is never learnable, and an archived lesson is
     * never learnable even when a viewer already holds an enrollment for it.
     *
     * @param lesson lesson entity
     * @return {@code true} when the lesson is learnable
     */
    public boolean isLearnable(Lesson lesson) {
        if (!LessonStatusCode.READY.equals(lesson.getStatus().getCode())) {
            return false;
        }
        String publicationStatusCode = lesson.getPublicationStatus().getCode();
        return LessonPublicationStatusCode.PUBLISHED.equals(publicationStatusCode)
            || LessonPublicationStatusCode.PRIVATE.equals(publicationStatusCode);
    }
}
