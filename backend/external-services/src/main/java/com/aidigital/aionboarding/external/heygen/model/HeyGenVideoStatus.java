package com.aidigital.aionboarding.external.heygen.model;

/**
 * Poll result for a HeyGen video render.
 *
 * @param id           provider video id
 * @param status       render status
 * @param videoUrl     signed download URL when ready
 * @param thumbnailUrl thumbnail URL when available
 * @param duration     duration in seconds when available
 */
public record HeyGenVideoStatus(
		String id,
		String status,
		String videoUrl,
		String thumbnailUrl,
		Double duration
) {

}
