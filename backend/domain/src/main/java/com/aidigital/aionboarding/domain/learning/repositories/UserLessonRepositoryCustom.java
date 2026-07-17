package com.aidigital.aionboarding.domain.learning.repositories;

import java.util.Collection;

/**
 * Criteria-API-backed queries for {@code UserLessonRepository} that a derived or JPQL
 * {@code @Query} method cannot express.
 */
public interface UserLessonRepositoryCustom {

    /**
     * Bulk-deletes the lesson enrollments a set of users gained via one roadmap's lesson list,
     * except lessons still granted by another roadmap the same user remains enrolled in.
     *
     * @param userIds   users whose roadmap-derived lesson enrollments are being revoked
     * @param roadmapId roadmap being revoked
     * @return number of deleted lesson-enrollment rows
     */
    int deleteRoadmapDerivedLessonEnrollments(Collection<Long> userIds, Long roadmapId);
}
