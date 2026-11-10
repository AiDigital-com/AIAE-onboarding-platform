import type { ReactNode } from "react";

import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { cleanup, fireEvent, render, screen } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import type { LibraryLesson } from "../api/types";
import type { CardActionItem } from "../model/cardActions";
import { LessonsGrid } from "./LessonsGrid";

/** Minimal ready-for-render lesson; tests override the fields the assertion depends on. */
function libraryLesson(overrides: Partial<LibraryLesson> = {}): LibraryLesson {
    return {
        id: 1,
        title: "Onboarding basics",
        status: "ready",
        publicationStatus: "published",
        contentMarkdownPreview: "Preview text",
        contentHtmlPreview: "<p>Preview text</p>",
        tags: [],
        createdBy: "AI Onboarding",
        createdAt: "2026-01-01T00:00:00Z",
        updatedAt: "2026-01-01T00:00:00Z",
        isPublished: true,
        isArchived: false,
        ...overrides,
    };
}

function Wrapper({ children }: { children: ReactNode }) {
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
}

function renderGrid(lessons: LibraryLesson[]) {
    render(
        <Wrapper>
            <LessonsGrid lessons={lessons} />
        </Wrapper>,
    );
}

describe("LessonsGrid (Library)", () => {
    afterEach(() => {
        cleanup();
    });

    it("should render a globe icon and a ready chip for a public ready lesson test", () => {
        // Given:
        const lesson = libraryLesson({ isPublished: true, status: "ready" });

        // When:
        renderGrid([lesson]);

        // Then:
        expect(screen.getByText("ready")).toBeTruthy();
        expect(
            screen.getByLabelText("Public — visible to everyone in the Library"),
        ).toBeTruthy();
        expect(
            screen.queryByLabelText("Private — visible in the Library only to assigned users"),
        ).toBeNull();
    });

    it("should render a lock icon and a ready chip for a private ready lesson test", () => {
        // Given:
        const lesson = libraryLesson({ isPublished: false, status: "ready" });

        // When:
        renderGrid([lesson]);

        // Then:
        expect(screen.getByText("ready")).toBeTruthy();
        expect(
            screen.getByLabelText("Private — visible in the Library only to assigned users"),
        ).toBeTruthy();
        expect(
            screen.queryByLabelText("Public — visible to everyone in the Library"),
        ).toBeNull();
    });

    it("should render the archived chip with its visibility icon for an archived lesson test", () => {
        // Given:
        const lesson = libraryLesson({ isArchived: true, isPublished: true, status: "ready" });

        // When:
        renderGrid([lesson]);

        // Then:
        expect(screen.getByText("archived")).toBeTruthy();
        expect(screen.queryByText("ready")).toBeNull();
        expect(
            screen.getByLabelText("Public — visible to everyone in the Library"),
        ).toBeTruthy();
    });

    it("should show a draft-status lesson as generating and never as draft test", () => {
        // Given:
        const lesson = libraryLesson({ status: "draft", isPublished: false });

        // When:
        render(
            <Wrapper>
                <LessonsGrid lessons={[lesson]} showEnrollmentAction canAssignLearning />
            </Wrapper>,
        );

        // Then:
        expect(screen.getByText("generating")).toBeTruthy();
        expect(screen.queryByText(/draft/i)).toBeNull();
    });

    it("should render no visibility icon for a generating lesson test", () => {
        // Given:
        const lesson = libraryLesson({ status: "generating", isPublished: true });

        // When:
        renderGrid([lesson]);

        // Then:
        expect(screen.getByText("generating")).toBeTruthy();
        expect(
            screen.queryByLabelText("Public — visible to everyone in the Library"),
        ).toBeNull();
        expect(
            screen.queryByLabelText("Private — visible in the Library only to assigned users"),
        ).toBeNull();
    });
});

describe("LessonsGrid card actions", () => {
    afterEach(() => {
        cleanup();
    });

    it("should run the chosen action from the more-actions menu without opening the lesson test", () => {
        // Given:
        const onOpenLesson = vi.fn();
        const onLessonAction = vi.fn();
        const actions: CardActionItem[] = [
            { id: "edit", label: "Edit lesson" },
            { id: "make-private", label: "Make private" },
            { id: "duplicate", label: "Duplicate" },
            { id: "archive", label: "Archive", separated: true },
            { id: "delete", label: "Delete…", danger: true },
        ];
        const lesson = libraryLesson({ id: 3 });
        render(
            <Wrapper>
                <LessonsGrid
                    lessons={[lesson]}
                    onOpenLesson={onOpenLesson}
                    getLessonActions={() => actions}
                    onLessonAction={onLessonAction}
                />
            </Wrapper>,
        );

        // When:
        fireEvent.click(screen.getByRole("button", { name: "More actions for Onboarding basics" }));

        // Then:
        expect(screen.getAllByRole("menuitem").map((node) => node.textContent)).toEqual([
            "Edit lesson",
            "Make private",
            "Duplicate",
            "Archive",
            "Delete…",
        ]);

        // When:
        fireEvent.click(screen.getByRole("menuitem", { name: "Archive" }));

        // Then:
        expect(onLessonAction).toHaveBeenCalledTimes(1);
        expect(onLessonAction).toHaveBeenCalledWith(lesson, "archive");
        expect(onOpenLesson).not.toHaveBeenCalled();
        expect(screen.queryByRole("menu")).toBeNull();
    });

    it("should open the actions menu on right-click and close it on an outside press test", () => {
        // Given:
        const actions: CardActionItem[] = [{ id: "delete", label: "Delete…", danger: true }];
        render(
            <Wrapper>
                <LessonsGrid lessons={[libraryLesson()]} getLessonActions={() => actions} />
            </Wrapper>,
        );

        // When:
        fireEvent.contextMenu(screen.getByRole("heading", { name: "Onboarding basics" }));

        // Then:
        expect(screen.getByRole("menuitem", { name: "Delete…" })).toBeTruthy();

        // When:
        fireEvent.pointerDown(document.body);

        // Then:
        expect(screen.queryByRole("menu")).toBeNull();
    });

    it("should render no more-actions button when the lesson has no actions test", () => {
        // Given:
        const lesson = libraryLesson();

        // When:
        render(
            <Wrapper>
                <LessonsGrid lessons={[lesson]} getLessonActions={() => []} />
            </Wrapper>,
        );

        // Then:
        expect(screen.queryByRole("button", { name: /More actions/ })).toBeNull();
    });
});

describe("LessonsGrid main action", () => {
    afterEach(() => {
        cleanup();
    });

    it("should offer Assign with no status wording for a private ready lesson to an assigner test", () => {
        // Given:
        const onAssignLesson = vi.fn();
        const onOpenLesson = vi.fn();
        const lesson = libraryLesson({ status: "ready", isPublished: false });
        render(
            <Wrapper>
                <LessonsGrid
                    lessons={[lesson]}
                    showEnrollmentAction
                    canAssignLearning
                    onAssignLesson={onAssignLesson}
                    onOpenLesson={onOpenLesson}
                />
            </Wrapper>,
        );

        // When:
        fireEvent.click(screen.getByRole("button", { name: "Assign" }));

        // Then:
        expect(onAssignLesson).toHaveBeenCalledTimes(1);
        expect(onAssignLesson).toHaveBeenCalledWith(lesson);
        expect(onOpenLesson).not.toHaveBeenCalled();
        expect(screen.queryByText(/draft/i)).toBeNull();
        expect(screen.queryByText(/Add to My Lessons/)).toBeNull();
    });

    it("should offer Restore instead of Assign for an archived lesson test", () => {
        // Given:
        const onRestoreLesson = vi.fn();
        const lesson = libraryLesson({ isArchived: true, publicationStatus: "archived" });
        render(
            <Wrapper>
                <LessonsGrid
                    lessons={[lesson]}
                    showEnrollmentAction
                    canAssignLearning
                    canRestoreLesson={() => true}
                    onRestoreLesson={onRestoreLesson}
                />
            </Wrapper>,
        );

        // When:
        fireEvent.click(screen.getByRole("button", { name: "Restore" }));

        // Then:
        expect(onRestoreLesson).toHaveBeenCalledTimes(1);
        expect(onRestoreLesson).toHaveBeenCalledWith(lesson);
        expect(screen.queryByRole("button", { name: "Assign" })).toBeNull();
    });

    it("should show no main button for a failed lesson test", () => {
        // Given:
        const lesson = libraryLesson({ status: "failed", isPublished: false });

        // When:
        render(
            <Wrapper>
                <LessonsGrid lessons={[lesson]} showEnrollmentAction canAssignLearning />
            </Wrapper>,
        );

        // Then:
        expect(screen.getByText("failed")).toBeTruthy();
        expect(screen.queryByRole("button")).toBeNull();
    });

    it("should let a learner add a public lesson and show a static label once it is theirs test", () => {
        // Given:
        const onEnrollLesson = vi.fn();
        const available = libraryLesson({ id: 1, title: "Available", isEnrolled: false });
        const added = libraryLesson({ id: 2, title: "Added", isEnrolled: true, isPublished: false });
        render(
            <Wrapper>
                <LessonsGrid lessons={[available, added]} showEnrollmentAction onEnrollLesson={onEnrollLesson} />
            </Wrapper>,
        );

        // When:
        fireEvent.click(screen.getByRole("button", { name: "Add to My Lessons" }));

        // Then:
        expect(onEnrollLesson).toHaveBeenCalledTimes(1);
        expect(onEnrollLesson).toHaveBeenCalledWith(available);
        expect(screen.getByText("In My Lessons")).toBeTruthy();
        expect(screen.getAllByRole("button")).toHaveLength(1);
    });

    it("should hide the status chip but keep the visibility icon when status is turned off test", () => {
        // Given:
        const lesson = libraryLesson({ status: "ready", isPublished: true });

        // When:
        render(
            <Wrapper>
                <LessonsGrid lessons={[lesson]} showEnrollmentAction showStatus={false} />
            </Wrapper>,
        );

        // Then:
        expect(screen.queryByText("ready")).toBeNull();
        expect(screen.getByLabelText("Public — visible to everyone in the Library")).toBeTruthy();
    });
});
