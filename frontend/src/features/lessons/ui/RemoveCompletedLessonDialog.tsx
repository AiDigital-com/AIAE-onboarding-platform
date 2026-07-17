import type { EnrolledLessonCard } from "../api/types";
import "./remove-completed-lesson-dialog.css";

interface Props {
    lesson: EnrolledLessonCard | null;
    onCancel: () => void;
    onConfirm: () => void;
}

export function RemoveCompletedLessonDialog({ lesson, onCancel, onConfirm }: Props) {
    if (!lesson) {
        return null;
    }

    return (
        <div className="remove-lesson-dialog" role="dialog" aria-modal="true" aria-labelledby="remove-lesson-title">
            <div className="remove-lesson-dialog__backdrop" onClick={onCancel} />
            <div className="remove-lesson-dialog__panel">
                <h2 id="remove-lesson-title" className="remove-lesson-dialog__title">
                    Remove completed lesson?
                </h2>
                <div className="remove-lesson-dialog__body">
                    <p>This lesson is already completed.</p>
                    <p className="remove-lesson-dialog__muted">
                        If you remove it from My Lessons, its completion status will be removed too.
                        Roadmaps that include this lesson will roll back their progress.
                    </p>
                    {lesson.title && <p className="remove-lesson-dialog__lesson-title">{lesson.title}</p>}
                </div>
                <div className="remove-lesson-dialog__actions">
                    <button type="button" className="remove-lesson-dialog__btn" onClick={onCancel}>
                        Cancel
                    </button>
                    <button
                        type="button"
                        className="remove-lesson-dialog__btn remove-lesson-dialog__btn--danger"
                        onClick={onConfirm}
                    >
                        Remove lesson
                    </button>
                </div>
            </div>
        </div>
    );
}
