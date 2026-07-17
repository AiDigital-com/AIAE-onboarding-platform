import "./ui-states.css";

interface LoadingSpinnerProps {
    label?: string;
    size?: "sm" | "md" | "lg";
}

/** Reusable loading spinner with an accessible, non-visible status label. */
export function LoadingSpinner({ label = "Loading", size = "md" }: LoadingSpinnerProps) {
    return (
        <span
            className={`ui-loading-spinner ui-loading-spinner--${size}`}
            role="status"
            aria-label={label}
        >
            <span className="ui-loading-spinner__dot" aria-hidden="true" />
        </span>
    );
}
