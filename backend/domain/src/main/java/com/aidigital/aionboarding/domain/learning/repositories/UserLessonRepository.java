package com.aidigital.aionboarding.domain.learning.repositories;

import com.aidigital.aionboarding.domain.learning.entities.UserLesson;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UserLessonRepository
    extends JpaRepository<UserLesson, UserLesson.UserLessonId>, UserLessonRepositoryCustom {

    @Query("SELECT ul FROM UserLesson ul WHERE ul.id.userId = :userId AND ul.id.lessonId = :lessonId")
    Optional<UserLesson> findByUserIdAndLessonId(@Param("userId") Long userId, @Param("lessonId") Long lessonId);

    @Query("SELECT ul FROM UserLesson ul WHERE ul.id.userId = :userId")
    List<UserLesson> findByUserId(@Param("userId") Long userId);

    @Query("SELECT ul FROM UserLesson ul WHERE ul.id.userId = :userId AND ul.id.lessonId IN :lessonIds")
    List<UserLesson> findByUserIdAndLessonIdIn(@Param("userId") Long userId, @Param("lessonIds") List<Long> lessonIds);

    @Query("""
        SELECT ul
        FROM UserLesson ul
        JOIN FETCH ul.user
        WHERE ul.id.lessonId = :lessonId
        ORDER BY ul.enrolledAt DESC
        """)
    List<UserLesson> findByLessonIdWithUser(@Param("lessonId") Long lessonId);

    @Query("""
        SELECT ul
        FROM UserLesson ul
        WHERE ul.id.userId IN :userIds
          AND ul.id.lessonId IN :lessonIds
        """)
    List<UserLesson> findByUserIdsAndLessonIds(
        @Param("userIds") Collection<Long> userIds,
        @Param("lessonIds") Collection<Long> lessonIds
    );

    /**
     * Returns a bounded page of one user's published-lesson enrollments as a lean summary
     * projection — never the full {@link com.aidigital.aionboarding.domain.lesson.entities.Lesson}
     * entity — ordered incomplete-first then by newest enrollment date. Content fields are
     * truncated to a short preview rather than carrying the full lesson body.
     *
     * <p>Filter: {@code publicationStatus.code = 'published'} excludes private and archived lessons.
     * <p>Order: incomplete enrollments ({@code completedAt IS NULL}) first, then newest {@code enrolledAt} first.
     */
    @Query(
        value = """
            SELECT new com.aidigital.aionboarding.domain.learning.repositories.MyLessonSummaryProjection(
                l.id, l.title, l.description, l.status.code, l.publicationStatus.code,
                l.coverImageStorageKey, l.coverImageOriginalName, l.coverImageMimeType,
                SUBSTRING(l.contentHtml, 1, 500), SUBSTRING(l.contentMarkdown, 1, 500),
                l.tags, l.createdBy, l.createdAt, l.updatedAt,
                ul.enrolledAt, ul.completedAt
            )
            FROM UserLesson ul
            JOIN ul.lesson l
            JOIN l.publicationStatus ps
            WHERE ul.id.userId = :userId
              AND ps.code = 'published'
            ORDER BY
              CASE WHEN ul.completedAt IS NULL THEN 0 ELSE 1 END ASC,
              ul.enrolledAt DESC
            """,
        countQuery = """
            SELECT COUNT(ul)
            FROM UserLesson ul
            JOIN ul.lesson l
            JOIN l.publicationStatus ps
            WHERE ul.id.userId = :userId
              AND ps.code = 'published'
            """
    )
    Page<MyLessonSummaryProjection> findMyLessonsPage(@Param("userId") Long userId, Pageable pageable);

    /**
     * Bulk-deletes a lesson enrollment row for a set of users. One set-based statement
     * regardless of user count.
     *
     * @param userIds  users whose lesson enrollment is being revoked
     * @param lessonId lesson being revoked
     * @return number of deleted lesson-enrollment rows
     */
    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM UserLesson ul WHERE ul.id.userId IN :userIds AND ul.id.lessonId = :lessonId")
    int deleteByUserIdsAndLessonId(@Param("userIds") Collection<Long> userIds, @Param("lessonId") Long lessonId);
}
