import type { ReactNode } from "react";

import type { TeamDashboardMemberV1 } from "./teamDashboard";

/** The member fields the scope picker needs to render and search an option. */
export type ScopePickerMember = Pick<TeamDashboardMemberV1, "id" | "name" | "email" | "role">;

/** One row of the scope picker's listbox. */
export interface ScopeOption {
    id: string;
    label: string;
    hint?: string;
}

export interface TeamScopePickerProps {
    members: ScopePickerMember[];
    /** Member id, or the team-wide scope constant. */
    scope: string;
    icon: ReactNode;
    onScopeChange: (scope: string) => void;
}
