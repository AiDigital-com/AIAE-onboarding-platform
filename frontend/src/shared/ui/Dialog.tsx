import { useEffect, type ReactNode } from "react";
import CloseOutlinedIcon from "@mui/icons-material/CloseOutlined";
import "./dialog.css";

type DialogSize = "sm" | "md" | "lg";

interface DialogProps {
    open: boolean;
    onClose?: () => void;
    title?: ReactNode;
    children: ReactNode;
    footer?: ReactNode;
    size?: DialogSize;
    richHeader?: boolean;
    flushBody?: boolean;
    whiteFooter?: boolean;
    closeDisabled?: boolean;
    closeLabel?: string;
    className?: string;
}

export function Dialog({
    open,
    onClose,
    title,
    children,
    footer,
    size = "md",
    richHeader = false,
    flushBody = false,
    whiteFooter = false,
    closeDisabled = false,
    closeLabel = "Close dialog",
    className = "",
}: DialogProps) {
    useEffect(() => {
        if (!open) {
            return undefined;
        }

        const previousOverflow = document.body.style.overflow;
        document.body.style.overflow = "hidden";

        const handleKeyDown = (event: KeyboardEvent) => {
            if (event.key === "Escape" && onClose && !closeDisabled) {
                onClose();
            }
        };

        window.addEventListener("keydown", handleKeyDown);

        return () => {
            document.body.style.overflow = previousOverflow;
            window.removeEventListener("keydown", handleKeyDown);
        };
    }, [closeDisabled, onClose, open]);

    if (!open) {
        return null;
    }

    return (
        <div
            className="ui-dialog__backdrop"
            role="presentation"
            onMouseDown={(event) => {
                if (event.target === event.currentTarget && onClose && !closeDisabled) {
                    onClose();
                }
            }}
        >
            <div
                className={["ui-dialog", `ui-dialog--${size}`, className].filter(Boolean).join(" ")}
                role="dialog"
                aria-modal="true"
            >
                {(title || onClose) && (
                    <div
                        className={[
                            "ui-dialog__header",
                            richHeader ? "ui-dialog__header--rich" : "",
                        ]
                            .filter(Boolean)
                            .join(" ")}
                    >
                        {title}
                        {onClose && (
                            <button
                                type="button"
                                className="ui-dialog__close"
                                aria-label={closeLabel}
                                onClick={onClose}
                                disabled={closeDisabled}
                            >
                                <CloseOutlinedIcon />
                            </button>
                        )}
                    </div>
                )}
                <div
                    className={[
                        "ui-dialog__body",
                        flushBody ? "ui-dialog__body--flush" : "",
                    ]
                        .filter(Boolean)
                        .join(" ")}
                >
                    {children}
                </div>
                {footer && (
                    <div
                        className={[
                            "ui-dialog__footer",
                            whiteFooter ? "ui-dialog__footer--white" : "",
                        ]
                            .filter(Boolean)
                            .join(" ")}
                    >
                        {footer}
                    </div>
                )}
            </div>
        </div>
    );
}
