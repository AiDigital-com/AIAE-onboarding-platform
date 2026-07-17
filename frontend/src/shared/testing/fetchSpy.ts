import { vi } from "vitest";

export interface FetchSpy {
    /** All recorded call URLs, in call order. */
    urls: string[];
    /** Number of calls whose URL contains `urlSubstring` (or all calls, if omitted). */
    countRequestsTo: (urlSubstring?: string) => number;
    /** Restores the original global `fetch`. Call in every test that uses this spy. */
    restore: () => void;
}

/**
 * Wraps the global `fetch` with a Vitest spy that always resolves with `responseBody`,
 * so a component/hook test can assert workflow request counts (per
 * `.claude/rules/50-frontend-tests.md`: "assert important request counts ... rerenders
 * do not produce duplicate requests") without needing a full mock-server dependency.
 *
 * Each call site owns its own spy instance and must call `restore()` — no shared
 * module-level mock state.
 *
 * @param responseBody body every intercepted call resolves with (defaults to `{}`)
 * @return the spy, with recorded URLs and a restore handle
 */
export function spyOnFetch(responseBody: unknown = {}): FetchSpy {
    const urls: string[] = [];
    const originalFetch = globalThis.fetch;

    globalThis.fetch = vi.fn(async (input: RequestInfo | URL) => {
        urls.push(typeof input === "string" ? input : input.toString());
        return new Response(JSON.stringify(responseBody), {
            status: 200,
            headers: { "Content-Type": "application/json" },
        });
    }) as typeof fetch;

    return {
        urls,
        countRequestsTo: (urlSubstring?: string) =>
            urlSubstring === undefined
                ? urls.length
                : urls.filter((url) => url.includes(urlSubstring)).length,
        restore: () => {
            globalThis.fetch = originalFetch;
        },
    };
}
