const VIDEO_VALIDATION_TIMEOUT_MS = 8000;

export function inferVideoMimeType(fileName: string): string | null {
    const lowerName = fileName.toLowerCase();
    if (lowerName.endsWith(".mp4") || lowerName.endsWith(".m4v")) {
        return "video/mp4";
    }
    if (lowerName.endsWith(".webm")) {
        return "video/webm";
    }
    if (lowerName.endsWith(".mov")) {
        return "video/quicktime";
    }
    return null;
}

export function isLikelyVideoFile(file: File): boolean {
    return file.type.startsWith("video/") || Boolean(inferVideoMimeType(file.name));
}

/**
 * Verifies that the current browser can load metadata for a selected video file.
 * This prevents unsupported-codec files from being uploaded as broken lesson assets.
 */
export function assertBrowserPlayableVideo(file: File): Promise<void> {
    if (!isLikelyVideoFile(file)) {
        return Promise.reject(new Error("Only video files can be uploaded."));
    }

    return new Promise((resolve, reject) => {
        const video = document.createElement("video");
        const objectUrl = URL.createObjectURL(file);
        let settled = false;

        const cleanup = () => {
            URL.revokeObjectURL(objectUrl);
            video.removeAttribute("src");
            video.load();
        };

        const finish = (error?: Error) => {
            if (settled) {
                return;
            }
            settled = true;
            window.clearTimeout(timeoutId);
            cleanup();
            if (error) {
                reject(error);
                return;
            }
            resolve();
        };

        const timeoutId = window.setTimeout(() => {
            finish(new Error("This video could not be checked. Try a smaller H.264/AAC MP4 file."));
        }, VIDEO_VALIDATION_TIMEOUT_MS);

        video.preload = "metadata";
        video.muted = true;
        video.onloadedmetadata = () => finish();
        video.onerror = () => {
            finish(new Error("This video is not playable in the browser. Convert it to H.264/AAC MP4 and upload again."));
        };
        video.src = objectUrl;
        video.load();
    });
}
