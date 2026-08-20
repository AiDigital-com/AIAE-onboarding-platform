import type { ReactNode } from "react";
import { MemoryRouter } from "react-router-dom";

import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { cleanup, render, screen } from "@testing-library/react";
import { afterEach, describe, expect, it } from "vitest";

import type { EnrolledLessonCard } from "../api/types";
import { LessonsGrid } from "./LessonsGrid";

/** Minimal ready-for-render enrolled lesson; tests override the fields the assertion depends on. */
function enrolledLesson(overrides: Partial<EnrolledLessonCard> = {}): EnrolledLessonCard {
    return {
        id: 1,
        title: "Onboarding basics",
        status: "ready",
        publicationStatus: "published",
        isPublished: true,
        isArchived: false,
        contentHtmlPreview: "<p>Preview text</p>",
        contentMarkdownPreview: "Preview text",
        createdBy: "AI Onboarding",
        createdAt: "2026-01-01T00:00:00Z",
        updatedAt: "2026-01-01T00:00:00Z",
        isCompleted: false,
        isEnrolled: true,
        tags: [],
        flashcardCount: 0,
        quizCount: 0,
        hasTeacherVideo: false,
        ...overrides,
    };
}

function Wrapper({ children }: { children: ReactNode }) {
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    return (
        <QueryClientProvider client={queryClient}>
            <MemoryRouter>{children}</MemoryRouter>
        </QueryClientProvider>
    );
}

function renderGrid(lessons: EnrolledLessonCard[]) {
    render(
        <Wrapper>
            <LessonsGrid lessons={lessons} />
        </Wrapper>,
    );
}

describe("LessonsGrid (My Lessons)", () => {
    afterEach(() => {
        cleanup();
    });

    it("should render a globe icon and a ready chip for a public ready lesson test", () => {
        // Given:
        const lesson = enrolledLesson({ isPublished: true, status: "ready" });

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
        const lesson = enrolledLesson({ isPublished: false, status: "ready" });

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

    it("should render the archived chip and no visibility icon for an archived lesson test", () => {
        // Given:
        const lesson = enrolledLesson({ isArchived: true, isPublished: true, status: "ready" });

        // When:
        renderGrid([lesson]);

        // Then:
        expect(screen.getByText("archived")).toBeTruthy();
        expect(
            screen.queryByLabelText("Public — visible to everyone in the Library"),
        ).toBeNull();
        expect(
            screen.queryByLabelText("Private — visible in the Library only to assigned users"),
        ).toBeNull();
    });

    it("should render no visibility icon for a generating lesson test", () => {
        // Given:
        const lesson = enrolledLesson({ status: "generating", isPublished: true });

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
