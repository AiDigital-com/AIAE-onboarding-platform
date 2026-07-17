import type { components } from "@/shared/api/generated/schema";

export type UserSummaryV1 = components["schemas"]["UserSummaryV1"];
export type GroupSummaryV1 = components["schemas"]["GroupSummaryV1"];
export type GroupV1 = components["schemas"]["GroupV1"];
export type GroupMemberV1 = components["schemas"]["GroupMemberV1"];
export type GradeV1 = components["schemas"]["GradeV1"];
export type PageInfoV1 = components["schemas"]["PageInfoV1"];
export type RoadmapGroupAssignmentV1 = components["schemas"]["RoadmapGroupAssignmentV1"];
export type GroupOrgStatsV1 = components["schemas"]["GroupOrgStatsV1"];

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
