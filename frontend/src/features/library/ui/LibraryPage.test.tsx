import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react";
import type { ReactNode } from "react";
import { MemoryRouter } from "react-router-dom";
import { afterEach, describe, expect, it, vi } from "vitest";
import { apiClient } from "@/shared/api/client";
import { TaskTrayProvider } from "@/shared/context/TaskTrayContext";
import { LibraryPage } from "./LibraryPage";

/** Minimal current-user response, varying only by role. */
function meResponse(role: "member" | "teamlead" | "admin") {
    return { user: { id: 1, name: "Test User", email: "test@test.com", role } };
}

/** Minimal permission snapshot response; every key defaults to false unless overridden. */
function permissionsResponse(effective: Record<string, boolean>) {
    return { permissions: { roleCode: "member", effective, overrides: {} } };
}

const MEMBER_PERMISSIONS = permissionsResponse({ "learning.enroll": true });
const TEAM_LEAD_PERMISSIONS = permissionsResponse({
    "materials.create": true,
    "materials.edit": true,
    "materials.delete": true,
    "lessons.create": true,
    "lessons.manage": true,
    "lessons.publish_archive": true,
    "roadmaps.create": true,
    "roadmaps.manage": true,
    "learning.assign": true,
    "learning.enroll": true,
});

function renderLibraryPage(role: "member" | "teamlead", initialEntry = "/library") {
    const permissions = role === "member" ? MEMBER_PERMISSIONS : TEAM_LEAD_PERMISSIONS;

    vi.spyOn(apiClient, "GET").mockImplementation(async (path: string) => {
        if (path === "/api/v1/users/me") {
            return { data: meResponse(role), error: undefined } as never;
        }
        if (path === "/api/v1/permissions") {
            return { data: permissions, error: undefined } as never;
        }
        return { data: undefined, error: undefined } as never;
    });

    const postSpy = vi.spyOn(apiClient, "POST").mockImplementation(async (path: string) => {
        if (path === "/api/v1/lessons/count" || path === "/api/v1/materials/count" || path === "/api/v1/roadmaps/count") {
            return { data: { totalElements: 0 }, error: undefined } as never;
        }
        return { data: undefined, error: undefined } as never;
    });

    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });

    function Wrapper({ children }: { children: ReactNode }) {
        return (
            <QueryClientProvider client={queryClient}>
                <TaskTrayProvider>
                    <MemoryRouter initialEntries={[initialEntry]}>{children}</MemoryRouter>
                </TaskTrayProvider>
            </QueryClientProvider>
        );
    }

    render(
        <Wrapper>
            <LibraryPage />
        </Wrapper>,
    );

    return { postSpy };
}

describe("LibraryPage", () => {
    afterEach(() => {
        vi.restoreAllMocks();
        cleanup();
    });

    it("should show only the Lessons tab for a Member test", async () => {
        // Given / When:
        renderLibraryPage("member");
        await waitFor(() => expect(screen.getByText("Lessons")).toBeTruthy());

        // Then:
        expect(screen.queryByText("Materials")).toBeNull();
        expect(screen.queryByText("Roadmaps")).toBeNull();
    });

    it("should show all three tabs for a Team Lead test", async () => {
        // Given / When:
        renderLibraryPage("teamlead");
        await waitFor(() => expect(screen.getByText("Materials")).toBeTruthy());

        // Then:
        expect(screen.getByText("Lessons")).toBeTruthy();
        expect(screen.getByText("Roadmaps")).toBeTruthy();
    });

    it("should fall back to Lessons and issue no materials or roadmaps request when a Member deep-links a forbidden tab test", async () => {
        // Given: a Member deep-links ?tab=materials
        const { postSpy } = renderLibraryPage("member", "/library?tab=materials");

        // When: the page settles
        await waitFor(() => expect(screen.getByText("Lessons")).toBeTruthy());

        // Then: only the Lessons tab renders, and no materials/roadmaps endpoint was ever called
        expect(screen.queryByText("Materials")).toBeNull();
        expect(screen.queryByText("Roadmaps")).toBeNull();
        expect(postSpy).not.toHaveBeenCalledWith("/api/v1/materials/search", expect.anything());
        expect(postSpy).not.toHaveBeenCalledWith("/api/v1/materials/count", expect.anything());
        expect(postSpy).not.toHaveBeenCalledWith("/api/v1/roadmaps/search", expect.anything());
        expect(postSpy).not.toHaveBeenCalledWith("/api/v1/roadmaps/count", expect.anything());
    });

    it("should issue no materials or roadmaps request anywhere on a Member's Library page test", async () => {
        // Given / When:
        const { postSpy } = renderLibraryPage("member");
        await waitFor(() => expect(screen.getByText("Lessons")).toBeTruthy());

        // Then:
        expect(postSpy).not.toHaveBeenCalledWith("/api/v1/materials/search", expect.anything());
        expect(postSpy).not.toHaveBeenCalledWith("/api/v1/materials/count", expect.anything());
        expect(postSpy).not.toHaveBeenCalledWith("/api/v1/roadmaps/search", expect.anything());
        expect(postSpy).not.toHaveBeenCalledWith("/api/v1/roadmaps/count", expect.anything());
    });

    it("should request learnableOnly lessons, not publicationStatus=published, for the roadmap lesson picker test", async () => {
        // Given: a Team Lead opens the Roadmaps tab and the create-roadmap dialog
        const { postSpy } = renderLibraryPage("teamlead");
        await waitFor(() => expect(screen.getByText("Roadmaps")).toBeTruthy());
        fireEvent.click(screen.getByText("Roadmaps"));
        await waitFor(() => expect(screen.getByText("Create Roadmap")).toBeTruthy());

        // When:
        fireEvent.click(screen.getByText("Create Roadmap"));

        // Then: the picker query narrows via learnableOnly, never via publicationStatus=published,
        // so a private lesson is not excluded from the roadmap picker.
        await waitFor(() =>
            expect(postSpy).toHaveBeenCalledWith(
                "/api/v1/lessons/search",
                expect.objectContaining({
                    body: expect.objectContaining({ readyOnly: true, learnableOnly: true }),
                }),
            ),
        );
        const calls = postSpy.mock.calls as unknown as [string, { body?: Record<string, unknown> }][];
        const lessonsSearchCall = calls.find(([path]) => path === "/api/v1/lessons/search");
        expect(lessonsSearchCall?.[1]?.body).not.toHaveProperty("publicationStatus");
    });
});
