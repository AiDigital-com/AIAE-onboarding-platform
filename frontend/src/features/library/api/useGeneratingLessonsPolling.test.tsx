import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { renderHook } from "@testing-library/react";
import type { ReactNode } from "react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { apiClient } from "@/shared/api/client";
import { libraryQueryKeys } from "./queryKeys";
import { useGeneratingLessonsPolling } from "./useGeneratingLessonsPolling";

function createWrapper(queryClient: QueryClient) {
    function Wrapper({ children }: { children: ReactNode }) {
        return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
    }
    return Wrapper;
}

describe("useGeneratingLessonsPolling", () => {
    beforeEach(() => {
        vi.useFakeTimers();
    });

    afterEach(() => {
        vi.useRealTimers();
        vi.restoreAllMocks();
    });

    it("should poll each generating lesson's status endpoint, not the full lessons list test", async () => {
        // Given:
        const getSpy = vi.spyOn(apiClient, "GET").mockResolvedValue({
            data: { status: "generating" },
            error: undefined,
        } as never);
        const postSpy = vi.spyOn(apiClient, "POST");
        const queryClient = new QueryClient();

        // When:
        renderHook(() => useGeneratingLessonsPolling([7]), { wrapper: createWrapper(queryClient) });
        await vi.waitFor(() =>
            expect(getSpy).toHaveBeenCalledWith("/api/v1/lessons/{id}/generation-status", {
                params: { path: { id: 7 } },
                signal: expect.anything(),
            }),
        );

        // Then: no full-list search request was made by this hook
        expect(postSpy).not.toHaveBeenCalled();
    });

    it("should invalidate the lessons list once a polled lesson's status leaves generating test", async () => {
        // Given: the status flips from generating to ready between polls
        const getSpy = vi
            .spyOn(apiClient, "GET")
            .mockResolvedValueOnce({ data: { status: "generating" }, error: undefined } as never)
            .mockResolvedValue({ data: { status: "ready" }, error: undefined } as never);
        const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
        const invalidateSpy = vi.spyOn(queryClient, "invalidateQueries");

        // When: first poll observes "generating"
        renderHook(() => useGeneratingLessonsPolling([7]), { wrapper: createWrapper(queryClient) });
        await vi.waitFor(() => expect(getSpy).toHaveBeenCalledTimes(1));
        expect(invalidateSpy).not.toHaveBeenCalled();

        // When: the next poll interval elapses and observes "ready"
        await vi.advanceTimersByTimeAsync(5000);
        await vi.waitFor(() => expect(getSpy).toHaveBeenCalledTimes(2));

        // Then: the status-change effect has already committed by the time the second poll resolves
        await vi.waitFor(() =>
            expect(invalidateSpy).toHaveBeenCalledWith({ queryKey: libraryQueryKeys.lessons }),
        );
    });
});
