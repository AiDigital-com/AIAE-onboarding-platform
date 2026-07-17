package com.aidigital.aionboarding.domain.lessonactivity.repositories;

import com.aidigital.aionboarding.domain.lessonactivity.entities.UserLessonActivityProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserLessonActivityProgressRepository extends
        JpaRepository<UserLessonActivityProgress, UserLessonActivityProgress.UserLessonActivityProgressId> {

    @Query("SELECT p FROM UserLessonActivityProgress p WHERE p.id.userId = :userId AND p.id.activityId = :activityId")
    Optional<UserLessonActivityProgress> findByUserIdAndActivityId(@Param("userId") Long userId, @Param("activityId") Long activityId);

    @Query("SELECT p FROM UserLessonActivityProgress p WHERE p.id.userId = :userId AND p.lesson.id = :lessonId")
    List<UserLessonActivityProgress> findByUserIdAndLessonId(@Param("userId") Long userId, @Param("lessonId") Long lessonId);

    @Query("""
        SELECT p
        FROM UserLessonActivityProgress p
        JOIN FETCH p.activity
        JOIN FETCH p.status
        WHERE p.id.userId = :userId
          AND p.lesson.id IN :lessonIds
        """)
    List<UserLessonActivityProgress> findByUserIdAndLessonIds(
        @Param("userId") Long userId,
        @Param("lessonIds") java.util.Collection<Long> lessonIds
    );
}
