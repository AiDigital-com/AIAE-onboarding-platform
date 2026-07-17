import { useQuery } from "@tanstack/react-query";
import { apiClient } from "@/shared/api/client";
import { getApiErrorMessage } from "@/shared/api/apiError";
import { groupsKeys } from "./queryKeys";
import { emptyPageInfo } from "./types";
import type { GroupSummaryV1, GroupV1, PagedResult } from "./types";

export const GROUPS_PAGE_SIZE = 10;
export const GROUP_MEMBERS_PAGE_SIZE = 8;

/** Aggregate team/member/lead counts for the stats row, scoped like the group list itself. */
export function useGroupOrgStatsQuery() {
    return useQuery({
        queryKey: groupsKeys.orgStats,
        queryFn: async () => {
            const { data, error } = await apiClient.GET("/api/v1/groups/stats", { params: {} });

            if (error) {
                throw new Error(getApiErrorMessage(error, "Failed to load team stats."));
            }

            return data!.stats;
        },
    });
}

/** One page of the searchable group list — replaces its page on each navigation, never accumulates. */
export function useGroupsQuery(search: string | undefined, page: number, options?: { enabled?: boolean }) {
    return useQuery({
        queryKey: [...groupsKeys.all, { search, page }] as const,
        enabled: options?.enabled ?? true,
        placeholderData: (previousData) => previousData,
        queryFn: async (): Promise<PagedResult<GroupSummaryV1>> => {
            const { data, error } = await apiClient.GET("/api/v1/groups", {
                params: {
                    query: {
                        search: search || undefined,
                        page,
                        size: GROUPS_PAGE_SIZE,
                    },
                },
            });

            if (error) {
                throw new Error(getApiErrorMessage(error, "Failed to load groups."));
            }

            return {
                items: data?.groups ?? [],
                page: data?.page ?? emptyPageInfo(page, GROUPS_PAGE_SIZE),
            };
        },
    });
}

/** Loads group detail — name, description, and leads. Members are paged separately. */
export function useGroupQuery(groupId: number | null, options?: { enabled?: boolean }) {
    return useQuery({
        queryKey: groupsKeys.detail(groupId ?? -1),
        enabled: (options?.enabled ?? true) && groupId !== null,
        queryFn: async (): Promise<GroupV1> => {
            const { data, error } = await apiClient.GET("/api/v1/groups/{id}", {
                params: { path: { id: groupId as number } },
            });

            if (error) {
                throw new Error(getApiErrorMessage(error, "Failed to load group."));
            }

            return data!.group;
        },
    });
}

/** One page of a group's members, optionally filtered by name/email search. */
export function useGroupMembersQuery(
    groupId: number | null,
    search: string | undefined,
    page: number,
    options?: { enabled?: boolean },
) {
    return useQuery({
        queryKey: groupsKeys.members(groupId ?? -1, search, page),
        enabled: (options?.enabled ?? true) && groupId !== null,
        placeholderData: (previousData) => previousData,
        queryFn: async () => {
            const { data, error } = await apiClient.GET("/api/v1/groups/{id}/members", {
                params: {
                    path: { id: groupId as number },
                    query: {
                        search: search || undefined,
                        page,
                        size: GROUP_MEMBERS_PAGE_SIZE,
                    },
                },
            });

            if (error) {
                throw new Error(getApiErrorMessage(error, "Failed to load team members."));
            }

            return {
                items: data?.members ?? [],
                page: data?.page ?? emptyPageInfo(page, GROUP_MEMBERS_PAGE_SIZE),
            };
        },
    });
}

/** Standing roadmap assignments for one group. Caller must be able to manage the group. */
export function useGroupRoadmapAssignmentsQuery(groupId: number | null, options?: { enabled?: boolean }) {
    return useQuery({
        queryKey: groupsKeys.groupRoadmapAssignments(groupId ?? -1),
        enabled: (options?.enabled ?? true) && groupId !== null,
        queryFn: async () => {
            const { data, error } = await apiClient.GET("/api/v1/groups/{id}/roadmap-assignments", {
                params: { path: { id: groupId as number } },
            });

            if (error) {
                throw new Error(getApiErrorMessage(error, "Failed to load roadmap assignments."));
            }

            return data?.assignments ?? [];
        },
    });
}

/**
 * Searches users eligible to be added to a group — excludes whoever already holds that role in
 * the group. Admin only. Used by the add-member/add-lead pickers instead of a static full list.
 */
export function useGroupCandidateUsersQuery(
    groupId: number | null,
    forLeads: boolean,
    search: string | undefined,
    options?: { enabled?: boolean },
) {
    return useQuery({
        queryKey: groupsKeys.candidateUsers(groupId ?? -1, forLeads, search),
        enabled: (options?.enabled ?? true) && groupId !== null,
        queryFn: async () => {
            const { data, error } = await apiClient.GET("/api/v1/groups/{id}/candidate-users", {
                params: {
                    path: { id: groupId as number },
                    query: {
                        forLeads,
                        search: search || undefined,
                        page: 0,
                        size: 20,
                    },
                },
            });

            if (error) {
                throw new Error(getApiErrorMessage(error, "Failed to load candidate users."));
            }

            return data?.users ?? [];
        },
    });
}
