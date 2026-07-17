import { useInfiniteQuery } from "@tanstack/react-query";
import { apiClient } from "@/shared/api/client";
import { getApiErrorMessage } from "@/shared/api/apiError";
import type { paths } from "@/shared/api/generated/schema";
import { COLLECTION_QUERY_OPTIONS } from "@/shared/api/queryPolicies";
import { libraryQueryKeys } from "./queryKeys";
import { emptyPageInfo, normalizeLesson } from "./normalizers";
import type { LibraryLesson, PagedResult } from "./types";

export type LessonsQueryParams = Partial<Omit<
    NonNullable<paths["/api/v1/lessons/search"]["post"]["requestBody"]["content"]["application/json"]>,
    "page"
>>;

const PAGE_SIZE = 24;

/**
 * Fetches lessons page-by-page; call fetchNextPage() to load more under the current filter.
 * Generation-in-progress lessons are refreshed via useGeneratingLessonsPolling's lightweight
 * per-lesson status poll instead of a refetchInterval here, which would re-fetch this entire list.
 */
export function useLessonsQuery(params: LessonsQueryParams = {}, options?: { enabled?: boolean }) {
    return useInfiniteQuery({
        queryKey: [...libraryQueryKeys.lessons, params] as const,
        enabled: options?.enabled ?? true,
        initialPageParam: 0,
        getNextPageParam: (lastPage: PagedResult<LibraryLesson>) =>
            lastPage.page.hasNext ? lastPage.page.page + 1 : undefined,
        queryFn: async ({ pageParam, signal }: { pageParam: number; signal: AbortSignal }): Promise<PagedResult<LibraryLesson>> => {
            const { data, error } = await apiClient.POST("/api/v1/lessons/search", {
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
                throw new Error(getApiErrorMessage(error, "Failed to load lessons."));
            }

            return {
                items: (data?.lessons ?? []).map(normalizeLesson),
                page: data?.page ?? emptyPageInfo(pageParam, params.size ?? PAGE_SIZE),
            };
        },
        ...COLLECTION_QUERY_OPTIONS,
        // One retry for a transient blip, not the default three-with-backoff — this is an
        // interactive search, so a slow failure path reads as the UI being stuck.
        retry: 1,
    });
}
