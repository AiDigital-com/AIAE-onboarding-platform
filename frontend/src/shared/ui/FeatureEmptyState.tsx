import "./feature-empty-state.css";

interface Props {
    title: string;
    description: string;
    actionLabel?: string;
    onAction?: () => void;
}

/** Card-style empty state matching the source MUI EmptyState copy layout. */
export function FeatureEmptyState({ title, description, actionLabel, onAction }: Props) {
    return (
        <section className="feature-empty-state">
            <div className="feature-empty-state__copy">
                <h2 className="feature-empty-state__title">{title}</h2>
                <p className="feature-empty-state__description">{description}</p>
            </div>
            {actionLabel && onAction && (
                <button type="button" className="feature-empty-state__action" onClick={onAction}>
                    {actionLabel}
                </button>
            )}
        </section>
    );
}
