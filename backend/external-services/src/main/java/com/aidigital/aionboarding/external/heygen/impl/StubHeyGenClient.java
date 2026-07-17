package com.aidigital.aionboarding.external.heygen.impl;

import com.aidigital.aionboarding.external.heygen.HeyGenClient;
import com.aidigital.aionboarding.external.heygen.HeyGenExternalException;
import com.aidigital.aionboarding.external.heygen.model.HeyGenTeacherVideoResult;
import com.aidigital.aionboarding.external.heygen.model.HeyGenVideoStatus;

/**
 * Stub HeyGen client used when the integration is disabled or unconfigured.
 */
public class StubHeyGenClient implements HeyGenClient {

	private static final String MESSAGE =
			"HeyGen is not configured. Set HEYGEN_API_KEY and app.external.heygen.enabled=true.";

	@Override
	public HeyGenTeacherVideoResult createTeacherVideo(String prompt) {
		throw new HeyGenExternalException(MESSAGE);
	}

	@Override
	public HeyGenVideoStatus getVideoStatus(String videoId) {
		throw new HeyGenExternalException(MESSAGE);
	}
}
