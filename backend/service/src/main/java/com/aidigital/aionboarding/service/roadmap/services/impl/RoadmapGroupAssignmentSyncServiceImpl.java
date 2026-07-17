package com.aidigital.aionboarding.service.roadmap.services.impl;

import com.aidigital.aionboarding.domain.grade.entities.Grade;
import com.aidigital.aionboarding.domain.group.entities.GroupMember;
import com.aidigital.aionboarding.domain.roadmap.entities.RoadmapGroupAssignment;
import com.aidigital.aionboarding.domain.roadmap.entities.RoadmapGroupAssignmentGrade;
import com.aidigital.aionboarding.domain.user.entities.User;
import com.aidigital.aionboarding.service.group.services.entity.GroupMemberEntityService;
import com.aidigital.aionboarding.service.learning.models.RoadmapAssignmentEnrollmentRecord;
import com.aidigital.aionboarding.service.learning.services.LearningEnrollmentService;
import com.aidigital.aionboarding.service.learning.support.LearningEnrollmentSupport;
import com.aidigital.aionboarding.service.roadmap.services.RoadmapGroupAssignmentSyncService;
import com.aidigital.aionboarding.service.roadmap.services.entity.RoadmapGroupAssignmentEntityService;
import com.aidigital.aionboarding.service.user.services.entity.UserEntityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoadmapGroupAssignmentSyncServiceImpl implements RoadmapGroupAssignmentSyncService {

    private final RoadmapGroupAssignmentEntityService roadmapGroupAssignmentEntityService;
    private final GroupMemberEntityService groupMemberEntityService;
    private final UserEntityService userEntityService;
    private final LearningEnrollmentService learningEnrollmentService;
    private final LearningEnrollmentSupport learningEnrollmentSupport;

    @Override
    @Transactional
    public List<RoadmapAssignmentEnrollmentRecord> syncGroupRoadmapEnrollment(Long groupId, Long roadmapId, Set<Long> gradeIdFilter) {
        List<Long> targetUserIds = resolveMatchingMemberIds(groupId, gradeIdFilter);
        return enroll(targetUserIds, roadmapId);
    }

    @Override
    @Transactional
    public void syncNewGroupMember(Long groupId, Long memberUserId) {
        List<RoadmapGroupAssignment> assignments = roadmapGroupAssignmentEntityService.findByGroupIdIn(List.of(groupId));
        if (assignments.isEmpty()) {
            return;
        }
        Long memberGradeId = userEntityService.findById(memberUserId)
            .map(User::getGrade)
            .map(Grade::getId)
            .orElse(null);

        for (RoadmapGroupAssignment assignment : assignments) {
            Set<Long> gradeFilter = gradeIdsOf(assignment);
            if (gradeFilter.isEmpty() || (memberGradeId != null && gradeFilter.contains(memberGradeId))) {
                enroll(List.of(memberUserId), assignment.getRoadmap().getId());
            }
        }
    }

    @Override
    @Transactional
    public void syncUserGradeChange(Long userId, Long newGradeId) {
        Set<Long> groupIds = groupMemberEntityService.findGroupIdsByMemberUserId(userId);
        if (groupIds.isEmpty()) {
            return;
        }
        List<RoadmapGroupAssignment> assignments = roadmapGroupAssignmentEntityService.findByGroupIdIn(groupIds);
        for (RoadmapGroupAssignment assignment : assignments) {
            Set<Long> gradeFilter = gradeIdsOf(assignment);
            boolean matches = gradeFilter.isEmpty() || (newGradeId != null && gradeFilter.contains(newGradeId));
            if (matches) {
                enroll(List.of(userId), assignment.getRoadmap().getId());
            }
        }
    }

    List<Long> resolveMatchingMemberIds(Long groupId, Set<Long> gradeIdFilter) {
        List<GroupMember> members = gradeIdFilter == null || gradeIdFilter.isEmpty()
            ? groupMemberEntityService.findByGroupId(groupId)
            : groupMemberEntityService.findByGroupIdAndMemberGradeIdIn(groupId, gradeIdFilter);
        return members.stream().map(member -> member.getId().getMemberUserId()).toList();
    }

    Set<Long> gradeIdsOf(RoadmapGroupAssignment assignment) {
        return roadmapGroupAssignmentEntityService.findGradesByAssignmentId(assignment.getId()).stream()
            .map(RoadmapGroupAssignmentGrade::getGrade)
            .map(Grade::getId)
            .collect(Collectors.toSet());
    }

    List<RoadmapAssignmentEnrollmentRecord> enroll(List<Long> targetUserIds, Long roadmapId) {
        if (targetUserIds.isEmpty()) {
            return List.of();
        }
        var enrollmentRows = learningEnrollmentService.enrollUsersInRoadmap(targetUserIds, roadmapId);
        List<RoadmapAssignmentEnrollmentRecord> enrollments = new ArrayList<>();
        for (int i = 0; i < targetUserIds.size(); i++) {
            enrollments.add(learningEnrollmentSupport.toRoadmapAssignmentEnrollment(enrollmentRows.get(i), targetUserIds.get(i)));
        }
        return enrollments;
    }
}
