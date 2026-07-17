import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { renderHook, waitFor } from "@testing-library/react";
import type { ReactNode } from "react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { apiClient } from "@/shared/api/client";
import { libraryQueryKeys } from "./queryKeys";
import { useRevokeLearningAssignmentMutation } from "./useLearningAssignmentMutation";

function createWrapper() {
    const queryClient = new QueryClient();
    function Wrapper({ children }: { children: ReactNode }) {
        return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
    }
    return { queryClient, Wrapper };
}

describe("useRevokeLearningAssignmentMutation", () => {
    afterEach(() => {
        vi.restoreAllMocks();
    });

    it("should send exactly one bulk POST for a multi-user revoke selection test", async () => {
        // Given:
        const postSpy = vi.spyOn(apiClient, "POST").mockResolvedValue({ data: { ok: true }, error: undefined } as never);
        const { Wrapper } = createWrapper();
        const { result } = renderHook(() => useRevokeLearningAssignmentMutation("roadmap"), {
            wrapper: Wrapper,
        });

        // When:
        result.current.mutate({ itemId: 5, userIds: [10, 20, 30] });
        await waitFor(() => expect(result.current.isSuccess).toBe(true));

        // Then: one bulk request, not one per selected user
        expect(postSpy).toHaveBeenCalledTimes(1);
        expect(postSpy).toHaveBeenCalledWith(
            "/api/v1/roadmaps/{id}/assignments/revoke",
            expect.objectContaining({
                params: { path: { id: 5 } },
                body: { userIds: [10, 20, 30] },
            }),
        );
    });

    it("should call the lesson revoke endpoint for itemType lesson test", async () => {
        // Given:
        const postSpy = vi.spyOn(apiClient, "POST").mockResolvedValue({ data: { ok: true }, error: undefined } as never);
        const { Wrapper } = createWrapper();
        const { result } = renderHook(() => useRevokeLearningAssignmentMutation("lesson"), {
            wrapper: Wrapper,
        });

        // When:
        result.current.mutate({ itemId: 7, userIds: [1, 2, 3] });
        await waitFor(() => expect(result.current.isSuccess).toBe(true));

        // Then:
        expect(postSpy).toHaveBeenCalledTimes(1);
        expect(postSpy).toHaveBeenCalledWith(
            "/api/v1/lessons/{id}/assignments/revoke",
            expect.objectContaining({
                params: { path: { id: 7 } },
                body: { userIds: [1, 2, 3] },
            }),
        );
    });

    it("should invalidate only the roadmap list, not the lesson list, on roadmap revoke test", async () => {
        // Given:
        vi.spyOn(apiClient, "POST").mockResolvedValue({ data: { ok: true }, error: undefined } as never);
        const { Wrapper, queryClient } = createWrapper();
        queryClient.setQueryData(libraryQueryKeys.roadmaps, { stale: "roadmaps" });
        queryClient.setQueryData(libraryQueryKeys.lessons, { stale: "lessons" });
        const { result } = renderHook(() => useRevokeLearningAssignmentMutation("roadmap"), {
            wrapper: Wrapper,
        });

        // When:
        result.current.mutate({ itemId: 5, userIds: [10] });
        await waitFor(() => expect(result.current.isSuccess).toBe(true));

        // Then:
        expect(queryClient.getQueryState(libraryQueryKeys.roadmaps)?.isInvalidated).toBe(true);
        expect(queryClient.getQueryState(libraryQueryKeys.lessons)?.isInvalidated).toBe(false);
    });

    it("should remove revoked users from the cached assignees list on success test", async () => {
        // Given:
        vi.spyOn(apiClient, "POST").mockResolvedValue({ data: { ok: true }, error: undefined } as never);
        const { Wrapper, queryClient } = createWrapper();
        const assigneesKey = libraryQueryKeys.roadmapAssignees(5);
        queryClient.setQueryData(assigneesKey, [
            { userId: 10, name: "A", email: "a@test.com", enrolledAt: "2026-01-01T00:00:00" },
            { userId: 20, name: "B", email: "b@test.com", enrolledAt: "2026-01-01T00:00:00" },
        ]);
        const { result } = renderHook(() => useRevokeLearningAssignmentMutation("roadmap"), {
            wrapper: Wrapper,
        });

        // When:
        result.current.mutate({ itemId: 5, userIds: [10] });
        await waitFor(() => expect(result.current.isSuccess).toBe(true));

        // Then:
        expect(queryClient.getQueryData(assigneesKey)).toEqual([
            { userId: 20, name: "B", email: "b@test.com", enrolledAt: "2026-01-01T00:00:00" },
        ]);
    });

    it("should surface an error message when the bulk request fails test", async () => {
        // Given:
        vi.spyOn(apiClient, "POST").mockResolvedValue({
            data: undefined,
            error: { message: "You can revoke roadmap assignments only for manageable users." },
        } as never);
        const { Wrapper } = createWrapper();
        const { result } = renderHook(() => useRevokeLearningAssignmentMutation("roadmap"), {
            wrapper: Wrapper,
        });

        // When:
        result.current.mutate({ itemId: 5, userIds: [10] });
        await waitFor(() => expect(result.current.isError).toBe(true));

        // Then:
        expect(result.current.error).toBeInstanceOf(Error);
        expect((result.current.error as Error).message).toBe(
            "You can revoke roadmap assignments only for manageable users.",
        );
    });
});
