import { useMutation, useQueryClient } from "@tanstack/react-query";
import { apiClient } from "@/shared/api/client";
import { getApiErrorMessage } from "@/shared/api/apiError";
import { groupsKeys } from "./queryKeys";

/** Creates a group; invalidates the group list so the new row appears. */
export function useCreateGroupMutation() {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: async ({ name, description }: { name: string; description?: string }) => {
            const { data, error } = await apiClient.POST("/api/v1/groups", {
                body: { name, description },
            });

            if (error) {
                throw new Error(getApiErrorMessage(error, "Failed to create team."));
            }

            return data!.group;
        },
        onSuccess: () => {
            void queryClient.invalidateQueries({ queryKey: groupsKeys.all });
        },
    });
}

/** Renames/updates a group; updates the detail cache directly from the authoritative response. */
export function useUpdateGroupMutation() {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: async ({
            groupId,
            name,
            description,
        }: {
            groupId: number;
            name: string;
            description?: string;
        }) => {
            const { data, error } = await apiClient.PUT("/api/v1/groups/{id}", {
                params: { path: { id: groupId } },
                body: { name, description },
            });

            if (error) {
                throw new Error(getApiErrorMessage(error, "Failed to update team."));
            }

            return data!.group;
        },
        onSuccess: (group) => {
            queryClient.setQueryData(groupsKeys.detail(group.id), group);
            void queryClient.invalidateQueries({ queryKey: groupsKeys.all });
        },
    });
}

/** Deletes a group. */
export function useDeleteGroupMutation() {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: async (groupId: number) => {
            const { error } = await apiClient.DELETE("/api/v1/groups/{id}", {
                params: { path: { id: groupId } },
            });

            if (error) {
                throw new Error(getApiErrorMessage(error, "Failed to delete team."));
            }
        },
        onSuccess: () => {
            void queryClient.invalidateQueries({ queryKey: groupsKeys.all });
        },
    });
}

/** Adds a member to a group, resolved by internal user id. */
export function useAddGroupMemberMutation() {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: async ({ groupId, memberUserId }: { groupId: number; memberUserId: number }) => {
            const { data, error } = await apiClient.POST("/api/v1/groups/{id}/members", {
                params: { path: { id: groupId } },
                body: { memberUserId },
            });

            if (error) {
                throw new Error(getApiErrorMessage(error, "Failed to add team member."));
            }

            return data!.member;
        },
        onSuccess: (_member, { groupId }) => {
            void queryClient.invalidateQueries({ queryKey: groupsKeys.detail(groupId) });
            void queryClient.invalidateQueries({ queryKey: groupsKeys.all });
        },
    });
}

/** Removes a member from a group. */
export function useRemoveGroupMemberMutation() {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: async ({ groupId, memberUserId }: { groupId: number; memberUserId: number }) => {
            const { error } = await apiClient.DELETE("/api/v1/groups/{id}/members/{userId}", {
                params: { path: { id: groupId, userId: memberUserId } },
            });

            if (error) {
                throw new Error(getApiErrorMessage(error, "Failed to remove team member."));
            }
        },
        onSuccess: (_data, { groupId }) => {
            void queryClient.invalidateQueries({ queryKey: groupsKeys.detail(groupId) });
            void queryClient.invalidateQueries({ queryKey: groupsKeys.all });
        },
    });
}

/** Assigns a user as an additional lead of a group; the user must already hold the Team Lead or Admin role. */
export function useAddGroupLeadMutation() {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: async ({ groupId, leadUserId }: { groupId: number; leadUserId: number }) => {
            const { data, error } = await apiClient.POST("/api/v1/groups/{id}/leads", {
                params: { path: { id: groupId } },
                body: { leadUserId },
            });

            if (error) {
                throw new Error(getApiErrorMessage(error, "Failed to add team lead."));
            }

            return data!.lead;
        },
        onSuccess: (_lead, { groupId }) => {
            void queryClient.invalidateQueries({ queryKey: groupsKeys.detail(groupId) });
            void queryClient.invalidateQueries({ queryKey: groupsKeys.all });
        },
    });
}

/** Removes a lead from a group. */
export function useRemoveGroupLeadMutation() {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: async ({ groupId, leadUserId }: { groupId: number; leadUserId: number }) => {
            const { error } = await apiClient.DELETE("/api/v1/groups/{id}/leads/{userId}", {
                params: { path: { id: groupId, userId: leadUserId } },
            });

            if (error) {
                throw new Error(getApiErrorMessage(error, "Failed to remove team lead."));
            }
        },
        onSuccess: (_data, { groupId }) => {
            void queryClient.invalidateQueries({ queryKey: groupsKeys.detail(groupId) });
            void queryClient.invalidateQueries({ queryKey: groupsKeys.all });
        },
    });
}
