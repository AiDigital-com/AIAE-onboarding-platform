import { useMutation, useQueryClient } from "@tanstack/react-query";
import { apiClient } from "@/shared/api/client";
import { getApiErrorMessage } from "@/shared/api/apiError";
import { groupsKeys } from "./queryKeys";

function invalidateGrades(queryClient: ReturnType<typeof useQueryClient>) {
    void queryClient.invalidateQueries({ queryKey: groupsKeys.grades });
    void queryClient.invalidateQueries({ queryKey: groupsKeys.gradesAll });
}

/** Creates a grade dictionary value. Requires grades.manage. */
export function useCreateGradeMutation() {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: async (name: string) => {
            const { data, error } = await apiClient.POST("/api/v1/grades", { body: { name } });

            if (error) {
                throw new Error(getApiErrorMessage(error, "Failed to create grade."));
            }

            return data!.grade;
        },
        onSuccess: () => invalidateGrades(queryClient),
    });
}

/** Renames a grade dictionary value. Requires grades.manage. */
export function useUpdateGradeMutation() {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: async ({ gradeId, name }: { gradeId: number; name: string }) => {
            const { data, error } = await apiClient.PUT("/api/v1/grades/{id}", {
                params: { path: { id: gradeId } },
                body: { name },
            });

            if (error) {
                throw new Error(getApiErrorMessage(error, "Failed to rename grade."));
            }

            return data!.grade;
        },
        onSuccess: () => invalidateGrades(queryClient),
    });
}

/** Deactivates a grade dictionary value. Requires grades.manage. */
export function useDeactivateGradeMutation() {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: async (gradeId: number) => {
            const { error } = await apiClient.DELETE("/api/v1/grades/{id}", {
                params: { path: { id: gradeId } },
            });

            if (error) {
                throw new Error(getApiErrorMessage(error, "Failed to deactivate grade."));
            }
        },
        onSuccess: () => invalidateGrades(queryClient),
    });
}

/** Reactivates a previously deactivated grade dictionary value. Requires grades.manage. */
export function useActivateGradeMutation() {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: async (gradeId: number) => {
            const { error } = await apiClient.POST("/api/v1/grades/{id}/activate", {
                params: { path: { id: gradeId } },
            });

            if (error) {
                throw new Error(getApiErrorMessage(error, "Failed to activate grade."));
            }
        },
        onSuccess: () => invalidateGrades(queryClient),
    });
}

/** Sets or clears a user's grade; invalidates group detail views so member rows refresh. */
export function useUpdateUserGradeMutation() {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: async ({ userId, gradeId }: { userId: number; gradeId: number | null }) => {
            const { data, error } = await apiClient.PATCH("/api/v1/users/{id}/grade", {
                params: { path: { id: userId } },
                body: { gradeId },
            });

            if (error) {
                throw new Error(getApiErrorMessage(error, "Failed to update grade."));
            }

            return data!.user;
        },
        onSuccess: () => {
            void queryClient.invalidateQueries({ queryKey: groupsKeys.all });
        },
    });
}
