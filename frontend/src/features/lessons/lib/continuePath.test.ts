import { describe, expect, it } from "vitest";
import type { LessonActivityV1 } from "@/features/lessons/api/types";
import {
    COMPLETE_ROADMAP_LABEL,
    CONTINUE_PATH_LABEL,
    ROADMAPS_HREF,
    canCompleteRoadmap,
    getContinuePathTarget,
} from "./continuePath";

const LESSON_ID = 10;

function activity(id: number, isCompleted: boolean): LessonActivityV1 {
    return {
        id,
        type: "flashcards",
        progress: { isCompleted },
    } as unknown as LessonActivityV1;
}

describe("getContinuePathTarget", () => {
    it("goes to the next incomplete activity in the same lesson", () => {
        const activities = [activity(1, true), activity(2, false)];

        expect(getContinuePathTarget(LESSON_ID, activities, 1, { nextLessonId: 99 })).toEqual({
            href: `/lessons/${LESSON_ID}/activities/2`,
            label: CONTINUE_PATH_LABEL,
        });
    });

    it("advances to the next roadmap lesson once every activity is complete", () => {
        const activities = [activity(1, true), activity(2, true)];

        expect(getContinuePathTarget(LESSON_ID, activities, 2, { nextLessonId: 99 })).toEqual({
            href: "/lessons/99",
            label: CONTINUE_PATH_LABEL,
        });
    });

    it("offers roadmap completion on the final lesson of a roadmap", () => {
        const activities = [activity(1, true), activity(2, true)];

        expect(getContinuePathTarget(LESSON_ID, activities, 2, { nextLessonId: null })).toEqual({
            href: ROADMAPS_HREF,
            label: COMPLETE_ROADMAP_LABEL,
        });
    });

    it("stays on the lesson when it is standalone and has no roadmap", () => {
        const activities = [activity(1, true), activity(2, true)];

        expect(getContinuePathTarget(LESSON_ID, activities, 2, null)).toEqual({
            href: `/lessons/${LESSON_ID}`,
            label: CONTINUE_PATH_LABEL,
        });
    });

    it("wraps back to an earlier incomplete activity instead of leaving the lesson", () => {
        const activities = [activity(1, false), activity(2, true)];

        expect(getContinuePathTarget(LESSON_ID, activities, 2, { nextLessonId: 99 })).toEqual({
            href: `/lessons/${LESSON_ID}/activities/1`,
            label: CONTINUE_PATH_LABEL,
        });
    });

    it("scans the whole lesson when no activity is current (lesson page)", () => {
        const activities = [activity(1, true), activity(2, false)];

        expect(getContinuePathTarget(LESSON_ID, activities, null, { nextLessonId: 99 })).toEqual({
            href: `/lessons/${LESSON_ID}/activities/2`,
            label: CONTINUE_PATH_LABEL,
        });
    });

    it("offers roadmap completion from the lesson page on the final lesson", () => {
        const activities = [activity(1, true), activity(2, true)];

        expect(getContinuePathTarget(LESSON_ID, activities, null, { nextLessonId: null })).toEqual({
            href: ROADMAPS_HREF,
            label: COMPLETE_ROADMAP_LABEL,
        });
    });

    it("requires a passing score before a quiz counts as complete", () => {
        const failedQuiz = {
            id: 2,
            type: "quiz",
            progress: { completedAt: "2026-08-04T10:00:00Z", score: 40 },
        } as unknown as LessonActivityV1;
        const activities = [activity(1, true), failedQuiz];

        expect(getContinuePathTarget(LESSON_ID, activities, 1, { nextLessonId: 99 })).toEqual({
            href: `/lessons/${LESSON_ID}/activities/2`,
            label: CONTINUE_PATH_LABEL,
        });
    });
});

describe("canCompleteRoadmap", () => {
    const done = [activity(1, true), activity(2, true)];
    const partial = [activity(1, true), activity(2, false)];

    it("allows completion on the final lesson once every activity is done", () => {
        expect(canCompleteRoadmap(true, false, done, false)).toBe(true);
    });

    it("refuses while an activity is still outstanding", () => {
        expect(canCompleteRoadmap(true, false, partial, false)).toBe(false);
    });

    it("refuses when a further lesson remains in the roadmap", () => {
        expect(canCompleteRoadmap(true, true, done, false)).toBe(false);
    });

    it("refuses outside a roadmap", () => {
        expect(canCompleteRoadmap(false, false, done, true)).toBe(false);
    });

    it("falls back to the reading flag for a lesson with no activities", () => {
        expect(canCompleteRoadmap(true, false, [], true)).toBe(true);
        expect(canCompleteRoadmap(true, false, [], false)).toBe(false);
    });
});
