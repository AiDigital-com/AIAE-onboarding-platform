package com.aidigital.aionboarding.external.youtube.model;

import java.util.List;

/**
 * Transcript fetch result for a YouTube video.
 *
 * @param videoId requested video id
 * @param segments caption segments when available
 * @param error error message when unavailable
 */
public record YoutubeTranscriptResult(
    String videoId,
    List<YoutubeTranscriptSegment> segments,
    String error
) {

    public boolean isAvailable() {
        return error == null || error.isBlank();
    }
}
