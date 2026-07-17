const DEFAULT_LESSON_PREVIEW = "Generated lesson preview will appear here.";

function normalizeWhitespace(value: string): string {
    return value.replace(/\s+/g, " ").trim();
}

function decodeHtmlEntities(value: string): string {
    if (!value || typeof window === "undefined") {
        return value;
    }

    const textarea = window.document.createElement("textarea");
    textarea.innerHTML = value;
    return textarea.value;
}

/**
 * Builds plain text from the short, already-truncated HTML preview returned by the backend.
 *
 * The backend may cut the preview in the middle of a tag, for example `<img src="https://...`.
 * Remove media tags before generic tag stripping so signed URLs never leak into card text.
 */
export function htmlPreviewToText(html?: string | null): string {
    if (!html) {
        return "";
    }

    const withoutMedia = html
        .replace(/<style[\s\S]*?(?:<\/style>|$)/gi, " ")
        .replace(/<script[\s\S]*?(?:<\/script>|$)/gi, " ")
        .replace(/<(?:img|video|iframe|source|picture)\b[\s\S]*?(?:>|$)/gi, " ");

    const withoutTags = withoutMedia
        .replace(/<[^>]*>/g, " ")
        .replace(/<[^>]*$/g, " ");

    return normalizeWhitespace(decodeHtmlEntities(withoutTags));
}

export function markdownPreviewToText(markdown?: string | null): string {
    if (!markdown) {
        return "";
    }

    return normalizeWhitespace(
        markdown
            .replace(/!\[[^\]]*]\([^)]*\)/g, " ")
            .replace(/\[[^\]]*]\([^)]*\)/g, (match) => match.match(/^\[([^\]]*)]/)?.[1] || " ")
            .replace(/^#+\s+/gm, "")
            .replace(/\*\*/g, ""),
    );
}

export function lessonPreviewText({
    contentHtmlPreview,
    contentMarkdownPreview,
    description,
}: {
    contentHtmlPreview?: string | null;
    contentMarkdownPreview?: string | null;
    description?: string | null;
}): string {
    return (
        htmlPreviewToText(contentHtmlPreview) ||
        markdownPreviewToText(contentMarkdownPreview) ||
        normalizeWhitespace(description || "") ||
        DEFAULT_LESSON_PREVIEW
    );
}
