import { useMutation } from "@tanstack/react-query";
import { apiClient } from "@/shared/api/client";
import { getApiErrorMessage } from "@/shared/api/apiError";

/** Uploads a profile avatar image. */
export function useUploadAvatarMutation() {
    return useMutation({
        mutationFn: async (file: File) => {
            const body = new FormData();
            body.append("file", file);

            const { data, error } = await apiClient.POST("/api/v1/users/me/avatar", {
                body: body as never,
                bodySerializer: (value) => value as unknown as FormData,
            });
            if (error) {
                throw new Error(getApiErrorMessage(error, "Failed to upload avatar."));
            }
            return data;
        },
    });
}
