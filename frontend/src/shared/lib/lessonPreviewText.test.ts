import { describe, expect, it } from "vitest";
import { lessonPreviewText } from "./lessonPreviewText";

describe("lessonPreviewText", () => {
    it("removes truncated media tags and signed urls from card preview text", () => {
        const preview = lessonPreviewText({
            contentHtmlPreview: '<h1><strong>What is programmatic?</strong></h1><img src="https://bucket.s3.amazonaws.com/file.png?X-Amz-Signature=secret',
            contentMarkdownPreview: "",
            description: "",
        });

        expect(preview).toBe("What is programmatic?");
    });

    it("falls back to markdown text when html contains media only", () => {
        const preview = lessonPreviewText({
            contentHtmlPreview: '<img src="https://bucket.s3.amazonaws.com/file.png?X-Amz-Signature=secret',
            contentMarkdownPreview: "## Programmatic basics",
            description: "",
        });

        expect(preview).toBe("Programmatic basics");
    });
});
