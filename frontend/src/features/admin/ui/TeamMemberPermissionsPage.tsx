import { useEffect, useState } from "react";
import { Navigate } from "react-router-dom";
import SearchOutlinedIcon from "@mui/icons-material/SearchOutlined";
import SettingsOutlinedIcon from "@mui/icons-material/SettingsOutlined";
import { usePermissionManagementQuery } from "@/shared/auth/usePermissionManagementQuery";
import { useHasPermission } from "@/shared/auth/useHasPermission";
import { useCurrentUser } from "@/shared/auth/useCurrentUser";
import { LoadingBlock } from "@/shared/ui/LoadingBlock";
import { ErrorAlert } from "@/shared/ui/ErrorAlert";
import { FeatureEmptyState } from "@/shared/ui/FeatureEmptyState";
import { Button } from "@/shared/ui/Button";
import { Toast, ToastSeverity } from "@/shared/ui/Toast";
import { UserAvatar } from "@/shared/ui/UserAvatar";
import { useFilePreviewUrls } from "@/shared/api/files";
import { getApiErrorMessage } from "@/shared/lib/apiError";
import { PermissionMatrixDrawer } from "@/features/permissions";
import "./admin.css";

const MEMBER_PERMISSION_GROUPS = ["Materials", "Lessons", "Roadmaps", "Learning"];

/**
 * Team-scoped permission editor for Team Leads holding `permissions.manage_team_members`:
 * lets them toggle Materials/Lessons/Roadmaps/Learning permissions for members of their own
 * team, without exposing role assignment or Admin/Teams-group permissions.
 */
export function TeamMemberPermissionsPage() {
    const hasPermission = useHasPermission();
    const canAccess = hasPermission("permissions.manage_team_members");
    const { user: currentUser } = useCurrentUser();
    const { data: managementData, isLoading, error } = usePermissionManagementQuery(canAccess);

    const [searchQuery, setSearchQuery] = useState("");
    const [selectedUserId, setSelectedUserId] = useState<number | null>(null);
    const [isPermissionsDrawerOpen, setIsPermissionsDrawerOpen] = useState(false);
    const [toast, setToast] = useState<{ open: boolean; message: string; severity: ToastSeverity }>({
        open: false,
        message: "",
        severity: "success",
    });

    const showToast = (message: string, severity: ToastSeverity = "success") => {
        setToast({ open: true, message, severity });
    };

    const members = (managementData?.users ?? []).filter(
        (user) => user.role === "member" && user.id !== currentUser?.id,
    );

    const normalizedSearch = searchQuery.trim().toLowerCase();
    const filteredMembers = normalizedSearch
        ? members.filter(
              (user) =>
                  user.name?.toLowerCase().includes(normalizedSearch) ||
                  user.email?.toLowerCase().includes(normalizedSearch),
          )
        : members;
    const avatarStorageKeys = filteredMembers
        .map((user) => user.avatarStorageKey)
        .filter((key): key is string => Boolean(key));
    const { data: avatarUrlByStorageKey } = useFilePreviewUrls(avatarStorageKeys);

    useEffect(() => {
        if (filteredMembers.length === 0) {
            if (selectedUserId !== null) {
                setSelectedUserId(null);
            }
            return;
        }
        if (selectedUserId === null || !filteredMembers.some((user) => user.id === selectedUserId)) {
            setSelectedUserId(filteredMembers[0].id);
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [filteredMembers]);

    const selectedUser = filteredMembers.find((user) => user.id === selectedUserId) ?? null;
    const selectedPermissionState = selectedUser
        ? managementData?.permissionsByUserId[String(selectedUser.id)]
        : undefined;
    const memberPermissionDefinitions = (managementData?.permissionDefinitions ?? []).filter((definition) =>
        MEMBER_PERMISSION_GROUPS.includes(definition.group),
    );

    if (!canAccess) {
        return <Navigate to="/teams" replace />;
    }

    if (isLoading) {
        return <LoadingBlock label="Loading team permissions…" />;
    }

    if (error) {
        return <ErrorAlert message={getApiErrorMessage(error, "Failed to load team permissions.")} />;
    }

    const permissionGroupCounts = (permissionState: NonNullable<typeof selectedPermissionState>) => {
        const enabledByGroup = new Map<string, number>();
        const totalByGroup = new Map<string, number>();
        memberPermissionDefinitions.forEach((definition) => {
            totalByGroup.set(definition.group, (totalByGroup.get(definition.group) ?? 0) + 1);
            if (permissionState.effective[definition.code]) {
                enabledByGroup.set(definition.group, (enabledByGroup.get(definition.group) ?? 0) + 1);
            }
        });
        return [...totalByGroup.entries()].map(([group, total]) => ({
            group,
            total,
            enabled: enabledByGroup.get(group) ?? 0,
        }));
    };

    return (
        <div className="admin-page">
            <div className="admin-page__head">
                <div>
                    <p className="admin-page__eyebrow">Access control</p>
                    <h1 className="admin-page__title">Team Permissions</h1>
                    <p className="admin-page__subtitle">
                        Review and edit content permissions for members of your team.
                    </p>
                </div>
            </div>

            <div className="admin-page__workspace">
                <div className="admin-list-panel">
                    <div className="admin-list-panel__head">
                        <div className="admin-list-panel__search">
                            <SearchOutlinedIcon aria-hidden="true" />
                            <input
                                type="search"
                                value={searchQuery}
                                placeholder="Search member by name or email…"
                                onChange={(event) => setSearchQuery(event.target.value)}
                            />
                        </div>
                    </div>

                    <div className="admin-list-panel__list">
                        {filteredMembers.length === 0 ? (
                            <FeatureEmptyState
                                title="No members found"
                                description="Try a different name or email, or check that your team has members assigned."
                            />
                        ) : (
                            filteredMembers.map((user) => (
                                <div
                                    key={user.id}
                                    className={`admin-user-row${user.id === selectedUserId ? " admin-user-row--active" : ""}`}
                                    onClick={() => setSelectedUserId(user.id)}
                                >
                                    <UserAvatar
                                        user={user}
                                        previewUrl={
                                            user.avatarStorageKey
                                                ? avatarUrlByStorageKey?.[user.avatarStorageKey]
                                                : undefined
                                        }
                                        size={42}
                                    />
                                    <div className="admin-user-row__copy">
                                        <span className="admin-user-row__name">{user.name}</span>
                                        <span className="admin-user-row__email">{user.email}</span>
                                    </div>
                                    <button
                                        type="button"
                                        className="admin-user-row__permissions"
                                        aria-label={`Edit permissions for ${user.name || user.email}`}
                                        onClick={(event) => {
                                            event.stopPropagation();
                                            setSelectedUserId(user.id);
                                            setIsPermissionsDrawerOpen(true);
                                        }}
                                    >
                                        <SettingsOutlinedIcon />
                                    </button>
                                </div>
                            ))
                        )}
                    </div>
                </div>

                <div className="admin-detail-panel">
                    {selectedUser ? (
                        <div className="admin-detail">
                            <div className="admin-detail__hero">
                                <UserAvatar user={selectedUser} size={56} />
                                <div className="admin-detail__hero-copy">
                                    <h2 className="admin-detail__name">{selectedUser.name}</h2>
                                    <p className="admin-detail__email">{selectedUser.email}</p>
                                </div>
                            </div>

                            <Button block onClick={() => setIsPermissionsDrawerOpen(true)}>
                                <SettingsOutlinedIcon /> Edit permissions
                            </Button>

                            {selectedPermissionState && (
                                <div className="admin-detail__section">
                                    <h3 className="admin-detail__section-title">Permission summary</h3>
                                    <div className="admin-detail__summary">
                                        {permissionGroupCounts(selectedPermissionState).map(({ group, enabled, total }) => (
                                            <div key={group} className="admin-detail__summary-row">
                                                <b>{group}</b>
                                                <span
                                                    className={`admin-role-pill${
                                                        enabled > 0 ? " admin-role-pill--enabled" : ""
                                                    }`}
                                                >
                                                    {enabled}/{total} enabled
                                                </span>
                                            </div>
                                        ))}
                                    </div>
                                </div>
                            )}
                        </div>
                    ) : (
                        <FeatureEmptyState
                            title="No member selected"
                            description="Choose a member from the list to see their permissions."
                        />
                    )}
                </div>
            </div>

            {selectedUser && selectedPermissionState && (
                <PermissionMatrixDrawer
                    open={isPermissionsDrawerOpen}
                    onClose={() => setIsPermissionsDrawerOpen(false)}
                    user={selectedUser}
                    permissionState={selectedPermissionState}
                    allowedGroups={MEMBER_PERMISSION_GROUPS}
                    presets={[]}
                    title="Member permissions"
                    description={`${selectedUser.name || selectedUser.email} · choose which content permissions this member has.`}
                    footerNote="Changes affect only this team member."
                    definitions={managementData?.permissionDefinitions ?? []}
                    onToast={showToast}
                />
            )}

            <Toast
                open={toast.open}
                message={toast.message}
                severity={toast.severity}
                onClose={() => setToast((prev) => ({ ...prev, open: false }))}
            />
        </div>
    );
}
