import { Editor } from "@tiptap/core";
import { StarterKit } from "@tiptap/starter-kit";
import { afterEach, describe, expect, it } from "vitest";
import { YoutubeEmbed } from "./youtube-extension";

function createEditor() {
    return new Editor({
        extensions: [StarterKit, YoutubeEmbed],
        content: "<p></p>",
    });
}

describe("YoutubeEmbed extension", () => {
    let editor: Editor;

    afterEach(() => {
        editor?.destroy();
    });

    it("should render an iframe pointed at the YouTube embed URL when inserted test", () => {
        // Given:
        editor = createEditor();

        // When:
        editor.commands.insertContent({
            type: "youtubeEmbed",
            attrs: { url: "https://youtu.be/abc123", title: "How to n8n" },
        });
        const html = editor.getHTML();

        // Then: the persisted HTML is a bare, playable iframe — the exact shape the
        // lesson reader's `.lesson-reader iframe` CSS rule and the "video files not just
        // links" toolbar work is meant to produce.
        expect(html).toContain('src="https://www.youtube.com/embed/abc123"');
        expect(html).toContain('data-youtube-url="https://youtu.be/abc123"');
        expect(html).toContain("allowfullscreen");
    });

    it("should round-trip a saved youtube embed back into a youtubeEmbed node on reload test", () => {
        // Given: content as it would be persisted and reloaded for editing
        const savedHtml =
            '<p></p><iframe data-youtube-url="https://youtu.be/xyz789" src="https://www.youtube.com/embed/xyz789" allowfullscreen="true"></iframe>';
        editor = new Editor({ extensions: [StarterKit, YoutubeEmbed], content: savedHtml });

        // When:
        const json = editor.getJSON();

        // Then: parseHTML recovered the original youtube URL from data-youtube-url
        const embedNode = json.content?.find((node) => node.type === "youtubeEmbed");
        expect(embedNode?.attrs?.url).toBe("https://youtu.be/xyz789");
    });

    it("should not treat an unrelated iframe as a youtube embed test", () => {
        // Given: a plain iframe with no data-youtube-url marker
        editor = new Editor({
            extensions: [StarterKit, YoutubeEmbed],
            content: '<p></p><iframe src="https://example.com/other"></iframe>',
        });

        // When:
        const json = editor.getJSON();

        // Then:
        const embedNode = json.content?.find((node) => node.type === "youtubeEmbed");
        expect(embedNode).toBeUndefined();
    });
});
