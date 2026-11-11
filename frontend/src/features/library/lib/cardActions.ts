import type { LibraryLesson, LibraryRoadmap } from "../api/types";
import {
    CARD_ACTION_DELETE,
    CARD_ACTION_DUPLICATE,
    LESSON_ACTION_ARCHIVE,
    LESSON_ACTION_EDIT,
    LESSON_ACTION_MAKE_PRIVATE,
    LESSON_ACTION_MAKE_PUBLIC,
    LESSON_ACTION_OPEN,
    ROADMAP_ACTION_EDIT,
} from "../constants/cardActions";
import type { CardActionItem, LessonCardActionAccess, RoadmapCardActionAccess } from "../model/cardActions";

/** Marks the first item of a trailing group so the menu draws a divider above it. */
function withDivider(leading: CardActionItem[], trailing: CardActionItem[]): CardActionItem[] {
    if (leading.length === 0 || trailing.length === 0) {
        return [...leading, ...trailing];
    }
    const [first, ...rest] = trailing;
    return [...leading, { ...first, separated: true }, ...rest];
}

export function isLessonArchived(lesson: LibraryLesson): boolean {
    return Boolean(lesson.isArchived) || lesson.publicationStatus === "archived";
}

/**
 * Items of a lesson card's ⋯ menu, by lesson state:
 * archived → Open, Duplicate | Delete; ready → Edit, Make public/private, Duplicate | Archive,
 * Delete; failed → Delete. A lesson that is still being generated (including the backend's
 * short-lived pre-generation state) has no actions. An empty list means no ⋯ button.
 */
export function getLessonCardActions(lesson: LibraryLesson, access: LessonCardActionAccess): CardActionItem[] {
    if (lesson.status !== "ready" && lesson.status !== "failed") {
        return [];
    }

    if (isLessonArchived(lesson)) {
        return access.canManage
            ? withDivider([LESSON_ACTION_OPEN, CARD_ACTION_DUPLICATE], [CARD_ACTION_DELETE])
            : [];
    }

    if (lesson.status === "failed") {
        return access.canManage ? [CARD_ACTION_DELETE] : [];
    }

    const leading: CardActionItem[] = [];
    if (access.canManage) {
        leading.push(LESSON_ACTION_EDIT);
    }
    if (access.canManage && access.canChangeStatus) {
        leading.push(lesson.isPublished ? LESSON_ACTION_MAKE_PRIVATE : LESSON_ACTION_MAKE_PUBLIC);
    }
    if (access.canManage) {
        leading.push(CARD_ACTION_DUPLICATE);
    }

    if (!access.canManage) {
        return [];
    }

    return withDivider(
        leading,
        access.canChangeStatus ? [LESSON_ACTION_ARCHIVE, CARD_ACTION_DELETE] : [CARD_ACTION_DELETE],
    );
}

/**
 * Items of a roadmap card's ⋯ menu: Edit, Duplicate | Delete for a roadmap the viewer manages;
 * Duplicate only for a view-only roadmap.
 */
export function getRoadmapCardActions(roadmap: LibraryRoadmap, access: RoadmapCardActionAccess): CardActionItem[] {
    const canManage = access.canManage && Boolean(roadmap.viewerCanManage);
    const leading: CardActionItem[] = [];
    if (canManage) {
        leading.push(ROADMAP_ACTION_EDIT);
    }
    if (access.canDuplicate) {
        leading.push(CARD_ACTION_DUPLICATE);
    }

    return withDivider(leading, canManage ? [CARD_ACTION_DELETE] : []);
}
