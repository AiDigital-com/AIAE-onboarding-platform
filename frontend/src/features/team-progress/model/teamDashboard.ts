import type { components } from "@/shared/api/generated/schema";

/** Full team dashboard payload returned by GET /api/v1/team-progress/dashboard. */
export type TeamDashboardV1 = components["schemas"]["TeamDashboardV1"];

/** One member row of the team dashboard. */
export type TeamDashboardMemberV1 = components["schemas"]["TeamDashboardMemberV1"];
