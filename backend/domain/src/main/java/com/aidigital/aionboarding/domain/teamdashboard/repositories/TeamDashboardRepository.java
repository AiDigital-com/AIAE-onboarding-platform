package com.aidigital.aionboarding.domain.teamdashboard.repositories;

import com.aidigital.aionboarding.domain.user.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Every query here stays a native SQL statement — deliberately, not by omission. Each one relies
 * on at least one Postgres/SQL feature the JPA Criteria API cannot express at all:
 * <ul>
 *   <li>{@code findMemberStats}, {@code findRoadmapStats}, {@code findWeeklyActivity} — CTEs
 *       ({@code WITH ...}), which JPA Criteria has no representation for whatsoever.
 *   <li>{@code findWeeklyActivity} additionally relies on {@code generate_series()} used as a
 *       virtual weeks table.
 *   <li>{@code findMemberStats} additionally relies on {@code ARRAY_AGG(... ORDER BY ...)} and
 *       {@code FILTER (WHERE ...)} aggregate qualifiers.
 *   <li>{@code findLowConfidenceLessons} relies on {@code JSONB_AGG}/{@code JSONB_BUILD_OBJECT}
 *       with an {@code ORDER BY} inside the aggregate.
 *   <li>{@code findRecentActivity} relies on {@code UNION ALL} of two heterogeneous SELECTs —
 *       JPQL/Criteria has no UNION support at all.
 * </ul>
 * {@code findIndividualRoadmapLessons} and {@code findStandaloneLessons} don't hit one of those
 * hard blockers individually, but they're part of this same consolidated single-pass reporting
 * family (avoiding N+1 dashboard fan-out) and stay native for consistency with the rest of this
 * family.
 */
public interface TeamDashboardRepository extends JpaRepository<User, Long> {

    @Query(value = """
        WITH roadmap_summary AS (
          SELECT
            user_roadmaps.user_id,
            COUNT(*)::int AS roadmap_count,
            ARRAY_AGG(CAST(roadmaps.id AS TEXT) ORDER BY user_roadmaps.enrolled_at DESC) AS roadmap_ids,
            ARRAY_AGG(roadmaps.title ORDER BY user_roadmaps.enrolled_at DESC) AS roadmap_titles
          FROM user_roadmaps
          JOIN roadmaps ON roadmaps.id = user_roadmaps.roadmap_id
          WHERE user_roadmaps.user_id IN (:memberIds)
          GROUP BY user_roadmaps.user_id
        ),
        roadmap_lesson_progress AS (
          SELECT
            roadmap_lessons_for_user.user_id,
            COUNT(roadmap_lessons_for_user.lesson_id)::int AS lesson_count,
            COUNT(user_lessons.lesson_id) FILTER (WHERE user_lessons.completed_at IS NOT NULL)::int AS completed_count
          FROM (
            SELECT DISTINCT user_roadmaps.user_id, roadmap_lessons.lesson_id
            FROM user_roadmaps
            JOIN roadmap_lessons ON roadmap_lessons.roadmap_id = user_roadmaps.roadmap_id
            WHERE user_roadmaps.user_id IN (:memberIds)
          ) roadmap_lessons_for_user
          LEFT JOIN user_lessons
            ON user_lessons.user_id = roadmap_lessons_for_user.user_id
            AND user_lessons.lesson_id = roadmap_lessons_for_user.lesson_id
          GROUP BY roadmap_lessons_for_user.user_id
        ),
        completions AS (
          SELECT
            user_id,
            COUNT(*) FILTER (
              WHERE completed_at IS NOT NULL
                AND completed_at >= NOW() - INTERVAL '7 days'
            )::int AS completed_week,
            COUNT(*) FILTER (
              WHERE completed_at IS NOT NULL
                AND completed_at >= NOW() - INTERVAL '30 days'
            )::int AS completed_month,
            COUNT(*) FILTER (
              WHERE completed_at IS NOT NULL
                AND completed_at >= NOW() - INTERVAL '90 days'
            )::int AS completed_quarter,
            COUNT(*) FILTER (WHERE completed_at IS NULL)::int AS open_assignments,
            MAX(GREATEST(enrolled_at, COALESCE(completed_at, enrolled_at))) AS last_lesson_at
          FROM user_lessons
          WHERE user_id IN (:memberIds)
          GROUP BY user_id
        ),
        quiz_scores AS (
          SELECT
            user_lesson_activity_attempts.user_id,
            ROUND(AVG(user_lesson_activity_attempts.score) FILTER (
              WHERE user_lesson_activity_attempts.created_at >= NOW() - INTERVAL '7 days'
            ))::int AS avg_score_week,
            ROUND(AVG(user_lesson_activity_attempts.score) FILTER (
              WHERE user_lesson_activity_attempts.created_at >= NOW() - INTERVAL '30 days'
            ))::int AS avg_score_month,
            ROUND(AVG(user_lesson_activity_attempts.score) FILTER (
              WHERE user_lesson_activity_attempts.created_at >= NOW() - INTERVAL '90 days'
            ))::int AS avg_score_quarter,
            MAX(user_lesson_activity_attempts.created_at) AS last_quiz_at
          FROM user_lesson_activity_attempts
          JOIN activity_type ON activity_type.id = user_lesson_activity_attempts.type_id
            AND activity_type.code = 'quiz'
          WHERE user_lesson_activity_attempts.user_id IN (:memberIds)
          GROUP BY user_lesson_activity_attempts.user_id
        )
        SELECT
          users.id AS id,
          users.name AS name,
          users.email AS email,
          user_role.code AS role,
          users.position AS position,
          users.avatar_storage_key AS avatarStorageKey,
          users.avatar_color AS avatarColor,
          COALESCE(roadmap_summary.roadmap_count, 0)::int AS roadmapCount,
          COALESCE(roadmap_summary.roadmap_ids, ARRAY[]::text[]) AS roadmapIds,
          COALESCE(roadmap_summary.roadmap_titles, ARRAY[]::text[]) AS roadmapTitles,
          COALESCE(roadmap_lesson_progress.lesson_count, 0)::int AS roadmapLessonCount,
          COALESCE(roadmap_lesson_progress.completed_count, 0)::int AS roadmapCompletedCount,
          COALESCE(completions.completed_week, 0)::int AS completedWeek,
          COALESCE(completions.completed_month, 0)::int AS completedMonth,
          COALESCE(completions.completed_quarter, 0)::int AS completedQuarter,
          COALESCE(completions.open_assignments, 0)::int AS openAssignments,
          quiz_scores.avg_score_week::int AS avgQuizScoreWeek,
          quiz_scores.avg_score_month::int AS avgQuizScoreMonth,
          quiz_scores.avg_score_quarter::int AS avgQuizScoreQuarter,
          GREATEST(completions.last_lesson_at, quiz_scores.last_quiz_at) AS lastActiveAt
        FROM users
        JOIN user_role ON user_role.id = users.role_id
        LEFT JOIN roadmap_summary ON roadmap_summary.user_id = users.id
        LEFT JOIN roadmap_lesson_progress ON roadmap_lesson_progress.user_id = users.id
        LEFT JOIN completions ON completions.user_id = users.id
        LEFT JOIN quiz_scores ON quiz_scores.user_id = users.id
        WHERE users.id IN (:memberIds)
        ORDER BY users.name ASC
        """, nativeQuery = true)
    List<MemberStatsProjection> findMemberStats(@Param("memberIds") List<Long> memberIds);

    @Query(value = """
        WITH roadmap_totals AS (
          SELECT roadmap_id, COUNT(*)::int AS lesson_count
          FROM roadmap_lessons
          GROUP BY roadmap_id
        ),
        enrolled AS (
          SELECT
            user_roadmaps.roadmap_id,
            user_roadmaps.user_id,
            COALESCE(roadmap_totals.lesson_count, 0)::int AS lesson_count
          FROM user_roadmaps
          LEFT JOIN roadmap_totals ON roadmap_totals.roadmap_id = user_roadmaps.roadmap_id
          WHERE user_roadmaps.user_id IN (:memberIds)
        ),
        completed AS (
          SELECT
            enrolled.roadmap_id,
            enrolled.user_id,
            COUNT(user_lessons.lesson_id)::int AS completed_count
          FROM enrolled
          JOIN roadmap_lessons ON roadmap_lessons.roadmap_id = enrolled.roadmap_id
          LEFT JOIN user_lessons
            ON user_lessons.user_id = enrolled.user_id
            AND user_lessons.lesson_id = roadmap_lessons.lesson_id
            AND user_lessons.completed_at IS NOT NULL
          GROUP BY enrolled.roadmap_id, enrolled.user_id
        )
        SELECT
          roadmaps.id AS id,
          roadmaps.title AS title,
          COUNT(enrolled.user_id)::int AS learners,
          COALESCE(MAX(enrolled.lesson_count), 0)::int AS lessonCount,
          ROUND(AVG(
            CASE
              WHEN enrolled.lesson_count > 0 THEN (completed.completed_count::numeric / enrolled.lesson_count) * 100
              ELSE 0
            END
          ))::int AS avgProgress
        FROM enrolled
        JOIN roadmaps ON roadmaps.id = enrolled.roadmap_id
        LEFT JOIN completed
          ON completed.roadmap_id = enrolled.roadmap_id
          AND completed.user_id = enrolled.user_id
        GROUP BY roadmaps.id, roadmaps.title
        ORDER BY learners DESC, roadmaps.title ASC
        LIMIT 6
        """, nativeQuery = true)
    List<RoadmapStatsProjection> findRoadmapStats(@Param("memberIds") List<Long> memberIds);

    @Query(value = """
        WITH weeks AS (
          SELECT generate_series(
            date_trunc('week', NOW()) - INTERVAL '7 weeks',
            date_trunc('week', NOW()),
            INTERVAL '1 week'
          ) AS week_start
        ),
        selected_users AS (
          SELECT id AS user_id FROM users WHERE id IN (:memberIds)
        ),
        lessons AS (
          SELECT user_id, date_trunc('week', completed_at) AS week_start, COUNT(*)::int AS count
          FROM user_lessons
          WHERE user_id IN (:memberIds)
            AND completed_at >= date_trunc('week', NOW()) - INTERVAL '7 weeks'
          GROUP BY user_id, date_trunc('week', completed_at)
        ),
        quizzes AS (
          SELECT user_lesson_activity_attempts.user_id,
                 date_trunc('week', user_lesson_activity_attempts.created_at) AS week_start,
                 COUNT(*)::int AS count
          FROM user_lesson_activity_attempts
          JOIN activity_type ON activity_type.id = user_lesson_activity_attempts.type_id
            AND activity_type.code = 'quiz'
          WHERE user_lesson_activity_attempts.user_id IN (:memberIds)
            AND user_lesson_activity_attempts.created_at >= date_trunc('week', NOW()) - INTERVAL '7 weeks'
          GROUP BY user_lesson_activity_attempts.user_id, date_trunc('week', user_lesson_activity_attempts.created_at)
        )
        SELECT
          selected_users.user_id AS userId,
          weeks.week_start AS weekStart,
          COALESCE(lessons.count, 0)::int AS lessons,
          COALESCE(quizzes.count, 0)::int AS quizzes
        FROM weeks
        CROSS JOIN selected_users
        LEFT JOIN lessons
          ON lessons.user_id = selected_users.user_id
          AND lessons.week_start = weeks.week_start
        LEFT JOIN quizzes
          ON quizzes.user_id = selected_users.user_id
          AND quizzes.week_start = weeks.week_start
        ORDER BY weeks.week_start ASC, selected_users.user_id ASC
        """, nativeQuery = true)
    List<WeeklyActivityProjection> findWeeklyActivity(@Param("memberIds") List<Long> memberIds);

    @Query(value = """
        WITH low_confidence_lessons AS (
          SELECT
            lessons.id AS id,
            lessons.title AS title,
            COUNT(user_lesson_activity_attempts.id)::int AS attempts,
            COUNT(DISTINCT user_lesson_activity_attempts.user_id)::int AS learners,
            ROUND(AVG(user_lesson_activity_attempts.score))::int AS avg_score,
            COUNT(user_lesson_activity_attempts.id) FILTER (
              WHERE user_lesson_activity_attempts.user_id IS DISTINCT FROM :leadId
            )::int AS attempts_excluding_lead,
            COUNT(DISTINCT user_lesson_activity_attempts.user_id) FILTER (
              WHERE user_lesson_activity_attempts.user_id IS DISTINCT FROM :leadId
            )::int AS learners_excluding_lead,
            ROUND(AVG(user_lesson_activity_attempts.score) FILTER (
              WHERE user_lesson_activity_attempts.user_id IS DISTINCT FROM :leadId
            ))::int AS avg_score_excluding_lead
          FROM user_lesson_activity_attempts
          JOIN activity_type ON activity_type.id = user_lesson_activity_attempts.type_id
            AND activity_type.code = 'quiz'
          JOIN lessons ON lessons.id = user_lesson_activity_attempts.lesson_id
          WHERE user_lesson_activity_attempts.user_id IN (:memberIds)
          GROUP BY lessons.id, lessons.title
          HAVING AVG(user_lesson_activity_attempts.score) < 80
          ORDER BY avg_score ASC, attempts DESC, lessons.title ASC
          LIMIT 4
        )
        SELECT
          low_confidence_lessons.id AS id,
          low_confidence_lessons.title AS title,
          low_confidence_lessons.attempts AS attempts,
          low_confidence_lessons.learners AS learners,
          low_confidence_lessons.avg_score AS avgScore,
          low_confidence_lessons.attempts_excluding_lead AS attemptsExcludingLead,
          low_confidence_lessons.learners_excluding_lead AS learnersExcludingLead,
          low_confidence_lessons.avg_score_excluding_lead AS avgScoreExcludingLead,
          CAST(JSONB_AGG(
            JSONB_BUILD_OBJECT(
              'id', recent_attempts.id,
              'userId', recent_attempts.user_id,
              'userName', recent_attempts.user_name,
              'userEmail', recent_attempts.user_email,
              'avatarStorageKey', recent_attempts.avatar_storage_key,
              'avatarColor', recent_attempts.avatar_color,
              'activityId', recent_attempts.activity_id,
              'activityTitle', recent_attempts.activity_title,
              'attemptNumber', recent_attempts.attempt_number,
              'score', recent_attempts.score,
              'passed', recent_attempts.passed,
              'correctCount', recent_attempts.correct_count,
              'totalCount', recent_attempts.total_count,
              'createdAt', recent_attempts.created_at
            )
            ORDER BY recent_attempts.created_at DESC
          ) AS TEXT) AS attemptItems
        FROM low_confidence_lessons
        JOIN LATERAL (
          SELECT
            user_lesson_activity_attempts.id,
            users.id AS user_id,
            users.name AS user_name,
            users.email AS user_email,
            users.avatar_storage_key,
            users.avatar_color,
            lesson_activities.id AS activity_id,
            COALESCE(NULLIF(lesson_activities.title, ''), 'Quiz') AS activity_title,
            user_lesson_activity_attempts.attempt_number,
            user_lesson_activity_attempts.score,
            user_lesson_activity_attempts.passed,
            user_lesson_activity_attempts.correct_count,
            user_lesson_activity_attempts.total_count,
            user_lesson_activity_attempts.created_at
          FROM user_lesson_activity_attempts
          JOIN activity_type ON activity_type.id = user_lesson_activity_attempts.type_id
            AND activity_type.code = 'quiz'
          JOIN users ON users.id = user_lesson_activity_attempts.user_id
          JOIN lesson_activities ON lesson_activities.id = user_lesson_activity_attempts.activity_id
          WHERE user_lesson_activity_attempts.lesson_id = low_confidence_lessons.id
            AND user_lesson_activity_attempts.user_id IN (:memberIds)
          ORDER BY user_lesson_activity_attempts.created_at DESC
          LIMIT :attemptSampleSize
        ) recent_attempts ON true
        GROUP BY
          low_confidence_lessons.id,
          low_confidence_lessons.title,
          low_confidence_lessons.attempts,
          low_confidence_lessons.learners,
          low_confidence_lessons.avg_score,
          low_confidence_lessons.attempts_excluding_lead,
          low_confidence_lessons.learners_excluding_lead,
          low_confidence_lessons.avg_score_excluding_lead
        ORDER BY avgScore ASC, attempts DESC, low_confidence_lessons.title ASC
        """, nativeQuery = true)
    List<LowConfidenceLessonProjection> findLowConfidenceLessons(
        @Param("memberIds") List<Long> memberIds,
        @Param("leadId") Long leadId,
        @Param("attemptSampleSize") int attemptSampleSize
    );

    @Query(value = """
        SELECT *
        FROM (
          SELECT
            'lesson' AS kind,
            user_lessons.completed_at AS happenedAt,
            users.id AS userId,
            users.name AS who,
            users.avatar_storage_key AS avatarStorageKey,
            users.avatar_color AS avatarColor,
            lessons.title AS what,
            NULL::numeric AS score,
            NULL::boolean AS passed
          FROM user_lessons
          JOIN users ON users.id = user_lessons.user_id
          JOIN lessons ON lessons.id = user_lessons.lesson_id
          WHERE user_lessons.user_id IN (:memberIds)
            AND user_lessons.completed_at IS NOT NULL
          UNION ALL
          SELECT
            'quiz' AS kind,
            user_lesson_activity_attempts.created_at AS happenedAt,
            users.id AS userId,
            users.name AS who,
            users.avatar_storage_key AS avatarStorageKey,
            users.avatar_color AS avatarColor,
            lesson_activities.title AS what,
            user_lesson_activity_attempts.score,
            user_lesson_activity_attempts.passed
          FROM user_lesson_activity_attempts
          JOIN activity_type ON activity_type.id = user_lesson_activity_attempts.type_id
            AND activity_type.code = 'quiz'
          JOIN users ON users.id = user_lesson_activity_attempts.user_id
          JOIN lesson_activities ON lesson_activities.id = user_lesson_activity_attempts.activity_id
          WHERE user_lesson_activity_attempts.user_id IN (:memberIds)
        ) activity
        ORDER BY happenedAt DESC
        LIMIT 12
        """, nativeQuery = true)
    List<RecentActivityProjection> findRecentActivity(@Param("memberIds") List<Long> memberIds);

    @Query(value = """
        SELECT
          user_roadmaps.user_id AS memberId,
          roadmaps.id AS roadmapId,
          roadmaps.title AS roadmapTitle,
          user_roadmaps.enrolled_at AS roadmapEnrolledAt,
          roadmap_lessons.sort_order AS sortOrder,
          lessons.id AS lessonId,
          lessons.title AS lessonTitle,
          user_lessons.completed_at AS completedAt,
          user_lessons.enrolled_at AS enrolledAt,
          ROUND(AVG(user_lesson_activity_attempts.score))::int AS avgScore,
          MAX(user_lesson_activity_attempts.created_at) AS lastQuizAt
        FROM user_roadmaps
        JOIN roadmaps ON roadmaps.id = user_roadmaps.roadmap_id
        JOIN roadmap_lessons ON roadmap_lessons.roadmap_id = roadmaps.id
        JOIN lessons ON lessons.id = roadmap_lessons.lesson_id
        LEFT JOIN user_lessons
          ON user_lessons.user_id = user_roadmaps.user_id
          AND user_lessons.lesson_id = lessons.id
        LEFT JOIN user_lesson_activity_attempts
          ON user_lesson_activity_attempts.lesson_id = lessons.id
          AND user_lesson_activity_attempts.user_id = user_roadmaps.user_id
          AND user_lesson_activity_attempts.type_id = (
            SELECT activity_type.id FROM activity_type WHERE activity_type.code = 'quiz' LIMIT 1
          )
        WHERE user_roadmaps.user_id IN (:memberIds)
        GROUP BY
          user_roadmaps.user_id,
          roadmaps.id,
          roadmaps.title,
          user_roadmaps.enrolled_at,
          roadmap_lessons.sort_order,
          lessons.id,
          lessons.title,
          user_lessons.completed_at,
          user_lessons.enrolled_at
        ORDER BY user_roadmaps.user_id ASC, user_roadmaps.enrolled_at DESC, roadmap_lessons.sort_order ASC
        """, nativeQuery = true)
    List<IndividualRoadmapLessonProjection> findIndividualRoadmapLessons(@Param("memberIds") List<Long> memberIds);

    @Query(value = """
        SELECT
          user_lessons.user_id AS memberId,
          lessons.id AS lessonId,
          lessons.title AS lessonTitle,
          user_lessons.completed_at AS completedAt,
          user_lessons.enrolled_at AS enrolledAt,
          ROUND(AVG(user_lesson_activity_attempts.score))::int AS avgScore,
          MAX(user_lesson_activity_attempts.created_at) AS lastQuizAt
        FROM user_lessons
        JOIN lessons ON lessons.id = user_lessons.lesson_id
        LEFT JOIN user_lesson_activity_attempts
          ON user_lesson_activity_attempts.lesson_id = lessons.id
          AND user_lesson_activity_attempts.user_id = user_lessons.user_id
          AND user_lesson_activity_attempts.type_id = (
            SELECT activity_type.id FROM activity_type WHERE activity_type.code = 'quiz' LIMIT 1
          )
        WHERE user_lessons.user_id IN (:memberIds)
          AND NOT EXISTS (
            SELECT 1
            FROM user_roadmaps
            JOIN roadmap_lessons ON roadmap_lessons.roadmap_id = user_roadmaps.roadmap_id
            WHERE user_roadmaps.user_id = user_lessons.user_id
              AND roadmap_lessons.lesson_id = user_lessons.lesson_id
          )
        GROUP BY user_lessons.user_id, lessons.id, lessons.title, user_lessons.completed_at, user_lessons.enrolled_at
        ORDER BY user_lessons.user_id ASC, user_lessons.completed_at ASC NULLS FIRST, user_lessons.enrolled_at DESC
        """, nativeQuery = true)
    List<StandaloneLessonProjection> findStandaloneLessons(@Param("memberIds") List<Long> memberIds);
}
