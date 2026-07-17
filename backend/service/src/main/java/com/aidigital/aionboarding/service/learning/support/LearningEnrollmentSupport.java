package com.aidigital.aionboarding.service.learning.support;

import com.aidigital.aionboarding.domain.learning.entities.UserLesson;
import com.aidigital.aionboarding.domain.learning.entities.UserRoadmap;
import com.aidigital.aionboarding.domain.learning.entities.UserLesson.UserLessonId;
import com.aidigital.aionboarding.domain.learning.entities.UserRoadmap.UserRoadmapId;
import com.aidigital.aionboarding.domain.roadmap.entities.RoadmapTeamAssignment;
import com.aidigital.aionboarding.service.learning.models.LessonAssignmentEnrollmentRecord;
import com.aidigital.aionboarding.service.learning.models.LessonEnrollmentRecord;
import com.aidigital.aionboarding.service.learning.models.RoadmapAssignmentEnrollmentRecord;
import com.aidigital.aionboarding.service.learning.models.RoadmapEnrollmentRecord;
import com.aidigital.aionboarding.service.learning.models.RoadmapTeamAssignmentRecord;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class LearningEnrollmentSupport {

    public UserLessonId userLessonId(Long userId, Long lessonId) {
        UserLessonId id = new UserLessonId();
        id.setUserId(userId);
        id.setLessonId(lessonId);
        return id;
    }

    public UserRoadmapId userRoadmapId(Long userId, Long roadmapId) {
        UserRoadmapId id = new UserRoadmapId();
        id.setUserId(userId);
        id.setRoadmapId(roadmapId);
        return id;
    }

    public List<Long> normalizeUserIds(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return List.of();
        }
        Set<Long> unique = new LinkedHashSet<>();
        for (Long userId : userIds) {
            if (userId != null) {
                unique.add(userId);
            }
        }
        return List.copyOf(unique);
    }

    public LessonEnrollmentRecord toLessonEnrollment(UserLesson enrollment) {
        return new LessonEnrollmentRecord(
            enrollment.getId().getLessonId(),
            enrollment.getEnrolledAt(),
            enrollment.getCompletedAt(),
            enrollment.getCompletedAt() != null
        );
    }

    public LessonAssignmentEnrollmentRecord toLessonAssignmentEnrollment(UserLesson enrollment, Long userId) {
        return new LessonAssignmentEnrollmentRecord(
            userId,
            enrollment.getId().getLessonId(),
            enrollment.getEnrolledAt(),
            enrollment.getCompletedAt(),
            enrollment.getCompletedAt() != null
        );
    }

    public RoadmapEnrollmentRecord toRoadmapEnrollment(UserRoadmap enrollment) {
        return new RoadmapEnrollmentRecord(
            enrollment.getId().getRoadmapId(),
            enrollment.getEnrolledAt()
        );
    }

    public RoadmapAssignmentEnrollmentRecord toRoadmapAssignmentEnrollment(UserRoadmap enrollment, Long userId) {
        return new RoadmapAssignmentEnrollmentRecord(
            userId,
            enrollment.getId().getRoadmapId(),
            enrollment.getEnrolledAt()
        );
    }

    public RoadmapTeamAssignmentRecord toRoadmapTeamAssignment(RoadmapTeamAssignment assignment) {
        return new RoadmapTeamAssignmentRecord(
            assignment.getId(),
            assignment.getRoadmap().getId(),
            assignment.getLeadUser().getId(),
            assignment.getAssignedByUser() != null ? assignment.getAssignedByUser().getId() : null,
            assignment.getCreatedAt()
        );
    }
}
