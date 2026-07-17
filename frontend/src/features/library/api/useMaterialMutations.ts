import { useMutation, useQueryClient, type InfiniteData } from "@tanstack/react-query";
import { apiClient } from "@/shared/api/client";
import { getApiErrorMessage } from "@/shared/api/apiError";
import type { components } from "@/shared/api/generated/schema";
import { libraryQueryKeys } from "./queryKeys";
import type { LibraryMaterial, PagedResult } from "./types";

type CreateMaterialRequestV1 = components["schemas"]["CreateMaterialRequestV1"];
type UpdateMaterialRequestV1 = components["schemas"]["UpdateMaterialRequestV1"];
type MaterialResponseV1 = components["schemas"]["MaterialResponseV1"];
type MaterialsInfiniteData = InfiniteData<PagedResult<LibraryMaterial>>;

function isMaterialsInfiniteData(value: unknown): value is MaterialsInfiniteData {
    return Boolean(value && typeof value === "object" && Array.isArray((value as MaterialsInfiniteData).pages));
}

/** Materials list is an infinite query under ["library","materials", params] — not a flat array. */
function removeMaterialFromInfiniteCache(
    data: MaterialsInfiniteData,
    materialId: number,
): MaterialsInfiniteData {
    let decrementedTotal = false;
    return {
        ...data,
        pages: data.pages.map((page) => {
            const nextItems = page.items.filter((material) => material.id !== materialId);
            const didRemove = nextItems.length !== page.items.length;
            // totalElements is list-wide; only adjust once across pages.
            const shouldAdjustTotal = didRemove && !decrementedTotal;
            if (shouldAdjustTotal) {
                decrementedTotal = true;
            }
            return {
                ...page,
                items: nextItems,
                page: {
                    ...page.page,
                    totalElements: shouldAdjustTotal
                        ? Math.max(0, (page.page.totalElements ?? page.items.length) - 1)
                        : page.page.totalElements,
                },
            };
        }),
    };
}

function prependMaterialToInfiniteCache(
    data: MaterialsInfiniteData,
    material: LibraryMaterial,
): MaterialsInfiniteData {
    if (data.pages.length === 0) {
        return data;
    }
    const [firstPage, ...restPages] = data.pages;
    return {
        ...data,
        pages: [
            {
                ...firstPage,
                items: [material, ...firstPage.items.filter((item) => item.id !== material.id)],
                page: {
                    ...firstPage.page,
                    totalElements: (firstPage.page.totalElements ?? firstPage.items.length) + 1,
                },
            },
            ...restPages,
        ],
    };
}

function toLibraryMaterialAttachments(
    attachments: UpdateMaterialRequestV1["attachments"] | undefined,
    fallback: LibraryMaterial["attachments"] = [],
): LibraryMaterial["attachments"] {
    if (!attachments) {
        return fallback;
    }

    return attachments.map((attachment) => ({
        id: attachment.id ?? 0,
        name: attachment.originalName ?? "",
        storageKey: attachment.storageKey ?? "",
        mimeType: attachment.mimeType ?? "",
        size: attachment.sizeBytes ?? 0,
        kind: attachment.kind ?? "file",
        openaiFileId: attachment.openaiFileId,
        openaiFilePurpose: attachment.openaiFilePurpose,
        openaiFileStatus: attachment.openaiFileStatus,
        openaiFileError: attachment.openaiFileError,
        openaiUploadedAt: attachment.openaiUploadedAt,
    }));
}

function replaceMaterialInInfiniteCache(
    data: MaterialsInfiniteData,
    materialId: number,
    payload: UpdateMaterialRequestV1,
): MaterialsInfiniteData {
    return {
        ...data,
        pages: data.pages.map((page) => ({
            ...page,
            items: page.items.map((item) =>
                item.id === materialId
                    ? {
                          ...item,
                          ...payload,
                          attachments: toLibraryMaterialAttachments(payload.attachments, item.attachments ?? []),
                          hasText: Boolean(payload.text ?? item.text),
                      }
                    : item,
            ),
        })),
    };
}

function writeMaterialIntoDetailCache(
    queryClient: ReturnType<typeof useQueryClient>,
    materialId: number,
    payload: UpdateMaterialRequestV1,
) {
    queryClient.setQueryData<MaterialResponseV1 | null>(
        libraryQueryKeys.materialDetail(String(materialId)),
        (current) => {
            if (!current?.material) {
                return current;
            }

            return {
                ...current,
                material: {
                    ...current.material,
                    ...payload,
                    attachments: toLibraryMaterialAttachments(
                        payload.attachments,
                        current.material.attachments ?? [],
                    ),
                    hasText: Boolean(payload.text ?? current.material.text),
                },
            } as MaterialResponseV1;
        },
    );
}

/**
 * Uploads a file directly to object storage via a presigned PUT URL, bypassing the app
 * server entirely — the backend never buffers the file's bytes. The PUT itself targets a
 * third-party presigned S3 URL, not our backend, so it deliberately uses the native `fetch`
 * API rather than the shared `apiClient` (openapi-fetch only knows our own documented
 * endpoints and would attach an irrelevant Bearer token to an external host).
 */
export function useUploadMaterialFileMutation() {
    return useMutation({
        mutationFn: async (file: File) => {
            const contentType = file.type || "application/octet-stream";

            const { data, error } = await apiClient.POST("/api/v1/materials/upload-url", {
                body: {
                    fileName: file.name,
                    contentType,
                    size: file.size,
                },
            });

            if (error || !data?.uploadUrl || !data?.storageKey) {
                throw new Error(getApiErrorMessage(error, `Failed to prepare upload for: ${file.name}`));
            }

            let putResponse: Response;
            try {
                putResponse = await fetch(data.uploadUrl, {
                    method: "PUT",
                    headers: {
                        "Content-Type": contentType,
                    },
                    body: file,
                });
            } catch {
                throw new Error(
                    `Could not reach storage to upload "${file.name}". Check your connection and try again.`,
                );
            }

            if (!putResponse.ok) {
                throw new Error(`Failed to upload "${file.name}" (storage rejected the upload).`);
            }

            return {
                storageKey: data.storageKey,
                originalName: file.name,
                mimeType: contentType,
                size: file.size,
            };
        },
    });
}

export function useCreateMaterialMutation() {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: async (payload: CreateMaterialRequestV1) => {
            const { data, error } = await apiClient.POST("/api/v1/materials", { body: payload });

            if (error) {
                throw new Error(getApiErrorMessage(error, "Failed to create material."));
            }

            return data;
        },
        onSuccess: (data) => {
            if (data?.material) {
                // The create response is already full-fidelity, so build the card directly
                // instead of routing through normalizeMaterial (which widens a bounded summary).
                const normalized: LibraryMaterial = {
                    ...data.material,
                    hasText: Boolean(data.material.text),
                    attachments: data.material.attachments ?? [],
                };
                queryClient.setQueriesData({ queryKey: libraryQueryKeys.materials }, (current) => {
                    if (!isMaterialsInfiniteData(current)) {
                        return current;
                    }
                    return prependMaterialToInfiniteCache(current, normalized);
                });
            }
            void queryClient.invalidateQueries({ queryKey: libraryQueryKeys.materials });
        },
    });
}

export function useUpdateMaterialMutation() {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: async ({ id, payload }: { id: number; payload: UpdateMaterialRequestV1 }) => {
            const { data, error } = await apiClient.PUT("/api/v1/materials/{id}", {
                params: { path: { id } },
                body: payload,
            });

            if (error) {
                throw new Error(getApiErrorMessage(error, "Failed to update material."));
            }

            return data;
        },
        onSuccess: async (_data, { id, payload }) => {
            writeMaterialIntoDetailCache(queryClient, id, payload);
            queryClient.setQueriesData({ queryKey: libraryQueryKeys.materials }, (current) => {
                if (!isMaterialsInfiniteData(current)) {
                    return current;
                }
                return replaceMaterialInInfiniteCache(current, id, payload);
            });
            await queryClient.invalidateQueries({ queryKey: libraryQueryKeys.materials });
        },
    });
}

export function useDeleteMaterialMutation() {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: async (id: number) => {
            const { data, error } = await apiClient.DELETE("/api/v1/materials/{id}", {
                params: { path: { id } },
            });

            if (error) {
                throw new Error(getApiErrorMessage(error, "Failed to delete material."));
            }

            return data;
        },
        onSuccess: (_data, id) => {
            // Infinite-query caches use ["library","materials", params], not a flat array.
            queryClient.setQueriesData({ queryKey: libraryQueryKeys.materials }, (current) => {
                if (!isMaterialsInfiniteData(current)) {
                    return current;
                }
                return removeMaterialFromInfiniteCache(current, id);
            });
            void queryClient.invalidateQueries({ queryKey: libraryQueryKeys.materials });
        },
    });
}
