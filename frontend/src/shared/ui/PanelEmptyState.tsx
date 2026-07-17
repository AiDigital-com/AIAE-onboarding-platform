import { Button } from "./Button";
import "./panel-empty-state.css";

interface PanelEmptyStateProps {
    title: string;
    description: string;
    actionLabel?: string;
    onAction?: () => void;
}

/** Rich empty state card used on library tabs. */
export function PanelEmptyState({
    title,
    description,
    actionLabel,
    onAction,
}: PanelEmptyStateProps) {
    return (
        <section className="panel-empty-state">
            <div className="panel-empty-state__copy">
                <h2 className="panel-empty-state__title">{title}</h2>
                <p className="panel-empty-state__description">{description}</p>
            </div>
            {actionLabel && onAction && (
                <Button variant="ghost" onClick={onAction}>
                    {actionLabel}
                </Button>
            )}
        </section>
    );
}
