import { useEffect, useState } from "react";

import BoltOutlinedIcon from "@mui/icons-material/BoltOutlined";

import { useGradesQuery } from "@/features/groups/api/useGradesQuery";
import { useGroupsQuery } from "@/features/groups/api/useGroupsQuery";
import { useDebounce } from "@/shared/hooks/useDebounce";
import { Button } from "@/shared/ui/Button";
import { Dialog } from "@/shared/ui/Dialog";
import { useAssignableUsersQuery } from "../api/useAssignableUsersQuery";
import {
    useLearningAssigneesQuery,
    useLearningAssignmentMutation,
    useRevokeLearningAssignmentMutation,
} from "../api/useLearningAssignmentMutation";
import { useRoadmapAutoAssignMutation } from "../api/useRoadmapAutoAssignMutation";
import {
    useAssignRoadmapToGroupMutation,
    useUnassignRoadmapFromGroupMutation,
} from "../api/useRoadmapGroupAssignmentMutations";
import { useRoadmapGroupAssignmentsQuery } from "../api/useRoadmapGroupAssignmentsQuery";
import {
    ASSIGN_PEOPLE_PAGE_SIZE,
    ASSIGN_SEARCH_DEBOUNCE_MS,
    AUTO_ASSIGN_ADMIN_ONLY_HINT,
    AUTO_ASSIGN_DESCRIPTION,
    AUTO_ASSIGN_LABEL,
    AUTO_ASSIGN_OFF_TOAST,
    AUTO_ASSIGN_OFF_WARNING,
    AUTO_ASSIGN_ON_TOAST,
    DISCARD_CHANGES_PROMPT,
} from "../constants/roadmapAssignment";
import {
    buildAssignmentPlan,
    hasPlannedChanges,
    isRemovalCovering,
    planHint,
    primaryActionLabel,
    sameUserIds,
    teamRemovalKey,
} from "../lib/roadmapAssignment";
import type {
    PickedItem,
    PickerResult,
    RemovalKey,
    RoadmapAssignDialogProps,
} from "../model/roadmapAssignment";
import { RoadmapAssignPicker } from "./RoadmapAssignPicker";
import { RoadmapAssignedList } from "./RoadmapAssignedList";
import "./roadmap-assign-dialog.css";

/**
 * Single "Assign roadmap" dialog: one search for people and teams, the admin-only "New users"
 * auto-assign toggle (saved immediately), and the assigned list with staged removals. Picks
 * and removals are applied together on Save.
 */
export function RoadmapAssignDialog({
    open,
    roadmap,
    canToggleAutoAssign,
    onClose,
    onNotify,
}: RoadmapAssignDialogProps) {
    const roadmapId = roadmap?.id ?? null;
    const [query, setQuery] = useState("");
    const [picked, setPicked] = useState<PickedItem[]>([]);
    const [removing, setRemoving] = useState<Set<RemovalKey>>(new Set());
    const [isConfirmingOff, setIsConfirmingOff] = useState(false);
    const [highlightedUserId, setHighlightedUserId] = useState<number | null>(null);
    const [isSaving, setIsSaving] = useState(false);
    // Owned locally so the switch flips as soon as the server confirms, even if the parent still
    // holds the roadmap snapshot it opened the dialog with.
    const [isAutoAssignOn, setIsAutoAssignOn] = useState(Boolean(roadmap?.autoAssignNewUsers));
    const debouncedQuery = useDebounce(query, ASSIGN_SEARCH_DEBOUNCE_MS);
    const trimmedQuery = debouncedQuery.trim();

    useEffect(() => {
        if (open) {
            setQuery("");
            setPicked([]);
            setRemoving(new Set());
            setIsConfirmingOff(false);
            setHighlightedUserId(null);
        }
    }, [open, roadmapId]);

    useEffect(() => {
        setIsAutoAssignOn(Boolean(roadmap?.autoAssignNewUsers));
    }, [roadmap?.autoAssignNewUsers, roadmapId]);

    const assigneesQuery = useLearningAssigneesQuery("roadmap", roadmapId, open);
    const teamAssignmentsQuery = useRoadmapGroupAssignmentsQuery(roadmapId, open);
    const usersQuery = useAssignableUsersQuery(open, {
        page: 0,
        size: ASSIGN_PEOPLE_PAGE_SIZE,
        query: trimmedQuery || undefined,
    });
    const groupsQuery = useGroupsQuery(trimmedQuery || undefined, 0, { enabled: open });
    const gradesQuery = useGradesQuery();

    const assignPeople = useLearningAssignmentMutation("roadmap");
    const revokePeople = useRevokeLearningAssignmentMutation("roadmap");
    const assignTeam = useAssignRoadmapToGroupMutation();
    const unassignTeam = useUnassignRoadmapFromGroupMutation();
    const setAutoAssign = useRoadmapAutoAssignMutation();

    const assignees = assigneesQuery.data ?? [];
    const teams = teamAssignmentsQuery.data ?? [];
    const assignedUserIds = new Set(assignees.map((assignee) => assignee.userId));
    const assignedTeamIdsByName = new Map(teams.map((team) => [team.groupName, team.groupId]));
    const assignedTeamIds = new Set(teams.map((team) => team.groupId));
    const isPicked = (type: PickedItem["type"], id: number) =>
        picked.some((item) => item.type === type && item.id === id);

    const results: PickerResult[] = [
        ...(usersQuery.data ?? []).map((user): PickerResult => {
            const assignee = assignees.find((item) => item.userId === user.id);
            const stillAssigned = Boolean(
                assignee && !isRemovalCovering(assignee, removing, assignedTeamIdsByName),
            );
            return { type: "person", user, isAssigned: stillAssigned, isSelected: isPicked("person", user.id) };
        }),
        ...(groupsQuery.data?.items ?? []).map((group): PickerResult => ({
            type: "team",
            group,
            isAssigned: assignedTeamIds.has(group.id) && !removing.has(teamRemovalKey(group.id)),
            isSelected: isPicked("team", group.id),
        })),
    ];

    const plan = buildAssignmentPlan(picked, removing, assignedUserIds);
    const isDirty = hasPlannedChanges(plan);
    const isBusy = isSaving || setAutoAssign.isPending;

    function handlePick(result: PickerResult) {
        if (result.isAssigned) {
            // Already has it: jump to the row in the assigned list instead of double-assigning.
            setHighlightedUserId(result.type === "person" ? result.user.id : null);
            return;
        }
        setHighlightedUserId(null);
        setPicked((items) =>
            result.type === "person"
                ? [...items, { type: "person", id: result.user.id, name: result.user.name ?? "", email: result.user.email }]
                : [
                      ...items,
                      {
                          type: "team",
                          id: result.group.id,
                          name: result.group.name,
                          membersCount: Number(result.group.membersCount),
                          gradeIds: [],
                      },
                  ],
        );
    }

    function requestClose() {
        if (isBusy) {
            return;
        }
        if (isDirty && !window.confirm(DISCARD_CHANGES_PROMPT)) {
            return;
        }
        onClose();
    }

    async function applyAutoAssign(enabled: boolean) {
        if (roadmapId === null) {
            return;
        }
        try {
            const response = await setAutoAssign.mutateAsync({ roadmapId, enabled });
            setIsAutoAssignOn(Boolean(response?.roadmap?.autoAssignNewUsers ?? enabled));
            setIsConfirmingOff(false);
            onNotify(enabled ? AUTO_ASSIGN_ON_TOAST : AUTO_ASSIGN_OFF_TOAST, "success", () => {
                void setAutoAssign
                    .mutateAsync({ roadmapId, enabled: !enabled })
                    .then((undone) => setIsAutoAssignOn(Boolean(undone?.roadmap?.autoAssignNewUsers ?? !enabled)));
            });
        } catch (error) {
            onNotify(error instanceof Error ? error.message : "Failed to update auto-assignment.", "error");
        }
    }

    async function handleSave() {
        if (roadmapId === null || !isDirty) {
            return;
        }
        setIsSaving(true);
        try {
            const removedUserIds = [...removing]
                .filter((key) => key.startsWith("p:"))
                .map((key) => Number(key.slice(2)));
            const removedGroupIds = [...removing]
                .filter((key) => key.startsWith("t:"))
                .map((key) => Number(key.slice(2)));
            if (removedUserIds.length > 0) {
                await revokePeople.mutateAsync({ itemId: roadmapId, userIds: removedUserIds });
            }
            for (const groupId of removedGroupIds) {
                await unassignTeam.mutateAsync({ roadmapId, groupId });
            }
            for (const item of picked) {
                if (item.type === "team") {
                    await assignTeam.mutateAsync({ roadmapId, groupId: item.id, gradeIds: item.gradeIds });
                }
            }
            const pickedUserIds = picked.filter((item) => item.type === "person").map((item) => item.id);
            if (pickedUserIds.length > 0) {
                await assignPeople.mutateAsync({ itemId: roadmapId, userIds: pickedUserIds });
            }
            const summary = [
                plan.newPeopleCount > 0
                    ? `Assigned to ${plan.newPeopleCount} ${plan.newPeopleCount === 1 ? "person" : "people"}`
                    : plan.pickedTeamCount > 0
                      ? `Assigned to ${plan.pickedTeamCount} team${plan.pickedTeamCount === 1 ? "" : "s"}`
                      : "",
                plan.removalCount > 0 ? `${plan.removalCount} removed` : "",
            ]
                .filter(Boolean)
                .join(" · ");
            onNotify(summary || "Assignments saved.");
            onClose();
        } catch (error) {
            onNotify(error instanceof Error ? error.message : "Failed to save assignments.", "error");
        } finally {
            setIsSaving(false);
        }
    }

    return (
        <Dialog
            open={open}
            onClose={requestClose}
            size="md"
            className="roadmap-assign-dialog"
            title={
                <span className="roadmap-assign-dialog__heading">
                    <span className="roadmap-assign-dialog__eyebrow">Assign roadmap</span>
                    <span className="roadmap-assign-dialog__title">{roadmap?.title}</span>
                </span>
            }
            closeDisabled={isBusy}
            footer={
                <div className="roadmap-assign-dialog__footer">
                    <p className="roadmap-assign-dialog__hint" role="status">
                        {planHint(plan)}
                    </p>
                    <div className="roadmap-assign-dialog__actions">
                        <Button variant="ghost" onClick={requestClose} disabled={isBusy}>
                            Cancel
                        </Button>
                        <Button
                            variant={plan.removalCount > 0 && plan.newPeopleCount === 0 && plan.pickedTeamCount === 0 ? "danger" : "primary"}
                            onClick={() => void handleSave()}
                            disabled={!isDirty || isBusy}
                        >
                            {isSaving ? "Saving…" : primaryActionLabel(plan)}
                        </Button>
                    </div>
                </div>
            }
        >
            <div className="roadmap-assign-dialog__body">
                <section className="roadmap-assign-dialog__section">
                    <p className="roadmap-assign-dialog__label">Add people or teams</p>
                    <RoadmapAssignPicker
                        roadmapId={roadmapId ?? -1}
                        query={query}
                        picked={picked}
                        results={results}
                        grades={gradesQuery.data ?? []}
                        isSearching={usersQuery.isFetching || groupsQuery.isFetching}
                        disabled={isBusy}
                        onQueryChange={setQuery}
                        onPick={handlePick}
                        onUnpick={(index) => setPicked((items) => items.filter((_, i) => i !== index))}
                        onTeamGradesChange={(groupId, gradeIds) =>
                            setPicked((items) =>
                                items.map((item) =>
                                    item.type === "team" && item.id === groupId ? { ...item, gradeIds } : item,
                                ),
                            )
                        }
                        onTeamPreview={(groupId, matchedUserIds) =>
                            setPicked((items) =>
                                items.map((item) =>
                                    item.type === "team" && item.id === groupId && !sameUserIds(item.matchedUserIds, matchedUserIds)
                                        ? { ...item, matchedUserIds }
                                        : item,
                                ),
                            )
                        }
                    />
                </section>

                <section className="roadmap-assign-dialog__section roadmap-assign-auto">
                    <div className="roadmap-assign-auto__row">
                        <span className="roadmap-assign-auto__icon" aria-hidden="true">
                            <BoltOutlinedIcon />
                        </span>
                        <span className="roadmap-assign-auto__text">
                            <strong>{AUTO_ASSIGN_LABEL}</strong>
                            <span>{AUTO_ASSIGN_DESCRIPTION}</span>
                        </span>
                        <button
                            type="button"
                            role="switch"
                            aria-checked={isAutoAssignOn}
                            aria-label="Automatically assign to new users"
                            className="roadmap-assign-switch"
                            disabled={!canToggleAutoAssign || isBusy}
                            title={canToggleAutoAssign ? undefined : AUTO_ASSIGN_ADMIN_ONLY_HINT}
                            onClick={() => {
                                if (isAutoAssignOn) {
                                    setIsConfirmingOff(true);
                                    return;
                                }
                                void applyAutoAssign(true);
                            }}
                        />
                    </div>
                    {!canToggleAutoAssign && (
                        <p className="roadmap-assign-auto__hint">{AUTO_ASSIGN_ADMIN_ONLY_HINT}</p>
                    )}
                    {isConfirmingOff && (
                        <div className="roadmap-assign-auto__confirm" role="alert">
                            <span>{AUTO_ASSIGN_OFF_WARNING}</span>
                            <button type="button" onClick={() => setIsConfirmingOff(false)}>
                                Keep on
                            </button>
                            <button
                                type="button"
                                className="roadmap-assign-auto__confirm-danger"
                                onClick={() => void applyAutoAssign(false)}
                            >
                                Turn off
                            </button>
                        </div>
                    )}
                </section>

                <section className="roadmap-assign-dialog__section">
                    <RoadmapAssignedList
                        assignees={assignees}
                        totalPeopleCount={roadmap?.assigneeCount}
                        teams={teams}
                        removing={removing}
                        isLoading={assigneesQuery.isLoading || teamAssignmentsQuery.isLoading}
                        disabled={isBusy}
                        highlightedUserId={highlightedUserId}
                        onStageRemoval={(key) => setRemoving((keys) => new Set([...keys, key]))}
                        onUndoRemoval={(key) =>
                            setRemoving((keys) => {
                                const next = new Set(keys);
                                next.delete(key);
                                return next;
                            })
                        }
                    />
                </section>
            </div>
        </Dialog>
    );
}
