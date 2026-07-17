package com.aidigital.aionboarding.external.youtube;

import com.aidigital.aionboarding.external.youtube.model.YoutubeOEmbedMetadata;
import com.aidigital.aionboarding.external.youtube.model.YoutubeTranscriptResult;

/**
 * Application-facing YouTube metadata and transcript adapter.
 */
public interface YoutubeClient {

	/**
	 * Fetches oEmbed metadata for a YouTube watch or short URL.
	 *
	 * @param url supported YouTube URL
	 * @return metadata with {@code error} populated on failure
	 */
	YoutubeOEmbedMetadata fetchOembed(String url);

	/**
	 * Fetches caption transcript segments for a video id via timedtext scraping.
	 *
	 * @param videoId YouTube video id
	 * @return transcript segments or an error-bearing result
	 */
	YoutubeTranscriptResult fetchTranscript(String videoId);
}
