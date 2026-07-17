package com.aidigital.aionboarding.service.teachervideo.services;

import com.aidigital.aionboarding.domain.lesson.entities.Lesson;
import com.aidigital.aionboarding.service.lesson.models.TeacherVideoRecord;

/**
 * Refreshes HeyGen teacher video status and signed URLs stored on lesson generation metadata.
 */
public interface TeacherVideoRefreshService {

	/**
	 * Polls HeyGen when needed and persists updated teacher video fields on the lesson.
	 *
	 * @param lesson       lesson entity carrying generation metadata
	 * @param teacherVideo current teacher video snapshot from lesson metadata
	 * @param force        when {@code true}, always refresh; otherwise refresh for active jobs, missing URLs,
	 *                     or URLs nearing expiry
	 * @return updated lesson and teacher video records (unchanged when refresh is skipped)
	 * @throws com.aidigital.aionboarding.service.common.error.AppException when HeyGen status lookup fails
	 */
	RefreshResult refreshTeacherVideoIfNeeded(Lesson lesson, TeacherVideoRecord teacherVideo, boolean force);

	/**
	 * Pair of lesson and teacher video records returned after a refresh attempt.
	 *
	 * @param lesson       lesson entity, possibly updated and saved
	 * @param teacherVideo teacher video snapshot, possibly refreshed from HeyGen
	 */
	record RefreshResult(Lesson lesson, TeacherVideoRecord teacherVideo) {

	}
}
