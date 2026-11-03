import { describe, expect, it } from "vitest";

import type { LearningAssigneeV1, PickedItem, RemovalKey } from "../model/roadmapAssignment";
import {
    assigneeSources,
    buildAssignmentPlan,
    canRemoveAssignee,
    isRemovalCovering,
    planHint,
    primaryActionLabel,
} from "./roadmapAssignment";

function assignee(overrides: Partial<LearningAssigneeV1> = {}): LearningAssigneeV1 {
    return {
        userId: 1,
        name: "Ann Smith",
        email: "ann@example.com",
        enrolledAt: "2026-11-01T00:00:00Z",
        autoAssigned: false,
        viaGroupNames: [],
        ...overrides,
    };
}

describe("assigneeSources", () => {
    it("should treat an enrollment with no team and no auto flag as direct test", () => {
        // When:
        const sources = assigneeSources(assignee());

        // Then:
        expect(sources).toEqual({ direct: true, autoAssigned: false, viaGroupNames: [] });
    });

    it("should not call a team-covered or auto-assigned enrollment direct test", () => {
        // When / Then:
        expect(assigneeSources(assignee({ viaGroupNames: ["Design"] })).direct).toBe(false);
        expect(assigneeSources(assignee({ autoAssigned: true })).direct).toBe(false);
    });
});

describe("canRemoveAssignee", () => {
    it("should allow removing direct and auto-assigned people but not team-only ones test", () => {
        // When / Then:
        expect(canRemoveAssignee(assignee())).toBe(true);
        expect(canRemoveAssignee(assignee({ autoAssigned: true, viaGroupNames: ["Design"] }))).toBe(true);
        expect(canRemoveAssignee(assignee({ viaGroupNames: ["Design"] }))).toBe(false);
    });
});

describe("isRemovalCovering", () => {
    const teamIds = new Map([["Design", 7]]);

    it("should be covered only once every source of the person is staged for removal test", () => {
        // Given: a person who has the roadmap directly and via Design
        const person = assignee({ userId: 3, autoAssigned: false, viaGroupNames: ["Design"] });

        // When / Then:
        expect(isRemovalCovering(person, new Set<RemovalKey>(["t:7"]), teamIds)).toBe(true);
        expect(isRemovalCovering(assignee({ userId: 3 }), new Set<RemovalKey>(["t:7"]), teamIds)).toBe(false);
        expect(isRemovalCovering(assignee({ userId: 3 }), new Set<RemovalKey>(["p:3"]), teamIds)).toBe(true);
    });
});

describe("buildAssignmentPlan and labels", () => {
    const ann: PickedItem = { type: "person", id: 1, name: "Ann", email: "ann@example.com" };
    const bob: PickedItem = { type: "person", id: 2, name: "Bob", email: "bob@example.com" };
    const design: PickedItem = { type: "team", id: 7, name: "Design", membersCount: 3, gradeIds: [] };

    it("should count only people who do not already have the roadmap test", () => {
        // Given: Ann already has it
        const plan = buildAssignmentPlan([ann, bob], new Set(), new Set([1]));

        // Then:
        expect(plan).toEqual({ newPeopleCount: 1, pickedTeamCount: 0, removalCount: 0 });
        expect(primaryActionLabel(plan)).toBe("Assign to 1 person");
        expect(planHint(plan)).toBe("1 person will get this roadmap");
    });

    it("should describe mixed adds and removals as Save changes test", () => {
        // Given:
        const plan = buildAssignmentPlan([bob, design], new Set<RemovalKey>(["p:1"]), new Set([1]));

        // Then:
        expect(plan).toEqual({ newPeopleCount: 1, pickedTeamCount: 1, removalCount: 1 });
        expect(primaryActionLabel(plan)).toBe("Save changes");
        expect(planHint(plan)).toBe("1 person (incl. 1 team) will get this roadmap · 1 will be removed");
    });

    it("should describe a pure removal and an empty plan test", () => {
        // When / Then:
        expect(primaryActionLabel(buildAssignmentPlan([], new Set<RemovalKey>(["p:1", "t:7"]), new Set()))).toBe("Remove 2");
        expect(primaryActionLabel(buildAssignmentPlan([], new Set(), new Set()))).toBe("Assign");
        expect(planHint(buildAssignmentPlan([], new Set(), new Set()))).toBe("Pick people or teams to assign.");
        expect(primaryActionLabel(buildAssignmentPlan([design], new Set(), new Set()))).toBe("Assign team");
    });
});
