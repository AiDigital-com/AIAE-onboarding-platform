import { useQuery } from "@tanstack/react-query";
import { apiClient } from "@/shared/api/client";
import { getApiErrorMessage } from "@/shared/api/apiError";
import { COLLECTION_QUERY_OPTIONS } from "@/shared/api/queryPolicies";
import { libraryQueryKeys } from "./queryKeys";
import type { RoadmapsQueryParams } from "./useRoadmapsQuery";

/**
 * Bounded count-only fetch for the Roadmaps tab label — a single COUNT query, not a full
 * search — so the tab count stays accurate while another tab's list is the one actually fetched.
 */
export function useRoadmapsCountQuery(params: RoadmapsQueryParams = {}) {
    return useQuery({
        queryKey: [...libraryQueryKeys.roadmaps, "count", params] as const,
        queryFn: async ({ signal }): Promise<number> => {
            // page/size/sort/direction are ignored server-side for a count-only request, but the
            // shared search request schema still declares them required.
            const { data, error } = await apiClient.POST("/api/v1/roadmaps/count", {
                body: {
                    ...params,
                    page: 0,
                    size: params.size ?? 1,
                    sort: params.sort ?? "createdAt",
                    direction: params.direction ?? "desc",
                },
                signal,
            });

            if (error) {
                throw new Error(getApiErrorMessage(error, "Failed to load roadmap count."));
            }

            return data?.totalElements ?? 0;
        },
        ...COLLECTION_QUERY_OPTIONS,
        // A tab-count badge isn't worth three silent retries before showing anything — surface
        // the unknown state (see LibraryTabs) and let the next filter/tab change try again.
        retry: false,
    });
}
