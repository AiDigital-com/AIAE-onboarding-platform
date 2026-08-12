package com.aidigital.aionboarding.service.roadmap.support;

import com.aidigital.aionboarding.domain.learning.entities.UserLesson;
import com.aidigital.aionboarding.domain.learning.entities.UserRoadmap;
import com.aidigital.aionboarding.domain.lesson.entities.Lesson;
import com.aidigital.aionboarding.domain.roadmap.entities.Roadmap;
import com.aidigital.aionboarding.service.common.security.AppUser;
import com.aidigital.aionboarding.service.common.time.CurrentTime;
import com.aidigital.aionboarding.service.learning.services.entity.LearningEnrollmentEntityService;
import com.aidigital.aionboarding.service.learning.support.LearningEnrollmentSupport;
import com.aidigital.aionboarding.service.roadmap.models.RoadmapLessonEnrollmentKey;
import com.aidigital.aionboarding.service.user.services.entity.UserEntityService;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Cross-aggregate (Roadmap + Learning-Enrollment) enrollment lookups and fan-out for
 * {@code RoadmapServiceImpl}. Kept in the service layer, composing
 * {@link LearningEnrollmentEntityService} with the {@link UserEntityService} reference for the
 * enrolling user, rather than pushed down into either entity's repository/entity-service, per
 * the architecture rule that cross-entity orchestration belongs in the service layer and must
 * not become a repository hub.
 */
@Component
@RequiredArgsConstructor
public class RoadmapEnrollmentFanOutSupport {

    private final LearningEnrollmentEntityService learningEnrollmentEntityService;
    private final UserEntityService userEntityService;
    private final LearningEnrollmentSupport learningEnrollmentSupport;
    private final CurrentTime currentTime;

    /**
     * Re-syncs a roadmap's existing enrollees to a newly-updated lesson set, enrolling each
     * enrolled user into any lesson they are not already enrolled in.
     *
     * @param roadmapId the roadmap whose lesson set changed
     * @param lessons   the roadmap's new lesson set, in display order
     */
    public void fanOutLessonsToEnrolledUsers(Long roadmapId, List<Lesson> lessons) {
        List<UserRoadmap> enrollments = learningEnrollmentEntityService.findUserRoadmapsByRoadmapId(roadmapId);
        if (enrollments.isEmpty() || lessons.isEmpty()) {
            return;
        }
        LocalDateTime base = currentTime.utcDateTime();
        List<Long> userIds = enrollments.stream()
                .map(enrollment -> enrollment.getId().getUserId())
                .toList();
        List<Long> lessonIds = lessons.stream()
                .map(Lesson::getId)
                .toList();
        Set<RoadmapLessonEnrollmentKey> existingKeys = learningEnrollmentEntityService
                .findUserLessonsByUserIdsAndLessonIds(userIds, lessonIds)
                .stream()
                .map(row -> new RoadmapLessonEnrollmentKey(row.getId().getUserId(), row.getId().getLessonId()))
                .collect(Collectors.toCollection(HashSet::new));

        List<UserLesson> missingRows = new ArrayList<>();
        for (Long userId : userIds) {
            for (int i = 0; i < lessons.size(); i++) {
                Lesson lesson = lessons.get(i);
                RoadmapLessonEnrollmentKey key = new RoadmapLessonEnrollmentKey(userId, lesson.getId());
                if (!existingKeys.contains(key)) {
                    UserLesson row = new UserLesson();
                    row.setId(learningEnrollmentSupport.userLessonId(userId, lesson.getId()));
                    row.setUser(userEntityService.getReference(userId));
                    row.setLesson(lesson);
                    row.setEnrolledAt(base.minusNanos(i * 1_000_000L));
                    missingRows.add(row);
                }
            }
        }
        learningEnrollmentEntityService.saveAllUserLessons(missingRows);
    }

    /**
     * Resolves the viewer's roadmap enrollments, keyed by roadmap ID, for the given roadmaps.
     *
     * @param viewer   the acting user
     * @param roadmaps the roadmaps being rendered
     * @return the roadmap-ID-to-enrollment map; empty if the viewer or roadmap list is absent/empty
     */
    public Map<Long, UserRoadmap> getViewerEnrollmentsByRoadmapId(AppUser viewer, List<Roadmap> roadmaps) {
        if (viewer == null || roadmaps.isEmpty()) {
            return Map.of();
        }
        List<Long> roadmapIds = roadmaps.stream()
                .map(Roadmap::getId)
                .toList();
        Map<Long, UserRoadmap> byRoadmapId = new HashMap<>();
        for (UserRoadmap enrollment :
                learningEnrollmentEntityService.findUserRoadmapsByUserIdAndRoadmapIds(viewer.internalId(),
                        roadmapIds)) {
            byRoadmapId.put(enrollment.getId().getRoadmapId(), enrollment);
        }
        return byRoadmapId;
    }
}
