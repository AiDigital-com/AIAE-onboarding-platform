import { Dialog } from "@/shared/ui/Dialog";
import { LessonPromptForm } from "./LessonPromptForm";
import type { LibraryLesson, LibraryMaterial } from "../api/types";

interface CreateLessonDialogProps {
    open: boolean;
    materials: LibraryMaterial[];
    onClose: () => void;
    onLessonGenerated: (lesson: LibraryLesson) => void | Promise<void>;
}

export function CreateLessonDialog({
    open,
    materials,
    onClose,
    onLessonGenerated,
}: CreateLessonDialogProps) {
    return (
        <Dialog
            open={open}
            onClose={onClose}
            size="lg"
            flushBody
            title="Create lesson"
            closeLabel="Close create lesson dialog"
        >
            <LessonPromptForm
                materials={materials}
                onLessonGenerated={onLessonGenerated}
                onLessonGenerationStarted={onClose}
            />
        </Dialog>
    );
}
