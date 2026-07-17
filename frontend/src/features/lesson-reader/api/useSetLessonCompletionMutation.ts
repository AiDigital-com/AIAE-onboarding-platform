import { useMutation, useQueryClient } from "@tanstack/react-query";
import { apiClient } from "@/shared/api/client";
import { extractApiError } from "@/shared/api/extractApiError";
import type { components } from "@/shared/api/generated/schema";
import { lessonReaderKeys, lessonsKeys } from "@/features/lessons/api/queryKeys";
import { roadmapsKeys } from "@/features/roadmaps/api/queryKeys";

type EnrollmentResponseV1 = components["schemas"]["EnrollmentResponseV1"];

interface SetCompletionInput {
    lessonId: number;
    completed: boolean;
}

export function useSetLessonCompletionMutation() {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: async ({ lessonId, completed }: SetCompletionInput): Promise<EnrollmentResponseV1> => {
            const { data, error } = await apiClient.PATCH("/api/v1/lessons/{id}/enrollment", {
                params: { path: { id: lessonId } },
                body: { completed },
            });
            if (error || !data) {
                throw new Error(extractApiError(error, "Failed to update lesson progress."));
            }
            return data;
        },
        onSuccess: (_data, variables) => {
            queryClient.invalidateQueries({
                queryKey: lessonReaderKeys.detail(String(variables.lessonId)),
            });
            queryClient.invalidateQueries({ queryKey: lessonsKeys.my() });
            queryClient.invalidateQueries({ queryKey: roadmapsKeys.all });
        },
    });
}
