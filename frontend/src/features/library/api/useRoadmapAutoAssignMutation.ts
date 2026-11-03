import { useMutation, useQueryClient } from "@tanstack/react-query";

import { apiClient } from "@/shared/api/client";
import { getApiErrorMessage } from "@/shared/api/apiError";
import { writeRoadmapIntoCaches } from "./useRoadmapMutations";

/**
 * Turns a roadmap's "automatically assign to new users" setting on or off. The response is the
 * authoritative roadmap, so it is written straight into every roadmap list cache instead of
 * refetching the whole Library page.
 */
export function useRoadmapAutoAssignMutation() {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: async ({ roadmapId, enabled }: { roadmapId: number; enabled: boolean }) => {
            const { data, error } = await apiClient.PUT("/api/v1/roadmaps/{id}/auto-assign", {
                params: { path: { id: roadmapId } },
                body: { enabled },
            });

            if (error) {
                throw new Error(getApiErrorMessage(error, "Failed to update auto-assignment."));
            }

            return data;
        },
        onSuccess: (data) => {
            if (data?.roadmap) {
                writeRoadmapIntoCaches(queryClient, data.roadmap);
            }
        },
    });
}
