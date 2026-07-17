import { useMutation, useQueryClient, type InfiniteData } from "@tanstack/react-query";
import { apiClient } from "@/shared/api/client";
import { getApiErrorMessage } from "@/shared/api/apiError";
import type { components } from "@/shared/api/generated/schema";
import { roadmapsKeys } from "@/features/roadmaps/api/queryKeys";
import { normalizeRoadmap } from "./normalizers";
import type { LibraryRoadmap, PagedResult } from "./types";
type RoadmapV1 = components["schemas"]["RoadmapV1"];
type CreateRoadmapRequestV1 = components["schemas"]["CreateRoadmapRequestV1"];
type UpdateRoadmapRequestV1 = components["schemas"]["UpdateRoadmapRequestV1"];
type RoadmapsInfiniteData = InfiniteData<PagedResult<LibraryRoadmap>>;

export interface LegacyRoadmapPayload {
    title: string;
    description?: string;
    tags?: string[];
    lessonIds?: number[];
}

function toCreateRoadmapRequest(payload: LegacyRoadmapPayload): CreateRoadmapRequestV1 {
    return {
        title: payload.title,
        description: payload.description,
        lessonIds: payload.lessonIds ?? [],
        tags: payload.tags,
    };
}

function toUpdateRoadmapRequest(payload: LegacyRoadmapPayload): UpdateRoadmapRequestV1 {
    return {
        title: payload.title,
        description: payload.description,
        lessonIds: payload.lessonIds,
        tags: payload.tags,
    };
}

function isRoadmapsInfiniteData(value: unknown): value is RoadmapsInfiniteData {
    return Boolean(value && typeof value === "object" && Array.isArray((value as RoadmapsInfiniteData).pages));
}

function replaceRoadmapInInfiniteCache(
    data: RoadmapsInfiniteData,
    roadmap: LibraryRoadmap,
): RoadmapsInfiniteData {
    return {
        ...data,
        pages: data.pages.map((page) => ({
            ...page,
            items: page.items.map((item) =>
                item.id === roadmap.id ? { ...item, ...roadmap } : item,
            ),
        })),
    };
}

function replaceRoadmapInArrayCache(current: RoadmapV1[] | undefined, roadmap: RoadmapV1) {
    return current?.map((item) => (item.id === roadmap.id ? { ...item, ...roadmap } : item)) ?? current;
}

function writeRoadmapIntoCaches(
    queryClient: ReturnType<typeof useQueryClient>,
    roadmap: RoadmapV1,
) {
    const normalized = normalizeRoadmap(roadmap);

    queryClient.setQueriesData({ queryKey: roadmapsKeys.all }, (current) => {
        if (isRoadmapsInfiniteData(current)) {
            return replaceRoadmapInInfiniteCache(current, normalized);
        }

        if (Array.isArray(current)) {
            return replaceRoadmapInArrayCache(current as RoadmapV1[], roadmap);
        }

        return current;
    });
}

export function useCreateRoadmapMutation() {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: async (payload: LegacyRoadmapPayload) => {
            const { data, error } = await apiClient.POST("/api/v1/roadmaps", {
                body: toCreateRoadmapRequest(payload),
            });

            if (error) {
                throw new Error(getApiErrorMessage(error, "Failed to create roadmap."));
            }

            return data;
        },
        onSuccess: async (data) => {
            if (data?.roadmap) {
                writeRoadmapIntoCaches(queryClient, data.roadmap);
            }
            await queryClient.invalidateQueries({ queryKey: roadmapsKeys.all });
        },
    });
}

export function useUpdateRoadmapMutation() {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: async ({ id, payload }: { id: number; payload: LegacyRoadmapPayload }) => {
            const { data, error } = await apiClient.PUT("/api/v1/roadmaps/{id}", {
                params: { path: { id } },
                body: toUpdateRoadmapRequest(payload),
            });

            if (error) {
                throw new Error(getApiErrorMessage(error, "Failed to update roadmap."));
            }

            return data;
        },
        onSuccess: async () => {
            await queryClient.invalidateQueries({ queryKey: roadmapsKeys.all });
        },
    });
}

export function useDeleteRoadmapMutation() {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: async (id: number) => {
            const { data, error } = await apiClient.DELETE("/api/v1/roadmaps/{id}", {
                params: { path: { id } },
            });

            if (error) {
                throw new Error(getApiErrorMessage(error, "Failed to delete roadmap."));
            }

            return data;
        },
        onSuccess: async () => {
            await queryClient.invalidateQueries({ queryKey: roadmapsKeys.all });
        },
    });
}

export function useRoadmapEnrollmentMutation() {
    const queryClient = useQueryClient();

    const enroll = useMutation({
        mutationFn: async (roadmapId: number) => {
            const { data, error } = await apiClient.POST("/api/v1/roadmaps/{id}/enrollment", {
                params: { path: { id: roadmapId } },
            });

            if (error) {
                throw new Error(getApiErrorMessage(error, "Failed to subscribe to roadmap."));
            }

            return data;
        },
        onMutate: async (roadmapId) => {
            await queryClient.cancelQueries({ queryKey: roadmapsKeys.all });
            const previousRoadmaps = queryClient.getQueryData<RoadmapV1[]>(roadmapsKeys.all);
            queryClient.setQueryData<RoadmapV1[]>(roadmapsKeys.all, (old) =>
                old?.map((roadmap) =>
                    roadmap.id === roadmapId
                        ? { ...roadmap, isEnrolled: true, enrolledAt: new Date().toISOString() }
                        : roadmap,
                ) ?? old,
            );
            return { previousRoadmaps };
        },
        onError: (_err, _roadmapId, context) => {
            if (context?.previousRoadmaps) {
                queryClient.setQueryData(roadmapsKeys.all, context.previousRoadmaps);
            }
        },
        onSettled: async () => {
            await queryClient.invalidateQueries({ queryKey: roadmapsKeys.all });
        },
    });

    const unenroll = useMutation({
        mutationFn: async (roadmapId: number) => {
            const { data, error } = await apiClient.DELETE("/api/v1/roadmaps/{id}/enrollment", {
                params: { path: { id: roadmapId } },
            });

            if (error) {
                throw new Error(getApiErrorMessage(error, "Failed to unsubscribe from roadmap."));
            }

            return data;
        },
        onMutate: async (roadmapId) => {
            await queryClient.cancelQueries({ queryKey: roadmapsKeys.all });
            const previousRoadmaps = queryClient.getQueryData<RoadmapV1[]>(roadmapsKeys.all);
            queryClient.setQueryData<RoadmapV1[]>(roadmapsKeys.all, (old) =>
                old?.map((roadmap) =>
                    roadmap.id === roadmapId ? { ...roadmap, isEnrolled: false, enrolledAt: undefined } : roadmap,
                ) ?? old,
            );
            return { previousRoadmaps };
        },
        onError: (_err, _roadmapId, context) => {
            if (context?.previousRoadmaps) {
                queryClient.setQueryData(roadmapsKeys.all, context.previousRoadmaps);
            }
        },
        onSettled: async () => {
            await queryClient.invalidateQueries({ queryKey: roadmapsKeys.all });
        },
    });

    return { enroll, unenroll };
}
