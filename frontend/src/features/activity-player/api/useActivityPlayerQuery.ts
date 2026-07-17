import { useQuery } from "@tanstack/react-query";
import { apiClient } from "@/shared/api/client";
import { getApiErrorMessage } from "@/shared/api/apiError";
import type {
    ActivityAttemptV1,
    LessonActivityV1,
    LessonV1,
} from "@/features/lessons/api/types";
import { activityPlayerKeys } from "@/features/lessons/api/queryKeys";
import { lessonDetailQueryOptions } from "@/features/lessons/api/lessonDetailQueryOptions";
import { DETAIL_QUERY_OPTIONS } from "@/shared/api/queryPolicies";

export interface ActivityPlayerData {
    lesson: LessonV1;
    activity: LessonActivityV1;
    attempts: ActivityAttemptV1[];
    lessonActivities: LessonActivityV1[];
}

export function useActivityPlayerQuery(lessonId: string, activityId: string) {
    // Shares the ["lessons","detail",lessonId] cache entry with useLessonReaderQuery.
    // If the user came from LessonDetail, this returns immediately without a network request.
    const lessonQuery = useQuery(lessonDetailQueryOptions(lessonId));

    const activityQuery = useQuery({
        queryKey: activityPlayerKeys.detail(lessonId, activityId),
        queryFn: async ({ signal }) => {
            const numericLessonId = Number(lessonId);
            const numericActivityId = Number(activityId);
            if (!Number.isFinite(numericLessonId) || !Number.isFinite(numericActivityId)) {
                throw new Error("Invalid lesson or activity id.");
            }

            const { data, error } = await apiClient.GET("/api/v1/lessons/{id}/activities/{activityId}", {
                params: { path: { id: numericLessonId, activityId: numericActivityId } },
                signal,
            });

            if (error) {
                throw new Error(getApiErrorMessage(error, "Failed to load activity."));
            }

            return data ?? null;
        },
        ...DETAIL_QUERY_OPTIONS,
        retry: false,
    });

    const isLoading = lessonQuery.isLoading || activityQuery.isLoading;
    const lessonData = lessonQuery.data;
    const activityData = activityQuery.data;

    const data: ActivityPlayerData | null =
        lessonData && activityData
            ? {
                  lesson: lessonData.lesson as LessonV1,
                  lessonActivities: (lessonData.activities ?? []) as LessonActivityV1[],
                  activity: activityData.activity as LessonActivityV1,
                  attempts: (activityData.attempts ?? []) as ActivityAttemptV1[],
              }
            : null;

    return { data, isLoading };
}
