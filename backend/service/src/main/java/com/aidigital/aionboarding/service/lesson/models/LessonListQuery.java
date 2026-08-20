package com.aidigital.aionboarding.service.lesson.models;

import org.springframework.data.domain.Sort;

import java.util.List;

/**
 * Typed filter and sort parameters for a paged lesson list query.
 *
 * @param searchText free-text search matched against the lesson title; {@code null} to skip
 * @param tags restricts results to lessons tagged with every given value; {@code null} or empty to skip
 * @param statusCode restricts results to lessons in this {@code lesson_status.code}; {@code null} to skip
 * @param publicationStatusCode restricts results to lessons in this {@code lesson_publication_status.code};
 *                              {@code null} to skip
 * @param createdByUserId restricts results to lessons created by this internal user id; {@code null} to skip
 * @param assignedToMe when {@code true}, restricts results to lessons the viewer is enrolled in
 * @param readyOnly when {@code true}, restricts results to lessons whose status is ready
 * @param activityTypeCode restricts results to lessons with at least one activity of this
 *                         {@code activity_type.code}; {@code null} to skip
 * @param hasActivities restricts results by presence of any activity; {@code null} to skip
 * @param learnableOnly when {@code true}, restricts results to lessons that are ready and either
 *                      published or private - i.e. eligible for roadmap inclusion and assignment.
 *                      This narrows the caller's existing visibility restrictions further; it never
 *                      widens them, and it applies independently of {@code publicationStatusCode}
 * @param sortField whitelisted field to sort by
 * @param direction sort direction
 */
public record LessonListQuery(
    String searchText,
    List<String> tags,
    String statusCode,
    String publicationStatusCode,
    Long createdByUserId,
    Boolean assignedToMe,
    Boolean readyOnly,
    String activityTypeCode,
    Boolean hasActivities,
    Boolean learnableOnly,
    LessonSortField sortField,
    Sort.Direction direction
) {
}
