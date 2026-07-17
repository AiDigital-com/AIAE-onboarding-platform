import { describe, expect, it } from "vitest";
import { sanitizeLessonHtml } from "./sanitizeLessonHtml";

describe("sanitizeLessonHtml", () => {
    it("should preserve ordinary formatting and block elements", () => {
        // Given:
        const html = "<h1>Title</h1><p>Some <strong>bold</strong> and <em>italic</em> text.</p>";

        // When:
        const result = sanitizeLessonHtml(html);

        // Then:
        expect(result).toBe(html);
    });

    it("should preserve an image with storage key and dimension style", () => {
        // Given:
        const html = '<img src="https://bucket.example.com/pic.png" alt="A picture" '
            + 'data-storage-key="uploads/550e8400-e29b-41d4-a716-446655440000/pic.png" style="width:320px;height:auto;">';

        // When:
        const result = sanitizeLessonHtml(html);

        // Then:
        expect(result).toContain('data-storage-key="uploads/550e8400-e29b-41d4-a716-446655440000/pic.png"');
        expect(result).toContain("width:320px;height:auto;");
    });

    it("should preserve a safe link and its target/rel attributes", () => {
        // Given:
        const html = '<a href="https://example.com/docs" target="_blank" rel="noopener noreferrer">docs</a>';

        // When:
        const result = sanitizeLessonHtml(html);

        // Then:
        expect(result).toBe(html);
    });

    it("should preserve a youtube embed iframe", () => {
        // Given:
        const html = '<iframe src="https://www.youtube.com/embed/dQw4w9WgXcQ" '
            + 'data-youtube-url="https://www.youtube.com/watch?v=dQw4w9WgXcQ" allowfullscreen="true"></iframe>';

        // When:
        const result = sanitizeLessonHtml(html);

        // Then:
        expect(result).toContain('src="https://www.youtube.com/embed/dQw4w9WgXcQ"');
    });

    it("should reject an iframe that does not point to youtube", () => {
        // Given: attacker attempts to reuse the one element allowed to carry a cross-origin src.
        const html = '<iframe src="https://evil.example.com/phish"></iframe>';

        // When:
        const result = sanitizeLessonHtml(html);

        // Then:
        expect(result).not.toContain("evil.example.com");
        expect(result).not.toContain("<iframe");
    });

    it("should remove script tags", () => {
        // Given:
        const html = "<p>Hello</p><script>alert(document.cookie)</script>";

        // When:
        const result = sanitizeLessonHtml(html);

        // Then:
        expect(result).not.toContain("<script");
        expect(result).not.toContain("alert(");
    });

    it("should remove an onerror event handler from an image", () => {
        // Given:
        const html = '<img src="https://example.com/x.png" onerror="alert(1)">';

        // When:
        const result = sanitizeLessonHtml(html);

        // Then:
        expect(result).not.toContain("onerror");
    });

    it("should remove javascript-protocol links", () => {
        // Given:
        const html = '<a href="javascript:alert(1)">click me</a>';

        // When:
        const result = sanitizeLessonHtml(html);

        // Then:
        expect(result).not.toContain("javascript:");
    });

    it("should remove an svg onload payload", () => {
        // Given:
        const html = '<svg onload="alert(1)"><circle /></svg>';

        // When:
        const result = sanitizeLessonHtml(html);

        // Then:
        expect(result).not.toContain("onload");
    });
});
