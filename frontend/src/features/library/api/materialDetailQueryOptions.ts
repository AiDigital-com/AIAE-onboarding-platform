import { queryOptions } from "@tanstack/react-query";
import { apiClient } from "@/shared/api/client";
import { getApiErrorMessage } from "@/shared/api/apiError";
import type { components } from "@/shared/api/generated/schema";
import { DETAIL_QUERY_OPTIONS } from "@/shared/api/queryPolicies";
import { libraryQueryKeys } from "./queryKeys";

export type MaterialResponseV1 = components["schemas"]["MaterialResponseV1"];

/**
 * Shared queryOptions factory for GET /api/v1/materials/{id}.
 * Used to hydrate a Library search card (bounded summary) with full-fidelity content
 * (body text, complete attachment/link fields) once a detail dialog or edit form opens.
 */
export function materialDetailQueryOptions(materialId: string) {
    return queryOptions({
        queryKey: libraryQueryKeys.materialDetail(materialId),
        queryFn: async ({ signal }): Promise<MaterialResponseV1 | null> => {
            const numericId = Number(materialId);
            if (!Number.isFinite(numericId)) throw new Error("Invalid material id.");

            const { data, error } = await apiClient.GET("/api/v1/materials/{id}", {
                params: { path: { id: numericId } },
                signal,
            });

            if (error) throw new Error(getApiErrorMessage(error, "Failed to load material."));
            return data ?? null;
        },
        ...DETAIL_QUERY_OPTIONS,
        retry: false,
    });
}
