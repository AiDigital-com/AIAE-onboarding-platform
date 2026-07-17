import { useMutation, useQueryClient } from "@tanstack/react-query";
import { apiClient } from "@/shared/api/client";
import type { components } from "@/shared/api/generated/schema";

type UserPermissionSnapshotV1 = components["schemas"]["UserPermissionSnapshotV1"];

interface Variables {
    userId: number;
    overrides: Record<string, boolean>;
}

/** Applies permission overrides for a managed user. */
export function useSetPermissionOverridesMutation() {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: async ({ userId, overrides }: Variables) => {
            const { data, error } = await apiClient.PUT("/api/v1/permissions", {
                body: { userId, overrides },
            });
            if (error) throw error;
            return data.permissions as UserPermissionSnapshotV1;
        },
        onSuccess: () => {
            void queryClient.invalidateQueries({ queryKey: ["session", "permissions"] });
            void queryClient.invalidateQueries({ queryKey: ["permissions", "management"] });
        },
    });
}

export type { UserPermissionSnapshotV1 };
