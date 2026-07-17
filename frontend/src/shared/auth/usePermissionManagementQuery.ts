import { useQuery } from "@tanstack/react-query";
import { apiClient } from "@/shared/api/client";
import { SESSION_QUERY_OPTIONS } from "@/shared/api/queryPolicies";

/**
 * Loads permissions for every user the caller may manage from
 * {@code GET /api/v1/permissions/management} — Admin sees the whole workspace, Team Lead sees
 * their own team. Separate from {@link usePermissionsQuery}, which only ever returns the
 * caller's own permissions and is used for sidebar/route gating on every authenticated shell load.
 */
export function usePermissionManagementQuery(enabled = true) {
    return useQuery({
        queryKey: ["permissions", "management"],
        queryFn: async () => {
            const { data, error } = await apiClient.GET("/api/v1/permissions/management");
            if (error) throw error;
            return data;
        },
        ...SESSION_QUERY_OPTIONS,
        retry: false,
        enabled,
    });
}
