import { useEffect, useRef, useState } from "react";
import ContentCopyOutlinedIcon from "@mui/icons-material/ContentCopyOutlined";
import MoreHorizOutlinedIcon from "@mui/icons-material/MoreHorizOutlined";

import type { DuplicateMenuButtonProps } from "../model/duplicateMenu";

/** Menu width used to right-align the popup under its round ⋯ trigger. */
const MENU_WIDTH = 180;

/**
 * Round ⋯ button at the end of a library card footer that opens a small menu with a single
 * "Duplicate" action. Clicks never reach the card, so they do not open it.
 */
export function DuplicateMenuButton({ label, onDuplicate }: DuplicateMenuButtonProps) {
    const triggerRef = useRef<HTMLButtonElement>(null);
    const menuRef = useRef<HTMLDivElement>(null);
    const [anchor, setAnchor] = useState<DOMRect | null>(null);

    useEffect(() => {
        if (!anchor) {
            return undefined;
        }

        const close = () => setAnchor(null);
        const handlePointerDown = (event: PointerEvent) => {
            const target = event.target;
            if (!(target instanceof Node)) {
                return;
            }
            if (menuRef.current?.contains(target) || triggerRef.current?.contains(target)) {
                return;
            }
            close();
        };
        const handleKeyDown = (event: KeyboardEvent) => {
            if (event.key === "Escape") {
                close();
                triggerRef.current?.focus();
            }
        };

        document.addEventListener("pointerdown", handlePointerDown);
        document.addEventListener("keydown", handleKeyDown);
        window.addEventListener("scroll", close, true);
        return () => {
            document.removeEventListener("pointerdown", handlePointerDown);
            document.removeEventListener("keydown", handleKeyDown);
            window.removeEventListener("scroll", close, true);
        };
    }, [anchor]);

    return (
        <>
            <button
                ref={triggerRef}
                type="button"
                className="library-card__more"
                aria-label={label}
                title="More actions"
                aria-haspopup="menu"
                aria-expanded={anchor !== null}
                onClick={(event) => {
                    event.preventDefault();
                    event.stopPropagation();
                    setAnchor((current) => (current ? null : event.currentTarget.getBoundingClientRect()));
                }}
            >
                <MoreHorizOutlinedIcon fontSize="small" />
            </button>
            {anchor && (
                <div
                    ref={menuRef}
                    className="library-menu"
                    role="menu"
                    style={{
                        position: "fixed",
                        top: anchor.bottom + 4,
                        left: Math.max(8, anchor.right - MENU_WIDTH),
                    }}
                    onClick={(event) => event.stopPropagation()}
                >
                    <button
                        type="button"
                        role="menuitem"
                        className="library-menu__item"
                        autoFocus
                        onClick={() => {
                            setAnchor(null);
                            onDuplicate();
                        }}
                    >
                        <ContentCopyOutlinedIcon fontSize="inherit" className="library-menu__icon" />
                        Duplicate
                    </button>
                </div>
            )}
        </>
    );
}
