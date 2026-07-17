package com.aidigital.aionboarding.external.youtube.impl;

import com.aidigital.aionboarding.external.youtube.YoutubeClient;
import com.aidigital.aionboarding.external.youtube.model.YoutubeOEmbedMetadata;
import com.aidigital.aionboarding.external.youtube.model.YoutubeTranscriptResult;

import java.util.List;

/**
 * Stub YouTube client used when the integration is explicitly disabled.
 */
public class StubYoutubeClient implements YoutubeClient {

    private static final String MESSAGE =
        "YouTube integration is disabled. Set app.external.youtube.enabled=true.";

    @Override
    public YoutubeOEmbedMetadata fetchOembed(String url) {
        return new YoutubeOEmbedMetadata("", "", "", "", null, null, "", MESSAGE);
    }

    @Override
    public YoutubeTranscriptResult fetchTranscript(String videoId) {
        return new YoutubeTranscriptResult(videoId, List.of(), MESSAGE);
    }
}
