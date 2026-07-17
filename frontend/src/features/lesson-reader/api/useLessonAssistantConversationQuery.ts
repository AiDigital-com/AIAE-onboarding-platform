import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { apiClient } from "@/shared/api/client";
import { extractApiError } from "@/shared/api/extractApiError";
import type { components } from "@/shared/api/generated/schema";

type LessonAssistantConversationResponseV1 = components["schemas"]["LessonAssistantConversationResponseV1"];

function lessonAssistantConversationQueryKey(lessonId: number) {
    return ["lesson-assistant-conversation", lessonId] as const;
}

/** Loads the learner's saved lesson-assistant chat for one lesson, so it can be restored after navigating away. */
export function useLessonAssistantConversationQuery(lessonId: number, options?: { enabled?: boolean }) {
    return useQuery({
        queryKey: lessonAssistantConversationQueryKey(lessonId),
        enabled: options?.enabled ?? true,
        queryFn: async (): Promise<LessonAssistantConversationResponseV1> => {
            const { data, error } = await apiClient.GET("/api/v1/lessons/{id}/assistant-conversation", {
                params: { path: { id: lessonId } },
            });
            if (error || !data) {
                throw new Error(extractApiError(error, "Failed to load the saved chat."));
            }
            return data;
        },
    });
}

/** Deletes the learner's saved lesson-assistant chat for one lesson. */
export function useClearLessonAssistantConversationMutation() {
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: async (lessonId: number): Promise<void> => {
            const { error } = await apiClient.DELETE("/api/v1/lessons/{id}/assistant-conversation", {
                params: { path: { id: lessonId } },
            });
            if (error) {
                throw new Error(extractApiError(error, "Failed to clear the saved chat."));
            }
        },
        onSuccess: (_data, lessonId) => {
            queryClient.setQueryData(lessonAssistantConversationQueryKey(lessonId), {
                messages: [],
                preset: "regular",
            });
        },
    });
}
