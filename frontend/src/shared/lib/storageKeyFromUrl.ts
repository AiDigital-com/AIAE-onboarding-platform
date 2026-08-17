/**
 * Matches the backend `LessonHtmlSanitizer` `STORAGE_KEY_PATTERN` exactly
 * (`^uploads/[0-9a-fA-F-]{36}/[A-Za-z0-9_.-]{1,255}$`). Any drift between the
 * two makes the sanitizer silently drop `data-storage-key` on save, so this
 * pattern must stay byte-for-byte identical to the backend's.
 */
const STORAGE_KEY_PATTERN = /^uploads\/[0-9a-fA-F-]{36}\/[A-Za-z0-9_.-]{1,255}$/;

/**
 * Same shape as {@link STORAGE_KEY_PATTERN}, but anchored to a path-segment
 * boundary so a foreign path such as `/myuploads/<uuid>/pic.gif` cannot be
 * mistaken for a storage key.
 */
const STORAGE_KEY_IN_PATH_PATTERN =
    /(?:^|\/)(uploads\/[0-9a-fA-F-]{36}\/[A-Za-z0-9_.-]{1,255})$/;

/**
 * Returns whether a URL carries an object-storage signature — CloudFront
 * (`Signature` + `Key-Pair-Id`) or SigV4 (`X-Amz-Signature`).
 *
 * Repair is gated on this because an unsigned third-party URL that merely
 * happens to match the key shape would be written into the lesson as a
 * `data-storage-key` the backend cannot authorize. `getFilePreviews`
 * authorizes keys with a `forEach` that throws on the first inaccessible
 * one, so a single bogus key fails the whole batch and blanks every image
 * in the lesson.
 */
function isSignedStorageUrl(url: string): boolean {
    try {
        const base = typeof window !== "undefined" ? window.location.origin : "http://localhost";
        const { searchParams } = new URL(url, base);
        return searchParams.has("Signature") || searchParams.has("X-Amz-Signature");
    } catch {
        return false;
    }
}

/**
 * Derives a stable object-storage key (e.g. `uploads/<uuid>/<filename>`)
 * from a signed URL such as a CloudFront distribution URL or an S3
 * path-style URL. The signature/expiry lives in the query string and is
 * ignored; the key is anchored on the `uploads/` path segment rather than
 * assumed to start at the path root, since S3 path-style URLs carry a
 * leading bucket segment.
 *
 * Returns `null` for anything that is not a well-formed storage URL,
 * including path-traversal attempts, malformed UUID segments, and
 * filenames over 255 characters. Never throws, even for a malformed or
 * relative URL string.
 */
export function storageKeyFromUrl(url: string): string | null {
    if (typeof url !== "string" || url.length === 0) {
        return null;
    }

    let pathname: string;
    try {
        const base = typeof window !== "undefined" ? window.location.origin : "http://localhost";
        pathname = new URL(url, base).pathname;
    } catch {
        pathname = url.split("?")[0].split("#")[0];
    }

    let decodedPath: string;
    try {
        decodedPath = decodeURIComponent(pathname);
    } catch {
        decodedPath = pathname;
    }

    const match = STORAGE_KEY_IN_PATH_PATTERN.exec(decodedPath);
    if (!match) {
        return null;
    }

    return STORAGE_KEY_PATTERN.test(match[1]) ? match[1] : null;
}

/**
 * Repairs pasted/dropped lesson HTML that lost `data-storage-key` because
 * the browser's default clipboard/drag handling built the node from the
 * DOM's `src`/serialized `outerHTML` instead of the app's own insertion
 * path. For every `<img>`/`<video>` with a signed `src` but no
 * `data-storage-key`, derives the key from `src` and sets the attribute when
 * it resolves. Unsigned URLs are skipped — see {@link isSignedStorageUrl}.
 *
 * Elements that already carry `data-storage-key` are left untouched, and
 * the original string is returned unmodified when nothing changed (instead
 * of a re-serialized equivalent) so unrelated formatting is not disturbed.
 */
export function repairPastedStorageHtml(html: string): string {
    if (typeof DOMParser === "undefined" || !html) {
        return html;
    }

    const parser = new DOMParser();
    const parsedDocument = parser.parseFromString(html, "text/html");
    const candidates = Array.from(
        parsedDocument.querySelectorAll<HTMLImageElement | HTMLVideoElement>(
            "img[src]:not([data-storage-key]), video[src]:not([data-storage-key])",
        ),
    );

    let changed = false;

    candidates.forEach((element) => {
        const src = element.getAttribute("src");
        if (!src || !isSignedStorageUrl(src)) {
            return;
        }

        const storageKey = storageKeyFromUrl(src);
        if (!storageKey) {
            return;
        }

        element.setAttribute("data-storage-key", storageKey);
        changed = true;
    });

    return changed ? parsedDocument.body.innerHTML : html;
}
