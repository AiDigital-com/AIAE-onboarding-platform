import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { apiClient } from "@/shared/api/client";
import { getApiErrorMessage } from "@/shared/api/apiError";
import type { components } from "@/shared/api/generated/schema";
import { libraryQueryKeys } from "./queryKeys";

type AssignmentRequestV1 = components["schemas"]["AssignmentRequestV1"];
type LearningAssigneeV1 = components["schemas"]["LearningAssigneeV1"];

export function useLearningAssignmentMutation(itemType: "lesson" | "roadmap") {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: async ({ itemId, userIds }: { itemId: number; userIds: number[] }) => {
            const body: AssignmentRequestV1 = { userIds };
            const path =
                itemType === "roadmap"
                    ? "/api/v1/roadmaps/{id}/assignments"
                    : "/api/v1/lessons/{id}/assignments";

            const { data, error } = await apiClient.POST(path, {
                params: { path: { id: itemId } },
                body,
            });

            if (error) {
                throw new Error(getApiErrorMessage(error, `Failed to assign ${itemType}.`));
            }

            return data;
        },
        onSuccess: (_data, { itemId }) => {
            const queryKey =
                itemType === "roadmap"
                    ? libraryQueryKeys.roadmapAssignees(itemId)
                    : libraryQueryKeys.lessonAssignees(itemId);
            void queryClient.invalidateQueries({ queryKey });
        },
    });
}

/** Learners currently assigned to (enrolled in) a lesson or roadmap, for the revoke UI. */
export function useLearningAssigneesQuery(
    itemType: "lesson" | "roadmap",
    itemId: number | null,
    enabled = true,
) {
    const queryKey =
        itemType === "roadmap"
            ? libraryQueryKeys.roadmapAssignees(itemId ?? -1)
            : libraryQueryKeys.lessonAssignees(itemId ?? -1);

    return useQuery({
        queryKey,
        enabled: enabled && itemId !== null,
        queryFn: async () => {
            const path =
                itemType === "roadmap" ? "/api/v1/roadmaps/{id}/assignees" : "/api/v1/lessons/{id}/assignees";

            const { data, error } = await apiClient.GET(path, {
                params: { path: { id: itemId as number } },
            });

            if (error) {
                throw new Error(getApiErrorMessage(error, `Failed to load ${itemType} assignees.`));
            }

            return data?.assignees ?? [];
        },
    });
}

/**
 * Revokes one or more learners' lesson or roadmap assignment (enrollment) in a single bulk
 * request; updates the assignees cache directly and invalidates only the affected list.
 */
export function useRevokeLearningAssignmentMutation(itemType: "lesson" | "roadmap") {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: async ({ itemId, userIds }: { itemId: number; userIds: number[] }) => {
            const body: AssignmentRequestV1 = { userIds };
            const path =
                itemType === "roadmap"
                    ? "/api/v1/roadmaps/{id}/assignments/revoke"
                    : "/api/v1/lessons/{id}/assignments/revoke";

            const { error } = await apiClient.POST(path, {
                params: { path: { id: itemId } },
                body,
            });

            if (error) {
                throw new Error(getApiErrorMessage(error, `Failed to revoke ${itemType} assignment.`));
            }
        },
        onSuccess: (_data, { itemId, userIds }) => {
            const revokedUserIds = new Set(userIds);
            const queryKey =
                itemType === "roadmap"
                    ? libraryQueryKeys.roadmapAssignees(itemId)
                    : libraryQueryKeys.lessonAssignees(itemId);
            queryClient.setQueryData<LearningAssigneeV1[]>(queryKey, (previous) =>
                (previous ?? []).filter((assignee) => !revokedUserIds.has(assignee.userId)),
            );
            void queryClient.invalidateQueries({
                queryKey: itemType === "roadmap" ? libraryQueryKeys.roadmaps : libraryQueryKeys.lessons,
            });
        },
    });
}
