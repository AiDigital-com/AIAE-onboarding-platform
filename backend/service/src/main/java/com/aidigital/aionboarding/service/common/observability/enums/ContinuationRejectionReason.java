package com.aidigital.aionboarding.service.common.observability.enums;

/**
 * Low-cardinality reason a lesson-assistant conversation continuation attempt was rejected, used
 * only as a {@link SecurityMetrics} tag — never alongside the attempted response/conversation ID.
 */
public enum ContinuationRejectionReason {
    NO_SAVED_CONVERSATION("no_saved_conversation"),
    LESSON_MISMATCH("lesson_mismatch"),
    STALE_VERSION("stale_version");

    private final String value;

    ContinuationRejectionReason(String value) {
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
