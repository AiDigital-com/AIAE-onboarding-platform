import { useState } from "react";
import { NavLink } from "react-router-dom";
import { useClerk } from "@clerk/clerk-react";
import AdminPanelSettingsOutlinedIcon from "@mui/icons-material/AdminPanelSettingsOutlined";
import GroupsOutlinedIcon from "@mui/icons-material/GroupsOutlined";
import ManageAccountsOutlinedIcon from "@mui/icons-material/ManageAccountsOutlined";
import LibraryBooksOutlinedIcon from "@mui/icons-material/LibraryBooksOutlined";
import LogoutOutlinedIcon from "@mui/icons-material/LogoutOutlined";
import QueryStatsOutlinedIcon from "@mui/icons-material/QueryStatsOutlined";
import RouteOutlinedIcon from "@mui/icons-material/RouteOutlined";
import SchoolOutlinedIcon from "@mui/icons-material/SchoolOutlined";
import { useCurrentUser } from "@/shared/auth/useCurrentUser";
import { useHasPermission } from "@/shared/auth/useHasPermission";
import { ProfileDialog } from "@/features/profile";
import { UserAvatar } from "./UserAvatar";
import "./sidebar.css";

interface NavItem {
    label: string;
    to: string;
    icon: React.ReactNode;
    teamLeadOnly?: boolean;
}

const NAV_ITEMS: NavItem[] = [
    { label: "Library",       to: "/library",       icon: <LibraryBooksOutlinedIcon /> },
    { label: "My Lessons",    to: "/lessons",        icon: <SchoolOutlinedIcon /> },
    { label: "My Roadmaps",   to: "/roadmaps",       icon: <RouteOutlinedIcon /> },
    { label: "Team progress", to: "/team-progress",  icon: <QueryStatsOutlinedIcon />, teamLeadOnly: true },
    { label: "Teams",         to: "/teams",          icon: <GroupsOutlinedIcon />, teamLeadOnly: true },
];

const ADMIN_ITEM: NavItem = {
    label: "Admin",
    to: "/admin",
    icon: <AdminPanelSettingsOutlinedIcon />,
};

const TEAM_PERMISSIONS_ITEM: NavItem = {
    label: "Team Permissions",
    to: "/team-permissions",
    icon: <ManageAccountsOutlinedIcon />,
};

/** Collapsible left sidebar — collapsed 84 px, expands to 280 px on hover. */
export function Sidebar() {
    const { signOut } = useClerk();
    const { user } = useCurrentUser();
    const hasPermission = useHasPermission();
    const [profileOpen, setProfileOpen] = useState(false);

    const role = user?.role;
    const canAccessAdmin = hasPermission("admin.manage_roles");
    const visibleItems = [
        ...NAV_ITEMS.filter(
            (item) =>
                !item.teamLeadOnly ||
                role === "teamlead" ||
                role === "admin",
        ),
        ...(canAccessAdmin ? [ADMIN_ITEM] : []),
        // Team Leads who can only manage their own team's member permissions (not full Admin
        // access) get a scoped entry point instead — Admin itself already covers this for admins.
        ...(!canAccessAdmin && hasPermission("permissions.manage_team_members") ? [TEAM_PERMISSIONS_ITEM] : []),
    ];

    return (
        <>
            <aside className="sidebar">
                {/* ── Header / logo ── */}
                <div className="sidebar__header">
                    <NavLink to="/" className="sidebar__logo-link" title="Home">
                        <img
                            className="sidebar__logo"
                            src="/aidlogo.png"
                            alt="AI Onboarding Logo"
                            width={40}
                            height={40}
                        />
                    </NavLink>
                    <div className="sidebar__brand sidebar-text">
                        <span className="sidebar__brand-title">AI Digital</span>
                        <span className="sidebar__brand-subtitle">Learning Hub</span>
                    </div>
                </div>

                <div className="sidebar__divider" />

                {/* ── Navigation ── */}
                <nav className="sidebar__nav" aria-label="Main navigation">
                    {visibleItems.map((item) => (
                        <NavLink
                            key={item.to}
                            to={item.to}
                            title={item.label}
                            className={({ isActive }) =>
                                ["sidebar__item", isActive ? "sidebar__item--active" : ""]
                                    .filter(Boolean)
                                    .join(" ")
                            }
                        >
                            <span className="sidebar__item-icon" aria-hidden="true">
                                {item.icon}
                            </span>
                            <span className="sidebar__item-label sidebar-text">
                                {item.label}
                            </span>
                        </NavLink>
                    ))}
                </nav>

                <div className="sidebar__spacer" />
                <div className="sidebar__divider" />

                {/* ── User area ── */}
                <button
                    type="button"
                    className="sidebar__user"
                    onClick={() => setProfileOpen(true)}
                    title={user?.name || "Profile settings"}
                >
                    <span className="sidebar__user-avatar">
                        {user && <UserAvatar user={user} />}
                    </span>
                    <span className="sidebar__user-details sidebar-user-details">
                        <span className="sidebar__user-name">{user?.name}</span>
                        <span className="sidebar__user-email">{user?.email}</span>
                        <span className="sidebar__user-role">
                            {user?.position || user?.role}
                        </span>
                    </span>
                </button>

                {/* ── Sign out ── */}
                <button
                    type="button"
                    className="sidebar__logout"
                    title="Sign out"
                    onClick={() => void signOut({ redirectUrl: "/login" })}
                >
                    <span className="sidebar__logout-icon" aria-hidden="true">
                        <LogoutOutlinedIcon />
                    </span>
                    <span className="sidebar__logout-text sidebar-logout-text">
                        Sign out
                    </span>
                </button>
            </aside>

            {user && (
                <ProfileDialog
                    open={profileOpen}
                    user={user}
                    onClose={() => setProfileOpen(false)}
                />
            )}
        </>
    );
}
