package com.aidigital.aionboarding.external.youtube.model;

/**
 * A single transcript segment with offset in seconds.
 *
 * @param offsetSeconds segment start offset in seconds
 * @param text          spoken text
 */
public record YoutubeTranscriptSegment(double offsetSeconds, String text) {

}
