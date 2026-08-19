import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { cleanup, render, screen } from "@testing-library/react";
import type { ReactNode } from "react";
import { afterEach, describe, expect, it } from "vitest";
import type { LibraryLesson } from "../api/types";
import { LessonDetailsDialog } from "./LessonDetailsDialog";

function Wrapper({ children }: { children: ReactNode }) {
    const queryClient = new QueryClient();
    return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
}

function publishedLesson(): LibraryLesson {
    return {
        id: 1,
        title: "Onboarding basics",
        status: "ready",
        isPublished: true,
        isArchived: false,
        publicationStatus: "published",
        contentHtml: "<p>Body</p>",
        contentMarkdown: "",
        tags: [],
        activities: [],
        createdBy: "Author",
    } as unknown as LibraryLesson;
}

describe("LessonDetailsDialog", () => {
    afterEach(() => {
        cleanup();
    });

    it("should render the Make private control for a lessons.publish_archive holder test", () => {
        // Given: a published, ready lesson and a viewer who may publish/archive and manage it
        render(
            <Wrapper>
                <LessonDetailsDialog
                    open
                    lesson={publishedLesson()}
                    onClose={() => {}}
                    canPublish
                    canManageLesson
                />
            </Wrapper>,
        );

        // Then:
        expect(screen.getByRole("button", { name: "Make private" })).toBeTruthy();
    });

    it("should not render the Make private control for a viewer lacking lessons.publish_archive test", () => {
        // Given: the same lesson, but the viewer holds neither publish/archive nor manage
        render(
            <Wrapper>
                <LessonDetailsDialog
                    open
                    lesson={publishedLesson()}
                    onClose={() => {}}
                    canPublish={false}
                    canManageLesson={false}
                />
            </Wrapper>,
        );

        // Then:
        expect(screen.queryByRole("button", { name: "Make private" })).toBeNull();
    });
});
