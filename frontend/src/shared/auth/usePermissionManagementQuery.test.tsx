import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { renderHook, waitFor } from "@testing-library/react";
import type { ReactNode } from "react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { apiClient } from "@/shared/api/client";
import { usePermissionManagementQuery } from "./usePermissionManagementQuery";

function createWrapper() {
    const queryClient = new QueryClient();
    function Wrapper({ children }: { children: ReactNode }) {
        return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
    }
    return { Wrapper };
}

describe("usePermissionManagementQuery", () => {
    afterEach(() => {
        vi.restoreAllMocks();
    });

    it("should fetch the management snapshot endpoint, not the bootstrap one test", async () => {
        // Given:
        const getSpy = vi.spyOn(apiClient, "GET").mockResolvedValue({
            data: { permissionDefinitions: [], permissionsByUserId: {}, users: [] },
            error: undefined,
        } as never);
        const { Wrapper } = createWrapper();

        // When:
        const { result } = renderHook(() => usePermissionManagementQuery(), { wrapper: Wrapper });
        await waitFor(() => expect(result.current.isSuccess).toBe(true));

        // Then:
        expect(getSpy).toHaveBeenCalledWith("/api/v1/permissions/management");
    });

    it("should not fetch while disabled test", () => {
        // Given:
        const getSpy = vi.spyOn(apiClient, "GET").mockResolvedValue({ data: undefined, error: undefined } as never);
        const { Wrapper } = createWrapper();

        // When:
        renderHook(() => usePermissionManagementQuery(false), { wrapper: Wrapper });

        // Then:
        expect(getSpy).not.toHaveBeenCalled();
    });
});
