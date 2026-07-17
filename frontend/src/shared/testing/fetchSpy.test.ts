import { afterEach, describe, expect, it } from "vitest";
import { spyOnFetch, type FetchSpy } from "./fetchSpy";

describe("spyOnFetch", () => {
    let activeSpy: FetchSpy | null = null;

    afterEach(() => {
        activeSpy?.restore();
        activeSpy = null;
    });

    it("should count every intercepted fetch call test", async () => {
        // Given:
        activeSpy = spyOnFetch();

        // When:
        await fetch("/api/v1/materials/search");
        await fetch("/api/v1/lessons/search");
        await fetch("/api/v1/materials/search");

        // Then:
        expect(activeSpy.countRequestsTo()).toBe(3);
    });

    it("should count only calls matching a URL substring test", async () => {
        // Given:
        activeSpy = spyOnFetch();

        // When:
        await fetch("/api/v1/materials/search");
        await fetch("/api/v1/lessons/search");
        await fetch("/api/v1/materials/search");

        // Then:
        expect(activeSpy.countRequestsTo("/materials/search")).toBe(2);
        expect(activeSpy.countRequestsTo("/lessons/search")).toBe(1);
        expect(activeSpy.countRequestsTo("/roadmaps/search")).toBe(0);
    });

    it("should resolve intercepted calls with the given JSON body test", async () => {
        // Given:
        activeSpy = spyOnFetch({ total: 42 });

        // When:
        const response = await fetch("/api/v1/materials/search");
        const body = await response.json();

        // Then:
        expect(response.ok).toBe(true);
        expect(body).toEqual({ total: 42 });
    });

    it("should restore the original fetch after restore is called test", () => {
        // Given:
        const originalFetch = globalThis.fetch;
        activeSpy = spyOnFetch();

        // When:
        activeSpy.restore();

        // Then:
        expect(globalThis.fetch).toBe(originalFetch);
    });
});
