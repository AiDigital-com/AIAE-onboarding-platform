import { cleanup, fireEvent, render, screen } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { DuplicateLessonDialog } from "./DuplicateLessonDialog";

describe("DuplicateLessonDialog", () => {
    afterEach(() => {
        cleanup();
    });

    it("should pre-fill the name as Copy of the original title test", () => {
        // Given / When:
        render(
            <DuplicateLessonDialog open lessonTitle="Day 1" onCancel={vi.fn()} onConfirm={vi.fn()} />,
        );

        // Then:
        expect((screen.getByLabelText("Name of the copy") as HTMLInputElement).value).toBe("Copy of Day 1");
    });

    it("should clamp the pre-filled name to 100 characters test", () => {
        // Given / When:
        render(
            <DuplicateLessonDialog open lessonTitle={"x".repeat(150)} onCancel={vi.fn()} onConfirm={vi.fn()} />,
        );

        // Then:
        expect((screen.getByLabelText("Name of the copy") as HTMLInputElement).value).toHaveLength(100);
    });

    it("should confirm with the trimmed edited name test", () => {
        // Given:
        const onConfirm = vi.fn();
        render(<DuplicateLessonDialog open lessonTitle="Day 1" onCancel={vi.fn()} onConfirm={onConfirm} />);

        // When:
        fireEvent.change(screen.getByLabelText("Name of the copy"), { target: { value: "  Day 1 v2  " } });
        fireEvent.click(screen.getByRole("button", { name: "Duplicate" }));

        // Then:
        expect(onConfirm).toHaveBeenCalledWith("Day 1 v2");
    });

    it("should disable Duplicate for a blank name test", () => {
        // Given:
        const onConfirm = vi.fn();
        render(<DuplicateLessonDialog open lessonTitle="Day 1" onCancel={vi.fn()} onConfirm={onConfirm} />);

        // When:
        fireEvent.change(screen.getByLabelText("Name of the copy"), { target: { value: "   " } });

        // Then:
        const button = screen.getByRole("button", { name: "Duplicate" }) as HTMLButtonElement;
        expect(button.disabled).toBe(true);
        fireEvent.click(button);
        expect(onConfirm).not.toHaveBeenCalled();
    });

    it("should say what is and is not copied and show a server error test", () => {
        // Given / When:
        render(
            <DuplicateLessonDialog
                open
                lessonTitle="Day 1"
                error="Failed to duplicate lesson."
                onCancel={vi.fn()}
                onConfirm={vi.fn()}
            />,
        );

        // Then:
        expect(screen.getByText(/assignees and learner progress/i)).toBeTruthy();
        expect(screen.getByText("Failed to duplicate lesson.")).toBeTruthy();
    });
});
