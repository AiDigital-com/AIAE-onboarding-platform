import { useQuery } from "@tanstack/react-query";
import { apiClient } from "@/shared/api/client";
import { getApiErrorMessage } from "@/shared/api/apiError";
import { COLLECTION_QUERY_OPTIONS } from "@/shared/api/queryPolicies";
import { libraryQueryKeys } from "./queryKeys";
import type { LessonsQueryParams } from "./useLessonsQuery";

/**
 * Bounded count-only fetch for the Lessons tab label — a single COUNT query, not a full
 * search — so the tab count stays accurate while another tab's list is the one actually fetched.
 */
export function useLessonsCountQuery(params: LessonsQueryParams = {}) {
    return useQuery({
        queryKey: [...libraryQueryKeys.lessons, "count", params] as const,
        queryFn: async ({ signal }): Promise<number> => {
            // page/size/sort/direction are ignored server-side for a count-only request, but the
            // shared search request schema still declares them required.
            const { data, error } = await apiClient.POST("/api/v1/lessons/count", {
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
                throw new Error(getApiErrorMessage(error, "Failed to load lesson count."));
            }

            return data?.totalElements ?? 0;
        },
        ...COLLECTION_QUERY_OPTIONS,
        // A tab-count badge isn't worth three silent retries before showing anything — surface
        // the unknown state (see LibraryTabs) and let the next filter/tab change try again.
        retry: false,
    });
}
