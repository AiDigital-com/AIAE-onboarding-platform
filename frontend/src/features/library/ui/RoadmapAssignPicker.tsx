import { useEffect, useId, useRef, useState } from "react";
import type { KeyboardEvent } from "react";

import CloseOutlinedIcon from "@mui/icons-material/CloseOutlined";
import SearchOutlinedIcon from "@mui/icons-material/SearchOutlined";

import { leadSummary } from "@/features/groups/lib/leadSummary";
import { UserAvatar } from "@/shared/ui/UserAvatar";
import { useRoadmapGroupAssignmentPreviewQuery } from "../api/useRoadmapGroupAssignmentPreviewQuery";
import { PICKER_PLACEHOLDER, PICKER_PLACEHOLDER_MORE } from "../constants/roadmapAssignment";
import type {
    GradeV1,
    PickedTeam,
    PickerResult,
    RoadmapAssignPickerProps,
} from "../model/roadmapAssignment";

function resultKey(result: PickerResult): string {
    return result.type === "person" ? `p:${result.user.id}` : `t:${result.group.id}`;
}

function teamGradeLabel(team: PickedTeam, grades: GradeV1[]): string {
    if (team.gradeIds.length === 0) {
        return "All grades";
    }
    return grades
        .filter((grade) => team.gradeIds.includes(grade.id))
        .map((grade) => grade.name)
        .join(", ");
}

/**
 * Headless companion of a picked team chip: keeps the team's member preview loaded and reports
 * the matched user ids upward so the plan can de-duplicate them against picked people.
 */
function PickedTeamPreview({
    roadmapId,
    team,
    onPreview,
}: {
    roadmapId: number;
    team: PickedTeam;
    onPreview: (matchedUserIds: number[] | undefined) => void;
}) {
    const previewQuery = useRoadmapGroupAssignmentPreviewQuery(roadmapId, team.id, team.gradeIds, true);
    const matchedUserIds = previewQuery.data?.matchedUserIds;

    useEffect(() => {
        // Re-run only when the preview result changes; the parent callback is recreated per render.
        onPreview(matchedUserIds);
    }, [matchedUserIds]);

    return null;
}

/** Grade filter for one picked team, with the live "N of M members match" preview. */
function TeamGradeFilter({
    roadmapId,
    team,
    grades,
    onChange,
    onDone,
}: {
    roadmapId: number;
    team: PickedTeam;
    grades: GradeV1[];
    onChange: (gradeIds: number[]) => void;
    onDone: () => void;
}) {
    const previewQuery = useRoadmapGroupAssignmentPreviewQuery(roadmapId, team.id, team.gradeIds, true);
    const preview = previewQuery.data;

    return (
        <div className="roadmap-assign-grades" role="group" aria-label={`Grades for ${team.name}`}>
            <p className="roadmap-assign-grades__title">Grades for {team.name}</p>
            <div className="roadmap-assign-grades__options">
                {grades.map((grade) => (
                    <label key={grade.id} className="roadmap-assign-grades__option">
                        <input
                            type="checkbox"
                            checked={team.gradeIds.includes(grade.id)}
                            onChange={() =>
                                onChange(
                                    team.gradeIds.includes(grade.id)
                                        ? team.gradeIds.filter((id) => id !== grade.id)
                                        : [...team.gradeIds, grade.id],
                                )
                            }
                        />
                        {grade.name}
                    </label>
                ))}
            </div>
            <p className="roadmap-assign-grades__hint">
                {team.gradeIds.length === 0
                    ? "No grade selected — the whole team will be enrolled."
                    : preview
                      ? `${preview.membersMatchedCount} of ${preview.groupMembersCount} members match.${
                            preview.membersWithoutGradeCount > 0
                                ? ` ${preview.membersWithoutGradeCount} without a grade will be skipped.`
                                : ""
                        }`
                      : "Checking match…"}
            </p>
            <button type="button" className="roadmap-assign-grades__done" onClick={onDone}>
                Done
            </button>
        </div>
    );
}

/**
 * Combined people-or-teams picker: a chip field with a searchable listbox underneath. Picking a
 * team adds a grade-filter chip so the existing grade-narrowed team assignment stays available.
 */
export function RoadmapAssignPicker({
    roadmapId,
    query,
    picked,
    results,
    grades,
    isSearching,
    disabled = false,
    onQueryChange,
    onPick,
    onUnpick,
    onTeamGradesChange,
    onTeamPreview,
}: RoadmapAssignPickerProps) {
    const listboxId = useId();
    const rootRef = useRef<HTMLDivElement>(null);
    const inputRef = useRef<HTMLInputElement>(null);
    const [isOpen, setIsOpen] = useState(false);
    const [highlightedIndex, setHighlightedIndex] = useState(-1);
    const [editingTeamId, setEditingTeamId] = useState<number | null>(null);

    const people = results.filter((result) => result.type === "person");
    const teams = results.filter((result) => result.type === "team");
    const ordered = [...people, ...teams];
    const editingTeam = picked.find(
        (item): item is PickedTeam => item.type === "team" && item.id === editingTeamId,
    );

    useEffect(() => {
        if (!isOpen) {
            return undefined;
        }
        function handlePointerDown(event: PointerEvent) {
            if (rootRef.current && !rootRef.current.contains(event.target as Node)) {
                setIsOpen(false);
            }
        }
        document.addEventListener("pointerdown", handlePointerDown);
        return () => document.removeEventListener("pointerdown", handlePointerDown);
    }, [isOpen]);

    function choose(result: PickerResult) {
        if (result.isSelected) {
            return;
        }
        onPick(result);
        if (!result.isAssigned) {
            onQueryChange("");
            setHighlightedIndex(-1);
            inputRef.current?.focus();
        } else {
            setIsOpen(false);
        }
    }

    function handleKeyDown(event: KeyboardEvent<HTMLInputElement>) {
        switch (event.key) {
            case "ArrowDown":
                event.preventDefault();
                setIsOpen(true);
                setHighlightedIndex((index) => Math.min(index + 1, ordered.length - 1));
                break;
            case "ArrowUp":
                event.preventDefault();
                setHighlightedIndex((index) => Math.max(index - 1, 0));
                break;
            case "Enter": {
                const result = ordered[highlightedIndex];
                if (isOpen && result) {
                    event.preventDefault();
                    choose(result);
                }
                break;
            }
            case "Escape":
                if (isOpen) {
                    event.stopPropagation();
                    setIsOpen(false);
                }
                break;
            case "Backspace":
                if (!query && picked.length > 0) {
                    onUnpick(picked.length - 1);
                }
                break;
            default:
                break;
        }
    }

    function renderResult(result: PickerResult, index: number) {
        const isHighlighted = index === highlightedIndex;
        const status = result.isAssigned ? "Assigned" : result.isSelected ? "Selected" : null;
        return (
            <li
                key={resultKey(result)}
                id={`${listboxId}-${index}`}
                role="option"
                aria-selected={result.isSelected}
                aria-disabled={result.isSelected}
                className={[
                    "roadmap-assign-picker__option",
                    isHighlighted ? "roadmap-assign-picker__option--active" : "",
                    result.isSelected ? "roadmap-assign-picker__option--disabled" : "",
                ]
                    .filter(Boolean)
                    .join(" ")}
                onMouseDown={(event) => event.preventDefault()}
                onMouseEnter={() => setHighlightedIndex(index)}
                onClick={() => choose(result)}
            >
                {result.type === "person" ? (
                    <UserAvatar user={result.user} size={32} />
                ) : (
                    <span className="roadmap-assign-avatar roadmap-assign-avatar--team" aria-hidden="true">
                        {(result.group.name[0] ?? "T").toUpperCase()}
                    </span>
                )}
                <span className="roadmap-assign-picker__who">
                    <strong>{result.type === "person" ? result.user.name || result.user.email : result.group.name}</strong>
                    <span>
                        {result.type === "person"
                            ? result.user.email
                            : `${leadSummary(result.group.leads)} · ${result.group.membersCount} member${
                                  result.group.membersCount === 1 ? "" : "s"
                              }`}
                    </span>
                </span>
                {status && (
                    <span
                        className={`roadmap-assign-status roadmap-assign-status--${status.toLowerCase()}`}
                    >
                        {status === "Assigned" ? "✓ Assigned" : status}
                    </span>
                )}
            </li>
        );
    }

    return (
        <div ref={rootRef} className="roadmap-assign-picker">
            <div
                className="roadmap-assign-picker__field"
                onClick={() => inputRef.current?.focus()}
            >
                <SearchOutlinedIcon aria-hidden="true" />
                {picked.map((item, index) => (
                    <span key={`${item.type}:${item.id}`} className="roadmap-assign-chip">
                        {item.type === "person" ? (
                            <UserAvatar user={{ name: item.name, email: item.email }} size={22} />
                        ) : (
                            <span className="roadmap-assign-avatar roadmap-assign-avatar--team roadmap-assign-avatar--chip" aria-hidden="true">
                                {(item.name[0] ?? "T").toUpperCase()}
                            </span>
                        )}
                        <span className="roadmap-assign-chip__label">
                            {item.type === "person" ? item.name || item.email : `${item.name} · ${item.membersCount}`}
                        </span>
                        {item.type === "team" && grades.length > 0 && (
                            <button
                                type="button"
                                className="roadmap-assign-chip__grades"
                                aria-expanded={editingTeamId === item.id}
                                onClick={(event) => {
                                    event.stopPropagation();
                                    setEditingTeamId(editingTeamId === item.id ? null : item.id);
                                }}
                            >
                                {teamGradeLabel(item, grades)} ▾
                            </button>
                        )}
                        <button
                            type="button"
                            className="roadmap-assign-chip__remove"
                            aria-label={`Remove ${item.name}`}
                            disabled={disabled}
                            onClick={(event) => {
                                event.stopPropagation();
                                onUnpick(index);
                                inputRef.current?.focus();
                            }}
                        >
                            <CloseOutlinedIcon />
                        </button>
                    </span>
                ))}
                <input
                    ref={inputRef}
                    type="text"
                    role="combobox"
                    aria-label="Search people or teams"
                    aria-autocomplete="list"
                    aria-expanded={isOpen}
                    aria-controls={listboxId}
                    aria-activedescendant={isOpen && highlightedIndex >= 0 ? `${listboxId}-${highlightedIndex}` : undefined}
                    autoComplete="off"
                    placeholder={picked.length > 0 ? PICKER_PLACEHOLDER_MORE : PICKER_PLACEHOLDER}
                    value={query}
                    disabled={disabled}
                    onFocus={() => setIsOpen(true)}
                    onChange={(event) => {
                        onQueryChange(event.target.value);
                        setHighlightedIndex(-1);
                        setIsOpen(true);
                    }}
                    onKeyDown={handleKeyDown}
                />
            </div>
            {isOpen && (
                <ul id={listboxId} role="listbox" aria-label="People and teams" className="roadmap-assign-picker__list">
                    {isSearching && ordered.length === 0 && (
                        <li className="roadmap-assign-picker__empty" role="presentation">
                            Searching…
                        </li>
                    )}
                    {!isSearching && ordered.length === 0 && (
                        <li className="roadmap-assign-picker__empty" role="presentation">
                            {query.trim() ? `No people or teams match “${query.trim()}”.` : "No people or teams to show."}
                        </li>
                    )}
                    {people.length > 0 && (
                        <li className="roadmap-assign-picker__group" role="presentation">
                            People
                        </li>
                    )}
                    {people.map((result, index) => renderResult(result, index))}
                    {teams.length > 0 && (
                        <li className="roadmap-assign-picker__group" role="presentation">
                            Teams
                        </li>
                    )}
                    {teams.map((result, index) => renderResult(result, people.length + index))}
                </ul>
            )}
            {editingTeam && (
                <TeamGradeFilter
                    roadmapId={roadmapId}
                    team={editingTeam}
                    grades={grades}
                    onChange={(gradeIds) => onTeamGradesChange(editingTeam.id, gradeIds)}
                    onDone={() => setEditingTeamId(null)}
                />
            )}
            {picked
                .filter((item): item is PickedTeam => item.type === "team")
                .map((team) => (
                    <PickedTeamPreview
                        key={team.id}
                        roadmapId={roadmapId}
                        team={team}
                        onPreview={(matchedUserIds) => onTeamPreview(team.id, matchedUserIds)}
                    />
                ))}
        </div>
    );
}
