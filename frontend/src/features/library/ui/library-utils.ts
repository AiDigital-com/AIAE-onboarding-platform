export function formatDate(isoString?: string): string {
    if (!isoString) {
        return "";
    }

    try {
        return new Intl.DateTimeFormat("en", {
            year: "numeric",
            month: "short",
            day: "numeric",
        }).format(new Date(isoString));
    } catch {
        return "";
    }
}

export function formatDateTime(isoString?: string): string {
    if (!isoString) {
        return "";
    }

    try {
        return new Intl.DateTimeFormat("en", {
            year: "numeric",
            month: "short",
            day: "numeric",
            hour: "2-digit",
            minute: "2-digit",
        }).format(new Date(isoString));
    } catch {
        return "";
    }
}

export function formatFileSize(size?: number): string {
    if (!Number.isFinite(size) || !size || size <= 0) {
        return "";
    }

    const units = ["B", "KB", "MB", "GB"];
    let value = size;
    let unitIndex = 0;

    while (value >= 1024 && unitIndex < units.length - 1) {
        value /= 1024;
        unitIndex += 1;
    }

    return `${value.toFixed(value >= 10 || unitIndex === 0 ? 0 : 1)} ${units[unitIndex]}`;
}

export { extractYoutubeVideoId, getYoutubeThumbnail, getYoutubeEmbedUrl } from "@/shared/lib/youtube";

export const LIBRARY_SORT_OPTIONS = [
    { value: "recent", label: "Most recent" },
    { value: "oldest", label: "Oldest first" },
    { value: "az", label: "Title A -> Z" },
    { value: "za", label: "Title Z -> A" },
    { value: "popular", label: "Most popular" },
] as const;
