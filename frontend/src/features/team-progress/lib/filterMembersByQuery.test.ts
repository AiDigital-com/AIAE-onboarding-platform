import { describe, expect, it } from "vitest";

import { filterMembersByQuery } from "./filterMembersByQuery";

function member(id: string, name: string, email: string, role: string) {
    return { id, name, email, role };
}

describe("filterMembersByQuery", () => {
    it("should return every member unchanged for a blank query test", () => {
        // Given:
        const members = [member("1", "Ann", "ann@example.com", "Designer")];

        // When:
        const result = filterMembersByQuery(members, "   ");

        // Then:
        expect(result).toBe(members);
    });

    it("should match name, email, and role case-insensitively test", () => {
        // Given:
        const members = [
            member("1", "Ann Smith", "ann@example.com", "Designer"),
            member("2", "Bob Jones", "bob@example.com", "Engineer"),
        ];

        // When / Then:
        expect(filterMembersByQuery(members, "ANN").map((m) => m.id)).toEqual(["1"]);
        expect(filterMembersByQuery(members, "bob@").map((m) => m.id)).toEqual(["2"]);
        expect(filterMembersByQuery(members, "engineer").map((m) => m.id)).toEqual(["2"]);
    });

    it("should require every whitespace-separated term to match test", () => {
        // Given:
        const members = [
            member("1", "Ann Smith", "ann@example.com", "Designer"),
            member("2", "Ann Lee", "lee@example.com", "Engineer"),
        ];

        // When:
        const result = filterMembersByQuery(members, "ann design");

        // Then:
        expect(result.map((m) => m.id)).toEqual(["1"]);
    });
});
