import { MemoryRouter } from "react-router-dom";

import { cleanup, fireEvent, render, screen } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import type { LibraryRoadmap } from "../api/types";
import type { CardActionItem } from "../model/cardActions";
import { RoadmapsGrid } from "./RoadmapsGrid";

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

function renderGrid(items: LibraryRoadmap[], props: Partial<Parameters<typeof RoadmapsGrid>[0]> = {}) {
    return render(
        <MemoryRouter>
            <RoadmapsGrid roadmaps={items} canAssignLearning onAssignRoadmap={vi.fn()} {...props} />
        </MemoryRouter>,
    );
}

afterEach(() => {
    cleanup();
});

describe("RoadmapsGrid assignment summary", () => {
    it("should open the assign dialog from the Assign button and from the auto-assign badge test", () => {
        // Given:
        const onAssignRoadmap = vi.fn();
        renderGrid([roadmap({ autoAssignNewUsers: true })], { onAssignRoadmap });

        // When:
        fireEvent.click(screen.getByRole("button", { name: "Assign" }));
        fireEvent.click(screen.getByRole("button", { name: /Auto-assigned to new users/ }));

        // Then:
        expect(onAssignRoadmap).toHaveBeenCalledTimes(2);
        expect(screen.queryByText("Add...")).toBeNull();
        expect(screen.queryByText("Subscribe")).toBeNull();
    });

    it("should show the assignee count with an avatar stack and overflow test", () => {
        // Given: six assignees, four previewed
        renderGrid([
            roadmap({
                assigneeCount: 6,
                assigneePreview: [1, 2, 3, 4].map((id) => ({ userId: id, name: `User ${id}`, email: `u${id}@example.com` })),
            }),
        ]);

        // Then:
        expect(screen.getByText("6 assigned")).not.toBeNull();
        expect(screen.getByText("+2")).not.toBeNull();
    });

    it("should say when nobody is assigned and hide the count from a non-manager test", () => {
        // Given:
        renderGrid([roadmap({ id: 1, assigneeCount: 0 }), roadmap({ id: 2, assigneeCount: null, viewerCanManage: false })]);

        // Then:
        expect(screen.getByText("Not assigned yet")).not.toBeNull();
        expect(screen.getAllByText(/assigned/i)).toHaveLength(1);
    });

    it("should hide the Assign action without the learning.assign permission test", () => {
        // Given:
        renderGrid([roadmap({ autoAssignNewUsers: true })], { canAssignLearning: false });

        // Then: the badge is informational only
        expect(screen.queryByRole("button", { name: "Assign" })).toBeNull();
        expect(screen.queryByRole("button", { name: /Auto-assigned/ })).toBeNull();
        expect(screen.getByText("Auto-assigned to new users")).not.toBeNull();
    });
});

describe("RoadmapsGrid card actions", () => {
    it("should open the actions menu from the more-actions button without opening the roadmap test", () => {
        // Given:
        const onOpenRoadmap = vi.fn();
        const onRoadmapAction = vi.fn();
        const actions: CardActionItem[] = [
            { id: "edit", label: "Edit roadmap" },
            { id: "duplicate", label: "Duplicate" },
            { id: "delete", label: "Delete…", danger: true, separated: true },
        ];
        const item = roadmap({ id: 7 });
        renderGrid([item], { onOpenRoadmap, onRoadmapAction, getRoadmapActions: () => actions });

        // When:
        fireEvent.click(screen.getByRole("button", { name: "More actions for Company Essentials" }));

        // Then:
        expect(screen.getAllByRole("menuitem").map((node) => node.textContent)).toEqual([
            "Edit roadmap",
            "Duplicate",
            "Delete…",
        ]);
        expect(onOpenRoadmap).not.toHaveBeenCalled();

        // When:
        fireEvent.click(screen.getByRole("menuitem", { name: "Duplicate" }));

        // Then:
        expect(onRoadmapAction).toHaveBeenCalledTimes(1);
        expect(onRoadmapAction).toHaveBeenCalledWith(item, "duplicate");
        expect(onOpenRoadmap).not.toHaveBeenCalled();
        expect(screen.queryByRole("menu")).toBeNull();
    });

    it("should open the same menu on right-click and close it on Escape test", () => {
        // Given:
        const actions: CardActionItem[] = [{ id: "duplicate", label: "Duplicate" }];
        renderGrid([roadmap()], { getRoadmapActions: () => actions });

        // When:
        fireEvent.contextMenu(screen.getByRole("heading", { name: "Company Essentials" }));

        // Then:
        const menu = screen.getByRole("menu");
        expect(screen.getByRole("menuitem", { name: "Duplicate" })).not.toBeNull();

        // When:
        fireEvent.keyDown(menu, { key: "Escape" });

        // Then:
        expect(screen.queryByRole("menu")).toBeNull();
    });

    it("should move focus between menu items with the arrow keys test", () => {
        // Given:
        const actions: CardActionItem[] = [
            { id: "edit", label: "Edit roadmap" },
            { id: "delete", label: "Delete…", danger: true },
        ];
        renderGrid([roadmap()], { getRoadmapActions: () => actions });
        fireEvent.click(screen.getByRole("button", { name: "More actions for Company Essentials" }));
        const menu = screen.getByRole("menu");

        // Then: the first item is focused on open
        expect(document.activeElement).toBe(screen.getByRole("menuitem", { name: "Edit roadmap" }));

        // When:
        fireEvent.keyDown(menu, { key: "ArrowDown" });

        // Then:
        expect(document.activeElement).toBe(screen.getByRole("menuitem", { name: "Delete…" }));

        // When: wraps around
        fireEvent.keyDown(menu, { key: "ArrowDown" });

        // Then:
        expect(document.activeElement).toBe(screen.getByRole("menuitem", { name: "Edit roadmap" }));
    });

    it("should hide the more-actions button when a roadmap has no actions test", () => {
        // Given:
        renderGrid([roadmap()], { getRoadmapActions: () => [] });

        // Then:
        expect(screen.queryByRole("button", { name: /More actions/ })).toBeNull();
    });
});
