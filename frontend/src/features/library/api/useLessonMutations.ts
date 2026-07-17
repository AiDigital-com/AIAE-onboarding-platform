import { useMutation, useQueryClient } from "@tanstack/react-query";
import { apiClient } from "@/shared/api/client";
import { getApiErrorMessage } from "@/shared/api/apiError";
import type { components } from "@/shared/api/generated/schema";
import { assertBrowserPlayableVideo, inferVideoMimeType } from "@/shared/lib/videoFileValidation";
import { libraryQueryKeys } from "./queryKeys";
import { toLessonContentPreview } from "./normalizers";
import type { LibraryLesson } from "./types";
import { activityPlayerKeys, lessonsKeys, lessonReaderKeys } from "@/features/lessons/api/queryKeys";

type CreateLessonRequestV1 = components["schemas"]["CreateLessonRequestV1"];
type UpdateLessonContentRequestV1 = components["schemas"]["UpdateLessonContentRequestV1"];
type ChangeLessonStatusRequestV1 = components["schemas"]["ChangeLessonStatusRequestV1"];
type ReviseLessonRequestV1 = components["schemas"]["ReviseLessonRequestV1"];
type GenerateActivityRequestV1 = components["schemas"]["GenerateActivityRequestV1"];
type UpdateActivityRequestV1 = components["schemas"]["UpdateActivityRequestV1"];
type AddLessonAssetRequestV1 = components["schemas"]["AddLessonAssetRequestV1"];
type LessonV1 = components["schemas"]["LessonV1"];
type LessonDetailResponseV1 = components["schemas"]["LessonDetailResponseV1"];
type LessonActivityV1 = components["schemas"]["LessonActivityV1"];
type LessonActivityDetailResponseV1 = components["schemas"]["LessonActivityDetailResponseV1"];
type ActivityResponseV1 = components["schemas"]["ActivityResponseV1"];

export interface LegacyCreateLessonPayload {
    action?: "generate" | "create-manual";
    materialIds?: number[];
    userInstructions?: string;
    depth?: string;
    tone?: string;
    desiredFormat?: string;
    tags?: string[];
    title?: string;
    description?: string;
    contentHtml?: string;
}

function toCreateLessonRequest(payload: LegacyCreateLessonPayload): CreateLessonRequestV1 {
    if (payload.action === "create-manual") {
        return {
            action: "create-manual",
            title: payload.title,
            instructions: payload.description || payload.userInstructions,
            description: payload.description,
            contentHtml: payload.contentHtml,
            tags: payload.tags,
        };
    }

    return {
        action: "generate",
        title: payload.title,
        instructions: payload.userInstructions,
        depth: payload.depth,
        tone: payload.tone,
        desiredFormat: payload.desiredFormat,
        materialIds: payload.materialIds,
        tags: payload.tags,
    };
}

export function useCreateLessonMutation() {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: async (payload: LegacyCreateLessonPayload) => {
            const { data, error } = await apiClient.POST("/api/v1/lessons", {
                body: toCreateLessonRequest(payload),
            });

            if (error) {
                const fallback =
                    payload.action === "generate"
                        ? "Failed to generate lesson."
                        : "Failed to create lesson.";
                throw new Error(getApiErrorMessage(error, fallback));
            }

            return data;
        },
        onSuccess: async () => {
            await queryClient.invalidateQueries({ queryKey: libraryQueryKeys.lessons });
        },
    });
}

export function useUpdateLessonContentMutation() {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: async ({ id, payload }: { id: number; payload: UpdateLessonContentRequestV1 }) => {
            const { data, error } = await apiClient.PUT("/api/v1/lessons/{id}", {
                params: { path: { id } },
                body: payload,
            });

            if (error) {
                throw new Error(getApiErrorMessage(error, "Failed to save lesson."));
            }

            return data;
        },
        onSuccess: async (data, { id, payload }) => {
            if (data?.lesson) {
                writeLessonIntoDetailCache(queryClient, id, data.lesson);
                queryClient.setQueryData<LibraryLesson[]>(libraryQueryKeys.lessons, (current = []) =>
                    current.map((lesson) =>
                        lesson.id === id
                            ? {
                                  ...lesson,
                                  ...(data.lesson as unknown as LibraryLesson),
                                  contentHtmlPreview: toLessonContentPreview(data.lesson!.contentHtml),
                                  contentMarkdownPreview: toLessonContentPreview(data.lesson!.contentMarkdown),
                                  tags: payload.tags ?? data.lesson.tags ?? lesson.tags ?? [],
                              }
                            : lesson,
                    ),
                );
            }
            await queryClient.invalidateQueries({ queryKey: libraryQueryKeys.lessons });
            await queryClient.invalidateQueries({ queryKey: lessonReaderKeys.detail(String(id)) });
        },
    });
}

export function useChangeLessonStatusMutation() {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: async ({ id, payload }: { id: number; payload: ChangeLessonStatusRequestV1 }) => {
            const { data, error } = await apiClient.PATCH("/api/v1/lessons/{id}/status", {
                params: { path: { id } },
                body: payload,
            });

            if (error) {
                throw new Error(getApiErrorMessage(error, "Failed to change lesson status."));
            }

            return data;
        },
        onSuccess: async (_data, { id }) => {
            await queryClient.invalidateQueries({ queryKey: libraryQueryKeys.lessons });
            await queryClient.invalidateQueries({ queryKey: lessonReaderKeys.detail(String(id)) });
        },
    });
}

export function useDeleteLessonMutation() {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: async (id: number) => {
            const { data, error } = await apiClient.DELETE("/api/v1/lessons/{id}", {
                params: { path: { id } },
            });

            if (error) {
                throw new Error(getApiErrorMessage(error, "Failed to delete lesson."));
            }

            return data;
        },
        onSuccess: async () => {
            await queryClient.invalidateQueries({ queryKey: libraryQueryKeys.lessons });
        },
    });
}

export function useReviseLessonMutation() {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: async ({ id, payload }: { id: number; payload: ReviseLessonRequestV1 }) => {
            const { data, error } = await apiClient.POST("/api/v1/lessons/{id}/revisions", {
                params: { path: { id } },
                body: payload,
            });

            if (error) {
                throw new Error(getApiErrorMessage(error, "Failed to revise lesson."));
            }

            return data;
        },
        onSuccess: async (_data, { id }) => {
            await queryClient.invalidateQueries({ queryKey: libraryQueryKeys.lessons });
            await queryClient.invalidateQueries({ queryKey: lessonReaderKeys.detail(String(id)) });
        },
    });
}

export function useGenerateActivityMutation() {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: async ({ id, payload }: { id: number; payload: GenerateActivityRequestV1 }) => {
            const { data, error } = await apiClient.POST("/api/v1/lessons/{id}/activities", {
                params: { path: { id } },
                body: payload,
            });

            if (error) {
                throw new Error(getApiErrorMessage(error, "Failed to generate activity."));
            }

            return data;
        },
        onSuccess: async (_data, { id }) => {
            await queryClient.invalidateQueries({ queryKey: libraryQueryKeys.lessons });
            await queryClient.invalidateQueries({ queryKey: lessonReaderKeys.detail(String(id)) });
        },
    });
}

export function useUpdateActivityMutation() {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: async ({
            lessonId,
            activityId,
            payload,
        }: {
            lessonId: number;
            activityId: number;
            payload: UpdateActivityRequestV1;
        }) => {
            const { data, error } = await apiClient.PUT("/api/v1/lessons/{id}/activities/{activityId}", {
                params: { path: { id: lessonId, activityId } },
                body: payload,
            });

            if (error) {
                throw new Error(getApiErrorMessage(error, "Failed to update activity."));
            }

            return data;
        },
        onSuccess: async (data, { lessonId, activityId }) => {
            writeActivityIntoDetailCaches(queryClient, lessonId, activityId, data);

            await queryClient.invalidateQueries({ queryKey: libraryQueryKeys.lessons });
            await queryClient.invalidateQueries({ queryKey: lessonReaderKeys.detail(String(lessonId)) });
            await queryClient.invalidateQueries({
                queryKey: activityPlayerKeys.detail(String(lessonId), String(activityId)),
            });
        },
    });
}

/** Deletes a lesson activity and returns the lesson's refreshed activity list. */
export function useDeleteActivityMutation() {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: async ({ lessonId, activityId }: { lessonId: number; activityId: number }) => {
            const { data, error } = await apiClient.DELETE("/api/v1/lessons/{id}/activities/{activityId}", {
                params: { path: { id: lessonId, activityId } },
            });

            if (error) {
                throw new Error(getApiErrorMessage(error, "Failed to delete activity."));
            }

            return data;
        },
        onSuccess: async (data, { lessonId, activityId }) => {
            await queryClient.invalidateQueries({ queryKey: libraryQueryKeys.lessons });
            await queryClient.invalidateQueries({ queryKey: lessonReaderKeys.detail(String(lessonId)) });
            await queryClient.invalidateQueries({
                queryKey: activityPlayerKeys.detail(String(lessonId), String(activityId)),
            });
            if (data?.activities) {
                queryClient.setQueryData(lessonReaderKeys.detail(String(lessonId)), (current: unknown) => {
                    if (!current || typeof current !== "object") {
                        return current;
                    }
                    return { ...(current as Record<string, unknown>), activities: data.activities };
                });
            }
        },
    });
}

/**
 * Uploads a file directly to object storage via a presigned PUT URL, bypassing the app
 * server entirely — the backend never buffers the file's bytes. The PUT itself targets a
 * third-party presigned S3 URL, not our backend, so it deliberately uses the native `fetch`
 * API rather than the shared `apiClient` (openapi-fetch only knows our own documented
 * endpoints and would attach an irrelevant Bearer token to an external host).
 */
async function uploadFileViaPresignedUrl(file: File, contentType: string) {
    const { data, error } = await apiClient.POST("/api/v1/lessons/upload-url", {
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
        // A network-level failure here (e.g. the storage host rejecting the
        // cross-origin request) surfaces from fetch() as an opaque, unreadable
        // TypeError such as "Failed to fetch" — replace it with something actionable.
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
}

export function useUploadLessonFileMutation() {
    return useMutation({
        mutationFn: async (file: File) => {
            const contentType = file.type || "application/octet-stream";
            return uploadFileViaPresignedUrl(file, contentType);
        },
    });
}

export function useUploadLessonVideoMutation() {
    return useMutation({
        mutationFn: async (file: File) => {
            await assertBrowserPlayableVideo(file);
            const contentType = file.type || inferVideoMimeType(file.name) || "application/octet-stream";
            return uploadFileViaPresignedUrl(file, contentType);
        },
    });
}

/**
 * Writes the authoritative lesson returned by an asset mutation into the cached
 * lesson detail, so re-opening the lesson does not serve a pre-mutation snapshot
 * (the dialog reads the detail cache via ensureQueryData, which does not refetch
 * inactive stale entries on its own).
 */
function writeLessonIntoDetailCache(
    queryClient: ReturnType<typeof useQueryClient>,
    lessonId: number,
    lesson: LessonV1 | undefined,
) {
    if (!lesson) {
        return;
    }
    queryClient.setQueryData<LessonDetailResponseV1 | null>(
        lessonReaderKeys.detail(String(lessonId)),
        (current) => (current ? { ...current, lesson } : current),
    );
}

function mergeUpdatedActivity(
    currentActivities: LessonActivityV1[],
    updatedActivity: LessonActivityV1 | undefined,
    responseActivities: LessonActivityV1[] | undefined,
) {
    if (responseActivities) {
        return responseActivities;
    }
    if (!updatedActivity) {
        return currentActivities;
    }

    let replaced = false;
    const nextActivities = currentActivities.map((activity) => {
        if (activity.id !== updatedActivity.id) {
            return activity;
        }

        replaced = true;
        return { ...activity, ...updatedActivity };
    });

    return replaced ? nextActivities : [updatedActivity, ...nextActivities];
}

/**
 * Activity updates return the authoritative updated activity and a compact lesson-with-activities
 * shape. Keep the canonical lesson-detail cache in sync without replacing the full cached lesson
 * object with that compact shape; otherwise reopening the dialog can show stale flashcard/quiz
 * payloads from the previous detail snapshot.
 */
function writeActivityIntoDetailCaches(
    queryClient: ReturnType<typeof useQueryClient>,
    lessonId: number,
    activityId: number,
    response: ActivityResponseV1 | undefined,
) {
    const updatedActivity = response?.activity;
    const responseActivities = response?.lesson?.activities;

    queryClient.setQueryData<LessonDetailResponseV1 | null>(
        lessonReaderKeys.detail(String(lessonId)),
        (current) => {
            if (!current) {
                return current;
            }

            return {
                ...current,
                activities: mergeUpdatedActivity(current.activities, updatedActivity, responseActivities),
            };
        },
    );

    if (!updatedActivity) {
        return;
    }

    queryClient.setQueryData<LessonActivityDetailResponseV1 | null>(
        activityPlayerKeys.detail(String(lessonId), String(activityId)),
        (current) => {
            if (!current) {
                return current;
            }

            return {
                ...current,
                activity: { ...current.activity, ...updatedActivity },
            };
        },
    );
}

export function useAddLessonAssetMutation() {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: async ({ lessonId, payload }: { lessonId: number; payload: AddLessonAssetRequestV1 }) => {
            const { data, error } = await apiClient.POST("/api/v1/lessons/{id}/assets", {
                params: { path: { id: lessonId } },
                body: payload,
            });

            if (error) {
                throw new Error(getApiErrorMessage(error, "Failed to add asset."));
            }

            return data;
        },
        onSuccess: async (data, { lessonId }) => {
            writeLessonIntoDetailCache(queryClient, lessonId, data?.lesson);
            await queryClient.invalidateQueries({ queryKey: libraryQueryKeys.lessons });
        },
    });
}

export function useDeleteLessonAssetMutation() {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: async ({ lessonId, assetId }: { lessonId: number; assetId: number }) => {
            const { data, error } = await apiClient.DELETE("/api/v1/lessons/{id}/assets/{assetId}", {
                params: { path: { id: lessonId, assetId } },
            });

            if (error) {
                throw new Error(getApiErrorMessage(error, "Failed to delete asset."));
            }

            return data;
        },
        onSuccess: async (data, { lessonId }) => {
            writeLessonIntoDetailCache(queryClient, lessonId, data?.lesson);
            await queryClient.invalidateQueries({ queryKey: libraryQueryKeys.lessons });
        },
    });
}

export function useTeacherVideoMutation() {
    const queryClient = useQueryClient();

    const refresh = useMutation({
        mutationFn: async (lessonId: number) => {
            const { data, error } = await apiClient.GET("/api/v1/lessons/{id}/teacher-video", {
                params: { path: { id: lessonId } },
            });

            if (error) {
                throw new Error(getApiErrorMessage(error, "Failed to refresh teacher video."));
            }

            return data;
        },
        onSuccess: async (_data, lessonId) => {
            await queryClient.invalidateQueries({ queryKey: libraryQueryKeys.lessons });
            await queryClient.invalidateQueries({ queryKey: lessonReaderKeys.detail(String(lessonId)) });
        },
    });

    const generate = useMutation({
        mutationFn: async (lessonId: number) => {
            const { data, error } = await apiClient.POST("/api/v1/lessons/{id}/teacher-video", {
                params: { path: { id: lessonId } },
                body: {},
            });

            if (error) {
                throw new Error(getApiErrorMessage(error, "Failed to generate teacher video."));
            }

            return data;
        },
        onSuccess: async (_data, lessonId) => {
            await queryClient.invalidateQueries({ queryKey: libraryQueryKeys.lessons });
            await queryClient.invalidateQueries({ queryKey: lessonReaderKeys.detail(String(lessonId)) });
        },
    });

    const remove = useMutation({
        mutationFn: async (lessonId: number) => {
            const { data, error } = await apiClient.DELETE("/api/v1/lessons/{id}/teacher-video", {
                params: { path: { id: lessonId } },
            });

            if (error) {
                throw new Error(getApiErrorMessage(error, "Failed to remove teacher video."));
            }

            return data;
        },
        onSuccess: async (_data, lessonId) => {
            await queryClient.invalidateQueries({ queryKey: libraryQueryKeys.lessons });
            await queryClient.invalidateQueries({ queryKey: lessonReaderKeys.detail(String(lessonId)) });
        },
    });

    return { refresh, generate, remove };
}

export function useLessonEnrollmentMutation() {
    const queryClient = useQueryClient();

    const enroll = useMutation({
        mutationFn: async (lessonId: number) => {
            const { data, error } = await apiClient.POST("/api/v1/lessons/{id}/enrollment", {
                params: { path: { id: lessonId } },
            });

            if (error) {
                throw new Error(getApiErrorMessage(error, "Failed to add lesson to My Lessons."));
            }

            return data;
        },
        onMutate: async (lessonId) => {
            await queryClient.cancelQueries({ queryKey: libraryQueryKeys.lessons });
            const previousLessons = queryClient.getQueryData<LibraryLesson[]>(libraryQueryKeys.lessons);
            queryClient.setQueryData<LibraryLesson[]>(libraryQueryKeys.lessons, (old) =>
                old?.map((lesson) => (lesson.id === lessonId ? { ...lesson, isEnrolled: true } : lesson)) ?? old,
            );
            return { previousLessons };
        },
        onError: (_err, _lessonId, context) => {
            if (context?.previousLessons) {
                queryClient.setQueryData(libraryQueryKeys.lessons, context.previousLessons);
            }
        },
        onSettled: async () => {
            await queryClient.invalidateQueries({ queryKey: libraryQueryKeys.lessons });
            await queryClient.invalidateQueries({ queryKey: lessonsKeys.my() });
        },
    });

    const unenroll = useMutation({
        mutationFn: async (lessonId: number) => {
            const { data, error } = await apiClient.DELETE("/api/v1/lessons/{id}/enrollment", {
                params: { path: { id: lessonId } },
            });

            if (error) {
                throw new Error(getApiErrorMessage(error, "Failed to remove lesson from My Lessons."));
            }

            return data;
        },
        onMutate: async (lessonId) => {
            await queryClient.cancelQueries({ queryKey: libraryQueryKeys.lessons });
            const previousLessons = queryClient.getQueryData<LibraryLesson[]>(libraryQueryKeys.lessons);
            queryClient.setQueryData<LibraryLesson[]>(libraryQueryKeys.lessons, (old) =>
                old?.map((lesson) => (lesson.id === lessonId ? { ...lesson, isEnrolled: false } : lesson)) ?? old,
            );
            return { previousLessons };
        },
        onError: (_err, _lessonId, context) => {
            if (context?.previousLessons) {
                queryClient.setQueryData(libraryQueryKeys.lessons, context.previousLessons);
            }
        },
        onSettled: async () => {
            await queryClient.invalidateQueries({ queryKey: libraryQueryKeys.lessons });
            await queryClient.invalidateQueries({ queryKey: lessonsKeys.my() });
        },
    });

    return { enroll, unenroll };
}

export function mapLessonResponseToLibraryLesson(lesson: LibraryLesson): LibraryLesson {
    return lesson;
}
