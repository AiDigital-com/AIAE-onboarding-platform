package com.aidigital.aionboarding.domain.lessonactivity.repositories;

import com.aidigital.aionboarding.domain.lessonactivity.entities.UserLessonActivityAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserLessonActivityAttemptRepository extends JpaRepository<UserLessonActivityAttempt, Long> {

	@Query("""
			SELECT COALESCE(MAX(a.attemptNumber), 0) + 1
			FROM UserLessonActivityAttempt a
			WHERE a.user.id = :userId
			  AND a.activity.id = :activityId
			""")
	int nextAttemptNumber(@Param("userId") Long userId, @Param("activityId") Long activityId);

	@Query("""
			SELECT a FROM UserLessonActivityAttempt a
			WHERE a.user.id = :userId
			  AND a.lesson.id = :lessonId
			  AND a.activity.id = :activityId
			ORDER BY a.attemptNumber DESC
			""")
	List<UserLessonActivityAttempt> findByUserIdAndLessonIdAndActivityIdOrderByAttemptNumberDesc(
			@Param("userId") Long userId,
			@Param("lessonId") Long lessonId,
			@Param("activityId") Long activityId
	);
}
