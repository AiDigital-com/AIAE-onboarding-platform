import type { components } from "@/shared/api/generated/schema";

export type UserSummaryV1 = components["schemas"]["UserSummaryV1"];
export type UserRoleCodeV1 = components["schemas"]["UserRoleCodeV1"];
export type PageInfoV1 = components["schemas"]["PageInfoV1"];
export type AdminUserStatsV1 = components["schemas"]["AdminUserStatsV1"];

/** A single fetched page of items plus the server's pagination metadata. */
export interface PagedResult<T> {
    items: T[];
    page: PageInfoV1;
}

export function emptyPageInfo(requestedPage: number, requestedSize: number): PageInfoV1 {
    return {
        page: requestedPage,
        size: requestedSize,
        totalElements: 0,
        totalPages: 0,
        hasNext: false,
        hasPrevious: false,
    };
}
