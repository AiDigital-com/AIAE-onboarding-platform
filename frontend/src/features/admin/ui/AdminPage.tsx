import { useEffect, useState } from "react";
import { Navigate } from "react-router-dom";
import ChevronLeftOutlinedIcon from "@mui/icons-material/ChevronLeftOutlined";
import ChevronRightOutlinedIcon from "@mui/icons-material/ChevronRightOutlined";
import SearchOutlinedIcon from "@mui/icons-material/SearchOutlined";
import SettingsOutlinedIcon from "@mui/icons-material/SettingsOutlined";
import { usePermissionManagementQuery } from "@/shared/auth/usePermissionManagementQuery";
import { useHasPermission } from "@/shared/auth/useHasPermission";
import { useCurrentUser } from "@/shared/auth/useCurrentUser";
import { useDebounce } from "@/shared/hooks/useDebounce";
import { ErrorAlert } from "@/shared/ui/ErrorAlert";
import { LoadingBlock } from "@/shared/ui/LoadingBlock";
import { FeatureEmptyState } from "@/shared/ui/FeatureEmptyState";
import { Button } from "@/shared/ui/Button";
import { Toast, ToastSeverity } from "@/shared/ui/Toast";
import { UserAvatar } from "@/shared/ui/UserAvatar";
import { useFilePreviewUrls } from "@/shared/api/files";
import { getApiErrorMessage } from "@/shared/lib/apiError";
import { PermissionMatrixDrawer } from "@/features/permissions";
import { useAdminUserStatsQuery, useAdminUsersQuery } from "../api/useAdminUsersQuery";
import { useAssignUserRoleMutation } from "../api/useAssignUserRoleMutation";
import "@/shared/ui/select.css";
import "./admin.css";

type RoleCode = "admin" | "teamlead" | "member";

const ADMIN_ONLY_PERMISSION_KEYS = ["admin.manage_roles", "permissions.manage_teamleads"];

const ROLE_OPTIONS: Array<{ value: RoleCode; label: string }> = [
    { value: "admin", label: "Admin" },
    { value: "teamlead", label: "Team Lead" },
    { value: "member", label: "User" },
];

type RoleFilter = "all" | RoleCode;

const ROLE_FILTERS: Array<{ value: RoleFilter; label: string }> = [
    { value: "all", label: "All" },
    { value: "admin", label: "Admins" },
    { value: "teamlead", label: "Leads" },
    { value: "member", label: "Users" },
];

function roleLabel(role: RoleCode): string {
    return ROLE_OPTIONS.find((option) => option.value === role)?.label ?? role;
}

/** Admin tools: assign user roles and review/edit Team Lead permission overrides. */
export function AdminPage() {
    const hasPermission = useHasPermission();
    const canAccessAdmin = hasPermission("admin.manage_roles");
    const { user: currentUser } = useCurrentUser();

    const [searchQuery, setSearchQuery] = useState("");
    const debouncedSearchQuery = useDebounce(searchQuery, 300);
    const [roleFilter, setRoleFilter] = useState<RoleFilter>("all");
    const [page, setPage] = useState(0);

    const usersQuery = useAdminUsersQuery(
        debouncedSearchQuery.trim() || undefined,
        roleFilter === "all" ? undefined : roleFilter,
        page,
        canAccessAdmin,
    );
    const statsQuery = useAdminUserStatsQuery(canAccessAdmin);
    const { data: managementData } = usePermissionManagementQuery(canAccessAdmin);
    const assignRoleMutation = useAssignUserRoleMutation();

    const [selectedUserId, setSelectedUserId] = useState<number | null>(null);
    const [pendingUserId, setPendingUserId] = useState<number | null>(null);
    const [isPermissionsDrawerOpen, setIsPermissionsDrawerOpen] = useState(false);
    const [toast, setToast] = useState<{ open: boolean; message: string; severity: ToastSeverity }>({
        open: false,
        message: "",
        severity: "success",
    });

    const showToast = (message: string, severity: ToastSeverity = "success") => {
        setToast({ open: true, message, severity });
    };

    const pageUsers = usersQuery.data?.items ?? [];
    const pageInfo = usersQuery.data?.page;
    const stats = statsQuery.data;
    const avatarStorageKeys = pageUsers
        .map((user) => user.avatarStorageKey)
        .filter((key): key is string => Boolean(key));
    const { data: avatarUrlByStorageKey } = useFilePreviewUrls(avatarStorageKeys);

    const handleSearchChange = (value: string) => {
        setSearchQuery(value);
        setPage(0);
    };

    const handleRoleFilterChange = (value: RoleFilter) => {
        setRoleFilter(value);
        setPage(0);
    };

    useEffect(() => {
        if (pageUsers.length === 0) {
            if (selectedUserId !== null) {
                setSelectedUserId(null);
            }
            return;
        }
        if (selectedUserId === null || !pageUsers.some((user) => user.id === selectedUserId)) {
            setSelectedUserId(pageUsers[0].id);
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [pageUsers]);

    const selectedUser = pageUsers.find((user) => user.id === selectedUserId) ?? null;
    const selectedPermissionState = selectedUser
        ? managementData?.permissionsByUserId[String(selectedUser.id)]
        : undefined;
    const permissionDefinitions = managementData?.permissionDefinitions ?? [];

    if (!canAccessAdmin) {
        return <Navigate to="/teams" replace />;
    }

    if (usersQuery.isLoading) {
        return <LoadingBlock label="Loading admin tools…" />;
    }

    if (usersQuery.error) {
        return <ErrorAlert message={getApiErrorMessage(usersQuery.error, "Failed to load admin data.")} />;
    }

    const changeRole = async (userId: number, roleCode: RoleCode) => {
        try {
            setPendingUserId(userId);
            await assignRoleMutation.mutateAsync({ userId, roleCode });
            showToast("Role updated.");
        } catch (roleError) {
            showToast(getApiErrorMessage(roleError, "Failed to update role."), "error");
        } finally {
            setPendingUserId(null);
        }
    };

    const permissionGroupCounts = (permissionState: NonNullable<typeof selectedPermissionState>) => {
        const enabledByGroup = new Map<string, number>();
        const totalByGroup = new Map<string, number>();
        permissionDefinitions.forEach((definition) => {
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
                    <h1 className="admin-page__title">Admin</h1>
                    <p className="admin-page__subtitle">
                        Assign user roles and review Team Lead permissions. Manage teams and grades from the Teams tab.
                    </p>
                </div>
            </div>

            {stats && (
                <div className="admin-page__stats">
                    <div className="admin-stat-card">
                        <p className="admin-stat-card__value">{stats.totalUsers}</p>
                        <p className="admin-stat-card__label">Users</p>
                    </div>
                    <div className="admin-stat-card">
                        <p className="admin-stat-card__value">{stats.totalAdmins}</p>
                        <p className="admin-stat-card__label">Admins</p>
                    </div>
                    <div className="admin-stat-card">
                        <p className="admin-stat-card__value">{stats.totalTeamLeads}</p>
                        <p className="admin-stat-card__label">Team lead{stats.totalTeamLeads === 1 ? "" : "s"}</p>
                    </div>
                    <div className="admin-stat-card">
                        <p className="admin-stat-card__value">{stats.totalPermissionsEnabled}</p>
                        <p className="admin-stat-card__label">Permissions enabled</p>
                    </div>
                </div>
            )}

            <div className="admin-page__workspace">
                <div className="admin-list-panel">
                    <div className="admin-list-panel__head">
                        <div className="admin-list-panel__search">
                            <SearchOutlinedIcon aria-hidden="true" />
                            <input
                                type="search"
                                value={searchQuery}
                                placeholder="Search user by name or email…"
                                onChange={(event) => handleSearchChange(event.target.value)}
                            />
                        </div>
                        <div className="admin-list-panel__filters" role="tablist">
                            {ROLE_FILTERS.map((filterOption) => (
                                <button
                                    key={filterOption.value}
                                    type="button"
                                    role="tab"
                                    aria-selected={roleFilter === filterOption.value}
                                    className={`admin-list-panel__filter${
                                        roleFilter === filterOption.value ? " admin-list-panel__filter--active" : ""
                                    }`}
                                    onClick={() => handleRoleFilterChange(filterOption.value)}
                                >
                                    {filterOption.label}
                                </button>
                            ))}
                        </div>
                    </div>

                    <div className="admin-list-panel__list">
                        {pageUsers.length === 0 ? (
                            <FeatureEmptyState
                                title="No users match"
                                description="Try a different name, email, or role filter."
                            />
                        ) : (
                            pageUsers.map((user) => {
                                const isSelf = user.id === currentUser?.id;
                                const isPending = pendingUserId === user.id;

                                return (
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
                                            {isSelf && <span className="admin-user-row__you">You</span>}
                                        </div>
                                        <select
                                            className="ui-select admin-role-select"
                                            value={user.role}
                                            disabled={isSelf || isPending}
                                            title={isSelf ? "You cannot change your own role." : undefined}
                                            onClick={(event) => event.stopPropagation()}
                                            onChange={(event) => {
                                                setSelectedUserId(user.id);
                                                void changeRole(user.id, event.target.value as RoleCode);
                                            }}
                                        >
                                            {ROLE_OPTIONS.map((option) => (
                                                <option key={option.value} value={option.value}>
                                                    {option.label}
                                                </option>
                                            ))}
                                        </select>
                                        {user.role === "teamlead" ? (
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
                                        ) : (
                                            <span className="admin-user-row__permissions-spacer" aria-hidden="true" />
                                        )}
                                    </div>
                                );
                            })
                        )}
                    </div>

                    {pageInfo && pageInfo.totalPages > 1 && (
                        <div className="admin-pagination">
                            <button
                                type="button"
                                className="admin-pagination__button"
                                disabled={!pageInfo.hasPrevious}
                                onClick={() => setPage((prev) => Math.max(0, prev - 1))}
                            >
                                <ChevronLeftOutlinedIcon /> Prev
                            </button>
                            <span className="admin-pagination__label">
                                Page {pageInfo.page + 1} of {pageInfo.totalPages}
                            </span>
                            <button
                                type="button"
                                className="admin-pagination__button"
                                disabled={!pageInfo.hasNext}
                                onClick={() => setPage((prev) => prev + 1)}
                            >
                                Next <ChevronRightOutlinedIcon />
                            </button>
                        </div>
                    )}
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

                            {selectedUser.role === "teamlead" ? (
                                <Button block onClick={() => setIsPermissionsDrawerOpen(true)}>
                                    <SettingsOutlinedIcon /> Edit permissions
                                </Button>
                            ) : (
                                <p className="admin-detail__permissions-hint">
                                    Permission overrides are available only for Team Leads.
                                </p>
                            )}

                            <div className="admin-detail__section">
                                <h3 className="admin-detail__section-title">Role details</h3>
                                <div className="admin-detail__info">
                                    <div className="admin-detail__kv">
                                        <span className="admin-detail__kv-key">Role</span>
                                        <span className="admin-detail__kv-value">{roleLabel(selectedUser.role)}</span>
                                    </div>
                                </div>
                            </div>

                            {selectedUser.role === "teamlead" && selectedPermissionState && (
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
                            title="No user selected"
                            description="Choose a user from the list to see their role and permissions."
                        />
                    )}
                </div>
            </div>

            {selectedUser && selectedUser.role === "teamlead" && selectedPermissionState && (
                <PermissionMatrixDrawer
                    open={isPermissionsDrawerOpen}
                    onClose={() => setIsPermissionsDrawerOpen(false)}
                    user={selectedUser}
                    permissionState={selectedPermissionState}
                    disabledKeys={ADMIN_ONLY_PERMISSION_KEYS}
                    definitions={permissionDefinitions}
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
