import type { TeamDashboardMemberV1 } from "../model/teamDashboard";

/**
 * Filters members by a free-text query. Every whitespace-separated term must match the member's
 * name, email, or role (case-insensitive substring), so "ann design" finds Ann the Designer but
 * not Ann the Engineer. A blank query returns every member unchanged.
 */
export function filterMembersByQuery<T extends Pick<TeamDashboardMemberV1, "name" | "email" | "role">>(
    members: T[],
    query: string,
): T[] {
    const terms = query.trim().toLowerCase().split(/\s+/).filter(Boolean);
    if (terms.length === 0) {
        return members;
    }
    return members.filter((member) => {
        const haystack = [member.name, member.email, member.role]
            .filter((value): value is string => Boolean(value))
            .join(" ")
            .toLowerCase();
        return terms.every((term) => haystack.includes(term));
    });
}
