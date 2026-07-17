import { useQuery } from "@tanstack/react-query";
import { apiClient } from "@/shared/api/client";
import { getApiErrorMessage } from "@/shared/api/apiError";
import { REFERENCE_QUERY_OPTIONS } from "@/shared/api/queryPolicies";
import { groupsKeys } from "./queryKeys";

/** Active grades — cached like other rarely-changing reference lists. */
export function useGradesQuery() {
    return useQuery({
        queryKey: groupsKeys.grades,
        queryFn: async () => {
            const { data, error } = await apiClient.GET("/api/v1/grades", { params: { query: {} } });

            if (error) {
                throw new Error(getApiErrorMessage(error, "Failed to load grades."));
            }

            return data?.grades ?? [];
        },
        ...REFERENCE_QUERY_OPTIONS,
    });
}

/** Every grade including deactivated ones, for the grade management UI. Requires grades.manage. */
export function useAllGradesQuery(enabled: boolean) {
    return useQuery({
        queryKey: groupsKeys.gradesAll,
        enabled,
        queryFn: async () => {
            const { data, error } = await apiClient.GET("/api/v1/grades", {
                params: { query: { includeInactive: true } },
            });

            if (error) {
                throw new Error(getApiErrorMessage(error, "Failed to load grades."));
            }

            return data?.grades ?? [];
        },
    });
}
