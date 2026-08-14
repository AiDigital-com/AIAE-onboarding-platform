package com.aidigital.aionboarding.service.learning.services;

import com.aidigital.aionboarding.domain.learning.entities.UserRoadmap;
import java.util.Collection;
import java.util.List;

/**
 * Creates and updates roadmap enrollment rows for learners, including fanning out the roadmap's
 * lessons into per-lesson enrollment rows. Lesson enrollment lives in
 * {@link LearningEnrollmentService}.
 */
public interface RoadmapEnrollmentService {

    /**
     * Enrolls a user in a roadmap and fans out roadmap lesson enrollments.
     *
     * @param userId learner identifier
     * @param roadmapId roadmap identifier
     * @return persisted roadmap enrollment row
     * @throws com.aidigital.aionboarding.service.common.error.AppException when the roadmap is missing
     */
    UserRoadmap enrollUserInRoadmap(Long userId, Long roadmapId);

    /**
     * Enrolls several users in one roadmap and fans out its lessons using batch enrollment
     * lookups and saves.
     *
     * @param userIds learners to enroll
     * @param roadmapId roadmap identifier
     * @return roadmap enrollment rows ordered like {@code userIds}
     */
    List<UserRoadmap> enrollUsersInRoadmap(Collection<Long> userIds, Long roadmapId);

    /**
     * Removes a user's roadmap enrollment when present.
     *
     * @param userId learner identifier
     * @param roadmapId roadmap identifier
     */
    void unenrollUserFromRoadmap(Long userId, Long roadmapId);

    /**
     * Removes several users' roadmap enrollments and their roadmap-derived lesson enrollments
     * using set-based bulk deletes, regardless of user or roadmap-lesson count.
     *
     * @param userIds learners to revoke
     * @param roadmapId roadmap identifier
     */
    void unenrollUsersFromRoadmap(Collection<Long> userIds, Long roadmapId);
}
