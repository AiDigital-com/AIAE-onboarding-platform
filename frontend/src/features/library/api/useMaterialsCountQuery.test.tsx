import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { renderHook, waitFor } from "@testing-library/react";
import type { ReactNode } from "react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { apiClient } from "@/shared/api/client";
import { useMaterialsCountQuery } from "./useMaterialsCountQuery";

function createWrapper() {
    const queryClient = new QueryClient();
    function Wrapper({ children }: { children: ReactNode }) {
        return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
    }
    return { Wrapper };
}

describe("useMaterialsCountQuery", () => {
    afterEach(() => {
        vi.restoreAllMocks();
    });

    it("should request the count endpoint with the current filter and an abort signal test", async () => {
        // Given:
        const postSpy = vi.spyOn(apiClient, "POST").mockResolvedValue({
            data: { totalElements: 42 },
            error: undefined,
        } as never);
        const { Wrapper } = createWrapper();

        // When:
        const { result } = renderHook(() => useMaterialsCountQuery({ query: "design" }), { wrapper: Wrapper });
        await waitFor(() => expect(result.current.isSuccess).toBe(true));

        // Then:
        expect(postSpy).toHaveBeenCalledWith(
            "/api/v1/materials/count",
            expect.objectContaining({
                body: expect.objectContaining({ query: "design" }),
                signal: expect.anything(),
            }),
        );
        expect(result.current.data).toBe(42);
    });

    it("should not retry on failure so the tab badge fails fast test", async () => {
        // Given:
        vi.spyOn(apiClient, "POST").mockResolvedValue({
            data: undefined,
            error: { message: "Failed to load material count." },
        } as never);
        const { Wrapper } = createWrapper();

        // When:
        const { result } = renderHook(() => useMaterialsCountQuery(), { wrapper: Wrapper });
        await waitFor(() => expect(result.current.isError).toBe(true));

        // Then:
        expect(result.current.failureCount).toBe(1);
    });
});
