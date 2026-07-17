import { useEffect, type ReactNode } from "react";
import CloseOutlinedIcon from "@mui/icons-material/CloseOutlined";
import "./drawer.css";

interface DrawerProps {
    open: boolean;
    onClose: () => void;
    title: ReactNode;
    description?: ReactNode;
    children: ReactNode;
    footer?: ReactNode;
    size?: "md" | "lg";
    closeLabel?: string;
}

/** Right-anchored slide-out panel for secondary settings that should not compete with primary content. */
export function Drawer({
    open,
    onClose,
    title,
    description,
    children,
    footer,
    size = "md",
    closeLabel = "Close panel",
}: DrawerProps) {
    useEffect(() => {
        if (!open) {
            return undefined;
        }

        const previousOverflow = document.body.style.overflow;
        document.body.style.overflow = "hidden";

        const handleKeyDown = (event: KeyboardEvent) => {
            if (event.key === "Escape") {
                onClose();
            }
        };

        window.addEventListener("keydown", handleKeyDown);

        return () => {
            document.body.style.overflow = previousOverflow;
            window.removeEventListener("keydown", handleKeyDown);
        };
    }, [onClose, open]);

    if (!open) {
        return null;
    }

    return (
        <div
            className="ui-drawer__backdrop"
            role="presentation"
            onMouseDown={(event) => {
                if (event.target === event.currentTarget) {
                    onClose();
                }
            }}
        >
            <aside className={`ui-drawer ui-drawer--${size}`} role="dialog" aria-modal="true">
                <div className="ui-drawer__header">
                    <div>
                        <h2 className="ui-drawer__title">{title}</h2>
                        {description && <p className="ui-drawer__description">{description}</p>}
                    </div>
                    <button type="button" className="ui-drawer__close" aria-label={closeLabel} onClick={onClose}>
                        <CloseOutlinedIcon />
                    </button>
                </div>
                <div className="ui-drawer__body">{children}</div>
                {footer && <div className="ui-drawer__footer">{footer}</div>}
            </aside>
        </div>
    );
}
