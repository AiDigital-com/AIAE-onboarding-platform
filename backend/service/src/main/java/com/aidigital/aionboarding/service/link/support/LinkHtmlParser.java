package com.aidigital.aionboarding.service.link.support;

import org.springframework.stereotype.Component;

@Component
public class LinkHtmlParser {

    public String stripHtml(String value) {
        return decodeHtmlEntities(value
            .replaceAll("(?i)<script[\\s\\S]*?</script>", " ")
            .replaceAll("(?i)<style[\\s\\S]*?</style>", " ")
            .replaceAll("(?i)<noscript[\\s\\S]*?</noscript>", " ")
            .replaceAll("(?i)<svg[\\s\\S]*?</svg>", " ")
            .replaceAll("(?i)<(?:br|p|div|section|article|li|h[1-6])\\b[^>]*>", "\n")
            .replaceAll("<[^>]+>", " "));
    }

    public String decodeHtmlEntities(String value) {
        return value
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&#039;", "'")
            .replace("&apos;", "'")
            .replace("&lt;", "<")
            .replace("&gt;", ">");
    }
}
