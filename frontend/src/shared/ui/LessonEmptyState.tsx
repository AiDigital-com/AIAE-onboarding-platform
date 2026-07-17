import "./lesson-empty-state.css";

interface Props {
    title: string;
    description: string;
    actionLabel?: string;
    onAction?: () => void;
}

/** Card-style empty state matching the source My Lessons / grid empty blocks. */
export function LessonEmptyState({ title, description, actionLabel, onAction }: Props) {
    return (
        <article className="lesson-empty-state">
            <div className="lesson-empty-state__copy">
                <h3 className="lesson-empty-state__title">{title}</h3>
                <p className="lesson-empty-state__description">{description}</p>
            </div>
            {actionLabel && onAction && (
                <button type="button" className="lesson-empty-state__action" onClick={onAction}>
                    {actionLabel}
                </button>
            )}
        </article>
    );
}
