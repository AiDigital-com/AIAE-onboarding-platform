import { useState } from "react";
import { Dialog } from "@/shared/ui/Dialog";
import { Button } from "@/shared/ui/Button";
import { LoadingSpinner } from "@/shared/ui/LoadingSpinner";
import { useDebounce } from "@/shared/hooks/useDebounce";
import { useGroupsQuery } from "@/features/groups/api/useGroupsQuery";
import { useGradesQuery } from "@/features/groups/api/useGradesQuery";
import { leadSummary } from "@/features/groups/lib/leadSummary";
import type { GradeV1, GroupSummaryV1, RoadmapGroupAssignmentV1 } from "@/features/groups/api/types";
import { useRoadmapGroupAssignmentPreviewQuery } from "../api/useRoadmapGroupAssignmentPreviewQuery";
import type { LibraryRoadmap } from "../api/types";

interface RoadmapGroupAssignmentDialogProps {
    open: boolean;
    roadmap: LibraryRoadmap | null;
    assignments: RoadmapGroupAssignmentV1[];
    isLoadingAssignments?: boolean;
    pendingGroupId?: number | null;
    onClose: () => void;
    onAssign: (groupId: number, gradeIds: number[]) => void;
    onRevoke: (groupId: number) => void;
}

function noGradeWarning(count: number): string | null {
    if (count <= 0) {
        return null;
    }
    return count === 1
        ? "1 member has no grade and will be skipped."
        : `${count} members have no grade and will be skipped.`;
}

function gradeFilterLabel(grades: GradeV1[]): string {
    return grades.length === 0 ? "All grades" : grades.map((grade) => grade.name).join(", ");
}

/** Grade multi-select checkboxes for narrowing a group assignment; empty selection means the whole group. */
function GradeFilterPicker({
    grades,
    selectedGradeIds,
    onToggle,
}: {
    grades: GradeV1[];
    selectedGradeIds: number[];
    onToggle: (gradeId: number) => void;
}) {
    return (
        <div className="library-assignment-grade-picker">
            {grades.map((grade) => (
                <label key={grade.id} className="library-assignment-grade-picker__option">
                    <input
                        type="checkbox"
                        checked={selectedGradeIds.includes(grade.id)}
                        onChange={() => onToggle(grade.id)}
                    />
                    {grade.name}
                </label>
            ))}
        </div>
    );
}

function AssignedGroupRow({
    assignment,
    isPending,
    onRevoke,
}: {
    assignment: RoadmapGroupAssignmentV1;
    isPending: boolean;
    onRevoke: () => void;
}) {
    return (
        <div className="library-assignment-item library-assignment-item--static">
            <span className="library-assignment-item__copy">
                <p className="library-assignment-item__primary">{assignment.groupName}</p>
                <p className="library-assignment-item__secondary">{leadSummary(assignment.groupLeads)}</p>
                <p className="library-assignment-item__meta">
                    {gradeFilterLabel(assignment.gradeFilters)} · {assignment.membersMatchedCount} enrolled
                    {assignment.gradeFilters.length > 0 && assignment.membersWithoutGradeCount > 0
                        ? ` · ${noGradeWarning(assignment.membersWithoutGradeCount)}`
                        : ""}
                    {assignment.assignedByUserName ? ` · assigned by ${assignment.assignedByUserName}` : ""}
                </p>
            </span>
            <Button variant="danger" disabled={isPending} onClick={onRevoke}>
                {isPending ? "Working…" : "Revoke"}
            </Button>
        </div>
    );
}

function AssignableGroupRow({
    group,
    grades,
    roadmapId,
    isPending,
    onAssign,
}: {
    group: GroupSummaryV1;
    grades: GradeV1[];
    roadmapId: number;
    isPending: boolean;
    onAssign: (gradeIds: number[]) => void;
}) {
    const [isExpanded, setIsExpanded] = useState(false);
    const [selectedGradeIds, setSelectedGradeIds] = useState<number[]>([]);

    const previewQuery = useRoadmapGroupAssignmentPreviewQuery(
        roadmapId,
        group.id,
        selectedGradeIds,
        isExpanded,
    );

    const toggleGrade = (gradeId: number) => {
        setSelectedGradeIds((prev) =>
            prev.includes(gradeId) ? prev.filter((id) => id !== gradeId) : [...prev, gradeId],
        );
    };

    return (
        <div className="library-assignment-item library-assignment-item--expandable">
            <button
                type="button"
                className="library-assignment-item__row"
                onClick={() => setIsExpanded((prev) => !prev)}
                disabled={isPending}
            >
                <span className="library-assignment-item__copy">
                    <p className="library-assignment-item__primary">{group.name}</p>
                    <p className="library-assignment-item__secondary">{leadSummary(group.leads)}</p>
                    <p className="library-assignment-item__meta">
                        {group.membersCount} member{group.membersCount === 1 ? "" : "s"}
                    </p>
                </span>
                <span className="library-assignment-item__action">{isExpanded ? "Cancel" : "Assign"}</span>
            </button>

            {isExpanded && (
                <div className="library-assignment-item__expansion">
                    {grades.length > 0 && (
                        <GradeFilterPicker
                            grades={grades}
                            selectedGradeIds={selectedGradeIds}
                            onToggle={toggleGrade}
                        />
                    )}
                    <p className="library-assignment-item__hint">
                        {selectedGradeIds.length === 0
                            ? "No grade selected — the whole team will be enrolled."
                            : previewQuery.data
                              ? `${previewQuery.data.membersMatchedCount} of ${previewQuery.data.groupMembersCount} members match.`
                              : "Checking match…"}
                    </p>
                    {selectedGradeIds.length > 0 &&
                        previewQuery.data &&
                        previewQuery.data.membersWithoutGradeCount > 0 && (
                            <p className="library-assignment-item__warning">
                                {noGradeWarning(previewQuery.data.membersWithoutGradeCount)}
                            </p>
                        )}
                    <Button disabled={isPending} onClick={() => onAssign(selectedGradeIds)}>
                        {isPending ? "Assigning…" : "Confirm assign"}
                    </Button>
                </div>
            )}
        </div>
    );
}

/** Assigns a roadmap to a group as a standing, grade-filterable rule. Explicit Assign/Revoke actions per row. */
export function RoadmapGroupAssignmentDialog({
    open,
    roadmap,
    assignments,
    isLoadingAssignments = false,
    pendingGroupId = null,
    onClose,
    onAssign,
    onRevoke,
}: RoadmapGroupAssignmentDialogProps) {
    const [searchQuery, setSearchQuery] = useState("");
    const debouncedSearchQuery = useDebounce(searchQuery, 300);
    const [page, setPage] = useState(0);

    const groupsQuery = useGroupsQuery(debouncedSearchQuery.trim() || undefined, page, { enabled: open });
    const gradesQuery = useGradesQuery();

    const groups = groupsQuery.data?.items ?? [];
    const pageInfo = groupsQuery.data?.page;
    const grades = gradesQuery.data ?? [];
    const assignedGroupIds = new Set(assignments.map((assignment) => assignment.groupId));
    const assignableGroups = groups.filter((group) => !assignedGroupIds.has(group.id));

    return (
        <Dialog
            open={open}
            onClose={onClose}
            size="sm"
            title="Assign roadmap to team"
            flushBody
            footer={
                <Button variant="ghost" onClick={onClose}>
                    Close
                </Button>
            }
        >
            <div className="library-form-body">
                <p style={{ color: "#80808e" }}>
                    Choose teams that should get this roadmap. Matching members are enrolled
                    immediately, and members added or re-graded later are enrolled automatically.
                </p>
                {roadmap?.title && <strong style={{ fontSize: 18 }}>{roadmap.title}</strong>}

                {assignments.length > 0 && (
                    <div className="library-assignment-list">
                        {assignments.map((assignment) => (
                            <AssignedGroupRow
                                key={assignment.groupId}
                                assignment={assignment}
                                isPending={pendingGroupId === assignment.groupId}
                                onRevoke={() => onRevoke(assignment.groupId)}
                            />
                        ))}
                    </div>
                )}

                <label className="library-assignment-search">
                    <span>Search teams</span>
                    <input
                        type="search"
                        value={searchQuery}
                        onChange={(event) => {
                            setSearchQuery(event.target.value);
                            setPage(0);
                        }}
                        placeholder="Search team or lead…"
                    />
                </label>

                {isLoadingAssignments || (groups.length === 0 && groupsQuery.isLoading) ? (
                    <div className="library-assignment-loading">
                        <LoadingSpinner label="Loading teams" />
                    </div>
                ) : assignableGroups.length === 0 ? (
                    <p style={{ color: "#80808e" }}>No more teams available to assign.</p>
                ) : (
                    <div className="library-assignment-list">
                        {assignableGroups.map((group) => (
                            <AssignableGroupRow
                                key={group.id}
                                group={group}
                                grades={grades}
                                roadmapId={roadmap?.id ?? -1}
                                isPending={pendingGroupId === group.id}
                                onAssign={(gradeIds) => onAssign(group.id, gradeIds)}
                            />
                        ))}
                    </div>
                )}

                {pageInfo && pageInfo.totalPages > 1 && (
                    <div className="group-pagination">
                        <button
                            type="button"
                            className="group-pagination__button"
                            disabled={!pageInfo.hasPrevious}
                            onClick={() => setPage((prev) => Math.max(0, prev - 1))}
                        >
                            Prev
                        </button>
                        <span className="group-pagination__label">
                            Page {pageInfo.page + 1} of {pageInfo.totalPages}
                        </span>
                        <button
                            type="button"
                            className="group-pagination__button"
                            disabled={!pageInfo.hasNext}
                            onClick={() => setPage((prev) => prev + 1)}
                        >
                            Next
                        </button>
                    </div>
                )}
            </div>
        </Dialog>
    );
}
