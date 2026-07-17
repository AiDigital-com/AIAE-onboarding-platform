import type { UserSummaryV1 } from "../api/types";

/** Formats a group's lead list as short display text, e.g. `lead: Maria Pronkina` or `leads: Maria Pronkina +2 more`. */
export function leadSummary(leads: UserSummaryV1[]): string {
    if (leads.length === 0) {
        return "No leads assigned";
    }
    if (leads.length === 1) {
        return `lead: ${leads[0].name || leads[0].email}`;
    }
    if (leads.length === 2) {
        return `leads: ${leads[0].name || leads[0].email}, ${leads[1].name || leads[1].email}`;
    }
    return `leads: ${leads[0].name || leads[0].email} +${leads.length - 1} more`;
}
