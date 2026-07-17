export function extractYoutubeVideoId(url: string): string | null {
    try {
        const parsedUrl = new URL(url);
        const hostname = parsedUrl.hostname.replace(/^www\./, "");

        if (hostname === "youtu.be") {
            return parsedUrl.pathname.split("/").filter(Boolean)[0] || null;
        }

        if (hostname === "youtube.com" || hostname.endsWith(".youtube.com")) {
            const watchVideoId = parsedUrl.searchParams.get("v");

            if (watchVideoId) {
                return watchVideoId;
            }

            const [kind, videoId] = parsedUrl.pathname.split("/").filter(Boolean);

            if (["embed", "shorts", "live"].includes(kind)) {
                return videoId || null;
            }
        }

        return null;
    } catch {
        return null;
    }
}

export function getYoutubeThumbnail(url: string): string | null {
    const videoId = extractYoutubeVideoId(url);
    return videoId ? `https://img.youtube.com/vi/${videoId}/hqdefault.jpg` : null;
}

export function getYoutubeEmbedUrl(url: string): string | null {
    const videoId = extractYoutubeVideoId(url);
    return videoId ? `https://www.youtube.com/embed/${videoId}` : null;
}
