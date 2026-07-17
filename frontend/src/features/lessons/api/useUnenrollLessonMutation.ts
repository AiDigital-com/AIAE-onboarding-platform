import { useMutation, useQueryClient } from "@tanstack/react-query";
import { apiClient } from "@/shared/api/client";
import { extractApiError } from "@/shared/api/extractApiError";
import { lessonsKeys } from "./queryKeys";
import type { EnrolledLessonCard } from "./types";

export function useUnenrollLessonMutation() {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: async (lessonId: number) => {
            const { error } = await apiClient.DELETE("/api/v1/lessons/{id}/enrollment", {
                params: { path: { id: lessonId } },
            });
            if (error) {
                throw new Error(extractApiError(error, "Failed to remove lesson from My Lessons."));
            }
        },
        onMutate: async (lessonId: number) => {
            await queryClient.cancelQueries({ queryKey: lessonsKeys.my() });
            const previousLessons = queryClient.getQueryData<EnrolledLessonCard[]>(lessonsKeys.my());

            queryClient.setQueryData<EnrolledLessonCard[]>(lessonsKeys.my(), (current = []) =>
                current.filter((lesson) => lesson.id !== lessonId),
            );

            return { previousLessons };
        },
        onError: (_error, _lessonId, context) => {
            if (context?.previousLessons) {
                queryClient.setQueryData(lessonsKeys.my(), context.previousLessons);
            }
        },
        onSettled: () => {
            queryClient.invalidateQueries({ queryKey: lessonsKeys.my() });
        },
    });
}
