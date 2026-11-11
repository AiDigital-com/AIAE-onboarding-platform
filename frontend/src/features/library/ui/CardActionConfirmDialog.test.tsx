import { cleanup, fireEvent, render, screen } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { Dialog } from "@/shared/ui/Dialog";
import type { LibraryLesson, LibraryRoadmap } from "../api/types";
import { CardActionConfirmDialog } from "./CardActionConfirmDialog";

function lesson(overrides: Partial<LibraryLesson> = {}): LibraryLesson {
    return { id: 1, title: "Onboarding basics", status: "ready", ...overrides } as LibraryLesson;
}

function roadmap(overrides: Partial<LibraryRoadmap> = {}): LibraryRoadmap {
    return { id: 5, title: "Company Essentials", viewerCanManage: true, ...overrides } as LibraryRoadmap;
}

describe("CardActionConfirmDialog", () => {
    afterEach(() => {
        cleanup();
    });

    it("should start focus on Cancel and use a non-destructive confirm button for archive", () => {
        // Given:
        render(
            <CardActionConfirmDialog
                confirmation={{ kind: "archive-lesson", lesson: lesson() }}
                isPending={false}
                error=""
                onCancel={vi.fn()}
                onConfirm={vi.fn()}
            />,
        );

        // Then:
        expect(screen.getByText("Archive lesson?")).toBeTruthy();
        expect(document.activeElement).toBe(screen.getByRole("button", { name: "Cancel" }));
        const confirmButton = screen.getByRole("button", { name: "Archive lesson" });
        expect(confirmButton.className).toContain("ui-btn--secondary");
        expect(confirmButton.className).not.toContain("ui-btn--danger");
    });

    it("should list only the consequences that apply when deleting a roadmap", () => {
        // Given:
        const onConfirm = vi.fn();
        render(
            <CardActionConfirmDialog
                confirmation={{
                    kind: "delete-roadmap",
                    roadmap: roadmap({ assigneeCount: 3, autoAssignNewUsers: false }),
                }}
                isPending={false}
                error=""
                onCancel={vi.fn()}
                onConfirm={onConfirm}
            />,
        );

        // When:
        fireEvent.click(screen.getByRole("button", { name: "Delete permanently" }));

        // Then:
        expect(onConfirm).toHaveBeenCalledTimes(1);
        expect(screen.getByText(/Assigned to 3 people/)).toBeTruthy();
        expect(screen.queryByText(/New users will no longer get this roadmap/)).toBeNull();
        expect(screen.getByText("Lessons are not deleted.")).toBeTruthy();
        expect(screen.getByRole("button", { name: "Delete permanently" }).className).toContain("ui-btn--danger");
    });

    it("should show the error and block both buttons while the action is pending", () => {
        // Given:
        const onCancel = vi.fn();
        render(
            <CardActionConfirmDialog
                confirmation={{ kind: "delete-lesson", lesson: lesson() }}
                isPending
                error="Lesson is in use."
                onCancel={onCancel}
                onConfirm={vi.fn()}
            />,
        );

        // When:
        fireEvent.keyDown(window, { key: "Escape" });

        // Then:
        expect(screen.getByRole("alert").textContent).toBe("Lesson is in use.");
        expect(screen.getByRole("button", { name: "Deleting..." })).toHaveProperty("disabled", true);
        expect(screen.getByRole("button", { name: "Cancel" })).toHaveProperty("disabled", true);
        expect(onCancel).not.toHaveBeenCalled();
    });

    it("should answer Escape itself and leave the dialog underneath open", () => {
        // Given:
        const onCloseUnderlying = vi.fn();
        const onCancel = vi.fn();
        render(
            <>
                <Dialog open onClose={onCloseUnderlying} title="Edit roadmap">
                    <p>Form</p>
                </Dialog>
                <CardActionConfirmDialog
                    confirmation={{ kind: "delete-roadmap", roadmap: roadmap() }}
                    isPending={false}
                    error=""
                    onCancel={onCancel}
                    onConfirm={vi.fn()}
                />
            </>,
        );

        // When:
        fireEvent.keyDown(document.body, { key: "Escape" });

        // Then:
        expect(onCancel).toHaveBeenCalledTimes(1);
        expect(onCloseUnderlying).not.toHaveBeenCalled();
    });

    it("should release the page scroll lock when two stacked dialogs close together", () => {
        // Given:
        const { rerender } = render(
            <>
                <Dialog open onClose={() => undefined} title="Edit roadmap">
                    <p>Form</p>
                </Dialog>
                <CardActionConfirmDialog
                    confirmation={{ kind: "delete-roadmap", roadmap: roadmap() }}
                    isPending={false}
                    error=""
                    onCancel={() => undefined}
                    onConfirm={() => undefined}
                />
            </>,
        );
        expect(document.body.style.overflow).toBe("hidden");

        // When: confirming delete from the Edit dialog closes both at once
        rerender(
            <>
                <Dialog open={false} onClose={() => undefined} title="Edit roadmap">
                    <p>Form</p>
                </Dialog>
                <CardActionConfirmDialog
                    confirmation={null}
                    isPending={false}
                    error=""
                    onCancel={() => undefined}
                    onConfirm={() => undefined}
                />
            </>,
        );

        // Then:
        expect(document.body.style.overflow).toBe("");
    });
});
