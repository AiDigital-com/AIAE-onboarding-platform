import { render, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { SimpleEditor } from "./SimpleEditor";

// jsdom does not implement `window.matchMedia`, but the ported TipTap bundle's
// `useIsBreakpoint` hook calls it unconditionally on mount. Shim it locally —
// there is no global vitest setup file in this project.
if (typeof window.matchMedia !== "function") {
    window.matchMedia = ((query: string) => ({
        matches: false,
        media: query,
        onchange: null,
        addListener: () => {},
        removeListener: () => {},
        addEventListener: () => {},
        removeEventListener: () => {},
        dispatchEvent: () => false,
    })) as unknown as typeof window.matchMedia;
}

// jsdom's Range does not implement `getClientRects`/`getBoundingClientRect`,
// which ProseMirror's view touches while measuring cursor/selection coords.
// Shimmed locally for the same reason as above.
if (typeof Range.prototype.getClientRects !== "function") {
    Range.prototype.getClientRects = function getClientRects() {
        return [] as unknown as DOMRectList;
    };
}
if (typeof Range.prototype.getBoundingClientRect !== "function") {
    Range.prototype.getBoundingClientRect = function getBoundingClientRect() {
        return {
            x: 0, y: 0, top: 0, left: 0, right: 0, bottom: 0, width: 0, height: 0,
            toJSON() { return this; },
        } as DOMRect;
    };
}

/**
 * jsdom does not implement `DataTransfer`. This stands in for the browser's
 * paste `clipboardData`: it exposes `getData`/`types`, plus empty `files`/
 * `items` so the editor's own `handlePaste` image/video-file branches
 * correctly miss and execution falls through to ProseMirror's default
 * `parseFromClipboard` — which is the code path this suite verifies.
 */
function createClipboardData(html: string): DataTransfer {
    const store: Record<string, string> = {
        "text/html": html,
        "text/plain": "",
    };

    return {
        getData: (type: string) => store[type] ?? "",
        setData: (type: string, value: string) => {
            store[type] = value;
        },
        clearData: () => {
            Object.keys(store).forEach((key) => delete store[key]);
        },
        types: Object.keys(store),
        files: [] as unknown as FileList,
        items: [] as unknown as DataTransferItemList,
        dropEffect: "none",
        effectAllowed: "all",
    } as unknown as DataTransfer;
}

/**
 * Waits for the ProseMirror DOM node to mount (the editor uses
 * `immediatelyRender: false`, so it appears inside an effect) and then
 * dispatches a native `paste` event carrying only `text/html` — the same
 * shape a browser-native image drag/paste supplies, with no custom payload
 * and no `File`.
 */
async function pasteHtmlIntoEditor(container: HTMLElement, html: string) {
    const editorNode = await waitFor(() => {
        const node = container.querySelector<HTMLElement>(".ProseMirror.simple-editor");
        expect(node).not.toBeNull();
        return node!;
    });

    const pasteEvent = new Event("paste", { bubbles: true, cancelable: true });
    Object.defineProperty(pasteEvent, "clipboardData", { value: createClipboardData(html) });

    editorNode.dispatchEvent(pasteEvent);
}

const SIGNED_GIF_URL = "https://d7ygfgvqm0anp.cloudfront.net/uploads/9902618a-c54a-48b9-9a20-1584874b78b9/"
    + "repro-5-second-session.gif?Expires=1786729213&Signature=kcuPjBD95js9&Key-Pair-Id=K28HKTY55W7QJV";
const EXPECTED_KEY = "uploads/9902618a-c54a-48b9-9a20-1584874b78b9/repro-5-second-session.gif";

describe("SimpleEditor pasted-HTML storage-key repair", () => {
    afterEach(() => {
        vi.restoreAllMocks();
    });

    it("should add data-storage-key to a pasted image carrying the alt=\"\" drag fingerprint", async () => {
        // Given: the fingerprint left by dragging the thumbnail inside an attachment card —
        // alt="", no title, no data-storage-key.
        const onChange = vi.fn();
        const { container } = render(<SimpleEditor editable content="<p></p>" onChange={onChange} />);
        const pastedHtml = `<img src="${SIGNED_GIF_URL}" alt="">`;

        // When:
        await pasteHtmlIntoEditor(container, pastedHtml);

        // Then: the editor's serialized HTML — exactly what becomes the lesson PATCH body's
        // contentHtml — carries the repaired storage key.
        await waitFor(() => expect(onChange).toHaveBeenCalled());
        const lastHtml = onChange.mock.calls.at(-1)?.[0] as string;
        expect(lastHtml).toContain(`data-storage-key="${EXPECTED_KEY}"`);
    });

    it("should add data-storage-key to a pasted image carrying the no-alt drag fingerprint", async () => {
        // Given: the fingerprint left by dragging the image out of a standalone browser tab —
        // no alt attribute at all, no title, no data-storage-key.
        const onChange = vi.fn();
        const { container } = render(<SimpleEditor editable content="<p></p>" onChange={onChange} />);
        const pastedHtml = `<img src="${SIGNED_GIF_URL}">`;

        // When:
        await pasteHtmlIntoEditor(container, pastedHtml);

        // Then:
        await waitFor(() => expect(onChange).toHaveBeenCalled());
        const lastHtml = onChange.mock.calls.at(-1)?.[0] as string;
        expect(lastHtml).toContain(`data-storage-key="${EXPECTED_KEY}"`);
    });

    it("should not add data-storage-key to an unsigned third-party URL that merely matches the key shape", async () => {
        // Given: another site reusing the uploads/<uuid>/<file> convention with no signature
        // query params. A bogus key here would be rejected by getFilePreviews, which authorizes
        // keys with a forEach that throws on the first inaccessible one — a single bad key fails
        // the whole batch and blanks every image in the lesson.
        const onChange = vi.fn();
        const { container } = render(<SimpleEditor editable content="<p></p>" onChange={onChange} />);
        const pastedHtml = '<img src="https://other-app.example.com/uploads/'
            + '9902618a-c54a-48b9-9a20-1584874b78b9/pic.gif" alt="">';

        // When:
        await pasteHtmlIntoEditor(container, pastedHtml);

        // Then:
        await waitFor(() => expect(onChange).toHaveBeenCalled());
        const lastHtml = onChange.mock.calls.at(-1)?.[0] as string;
        expect(lastHtml).not.toContain("data-storage-key");
    });

    it("should keep the original data-storage-key unchanged when the image already carries one", async () => {
        // Given: an img that already has data-storage-key, with a src that would derive a
        // different key — proves the repair path leaves it untouched rather than overwriting it.
        const onChange = vi.fn();
        const { container } = render(<SimpleEditor editable content="<p></p>" onChange={onChange} />);
        const originalKey = "uploads/9902618a-c54a-48b9-9a20-1584874b78b9/original.gif";
        const pastedHtml = `<img src="${SIGNED_GIF_URL}" data-storage-key="${originalKey}">`;

        // When:
        await pasteHtmlIntoEditor(container, pastedHtml);

        // Then:
        await waitFor(() => expect(onChange).toHaveBeenCalled());
        const lastHtml = onChange.mock.calls.at(-1)?.[0] as string;
        expect(lastHtml).toContain(`data-storage-key="${originalKey}"`);
        expect(lastHtml).not.toContain(`data-storage-key="${EXPECTED_KEY}"`);
    });
});
