import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { renderHook, waitFor } from "@testing-library/react";
import type { ReactNode } from "react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { apiClient } from "@/shared/api/client";
import { useAssignUserRoleMutation } from "./useAssignUserRoleMutation";

function createWrapper() {
    const queryClient = new QueryClient();
    function Wrapper({ children }: { children: ReactNode }) {
        return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
    }
    return { Wrapper, queryClient };
}

describe("useAssignUserRoleMutation", () => {
    afterEach(() => {
        vi.restoreAllMocks();
    });

    it("should invalidate admin users, groups, teams, and both permission queries on success test", async () => {
        // Given: a role change makes the previously-fetched admin permission-management snapshot
        // (fetched once with staleTime: Infinity) stale for every affected user, not just the caller.
        vi.spyOn(apiClient, "PATCH").mockResolvedValue({
            data: { user: { id: 15, name: "Ivan Member", email: "ivan@test.com", role: "teamlead" } },
            error: undefined,
        } as never);
        const { Wrapper, queryClient } = createWrapper();
        queryClient.setQueryData(["admin", "users"], { stale: "adminUsers" });
        queryClient.setQueryData(["groups"], { stale: "groups" });
        queryClient.setQueryData(["teams"], { stale: "teams" });
        queryClient.setQueryData(["session", "permissions"], { stale: "bootstrap" });
        queryClient.setQueryData(["permissions", "management"], { stale: "management" });
        const { result } = renderHook(() => useAssignUserRoleMutation(), { wrapper: Wrapper });

        // When:
        result.current.mutate({ userId: 15, roleCode: "teamlead" });
        await waitFor(() => expect(result.current.isSuccess).toBe(true));

        // Then:
        expect(queryClient.getQueryState(["admin", "users"])?.isInvalidated).toBe(true);
        expect(queryClient.getQueryState(["groups"])?.isInvalidated).toBe(true);
        expect(queryClient.getQueryState(["teams"])?.isInvalidated).toBe(true);
        expect(queryClient.getQueryState(["session", "permissions"])?.isInvalidated).toBe(true);
        expect(queryClient.getQueryState(["permissions", "management"])?.isInvalidated).toBe(true);
    });
});
