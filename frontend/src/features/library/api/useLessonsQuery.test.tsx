import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { renderHook, waitFor } from "@testing-library/react";
import type { ReactNode } from "react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { apiClient } from "@/shared/api/client";
import { useLessonsQuery } from "./useLessonsQuery";

function createWrapper() {
    const queryClient = new QueryClient();
    function Wrapper({ children }: { children: ReactNode }) {
        return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
    }
    return { Wrapper };
}

function page(overrides: Record<string, unknown> = {}) {
    return { page: 0, size: 24, totalElements: 0, totalPages: 0, hasNext: false, hasPrevious: false, ...overrides };
}

describe("useLessonsQuery", () => {
    afterEach(() => {
        vi.restoreAllMocks();
    });

    it("should not fetch while disabled, matching the active-tab-only gating test", () => {
        // Given:
        const postSpy = vi.spyOn(apiClient, "POST").mockResolvedValue({
            data: { lessons: [], page: page() },
            error: undefined,
        } as never);
        const { Wrapper } = createWrapper();

        // When:
        renderHook(() => useLessonsQuery({}, { enabled: false }), { wrapper: Wrapper });

        // Then:
        expect(postSpy).not.toHaveBeenCalled();
    });

    it("should forward an abort signal so an obsolete search can be canceled test", async () => {
        // Given:
        const postSpy = vi.spyOn(apiClient, "POST").mockResolvedValue({
            data: { lessons: [], page: page() },
            error: undefined,
        } as never);
        const { Wrapper } = createWrapper();

        // When:
        const { result } = renderHook(() => useLessonsQuery({ query: "design" }), { wrapper: Wrapper });
        await waitFor(() => expect(result.current.isSuccess).toBe(true));

        // Then:
        expect(postSpy).toHaveBeenCalledWith(
            "/api/v1/lessons/search",
            expect.objectContaining({ signal: expect.anything() }),
        );
    });

    it("should retry at most once on failure instead of the default three retries test", async () => {
        // Given:
        vi.spyOn(apiClient, "POST").mockResolvedValue({
            data: undefined,
            error: { message: "Failed to load lessons." },
        } as never);
        const { Wrapper } = createWrapper();

        // When:
        const { result } = renderHook(() => useLessonsQuery(), { wrapper: Wrapper });
        await waitFor(() => expect(result.current.isError).toBe(true), { timeout: 3000 });

        // Then:
        expect(result.current.failureCount).toBe(2);
    });
});
