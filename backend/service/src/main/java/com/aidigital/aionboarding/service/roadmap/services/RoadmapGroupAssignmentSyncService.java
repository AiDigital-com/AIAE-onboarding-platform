package com.aidigital.aionboarding.service.roadmap.services;

import com.aidigital.aionboarding.service.learning.models.RoadmapAssignmentEnrollmentRecord;

import java.util.List;
import java.util.Set;

/**
 * Keeps roadmap enrollment in sync with the dynamic, standing rules created by group roadmap
 * assignments. Every method here only ever adds enrollments — none of them remove an enrollment
 * or touch existing progress, per product requirement.
 */
public interface RoadmapGroupAssignmentSyncService {

    /**
     * Enrolls every current group member who matches the given grade filter into the roadmap.
     * Called right after a standing assignment is created or its grade filter is replaced.
     *
     * @param groupId       group whose current members are considered
     * @param roadmapId     roadmap to enroll matching members into
     * @param gradeIdFilter grade ids to narrow by; {@code null} or empty means every member
     * @return one enrollment record per member enrolled or already enrolled
     */
    List<RoadmapAssignmentEnrollmentRecord> syncGroupRoadmapEnrollment(Long groupId, Long roadmapId, Set<Long> gradeIdFilter);

    /**
     * Enrolls a newly added group member into every standing assignment of that group whose grade
     * filter the member currently matches (or which has no filter at all).
     *
     * @param groupId      group the member was just added to
     * @param memberUserId the newly added member
     */
    void syncNewGroupMember(Long groupId, Long memberUserId);

    /**
     * Enrolls a user into every standing assignment of every group they belong to whose grade
     * filter their new grade now matches (or which has no filter at all). Never removes an
     * enrollment when the new grade stops matching a filter — existing progress is kept.
     *
     * @param userId      user whose grade changed
     * @param newGradeId  the user's new grade id, or {@code null} when the grade was cleared
     */
    void syncUserGradeChange(Long userId, Long newGradeId);
}
