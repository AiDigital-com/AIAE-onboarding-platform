import type { ReactNode } from "react";
import { MemoryRouter } from "react-router-dom";

import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { cleanup, fireEvent, render, screen, within } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { useCurrentUser } from "@/shared/auth/useCurrentUser";
import { useTeamDashboardQuery } from "../api/useTeamDashboardQuery";
import { TEAM_SCOPE, TEAM_SCOPE_LABEL } from "../constants/teamScope";
import type { TeamDashboardMemberV1, TeamDashboardV1 } from "../model/teamDashboard";
import { TeamProgressDashboard } from "./TeamProgressDashboard";
import { buildScopeOptions, TeamScopePicker } from "./TeamScopePicker";

vi.mock("@/shared/auth/useCurrentUser", () => ({ useCurrentUser: vi.fn() }));
vi.mock("../api/useTeamDashboardQuery", () => ({ useTeamDashboardQuery: vi.fn() }));
vi.mock("@/shared/api/files", () => ({
    useFilePreviewUrls: () => ({ data: undefined }),
    useFilePreviewUrl: () => ({ data: undefined }),
}));

function member(overrides: Partial<TeamDashboardMemberV1> = {}): TeamDashboardMemberV1 {
    return {
        id: "member-1",
        name: "Ann Smith",
        email: "ann.smith@example.com",
        role: "Designer",
        isTeamLead: false,
        roadmap: "Onboarding",
        roadmapId: "roadmap-1",
        roadmapCount: 1,
        roadmaps: [{ id: "roadmap-1", title: "Onboarding" }],
        roadmapLessonCount: 4,
        roadmapCompletedCount: 2,
        progress: 50,
        completedInPeriod: 1,
        completedByPeriod: { week: 1, month: 1, quarter: 1 },
        openAssignments: 2,
        quiz: 80,
        quizByPeriod: { week: 80, month: 80, quarter: 80 },
        status: "in-progress",
        lastActive: "Today",
        avatarBg: "#fff",
        avatarStorageKey: "",
        avatarColor: "",
        ...overrides,
    };
}

function ann() {
    return member({ id: "1", name: "Ann Smith", email: "ann.smith@example.com", role: "Designer" });
}

function bob() {
    return member({ id: "2", name: "Bob Jones", email: "bob.jones@example.com", role: "Engineer" });
}

function dashboard(members: TeamDashboardMemberV1[]): TeamDashboardV1 {
    return {
        teamName: "Design",
        refreshedAt: "2026-11-02T00:00:00Z",
        members,
        roadmaps: [],
        weekly: [],
        lowConfidenceLessons: [],
        recentActivity: [],
        individualRoadmapsByMemberId: {},
        kpis: { activeRoadmaps: 1, lessonsCompleted: 2, avgQuizScore: 80 },
    };
}

function renderDashboard(members: TeamDashboardMemberV1[]) {
    vi.mocked(useCurrentUser).mockReturnValue({
        user: { role: "teamlead" },
    } as unknown as ReturnType<typeof useCurrentUser>);
    vi.mocked(useTeamDashboardQuery).mockReturnValue({
        data: dashboard(members),
        isLoading: false,
        error: null,
    } as unknown as ReturnType<typeof useTeamDashboardQuery>);

    const queryClient = new QueryClient();
    function Wrapper({ children }: { children: ReactNode }) {
        return (
            <QueryClientProvider client={queryClient}>
                <MemoryRouter>{children}</MemoryRouter>
            </QueryClientProvider>
        );
    }
    return render(<TeamProgressDashboard />, { wrapper: Wrapper });
}

function combobox() {
    return screen.getByRole("combobox", { name: "Team member" }) as HTMLInputElement;
}

function listbox() {
    return within(screen.getByRole("listbox", { name: "Team members" }));
}

afterEach(() => {
    cleanup();
    vi.clearAllMocks();
});

describe("buildScopeOptions", () => {
    it("should keep the whole-team option first regardless of the query test", () => {
        // When:
        const options = buildScopeOptions([ann(), bob()], "bob");

        // Then:
        expect(options.map((option) => option.id)).toEqual([TEAM_SCOPE, "2"]);
        expect(options[0].label).toBe(TEAM_SCOPE_LABEL);
        expect(options[1].hint).toBe("Engineer · bob.jones@example.com");
    });
});

describe("TeamScopePicker", () => {
    it("should show the current scope label while closed test", () => {
        // Given:
        render(<TeamScopePicker members={[ann(), bob()]} scope="2" icon={null} onScopeChange={vi.fn()} />);

        // Then:
        expect(combobox().value).toBe("Bob Jones");
        expect(combobox().getAttribute("aria-expanded")).toBe("false");
        expect(screen.queryByRole("listbox")).toBeNull();
    });

    it("should narrow the options as the user types test", () => {
        // Given:
        render(<TeamScopePicker members={[ann(), bob()]} scope={TEAM_SCOPE} icon={null} onScopeChange={vi.fn()} />);

        // When:
        fireEvent.focus(combobox());
        fireEvent.change(combobox(), { target: { value: "engineer" } });

        // Then:
        const names = listbox()
            .getAllByRole("option")
            .map((option) => option.textContent);
        expect(names[0]).toContain(TEAM_SCOPE_LABEL);
        expect(names.some((text) => text?.includes("Bob Jones"))).toBe(true);
        expect(names.some((text) => text?.includes("Ann Smith"))).toBe(false);
    });

    it("should report when nothing matches but keep the whole-team option test", () => {
        // Given:
        render(<TeamScopePicker members={[ann(), bob()]} scope={TEAM_SCOPE} icon={null} onScopeChange={vi.fn()} />);

        // When:
        fireEvent.focus(combobox());
        fireEvent.change(combobox(), { target: { value: "zzz" } });

        // Then:
        expect(listbox().getAllByRole("option")).toHaveLength(1);
        expect(listbox().getByText(/No members match/).textContent).toContain("zzz");
    });

    it("should pick the highlighted option with the keyboard and restore the label on escape test", () => {
        // Given:
        const onScopeChange = vi.fn();
        render(<TeamScopePicker members={[ann(), bob()]} scope={TEAM_SCOPE} icon={null} onScopeChange={onScopeChange} />);

        // When: open, move to the first member, choose it
        fireEvent.keyDown(combobox(), { key: "ArrowDown" });
        fireEvent.keyDown(combobox(), { key: "ArrowDown" });
        fireEvent.keyDown(combobox(), { key: "Enter" });

        // Then:
        expect(onScopeChange).toHaveBeenCalledWith("1");
        expect(screen.queryByRole("listbox")).toBeNull();

        // When: reopen, type, and abandon
        fireEvent.focus(combobox());
        fireEvent.change(combobox(), { target: { value: "bo" } });
        fireEvent.keyDown(combobox(), { key: "Escape" });

        // Then: the current scope label is back and nothing else was picked
        expect(combobox().value).toBe(TEAM_SCOPE_LABEL);
        expect(onScopeChange).toHaveBeenCalledTimes(1);
    });
});

describe("TeamProgressDashboard scope picker", () => {
    it("should switch every section to the chosen member test", () => {
        // Given:
        renderDashboard([ann(), bob()]);
        expect(screen.getByText(/2 learners across/)).not.toBeNull();

        // When:
        fireEvent.focus(combobox());
        fireEvent.change(combobox(), { target: { value: "bob" } });
        fireEvent.click(listbox().getByText("Bob Jones"));

        // Then: hero, KPIs, and the lower section all follow the scope
        expect(screen.getByRole("heading", { level: 1 }).textContent).toBe("Bob Jones");
        expect(screen.getByText("Engineer - Onboarding")).not.toBeNull();
        expect(screen.getByRole("heading", { level: 2, name: "Roadmaps and lessons" })).not.toBeNull();
        expect(combobox().value).toBe("Bob Jones");
    });

    it("should return to the team-wide view from the picker test", () => {
        // Given:
        renderDashboard([ann(), bob()]);
        fireEvent.focus(combobox());
        fireEvent.click(listbox().getByText("Ann Smith"));
        expect(screen.getByRole("heading", { level: 1 }).textContent).toBe("Ann Smith");

        // When:
        fireEvent.focus(combobox());
        fireEvent.click(listbox().getByText(TEAM_SCOPE_LABEL));

        // Then:
        expect(screen.getByRole("heading", { level: 1 }).textContent).toBe("How the team is doing");
        expect(screen.getByText(/2 learners across/)).not.toBeNull();
    });
});
