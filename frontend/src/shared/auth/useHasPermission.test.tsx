import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { renderHook, waitFor } from "@testing-library/react";
import type { ReactNode } from "react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { apiClient } from "@/shared/api/client";
import { useHasPermission } from "./useHasPermission";

function createWrapper() {
    const queryClient = new QueryClient();
    function Wrapper({ children }: { children: ReactNode }) {
        return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
    }
    return { Wrapper };
}

describe("useHasPermission", () => {
    afterEach(() => {
        vi.restoreAllMocks();
    });

    it("should return false for every key before the current-user snapshot loads test", () => {
        // Given:
        vi.spyOn(apiClient, "GET").mockReturnValue(new Promise(() => {}) as never);
        const { Wrapper } = createWrapper();

        // When:
        const { result } = renderHook(() => useHasPermission(), { wrapper: Wrapper });

        // Then:
        expect(result.current("learning.ask")).toBe(false);
    });

    it("should read effective permissions from the caller's own snapshot only test", async () => {
        // Given: the lean bootstrap contract returns only the viewer's own permissions, not a per-user map
        const getSpy = vi.spyOn(apiClient, "GET").mockResolvedValue({
            data: {
                permissions: {
                    roleCode: "teamlead",
                    effective: { "learning.ask": true, "admin.manage_roles": false },
                    overrides: {},
                },
            },
            error: undefined,
        } as never);
        const { Wrapper } = createWrapper();
        const { result } = renderHook(() => useHasPermission(), { wrapper: Wrapper });

        // When:
        await waitFor(() => expect(result.current("learning.ask")).toBe(true));

        // Then:
        expect(getSpy).toHaveBeenCalledWith("/api/v1/permissions");
        expect(result.current("admin.manage_roles")).toBe(false);
        expect(result.current("unknown.key")).toBe(false);
    });
});
