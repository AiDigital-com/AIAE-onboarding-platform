import { useInfiniteQuery } from "@tanstack/react-query";
import { apiClient } from "@/shared/api/client";
import { getApiErrorMessage } from "@/shared/api/apiError";
import type { paths } from "@/shared/api/generated/schema";
import { COLLECTION_QUERY_OPTIONS } from "@/shared/api/queryPolicies";
import { roadmapsKeys } from "@/features/roadmaps/api/queryKeys";
import { emptyPageInfo, normalizeRoadmap } from "./normalizers";
import type { LibraryRoadmap, PagedResult } from "./types";

export type RoadmapsQueryParams = Partial<Omit<
    NonNullable<paths["/api/v1/roadmaps/search"]["post"]["requestBody"]["content"]["application/json"]>,
    "page"
>>;

const PAGE_SIZE = 24;

/**
 * Fetches roadmaps page-by-page under the canonical ["roadmaps", params] key (shared prefix with
 * the roadmaps feature's queries, so mutations invalidating roadmapsKeys.all refresh this view too).
 */
export function useRoadmapsQuery(params: RoadmapsQueryParams = {}, options?: { enabled?: boolean }) {
    return useInfiniteQuery({
        queryKey: [...roadmapsKeys.all, params] as const,
        enabled: options?.enabled ?? true,
        initialPageParam: 0,
        getNextPageParam: (lastPage: PagedResult<LibraryRoadmap>) =>
            lastPage.page.hasNext ? lastPage.page.page + 1 : undefined,
        queryFn: async ({ pageParam, signal }: { pageParam: number; signal: AbortSignal }): Promise<PagedResult<LibraryRoadmap>> => {
            const { data, error } = await apiClient.POST("/api/v1/roadmaps/search", {
                body: {
                    ...params,
                    page: pageParam,
                    size: params.size ?? PAGE_SIZE,
                    sort: params.sort ?? "createdAt",
                    direction: params.direction ?? "desc",
                },
                signal,
            });

            if (error) {
                throw new Error(getApiErrorMessage(error, "Failed to load roadmaps."));
            }

            return {
                items: (data?.roadmaps ?? []).map(normalizeRoadmap),
                page: data?.page ?? emptyPageInfo(pageParam, params.size ?? PAGE_SIZE),
            };
        },
        ...COLLECTION_QUERY_OPTIONS,
        // One retry for a transient blip, not the default three-with-backoff — this is an
        // interactive search, so a slow failure path reads as the UI being stuck.
        retry: 1,
    });
}
