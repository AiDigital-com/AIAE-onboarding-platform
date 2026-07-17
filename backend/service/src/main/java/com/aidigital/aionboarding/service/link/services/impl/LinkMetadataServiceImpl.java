package com.aidigital.aionboarding.service.link.services.impl;

import com.aidigital.aionboarding.external.link.LinkFetchClient;
import com.aidigital.aionboarding.external.link.model.LinkFetchFailureReason;
import com.aidigital.aionboarding.external.link.model.LinkFetchResult;
import com.aidigital.aionboarding.service.common.observability.SecurityMetrics;
import com.aidigital.aionboarding.service.common.observability.enums.SsrfBlockReason;
import com.aidigital.aionboarding.service.lesson.util.LessonTextUtil;
import com.aidigital.aionboarding.service.link.services.LinkMetadataService;
import com.aidigital.aionboarding.service.link.support.LinkHtmlParser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class LinkMetadataServiceImpl implements LinkMetadataService {

    private static final int MAX_LINK_TEXT_CHARACTERS = 10000;
    private static final Pattern TITLE_PATTERN =
        Pattern.compile("<title[^>]*>([\\s\\S]*?)</title>", Pattern.CASE_INSENSITIVE);

    private final LinkFetchClient linkFetchClient;
    private final SecurityMetrics securityMetrics;
    private final LessonTextUtil lessonTextUtil;
    private final LinkHtmlParser linkHtmlParser;

    @Override
    public Map<String, Object> fetch(String url) {
        LinkFetchResult result = linkFetchClient.fetch(url);
        if (!result.success()) {
            if (result.securityBlockReason() != null) {
                securityMetrics.ssrfBlocked(toSsrfBlockReason(result.securityBlockReason()));
            }
            return errorResult(result.errorMessage().isBlank() ? "Failed to parse link." : result.errorMessage());
        }

        String html = result.body();
        Map<String, Object> parsed = new LinkedHashMap<>();
        parsed.put("title", getTitle(html));
        parsed.put("description", getMetaContent(html, "og:description", "description", "twitter:description"));
        parsed.put("imageUrl", getMetaContent(html, "og:image", "twitter:image"));
        parsed.put("siteName", getSiteName(html, url));
        parsed.put("extractedText", extractMainText(html));
        parsed.put("error", "");
        return parsed;
    }

    /**
     * Translates the external client's failure vocabulary to this service's own security-metric
     * vocabulary, so {@code external-services} never needs to depend on {@code service}.
     *
     * @param reason the external client's block reason
     * @return the corresponding security-metric reason
     */
    SsrfBlockReason toSsrfBlockReason(LinkFetchFailureReason reason) {
        return switch (reason) {
            case DISALLOWED_SCHEME -> SsrfBlockReason.DISALLOWED_SCHEME;
            case USERINFO_PRESENT -> SsrfBlockReason.USERINFO_PRESENT;
            case DISALLOWED_PORT -> SsrfBlockReason.DISALLOWED_PORT;
            case PRIVATE_ADDRESS -> SsrfBlockReason.PRIVATE_ADDRESS;
            case REDIRECT_LIMIT_EXCEEDED -> SsrfBlockReason.REDIRECT_LIMIT_EXCEEDED;
            case RESPONSE_TOO_LARGE -> SsrfBlockReason.RESPONSE_TOO_LARGE;
            case UNSUPPORTED_CONTENT_TYPE -> SsrfBlockReason.UNSUPPORTED_CONTENT_TYPE;
            case TIMEOUT -> SsrfBlockReason.TIMEOUT;
        };
    }

    Map<String, Object> errorResult(String error) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("title", "");
        result.put("description", "");
        result.put("imageUrl", "");
        result.put("siteName", "");
        result.put("extractedText", "");
        result.put("error", error);
        return result;
    }

    String getTitle(String html) {
        String fromMeta = getMetaContent(html, "og:title", "twitter:title");
        if (!fromMeta.isBlank()) {
            return fromMeta;
        }
        Matcher matcher = TITLE_PATTERN.matcher(html);
        return matcher.find() ? linkHtmlParser.decodeHtmlEntities(matcher.group(1)).trim() : "";
    }

    String getSiteName(String html, String url) {
        String siteName = getMetaContent(html, "og:site_name", "application-name");
        if (!siteName.isBlank()) {
            return siteName;
        }
        try {
            String host = URI.create(url).getHost();
            return host == null ? "" : host.replaceFirst("^www\\.", "");
        } catch (Exception ex) {
            return "";
        }
    }

    String getMetaContent(String html, String... names) {
        for (String name : names) {
            String quotedName = Pattern.quote(name);
            Pattern propertyFirst = Pattern.compile(
                "<meta[^>]+(?:property|name)=[\"']" + quotedName + "[\"'][^>]+content=[\"']([^\"']+)[\"'][^>]*>",
                Pattern.CASE_INSENSITIVE);
            Matcher propertyMatcher = propertyFirst.matcher(html);
            if (propertyMatcher.find()) {
                return linkHtmlParser.decodeHtmlEntities(propertyMatcher.group(1)).trim();
            }
            Pattern contentFirst = Pattern.compile(
                "<meta[^>]+content=[\"']([^\"']+)[\"'][^>]+(?:property|name)=[\"']" + quotedName + "[\"'][^>]*>",
                Pattern.CASE_INSENSITIVE);
            Matcher contentMatcher = contentFirst.matcher(html);
            if (contentMatcher.find()) {
                return linkHtmlParser.decodeHtmlEntities(contentMatcher.group(1)).trim();
            }
        }
        return "";
    }

    String extractMainText(String html) {
        String articleMatch = extractTagContent(html, "article");
        String bodyMatch = extractTagContent(html, "body");
        String sourceHtml = !articleMatch.isBlank() ? articleMatch : (!bodyMatch.isBlank() ? bodyMatch : html);
        String text = lessonTextUtil.normalizeText(linkHtmlParser.stripHtml(sourceHtml));
        if (text.length() > MAX_LINK_TEXT_CHARACTERS) {
            return text.substring(0, MAX_LINK_TEXT_CHARACTERS) + "...";
        }
        return text;
    }

    String extractTagContent(String html, String tag) {
        Pattern pattern = Pattern.compile(
            "<" + tag + "\\b[^>]*>([\\s\\S]*?)</" + tag + ">",
            Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(html);
        return matcher.find() ? matcher.group(1) : "";
    }
}
