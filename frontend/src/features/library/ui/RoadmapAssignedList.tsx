import { useEffect, useState } from "react";

import BoltOutlinedIcon from "@mui/icons-material/BoltOutlined";
import ChevronRightOutlinedIcon from "@mui/icons-material/ChevronRightOutlined";
import CloseOutlinedIcon from "@mui/icons-material/CloseOutlined";
import SearchOutlinedIcon from "@mui/icons-material/SearchOutlined";

import { UserAvatar } from "@/shared/ui/UserAvatar";
import { ASSIGNED_FILTER_PLACEHOLDER, SUMMARY_AVATAR_STACK_SIZE } from "../constants/roadmapAssignment";
import {
    assigneeSources,
    canRemoveAssignee,
    matchesPersonQuery,
    personRemovalKey,
    teamRemovalKey,
} from "../lib/roadmapAssignment";
import type { RoadmapAssignedListProps, RoadmapGroupAssignmentV1 } from "../model/roadmapAssignment";

function gradeFilterLabel(team: RoadmapGroupAssignmentV1): string {
    return team.gradeFilters.length === 0
        ? "all grades"
        : team.gradeFilters.map((grade) => grade.name).join(", ");
}

/**
 * "Assigned to N people · M teams" summary that expands into the full assigned list, with a
 * sticky filter, teams first, then people, each person tagged with how they got the roadmap.
 * Removals are staged (struck through with Undo) and applied by the dialog's Save.
 */
export function RoadmapAssignedList({
    assignees,
    totalPeopleCount = null,
    teams,
    removing,
    isLoading,
    disabled = false,
    highlightedUserId = null,
    onStageRemoval,
    onUndoRemoval,
}: RoadmapAssignedListProps) {
    const [isExpanded, setIsExpanded] = useState(false);
    const [filter, setFilter] = useState("");

    // Clicking an already-assigned search result jumps here: open the list and show that person.
    useEffect(() => {
        if (highlightedUserId !== null) {
            setIsExpanded(true);
            setFilter("");
        }
    }, [highlightedUserId]);

    // The listed rows are bounded to people the viewer may manage, so the headline count comes
    // from the roadmap's full enrollment count whenever it is known.
    const peopleCount = Math.max(totalPeopleCount ?? 0, assignees.length);
    const hiddenPeopleCount = peopleCount - assignees.length;
    const teamCount = teams.length;
    const visibleTeams = teams.filter((team) => matchesPersonQuery(team.groupName, "", filter));
    const visiblePeople = assignees
        .filter((assignee) => matchesPersonQuery(assignee.name, assignee.email, filter))
        .sort((a, b) => (a.name || a.email).localeCompare(b.name || b.email));
    const expanded = isExpanded;

    if (isLoading) {
        return <p className="roadmap-assign-summary__empty">Loading assigned people…</p>;
    }

    if (peopleCount === 0 && teamCount === 0) {
        return <p className="roadmap-assign-summary__empty">Not assigned to anyone yet.</p>;
    }

    return (
        <div className="roadmap-assign-assigned">
            <button
                type="button"
                className="roadmap-assign-summary"
                aria-expanded={expanded}
                onClick={() => setIsExpanded((value) => !value)}
            >
                <span className="roadmap-assign-avatars" aria-hidden="true">
                    {assignees.slice(0, SUMMARY_AVATAR_STACK_SIZE).map((assignee) => (
                        <UserAvatar key={assignee.userId} user={{ name: assignee.name, email: assignee.email }} size={28} />
                    ))}
                    {peopleCount > SUMMARY_AVATAR_STACK_SIZE && (
                        <span className="roadmap-assign-avatar roadmap-assign-avatar--more">
                            +{peopleCount - SUMMARY_AVATAR_STACK_SIZE}
                        </span>
                    )}
                </span>
                <span className="roadmap-assign-summary__text">
                    Assigned to <strong>{peopleCount} {peopleCount === 1 ? "person" : "people"}</strong>
                    {teamCount > 0 ? ` · ${teamCount} team${teamCount === 1 ? "" : "s"}` : ""}
                </span>
                <span className="roadmap-assign-summary__manage">
                    Manage <ChevronRightOutlinedIcon />
                </span>
            </button>

            {expanded && (
                <div className="roadmap-assign-list">
                    <label className="roadmap-assign-list__filter">
                        <SearchOutlinedIcon aria-hidden="true" />
                        <input
                            type="search"
                            value={filter}
                            placeholder={ASSIGNED_FILTER_PLACEHOLDER}
                            aria-label="Find in assigned list"
                            onChange={(event) => setFilter(event.target.value)}
                        />
                    </label>
                    <div className="roadmap-assign-list__rows">
                        {visibleTeams.length > 0 && <p className="roadmap-assign-list__group">Teams</p>}
                        {visibleTeams.map((team) => {
                            const key = teamRemovalKey(team.groupId);
                            const isRemoving = removing.has(key);
                            return (
                                <div
                                    key={key}
                                    className={`roadmap-assign-row${isRemoving ? " roadmap-assign-row--removing" : ""}`}
                                >
                                    <span className="roadmap-assign-avatar roadmap-assign-avatar--team" aria-hidden="true">
                                        {(team.groupName[0] ?? "T").toUpperCase()}
                                    </span>
                                    <span className="roadmap-assign-row__who">
                                        <strong>{team.groupName}</strong>
                                        <span>
                                            {team.membersMatchedCount} member{team.membersMatchedCount === 1 ? "" : "s"} ·{" "}
                                            {gradeFilterLabel(team)} · new members get it automatically
                                        </span>
                                    </span>
                                    {isRemoving ? (
                                        <button type="button" className="roadmap-assign-row__undo" onClick={() => onUndoRemoval(key)}>
                                            Undo
                                        </button>
                                    ) : (
                                        <button
                                            type="button"
                                            className="roadmap-assign-row__remove"
                                            aria-label={`Remove ${team.groupName}`}
                                            title="Remove team"
                                            disabled={disabled}
                                            onClick={() => onStageRemoval(key)}
                                        >
                                            <CloseOutlinedIcon />
                                        </button>
                                    )}
                                </div>
                            );
                        })}
                        {visiblePeople.length > 0 && <p className="roadmap-assign-list__group">People</p>}
                        {visiblePeople.map((assignee) => {
                            const key = personRemovalKey(assignee.userId);
                            const isRemoving = removing.has(key);
                            const sources = assigneeSources(assignee);
                            const removable = canRemoveAssignee(assignee);
                            const name = assignee.name || assignee.email;
                            return (
                                <div
                                    key={key}
                                    data-user-id={assignee.userId}
                                    className={[
                                        "roadmap-assign-row",
                                        isRemoving ? "roadmap-assign-row--removing" : "",
                                        highlightedUserId === assignee.userId ? "roadmap-assign-row--flash" : "",
                                    ]
                                        .filter(Boolean)
                                        .join(" ")}
                                >
                                    <UserAvatar user={{ name: assignee.name, email: assignee.email }} size={32} />
                                    <span className="roadmap-assign-row__who">
                                        <strong>{name}</strong>
                                        <span>
                                            {assignee.email}
                                            {assignee.isCompleted != null
                                                ? ` · ${assignee.isCompleted ? "Completed" : "In progress"}`
                                                : ""}
                                        </span>
                                    </span>
                                    <span className="roadmap-assign-row__tags">
                                        {sources.direct && <span className="roadmap-assign-source">Direct</span>}
                                        {sources.autoAssigned && (
                                            <span className="roadmap-assign-source roadmap-assign-source--auto">
                                                <BoltOutlinedIcon /> New user
                                            </span>
                                        )}
                                        {sources.viaGroupNames.map((groupName) => (
                                            <span key={groupName} className="roadmap-assign-source">
                                                via {groupName}
                                            </span>
                                        ))}
                                    </span>
                                    {isRemoving ? (
                                        <button type="button" className="roadmap-assign-row__undo" onClick={() => onUndoRemoval(key)}>
                                            Undo
                                        </button>
                                    ) : (
                                        <button
                                            type="button"
                                            className="roadmap-assign-row__remove"
                                            aria-label={`Remove ${name}`}
                                            title={
                                                removable
                                                    ? "Remove"
                                                    : `Has it via ${sources.viaGroupNames.join(", ")}. Remove the team to unassign.`
                                            }
                                            disabled={disabled || !removable}
                                            onClick={() => onStageRemoval(key)}
                                        >
                                            <CloseOutlinedIcon />
                                        </button>
                                    )}
                                </div>
                            );
                        })}
                        {visibleTeams.length === 0 && visiblePeople.length === 0 && (
                            <p className="roadmap-assign-list__empty">
                                No one named “{filter.trim()}” has this roadmap.
                            </p>
                        )}
                        {hiddenPeopleCount > 0 && (
                            <p className="roadmap-assign-list__hidden">
                                {hiddenPeopleCount} more {hiddenPeopleCount === 1 ? "person has" : "people have"} this
                                roadmap but {hiddenPeopleCount === 1 ? "is" : "are"} not listed here: only people you
                                can manage are shown.
                            </p>
                        )}
                    </div>
                </div>
            )}
        </div>
    );
}
