import type { LessonActivityV1 } from "@/features/lessons/api/types";

export const PASSING_SCORE = 80;

export const ROADMAPS_HREF = "/roadmaps";
export const CONTINUE_PATH_LABEL = "Continue path";
export const COMPLETE_ROADMAP_LABEL = "Complete roadmap";

/** Roadmap position of the lesson being viewed; `null` when the lesson is standalone. */
export interface ContinueRoadmapContext {
    nextLessonId?: number | null;
}

export interface ContinuePathTarget {
    href: string;
    label: string;
}

/**
 * A quiz only counts as complete once it is both finished and passed — an attempt below
 * {@link PASSING_SCORE} still leaves the activity outstanding. Other activity types rely on their
 * own completion flag.
 */
export function isActivityComplete(activity: LessonActivityV1): boolean {
    if (activity.type === "quiz") {
        return Boolean(activity.progress?.completedAt) && Number(activity.progress?.score || 0) >= PASSING_SCORE;
    }
    return Boolean(activity.progress?.isCompleted);
}

/**
 * Returns the first activity the learner still has to finish, searching the activities after the
 * current one first and only then wrapping to earlier ones. Wrapping matters because activities can
 * be played out of order: without it, finishing the last activity while an earlier one is still
 * incomplete would advance past the unfinished activity.
 *
 * <p><b>The current activity is never inspected.</b> It has just been submitted, so the progress in
 * the props may lag the server. The caller decides whether the learner earned the move — see
 * {@link getContinueButtonLabel}, which keeps a neutral label while an attempt is unpassed. Without
 * that guard the resolver would skip straight past a failed quiz to the roadmap-completion branch.
 *
 * <p>Pass `currentActivityId = null` from the lesson page, where no single activity is "current" —
 * the search then simply runs over the whole list in order, current-activity caveat included.
 */
function findNextIncompleteActivity(
    activities: LessonActivityV1[],
    currentActivityId: number | null,
): LessonActivityV1 | undefined {
    const currentIndex =
        currentActivityId === null ? -1 : activities.findIndex((item) => item.id === currentActivityId);
    const searchOrder =
        currentIndex >= 0
            ? [...activities.slice(currentIndex + 1), ...activities.slice(0, currentIndex)]
            : activities;

    return searchOrder.find((item) => !isActivityComplete(item));
}

/**
 * Label for the activity player's continue button.
 *
 * <p>Only an earned move advertises the resolved target. While an attempt is unpassed the button is
 * disabled and stays neutral: {@link getContinuePathTarget} ignores the current activity, so on a
 * roadmap's last lesson it would otherwise promise "Complete roadmap" for a failed quiz.
 */
export function getContinueButtonLabel(hasEarnedContinue: boolean, target: ContinuePathTarget): string {
    return hasEarnedContinue ? target.label : CONTINUE_PATH_LABEL;
}

/**
 * Whether the lesson page may offer roadmap completion: the viewer is on the last lesson of an
 * enrolled roadmap and has finished *that lesson*.
 *
 * <p>Lesson-scoped only — the lesson-detail contract carries no roadmap-wide progress, so a learner
 * who skipped an earlier lesson still sees the action. The link goes to the roadmap overview, which
 * reports real progress, so this is a misleading label rather than a data problem. Gating on the
 * whole roadmap needs a new field on the API contract.
 *
 * @param isInRoadmap  viewer is enrolled in a roadmap containing this lesson
 * @param hasNextLesson a further lesson exists in that roadmap
 * @param activities   the lesson's activities; an empty list means the lesson is finished by reading
 * @param isRead       reading-completion flag, used only when the lesson has no activities
 */
export function canCompleteRoadmap(
    isInRoadmap: boolean,
    hasNextLesson: boolean,
    activities: LessonActivityV1[],
    isRead: boolean,
): boolean {
    if (!isInRoadmap || hasNextLesson) {
        return false;
    }
    return activities.length > 0 ? activities.every(isActivityComplete) : isRead;
}

/**
 * Resolves where the continue button leads, in priority order: remaining activity in this lesson →
 * next lesson in the roadmap → roadmap overview once the roadmap is finished. A standalone lesson
 * (no enrolled roadmap) has nowhere to advance to, so it falls back to its own lesson page.
 *
 * <p>Shared by the activity player and the lesson page so both surfaces stay in agreement about
 * what "continue" means.
 */
export function getContinuePathTarget(
    lessonId: number,
    activities: LessonActivityV1[],
    currentActivityId: number | null,
    roadmapContext?: ContinueRoadmapContext | null,
): ContinuePathTarget {
    const nextIncompleteActivity = findNextIncompleteActivity(activities, currentActivityId);

    if (nextIncompleteActivity) {
        return {
            href: `/lessons/${lessonId}/activities/${nextIncompleteActivity.id}`,
            label: CONTINUE_PATH_LABEL,
        };
    }

    if (roadmapContext?.nextLessonId) {
        return { href: `/lessons/${roadmapContext.nextLessonId}`, label: CONTINUE_PATH_LABEL };
    }

    // Enrolled roadmap with no next lesson: this was its final lesson.
    if (roadmapContext) {
        return { href: ROADMAPS_HREF, label: COMPLETE_ROADMAP_LABEL };
    }

    return { href: `/lessons/${lessonId}`, label: CONTINUE_PATH_LABEL };
}
