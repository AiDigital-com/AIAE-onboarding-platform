import { useState } from "react";
import { UserButton } from "@clerk/clerk-react";
import { NavLink } from "react-router-dom";
import { useCurrentUser } from "@/shared/auth/useCurrentUser";
import { useHasPermission } from "@/shared/auth/useHasPermission";
import { ProfileDialog } from "@/features/profile";
import "@/features/profile/ui/profile.css";
import "./app-shell.css";

const NAV_ITEMS = [
    { label: "Library", to: "/library" },
    { label: "My Lessons", to: "/lessons" },
    { label: "My Roadmaps", to: "/roadmaps" },
    { label: "Team progress", to: "/team-progress", teamLeadOnly: true },
    { label: "Teams", to: "/teams" },
] as const;

const ADMIN_NAV_ITEM = { label: "Admin", to: "/admin" } as const;

/** Top app header — Elevate layout (no left sidebar). */
export function AppHeader() {
    const { user } = useCurrentUser();
    const hasPermission = useHasPermission();
    const [profileOpen, setProfileOpen] = useState(false);
    const role = user?.role;

    const visibleNavItems = [
        ...NAV_ITEMS.filter(
            (item) =>
                !("teamLeadOnly" in item && item.teamLeadOnly) ||
                role === "teamlead" ||
                role === "admin",
        ),
        ...(hasPermission("admin.manage_roles") ? [ADMIN_NAV_ITEM] : []),
    ];

    return (
        <>
            <header className="app-header">
                <div className="app-header__start">
                    <NavLink className="app-header__brand" to="/">
                        <img
                            className="app-header__logo"
                            src="/aidlogo.png"
                            alt="AI Digital logo"
                            width={40}
                            height={40}
                        />
                        <span className="app-header__brand-text">
                            <span className="app-header__brand-title">AI Digital</span>
                            <span className="app-header__brand-subtitle">Learning Hub</span>
                        </span>
                    </NavLink>

                    <nav className="app-header__nav" aria-label="Main navigation">
                        {visibleNavItems.map((item) => (
                            <NavLink
                                key={item.to}
                                className={({ isActive }) =>
                                    [
                                        "app-header__nav-link",
                                        isActive ? "app-header__nav-link--active" : "",
                                    ]
                                        .filter(Boolean)
                                        .join(" ")
                                }
                                to={item.to}
                            >
                                {item.label}
                            </NavLink>
                        ))}
                    </nav>
                </div>

                <div className="app-header__actions">
                    {user && (
                        <button
                            type="button"
                            className="profile-header-btn"
                            onClick={() => setProfileOpen(true)}
                        >
                            Profile
                        </button>
                    )}
                    <UserButton afterSignOutUrl="/login" />
                </div>
            </header>

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
