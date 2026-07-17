package com.aidigital.aionboarding.service.lesson.support;

import org.owasp.html.AttributePolicy;
import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * Sanitizes lesson HTML (manual authoring, AI generation, AI revision) against a strict allowlist
 * matching the Tiptap schema configured in {@code simple-editor.tsx}, so no manual, generated, or
 * revised content can smuggle an active script, event handler, or unsafe URL into a stored lesson.
 * This is the sole security boundary; the frontend sanitizer pass is defense in depth only, not a
 * substitute for this.
 */
@Component
public class LessonHtmlSanitizer {

    /** Matches exactly the {@code uploads/<uuid>/<filename>} shape
     * {@link com.aidigital.aionboarding.service.storage.StorageService} generates — deliberately
     * anchored to that literal prefix/UUID segment (not just an allowed character set) so a
     * traversal sequence built entirely from otherwise-safe characters (e.g. {@code ../../etc/passwd})
     * cannot pass as a {@code data-storage-key}. */
    private static final Pattern STORAGE_KEY_PATTERN =
        Pattern.compile("^uploads/[0-9a-fA-F-]{36}/[A-Za-z0-9_.-]{1,255}$");

    /** Only a YouTube embed URL may populate an {@code iframe[src]} — the one element/attribute
     * combination in this schema that would otherwise let an attacker frame an arbitrary origin. */
    private static final Pattern YOUTUBE_EMBED_SRC_PATTERN =
        Pattern.compile("^https://(www\\.)?youtube(-nocookie)?\\.com/embed/[A-Za-z0-9_-]+(\\?[A-Za-z0-9_=&-]*)?$");

    private static final Pattern HTTP_URL_PATTERN = Pattern.compile("^https?://\\S+$");
    private static final Pattern MAILTO_URL_PATTERN = Pattern.compile("^mailto:\\S+$");
    private static final Pattern TASK_ITEM_CHECKED_PATTERN = Pattern.compile("^(true|false)$");
    private static final Pattern PIXEL_OR_PERCENT_PATTERN = Pattern.compile("^\\d{1,5}(\\.\\d+)?(px|%)$");
    private static final Pattern DIMENSION_OR_AUTO_PATTERN = Pattern.compile("^(auto|\\d{1,5}(\\.\\d+)?(px|%))$");
    private static final Pattern HEX_OR_NAMED_COLOR_PATTERN = Pattern.compile("^([a-zA-Z]{3,20}|#[0-9a-fA-F]{3,8})$");
    private static final Pattern TEXT_ALIGN_VALUE_PATTERN = Pattern.compile("^(left|right|center|justify)$");

    private final PolicyFactory policy = buildPolicy();

    /**
     * Sanitizes an HTML fragment down to the allowed Tiptap-compatible subset, dropping any
     * disallowed element, attribute, or attribute value rather than throwing.
     *
     * @param html HTML fragment to sanitize, possibly {@code null} or blank
     * @return the sanitized fragment; {@code null} or blank input is returned unchanged
     */
    public String sanitize(String html) {
        if (html == null || html.isBlank()) {
            return html;
        }
        return policy.sanitize(html);
    }

    PolicyFactory buildPolicy() {
        AttributePolicy storageKeyPolicy = matching(STORAGE_KEY_PATTERN);
        AttributePolicy httpOrMailtoUrlPolicy = matchingAny(HTTP_URL_PATTERN, MAILTO_URL_PATTERN);
        AttributePolicy httpUrlPolicy = matching(HTTP_URL_PATTERN);
        AttributePolicy youtubeEmbedSrcPolicy = matching(YOUTUBE_EMBED_SRC_PATTERN);
        AttributePolicy pixelOrPercentPolicy = matching(PIXEL_OR_PERCENT_PATTERN);
        AttributePolicy textAlignStylePolicy = (elementName, attributeName, value) ->
            sanitizeDeclarations(value, TEXT_ALIGN_VALUE_PATTERN, "text-align");
        AttributePolicy dimensionStylePolicy = (elementName, attributeName, value) ->
            sanitizeDeclarations(value, DIMENSION_OR_AUTO_PATTERN, "width", "height");

        return new HtmlPolicyBuilder()
            // Inline formatting.
            .allowElements("strong", "b", "em", "i", "s", "u", "mark", "sup", "sub", "code", "br")
            // Block structure.
            .allowElements("p", "h1", "h2", "h3", "h4", "h5", "h6", "blockquote", "pre", "hr")
            .allowAttributes("style").matching(textAlignStylePolicy).onElements("p", "h1", "h2", "h3", "h4", "h5", "h6")
            // Lists, including Tiptap task lists.
            .allowElements("ul", "ol", "li")
            .allowAttributes("data-type").matching(literal("taskList")).onElements("ul")
            .allowAttributes("data-type").matching(literal("taskItem")).onElements("li")
            .allowAttributes("data-checked").matching(TASK_ITEM_CHECKED_PATTERN).onElements("li")
            // Horizontal rule wrapper (`<div data-type="horizontalRule"><hr></div>`).
            .allowElements("div")
            .allowAttributes("data-type").matching(literal("horizontalRule")).onElements("div")
            // Highlight mark color.
            .allowAttributes("data-color").matching(HEX_OR_NAMED_COLOR_PATTERN).onElements("mark")
            .allowAttributes("style").matching((e, a, v) -> sanitizeDeclarations(v, HEX_OR_NAMED_COLOR_PATTERN, "background-color", "color")).onElements("mark")
            // Links.
            .allowElements("a")
            .allowAttributes("href").matching(httpOrMailtoUrlPolicy).onElements("a")
            .allowAttributes("target").matching(literal("_blank")).onElements("a")
            .allowAttributes("rel").matching(literal("noopener noreferrer")).onElements("a")
            .allowAttributes("data-storage-key").matching(storageKeyPolicy).onElements("a")
            // Images (resizable image node: src/width/style/storageKey).
            .allowElements("img")
            .allowAttributes("src").matching(httpUrlPolicy).onElements("img")
            .allowAttributes("alt", "title").onElements("img")
            .allowAttributes("width").matching(pixelOrPercentPolicy).onElements("img")
            .allowAttributes("style").matching(dimensionStylePolicy).onElements("img")
            .allowAttributes("data-storage-key").matching(storageKeyPolicy).onElements("img")
            // Video (backend-hosted, resolved client-side via data-storage-key).
            .allowElements("video")
            .allowAttributes("src").matching(httpUrlPolicy).onElements("video")
            .allowAttributes("controls", "preload", "title").onElements("video")
            .allowAttributes("data-storage-key").matching(storageKeyPolicy).onElements("video")
            // YouTube embed iframe — the only element allowed to carry a cross-origin src, and
            // only when it is exactly a YouTube embed URL.
            .allowElements("iframe")
            .allowAttributes("src").matching(youtubeEmbedSrcPolicy).onElements("iframe")
            .allowAttributes("data-youtube-url").matching(httpUrlPolicy).onElements("iframe")
            .allowAttributes("allow", "title").onElements("iframe")
            .allowAttributes("allowfullscreen").matching(literal("true")).onElements("iframe")
            // href/src carry a built-in scheme gate independent of the per-attribute policies
            // above; without an explicit allowlist here every href/src is dropped regardless of
            // what the matching() policies permit.
            .allowUrlProtocols("https", "http", "mailto")
            .toFactory();
    }

    /**
     * Builds an {@link AttributePolicy} that keeps the attribute value unchanged when it matches
     * {@code pattern}, or drops the attribute entirely otherwise.
     *
     * @param pattern pattern the raw attribute value must fully match
     * @return the attribute policy
     */
    AttributePolicy matching(Pattern pattern) {
        return (elementName, attributeName, value) -> pattern.matcher(value).matches() ? value : null;
    }

    /**
     * Builds an {@link AttributePolicy} that keeps the attribute value unchanged when it matches
     * any of {@code patterns}, or drops the attribute entirely otherwise.
     *
     * @param patterns patterns; the raw attribute value must fully match at least one
     * @return the attribute policy
     */
    AttributePolicy matchingAny(Pattern... patterns) {
        return (elementName, attributeName, value) -> {
            for (Pattern pattern : patterns) {
                if (pattern.matcher(value).matches()) {
                    return value;
                }
            }
            return null;
        };
    }

    /**
     * Builds an {@link AttributePolicy} that keeps the attribute only when its value is exactly
     * {@code expected}, replacing it with {@code expected} regardless of casing/whitespace so a
     * caller cannot smuggle a different value under an allowed attribute name.
     *
     * @param expected the only literal value this attribute may resolve to
     * @return the attribute policy
     */
    AttributePolicy literal(String expected) {
        return (elementName, attributeName, value) -> expected.equalsIgnoreCase(value.trim()) ? expected : null;
    }

    /**
     * Filters a {@code style} attribute value down to only the named CSS properties whose value
     * matches {@code valuePattern}, dropping every other declaration. Returns {@code null} (the
     * whole attribute is dropped) when nothing survives.
     *
     * @param style        raw {@code style} attribute value
     * @param valuePattern pattern each surviving property's value must match
     * @param allowedProperties CSS property names permitted in this context
     * @return the filtered style string, or {@code null} when empty
     */
    String sanitizeDeclarations(String style, Pattern valuePattern, String... allowedProperties) {
        if (style == null || style.isBlank()) {
            return null;
        }
        StringBuilder result = new StringBuilder();
        for (String declaration : style.split(";")) {
            String[] parts = declaration.split(":", 2);
            if (parts.length != 2) {
                continue;
            }
            String property = parts[0].trim().toLowerCase();
            String value = parts[1].trim();
            boolean propertyAllowed = false;
            for (String allowed : allowedProperties) {
                if (allowed.equals(property)) {
                    propertyAllowed = true;
                    break;
                }
            }
            if (propertyAllowed && valuePattern.matcher(value).matches()) {
                result.append(property).append(':').append(value).append(';');
            }
        }
        return result.isEmpty() ? null : result.toString();
    }
}
