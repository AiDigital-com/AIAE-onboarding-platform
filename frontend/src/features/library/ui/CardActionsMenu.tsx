import { useEffect, useLayoutEffect, useRef, useState, type CSSProperties, type KeyboardEvent } from "react";

import ArchiveOutlinedIcon from "@mui/icons-material/ArchiveOutlined";
import ContentCopyOutlinedIcon from "@mui/icons-material/ContentCopyOutlined";
import DeleteOutlineOutlinedIcon from "@mui/icons-material/DeleteOutlineOutlined";
import EditOutlinedIcon from "@mui/icons-material/EditOutlined";
import LockOutlinedIcon from "@mui/icons-material/LockOutlined";
import MoreHorizOutlinedIcon from "@mui/icons-material/MoreHorizOutlined";
import PublicOutlinedIcon from "@mui/icons-material/PublicOutlined";
import VisibilityOutlinedIcon from "@mui/icons-material/VisibilityOutlined";

import { cn } from "@/shared/lib/cn";
import { CARD_ACTIONS_MENU_VIEWPORT_MARGIN } from "../constants/cardActions";
import type { CardActionId, CardActionsButtonProps, CardActionsMenuProps } from "../model/cardActions";
import "./card-actions-menu.css";

function CardActionIcon({ actionId, danger }: { actionId: CardActionId; danger?: boolean }) {
    const className = cn("card-actions-menu__icon", danger && "card-actions-menu__icon--danger");
    switch (actionId) {
        case "edit":
            return <EditOutlinedIcon className={className} />;
        case "open":
            return <VisibilityOutlinedIcon className={className} />;
        case "make-public":
            return <PublicOutlinedIcon className={className} />;
        case "make-private":
            return <LockOutlinedIcon className={className} />;
        case "duplicate":
            return <ContentCopyOutlinedIcon className={className} />;
        case "archive":
            return <ArchiveOutlinedIcon className={className} />;
        case "delete":
            return <DeleteOutlineOutlinedIcon className={className} />;
    }
}

/** Round "More actions" (⋯) button placed at the far right of a library card footer. */
export function CardActionsButton({ label, expanded, onClick }: CardActionsButtonProps) {
    return (
        <button
            type="button"
            className={cn("card-actions-button", expanded && "card-actions-button--expanded")}
            aria-label={label}
            aria-haspopup="menu"
            aria-expanded={expanded}
            title="More actions"
            data-card-actions-trigger
            onClick={onClick}
        >
            <MoreHorizOutlinedIcon className="card-actions-button__icon" />
        </button>
    );
}

/**
 * Popup menu for a library card, opened from its ⋯ button or by right-clicking the card. Closes
 * on outside press, Esc, scroll, resize, or after an item is chosen; arrow keys, Home and End
 * move between items.
 */
export function CardActionsMenu({ anchor, items, label, onSelect, onClose }: CardActionsMenuProps) {
    const menuRef = useRef<HTMLDivElement>(null);
    const [position, setPosition] = useState({ left: anchor.x, top: anchor.y });
    // Read through a ref so the listeners below are not re-bound on every parent render.
    const onCloseRef = useRef(onClose);
    onCloseRef.current = onClose;

    // Hang the menu below the anchor; open it upward when it does not fit there, so it never
    // covers the button it came from. Then keep it inside the viewport.
    useLayoutEffect(() => {
        const menu = menuRef.current;
        if (!menu) {
            return;
        }
        const { width, height } = menu.getBoundingClientRect();
        const margin = CARD_ACTIONS_MENU_VIEWPORT_MARGIN;
        const preferredLeft = anchor.align === "end" ? anchor.x - width : anchor.x;
        const maxLeft = window.innerWidth - width - margin;
        const maxTop = window.innerHeight - height - margin;
        const fitsBelow = anchor.y <= maxTop;
        const preferredTop = fitsBelow ? anchor.y : anchor.flipY - height;
        setPosition({
            left: Math.max(margin, Math.min(preferredLeft, maxLeft)),
            top: Math.max(margin, Math.min(preferredTop, maxTop)),
        });
        menu.querySelector<HTMLElement>("[role='menuitem']")?.focus();
    }, [anchor]);

    useEffect(() => {
        const handlePointerDown = (event: PointerEvent) => {
            const target = event.target;
            if (!(target instanceof Element)) {
                return;
            }
            // The trigger toggles the menu itself; closing here would reopen it on click.
            if (menuRef.current?.contains(target) || target.closest("[data-card-actions-trigger]")) {
                return;
            }
            onCloseRef.current();
        };
        const handleDismiss = () => onCloseRef.current();

        document.addEventListener("pointerdown", handlePointerDown);
        window.addEventListener("scroll", handleDismiss, true);
        window.addEventListener("resize", handleDismiss);
        return () => {
            document.removeEventListener("pointerdown", handlePointerDown);
            window.removeEventListener("scroll", handleDismiss, true);
            window.removeEventListener("resize", handleDismiss);
        };
    }, []);

    const closeAndRestoreFocus = () => {
        const trigger = anchor.trigger;
        onClose();
        trigger?.focus();
    };

    const handleKeyDown = (event: KeyboardEvent<HTMLDivElement>) => {
        const menuItems = Array.from(
            menuRef.current?.querySelectorAll<HTMLElement>("[role='menuitem']") ?? [],
        );
        const currentIndex = menuItems.indexOf(document.activeElement as HTMLElement);
        const focusAt = (index: number) => {
            event.preventDefault();
            menuItems[(index + menuItems.length) % menuItems.length]?.focus();
        };

        switch (event.key) {
            case "Escape":
                event.preventDefault();
                event.stopPropagation();
                closeAndRestoreFocus();
                break;
            case "Tab":
                onClose();
                break;
            case "ArrowDown":
                focusAt(currentIndex + 1);
                break;
            case "ArrowUp":
                focusAt(currentIndex - 1);
                break;
            case "Home":
                focusAt(0);
                break;
            case "End":
                focusAt(menuItems.length - 1);
                break;
            default:
                break;
        }
    };

    return (
        <div
            ref={menuRef}
            className="card-actions-menu"
            role="menu"
            aria-label={label}
            style={
                {
                    "--card-actions-menu-left": `${position.left}px`,
                    "--card-actions-menu-top": `${position.top}px`,
                } as CSSProperties
            }
            onKeyDown={handleKeyDown}
            onContextMenu={(event) => event.preventDefault()}
        >
            {items.map((item) => (
                <button
                    key={item.id}
                    type="button"
                    role="menuitem"
                    className={cn(
                        "card-actions-menu__item",
                        item.danger && "card-actions-menu__item--danger",
                        item.separated && "card-actions-menu__item--separated",
                    )}
                    onClick={() => {
                        // Hand focus back first; an action that opens a dialog then moves it on.
                        anchor.trigger?.focus();
                        onClose();
                        onSelect(item.id);
                    }}
                >
                    <CardActionIcon actionId={item.id} danger={item.danger} />
                    <span className="card-actions-menu__label">{item.label}</span>
                </button>
            ))}
        </div>
    );
}
