import { describe, expect, it } from "vitest";
import type { components } from "@/shared/api/generated/schema";
import { buildLowConfidenceLessonsForMembers } from "./TeamProgressDashboard";

type LowConfidenceLessonV1 = components["schemas"]["TeamDashboardLowConfidenceLessonV1"];
type AttemptItemV1 = components["schemas"]["TeamDashboardLowConfidenceAttemptItemV1"];

function attemptItem(overrides: Partial<AttemptItemV1> = {}): AttemptItemV1 {
    return {
        id: "attempt-1",
        userId: "member-1",
        userName: "Member",
        activityId: "activity-1",
        activityTitle: "Quiz",
        attemptNumber: 1,
        score: 50,
        passed: false,
        ...overrides,
    };
}

function lesson(overrides: Partial<LowConfidenceLessonV1> = {}): LowConfidenceLessonV1 {
    return {
        id: "lesson-1",
        lesson: "Struggling Lesson",
        attempts: 7,
        learners: 2,
        avgScore: 62,
        attemptsExcludingLead: 5,
        learnersExcludingLead: 1,
        avgScoreExcludingLead: 50,
        attemptItems: [attemptItem({ userId: "lead-1" }), attemptItem({ userId: "member-1" })],
        ...overrides,
    };
}

describe("buildLowConfidenceLessonsForMembers", () => {
    it("should use the backend's full-history KPIs, not a recomputation from attemptItems, when the lead is included test", () => {
        // Given: a lesson whose bounded attemptItems sample (2 rows) is smaller than the true attempts count (7)
        const lessons = [lesson()];

        // When:
        const result = buildLowConfidenceLessonsForMembers(lessons, "lead-1", true);

        // Then: KPIs come straight from the backend fields, unaffected by the sample size
        expect(result).toHaveLength(1);
        expect(result[0].attempts).toBe(7);
        expect(result[0].learners).toBe(2);
        expect(result[0].avgScore).toBe(62);
        expect(result[0].attemptItems).toHaveLength(2);
    });

    it("should switch to the excluding-lead KPIs and drop the lead's rows from attemptItems when the lead is excluded test", () => {
        // Given:
        const lessons = [lesson()];

        // When:
        const result = buildLowConfidenceLessonsForMembers(lessons, "lead-1", false);

        // Then:
        expect(result).toHaveLength(1);
        expect(result[0].attempts).toBe(5);
        expect(result[0].learners).toBe(1);
        expect(result[0].avgScore).toBe(50);
        expect(result[0].attemptItems?.every((item) => item.userId !== "lead-1")).toBe(true);
    });

    it("should drop a lesson whose selected avgScore is no longer below the low-confidence threshold test", () => {
        // Given: excluding the lead raises this lesson's average back to a confident score
        const lessons = [lesson({ avgScoreExcludingLead: 85 })];

        // When:
        const result = buildLowConfidenceLessonsForMembers(lessons, "lead-1", false);

        // Then:
        expect(result).toHaveLength(0);
    });

    it("should drop a lesson with zero attempts for the selected population test", () => {
        // Given:
        const lessons = [lesson({ attemptsExcludingLead: 0, avgScoreExcludingLead: 0 })];

        // When:
        const result = buildLowConfidenceLessonsForMembers(lessons, "lead-1", false);

        // Then:
        expect(result).toHaveLength(0);
    });

    it("should not filter attemptItems when there is no lead in scope test", () => {
        // Given: an admin/org-wide view with no team lead concept
        const lessons = [lesson()];

        // When:
        const result = buildLowConfidenceLessonsForMembers(lessons, null, true);

        // Then:
        expect(result[0].attemptItems).toHaveLength(2);
    });
});
