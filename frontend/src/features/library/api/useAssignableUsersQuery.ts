import { useQuery } from "@tanstack/react-query";
import { apiClient } from "@/shared/api/client";
import { getApiErrorMessage } from "@/shared/api/apiError";
import type { paths } from "@/shared/api/generated/schema";
import { libraryQueryKeys } from "./queryKeys";

export type AssignableUsersQueryParams = NonNullable<
    paths["/api/v1/learning/assignees"]["get"]["parameters"]["query"]
>;

export function useAssignableUsersQuery(enabled = true, params: AssignableUsersQueryParams = {}) {
    return useQuery({
        queryKey: [...libraryQueryKeys.assignees, params] as const,
        enabled,
        queryFn: async () => {
            const { data, error } = await apiClient.GET("/api/v1/learning/assignees", {
                params: { query: params },
            });

            if (error) {
                throw new Error(getApiErrorMessage(error, "Failed to load team members."));
            }

            return data?.users ?? [];
        },
    });
}
