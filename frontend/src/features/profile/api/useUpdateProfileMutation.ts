import { useMutation, useQueryClient } from "@tanstack/react-query";
import type { components } from "@/shared/api/generated/schema";
import { apiClient } from "@/shared/api/client";

type UpdateProfileRequestV1 = components["schemas"]["UpdateProfileRequestV1"];

/** Updates the signed-in user's profile fields. */
export function useUpdateProfileMutation() {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: async (body: UpdateProfileRequestV1) => {
            const { data, error } = await apiClient.PATCH("/api/v1/users/me", { body });
            if (error) throw error;
            return data;
        },
        onSuccess: (data) => {
            if (data) {
                queryClient.setQueryData(["session", "me"], data);
            }
        },
    });
}
