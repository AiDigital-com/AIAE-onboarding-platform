package com.aidigital.aionboarding.service.user.support;

import com.aidigital.aionboarding.domain.grade.entities.Grade;
import com.aidigital.aionboarding.domain.user.entities.User;
import com.aidigital.aionboarding.service.common.error.AppException;
import com.aidigital.aionboarding.service.common.error.ErrorReason;
import com.aidigital.aionboarding.service.common.security.AppUser;
import com.aidigital.aionboarding.service.common.time.CurrentTime;
import com.aidigital.aionboarding.service.grade.services.entity.GradeEntityService;
import com.aidigital.aionboarding.service.group.support.GroupAccessPolicy;
import com.aidigital.aionboarding.service.mappers.user.UserRecordMapper;
import com.aidigital.aionboarding.service.user.models.UserRecord;
import com.aidigital.aionboarding.service.user.services.UserGradeAssignmentSyncService;
import com.aidigital.aionboarding.service.user.services.entity.UserEntityService;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Sets or clears a user's grade, re-syncing standing group roadmap assignments, for
 * {@code UserServiceImpl}.
 */
@Component
@RequiredArgsConstructor
public class UserGradeUpdateSupport {

    private final UserEntityService userEntityService;
    private final GradeEntityService gradeEntityService;
    private final GroupAccessPolicy groupAccessPolicy;
    private final UserGradeAssignmentSyncService userGradeAssignmentSyncService;
    private final UserRecordMapper userMapper;
    private final CurrentTime currentTime;

    /**
     * Sets or clears a user's grade. An admin may edit any user; a Team Lead may edit only users
     * who are members of a group they lead. Changing the grade re-evaluates standing group roadmap
     * assignments so the user is enrolled into any assignment their new grade now matches; it
     * never removes an existing enrollment.
     *
     * @param viewer authenticated caller
     * @param userId user whose grade is being set
     * @param gradeId new grade id, or {@code null} to clear the grade
     * @return updated user record
     * @throws AppException when the user or grade is missing, or the caller cannot edit this
     *                       user's grade
     */
    public UserRecord updateGrade(AppUser viewer, Long userId, Long gradeId) {
        if (!groupAccessPolicy.canEditMemberGrade(viewer, userId)) {
            throw new AppException(ErrorReason.C004, "You can edit grades only for members of groups you lead.");
        }
        User user = userEntityService.findById(userId)
            .orElseThrow(() -> new AppException(ErrorReason.C001, userId));
        Long previousGradeId = user.getGrade() == null ? null : user.getGrade().getId();

        if (gradeId == null) {
            user.setGrade(null);
        } else {
            Grade grade = gradeEntityService.findById(gradeId)
                .orElseThrow(() -> new AppException(ErrorReason.C001, "Grade not found: " + gradeId));
            user.setGrade(grade);
        }
        user.setUpdatedAt(currentTime.utcDateTime());
        UserRecord updated = userMapper.toRecord(userEntityService.save(user));

        if (!Objects.equals(previousGradeId, gradeId)) {
            userGradeAssignmentSyncService.onGradeChanged(userId, gradeId);
        }
        return updated;
    }
}
