import { useCallback, useState, type MouseEvent } from "react";

import { CARD_ACTIONS_MENU_OFFSET } from "../constants/cardActions";
import type { CardActionsMenuAnchor, CardActionsMenuController } from "../model/cardActions";

/**
 * Owns which card's ⋯ menu is open and where. One menu is shared by a whole grid, so opening it
 * for another card moves it instead of stacking a second one.
 */
export function useCardActionsMenu(): CardActionsMenuController {
    const [anchor, setAnchor] = useState<CardActionsMenuAnchor | null>(null);

    const close = useCallback(() => setAnchor(null), []);

    const toggleFromButton = useCallback((event: MouseEvent<HTMLElement>, itemId: number) => {
        // The button sits inside a clickable card: keep the click from opening the card.
        event.preventDefault();
        event.stopPropagation();
        const trigger = event.currentTarget;
        const rect = trigger.getBoundingClientRect();
        setAnchor((current) =>
            current?.itemId === itemId && current.trigger === trigger
                ? null
                : {
                      itemId,
                      x: rect.right,
                      y: rect.bottom + CARD_ACTIONS_MENU_OFFSET,
                      flipY: rect.top - CARD_ACTIONS_MENU_OFFSET,
                      align: "end",
                      trigger,
                  },
        );
    }, []);

    const openFromContextMenu = useCallback((event: MouseEvent<HTMLElement>, itemId: number) => {
        // Links, controls and selected text keep the browser's own menu (open in new tab, copy).
        const target = event.target;
        const isNativeTarget = target instanceof Element && Boolean(target.closest("a, button, input, textarea"));
        if (isNativeTarget || window.getSelection()?.toString()) {
            return;
        }
        event.preventDefault();
        setAnchor({
            itemId,
            x: event.clientX,
            y: event.clientY,
            flipY: event.clientY,
            align: "start",
            trigger: null,
        });
    }, []);

    return { anchor, toggleFromButton, openFromContextMenu, close };
}
