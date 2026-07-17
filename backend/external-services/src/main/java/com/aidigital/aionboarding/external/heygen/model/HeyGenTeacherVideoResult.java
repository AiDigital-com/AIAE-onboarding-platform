package com.aidigital.aionboarding.external.heygen.model;

/**
 * Result of starting a HeyGen teacher video render.
 *
 * @param provider  integration provider name
 * @param prompt    submitted prompt
 * @param avatarId  resolved avatar id (may be blank)
 * @param voiceId   resolved voice id (may be blank)
 * @param sessionId HeyGen session id
 * @param videoId   provider video id used for polling
 * @param status    initial render status
 */
public record HeyGenTeacherVideoResult(
		String provider,
		String prompt,
		String avatarId,
		String voiceId,
		String sessionId,
		String videoId,
		String status
) {

}
