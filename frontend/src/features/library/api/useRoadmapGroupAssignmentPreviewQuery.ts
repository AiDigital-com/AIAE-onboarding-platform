import { useQuery } from "@tanstack/react-query";
import { apiClient } from "@/shared/api/client";
import { getApiErrorMessage } from "@/shared/api/apiError";

/**
 * Dry-run preview of how many current group members a grade filter would match, without
 * creating an assignment or enrolling anyone. Read-only despite the transport being POST.
 */
export function useRoadmapGroupAssignmentPreviewQuery(
    roadmapId: number | null,
    groupId: number | null,
    gradeIds: number[],
    enabled: boolean,
) {
    const sortedGradeIds = [...gradeIds].sort((a, b) => a - b);

    return useQuery({
        queryKey: ["roadmaps", roadmapId, "group-assignments", "preview", groupId, sortedGradeIds] as const,
        enabled: enabled && roadmapId !== null && groupId !== null,
        queryFn: async () => {
            const { data, error } = await apiClient.POST("/api/v1/roadmaps/{id}/group-assignments/preview", {
                params: { path: { id: roadmapId as number } },
                body: { groupId: groupId as number, gradeIds: sortedGradeIds },
            });

            if (error) {
                throw new Error(getApiErrorMessage(error, "Failed to preview assignment."));
            }

            return data!.preview;
        },
    });
}
