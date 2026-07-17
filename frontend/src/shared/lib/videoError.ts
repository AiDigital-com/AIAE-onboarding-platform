/**
 * Maps a native <video> MediaError to a message that distinguishes a corrupted/incomplete
 * upload from a codec/format the viewer's browser simply can't decode, instead of a generic
 * "couldn't play" message for every failure mode.
 * https://developer.mozilla.org/en-US/docs/Web/API/MediaError/code
 */
export function getVideoErrorMessage(mediaError?: MediaError | null): string {
    switch (mediaError?.code) {
        case MediaError.MEDIA_ERR_DECODE:
            return "This video file looks incomplete or corrupted (the upload may have been interrupted).";
        case MediaError.MEDIA_ERR_SRC_NOT_SUPPORTED:
            return "This video's format or codec isn't supported by your browser.";
        default:
            return "This video couldn't be played.";
    }
}
