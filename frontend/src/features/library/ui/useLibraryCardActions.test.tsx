import type { ReactNode } from "react";

import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { act, renderHook, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { apiClient } from "@/shared/api/client";
import type { LibraryLesson, LibraryRoadmap } from "../api/types";
import type { UseLibraryCardActionsOptions } from "../model/cardActions";
import { useLibraryCardActions } from "./useLibraryCardActions";

function lesson(overrides: Partial<LibraryLesson> = {}): LibraryLesson {
    return { id: 11, title: "Onboarding basics", status: "ready", isPublished: false, ...overrides } as LibraryLesson;
}

function roadmap(overrides: Partial<LibraryRoadmap> = {}): LibraryRoadmap {
    return { id: 5, title: "Company Essentials", viewerCanManage: true, ...overrides } as LibraryRoadmap;
}

function renderCardActions(overrides: Partial<UseLibraryCardActionsOptions> = {}) {
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    function Wrapper({ children }: { children: ReactNode }) {
        return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
    }
    const options: UseLibraryCardActionsOptions = {
        notify: vi.fn(),
        getLessonAccess: () => ({ canManage: true, canChangeStatus: true }),
        getRoadmapAccess: () => ({ canManage: true, canDuplicate: true }),
        onOpenLesson: vi.fn(),
        onOpenRoadmap: vi.fn(),
        ...overrides,
    };
    return { options, ...renderHook(() => useLibraryCardActions(options), { wrapper: Wrapper }) };
}

describe("useLibraryCardActions", () => {
    afterEach(() => {
        vi.restoreAllMocks();
    });

    it("should show a placeholder notice and send no request when duplicating", () => {
        // Given:
        const patchSpy = vi.spyOn(apiClient, "PATCH");
        const postSpy = vi.spyOn(apiClient, "POST");
        const notify = vi.fn();
        const { result } = renderCardActions({ notify });

        // When:
        act(() => {
            result.current.onLessonAction(lesson(), "duplicate");
            result.current.onRoadmapAction(roadmap(), "duplicate");
        });

        // Then:
        expect(notify).toHaveBeenNthCalledWith(1, "Duplicating lessons is coming soon.", "warning");
        expect(notify).toHaveBeenNthCalledWith(2, "Duplicating roadmaps is coming soon.", "warning");
        expect(patchSpy).not.toHaveBeenCalled();
        expect(postSpy).not.toHaveBeenCalled();
    });

    it("should publish a lesson immediately and offer an undo that makes it private again", async () => {
        // Given:
        const patchSpy = vi.spyOn(apiClient, "PATCH").mockResolvedValue({ data: {}, error: undefined } as never);
        const notify = vi.fn();
        const { result } = renderCardActions({ notify });

        // When:
        act(() => result.current.onLessonAction(lesson({ id: 11 }), "make-public"));
        await waitFor(() => expect(notify).toHaveBeenCalledTimes(1));

        // Then:
        expect(result.current.confirmation).toBeNull();
        expect(patchSpy).toHaveBeenCalledTimes(1);
        expect(patchSpy).toHaveBeenCalledWith("/api/v1/lessons/{id}/status", {
            params: { path: { id: 11 } },
            body: { action: "publish" },
        });
        const [message, severity, undo] = notify.mock.calls[0];
        expect(message).toBe("Lesson is now public.");
        expect(severity).toBe("success");
        expect(undo.label).toBe("Undo");

        // When:
        act(() => undo.onClick());
        await waitFor(() => expect(notify).toHaveBeenCalledTimes(2));

        // Then:
        expect(patchSpy).toHaveBeenCalledTimes(2);
        expect(patchSpy).toHaveBeenLastCalledWith("/api/v1/lessons/{id}/status", {
            params: { path: { id: 11 } },
            body: { action: "unpublish" },
        });
        expect(notify).toHaveBeenLastCalledWith("Lesson is now private.", "success", undefined);
    });

    it("should archive a lesson only after the confirmation is accepted", async () => {
        // Given:
        const patchSpy = vi.spyOn(apiClient, "PATCH").mockResolvedValue({ data: {}, error: undefined } as never);
        const notify = vi.fn();
        const { result } = renderCardActions({ notify });
        const item = lesson({ id: 12 });

        // When:
        act(() => result.current.onLessonAction(item, "archive"));

        // Then:
        expect(result.current.confirmation).toEqual({ kind: "archive-lesson", lesson: item });
        expect(patchSpy).not.toHaveBeenCalled();

        // When:
        act(() => result.current.confirm());
        await waitFor(() => expect(result.current.confirmation).toBeNull());

        // Then:
        expect(patchSpy).toHaveBeenCalledTimes(1);
        expect(patchSpy).toHaveBeenCalledWith("/api/v1/lessons/{id}/status", {
            params: { path: { id: 12 } },
            body: { action: "archive" },
        });
        expect(notify.mock.calls[0][0]).toBe("Lesson archived.");
    });

    it("should restore an archived lesson immediately and offer an undo that archives it again", async () => {
        // Given:
        const patchSpy = vi.spyOn(apiClient, "PATCH").mockResolvedValue({ data: {}, error: undefined } as never);
        const notify = vi.fn();
        const { result } = renderCardActions({ notify });
        const item = lesson({ id: 13, isArchived: true });

        // When:
        act(() => result.current.onRestoreLesson(item));
        await waitFor(() => expect(notify).toHaveBeenCalledTimes(1));

        // Then:
        expect(result.current.canRestoreLesson(item)).toBe(true);
        expect(result.current.confirmation).toBeNull();
        expect(patchSpy).toHaveBeenCalledWith("/api/v1/lessons/{id}/status", {
            params: { path: { id: 13 } },
            body: { action: "restore" },
        });
        const [message, , undo] = notify.mock.calls[0];
        expect(message).toBe("Lesson restored.");

        // When:
        act(() => undo.onClick());
        await waitFor(() => expect(notify).toHaveBeenCalledTimes(2));

        // Then:
        expect(patchSpy).toHaveBeenCalledTimes(2);
        expect(patchSpy).toHaveBeenLastCalledWith("/api/v1/lessons/{id}/status", {
            params: { path: { id: 13 } },
            body: { action: "archive" },
        });
        expect(notify).toHaveBeenLastCalledWith("Lesson archived.", "success", undefined);
    });

    it("should not offer restore without the publish/archive permission", () => {
        // Given:
        const { result } = renderCardActions({
            getLessonAccess: () => ({ canManage: true, canChangeStatus: false }),
        });

        // Then:
        expect(result.current.canRestoreLesson(lesson({ isArchived: true }))).toBe(false);
    });

    it("should send one request when restore is triggered twice in a row", async () => {
        // Given:
        const patchSpy = vi.spyOn(apiClient, "PATCH").mockResolvedValue({ data: {}, error: undefined } as never);
        const notify = vi.fn();
        const { result } = renderCardActions({ notify });
        const item = lesson({ id: 14, isArchived: true });

        // When:
        act(() => {
            result.current.onRestoreLesson(item);
            result.current.onRestoreLesson(item);
        });
        await waitFor(() => expect(notify).toHaveBeenCalledTimes(1));

        // Then:
        expect(patchSpy).toHaveBeenCalledTimes(1);
    });

    it("should delete a roadmap once and report it after the confirmation is accepted", async () => {
        // Given:
        const deleteSpy = vi.spyOn(apiClient, "DELETE").mockResolvedValue({ data: {}, error: undefined } as never);
        const notify = vi.fn();
        const onRoadmapDeleted = vi.fn();
        const { result } = renderCardActions({ notify, onRoadmapDeleted });
        const item = roadmap({ id: 5 });

        // When:
        act(() => result.current.onRoadmapAction(item, "delete"));
        act(() => result.current.confirm());
        await waitFor(() => expect(result.current.confirmation).toBeNull());

        // Then:
        expect(deleteSpy).toHaveBeenCalledTimes(1);
        expect(deleteSpy).toHaveBeenCalledWith("/api/v1/roadmaps/{id}", { params: { path: { id: 5 } } });
        expect(onRoadmapDeleted).toHaveBeenCalledWith(item);
        expect(notify).toHaveBeenCalledWith("Roadmap deleted successfully.");
    });

    it("should keep the confirmation open with the error when deleting a lesson fails", async () => {
        // Given:
        vi.spyOn(apiClient, "DELETE").mockResolvedValue({
            data: undefined,
            error: { message: "Lesson is in use." },
        } as never);
        const notify = vi.fn();
        const onLessonDeleted = vi.fn();
        const { result } = renderCardActions({ notify, onLessonDeleted });

        // When:
        act(() => result.current.onLessonAction(lesson(), "delete"));
        act(() => result.current.confirm());
        await waitFor(() => expect(result.current.confirmError).not.toBe(""));

        // Then:
        expect(result.current.confirmation?.kind).toBe("delete-lesson");
        expect(onLessonDeleted).not.toHaveBeenCalled();
        expect(notify).not.toHaveBeenCalled();
    });

    it("should not send a request when the confirmation is cancelled", () => {
        // Given:
        const deleteSpy = vi.spyOn(apiClient, "DELETE");
        const { result } = renderCardActions();

        // When:
        act(() => result.current.onLessonAction(lesson(), "delete"));
        act(() => result.current.cancelConfirmation());

        // Then:
        expect(result.current.confirmation).toBeNull();
        expect(deleteSpy).not.toHaveBeenCalled();
    });
});
