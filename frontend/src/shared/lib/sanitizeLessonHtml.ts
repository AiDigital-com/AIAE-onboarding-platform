import DOMPurify from "dompurify";

const YOUTUBE_EMBED_SRC_PATTERN = /^https:\/\/(www\.)?youtube(-nocookie)?\.com\/embed\/[A-Za-z0-9_-]+(\?[A-Za-z0-9_=&-]*)?$/;

let iframeHookInstalled = false;

function installYoutubeOnlyIframeHook() {
    if (iframeHookInstalled) {
        return;
    }
    iframeHookInstalled = true;

    DOMPurify.addHook("uponSanitizeElement", (node, data) => {
        if (data.tagName !== "iframe") {
            return;
        }
        const src = node instanceof Element ? node.getAttribute("src") ?? "" : "";
        if (!YOUTUBE_EMBED_SRC_PATTERN.test(src)) {
            node.parentNode?.removeChild(node);
        }
    });
}

/**
 * Sanitizes lesson HTML on the client immediately before it reaches `dangerouslySetInnerHTML`.
 *
 * This is a defense-in-depth boundary only: the server-side `LessonHtmlSanitizer` is the
 * sole authoritative sanitization boundary, applied at persistence time. This pass protects
 * against any content that reached the DOM through a path other than the persisted lesson
 * (e.g. a future regression that skips the server sanitizer).
 *
 * @param html the resolved lesson HTML about to be rendered
 * @returns the sanitized HTML, safe to pass to `dangerouslySetInnerHTML`
 */
export function sanitizeLessonHtml(html: string): string {
    installYoutubeOnlyIframeHook();
    return DOMPurify.sanitize(html, {
        ADD_TAGS: ["iframe"],
        ADD_ATTR: ["target", "allow", "allowfullscreen"],
    });
}
