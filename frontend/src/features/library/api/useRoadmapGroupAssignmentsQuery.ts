import { useQuery } from "@tanstack/react-query";
import { apiClient } from "@/shared/api/client";
import { getApiErrorMessage } from "@/shared/api/apiError";
import { groupsKeys } from "@/features/groups/api/queryKeys";

/** Standing group assignments for one roadmap, restricted server-side to groups the caller can manage. */
export function useRoadmapGroupAssignmentsQuery(roadmapId: number | null, enabled = true) {
    return useQuery({
        queryKey: groupsKeys.roadmapAssignments(roadmapId ?? -1),
        enabled: enabled && roadmapId !== null,
        queryFn: async () => {
            const { data, error } = await apiClient.GET("/api/v1/roadmaps/{id}/group-assignments", {
                params: { path: { id: roadmapId as number } },
            });

            if (error) {
                throw new Error(getApiErrorMessage(error, "Failed to load team assignments."));
            }

            return data?.assignments ?? [];
        },
    });
}
