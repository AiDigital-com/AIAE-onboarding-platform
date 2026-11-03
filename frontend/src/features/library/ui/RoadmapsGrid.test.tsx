import { MemoryRouter } from "react-router-dom";

import { cleanup, fireEvent, render, screen } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import type { LibraryRoadmap } from "../api/types";
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
