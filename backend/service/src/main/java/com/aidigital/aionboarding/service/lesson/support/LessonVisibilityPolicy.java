package com.aidigital.aionboarding.service.lesson.support;

import com.aidigital.aionboarding.domain.common.dictionary.LessonPublicationStatusCode;
import com.aidigital.aionboarding.domain.lesson.entities.Lesson;
import com.aidigital.aionboarding.service.common.security.AppUser;
import com.aidigital.aionboarding.service.learning.services.entity.LearningEnrollmentEntityService;
import com.aidigital.aionboarding.service.permission.PermissionKeys;
import com.aidigital.aionboarding.service.permission.services.PermissionService;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Single implementation of the lesson viewer-visibility rule: an admin sees every lesson; a
 * {@code LESSONS_MANAGE} holder additionally sees their own-authored lessons in every
 * publication state; everyone else sees published (Public) lessons plus private
 * (assigned-only) lessons they hold an existing enrollment for. Archived lessons are never
 * visible through this policy, even to a viewer already enrolled in them.
 * <p>
 * This mirrors, and must be kept in lockstep with, the SQL predicate in
 * {@code LessonSpecificationBuilder.visibilityPredicate}, which expresses the identical rule for
 * the paged list/count query. That predicate cannot delegate here: it must stay a JPA Criteria
 * predicate so the rule reaches the derived {@code COUNT} query and pagination stays correct
 * (see {@code .claude/rules/12-database.md}, {@code 14-performance.md}). The SQL predicate and
 * this policy are therefore deliberately two implementations of one rule, not one — any change
 * to the rule must touch both.
 * <p>
 * Consumed by {@code LessonMutationSupport.canView} (direct-by-ID access),
 * {@code LessonActivityServiceImpl.canViewLesson} (activity read access), and
 * {@code RoadmapLessonValidator} (rejecting a roadmap-lesson selection the actor cannot see, so a
 * roadmap cannot be used to fan out a private lesson the actor themselves may not view).
 */
@Component
@RequiredArgsConstructor
public class LessonVisibilityPolicy {

    private final PermissionService permissionService;
    private final LearningEnrollmentEntityService learningEnrollmentEntityService;

    /**
     * Checks whether the viewer may see a single lesson.
     *
     * @param viewer authenticated viewer
     * @param lesson lesson entity to check, with {@code publicationStatus} and
     *               {@code createdByUser} initialised
     * @return {@code true} when the lesson is visible to the viewer
     */
    public boolean isVisible(AppUser viewer, Lesson lesson) {
        return visibleLessonIds(viewer, List.of(lesson)).contains(lesson.getId());
    }

    /**
     * Returns the subset of the given lessons' IDs that the viewer may see, resolving the
     * viewer's private-lesson enrollments in at most one query regardless of how many lessons
     * are checked — never once per lesson.
     *
     * @param viewer  authenticated viewer
     * @param lessons candidate lessons, each with {@code publicationStatus} and
     *                {@code createdByUser} initialised
     * @return the IDs, among {@code lessons}, that the viewer may see
     */
    public Set<Long> visibleLessonIds(AppUser viewer, Collection<Lesson> lessons) {
        if (lessons.isEmpty()) {
            return Set.of();
        }
        if (viewer.isAdmin()) {
            return lessons.stream().map(Lesson::getId).collect(Collectors.toSet());
        }
        boolean canManageLessons = permissionService.userHasPermission(viewer, PermissionKeys.LESSONS_MANAGE);
        List<Long> privateLessonIds = lessons.stream()
            .filter(lesson -> LessonPublicationStatusCode.PRIVATE.equals(lesson.getPublicationStatus().getCode()))
            .map(Lesson::getId)
            .toList();
        Set<Long> enrolledLessonIds = privateLessonIds.isEmpty()
            ? Set.of()
            : learningEnrollmentEntityService
                .findUserLessonsByUserIdsAndLessonIds(List.of(viewer.internalId()), privateLessonIds)
                .stream()
                .map(userLesson -> userLesson.getId().getLessonId())
                .collect(Collectors.toSet());

        Set<Long> visible = new HashSet<>();
        for (Lesson lesson : lessons) {
            Long authorId = lesson.getCreatedByUser() == null ? null : lesson.getCreatedByUser().getId();
            if (canManageLessons && permissionService.canManageExistingLesson(viewer, authorId)) {
                visible.add(lesson.getId());
                continue;
            }
            String publicationStatusCode = lesson.getPublicationStatus().getCode();
            if (LessonPublicationStatusCode.PUBLISHED.equals(publicationStatusCode)) {
                visible.add(lesson.getId());
            } else if (LessonPublicationStatusCode.PRIVATE.equals(publicationStatusCode)
                    && enrolledLessonIds.contains(lesson.getId())) {
                visible.add(lesson.getId());
            }
        }
        return visible;
    }
}
