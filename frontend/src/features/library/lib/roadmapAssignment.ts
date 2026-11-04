import { PRIMARY_ACTION_ASSIGN, PRIMARY_ACTION_REMOVE, PRIMARY_ACTION_SAVE } from "../constants/roadmapAssignment";
import type {
    AssigneeSources,
    AssignmentPlan,
    LearningAssigneeV1,
    PickedItem,
    PickedTeam,
    RemovalKey,
} from "../model/roadmapAssignment";

/** Builds the staged-removal key for a person. */
export function personRemovalKey(userId: number): RemovalKey {
    return `p:${userId}`;
}

/** Builds the staged-removal key for a team. */
export function teamRemovalKey(groupId: number): RemovalKey {
    return `t:${groupId}`;
}

/**
 * Resolves how an assignee holds the roadmap. "Direct" means an explicit assignment (or a
 * self-enrollment): anything that is neither the auto-assign rule nor a standing team
 * assignment.
 */
export function assigneeSources(assignee: LearningAssigneeV1): AssigneeSources {
    const viaGroupNames = assignee.viaGroupNames ?? [];
    const autoAssigned = Boolean(assignee.autoAssigned);
    return {
        direct: !autoAssigned && viaGroupNames.length === 0,
        autoAssigned,
        viaGroupNames,
    };
}

/**
 * Whether a person's own enrollment row may be removed from the dialog. A person who only has
 * the roadmap through a team keeps it as long as the team assignment stands, so removing the
 * row alone would be misleading; the team must be removed instead.
 */
export function canRemoveAssignee(assignee: LearningAssigneeV1): boolean {
    const sources = assigneeSources(assignee);
    return sources.direct || sources.autoAssigned;
}

/**
 * Whether every reason a person has the roadmap is staged for removal, so the picker may offer
 * them again as "not assigned".
 */
export function isRemovalCovering(
    assignee: LearningAssigneeV1,
    removing: ReadonlySet<RemovalKey>,
    assignedTeamIdsByName: ReadonlyMap<string, number>,
): boolean {
    const sources = assigneeSources(assignee);
    if ((sources.direct || sources.autoAssigned) && !removing.has(personRemovalKey(assignee.userId))) {
        return false;
    }
    return sources.viaGroupNames.every((groupName) => {
        const groupId = assignedTeamIdsByName.get(groupName);
        return groupId !== undefined && removing.has(teamRemovalKey(groupId));
    });
}

/** How many people a picked team brings: the grade-filtered match when known, else the whole team. */
export function teamPeopleCount(team: PickedTeam): number {
    if (team.gradeIds.length === 0) {
        return team.membersCount;
    }
    return team.matchedMembersCount ?? team.membersCount;
}

/**
 * Counts what Save will change: explicitly picked people who do not have the roadmap yet plus
 * the members of every picked team. Team members already holding the roadmap are only known
 * server-side, so the team size is used as is.
 */
export function buildAssignmentPlan(
    picked: readonly PickedItem[],
    removing: ReadonlySet<RemovalKey>,
    assignedUserIds: ReadonlySet<number>,
): AssignmentPlan {
    const newDirectPeopleCount = picked.filter(
        (item) =>
            item.type === "person" &&
            (!assignedUserIds.has(item.id) || removing.has(personRemovalKey(item.id))),
    ).length;
    const teams = picked.filter((item): item is PickedTeam => item.type === "team");
    const teamPeople = teams.reduce((sum, team) => sum + teamPeopleCount(team), 0);
    return {
        newPeopleCount: newDirectPeopleCount + teamPeople,
        pickedTeamCount: teams.length,
        removalCount: removing.size,
    };
}

/** Whether the dialog has anything to save. */
export function hasPlannedChanges(plan: AssignmentPlan): boolean {
    return plan.newPeopleCount > 0 || plan.pickedTeamCount > 0 || plan.removalCount > 0;
}

/**
 * Primary button label for the planned change. Counts stay in the footer hint so the button
 * keeps a fixed width however many people are involved.
 */
export function primaryActionLabel(plan: AssignmentPlan): string {
    const adds = plan.newPeopleCount > 0 || plan.pickedTeamCount > 0;
    if (adds && plan.removalCount > 0) {
        return PRIMARY_ACTION_SAVE;
    }
    if (plan.removalCount > 0) {
        return PRIMARY_ACTION_REMOVE;
    }
    return PRIMARY_ACTION_ASSIGN;
}

/** Footer hint sentence summarising the planned change. */
export function planHint(plan: AssignmentPlan): string {
    if (!hasPlannedChanges(plan)) {
        return "Pick people or teams to assign.";
    }
    const parts: string[] = [];
    if (plan.newPeopleCount > 0 || plan.pickedTeamCount > 0) {
        const people = `${plan.newPeopleCount} ${plan.newPeopleCount === 1 ? "person" : "people"}`;
        const teams =
            plan.pickedTeamCount > 0
                ? ` (incl. ${plan.pickedTeamCount} team${plan.pickedTeamCount === 1 ? "" : "s"})`
                : "";
        parts.push(`${people}${teams} will get this roadmap`);
    }
    if (plan.removalCount > 0) {
        parts.push(`${plan.removalCount} will be removed`);
    }
    return parts.join(" · ");
}

/** Case-insensitive match of a free-text filter against a name and email. */
export function matchesPersonQuery(name: string | undefined, email: string | undefined, query: string): boolean {
    const normalized = query.trim().toLowerCase();
    if (!normalized) {
        return true;
    }
    return (name ?? "").toLowerCase().includes(normalized) || (email ?? "").toLowerCase().includes(normalized);
}
