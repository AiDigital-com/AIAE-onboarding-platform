import type { ReactNode } from "react";

import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { cleanup, fireEvent, render, screen, waitFor, within } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { apiClient } from "@/shared/api/client";
import type { LibraryRoadmap } from "../api/types";
import { RoadmapAssignDialog } from "./RoadmapAssignDialog";

vi.mock("@/shared/api/files", () => ({
    useFilePreviewUrls: () => ({ data: undefined }),
    useFilePreviewUrl: () => ({ data: undefined }),
}));

function roadmap(overrides: Partial<LibraryRoadmap> = {}): LibraryRoadmap {
    return {
        id: 5,
        title: "Company Essentials",
        tags: [],
        lessons: [],
        lessonIds: [],
        isEnrolled: false,
        viewerCanManage: true,
        autoAssignNewUsers: false,
        createdBy: "Admin",
        createdAt: "2026-11-01T00:00:00Z",
        updatedAt: "2026-11-01T00:00:00Z",
        ...overrides,
    } as LibraryRoadmap;
}

/** Routes every GET the dialog issues to canned data so the test controls what is "assigned". */
function mockGets(options: {
    assignees?: unknown[];
    groupAssignments?: unknown[];
    users?: unknown[];
    groups?: unknown[];
}) {
    return vi.spyOn(apiClient, "GET").mockImplementation((async (path: string) => {
        if (path === "/api/v1/roadmaps/{id}/assignees") {
            return { data: { assignees: options.assignees ?? [] }, error: undefined };
        }
        if (path === "/api/v1/roadmaps/{id}/group-assignments") {
            return { data: { assignments: options.groupAssignments ?? [] }, error: undefined };
        }
        if (path === "/api/v1/learning/assignees") {
            return { data: { users: options.users ?? [] }, error: undefined };
        }
        if (path === "/api/v1/groups") {
            return {
                data: {
                    groups: options.groups ?? [],
                    page: { page: 0, size: 10, totalElements: 0, totalPages: 1, hasNext: false, hasPrevious: false },
                },
                error: undefined,
            };
        }
        if (path === "/api/v1/grades") {
            return { data: { grades: [] }, error: undefined };
        }
        return { data: undefined, error: undefined };
    }) as never);
}

function renderDialog(props: Partial<Parameters<typeof RoadmapAssignDialog>[0]> = {}) {
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    function Wrapper({ children }: { children: ReactNode }) {
        return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
    }
    const onClose = vi.fn();
    const onNotify = vi.fn();
    render(
        <RoadmapAssignDialog
            open
            roadmap={roadmap()}
            canToggleAutoAssign
            onClose={onClose}
            onNotify={onNotify}
            {...props}
        />,
        { wrapper: Wrapper },
    );
    return { onClose, onNotify };
}

function ann() {
    return { id: 1, name: "Ann Smith", email: "ann@example.com", role: "member" };
}

function bob() {
    return { id: 2, name: "Bob Jones", email: "bob@example.com", role: "member" };
}

afterEach(() => {
    cleanup();
    vi.restoreAllMocks();
});

describe("RoadmapAssignDialog", () => {
    it("should list people and teams in one search and stage a pick as a chip test", async () => {
        // Given:
        mockGets({
            users: [ann(), bob()],
            groups: [{ id: 7, name: "Design", description: "", leads: [], membersCount: 3, membersWithoutGradeCount: 0, createdAt: "", updatedAt: "" }],
        });
        renderDialog();
        const combobox = screen.getByRole("combobox", { name: "Search people or teams" });

        // When:
        fireEvent.focus(combobox);
        const listbox = await screen.findByRole("listbox", { name: "People and teams" });
        await within(listbox).findByText("Bob Jones");
        fireEvent.click(within(listbox).getByText("Bob Jones"));

        // Then: Bob is a chip, the footer describes the change, and the primary button is enabled
        expect(screen.getByRole("button", { name: "Remove Bob Jones" })).not.toBeNull();
        expect(screen.getByRole("status").textContent).toBe("1 person will get this roadmap");
        expect((screen.getByRole("button", { name: "Assign" }) as HTMLButtonElement).disabled).toBe(false);
        expect(within(listbox).queryByText("Design")).not.toBeNull();
    });

    it("should mark an already assigned person and show how each assignee got the roadmap test", async () => {
        // Given: Ann holds it via a team only, Bob by the new-user rule
        mockGets({
            users: [ann(), bob()],
            assignees: [
                { userId: 1, name: "Ann Smith", email: "ann@example.com", enrolledAt: "2026-11-01T00:00:00Z", autoAssigned: false, viaGroupNames: ["Design"] },
                { userId: 2, name: "Bob Jones", email: "bob@example.com", enrolledAt: "2026-11-01T00:00:00Z", autoAssigned: true, viaGroupNames: [] },
            ],
        });
        renderDialog();

        // When: the assigned list is opened
        const manage = await screen.findByRole("button", { name: /Assigned to 2 people/ });
        fireEvent.click(manage);

        // Then: sources are labelled and a team-only person cannot be removed on their own
        expect(screen.getByText("via Design")).not.toBeNull();
        expect(screen.getByText("New user")).not.toBeNull();
        expect((screen.getByRole("button", { name: "Remove Ann Smith" }) as HTMLButtonElement).disabled).toBe(true);
        expect((screen.getByRole("button", { name: "Remove Bob Jones" }) as HTMLButtonElement).disabled).toBe(false);

        // When: searching, Ann shows as assigned
        fireEvent.focus(screen.getByRole("combobox", { name: "Search people or teams" }));
        const listbox = await screen.findByRole("listbox", { name: "People and teams" });
        await within(listbox).findByText("Ann Smith");
        expect(within(listbox).getAllByText("✓ Assigned")).toHaveLength(2);
    });

    it("should stage a removal with undo and apply it through one bulk revoke on save test", async () => {
        // Given:
        mockGets({
            assignees: [
                { userId: 2, name: "Bob Jones", email: "bob@example.com", enrolledAt: "2026-11-01T00:00:00Z", autoAssigned: false, viaGroupNames: [] },
            ],
        });
        const postSpy = vi.spyOn(apiClient, "POST").mockResolvedValue({ data: { ok: true }, error: undefined } as never);
        const { onClose, onNotify } = renderDialog();
        fireEvent.click(await screen.findByRole("button", { name: /Assigned to 1 person/ }));

        // When: Bob is removed, then undone, then removed again and saved
        fireEvent.click(screen.getByRole("button", { name: "Remove Bob Jones" }));
        expect(screen.getByRole("button", { name: "Remove" })).not.toBeNull();
        expect(screen.getByRole("status").textContent).toBe("1 will be removed");
        fireEvent.click(screen.getByRole("button", { name: "Undo" }));
        expect(screen.getByRole("button", { name: "Assign" })).not.toBeNull();
        fireEvent.click(screen.getByRole("button", { name: "Remove Bob Jones" }));
        fireEvent.click(screen.getByRole("button", { name: "Remove" }));

        // Then:
        await waitFor(() => expect(onClose).toHaveBeenCalled());
        expect(postSpy).toHaveBeenCalledTimes(1);
        expect(postSpy).toHaveBeenCalledWith(
            "/api/v1/roadmaps/{id}/assignments/revoke",
            expect.objectContaining({ params: { path: { id: 5 } }, body: { userIds: [2] } }),
        );
        expect(onNotify).toHaveBeenCalledWith("1 removed");
    });

    it("should save the auto-assign toggle immediately and ask before turning it off test", async () => {
        // Given: the toggle is currently off
        mockGets({});
        const putSpy = vi.spyOn(apiClient, "PUT").mockResolvedValue({
            data: { roadmap: roadmap({ autoAssignNewUsers: true }) },
            error: undefined,
        } as never);
        const { onNotify } = renderDialog();

        // When: turned on
        fireEvent.click(screen.getByRole("switch", { name: "Automatically assign to new users" }));

        // Then: one PUT, a toast with undo, no Save needed
        await waitFor(() => expect(putSpy).toHaveBeenCalledTimes(1));
        expect(putSpy).toHaveBeenCalledWith(
            "/api/v1/roadmaps/{id}/auto-assign",
            expect.objectContaining({ params: { path: { id: 5 } }, body: { enabled: true } }),
        );
        await waitFor(() => expect(onNotify).toHaveBeenCalled());
        expect(onNotify.mock.calls[0][0]).toContain("Auto-assign on");
        expect(typeof onNotify.mock.calls[0][2]).toBe("function");
        // The switch reflects the saved state even though the parent still passes the old roadmap
        expect(
            screen.getByRole("switch", { name: "Automatically assign to new users" }).getAttribute("aria-checked"),
        ).toBe("true");
    });

    it("should require confirmation before turning auto-assign off and keep it on by default test", () => {
        // Given: the toggle is on
        mockGets({});
        const putSpy = vi.spyOn(apiClient, "PUT");
        renderDialog({ roadmap: roadmap({ autoAssignNewUsers: true }) });

        // When:
        fireEvent.click(screen.getByRole("switch", { name: "Automatically assign to new users" }));

        // Then: the inline warning appears and nothing is sent until "Turn off"
        expect(screen.getByRole("alert").textContent).toContain("New users won't get this roadmap anymore");
        expect(putSpy).not.toHaveBeenCalled();
        fireEvent.click(screen.getByRole("button", { name: "Keep on" }));
        expect(screen.queryByRole("alert")).toBeNull();
    });

    it("should disable the toggle for a non-admin test", () => {
        // Given:
        mockGets({});
        renderDialog({ canToggleAutoAssign: false });

        // Then:
        const toggle = screen.getByRole("switch", { name: "Automatically assign to new users" }) as HTMLButtonElement;
        expect(toggle.disabled).toBe(true);
        expect(screen.getByText("Only an admin can change this setting")).not.toBeNull();
    });
});
