import { useEffect, useRef } from "react";

import { Button } from "@/shared/ui/Button";
import { Dialog } from "@/shared/ui/Dialog";
import type { CardActionConfirmDialogProps, ConfirmActionDialogProps } from "../model/cardActions";
import "./card-actions-menu.css";

/**
 * Small confirmation dialog shared by the card actions: same shell as the lesson editor's
 * "Delete lesson?" dialog. Focus starts on Cancel so Enter never confirms by accident.
 */
export function ConfirmActionDialog({
    open,
    title,
    confirmLabel,
    pendingLabel,
    confirmVariant,
    isPending,
    error,
    children,
    onCancel,
    onConfirm,
}: ConfirmActionDialogProps) {
    // Read through a ref so the listener below is bound once per open, not once per render.
    const cancelRef = useRef({ isPending, onCancel });
    cancelRef.current = { isPending, onCancel };

    // Give focus back to whatever had it (the ⋯ button, or the Edit dialog's Delete button).
    useEffect(() => {
        if (!open) {
            return undefined;
        }
        const previouslyFocused = document.activeElement;
        return () => {
            if (previouslyFocused instanceof HTMLElement && previouslyFocused.isConnected) {
                previouslyFocused.focus();
            }
        };
    }, [open]);

    // This dialog can sit on top of another one (Edit roadmap). Esc must answer only the
    // confirmation, so it is handled in the capture phase and kept from the dialog underneath.
    useEffect(() => {
        if (!open) {
            return undefined;
        }
        const handleKeyDown = (event: KeyboardEvent) => {
            if (event.key !== "Escape") {
                return;
            }
            event.stopPropagation();
            if (!cancelRef.current.isPending) {
                cancelRef.current.onCancel();
            }
        };
        window.addEventListener("keydown", handleKeyDown, true);
        return () => window.removeEventListener("keydown", handleKeyDown, true);
    }, [open]);

    return (
        <Dialog
            open={open}
            onClose={isPending ? undefined : onCancel}
            size="sm"
            title={title}
            flushBody
            closeDisabled={isPending}
            footer={
                <>
                    <Button autoFocus onClick={onCancel} disabled={isPending}>
                        Cancel
                    </Button>
                    <Button variant={confirmVariant} onClick={onConfirm} disabled={isPending}>
                        {isPending ? pendingLabel : confirmLabel}
                    </Button>
                </>
            }
        >
            <div className="library-form-body">
                <div className="card-actions-confirm">
                    {children}
                    {error && (
                        <p className="library-alert library-alert--error" role="alert">
                            {error}
                        </p>
                    )}
                </div>
            </div>
        </Dialog>
    );
}

/** Confirmation for the card actions that need one: archive lesson, delete lesson, delete roadmap. */
export function CardActionConfirmDialog({
    confirmation,
    isPending,
    error,
    onCancel,
    onConfirm,
}: CardActionConfirmDialogProps) {
    if (!confirmation) {
        return null;
    }

    if (confirmation.kind === "archive-lesson") {
        return (
            <ConfirmActionDialog
                open
                title="Archive lesson?"
                confirmLabel="Archive lesson"
                pendingLabel="Archiving..."
                confirmVariant="secondary"
                isPending={isPending}
                error={error}
                onCancel={onCancel}
                onConfirm={onConfirm}
            >
                <p>
                    This will archive <strong>{confirmation.lesson.title}</strong>.
                </p>
                <p className="card-actions-confirm__note">
                    Archived lessons are hidden from published library views and cannot be added to My Lessons
                    until restored.
                </p>
            </ConfirmActionDialog>
        );
    }

    if (confirmation.kind === "delete-lesson") {
        return (
            <ConfirmActionDialog
                open
                title="Delete lesson?"
                confirmLabel="Delete permanently"
                pendingLabel="Deleting..."
                confirmVariant="danger"
                isPending={isPending}
                error={error}
                onCancel={onCancel}
                onConfirm={onConfirm}
            >
                <p>
                    This action will permanently remove <strong>{confirmation.lesson.title}</strong>.
                </p>
                <p className="card-actions-confirm__note">
                    The lesson will be removed from the library and from every user&apos;s My Lessons.
                </p>
            </ConfirmActionDialog>
        );
    }

    const { roadmap } = confirmation;
    const assigneeCount = roadmap.assigneeCount ?? 0;

    return (
        <ConfirmActionDialog
            open
            title="Delete roadmap?"
            confirmLabel="Delete permanently"
            pendingLabel="Deleting..."
            confirmVariant="danger"
            isPending={isPending}
            error={error}
            onCancel={onCancel}
            onConfirm={onConfirm}
        >
            <p>
                This action will permanently remove <strong>{roadmap.title}</strong>.
            </p>
            {assigneeCount > 0 && (
                <p className="card-actions-confirm__note">
                    Assigned to {assigneeCount} {assigneeCount === 1 ? "person" : "people"}. Their progress on
                    this roadmap will be lost.
                </p>
            )}
            {roadmap.autoAssignNewUsers && (
                <p className="card-actions-confirm__note">
                    New users will no longer get this roadmap automatically.
                </p>
            )}
            <p className="card-actions-confirm__note">Lessons are not deleted.</p>
        </ConfirmActionDialog>
    );
}
