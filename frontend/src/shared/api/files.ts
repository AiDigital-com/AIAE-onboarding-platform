import { useQuery } from "@tanstack/react-query";
import { apiClient } from "./client";
import { notifyApiError } from "./apiErrorEvents";
import { extractApiError } from "./extractApiError";
import { SIGNED_MEDIA_QUERY_OPTIONS } from "./queryPolicies";

/** Resolves a protected storage key to a short-lived object-store URL. */
export async function fetchFilePreviewUrl(storageKey: string): Promise<string> {
    const previewUrlByStorageKey = await fetchFilePreviewUrls([storageKey]);
    const previewUrl = previewUrlByStorageKey[storageKey];
    if (!previewUrl) {
        throw new Error("Failed to load file.");
    }

    return previewUrl;
}

/** Caches a signed file URL while preserving Clerk authentication on lookup. */
export function useFilePreviewUrl(storageKey?: string) {
    return useQuery({
        queryKey: ["files", "preview", storageKey],
        enabled: Boolean(storageKey),
        queryFn: () => fetchFilePreviewUrl(storageKey!),
        ...SIGNED_MEDIA_QUERY_OPTIONS,
    });
}

/** Resolves many protected storage keys to short-lived object-store URLs in one backend call. */
export async function fetchFilePreviewUrls(storageKeys: string[]): Promise<Record<string, string>> {
    const uniqueKeys = Array.from(new Set(storageKeys.filter((key) => key.length > 0)));
    if (uniqueKeys.length === 0) {
        return {};
    }

    const { data, error } = await apiClient.POST("/api/v1/files/previews", {
        body: { storageKeys: uniqueKeys },
    });

    if (error || !data?.previews) {
        const message = extractApiError(error, "Failed to load files.");
        notifyApiError(message);
        throw new Error(message);
    }

    return Object.fromEntries(data.previews.map((preview) => [preview.storageKey, preview.previewUrl]));
}

/** Caches signed URLs for many storage keys at once, so a grid of cards issues one request. */
export function useFilePreviewUrls(storageKeys: string[]) {
    const uniqueSortedKeys = Array.from(new Set(storageKeys.filter((key) => key.length > 0))).sort();

    return useQuery({
        queryKey: ["files", "previews", uniqueSortedKeys],
        enabled: uniqueSortedKeys.length > 0,
        queryFn: () => fetchFilePreviewUrls(uniqueSortedKeys),
        ...SIGNED_MEDIA_QUERY_OPTIONS,
    });
}

/** Opens a protected object without exposing the object endpoint anonymously. */
export async function openStorageFile(storageKey: string): Promise<void> {
    const openedWindow = window.open("", "_blank");

    try {
        const previewUrl = await fetchFilePreviewUrl(storageKey);
        if (openedWindow) {
            openedWindow.opener = null;
            openedWindow.location.href = previewUrl;
        } else {
            window.location.href = previewUrl;
        }
    } catch (error) {
        openedWindow?.close();
        throw error;
    }
}
