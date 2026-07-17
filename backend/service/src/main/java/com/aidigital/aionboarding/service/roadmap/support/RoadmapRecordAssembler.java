package com.aidigital.aionboarding.service.roadmap.support;

import com.aidigital.aionboarding.domain.learning.entities.UserRoadmap;
import com.aidigital.aionboarding.domain.lesson.entities.Lesson;
import com.aidigital.aionboarding.domain.roadmap.entities.Roadmap;
import com.aidigital.aionboarding.domain.roadmap.entities.RoadmapLesson;
import com.aidigital.aionboarding.service.common.security.AppUser;
import com.aidigital.aionboarding.service.learning.services.entity.LearningEnrollmentEntityService;
import com.aidigital.aionboarding.service.mappers.roadmap.RoadmapMapper;
import com.aidigital.aionboarding.service.roadmap.models.RoadmapLessonRecord;
import com.aidigital.aionboarding.service.roadmap.models.RoadmapRecord;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Assembles {@link RoadmapRecord} and {@link RoadmapLessonRecord} DTOs from roadmap entities and
 * their pre-fetched (or lazily-fetched) lesson/enrollment/completion data.
 */
@Component
@RequiredArgsConstructor
public class RoadmapRecordAssembler {

    private final RoadmapMapper roadmapMapper;
    private final LearningEnrollmentEntityService learningEnrollmentEntityService;

    /**
     * Assembles a single roadmap record, lazily loading its lessons and the viewer's completion
     * map. Intended for single-roadmap call sites (create/update) where N+1 queries are not a
     * concern.
     *
     * @param roadmap       the roadmap entity
     * @param viewer        the acting user, used to compute the lesson completion map
     * @param manageableIds the set of roadmap IDs the viewer may manage, or {@code null} to mean
     *                      "can manage everything"
     * @param enrollment    the viewer's enrollment in this roadmap, or {@code null} if not enrolled
     * @param roadmapLessons the roadmap's lesson join rows, in sort order
     * @return the assembled {@link RoadmapRecord}
     */
    public RoadmapRecord toRecord(
        Roadmap roadmap,
        AppUser viewer,
        Set<Long> manageableIds,
        UserRoadmap enrollment,
        List<RoadmapLesson> roadmapLessons
    ) {
        List<Long> lessonIds = roadmapLessons.stream()
            .map(rl -> rl.getId().getLessonId())
            .toList();
        Map<Long, Boolean> completionMap = lessonCompletionMap(viewer, lessonIds);
        return toRecord(roadmap, manageableIds, enrollment, roadmapLessons, completionMap);
    }

    /**
     * Assembles a single roadmap record from pre-fetched lessons and a pre-computed completion
     * map. Intended for batch call sites (list views) where the same completion map is reused
     * across many roadmaps to avoid N+1 queries.
     *
     * @param roadmap        the roadmap entity
     * @param manageableIds  the set of roadmap IDs the viewer may manage, or {@code null} to mean
     *                       "can manage everything"
     * @param enrollment     the viewer's enrollment in this roadmap, or {@code null} if not enrolled
     * @param roadmapLessons the roadmap's lesson join rows, in sort order
     * @param allCompletions the pre-computed lesson-ID-to-completion map, shared across roadmaps
     * @return the assembled {@link RoadmapRecord}
     */
    public RoadmapRecord toRecord(
        Roadmap roadmap,
        Set<Long> manageableIds,
        UserRoadmap enrollment,
        List<RoadmapLesson> roadmapLessons,
        Map<Long, Boolean> allCompletions
    ) {
        RoadmapRecord base = roadmapMapper.toRecord(roadmap);
        List<Long> lessonIds = roadmapLessons.stream()
            .map(rl -> rl.getId().getLessonId())
            .toList();
        List<RoadmapLessonRecord> lessonRecords = roadmapLessons.stream()
            .map(rl -> toLessonRecord(rl, allCompletions))
            .toList();
        boolean isEnrolled = enrollment != null;
        LocalDateTime enrolledAt = enrollment != null ? enrollment.getEnrolledAt() : null;
        boolean viewerCanManage = manageableIds == null || manageableIds.contains(roadmap.getId());
        return new RoadmapRecord(
            base.id(), base.title(), base.description(), base.tags(),
            lessonIds, lessonRecords, isEnrolled, enrolledAt, viewerCanManage,
            base.createdBy(), base.createdAt(), base.updatedAt()
        );
    }

    /**
     * Builds a lesson-ID-to-completion-status map for the viewer, restricted to the given lesson
     * IDs. A missing entry (queried via {@link Map#getOrDefault}) means "not applicable", distinct
     * from an explicit {@code false}.
     *
     * @param viewer    the acting user
     * @param lessonIds the lesson IDs to check completion for
     * @return the lesson-ID-to-completion map; empty if the viewer has no internal ID or no
     *         lessons were requested
     */
    public Map<Long, Boolean> lessonCompletionMap(AppUser viewer, List<Long> lessonIds) {
        if (viewer.internalId() == null || lessonIds.isEmpty()) {
            return Map.of();
        }
        return learningEnrollmentEntityService.findByUserIdAndLessonIdIn(viewer.internalId(), lessonIds)
            .stream()
            .collect(Collectors.toMap(
                ul -> ul.getId().getLessonId(),
                ul -> ul.getCompletedAt() != null
            ));
    }

    /**
     * Assembles a single lesson record within a roadmap, resolving the viewer's completion status
     * for that lesson ({@code null} when not applicable, not {@code false}).
     *
     * @param roadmapLesson  the roadmap-lesson join row
     * @param completionMap  the lesson-ID-to-completion map to resolve status from
     * @return the assembled {@link RoadmapLessonRecord}
     */
    public RoadmapLessonRecord toLessonRecord(RoadmapLesson roadmapLesson, Map<Long, Boolean> completionMap) {
        Lesson lesson = roadmapLesson.getLesson();
        return new RoadmapLessonRecord(
            lesson.getId(),
            lesson.getTitle(),
            lesson.getDescription(),
            lesson.getStatus() == null ? null : lesson.getStatus().getCode(),
            lesson.getCreatedAt(),
            roadmapLesson.getSortOrder(),
            completionMap.getOrDefault(lesson.getId(), null)
        );
    }
}
