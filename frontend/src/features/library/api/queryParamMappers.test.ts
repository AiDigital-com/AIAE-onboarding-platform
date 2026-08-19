import { describe, expect, it } from "vitest";
import { lessonStatusFilterToParams } from "./queryParamMappers";

describe("lessonStatusFilterToParams", () => {
    it("should map published to status ready and publicationStatus published test", () => {
        // Given / When:
        const result = lessonStatusFilterToParams("published");

        // Then: Public in the UI — ready and published lessons only
        expect(result).toEqual({ status: "ready", publicationStatus: "published" });
    });

    it("should map private to publicationStatus private test", () => {
        // Given / When:
        const result = lessonStatusFilterToParams("private");

        // Then: Private in the UI — the assigned-only publication state
        expect(result).toEqual({ publicationStatus: "private" });
    });

    it("should map archived to publicationStatus archived test", () => {
        // Given / When:
        const result = lessonStatusFilterToParams("archived");

        // Then:
        expect(result).toEqual({ publicationStatus: "archived" });
    });

    it("should map all/unknown values to no filter test", () => {
        // Given / When:
        const result = lessonStatusFilterToParams("all");

        // Then:
        expect(result).toEqual({});
    });
});
