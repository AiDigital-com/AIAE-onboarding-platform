import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { renderHook, waitFor } from "@testing-library/react";
import type { ReactNode } from "react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { apiClient } from "@/shared/api/client";
import { useMyLessonsQuery } from "./useMyLessonsQuery";

function createWrapper() {
    const queryClient = new QueryClient();
    function Wrapper({ children }: { children: ReactNode }) {
        return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
    }
    return { Wrapper };
}

function summary(overrides: Record<string, unknown> = {}) {
    return {
        id: 1,
        title: "Lesson",
        description: "desc",
        status: "ready",
        publicationStatus: "published",
        coverImageStorageKey: "key",
        coverImageOriginalName: "cover.png",
        coverImageMimeType: "image/png",
        contentHtmlPreview: "<p>preview</p>",
        contentMarkdownPreview: "preview",
        hasTeacherVideo: false,
        tags: ["tag"],
        createdBy: "Author",
        createdAt: "2026-01-01T00:00:00",
        updatedAt: "2026-01-01T00:00:00",
        enrollment: { lessonId: 1, enrolledAt: "2026-01-01T00:00:00", completedAt: null, isCompleted: false },
        activityCounts: { flashcardCount: 2, quizCount: 1 },
        ...overrides,
    };
}

describe("useMyLessonsQuery", () => {
    afterEach(() => {
        vi.restoreAllMocks();
    });

    it("should request the first page with the default page size test", async () => {
        // Given:
        const getSpy = vi.spyOn(apiClient, "GET").mockResolvedValue({
            data: { lessons: [summary()], page: { page: 0, size: 20, totalElements: 1, totalPages: 1, hasNext: false, hasPrevious: false } },
            error: undefined,
        } as never);
        const { Wrapper } = createWrapper();

        // When:
        const { result } = renderHook(() => useMyLessonsQuery(), { wrapper: Wrapper });
        await waitFor(() => expect(result.current.isSuccess).toBe(true));

        // Then:
        expect(getSpy).toHaveBeenCalledWith("/api/v1/learning/my-lessons", {
            params: { query: { page: 0, size: 20 } },
        });
        expect(result.current.data?.pages[0]?.items).toHaveLength(1);
    });

    it("should map bounded summary fields onto the card model without carrying full content test", async () => {
        // Given:
        vi.spyOn(apiClient, "GET").mockResolvedValue({
            data: {
                lessons: [summary({ hasTeacherVideo: true, activityCounts: { flashcardCount: 3, quizCount: 4 } })],
                page: { page: 0, size: 20, totalElements: 1, totalPages: 1, hasNext: false, hasPrevious: false },
            },
            error: undefined,
        } as never);
        const { Wrapper } = createWrapper();

        // When:
        const { result } = renderHook(() => useMyLessonsQuery(), { wrapper: Wrapper });
        await waitFor(() => expect(result.current.isSuccess).toBe(true));

        // Then:
        const card = result.current.data?.pages[0]?.items[0];
        expect(card).toMatchObject({
            id: 1,
            contentHtmlPreview: "<p>preview</p>",
            contentMarkdownPreview: "preview",
            hasTeacherVideo: true,
            flashcardCount: 3,
            quizCount: 4,
            isPublished: true,
            isArchived: false,
        });
    });

    it("should request the next page number when fetchNextPage is called test", async () => {
        // Given:
        const getSpy = vi.spyOn(apiClient, "GET").mockResolvedValue({
            data: { lessons: [summary()], page: { page: 0, size: 20, totalElements: 2, totalPages: 2, hasNext: true, hasPrevious: false } },
            error: undefined,
        } as never);
        const { Wrapper } = createWrapper();
        const { result } = renderHook(() => useMyLessonsQuery(), { wrapper: Wrapper });
        await waitFor(() => expect(result.current.isSuccess).toBe(true));

        // When:
        getSpy.mockResolvedValue({
            data: { lessons: [summary({ id: 2 })], page: { page: 1, size: 20, totalElements: 2, totalPages: 2, hasNext: false, hasPrevious: true } },
            error: undefined,
        } as never);
        void result.current.fetchNextPage();
        await waitFor(() => expect(result.current.data?.pages).toHaveLength(2));

        // Then:
        expect(getSpy).toHaveBeenLastCalledWith("/api/v1/learning/my-lessons", {
            params: { query: { page: 1, size: 20 } },
        });
    });

    it("should surface an error message when the request fails test", async () => {
        // Given:
        vi.spyOn(apiClient, "GET").mockResolvedValue({
            data: undefined,
            error: { message: "Failed to load my lessons." },
        } as never);
        const { Wrapper } = createWrapper();

        // When:
        const { result } = renderHook(() => useMyLessonsQuery(), { wrapper: Wrapper });
        await waitFor(() => expect(result.current.isError).toBe(true));

        // Then:
        expect(result.current.error).toBeInstanceOf(Error);
    });
});
