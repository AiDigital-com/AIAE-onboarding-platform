import { useMutation, useQueryClient } from "@tanstack/react-query";
import { apiClient } from "@/shared/api/client";
import { roadmapsKeys } from "./queryKeys";

/** Removes the current user's enrollment from a roadmap. */
export function useUnenrollRoadmapMutation() {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: async (roadmapId: number) => {
            const { error } = await apiClient.DELETE("/api/v1/roadmaps/{id}/enrollment", {
                params: { path: { id: roadmapId } },
            });
            if (error) throw error;
        },
        onSuccess: () => {
            void queryClient.invalidateQueries({ queryKey: roadmapsKeys.all });
        },
    });
}
