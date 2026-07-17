import { queryOptions } from "@tanstack/react-query";
import { apiClient } from "@/shared/api/client";
import { getApiErrorMessage } from "@/shared/api/apiError";
import type { components } from "@/shared/api/generated/schema";
import { DETAIL_QUERY_OPTIONS } from "@/shared/api/queryPolicies";
import { lessonsKeys } from "./queryKeys";

export type LessonDetailResponseV1 = components["schemas"]["LessonDetailResponseV1"];

/**
 * Shared queryOptions factory for GET /api/v1/lessons/{id}.
 * Used by both useLessonReaderQuery (with select) and useActivityPlayerQuery.
 * Caches raw LessonDetailResponseV1 under the canonical ["lessons","detail",id] key.
 */
export function lessonDetailQueryOptions(lessonId: string) {
    return queryOptions({
        queryKey: lessonsKeys.detail(lessonId),
        queryFn: async ({ signal }): Promise<LessonDetailResponseV1 | null> => {
            const numericId = Number(lessonId);
            if (!Number.isFinite(numericId)) throw new Error("Invalid lesson id.");

            const { data, error } = await apiClient.GET("/api/v1/lessons/{id}", {
                params: { path: { id: numericId } },
                signal,
            });

            if (error) throw new Error(getApiErrorMessage(error, "Failed to load lesson."));
            return data ?? null;
        },
        ...DETAIL_QUERY_OPTIONS,
        retry: false,
    });
}
