package com.aidigital.aionboarding.external.heygen;

import com.aidigital.aionboarding.external.heygen.model.HeyGenTeacherVideoResult;
import com.aidigital.aionboarding.external.heygen.model.HeyGenVideoStatus;

/**
 * Narrow application-facing interface for the HeyGen video API.
 */
public interface HeyGenClient {

	/**
	 * Starts a teacher-style talking-head video render.
	 *
	 * @param prompt narration / scene prompt
	 * @return creation metadata including provider video id
	 * @throws HeyGenExternalException on HTTP or parse failures
	 */
	HeyGenTeacherVideoResult createTeacherVideo(String prompt);

	/**
	 * Polls render status for a previously created video.
	 *
	 * @param videoId HeyGen video identifier
	 * @return current status and URLs when available
	 * @throws HeyGenExternalException on HTTP or parse failures
	 */
	HeyGenVideoStatus getVideoStatus(String videoId);
}
