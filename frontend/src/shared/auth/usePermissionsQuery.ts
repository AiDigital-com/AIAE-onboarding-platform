import { useQuery } from "@tanstack/react-query";
import { apiClient } from "@/shared/api/client";
import { SESSION_QUERY_OPTIONS } from "@/shared/api/queryPolicies";

/** Loads the permission snapshot from {@code GET /api/v1/permissions}. */
export function usePermissionsQuery() {
    return useQuery({
        queryKey: ["session", "permissions"],
        queryFn: async () => {
            const { data, error } = await apiClient.GET("/api/v1/permissions");
            if (error) throw error;
            return data;
        },
        ...SESSION_QUERY_OPTIONS,
        retry: false,
    });
}
