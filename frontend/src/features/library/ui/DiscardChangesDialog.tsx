import { Dialog } from "@/shared/ui/Dialog";
import { Button } from "@/shared/ui/Button";

interface DiscardChangesDialogProps {
    open: boolean;
    onKeepEditing: () => void;
    onDiscard: () => void;
    disabled?: boolean;
}

/** Warn before leaving an edit surface when unsaved changes exist. */
export function DiscardChangesDialog({
    open,
    onKeepEditing,
    onDiscard,
    disabled = false,
}: DiscardChangesDialogProps) {
    return (
        <Dialog
            open={open}
            onClose={disabled ? undefined : onKeepEditing}
            size="sm"
            title="Discard changes?"
            flushBody
            closeDisabled={disabled}
            closeLabel="Keep editing"
            footer={
                <>
                    <Button variant="ghost" onClick={onKeepEditing} disabled={disabled}>
                        Keep editing
                    </Button>
                    <Button variant="danger" onClick={onDiscard} disabled={disabled}>
                        Discard changes
                    </Button>
                </>
            }
        >
            <div className="library-form-body">
                <p>
                    You&apos;ve made changes that haven&apos;t been saved. If you leave now, your edits will be
                    lost.
                </p>
            </div>
        </Dialog>
    );
}
