import { useQuery } from "@tanstack/react-query";
import { apiClient } from "@/shared/api/client";
import { SESSION_QUERY_OPTIONS } from "@/shared/api/queryPolicies";

/** Loads the signed-in user's profile from {@code GET /api/v1/users/me}. */
export function useAuthMeQuery() {
    return useQuery({
        queryKey: ["session", "me"],
        queryFn: async () => {
            const { data, error } = await apiClient.GET("/api/v1/users/me");
            if (error) throw error;
            return data;
        },
        ...SESSION_QUERY_OPTIONS,
        retry: false,
    });
}
