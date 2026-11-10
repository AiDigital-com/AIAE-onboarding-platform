import { useEffect, useRef, useState, type FormEvent } from "react";
import { Dialog } from "@/shared/ui/Dialog";
import { Button } from "@/shared/ui/Button";
import { clampEntityTitle, MAX_ENTITY_TITLE_LENGTH } from "@/shared/lib/entityTitle";

interface DuplicateLessonDialogProps {
    open: boolean;
    /** Title of the lesson being duplicated; the name field is pre-filled as "Copy of <title>". */
    lessonTitle: string;
    isSaving?: boolean;
    error?: string;
    onCancel: () => void;
    onConfirm: (title: string) => void;
}

/** Small dialog that names the copy of a lesson before it is created. */
export function DuplicateLessonDialog({
    open,
    lessonTitle,
    isSaving = false,
    error = "",
    onCancel,
    onConfirm,
}: DuplicateLessonDialogProps) {
    const [title, setTitle] = useState("");
    const inputRef = useRef<HTMLInputElement>(null);

    // Re-seed and pre-select the name every time the dialog opens, so typing replaces it.
    useEffect(() => {
        if (!open) {
            return;
        }
        setTitle(clampEntityTitle(`Copy of ${lessonTitle}`));
        const frame = window.requestAnimationFrame(() => {
            inputRef.current?.focus();
            inputRef.current?.select();
        });
        return () => window.cancelAnimationFrame(frame);
    }, [open, lessonTitle]);

    const trimmedTitle = title.trim();
    const canSubmit = trimmedTitle.length > 0 && !isSaving;

    const handleSubmit = (event: FormEvent) => {
        event.preventDefault();
        if (canSubmit) {
            onConfirm(trimmedTitle);
        }
    };

    return (
        <Dialog
            open={open}
            onClose={isSaving ? undefined : onCancel}
            size="sm"
            title="Duplicate lesson"
            closeDisabled={isSaving}
            footer={
                <>
                    <Button variant="ghost" onClick={onCancel} disabled={isSaving}>
                        Cancel
                    </Button>
                    <Button variant="primary" onClick={() => canSubmit && onConfirm(trimmedTitle)} disabled={!canSubmit}>
                        {isSaving ? "Duplicating…" : "Duplicate"}
                    </Button>
                </>
            }
        >
            <form className="library-form-body" onSubmit={handleSubmit}>
                <label className="library-field">
                    <span className="library-field__label">NAME</span>
                    <input
                        ref={inputRef}
                        className="library-field__control"
                        value={title}
                        maxLength={MAX_ENTITY_TITLE_LENGTH}
                        disabled={isSaving}
                        onChange={(event) => setTitle(event.target.value)}
                        aria-label="Name of the copy"
                    />
                </label>
                <p className="library-duplicate-note">
                    <b>Copied:</b> reading content, flashcards, quiz, sources, assets, tags.
                    <br />
                    <b>Not copied:</b> assignees and learner progress. The copy is Ready and Private.
                </p>
                {error && <p className="library-alert library-alert--error">{error}</p>}
            </form>
        </Dialog>
    );
}
