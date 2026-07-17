package com.aidigital.aionboarding.service.common.observability.enums;

/**
 * Low-cardinality reason a presigned or direct upload was rejected before it could be attached
 * to an entity, used only as a {@link SecurityMetrics} tag — never as user-facing error text.
 */
public enum UploadRejectionReason {
	SIZE_EXCEEDED("size_exceeded"),
	MIME_NOT_ALLOWED("mime_not_allowed"),
	OWNERSHIP_MISMATCH("ownership_mismatch"),
	ALREADY_CONFIRMED("already_confirmed"),
	EXPIRED("expired"),
	SIZE_MISMATCH("size_mismatch"),
	NOT_FOUND("not_found");

	private final String value;

	UploadRejectionReason(String value) {
		this.value = value;
	}

	/**
	 * Returns the metric tag value for this reason.
	 *
	 * @return reason value
	 */
	public String value() {
		return value;
	}
}
