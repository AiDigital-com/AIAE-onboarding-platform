import { useMutation } from "@tanstack/react-query";
import { apiClient } from "@/shared/api/client";
import { extractApiError } from "@/shared/api/extractApiError";
import type { components } from "@/shared/api/generated/schema";

type AskLessonResponseV1 = components["schemas"]["AskLessonResponseV1"];
type ChatMessageV1 = components["schemas"]["ChatMessageV1"];
export type LessonAssistantPresetV1 = components["schemas"]["LessonAssistantPresetV1"];

interface AskLessonInput {
    lessonId: number;
    question: string;
    history: ChatMessageV1[];
    preset?: LessonAssistantPresetV1;
}

export function useAskLessonMutation() {
    return useMutation({
        mutationFn: async (
            { lessonId, question, history, preset }: AskLessonInput,
        ): Promise<AskLessonResponseV1> => {
            const { data, error } = await apiClient.POST("/api/v1/lessons/{id}/ask", {
                params: { path: { id: lessonId } },
                body: { question, history, preset },
            });
            if (error || !data) {
                throw new Error(extractApiError(error, "Failed to answer the question."));
            }
            return data;
        },
    });
}
