import { describe, expect, it } from "vitest";
import { repairPastedStorageHtml, storageKeyFromUrl } from "./storageKeyFromUrl";

describe("storageKeyFromUrl", () => {
    it("should extract the storage key from a CloudFront signed URL with a query string", () => {
        // Given:
        const url = "https://d7ygfgvqm0anp.cloudfront.net/uploads/9902618a-c54a-48b9-9a20-1584874b78b9/"
            + "repro-5-second-session.gif?Expires=1786729213&Signature=kcuPjBD95js9&Key-Pair-Id=K28HKTY55W7QJV";

        // When:
        const result = storageKeyFromUrl(url);

        // Then:
        expect(result).toBe("uploads/9902618a-c54a-48b9-9a20-1584874b78b9/repro-5-second-session.gif");
    });

    it("should extract the storage key from an S3 path-style URL with a leading bucket segment", () => {
        // Given:
        const url = "https://s3.eu-central-1.amazonaws.com/my-lessons-bucket/uploads/"
            + "550e8400-e29b-41d4-a716-446655440000/diagram.png?X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Signature=abc";

        // When:
        const result = storageKeyFromUrl(url);

        // Then:
        expect(result).toBe("uploads/550e8400-e29b-41d4-a716-446655440000/diagram.png");
    });

    it("should return null for a URL that is not a storage object URL", () => {
        // Given:
        const url = "https://example.com/marketing/hero.png";

        // When:
        const result = storageKeyFromUrl(url);

        // Then:
        expect(result).toBeNull();
    });

    it("should return null for a path-traversal attempt", () => {
        // Given:
        const url = "https://cdn.example.com/uploads/../../etc/passwd";

        // When:
        const result = storageKeyFromUrl(url);

        // Then:
        expect(result).toBeNull();
    });

    it("should return null for a filename longer than 255 characters", () => {
        // Given:
        const longFilename = `${"a".repeat(256)}.png`;
        const url = `https://d7ygfgvqm0anp.cloudfront.net/uploads/9902618a-c54a-48b9-9a20-1584874b78b9/${longFilename}`;

        // When:
        const result = storageKeyFromUrl(url);

        // Then:
        expect(result).toBeNull();
    });

    it("should return null for a malformed UUID segment", () => {
        // Given:
        const url = "https://d7ygfgvqm0anp.cloudfront.net/uploads/not-a-uuid/repro-5-second-session.gif";

        // When:
        const result = storageKeyFromUrl(url);

        // Then:
        expect(result).toBeNull();
    });

    it("should return null when uploads/ is not at a path-segment boundary", () => {
        // Given: a foreign path whose segment merely ends with "uploads".
        const url = "https://cdn.example.com/myuploads/9902618a-c54a-48b9-9a20-1584874b78b9/pic.gif";

        // When:
        const result = storageKeyFromUrl(url);

        // Then:
        expect(result).toBeNull();
    });

    it("should return null instead of throwing for a malformed URL string", () => {
        // Given:
        const url = ":::::not-a-url:::::";

        // When:
        const result = storageKeyFromUrl(url);

        // Then:
        expect(result).toBeNull();
    });

    it("should return null instead of throwing for a non-string input", () => {
        // Given:
        const url = null as unknown as string;

        // When:
        const result = storageKeyFromUrl(url);

        // Then:
        expect(result).toBeNull();
    });
});

describe("repairPastedStorageHtml", () => {
    it("should add data-storage-key to an img that only has a signed src", () => {
        // Given:
        const html = '<p><img src="https://d7ygfgvqm0anp.cloudfront.net/uploads/9902618a-c54a-48b9-9a20-1584874b78b9/'
            + 'repro-5-second-session.gif?Expires=1786729213&Signature=kcuPjBD95js9" alt=""></p>';

        // When:
        const result = repairPastedStorageHtml(html);

        // Then:
        expect(result).toContain('data-storage-key="uploads/9902618a-c54a-48b9-9a20-1584874b78b9/repro-5-second-session.gif"');
    });

    it("should leave an img that already has data-storage-key untouched", () => {
        // Given:
        const html = '<img src="https://d7ygfgvqm0anp.cloudfront.net/uploads/9902618a-c54a-48b9-9a20-1584874b78b9/'
            + 'other.gif?Expires=1" data-storage-key="uploads/9902618a-c54a-48b9-9a20-1584874b78b9/original.gif">';

        // When:
        const result = repairPastedStorageHtml(html);

        // Then:
        expect(result).toBe(html);
    });

    it("should leave an img with an unrelated src untouched and return the original string", () => {
        // Given:
        const html = '<img src="https://example.com/marketing/hero.png" alt="Hero">';

        // When:
        const result = repairPastedStorageHtml(html);

        // Then:
        expect(result).toBe(html);
    });

    it("should repair the alt=\"\" fingerprint left by dragging the attachment thumbnail", () => {
        // Given: dragging the Assets panel thumbnail loses data-storage-key and leaves alt="".
        const html = '<img src="https://d7ygfgvqm0anp.cloudfront.net/uploads/9902618a-c54a-48b9-9a20-1584874b78b9/'
            + 'repro-5-second-session.gif?Expires=1786729213&Signature=kcuPjBD95js9" alt="">';

        // When:
        const result = repairPastedStorageHtml(html);

        // Then:
        expect(result).toContain('data-storage-key="uploads/9902618a-c54a-48b9-9a20-1584874b78b9/repro-5-second-session.gif"');
    });

    it("should repair the no-alt fingerprint left by dragging out of a standalone browser tab", () => {
        // Given: dragging an image out of a tab opened via "open" loses data-storage-key and has no alt attribute.
        const html = '<img src="https://d7ygfgvqm0anp.cloudfront.net/uploads/9902618a-c54a-48b9-9a20-1584874b78b9/'
            + 'repro-5-second-session.gif?Expires=1786729213&Signature=kcuPjBD95js9">';

        // When:
        const result = repairPastedStorageHtml(html);

        // Then:
        expect(result).toContain('data-storage-key="uploads/9902618a-c54a-48b9-9a20-1584874b78b9/repro-5-second-session.gif"');
    });

    it("should leave an unsigned third-party img alone even when its path matches the key shape", () => {
        // Given: another site using the same uploads/<uuid>/<file> convention, with no signature.
        // Writing this key into the lesson would fail authorization and blank every image in it.
        const html = '<img src="https://other-app.example.com/uploads/'
            + '9902618a-c54a-48b9-9a20-1584874b78b9/pic.gif" alt="">';

        // When:
        const result = repairPastedStorageHtml(html);

        // Then:
        expect(result).toBe(html);
    });

    it("should return the original string unchanged when nothing needed repair", () => {
        // Given:
        const html = "<p>Just some text with <strong>formatting</strong>.</p>";

        // When:
        const result = repairPastedStorageHtml(html);

        // Then:
        expect(result).toBe(html);
    });
});
