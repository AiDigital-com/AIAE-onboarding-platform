import { useMutation, useQueryClient } from "@tanstack/react-query";
import { apiClient } from "@/shared/api/client";
import { getApiErrorMessage } from "@/shared/api/apiError";

type RoleCode = "admin" | "teamlead" | "member";

/** Sets a user's role to Admin, Team Lead, or User. Admin only. */
export function useAssignUserRoleMutation() {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: async ({ userId, roleCode }: { userId: number; roleCode: RoleCode }) => {
            const { data, error } = await apiClient.PATCH("/api/v1/admin/users/{id}/role", {
                params: { path: { id: userId } },
                body: { roleCode },
            });

            if (error) {
                throw new Error(getApiErrorMessage(error, "Failed to update role."));
            }

            return data;
        },
        onSuccess: () => {
            void queryClient.invalidateQueries({ queryKey: ["admin", "users"] });
            void queryClient.invalidateQueries({ queryKey: ["groups"] });
            void queryClient.invalidateQueries({ queryKey: ["teams"] });
            void queryClient.invalidateQueries({ queryKey: ["session", "permissions"] });
            void queryClient.invalidateQueries({ queryKey: ["permissions", "management"] });
        },
    });
}
