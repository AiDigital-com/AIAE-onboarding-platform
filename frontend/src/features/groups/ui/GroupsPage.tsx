import { useEffect, useState } from "react";
import AddOutlinedIcon from "@mui/icons-material/AddOutlined";
import ChevronLeftOutlinedIcon from "@mui/icons-material/ChevronLeftOutlined";
import ChevronRightOutlinedIcon from "@mui/icons-material/ChevronRightOutlined";
import FormatListBulletedOutlinedIcon from "@mui/icons-material/FormatListBulletedOutlined";
import SearchOutlinedIcon from "@mui/icons-material/SearchOutlined";
import { useCurrentUser } from "@/shared/auth/useCurrentUser";
import { useHasPermission } from "@/shared/auth/useHasPermission";
import { useDebounce } from "@/shared/hooks/useDebounce";
import { FeatureEmptyState } from "@/shared/ui/FeatureEmptyState";
import { ErrorAlert } from "@/shared/ui/ErrorAlert";
import { LoadingBlock } from "@/shared/ui/LoadingBlock";
import { Button } from "@/shared/ui/Button";
import { Dialog } from "@/shared/ui/Dialog";
import { Toast, ToastSeverity } from "@/shared/ui/Toast";
import { getApiErrorMessage } from "@/shared/lib/apiError";
import { useGroupOrgStatsQuery, useGroupsQuery } from "../api/useGroupsQuery";
import { useGradesQuery } from "../api/useGradesQuery";
import { useCreateGroupMutation, useDeleteGroupMutation } from "../api/useGroupMutations";
import { GroupDetailView } from "./GroupDetailView";
import { GradesDrawer } from "./GradesDrawer";
import { leadSummary } from "../lib/leadSummary";
import type { GroupSummaryV1 } from "../api/types";
import "./groups.css";

/** Teams admin page: searchable team list on the left, persistent detail panel on the right. */
export function GroupsPage() {
    const { user: currentUser } = useCurrentUser();
    const hasPermission = useHasPermission();
    const canManageStructure = hasPermission("groups.manage");
    const canManageGrades = hasPermission("grades.manage");

    const [searchQuery, setSearchQuery] = useState("");
    const debouncedSearchQuery = useDebounce(searchQuery, 300);
    const [page, setPage] = useState(0);
    const [selectedGroupId, setSelectedGroupId] = useState<number | null>(null);
    const [isGradesDrawerOpen, setIsGradesDrawerOpen] = useState(false);

    const groupsQuery = useGroupsQuery(debouncedSearchQuery.trim() || undefined, page);
    const gradesQuery = useGradesQuery();
    const statsQuery = useGroupOrgStatsQuery();

    const createMutation = useCreateGroupMutation();
    const deleteMutation = useDeleteGroupMutation();

    const [isCreatingInline, setIsCreatingInline] = useState(false);
    const [newTeamName, setNewTeamName] = useState("");
    const [newTeamDescription, setNewTeamDescription] = useState("");
    const [teamNameError, setTeamNameError] = useState("");
    const [isCreatingTeam, setIsCreatingTeam] = useState(false);
    const [deletingGroup, setDeletingGroup] = useState<GroupSummaryV1 | null>(null);
    const [isDeleting, setIsDeleting] = useState(false);
    const [toast, setToast] = useState<{ open: boolean; message: string; severity: ToastSeverity }>({
        open: false,
        message: "",
        severity: "success",
    });

    const showToast = (message: string, severity: ToastSeverity = "success") => {
        setToast({ open: true, message, severity });
    };

    const groups = groupsQuery.data?.items ?? [];
    const pageInfo = groupsQuery.data?.page;
    const grades = gradesQuery.data ?? [];
    const stats = statsQuery.data;

    useEffect(() => {
        if (groups.length === 0) {
            if (selectedGroupId !== null) {
                setSelectedGroupId(null);
            }
            return;
        }
        if (selectedGroupId === null || !groups.some((group) => group.id === selectedGroupId)) {
            setSelectedGroupId(groups[0].id);
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [groups]);

    const selectedGroup = groups.find((group) => group.id === selectedGroupId) ?? null;

    const canManageGroup = (group: GroupSummaryV1) =>
        canManageStructure || group.leads.some((lead) => lead.id === currentUser?.id);

    const handleSearchChange = (value: string) => {
        setSearchQuery(value);
        setPage(0);
    };

    const startCreate = () => {
        setNewTeamName("");
        setNewTeamDescription("");
        setTeamNameError("");
        setIsCreatingInline(true);
    };

    const cancelCreate = () => {
        setIsCreatingInline(false);
        setTeamNameError("");
    };

    const submitCreate = async () => {
        const trimmed = newTeamName.trim();
        if (!trimmed) {
            setTeamNameError("Team name is required.");
            showToast("Please enter a team name.", "error");
            return;
        }
        if (trimmed.length < 3 || trimmed.length > 100) {
            setTeamNameError("Team name must be 3-100 characters.");
            showToast("Team name must be 3-100 characters.", "warning");
            return;
        }
        try {
            setIsCreatingTeam(true);
            setTeamNameError("");
            const createdGroup = await createMutation.mutateAsync({
                name: trimmed,
                description: newTeamDescription.trim(),
            });
            setSelectedGroupId(createdGroup.id);
            setIsCreatingInline(false);
            showToast("Team created.");
        } catch (error) {
            const message = getApiErrorMessage(error, "Failed to create team.");
            const isDuplicate =
                /already exists/i.test(message) || /conflict/i.test(message) || /409/.test(message);
            const displayMessage = isDuplicate ? "A team with this name already exists." : message;
            setTeamNameError(displayMessage);
            showToast(displayMessage, "error");
        } finally {
            setIsCreatingTeam(false);
        }
    };

    const handleDelete = async () => {
        if (!deletingGroup) {
            return;
        }
        try {
            setIsDeleting(true);
            await deleteMutation.mutateAsync(deletingGroup.id);
            showToast("Team deleted.");
            setDeletingGroup(null);
        } catch (error) {
            showToast(getApiErrorMessage(error, "Failed to delete team."), "error");
        } finally {
            setIsDeleting(false);
        }
    };

    if (groupsQuery.isLoading) {
        return <LoadingBlock label="Loading teams…" />;
    }

    if (groupsQuery.error) {
        return <ErrorAlert message={getApiErrorMessage(groupsQuery.error, "Failed to load teams.")} />;
    }

    return (
        <div className="groups-page">
            <div className="groups-page__head">
                <div>
                    <p className="groups-page__eyebrow">Organization</p>
                    <h1 className="groups-page__title">Teams</h1>
                    <p className="groups-page__subtitle">
                        Browse teams, manage members and grades, and assign roadmaps to a whole team at once.
                    </p>
                </div>
                <div className="groups-page__head-actions">
                    {canManageGrades && (
                        <Button variant="secondary" onClick={() => setIsGradesDrawerOpen(true)}>
                            <FormatListBulletedOutlinedIcon /> Manage grades
                        </Button>
                    )}
                    {canManageStructure && (
                        <Button onClick={startCreate} disabled={isCreatingInline}>
                            <AddOutlinedIcon /> Create team
                        </Button>
                    )}
                </div>
            </div>

            {stats && (
                <div className="groups-page__stats">
                    <div className="groups-stat-card">
                        <p className="groups-stat-card__value">{stats.totalGroups}</p>
                        <p className="groups-stat-card__label">Teams</p>
                    </div>
                    <div className="groups-stat-card">
                        <p className="groups-stat-card__value">{stats.totalMembers}</p>
                        <p className="groups-stat-card__label">Members</p>
                    </div>
                    <div className="groups-stat-card">
                        <p className="groups-stat-card__value">{stats.totalLeads}</p>
                        <p className="groups-stat-card__label">Leads</p>
                    </div>
                    <div className="groups-stat-card">
                        <p className="groups-stat-card__value">{grades.length}</p>
                        <p className="groups-stat-card__label">Active grades</p>
                    </div>
                </div>
            )}

            <div className="groups-page__workspace">
                <div className="groups-list-panel">
                    <div className="groups-list-panel__head">
                        <div className="groups-list-panel__search">
                            <SearchOutlinedIcon aria-hidden="true" />
                            <input
                                type="search"
                                value={searchQuery}
                                placeholder="Search team or lead…"
                                onChange={(event) => handleSearchChange(event.target.value)}
                            />
                        </div>
                    </div>

                    {isCreatingInline && (
                        <div className="groups-list-panel__create">
                            <input
                                type="text"
                                className={`group-add-form__input${teamNameError ? " group-add-form__input--error" : ""}`}
                                value={newTeamName}
                                placeholder="Team name, e.g. CS Campaign"
                                autoFocus
                                disabled={isCreatingTeam}
                                aria-invalid={Boolean(teamNameError)}
                                onChange={(event) => {
                                    setNewTeamName(event.target.value);
                                    if (teamNameError) {
                                        setTeamNameError("");
                                    }
                                }}
                                onKeyDown={(event) => {
                                    if (event.key === "Escape") {
                                        cancelCreate();
                                    }
                                    if (event.key === "Enter") {
                                        void submitCreate();
                                    }
                                }}
                            />
                            {teamNameError && <p className="group-add-form__error">{teamNameError}</p>}
                            <textarea
                                className="groups-list-panel__create-description"
                                value={newTeamDescription}
                                placeholder="Description (optional)"
                                rows={2}
                                disabled={isCreatingTeam}
                                onChange={(event) => setNewTeamDescription(event.target.value)}
                            />
                            <div className="groups-list-panel__create-actions">
                                <Button variant="ghost" size="sm" onClick={cancelCreate} disabled={isCreatingTeam}>
                                    Cancel
                                </Button>
                                <Button size="sm" onClick={() => void submitCreate()} disabled={isCreatingTeam}>
                                    {isCreatingTeam ? "Creating…" : "Create team"}
                                </Button>
                            </div>
                        </div>
                    )}

                    <div className="groups-list-panel__list">
                        {groups.length === 0 ? (
                            <FeatureEmptyState
                                title={debouncedSearchQuery.trim() ? "No teams match this search" : "No teams yet"}
                                description={
                                    debouncedSearchQuery.trim()
                                        ? "Try a different name or lead."
                                        : canManageStructure
                                          ? "Create a team to start organizing members and assigning roadmaps."
                                          : "You don't lead any teams yet."
                                }
                            />
                        ) : (
                            groups.map((group) => (
                                <button
                                    key={group.id}
                                    type="button"
                                    className={`groups-list-row${
                                        group.id === selectedGroupId ? " groups-list-row--active" : ""
                                    }`}
                                    onClick={() => setSelectedGroupId(group.id)}
                                    aria-current={group.id === selectedGroupId}
                                >
                                    <span className={`groups-list-row__avatar groups-list-row__avatar--${group.id % 7}`}>
                                        {group.name.charAt(0).toUpperCase()}
                                    </span>
                                    <span className="groups-list-row__copy">
                                        <span className="groups-list-row__title">{group.name}</span>
                                        <span className="groups-list-row__meta">{leadSummary(group.leads)}</span>
                                        <span className="groups-list-row__count">
                                            {group.membersCount} member{group.membersCount === 1 ? "" : "s"}
                                        </span>
                                    </span>
                                </button>
                            ))
                        )}
                    </div>

                    {pageInfo && pageInfo.totalPages > 1 && (
                        <div className="group-pagination group-pagination--panel">
                            <button
                                type="button"
                                className="group-pagination__button"
                                disabled={!pageInfo.hasPrevious}
                                onClick={() => setPage((prev) => Math.max(0, prev - 1))}
                            >
                                <ChevronLeftOutlinedIcon /> Prev
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
                                Next <ChevronRightOutlinedIcon />
                            </button>
                        </div>
                    )}
                </div>

                <div className="groups-detail-panel">
                    {selectedGroup ? (
                        <GroupDetailView
                            key={selectedGroup.id}
                            group={selectedGroup}
                            grades={grades}
                            canManageStructure={canManageStructure}
                            canManageGroup={canManageGroup(selectedGroup)}
                            onDelete={() => setDeletingGroup(selectedGroup)}
                            onToast={showToast}
                        />
                    ) : (
                        <FeatureEmptyState
                            title="No team selected"
                            description="Choose a team from the list to see its members, leads, and roadmaps."
                        />
                    )}
                </div>
            </div>

            <Dialog
                open={Boolean(deletingGroup)}
                onClose={() => (isDeleting ? undefined : setDeletingGroup(null))}
                closeDisabled={isDeleting}
                size="sm"
                title="Delete team"
                footer={
                    <>
                        <Button variant="ghost" onClick={() => setDeletingGroup(null)} disabled={isDeleting}>
                            Cancel
                        </Button>
                        <Button variant="danger" onClick={() => void handleDelete()} disabled={isDeleting}>
                            {isDeleting ? "Deleting…" : "Delete"}
                        </Button>
                    </>
                }
            >
                <p>
                    Delete <strong>{deletingGroup?.name}</strong>? Members keep their existing progress;
                    this only removes the team and its standing roadmap assignments.
                </p>
            </Dialog>

            <GradesDrawer
                open={isGradesDrawerOpen}
                onClose={() => setIsGradesDrawerOpen(false)}
                onToast={showToast}
            />

            <Toast
                open={toast.open}
                message={toast.message}
                severity={toast.severity}
                onClose={() => setToast((prev) => ({ ...prev, open: false }))}
            />
        </div>
    );
}
