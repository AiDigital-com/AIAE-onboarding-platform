import "./ui-states.css";
import { LoadingSpinner } from "./LoadingSpinner";

interface Props {
    label?: string;
}

/** Centered loading indicator for async/query states. */
export function LoadingBlock({ label = "Loading…" }: Props) {
    return (
        <div className="ui-state ui-state--loading" aria-live="polite">
            <LoadingSpinner label={label} size="lg" />
        </div>
    );
}
