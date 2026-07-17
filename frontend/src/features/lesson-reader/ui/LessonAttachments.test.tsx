import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen, waitFor } from "@testing-library/react";
import type { ReactNode } from "react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { apiClient } from "@/shared/api/client";
import { LessonAttachments, normalizeLessonAsset } from "./LessonAttachments";

function createWrapper() {
    const queryClient = new QueryClient();
    function Wrapper({ children }: { children: ReactNode }) {
        return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
    }
    return Wrapper;
}

describe("normalizeLessonAsset", () => {
    it("should map a youtube lesson asset to its attachment card shape test", () => {
        // Given:
        const asset = {
            id: 1,
            kind: "youtube",
            title: "How to n8n",
            url: "https://youtu.be/abc123",
            imageUrl: "https://img.example.com/thumb.jpg",
        };

        // When:
        const result = normalizeLessonAsset(asset);

        // Then:
        expect(result).toMatchObject({
            id: 1,
            kind: "youtube",
            name: "How to n8n",
            url: "https://youtu.be/abc123",
            youtubeThumbnailUrl: "https://img.example.com/thumb.jpg",
            isLessonAsset: true,
        });
    });

    it("should map a link lesson asset to its attachment card shape test", () => {
        // Given:
        const asset = { id: 2, kind: "link", title: "Docs", url: "https://example.com", siteName: "Example" };

        // When:
        const result = normalizeLessonAsset(asset);

        // Then:
        expect(result).toMatchObject({ id: 2, kind: "link", name: "Docs", linkSiteName: "Example" });
    });

    it("should map a file lesson asset to its attachment card shape test", () => {
        // Given:
        const asset = { id: 3, kind: "file", name: "notes.pdf", mimeType: "application/pdf", storageKey: "k1" };

        // When:
        const result = normalizeLessonAsset(asset);

        // Then:
        expect(result).toMatchObject({ id: 3, kind: "file", name: "notes.pdf", storageKey: "k1" });
    });
});

describe("LessonAttachments", () => {
    afterEach(() => {
        vi.restoreAllMocks();
    });

    it("should resolve preview URLs for multiple image/video attachments with a single batch request test", async () => {
        // Given: three image/video attachments, each with its own storage key
        const attachments = [
            normalizeLessonAsset({ id: 1, kind: "image", name: "diagram.png", storageKey: "k1" }),
            normalizeLessonAsset({ id: 2, kind: "image", name: "photo.png", storageKey: "k2" }),
            normalizeLessonAsset({ id: 3, kind: "video", name: "clip.mp4", storageKey: "k3" }),
        ];
        const postSpy = vi.spyOn(apiClient, "POST").mockResolvedValue({
            data: {
                previews: [
                    { storageKey: "k1", previewUrl: "https://cdn.example.com/k1" },
                    { storageKey: "k2", previewUrl: "https://cdn.example.com/k2" },
                    { storageKey: "k3", previewUrl: "https://cdn.example.com/k3" },
                ],
            },
            error: undefined,
        } as never);

        // When:
        render(<LessonAttachments attachments={attachments} />, { wrapper: createWrapper() });

        // Then: exactly one batch call resolves all three keys, not one call per attachment
        await waitFor(() => expect(postSpy).toHaveBeenCalledTimes(1));
        expect(postSpy).toHaveBeenCalledWith(
            "/api/v1/files/previews",
            expect.objectContaining({ body: { storageKeys: ["k1", "k2", "k3"] } }),
        );
        await waitFor(() => {
            const images = screen.getAllByAltText("");
            expect(images).toHaveLength(2);
            expect(images[0].getAttribute("src")).toBe("https://cdn.example.com/k1");
            expect(images[1].getAttribute("src")).toBe("https://cdn.example.com/k2");
        });
    });

    it("should render lesson-attached assets alongside AI-generation source references, not one or the other test", () => {
        // Given: one asset attached directly to the lesson, and one derived from source references
        // (the reader used to render only whichever of these two arrived non-empty, silently
        // dropping directly-attached YouTube/link assets)
        const attachments = [
            normalizeLessonAsset({ id: 1, kind: "link", title: "Directly attached link", url: "https://a.example.com" }),
        ];
        const sourceReferences = [
            {
                id: "source-1",
                youtubeUrls: ["https://youtu.be/xyz"],
                youtubeVideos: [{ url: "https://youtu.be/xyz", title: "Source material video" }],
            },
        ];

        // When:
        render(
            <LessonAttachments attachments={attachments} sourceReferences={sourceReferences} />,
            { wrapper: createWrapper() },
        );

        // Then: both are visible
        expect(screen.getByText("Directly attached link")).toBeTruthy();
        expect(screen.getByText("Source material video")).toBeTruthy();
    });

    it("should render nothing when there are no attachments and no source references test", () => {
        // Given / When:
        const { container } = render(<LessonAttachments />, { wrapper: createWrapper() });

        // Then:
        expect(container.firstChild).toBeNull();
    });
});
