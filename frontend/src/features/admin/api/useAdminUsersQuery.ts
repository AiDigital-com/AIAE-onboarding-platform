import { useQuery } from "@tanstack/react-query";
import { apiClient } from "@/shared/api/client";
import { getApiErrorMessage } from "@/shared/api/apiError";
import { emptyPageInfo } from "./types";
import type { PagedResult, UserRoleCodeV1, UserSummaryV1 } from "./types";

export const ADMIN_USERS_PAGE_SIZE = 10;

/** One page of the searchable, role-filterable admin user list. */
export function useAdminUsersQuery(
    search: string | undefined,
    role: UserRoleCodeV1 | undefined,
    page: number,
    enabled = true,
) {
    return useQuery({
        queryKey: ["admin", "users", { search, role, page }] as const,
        enabled,
        placeholderData: (previousData) => previousData,
        queryFn: async (): Promise<PagedResult<UserSummaryV1>> => {
            const { data, error } = await apiClient.GET("/api/v1/admin/users", {
                params: {
                    query: {
                        search: search || undefined,
                        role,
                        page,
                        size: ADMIN_USERS_PAGE_SIZE,
                    },
                },
            });

            if (error) {
                throw new Error(getApiErrorMessage(error, "Failed to load users."));
            }

            return {
                items: data?.users ?? [],
                page: data?.page ?? emptyPageInfo(page, ADMIN_USERS_PAGE_SIZE),
            };
        },
    });
}

/** Workspace-wide user/admin/team-lead counts for the Admin page stats row. */
export function useAdminUserStatsQuery(enabled = true) {
    return useQuery({
        queryKey: ["admin", "users", "stats"] as const,
        enabled,
        queryFn: async () => {
            const { data, error } = await apiClient.GET("/api/v1/admin/users/stats");

            if (error) {
                throw new Error(getApiErrorMessage(error, "Failed to load user stats."));
            }

            return data!.stats;
        },
    });
}
