import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { renderHook, waitFor } from "@testing-library/react";
import type { ReactNode } from "react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { apiClient } from "@/shared/api/client";
import { useUploadLessonFileMutation, useUploadLessonVideoMutation } from "./useLessonMutations";

vi.mock("@/shared/lib/videoFileValidation", () => ({
    assertBrowserPlayableVideo: vi.fn().mockResolvedValue(undefined),
    inferVideoMimeType: () => null,
}));

function createWrapper() {
    const queryClient = new QueryClient();
    function Wrapper({ children }: { children: ReactNode }) {
        return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
    }
    return { Wrapper };
}

describe("useUploadLessonFileMutation", () => {
    afterEach(() => {
        vi.restoreAllMocks();
        vi.unstubAllGlobals();
    });

    it("should presign then PUT the file directly to storage instead of the multipart endpoint test", async () => {
        // Given:
        const postSpy = vi.spyOn(apiClient, "POST").mockResolvedValue({
            data: { uploadUrl: "https://bucket.example.com/presigned", storageKey: "uploads/abc/diagram.png" },
            error: undefined,
        } as never);
        const fetchMock = vi.fn().mockResolvedValue({ ok: true } as Response);
        vi.stubGlobal("fetch", fetchMock);
        const file = new File(["hello"], "diagram.png", { type: "image/png" });
        const { Wrapper } = createWrapper();

        // When:
        const { result } = renderHook(() => useUploadLessonFileMutation(), { wrapper: Wrapper });
        result.current.mutate(file);
        await waitFor(() => expect(result.current.isSuccess).toBe(true));

        // Then:
        expect(postSpy).toHaveBeenCalledWith(
            "/api/v1/lessons/upload-url",
            expect.objectContaining({ body: { fileName: "diagram.png", contentType: "image/png", size: file.size } }),
        );
        expect(fetchMock).toHaveBeenCalledWith(
            "https://bucket.example.com/presigned",
            expect.objectContaining({ method: "PUT" }),
        );
        expect(result.current.data).toMatchObject({ storageKey: "uploads/abc/diagram.png" });
    });
});

describe("useUploadLessonVideoMutation", () => {
    afterEach(() => {
        vi.restoreAllMocks();
        vi.unstubAllGlobals();
    });

    it("should presign then PUT the video directly to storage test", async () => {
        // Given:
        const postSpy = vi.spyOn(apiClient, "POST").mockResolvedValue({
            data: { uploadUrl: "https://bucket.example.com/presigned", storageKey: "uploads/abc/clip.mp4" },
            error: undefined,
        } as never);
        const fetchMock = vi.fn().mockResolvedValue({ ok: true } as Response);
        vi.stubGlobal("fetch", fetchMock);
        const file = new File(["hello"], "clip.mp4", { type: "video/mp4" });
        const { Wrapper } = createWrapper();

        // When:
        const { result } = renderHook(() => useUploadLessonVideoMutation(), { wrapper: Wrapper });
        result.current.mutate(file);
        await waitFor(() => expect(result.current.isSuccess).toBe(true));

        // Then:
        expect(postSpy).toHaveBeenCalledWith(
            "/api/v1/lessons/upload-url",
            expect.objectContaining({ body: { fileName: "clip.mp4", contentType: "video/mp4", size: file.size } }),
        );
        expect(result.current.data).toMatchObject({ storageKey: "uploads/abc/clip.mp4" });
    });
});
