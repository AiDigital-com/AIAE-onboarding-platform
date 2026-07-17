import { useEffect, useRef, useState } from "react";
import AddOutlinedIcon from "@mui/icons-material/AddOutlined";
import ChevronLeftOutlinedIcon from "@mui/icons-material/ChevronLeftOutlined";
import ChevronRightOutlinedIcon from "@mui/icons-material/ChevronRightOutlined";
import DeleteOutlineOutlinedIcon from "@mui/icons-material/DeleteOutlineOutlined";
import EditOutlinedIcon from "@mui/icons-material/EditOutlined";
import MoreHorizOutlinedIcon from "@mui/icons-material/MoreHorizOutlined";
import { Button } from "@/shared/ui/Button";
import { UserAvatar } from "@/shared/ui/UserAvatar";
import { useFilePreviewUrls } from "@/shared/api/files";
import { LoadingSpinner } from "@/shared/ui/LoadingSpinner";
import { useDebounce } from "@/shared/hooks/useDebounce";
import { getApiErrorMessage } from "@/shared/lib/apiError";
import { useGroupMembersQuery, useGroupRoadmapAssignmentsQuery } from "../api/useGroupsQuery";
import {
    useAddGroupLeadMutation,
    useAddGroupMemberMutation,
    useRemoveGroupLeadMutation,
    useRemoveGroupMemberMutation,
    useUpdateGroupMutation,
} from "../api/useGroupMutations";
import { useUpdateUserGradeMutation } from "../api/useGradeMutations";
import { InlineGradeSelect } from "./InlineGradeSelect";
import { UserSearchCombobox } from "./UserSearchCombobox";
import { leadSummary } from "../lib/leadSummary";
import type { GradeV1, GroupSummaryV1, UserSummaryV1 } from "../api/types";

type DetailTab = "members" | "leads" | "roadmaps";

const GROUP_MEMBERS_SEARCH_THRESHOLD = 8;

interface GroupDetailViewProps {
    group: GroupSummaryV1;
    grades: GradeV1[];
    /** Admin only: full structural management (create/delete group, add/remove member/lead). */
    canManageStructure: boolean;
    /** Admin or the group's own lead: rename, edit member grades. */
    canManageGroup: boolean;
    onDelete: () => void;
    onToast: (message: string, severity?: "success" | "error" | "warning") => void;
}

export function GroupDetailView({
    group,
    grades,
    canManageStructure,
    canManageGroup,
    onDelete,
    onToast,
}: GroupDetailViewProps) {
    const [activeTab, setActiveTab] = useState<DetailTab>("members");
    const [isMoreMenuOpen, setIsMoreMenuOpen] = useState(false);
    const moreMenuRef = useRef<HTMLDivElement>(null);

    const [isEditingName, setIsEditingName] = useState(false);
    const [nameDraft, setNameDraft] = useState(group.name);
    const [descriptionDraft, setDescriptionDraft] = useState(group.description ?? "");
    const updateGroupMutation = useUpdateGroupMutation();

    const [selectedMember, setSelectedMember] = useState<UserSummaryV1 | null>(null);
    const [selectedLead, setSelectedLead] = useState<UserSummaryV1 | null>(null);
    const [memberPickerResetKey, setMemberPickerResetKey] = useState(0);
    const [leadPickerResetKey, setLeadPickerResetKey] = useState(0);
    const [pendingAction, setPendingAction] = useState(false);
    const [memberSearch, setMemberSearch] = useState("");
    const [memberPage, setMemberPage] = useState(0);
    const debouncedMemberSearch = useDebounce(memberSearch, 300);

    const membersQuery = useGroupMembersQuery(group.id, debouncedMemberSearch.trim() || undefined, memberPage, {
        enabled: activeTab === "members",
    });
    const roadmapAssignmentsQuery = useGroupRoadmapAssignmentsQuery(group.id, {
        enabled: activeTab === "roadmaps",
    });
    const addMemberMutation = useAddGroupMemberMutation();
    const removeMemberMutation = useRemoveGroupMemberMutation();
    const addLeadMutation = useAddGroupLeadMutation();
    const removeLeadMutation = useRemoveGroupLeadMutation();
    const updateGradeMutation = useUpdateUserGradeMutation();

    const members = membersQuery.data?.items ?? [];
    const memberPageInfo = membersQuery.data?.page;
    const roadmapAssignments = roadmapAssignmentsQuery.data ?? [];

    const memberAvatarStorageKeys = members
        .map((member) => member.user.avatarStorageKey)
        .filter((key): key is string => Boolean(key));
    const { data: memberAvatarUrlByStorageKey } = useFilePreviewUrls(memberAvatarStorageKeys);

    const leadAvatarStorageKeys = group.leads
        .map((lead) => lead.avatarStorageKey)
        .filter((key): key is string => Boolean(key));
    const { data: leadAvatarUrlByStorageKey } = useFilePreviewUrls(leadAvatarStorageKeys);

    useEffect(() => {
        if (!isMoreMenuOpen) {
            return undefined;
        }
        const handleOutsideClick = (event: MouseEvent) => {
            if (moreMenuRef.current && !moreMenuRef.current.contains(event.target as Node)) {
                setIsMoreMenuOpen(false);
            }
        };
        document.addEventListener("mousedown", handleOutsideClick);
        return () => document.removeEventListener("mousedown", handleOutsideClick);
    }, [isMoreMenuOpen]);

    const startEditingName = () => {
        setNameDraft(group.name);
        setDescriptionDraft(group.description ?? "");
        setIsEditingName(true);
    };

    const cancelEditingName = () => {
        setIsEditingName(false);
    };

    const saveName = async () => {
        const trimmed = nameDraft.trim();
        if (trimmed.length < 3 || trimmed.length > 100) {
            onToast("Team name must be 3-100 characters.", "warning");
            return;
        }
        try {
            setPendingAction(true);
            await updateGroupMutation.mutateAsync({
                groupId: group.id,
                name: trimmed,
                description: descriptionDraft.trim(),
            });
            onToast("Team renamed.");
            setIsEditingName(false);
        } catch (error) {
            onToast(getApiErrorMessage(error, "Failed to rename team."), "error");
        } finally {
            setPendingAction(false);
        }
    };

    const addMember = async () => {
        if (!selectedMember) {
            onToast("Choose a user from the list first.", "warning");
            return;
        }
        try {
            setPendingAction(true);
            await addMemberMutation.mutateAsync({ groupId: group.id, memberUserId: selectedMember.id });
            setSelectedMember(null);
            setMemberPickerResetKey((prev) => prev + 1);
            onToast("Team member added.");
        } catch (error) {
            onToast(getApiErrorMessage(error, "Failed to add team member."), "error");
        } finally {
            setPendingAction(false);
        }
    };

    const removeMember = async (memberUserId: number) => {
        try {
            setPendingAction(true);
            await removeMemberMutation.mutateAsync({ groupId: group.id, memberUserId });
            onToast("Team member removed.");
        } catch (error) {
            onToast(getApiErrorMessage(error, "Failed to remove team member."), "error");
        } finally {
            setPendingAction(false);
        }
    };

    const addLead = async () => {
        if (!selectedLead) {
            onToast("Choose a user from the list first.", "warning");
            return;
        }
        try {
            setPendingAction(true);
            await addLeadMutation.mutateAsync({ groupId: group.id, leadUserId: selectedLead.id });
            setSelectedLead(null);
            setLeadPickerResetKey((prev) => prev + 1);
            onToast("Team lead added.");
        } catch (error) {
            onToast(getApiErrorMessage(error, "Failed to add team lead."), "error");
        } finally {
            setPendingAction(false);
        }
    };

    const removeLead = async (leadUserId: number) => {
        try {
            setPendingAction(true);
            await removeLeadMutation.mutateAsync({ groupId: group.id, leadUserId });
            onToast("Team lead removed.");
        } catch (error) {
            onToast(getApiErrorMessage(error, "Failed to remove team lead."), "error");
        } finally {
            setPendingAction(false);
        }
    };

    const handleGradeChange = async (userId: number, gradeId: number | null) => {
        try {
            await updateGradeMutation.mutateAsync({ userId, gradeId });
            onToast("Grade updated.");
        } catch (error) {
            onToast(getApiErrorMessage(error, "Failed to update grade."), "error");
        }
    };

    return (
        <div className="group-detail">
            <div className="group-detail__hero">
                <div className="group-detail__hero-left">
                    <span className={`group-detail__avatar group-detail__avatar--${group.id % 7}`} aria-hidden="true">
                        {group.name.charAt(0).toUpperCase()}
                    </span>
                    <div className="group-detail__hero-copy">
                        {isEditingName ? (
                            <input
                                type="text"
                                className="group-detail__title-input"
                                value={nameDraft}
                                autoFocus
                                disabled={pendingAction}
                                onChange={(event) => setNameDraft(event.target.value)}
                                onKeyDown={(event) => {
                                    if (event.key === "Enter") {
                                        void saveName();
                                    } else if (event.key === "Escape") {
                                        cancelEditingName();
                                    }
                                }}
                            />
                        ) : (
                            <h2 className="group-detail__title">{group.name}</h2>
                        )}
                        <p className="group-detail__subtitle">
                            {leadSummary(group.leads)} · {group.membersCount} member{group.membersCount === 1 ? "" : "s"}
                        </p>
                    </div>
                </div>
                <div className="group-detail__hero-actions">
                    {isEditingName ? (
                        <>
                            <Button variant="ghost" size="sm" onClick={cancelEditingName} disabled={pendingAction}>
                                Cancel
                            </Button>
                            <Button size="sm" onClick={() => void saveName()} disabled={pendingAction}>
                                {pendingAction ? "Saving…" : "Save"}
                            </Button>
                        </>
                    ) : (
                        <>
                            {canManageGroup && (
                                <Button variant="ghost" size="sm" onClick={startEditingName}>
                                    <EditOutlinedIcon /> Rename
                                </Button>
                            )}
                            {canManageStructure && (
                                <div className="group-detail__more" ref={moreMenuRef}>
                                    <Button
                                        variant="ghost"
                                        size="sm"
                                        onClick={() => setIsMoreMenuOpen((prev) => !prev)}
                                        aria-haspopup="true"
                                        aria-expanded={isMoreMenuOpen}
                                    >
                                        <MoreHorizOutlinedIcon /> More actions
                                    </Button>
                                    {isMoreMenuOpen && (
                                        <div className="group-detail__more-menu" role="menu">
                                            <button
                                                type="button"
                                                role="menuitem"
                                                className="group-detail__more-item group-detail__more-item--danger"
                                                onClick={() => {
                                                    setIsMoreMenuOpen(false);
                                                    onDelete();
                                                }}
                                            >
                                                <DeleteOutlineOutlinedIcon /> Delete team
                                            </button>
                                        </div>
                                    )}
                                </div>
                            )}
                        </>
                    )}
                </div>
            </div>

            {isEditingName ? (
                <textarea
                    className="group-detail__description-input"
                    value={descriptionDraft}
                    placeholder="Description (optional)"
                    rows={2}
                    disabled={pendingAction}
                    onChange={(event) => setDescriptionDraft(event.target.value)}
                />
            ) : (
                group.description && <p className="group-detail__description">{group.description}</p>
            )}

            <div className="group-detail__tabs" role="tablist">
                {(
                    [
                        ["members", "Members"],
                        ["leads", "Leads"],
                        ["roadmaps", "Roadmaps"],
                    ] as const
                ).map(([tab, label]) => (
                    <button
                        key={tab}
                        type="button"
                        role="tab"
                        aria-selected={activeTab === tab}
                        className={`group-detail__tab${activeTab === tab ? " group-detail__tab--active" : ""}`}
                        onClick={() => setActiveTab(tab)}
                    >
                        {label}
                    </button>
                ))}
            </div>

            <div className="group-detail__body">
                {activeTab === "members" && (
                    <section className="group-detail__section">
                        <div className="group-detail__section-head">
                            <h3 className="group-detail__section-title">Members</h3>
                            {group.membersCount > GROUP_MEMBERS_SEARCH_THRESHOLD && (
                                <input
                                    type="search"
                                    className="group-member-search"
                                    value={memberSearch}
                                    placeholder="Search members…"
                                    onChange={(event) => {
                                        setMemberSearch(event.target.value);
                                        setMemberPage(0);
                                    }}
                                />
                            )}
                        </div>

                        {membersQuery.isLoading ? (
                            <LoadingSpinner label="Loading members" />
                        ) : members.length === 0 ? (
                            <p className="group-detail__empty">
                                {debouncedMemberSearch.trim()
                                    ? "No members match this search."
                                    : "This team does not have members yet."}
                            </p>
                        ) : (
                            <div className="group-person-list">
                                {members.map((member) => (
                                    <div key={member.user.id} className="group-person-row">
                                        <UserAvatar
                                            user={member.user}
                                            previewUrl={
                                                member.user.avatarStorageKey
                                                    ? memberAvatarUrlByStorageKey?.[member.user.avatarStorageKey]
                                                    : undefined
                                            }
                                            size={42}
                                        />
                                        <div className="group-person-row__copy">
                                            <span className="group-person-row__name">{member.user.name}</span>
                                            <span className="group-person-row__email">{member.user.email}</span>
                                        </div>
                                        <InlineGradeSelect
                                            userId={member.user.id}
                                            gradeId={member.user.gradeId}
                                            gradeName={member.user.gradeName}
                                            grades={grades}
                                            canEdit={canManageGroup}
                                            onChange={handleGradeChange}
                                        />
                                        {canManageStructure && (
                                            <button
                                                type="button"
                                                className="group-person-row__remove"
                                                aria-label={`Remove ${member.user.name} from team`}
                                                disabled={pendingAction}
                                                onClick={() => void removeMember(member.user.id)}
                                            >
                                                <DeleteOutlineOutlinedIcon />
                                            </button>
                                        )}
                                    </div>
                                ))}
                            </div>
                        )}

                        {memberPageInfo && memberPageInfo.totalPages > 1 && (
                            <div className="group-pagination">
                                <button
                                    type="button"
                                    className="group-pagination__button"
                                    disabled={!memberPageInfo.hasPrevious}
                                    onClick={() => setMemberPage((prev) => Math.max(0, prev - 1))}
                                >
                                    <ChevronLeftOutlinedIcon /> Prev
                                </button>
                                <span className="group-pagination__label">
                                    Page {memberPageInfo.page + 1} of {memberPageInfo.totalPages}
                                </span>
                                <button
                                    type="button"
                                    className="group-pagination__button"
                                    disabled={!memberPageInfo.hasNext}
                                    onClick={() => setMemberPage((prev) => prev + 1)}
                                >
                                    Next <ChevronRightOutlinedIcon />
                                </button>
                            </div>
                        )}

                        {canManageStructure && (
                            <div className="group-add-form">
                                <UserSearchCombobox
                                    key={memberPickerResetKey}
                                    groupId={group.id}
                                    forLeads={false}
                                    label="Add member"
                                    placeholder="Search name or email…"
                                    disabled={pendingAction}
                                    onSelect={setSelectedMember}
                                />
                                <button
                                    type="button"
                                    className="group-add-form__submit"
                                    disabled={pendingAction || !selectedMember}
                                    onClick={() => void addMember()}
                                >
                                    <AddOutlinedIcon /> Add
                                </button>
                            </div>
                        )}
                    </section>
                )}

                {activeTab === "leads" && (
                    <section className="group-detail__section">
                        <div className="group-detail__section-head">
                            <h3 className="group-detail__section-title">Leads</h3>
                        </div>

                        <div className="group-person-list">
                            {group.leads.map((lead) => (
                                <div key={lead.id} className="group-person-row">
                                    <UserAvatar
                                        user={lead}
                                        previewUrl={
                                            lead.avatarStorageKey
                                                ? leadAvatarUrlByStorageKey?.[lead.avatarStorageKey]
                                                : undefined
                                        }
                                        size={42}
                                    />
                                    <div className="group-person-row__copy">
                                        <span className="group-person-row__name">{lead.name}</span>
                                        <span className="group-person-row__email">{lead.email}</span>
                                    </div>
                                    {canManageStructure && (
                                        <button
                                            type="button"
                                            className="group-person-row__remove"
                                            aria-label={`Remove ${lead.name} as lead`}
                                            disabled={pendingAction}
                                            onClick={() => void removeLead(lead.id)}
                                        >
                                            <DeleteOutlineOutlinedIcon />
                                        </button>
                                    )}
                                </div>
                            ))}
                        </div>

                        {canManageStructure && (
                            <div className="group-add-form">
                                <UserSearchCombobox
                                    key={leadPickerResetKey}
                                    groupId={group.id}
                                    forLeads
                                    label="Add lead"
                                    placeholder="Search name or email…"
                                    disabled={pendingAction}
                                    onSelect={setSelectedLead}
                                />
                                <button
                                    type="button"
                                    className="group-add-form__submit"
                                    disabled={pendingAction || !selectedLead}
                                    onClick={() => void addLead()}
                                >
                                    <AddOutlinedIcon /> Add lead
                                </button>
                            </div>
                        )}
                    </section>
                )}

                {activeTab === "roadmaps" && (
                    <section className="group-detail__section">
                        {roadmapAssignmentsQuery.isLoading ? (
                            <LoadingSpinner label="Loading roadmap assignments" size="sm" />
                        ) : roadmapAssignments.length === 0 ? (
                            <p className="group-detail__empty">
                                No roadmaps assigned yet. Assign roadmaps from Library → Roadmaps using
                                “Assign to teams”.
                            </p>
                        ) : (
                            <div className="group-roadmap-list">
                                {roadmapAssignments.map((assignment) => (
                                    <div key={assignment.id} className="group-roadmap-row">
                                        <div className="group-roadmap-row__copy">
                                            <p className="group-roadmap-row__title">{assignment.roadmapTitle}</p>
                                            <p className="group-roadmap-row__meta">
                                                {assignment.gradeFilters.length === 0
                                                    ? "All members"
                                                    : `Grades: ${assignment.gradeFilters
                                                          .map((grade) => grade.name)
                                                          .join(", ")}`}
                                            </p>
                                        </div>
                                        <span className="group-roadmap-row__count">
                                            {assignment.membersMatchedCount} member
                                            {assignment.membersMatchedCount === 1 ? "" : "s"}
                                        </span>
                                    </div>
                                ))}
                            </div>
                        )}
                    </section>
                )}
            </div>
        </div>
    );
}
