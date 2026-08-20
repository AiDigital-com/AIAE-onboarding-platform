package com.aidigital.aionboarding.service.learning.services.impl;

import com.aidigital.aionboarding.domain.learning.entities.UserLesson;
import com.aidigital.aionboarding.domain.lesson.entities.Lesson;
import com.aidigital.aionboarding.service.common.error.AppException;
import com.aidigital.aionboarding.service.common.error.ErrorReason;
import com.aidigital.aionboarding.service.common.security.AppUser;
import com.aidigital.aionboarding.service.common.time.CurrentTime;
import com.aidigital.aionboarding.service.learning.models.LearningAssigneeRecord;
import com.aidigital.aionboarding.service.learning.models.LessonAssignmentEnrollmentRecord;
import com.aidigital.aionboarding.service.learning.models.LessonAssignmentResultRecord;
import com.aidigital.aionboarding.service.learning.models.LessonEnrollmentResultRecord;
import com.aidigital.aionboarding.service.learning.services.LearningEnrollmentService;
import com.aidigital.aionboarding.service.learning.services.LearningService;
import com.aidigital.aionboarding.service.learning.services.entity.LearningEnrollmentEntityService;
import com.aidigital.aionboarding.service.learning.support.LearningAssignmentAccessPolicy;
import com.aidigital.aionboarding.service.learning.support.LearningEnrollmentSupport;
import com.aidigital.aionboarding.service.learning.support.LessonCompletionWorkflow;
import com.aidigital.aionboarding.service.permission.PermissionKeys;
import com.aidigital.aionboarding.service.permission.services.PermissionService;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LearningServiceImpl implements LearningService {

    private final LearningEnrollmentEntityService learningEnrollmentEntityService;
    private final PermissionService permissionService;
    private final LearningEnrollmentService learningEnrollmentService;
    private final LearningEnrollmentSupport learningEnrollmentSupport;
    private final LearningAssignmentAccessPolicy learningAssignmentAccessPolicy;
    private final LessonCompletionWorkflow lessonCompletionWorkflow;
    private final CurrentTime currentTime;

    @Override
    @Transactional
    public LessonAssignmentResultRecord assignLesson(AppUser actor, Long lessonId, List<Long> userIds) {
        permissionService.requirePermission(actor, PermissionKeys.LEARNING_ASSIGN);
        List<Long> targetUserIds = learningEnrollmentSupport.normalizeUserIds(userIds);
        if (targetUserIds.isEmpty()) {
            throw new AppException(ErrorReason.C002, "Select at least one team member.");
        }
        learningAssignmentAccessPolicy.requireAssignableTargets(
            actor, targetUserIds, "You can assign lessons only to manageable users.");

        Lesson lesson = learningEnrollmentService.requireLearnableLesson(lessonId);
        LocalDateTime enrolledAt = currentTime.utcDateTime();
        List<UserLesson> enrollmentRows =
            learningEnrollmentService.enrollUsersInLesson(targetUserIds, lesson, enrolledAt, false);
        List<LessonAssignmentEnrollmentRecord> enrollments = new ArrayList<>();
        for (int i = 0; i < targetUserIds.size(); i++) {
            enrollments.add(learningEnrollmentSupport.toLessonAssignmentEnrollment(enrollmentRows.get(i), targetUserIds.get(i)));
        }

        return new LessonAssignmentResultRecord(true, enrollments);
    }

    /**
     * Lists the assignees for a lesson, restricted to the subset of enrollees the actor may
     * actually manage (every user but themselves for an admin, their own team's members for a
     * team lead). {@code LEARNING_ASSIGN} alone does not tie the actor to this specific lesson,
     * and since Phase B made lessons genuinely private, an unfiltered roster here would disclose
     * the names, emails, and enrollment/completion state of every enrollee of an arbitrary lesson
     * id to any assign-permission holder — the same class of leak fixed for
     * {@code RoadmapAssignmentServiceImpl.listRoadmapAssignees}.
     */
    @Override
    @Transactional(readOnly = true)
    public List<LearningAssigneeRecord> listLessonAssignees(AppUser actor, Long lessonId) {
        permissionService.requirePermission(actor, PermissionKeys.LEARNING_ASSIGN);
        learningEnrollmentService.requireLearnableLesson(lessonId);
        Set<Long> assignableUserIds = learningAssignmentAccessPolicy.assignableUserIds(actor);
        return learningEnrollmentEntityService.findByLessonIdWithUser(lessonId).stream()
            .filter(enrollment -> assignableUserIds.contains(enrollment.getId().getUserId()))
            .map(enrollment -> new LearningAssigneeRecord(
                enrollment.getId().getUserId(),
                enrollment.getUser().getName(),
                enrollment.getUser().getEmail(),
                enrollment.getEnrolledAt(),
                enrollment.getCompletedAt() != null
            ))
            .toList();
    }

    @Override
    @Transactional
    public void revokeLessonAssignments(AppUser actor, Long lessonId, List<Long> userIds) {
        permissionService.requirePermission(actor, PermissionKeys.LEARNING_ASSIGN);
        List<Long> targetUserIds = learningEnrollmentSupport.normalizeUserIds(userIds);
        if (targetUserIds.isEmpty()) {
            throw new AppException(ErrorReason.C002, "Select at least one team member.");
        }
        learningAssignmentAccessPolicy.requireAssignableTargets(
            actor, targetUserIds, "You can revoke lesson assignments only for manageable users.");
        learningEnrollmentService.requireLearnableLesson(lessonId);
        learningEnrollmentService.unenrollUsersFromLesson(targetUserIds, lessonId);
    }

    @Override
    @Transactional
    public LessonEnrollmentResultRecord enrollLesson(AppUser user, Long lessonId) {
        permissionService.requirePermission(user, PermissionKeys.LEARNING_ENROLL);
        Lesson lesson = learningEnrollmentService.requireSelfEnrollableLesson(lessonId);
        UserLesson enrollment = learningEnrollmentService.enrollUserInLesson(
            user.internalId(),
            lesson,
            currentTime.utcDateTime(),
            false
        );

        return new LessonEnrollmentResultRecord(
            true,
            learningEnrollmentSupport.toLessonEnrollment(enrollment),
            List.of()
        );
    }

    @Override
    @Transactional
    public void unenrollLesson(AppUser user, Long lessonId) {
        permissionService.requirePermission(user, PermissionKeys.LEARNING_ENROLL);
        learningEnrollmentService.unenrollUserFromLesson(user.internalId(), lessonId);
    }

    @Override
    @Transactional
    public LessonEnrollmentResultRecord setLessonCompletion(AppUser user, Long lessonId, boolean completed) {
        permissionService.requirePermission(user, PermissionKeys.LEARNING_COMPLETE);
        return lessonCompletionWorkflow.setLessonCompletion(user.internalId(), lessonId, completed);
    }
}
