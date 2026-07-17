import { useMutation, useQueryClient } from "@tanstack/react-query";
import { apiClient } from "@/shared/api/client";
import { getApiErrorMessage } from "@/shared/api/apiError";
import type { components } from "@/shared/api/generated/schema";
import { lessonsKeys } from "@/features/lessons/api/queryKeys";
import { groupsKeys } from "@/features/groups/api/queryKeys";
import { libraryQueryKeys } from "./queryKeys";

type RoadmapGroupAssignmentV1 = components["schemas"]["RoadmapGroupAssignmentV1"];

/**
 * Assigns (or re-narrows) a roadmap's standing assignment to a group and enrolls every
 * currently matching member; updates the assignments cache from the response.
 */
export function useAssignRoadmapToGroupMutation() {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: async ({
            roadmapId,
            groupId,
            gradeIds,
        }: {
            roadmapId: number;
            groupId: number;
            gradeIds: number[];
        }) => {
            const { data, error } = await apiClient.POST("/api/v1/roadmaps/{id}/group-assignments", {
                params: { path: { id: roadmapId } },
                body: { groupId, gradeIds },
            });

            if (error) {
                throw new Error(getApiErrorMessage(error, "Failed to assign roadmap to team."));
            }

            return data;
        },
        onSuccess: (data, { roadmapId }) => {
            if (!data) {
                return;
            }
            queryClient.setQueryData<RoadmapGroupAssignmentV1[]>(
                groupsKeys.roadmapAssignments(roadmapId),
                (previous) => {
                    const withoutExisting = (previous ?? []).filter(
                        (existing) => existing.groupId !== data.assignment.groupId,
                    );
                    return [...withoutExisting, data.assignment];
                },
            );
            void queryClient.invalidateQueries({ queryKey: libraryQueryKeys.roadmaps });
            void queryClient.invalidateQueries({ queryKey: libraryQueryKeys.lessons });
            void queryClient.invalidateQueries({ queryKey: lessonsKeys.my() });
        },
    });
}

/** Removes a roadmap's standing assignment to a group; updates the assignments cache directly. */
export function useUnassignRoadmapFromGroupMutation() {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: async ({ roadmapId, groupId }: { roadmapId: number; groupId: number }) => {
            const { error } = await apiClient.DELETE("/api/v1/roadmaps/{id}/group-assignments/{groupId}", {
                params: { path: { id: roadmapId, groupId } },
            });

            if (error) {
                throw new Error(getApiErrorMessage(error, "Failed to unassign roadmap from team."));
            }
        },
        onSuccess: (_data, { roadmapId, groupId }) => {
            queryClient.setQueryData<RoadmapGroupAssignmentV1[]>(
                groupsKeys.roadmapAssignments(roadmapId),
                (previous) => (previous ?? []).filter((existing) => existing.groupId !== groupId),
            );
            void queryClient.invalidateQueries({ queryKey: libraryQueryKeys.roadmaps });
        },
    });
}
