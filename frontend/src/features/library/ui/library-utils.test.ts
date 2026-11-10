import { describe, expect, it } from "vitest";

import { placeItemAfter } from "./library-utils";

const item = (id: number) => ({ id });

describe("placeItemAfter", () => {
    it("should move the copy right after its original test", () => {
        // Given: the server put the copy first (sorted by newest)
        const items = [item(9), item(1), item(2), item(3)];

        // When / Then:
        expect(placeItemAfter(items, 9, 2).map((i) => i.id)).toEqual([1, 2, 9, 3]);
    });

    it("should move the copy to the end when the original is last test", () => {
        expect(placeItemAfter([item(9), item(1), item(2)], 9, 2).map((i) => i.id)).toEqual([1, 2, 9]);
    });

    it("should leave the list untouched when the original is not loaded test", () => {
        const items = [item(9), item(1)];
        expect(placeItemAfter(items, 9, 5)).toBe(items);
    });

    it("should leave the list untouched when the copy is not loaded test", () => {
        const items = [item(1), item(2)];
        expect(placeItemAfter(items, 9, 1)).toBe(items);
    });
});
