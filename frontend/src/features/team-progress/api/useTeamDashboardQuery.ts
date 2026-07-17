import { useQuery } from "@tanstack/react-query";
import type { components } from "@/shared/api/generated/schema";
import { apiClient } from "@/shared/api/client";

export type DashboardPeriod = components["schemas"]["DashboardPeriodV1"];

/** Loads aggregated team learning progress metrics. */
export function useTeamDashboardQuery(period: DashboardPeriod) {
    return useQuery({
        queryKey: ["team-progress", "dashboard", period],
        queryFn: async () => {
            const { data, error } = await apiClient.GET("/api/v1/team-progress/dashboard", {
                params: { query: { period } },
            });
            if (error) throw error;
            return data;
        },
    });
}
