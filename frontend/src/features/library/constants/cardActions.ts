import type { CardActionItem } from "../model/cardActions";

/** Gap between the ⋯ button (or cursor) and the menu, and the minimum distance to the viewport edge. */
export const CARD_ACTIONS_MENU_OFFSET = 6;
export const CARD_ACTIONS_MENU_VIEWPORT_MARGIN = 8;

/** A toast that carries an action (Undo) stays long enough to be read and used. */
export const ACTION_TOAST_DURATION_MS = 6000;

export const LESSON_ACTION_EDIT: CardActionItem = { id: "edit", label: "Edit lesson" };
export const LESSON_ACTION_OPEN: CardActionItem = { id: "open", label: "Open lesson" };
export const LESSON_ACTION_MAKE_PUBLIC: CardActionItem = { id: "make-public", label: "Make public" };
export const LESSON_ACTION_MAKE_PRIVATE: CardActionItem = { id: "make-private", label: "Make private" };
export const LESSON_ACTION_ARCHIVE: CardActionItem = { id: "archive", label: "Archive" };

export const ROADMAP_ACTION_EDIT: CardActionItem = { id: "edit", label: "Edit roadmap" };

export const CARD_ACTION_DUPLICATE: CardActionItem = { id: "duplicate", label: "Duplicate" };
/** The trailing ellipsis tells the user a confirmation follows. */
export const CARD_ACTION_DELETE: CardActionItem = { id: "delete", label: "Delete…", danger: true };

export const CARD_ACTION_MESSAGES = {
    lessonDuplicateUnavailable: "Duplicating lessons is coming soon.",
    roadmapDuplicateUnavailable: "Duplicating roadmaps is coming soon.",
    lessonMadePublic: "Lesson is now public.",
    lessonMadePrivate: "Lesson is now private.",
    lessonVisibilityFailed: "Failed to change lesson visibility.",
    lessonArchived: "Lesson archived.",
    lessonRestored: "Lesson restored.",
    lessonArchiveFailed: "Failed to archive lesson.",
    lessonRestoreFailed: "Failed to restore lesson.",
    lessonDeleted: "Lesson deleted successfully.",
    lessonDeleteFailed: "Failed to delete lesson.",
    roadmapDeleted: "Roadmap deleted successfully.",
    roadmapDeleteFailed: "Failed to delete roadmap.",
    undo: "Undo",
} as const;
