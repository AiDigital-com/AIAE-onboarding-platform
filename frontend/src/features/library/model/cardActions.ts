import type { KeyboardEvent, MouseEvent, ReactNode } from "react";

import type { LibraryLesson, LibraryRoadmap } from "../api/types";

/** Every action a library card's "More actions" (⋯) menu can offer. */
export type CardActionId =
    | "edit"
    | "open"
    | "make-public"
    | "make-private"
    | "duplicate"
    | "archive"
    | "delete";

/** One row of the card actions menu. */
export interface CardActionItem {
    id: CardActionId;
    label: string;
    /** Destructive action, rendered in the danger colour. */
    danger?: boolean;
    /** Draws a divider above this item. */
    separated?: boolean;
}

/** What the viewer may do with one lesson card. */
export interface LessonCardActionAccess {
    /** Edit, duplicate and delete: the viewer manages this lesson. */
    canManage: boolean;
    /**
     * Make public / make private, archive and restore. The backend requires both the
     * publish/archive permission and managing the lesson for every one of them.
     */
    canChangeStatus: boolean;
}

/** What the viewer may do with one roadmap card. */
export interface RoadmapCardActionAccess {
    /** Edit and delete: the viewer manages this roadmap. */
    canManage: boolean;
    /** Duplicate: the viewer may create roadmaps. */
    canDuplicate: boolean;
}

/** Where the open menu is anchored, in viewport coordinates. */
export interface CardActionsMenuAnchor {
    /** Id of the card the menu belongs to. */
    itemId: number;
    x: number;
    y: number;
    /** Bottom edge the menu takes when it does not fit below `y` and opens upward instead. */
    flipY: number;
    /** `end` hangs the menu from its right edge (⋯ button); `start` from the cursor. */
    align: "start" | "end";
    /** Element that regains focus when the menu closes from the keyboard. */
    trigger: HTMLElement | null;
}

export interface CardActionsMenuProps {
    anchor: CardActionsMenuAnchor;
    items: CardActionItem[];
    /** Accessible name of the menu, e.g. the card title. */
    label: string;
    onSelect: (actionId: CardActionId) => void;
    onClose: () => void;
}

export interface CardActionsButtonProps {
    /** Accessible name, e.g. `More actions for <title>`. */
    label: string;
    expanded: boolean;
    onClick: (event: MouseEvent<HTMLButtonElement>) => void;
}

/** State and handlers returned by `useCardActionsMenu`. */
export interface CardActionsMenuController {
    anchor: CardActionsMenuAnchor | null;
    toggleFromButton: (event: MouseEvent<HTMLElement>, itemId: number) => void;
    openFromContextMenu: (event: MouseEvent<HTMLElement>, itemId: number) => void;
    close: () => void;
}

export type CardActionsMenuKeyHandler = (event: KeyboardEvent<HTMLDivElement>) => void;

/** A confirmation the user must answer before a card action runs. */
export type CardActionConfirmation =
    | { kind: "archive-lesson"; lesson: LibraryLesson }
    | { kind: "delete-lesson"; lesson: LibraryLesson }
    | { kind: "delete-roadmap"; roadmap: LibraryRoadmap };

export interface CardActionConfirmDialogProps {
    confirmation: CardActionConfirmation | null;
    isPending: boolean;
    error: string;
    onCancel: () => void;
    onConfirm: () => void;
}

export interface ConfirmActionDialogProps {
    open: boolean;
    title: string;
    confirmLabel: string;
    pendingLabel: string;
    /** `danger` for irreversible actions, `secondary` for ones that can be undone. */
    confirmVariant: "danger" | "secondary";
    isPending: boolean;
    error?: string;
    children: ReactNode;
    onCancel: () => void;
    onConfirm: () => void;
}

/** Toast callback shared by the Library page. */
export type CardActionNotify = (
    message: string,
    severity?: "success" | "error" | "warning",
    action?: { label: string; onClick: () => void },
) => void;

export interface UseLibraryCardActionsOptions {
    notify: CardActionNotify;
    getLessonAccess: (lesson: LibraryLesson) => LessonCardActionAccess;
    getRoadmapAccess: (roadmap: LibraryRoadmap) => RoadmapCardActionAccess;
    onOpenLesson: (lesson: LibraryLesson) => void;
    onOpenRoadmap: (roadmap: LibraryRoadmap) => void;
    /** Runs after a lesson is deleted or a roadmap is deleted, e.g. to close an open editor. */
    onLessonDeleted?: (lesson: LibraryLesson) => void;
    onRoadmapDeleted?: (roadmap: LibraryRoadmap) => void;
}

export interface LibraryCardActions {
    getLessonActions: (lesson: LibraryLesson) => CardActionItem[];
    getRoadmapActions: (roadmap: LibraryRoadmap) => CardActionItem[];
    onLessonAction: (lesson: LibraryLesson, actionId: CardActionId) => void;
    onRoadmapAction: (roadmap: LibraryRoadmap, actionId: CardActionId) => void;
    /** Whether the viewer may restore this archived lesson from its card. */
    canRestoreLesson: (lesson: LibraryLesson) => boolean;
    /** Restores an archived lesson right away; the toast offers Undo. */
    onRestoreLesson: (lesson: LibraryLesson) => void;
    /** Opens the delete confirmation for a roadmap (also used by the Edit roadmap dialog). */
    requestDeleteRoadmap: (roadmap: LibraryRoadmap) => void;
    confirmation: CardActionConfirmation | null;
    isConfirming: boolean;
    confirmError: string;
    cancelConfirmation: () => void;
    confirm: () => void;
}
