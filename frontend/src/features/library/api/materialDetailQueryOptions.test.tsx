import { QueryClient, QueryClientProvider, useQuery } from "@tanstack/react-query";
import { renderHook, waitFor } from "@testing-library/react";
import type { ReactNode } from "react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { apiClient } from "@/shared/api/client";
import { materialDetailQueryOptions } from "./materialDetailQueryOptions";

function createWrapper() {
    const queryClient = new QueryClient();
    function Wrapper({ children }: { children: ReactNode }) {
        return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
    }
    return { Wrapper };
}

describe("materialDetailQueryOptions", () => {
    afterEach(() => {
        vi.restoreAllMocks();
    });

    it("should request the material by numeric id test", async () => {
        // Given:
        const getSpy = vi.spyOn(apiClient, "GET").mockResolvedValue({
            data: { material: { id: 7, title: "Guide", description: "", text: "full body", tags: [], usageCount: 0, youtubeUrls: [], youtubeVideos: [], links: [], linkAssets: [], attachments: [] } },
            error: undefined,
        } as never);
        const { Wrapper } = createWrapper();

        // When:
        const { result } = renderHook(() => useQuery(materialDetailQueryOptions("7")), { wrapper: Wrapper });
        await waitFor(() => expect(result.current.isSuccess).toBe(true));

        // Then:
        expect(getSpy).toHaveBeenCalledWith("/api/v1/materials/{id}", {
            params: { path: { id: 7 } },
            signal: expect.anything(),
        });
        expect(result.current.data?.material?.text).toBe("full body");
    });

    it("should surface an error message when the request fails test", async () => {
        // Given:
        vi.spyOn(apiClient, "GET").mockResolvedValue({
            data: undefined,
            error: { message: "Failed to load material." },
        } as never);
        const { Wrapper } = createWrapper();

        // When:
        const { result } = renderHook(() => useQuery(materialDetailQueryOptions("7")), { wrapper: Wrapper });
        await waitFor(() => expect(result.current.isError).toBe(true));

        // Then:
        expect(result.current.error).toBeInstanceOf(Error);
    });
});
