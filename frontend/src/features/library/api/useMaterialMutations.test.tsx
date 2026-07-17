import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { renderHook, waitFor } from "@testing-library/react";
import type { ReactNode } from "react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { apiClient } from "@/shared/api/client";
import { useUploadMaterialFileMutation } from "./useMaterialMutations";

function createWrapper() {
    const queryClient = new QueryClient();
    function Wrapper({ children }: { children: ReactNode }) {
        return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
    }
    return { Wrapper };
}

describe("useUploadMaterialFileMutation", () => {
    afterEach(() => {
        vi.restoreAllMocks();
        vi.unstubAllGlobals();
    });

    it("should presign then PUT the file directly to storage, never sending bytes through the backend test", async () => {
        // Given:
        const postSpy = vi.spyOn(apiClient, "POST").mockResolvedValue({
            data: { uploadUrl: "https://bucket.example.com/presigned", storageKey: "uploads/abc/notes.pdf" },
            error: undefined,
        } as never);
        const fetchMock = vi.fn().mockResolvedValue({ ok: true } as Response);
        vi.stubGlobal("fetch", fetchMock);
        const file = new File(["hello"], "notes.pdf", { type: "application/pdf" });
        const { Wrapper } = createWrapper();

        // When:
        const { result } = renderHook(() => useUploadMaterialFileMutation(), { wrapper: Wrapper });
        result.current.mutate(file);
        await waitFor(() => expect(result.current.isSuccess).toBe(true));

        // Then:
        expect(postSpy).toHaveBeenCalledWith(
            "/api/v1/materials/upload-url",
            expect.objectContaining({ body: { fileName: "notes.pdf", contentType: "application/pdf", size: file.size } }),
        );
        expect(fetchMock).toHaveBeenCalledWith(
            "https://bucket.example.com/presigned",
            expect.objectContaining({ method: "PUT", headers: { "Content-Type": "application/pdf" } }),
        );
        expect(result.current.data).toMatchObject({ storageKey: "uploads/abc/notes.pdf", originalName: "notes.pdf" });
    });

    it("should reject when the storage PUT is rejected test", async () => {
        // Given:
        vi.spyOn(apiClient, "POST").mockResolvedValue({
            data: { uploadUrl: "https://bucket.example.com/presigned", storageKey: "uploads/abc/notes.pdf" },
            error: undefined,
        } as never);
        vi.stubGlobal("fetch", vi.fn().mockResolvedValue({ ok: false } as Response));
        const file = new File(["hello"], "notes.pdf", { type: "application/pdf" });
        const { Wrapper } = createWrapper();

        // When:
        const { result } = renderHook(() => useUploadMaterialFileMutation(), { wrapper: Wrapper });
        result.current.mutate(file);
        await waitFor(() => expect(result.current.isError).toBe(true));

        // Then:
        expect(result.current.error).toBeInstanceOf(Error);
    });
});
