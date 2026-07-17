import { useEffect } from "react";
import { useQueries, useQueryClient } from "@tanstack/react-query";
import { apiClient } from "@/shared/api/client";
import { getApiErrorMessage } from "@/shared/api/apiError";
import { libraryQueryKeys } from "./queryKeys";

const POLL_INTERVAL_MS = 5000;

/**
 * Polls each generating lesson's lightweight status endpoint instead of refetching the full
 * Lessons list every interval. When a lesson's status moves off "generating", invalidates the
 * Lessons list once so the real (full-fidelity) search result replaces the stale card.
 */
export function useGeneratingLessonsPolling(generatingLessonIds: number[]) {
    const queryClient = useQueryClient();

    const results = useQueries({
        queries: generatingLessonIds.map((id) => ({
            queryKey: ["library", "lessons", "generation-status", id] as const,
            queryFn: async ({ signal }: { signal: AbortSignal }) => {
                const { data, error } = await apiClient.GET("/api/v1/lessons/{id}/generation-status", {
                    params: { path: { id } },
                    signal,
                });

                if (error) {
                    throw new Error(getApiErrorMessage(error, "Failed to check lesson status."));
                }

                return data?.status;
            },
            refetchInterval: POLL_INTERVAL_MS,
            retry: false,
        })),
    });

    useEffect(() => {
        const anyFinished = results.some((result) => result.data && result.data !== "generating");
        if (anyFinished) {
            void queryClient.invalidateQueries({ queryKey: libraryQueryKeys.lessons });
        }
    }, [results, queryClient]);
}
