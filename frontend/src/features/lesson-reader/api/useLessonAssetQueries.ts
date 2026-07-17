import { useQuery } from "@tanstack/react-query";
import { apiClient } from "@/shared/api/client";
import { extractApiError } from "@/shared/api/extractApiError";

export function useYoutubeOembedQuery(url?: string, enabled = true) {
    return useQuery({
        queryKey: ["youtube", "oembed", url],
        enabled: Boolean(url) && enabled,
        queryFn: async () => {
            const { data, error } = await apiClient.GET("/api/v1/youtube/oembed", {
                params: { query: { url: url! } },
            });
            if (error || !data) {
                throw new Error(extractApiError(error, "Failed to load YouTube metadata."));
            }
            return data;
        },
        staleTime: 10 * 60_000,
    });
}
