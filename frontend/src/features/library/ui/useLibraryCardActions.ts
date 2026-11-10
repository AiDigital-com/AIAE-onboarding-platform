import { useRef, useState } from "react";

import type { LibraryLesson, LibraryRoadmap } from "../api/types";
import { useChangeLessonStatusMutation, useDeleteLessonMutation } from "../api/useLessonMutations";
import { useDeleteRoadmapMutation } from "../api/useRoadmapMutations";
import { CARD_ACTION_MESSAGES } from "../constants/cardActions";
import { getLessonCardActions, getRoadmapCardActions, isLessonArchived } from "../lib/cardActions";
import type {
    CardActionConfirmation,
    CardActionId,
    LibraryCardActions,
    UseLibraryCardActionsOptions,
} from "../model/cardActions";

function errorMessage(error: unknown, fallback: string): string {
    return error instanceof Error ? error.message : fallback;
}

/**
 * Behaviour behind the ⋯ menus of library lesson and roadmap cards: which items a card offers,
 * what each one does, and the confirmation that archive and delete go through first.
 */
export function useLibraryCardActions({
    notify,
    getLessonAccess,
    getRoadmapAccess,
    onOpenLesson,
    onOpenRoadmap,
    onLessonDeleted,
    onRoadmapDeleted,
}: UseLibraryCardActionsOptions): LibraryCardActions {
    const changeLessonStatus = useChangeLessonStatusMutation();
    const deleteLesson = useDeleteLessonMutation();
    const deleteRoadmap = useDeleteRoadmapMutation();
    const [confirmation, setConfirmation] = useState<CardActionConfirmation | null>(null);
    const [confirmError, setConfirmError] = useState("");

    // Only the confirmed action itself: an unrelated status change (make public, restore, undo)
    // still in flight must not freeze a confirmation the user has just opened.
    const [isConfirming, setIsConfirming] = useState(false);
    // Lessons with an immediate status change in flight, so a double click sends one request.
    const busyLessonIds = useRef(new Set<number>());

    const runOncePerLesson = async (lessonId: number, task: () => Promise<void>) => {
        if (busyLessonIds.current.has(lessonId)) {
            return;
        }
        busyLessonIds.current.add(lessonId);
        try {
            await task();
        } finally {
            busyLessonIds.current.delete(lessonId);
        }
    };

    const openConfirmation = (next: CardActionConfirmation) => {
        setConfirmError("");
        setConfirmation(next);
    };

    const cancelConfirmation = () => {
        if (isConfirming) {
            return;
        }
        setConfirmation(null);
        setConfirmError("");
    };

    /** Applies immediately, without a confirmation; the toast offers Undo. */
    const changeLessonVisibility = async (lesson: LibraryLesson, makePublic: boolean, offerUndo: boolean) => {
        try {
            await changeLessonStatus.mutateAsync({
                id: lesson.id,
                payload: { action: makePublic ? "publish" : "unpublish" },
            });
            notify(
                makePublic ? CARD_ACTION_MESSAGES.lessonMadePublic : CARD_ACTION_MESSAGES.lessonMadePrivate,
                "success",
                offerUndo
                    ? {
                          label: CARD_ACTION_MESSAGES.undo,
                          onClick: () => void changeLessonVisibility(lesson, !makePublic, false),
                      }
                    : undefined,
            );
        } catch (error) {
            notify(errorMessage(error, CARD_ACTION_MESSAGES.lessonVisibilityFailed), "error");
        }
    };

    /** Archive and restore undo each other, so each toast offers the opposite action once. */
    const setLessonArchived = async (lesson: LibraryLesson, archived: boolean, offerUndo: boolean) => {
        await changeLessonStatus.mutateAsync({
            id: lesson.id,
            payload: { action: archived ? "archive" : "restore" },
        });
        notify(
            archived ? CARD_ACTION_MESSAGES.lessonArchived : CARD_ACTION_MESSAGES.lessonRestored,
            "success",
            offerUndo
                ? {
                      label: CARD_ACTION_MESSAGES.undo,
                      onClick: () => void applyLessonArchived(lesson, !archived, false),
                  }
                : undefined,
        );
    };

    /** Applies immediately, without a confirmation: restore, and the Undo of archive/restore. */
    const applyLessonArchived = async (lesson: LibraryLesson, archived: boolean, offerUndo: boolean) => {
        try {
            await setLessonArchived(lesson, archived, offerUndo);
        } catch (error) {
            notify(
                errorMessage(
                    error,
                    archived ? CARD_ACTION_MESSAGES.lessonArchiveFailed : CARD_ACTION_MESSAGES.lessonRestoreFailed,
                ),
                "error",
            );
        }
    };

    const confirm = async () => {
        if (!confirmation || isConfirming) {
            return;
        }

        try {
            setConfirmError("");
            setIsConfirming(true);
            if (confirmation.kind === "archive-lesson") {
                await setLessonArchived(confirmation.lesson, true, true);
            } else if (confirmation.kind === "delete-lesson") {
                await deleteLesson.mutateAsync(confirmation.lesson.id);
                onLessonDeleted?.(confirmation.lesson);
                notify(CARD_ACTION_MESSAGES.lessonDeleted);
            } else {
                await deleteRoadmap.mutateAsync(confirmation.roadmap.id);
                onRoadmapDeleted?.(confirmation.roadmap);
                notify(CARD_ACTION_MESSAGES.roadmapDeleted);
            }
            setConfirmation(null);
        } catch (error) {
            const fallback =
                confirmation.kind === "archive-lesson"
                    ? CARD_ACTION_MESSAGES.lessonArchiveFailed
                    : confirmation.kind === "delete-lesson"
                      ? CARD_ACTION_MESSAGES.lessonDeleteFailed
                      : CARD_ACTION_MESSAGES.roadmapDeleteFailed;
            setConfirmError(errorMessage(error, fallback));
        } finally {
            setIsConfirming(false);
        }
    };

    const onLessonAction = (lesson: LibraryLesson, actionId: CardActionId) => {
        switch (actionId) {
            case "edit":
            case "open":
                onOpenLesson(lesson);
                break;
            case "make-public":
                void runOncePerLesson(lesson.id, () => changeLessonVisibility(lesson, true, true));
                break;
            case "make-private":
                void runOncePerLesson(lesson.id, () => changeLessonVisibility(lesson, false, true));
                break;
            case "duplicate":
                // Placeholder until lesson duplication ships.
                notify(CARD_ACTION_MESSAGES.lessonDuplicateUnavailable, "warning");
                break;
            case "archive":
                openConfirmation({ kind: "archive-lesson", lesson });
                break;
            case "delete":
                openConfirmation({ kind: "delete-lesson", lesson });
                break;
        }
    };

    const requestDeleteRoadmap = (roadmap: LibraryRoadmap) => {
        openConfirmation({ kind: "delete-roadmap", roadmap });
    };

    const onRoadmapAction = (roadmap: LibraryRoadmap, actionId: CardActionId) => {
        switch (actionId) {
            case "edit":
                onOpenRoadmap(roadmap);
                break;
            case "duplicate":
                // Placeholder until roadmap duplication ships.
                notify(CARD_ACTION_MESSAGES.roadmapDuplicateUnavailable, "warning");
                break;
            case "delete":
                requestDeleteRoadmap(roadmap);
                break;
            default:
                break;
        }
    };

    return {
        getLessonActions: (lesson) => getLessonCardActions(lesson, getLessonAccess(lesson)),
        getRoadmapActions: (roadmap) => getRoadmapCardActions(roadmap, getRoadmapAccess(roadmap)),
        onLessonAction,
        onRoadmapAction,
        canRestoreLesson: (lesson) => {
            const access = getLessonAccess(lesson);
            return isLessonArchived(lesson) && access.canManage && access.canChangeStatus;
        },
        onRestoreLesson: (lesson) =>
            void runOncePerLesson(lesson.id, () => applyLessonArchived(lesson, false, true)),
        requestDeleteRoadmap,
        confirmation,
        isConfirming,
        confirmError,
        cancelConfirmation,
        confirm: () => void confirm(),
    };
}
