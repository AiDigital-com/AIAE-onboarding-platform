import { useState } from "react";
import { Dialog } from "@/shared/ui/Dialog";
import { Button } from "@/shared/ui/Button";
import { LoadingSpinner } from "@/shared/ui/LoadingSpinner";
import { UserAvatar } from "@/shared/ui/UserAvatar";
import type { components } from "@/shared/api/generated/schema";
import type { AssignableUserV1 } from "../api/types";
import type { LibraryLesson, LibraryRoadmap } from "../api/types";

type LearningAssigneeV1 = components["schemas"]["LearningAssigneeV1"];

const PREVIEW_ROW_COUNT = 3;

interface LearningAssignmentDialogProps {
    open: boolean;
    item: LibraryLesson | LibraryRoadmap | null;
    itemType?: "lesson" | "roadmap";
    users: AssignableUserV1[];
    assignees?: LearningAssigneeV1[];
    isLoadingAssignees?: boolean;
    searchQuery?: string;
    selectedUserIds: number[];
    selectedRevokeUserIds?: number[];
    isLoading?: boolean;
    isSaving?: boolean;
    isRevoking?: boolean;
    onClose: () => void;
    onSearchChange?: (value: string) => void;
    onToggleUser: (userId: number) => void;
    onToggleAll: () => void;
    onAssign: () => void;
    onToggleRevokeUser?: (userId: number) => void;
    onToggleAllRevoke?: () => void;
    onRevokeSelected?: () => void;
}

function SelectableUserRow({
    id,
    name,
    email,
    meta,
    checked,
    disabled,
    onClick,
}: {
    id: number;
    name: string;
    email: string;
    meta?: string;
    checked: boolean;
    disabled?: boolean;
    onClick: () => void;
}) {
    return (
        <button
            key={id}
            type="button"
            className="library-assignment-item library-assignment-user-row"
            onClick={onClick}
            disabled={disabled}
        >
            <UserAvatar user={{ name, email }} />
            <span className="library-assignment-user-row__copy">
                <p className="library-assignment-item__primary">{name || email}</p>
                <p className="library-assignment-item__secondary">{email}</p>
                {meta && <p className="library-assignment-item__meta">{meta}</p>}
            </span>
            <input
                className="library-assignment-user-row__checkbox"
                type="checkbox"
                checked={checked}
                readOnly
                tabIndex={-1}
            />
        </button>
    );
}

function ShowMoreToggle({
    hiddenCount,
    isExpanded,
    onToggle,
}: {
    hiddenCount: number;
    isExpanded: boolean;
    onToggle: () => void;
}) {
    if (hiddenCount <= 0 && !isExpanded) {
        return null;
    }
    return (
        <button
            type="button"
            onClick={onToggle}
            style={{
                alignSelf: "flex-start",
                background: "none",
                border: "none",
                padding: "4px 0",
                color: "#0009dc",
                fontSize: 13,
                fontWeight: 700,
                cursor: "pointer",
            }}
        >
            {isExpanded ? "Show less" : `+${hiddenCount} more…`}
        </button>
    );
}

export function LearningAssignmentDialog({
    open,
    item,
    itemType = "lesson",
    users = [],
    assignees = [],
    isLoadingAssignees = false,
    searchQuery = "",
    selectedUserIds = [],
    selectedRevokeUserIds = [],
    isLoading = false,
    isSaving = false,
    isRevoking = false,
    onClose,
    onSearchChange,
    onToggleUser,
    onToggleAll,
    onAssign,
    onToggleRevokeUser,
    onToggleAllRevoke,
    onRevokeSelected,
}: LearningAssignmentDialogProps) {
    const [isAssignedExpanded, setIsAssignedExpanded] = useState(false);
    const [isAssignableExpanded, setIsAssignableExpanded] = useState(false);

    const assignedUserIds = new Set(assignees.map((assignee) => assignee.userId));
    const selectableUsers = users.filter((user) => !assignedUserIds.has(user.id));
    const allSelected = selectableUsers.length > 0 && selectedUserIds.length === selectableUsers.length;
    const allRevokeSelected = assignees.length > 0 && selectedRevokeUserIds.length === assignees.length;
    const title = itemType === "roadmap" ? "Assign roadmap" : "Assign lesson";
    const noun = itemType === "roadmap" ? "roadmap" : "lesson";

    const normalizedSearch = searchQuery.trim().toLowerCase();
    const isSearching = normalizedSearch.length > 0;
    const filteredAssignees = isSearching
        ? assignees.filter((assignee) =>
              (assignee.name || "").toLowerCase().includes(normalizedSearch) ||
              (assignee.email || "").toLowerCase().includes(normalizedSearch),
          )
        : assignees;

    const visibleAssignees =
        isSearching || isAssignedExpanded ? filteredAssignees : filteredAssignees.slice(0, PREVIEW_ROW_COUNT);
    const hiddenAssignedCount = filteredAssignees.length - visibleAssignees.length;

    const visibleSelectableUsers =
        isSearching || isAssignableExpanded ? selectableUsers : selectableUsers.slice(0, PREVIEW_ROW_COUNT);
    const hiddenSelectableCount = selectableUsers.length - visibleSelectableUsers.length;

    return (
        <Dialog
            open={open}
            onClose={isSaving || isRevoking ? undefined : onClose}
            size="sm"
            title={title}
            flushBody
            footer={
                <>
                    <Button variant="ghost" onClick={onClose} disabled={isSaving || isRevoking}>
                        Cancel
                    </Button>
                    <Button variant="primary" onClick={onAssign} disabled={isSaving || selectedUserIds.length === 0}>
                        {isSaving ? "Assigning..." : `Assign to ${selectedUserIds.length || 0}`}
                    </Button>
                </>
            }
        >
            <div className="library-form-body">
                <p style={{ color: "#80808e" }}>
                    Choose team members who should get this {noun} in their learning plan.
                </p>
                {item?.title && <strong style={{ fontSize: 18 }}>{item.title}</strong>}

                <label className="library-assignment-search">
                    <span>Search users</span>
                    <input
                        type="search"
                        value={searchQuery}
                        onChange={(event) => onSearchChange?.(event.target.value)}
                        placeholder="Name or email"
                        disabled={isSaving}
                    />
                </label>

                {(assignees.length > 0 || isLoadingAssignees) && (
                    <>
                        <p className="library-form-section-label">Currently assigned</p>
                        {isLoadingAssignees ? (
                            <div className="library-assignment-loading">
                                <LoadingSpinner label="Loading assigned learners" />
                            </div>
                        ) : (
                            <>
                                {assignees.length > 0 && (
                                    <label style={{ display: "flex", gap: 8, alignItems: "center" }}>
                                        <input
                                            type="checkbox"
                                            checked={allRevokeSelected}
                                            ref={(element) => {
                                                if (element) {
                                                    element.indeterminate =
                                                        selectedRevokeUserIds.length > 0 && !allRevokeSelected;
                                                }
                                            }}
                                            onChange={onToggleAllRevoke}
                                            disabled={isRevoking}
                                        />
                                        Select all
                                    </label>
                                )}
                                {filteredAssignees.length === 0 ? (
                                    <p style={{ color: "#80808e" }}>No assigned learners match your search.</p>
                                ) : (
                                    <div className="library-assignment-list">
                                        {visibleAssignees.map((assignee) => (
                                            <SelectableUserRow
                                                key={assignee.userId}
                                                id={assignee.userId}
                                                name={assignee.name || ""}
                                                email={assignee.email || ""}
                                                meta={
                                                    assignee.isCompleted != null
                                                        ? assignee.isCompleted
                                                            ? "Completed"
                                                            : "In progress"
                                                        : undefined
                                                }
                                                checked={selectedRevokeUserIds.includes(assignee.userId)}
                                                disabled={isRevoking}
                                                onClick={() => onToggleRevokeUser?.(assignee.userId)}
                                            />
                                        ))}
                                    </div>
                                )}
                                {!isSearching && (
                                    <ShowMoreToggle
                                        hiddenCount={hiddenAssignedCount}
                                        isExpanded={isAssignedExpanded}
                                        onToggle={() => setIsAssignedExpanded((prev) => !prev)}
                                    />
                                )}
                                <Button
                                    variant="danger"
                                    onClick={onRevokeSelected}
                                    disabled={isRevoking || selectedRevokeUserIds.length === 0}
                                >
                                    {isRevoking ? "Revoking..." : `Revoke ${selectedRevokeUserIds.length || 0}`}
                                </Button>
                            </>
                        )}
                    </>
                )}

                {selectableUsers.length > 0 && (
                    <label style={{ display: "flex", gap: 8, alignItems: "center" }}>
                        <input
                            type="checkbox"
                            checked={allSelected}
                            ref={(element) => {
                                if (element) {
                                    element.indeterminate = selectedUserIds.length > 0 && !allSelected;
                                }
                            }}
                            onChange={onToggleAll}
                        />
                        Select all
                    </label>
                )}

                {selectableUsers.length === 0 && isLoading ? (
                    <div className="library-assignment-loading">
                        <LoadingSpinner label="Loading team members" />
                    </div>
                ) : selectableUsers.length === 0 ? (
                    <p style={{ color: "#80808e" }}>No more assignable team members found.</p>
                ) : (
                    <>
                        <div className="library-assignment-list">
                            {visibleSelectableUsers.map((user) => (
                                <SelectableUserRow
                                    key={user.id}
                                    id={user.id}
                                    name={user.name || ""}
                                    email={user.email || ""}
                                    checked={selectedUserIds.includes(user.id)}
                                    disabled={isSaving}
                                    onClick={() => onToggleUser(user.id)}
                                />
                            ))}
                        </div>
                        {!isSearching && (
                            <ShowMoreToggle
                                hiddenCount={hiddenSelectableCount}
                                isExpanded={isAssignableExpanded}
                                onToggle={() => setIsAssignableExpanded((prev) => !prev)}
                            />
                        )}
                    </>
                )}
            </div>
        </Dialog>
    );
}
