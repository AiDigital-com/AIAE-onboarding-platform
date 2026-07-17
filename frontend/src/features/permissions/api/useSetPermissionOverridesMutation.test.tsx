import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { renderHook, waitFor } from "@testing-library/react";
import type { ReactNode } from "react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { apiClient } from "@/shared/api/client";
import { useSetPermissionOverridesMutation } from "./useSetPermissionOverridesMutation";

function createWrapper() {
    const queryClient = new QueryClient();
    function Wrapper({ children }: { children: ReactNode }) {
        return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
    }
    return { Wrapper, queryClient };
}

describe("useSetPermissionOverridesMutation", () => {
    afterEach(() => {
        vi.restoreAllMocks();
    });

    it("should invalidate both the bootstrap and management permission queries on success test", async () => {
        // Given: both the current-user bootstrap cache and the management-view cache hold stale data
        vi.spyOn(apiClient, "PUT").mockResolvedValue({
            data: { permissions: { roleCode: "member", effective: {}, overrides: {} } },
            error: undefined,
        } as never);
        const { Wrapper, queryClient } = createWrapper();
        queryClient.setQueryData(["session", "permissions"], { stale: "bootstrap" });
        queryClient.setQueryData(["permissions", "management"], { stale: "management" });
        const { result } = renderHook(() => useSetPermissionOverridesMutation(), { wrapper: Wrapper });

        // When:
        result.current.mutate({ userId: 10, overrides: { "learning.ask": true } });
        await waitFor(() => expect(result.current.isSuccess).toBe(true));

        // Then:
        expect(queryClient.getQueryState(["session", "permissions"])?.isInvalidated).toBe(true);
        expect(queryClient.getQueryState(["permissions", "management"])?.isInvalidated).toBe(true);
    });
});
