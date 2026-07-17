import { useMutation, useQueryClient } from "@tanstack/react-query";
import { apiClient } from "@/shared/api/client";
import { extractApiError } from "@/shared/api/extractApiError";
import type { components } from "@/shared/api/generated/schema";
import { activityPlayerKeys, lessonReaderKeys } from "@/features/lessons/api/queryKeys";
import { roadmapsKeys } from "@/features/roadmaps/api/queryKeys";

type ActivityProgressResponseV1 = components["schemas"]["ActivityProgressResponseV1"];

interface SubmitQuizInput {
    lessonId: number;
    activityId: number;
    answers: string[][];
}

interface SubmitFlashcardsInput {
    lessonId: number;
    activityId: number;
    reviewedCards: number;
}

export function useSubmitActivityProgressMutation() {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: async (
            input: SubmitQuizInput | SubmitFlashcardsInput,
        ): Promise<ActivityProgressResponseV1> => {
            const body =
                "answers" in input
                    ? { type: "quiz" as const, answers: input.answers }
                    : { type: "flashcards" as const, reviewedCards: input.reviewedCards };

            const { data, error } = await apiClient.POST(
                "/api/v1/lessons/{id}/activities/{activityId}/progress",
                {
                    params: {
                        path: { id: input.lessonId, activityId: input.activityId },
                    },
                    body,
                },
            );

            if (error || !data) {
                throw new Error(extractApiError(error, "Failed to save activity progress."));
            }
            return data;
        },
        onSuccess: (_data, variables) => {
            queryClient.invalidateQueries({
                queryKey: activityPlayerKeys.detail(
                    String(variables.lessonId),
                    String(variables.activityId),
                ),
            });
            queryClient.invalidateQueries({
                queryKey: lessonReaderKeys.detail(String(variables.lessonId)),
            });
            queryClient.invalidateQueries({ queryKey: roadmapsKeys.all });
        },
    });
}

export function useResetActivityProgressMutation() {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: async ({ lessonId, activityId }: { lessonId: number; activityId: number }) => {
            const { error } = await apiClient.DELETE(
                "/api/v1/lessons/{id}/activities/{activityId}/progress",
                {
                    params: { path: { id: lessonId, activityId } },
                },
            );
            if (error) {
                throw new Error(extractApiError(error, "Failed to reset activity progress."));
            }
        },
        onSuccess: (_data, variables) => {
            queryClient.invalidateQueries({
                queryKey: activityPlayerKeys.detail(
                    String(variables.lessonId),
                    String(variables.activityId),
                ),
            });
            queryClient.invalidateQueries({
                queryKey: lessonReaderKeys.detail(String(variables.lessonId)),
            });
        },
    });
}
