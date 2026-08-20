import { cleanup, render, screen, within } from "@testing-library/react";
import { afterEach, describe, expect, it } from "vitest";

import type { LibraryLesson, LibraryRoadmap } from "../api/types";
import { RoadmapFormDialog } from "./RoadmapFormDialog";

function publishedLesson(): LibraryLesson {
    return {
        id: 1,
        title: "Public lesson",
        status: "ready",
        isPublished: true,
        isArchived: false,
        publicationStatus: "published",
        contentMarkdownPreview: "",
        contentHtmlPreview: "",
        tags: [],
        createdBy: "Author",
        createdAt: "2026-01-01T00:00:00",
        updatedAt: "2026-01-01T00:00:00",
    } as unknown as LibraryLesson;
}

function privateLesson(): LibraryLesson {
    return {
        id: 2,
        title: "Private lesson",
        status: "ready",
        isPublished: false,
        isArchived: false,
        publicationStatus: "private",
        contentMarkdownPreview: "",
        contentHtmlPreview: "",
        tags: [],
        createdBy: "Author",
        createdAt: "2026-01-01T00:00:00",
        updatedAt: "2026-01-01T00:00:00",
    } as unknown as LibraryLesson;
}

describe("RoadmapFormDialog", () => {
    afterEach(() => {
        cleanup();
    });

    it("should offer a private lesson in the add-lesson picker test", () => {
        // Given: the catalog (already server-scoped to ready+learnable) includes a private lesson
        // When:
        render(
            <RoadmapFormDialog
                open
                mode="create"
                lessons={[publishedLesson(), privateLesson()]}
                onClose={() => {}}
                onSave={() => {}}
            />,
        );

        // Then: both the public and the private lesson are selectable
        const picker = screen.getByRole("combobox", { name: /Add lesson/ }) as HTMLSelectElement;
        const optionLabels = within(picker)
            .getAllByRole("option")
            .map((option) => option.textContent);
        expect(optionLabels).toContain("Public lesson");
        expect(optionLabels).toContain("Private lesson");
    });

    it("should not offer a not-yet-ready lesson in the add-lesson picker test", () => {
        // Given: archived-lesson exclusion is now enforced server-side (learnableOnly, asserted at
        // the query-parameter level in LibraryPage.test.tsx), so the dialog no longer re-filters by
        // publication state - it still defensively excludes a lesson whose generation status is
        // not "ready", which is a genuinely separate axis from publication state.
        const generatingLesson: LibraryLesson = {
            ...publishedLesson(),
            id: 3,
            title: "Still generating lesson",
            status: "generating",
        };

        // When:
        render(
            <RoadmapFormDialog
                open
                mode="create"
                lessons={[publishedLesson(), generatingLesson]}
                onClose={() => {}}
                onSave={() => {}}
            />,
        );

        // Then:
        const picker = screen.getByRole("combobox", { name: /Add lesson/ }) as HTMLSelectElement;
        const optionLabels = within(picker)
            .getAllByRole("option")
            .map((option) => option.textContent);
        expect(optionLabels).not.toContain("Still generating lesson");
    });

    it("should display a private roadmap lesson as Private, not Public, in edit mode test", () => {
        // Given: a roadmap containing one private lesson, and the full catalog already loaded
        const initialRoadmap: LibraryRoadmap = {
            id: 10,
            title: "Onboarding path",
            description: "",
            tags: [],
            lessonIds: [2],
            lessons: [{ id: 2, title: "Private lesson", status: "ready", sortOrder: 0 }],
        } as unknown as LibraryRoadmap;

        // When:
        render(
            <RoadmapFormDialog
                open
                mode="edit"
                initialRoadmap={initialRoadmap}
                lessons={[publishedLesson(), privateLesson()]}
                onClose={() => {}}
                onSave={() => {}}
            />,
        );

        // Then: the selected lesson row shows the Private label, never the Public one
        expect(screen.getByLabelText("Private — visible in the Library only to assigned users")).toBeTruthy();
        expect(screen.queryByLabelText("Public — visible to everyone in the Library")).toBeNull();
    });
});
