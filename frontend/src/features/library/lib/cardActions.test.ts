import { describe, expect, it } from "vitest";

import type { LibraryLesson, LibraryRoadmap } from "../api/types";
import { getLessonCardActions, getRoadmapCardActions } from "./cardActions";

function lesson(overrides: Partial<LibraryLesson> = {}): LibraryLesson {
    return {
        id: 1,
        title: "Onboarding basics",
        status: "ready",
        publicationStatus: "published",
        tags: [],
        createdAt: "2026-01-01T00:00:00Z",
        updatedAt: "2026-01-01T00:00:00Z",
        isPublished: true,
        isArchived: false,
        ...overrides,
    } as LibraryLesson;
}

function roadmap(overrides: Partial<LibraryRoadmap> = {}): LibraryRoadmap {
    return { id: 5, title: "Company Essentials", viewerCanManage: true, ...overrides } as LibraryRoadmap;
}

describe("getLessonCardActions", () => {
    it("should offer edit, make private, duplicate, archive and delete for a public ready lesson", () => {
        // Given:
        const item = lesson({ status: "ready", isPublished: true });

        // When:
        const actions = getLessonCardActions(item, { canManage: true, canChangeStatus: true });

        // Then:
        expect(actions.map((action) => action.id)).toEqual([
            "edit",
            "make-private",
            "duplicate",
            "archive",
            "delete",
        ]);
        expect(actions.find((action) => action.id === "archive")?.separated).toBe(true);
        expect(actions.find((action) => action.id === "delete")?.danger).toBe(true);
    });

    it("should offer make public for a private ready lesson", () => {
        // Given:
        const item = lesson({ status: "ready", isPublished: false });

        // When:
        const actions = getLessonCardActions(item, { canManage: true, canChangeStatus: true });

        // Then:
        expect(actions.map((action) => action.id)).toContain("make-public");
        expect(actions.map((action) => action.id)).not.toContain("make-private");
    });

    it("should offer only delete for a failed lesson", () => {
        // Given:
        const item = lesson({ status: "failed", isPublished: false });

        // When:
        const actions = getLessonCardActions(item, { canManage: true, canChangeStatus: true });

        // Then:
        expect(actions).toEqual([{ id: "delete", label: "Delete…", danger: true }]);
    });

    it("should offer open, duplicate and delete for an archived lesson", () => {
        // Given:
        const item = lesson({ isArchived: true, publicationStatus: "archived" });

        // When:
        const actions = getLessonCardActions(item, { canManage: true, canChangeStatus: true });

        // Then:
        expect(actions.map((action) => action.id)).toEqual(["open", "duplicate", "delete"]);
        expect(actions[2].separated).toBe(true);
    });

    it("should offer nothing to a publisher who does not manage the lesson", () => {
        // Given: the backend rejects a status change on a lesson the viewer does not manage
        const item = lesson({ status: "ready", isPublished: true });

        // When:
        const actions = getLessonCardActions(item, { canManage: false, canChangeStatus: true });

        // Then:
        expect(actions).toEqual([]);
    });

    it("should leave out visibility and archive for a manager without the publish/archive permission", () => {
        // Given:
        const item = lesson({ status: "ready", isPublished: true });

        // When:
        const actions = getLessonCardActions(item, { canManage: true, canChangeStatus: false });

        // Then:
        expect(actions.map((action) => action.id)).toEqual(["edit", "duplicate", "delete"]);
        expect(actions[2].separated).toBe(true);
    });

    it("should offer nothing to a viewer without rights or before the lesson finishes generating", () => {
        // Given:
        const ready = lesson({ status: "ready" });
        const generating = lesson({ status: "generating", isPublished: false });
        const notStarted = lesson({ status: "draft", isPublished: false });

        // When:
        const withoutRights = getLessonCardActions(ready, { canManage: false, canChangeStatus: false });
        const whileGenerating = getLessonCardActions(generating, { canManage: true, canChangeStatus: true });
        const beforeGeneration = getLessonCardActions(notStarted, { canManage: true, canChangeStatus: true });

        // Then:
        expect(withoutRights).toEqual([]);
        expect(whileGenerating).toEqual([]);
        expect(beforeGeneration).toEqual([]);
    });
});

describe("getRoadmapCardActions", () => {
    it("should offer edit, duplicate and delete for a roadmap the viewer manages", () => {
        // Given:
        const item = roadmap({ viewerCanManage: true });

        // When:
        const actions = getRoadmapCardActions(item, { canManage: true, canDuplicate: true });

        // Then:
        expect(actions.map((action) => action.label)).toEqual(["Edit roadmap", "Duplicate", "Delete…"]);
        expect(actions[2].separated).toBe(true);
    });

    it("should offer only duplicate for a view-only roadmap", () => {
        // Given:
        const item = roadmap({ viewerCanManage: false });

        // When:
        const actions = getRoadmapCardActions(item, { canManage: true, canDuplicate: true });

        // Then:
        expect(actions).toEqual([{ id: "duplicate", label: "Duplicate" }]);
    });

    it("should offer nothing when the viewer can neither manage nor create roadmaps", () => {
        // Given:
        const item = roadmap({ viewerCanManage: false });

        // When:
        const actions = getRoadmapCardActions(item, { canManage: false, canDuplicate: false });

        // Then:
        expect(actions).toEqual([]);
    });
});
