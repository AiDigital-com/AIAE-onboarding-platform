package com.aidigital.aionboarding.service.link.services.impl;

import com.aidigital.aionboarding.external.link.LinkFetchClient;
import com.aidigital.aionboarding.external.link.model.LinkFetchFailureReason;
import com.aidigital.aionboarding.external.link.model.LinkFetchResult;
import com.aidigital.aionboarding.service.common.observability.SecurityMetrics;
import com.aidigital.aionboarding.service.common.observability.enums.SsrfBlockReason;
import com.aidigital.aionboarding.service.lesson.util.LessonTextUtil;
import com.aidigital.aionboarding.service.link.support.LinkHtmlParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LinkMetadataServiceImplTest {

	@Mock
	private LinkFetchClient linkFetchClient;
	@Mock
	private SecurityMetrics securityMetrics;

	private LinkMetadataServiceImpl serviceUnderTest() {
		return new LinkMetadataServiceImpl(linkFetchClient, securityMetrics, new LessonTextUtil(),
				new LinkHtmlParser());
	}

	@Test
	void fetchShouldParseTitleDescriptionImageSiteNameAndTextFromASuccessfulResultTest() {
		// Given:
		String html = "<html><head>"
				+ "<title>Fallback Title</title>"
				+ "<meta property=\"og:title\" content=\"Real Title\">"
				+ "<meta name=\"description\" content=\"A short description\">"
				+ "<meta property=\"og:image\" content=\"https://example.com/cover.png\">"
				+ "<meta property=\"og:site_name\" content=\"Example Site\">"
				+ "</head><body><article><p>Main article content.</p></article></body></html>";
		when(linkFetchClient.fetch("https://example.com/article")).thenReturn(
				LinkFetchResult.success(html, "text/html"));

		// When:
		Map<String, Object> result = serviceUnderTest().fetch("https://example.com/article");

		// Then:
		assertThat(result.get("title")).isEqualTo("Real Title");
		assertThat(result.get("description")).isEqualTo("A short description");
		assertThat(result.get("imageUrl")).isEqualTo("https://example.com/cover.png");
		assertThat(result.get("siteName")).isEqualTo("Example Site");
		assertThat(result.get("extractedText")).isEqualTo("Main article content.");
		assertThat(result.get("error")).isEqualTo("");
		verify(securityMetrics, never()).ssrfBlocked(org.mockito.ArgumentMatchers.any());
	}

	@Test
	void fetchShouldFallBackToHtmlTitleTagAndHostnameWhenMetaTagsAreAbsentTest() {
		// Given:
		String html = "<html><head><title>Plain Title</title></head><body><p>Body text.</p></body></html>";
		when(linkFetchClient.fetch("https://www.example.com/page")).thenReturn(
				LinkFetchResult.success(html, "text/html"));

		// When:
		Map<String, Object> result = serviceUnderTest().fetch("https://www.example.com/page");

		// Then:
		assertThat(result.get("title")).isEqualTo("Plain Title");
		assertThat(result.get("siteName")).isEqualTo("example.com");
	}

	@Test
	void fetchShouldRecordSsrfBlockedMetricAndReturnErrorWhenSecurityPolicyBlocksTheFetchTest() {
		// Given:
		when(linkFetchClient.fetch("http://169.254.169.254/latest/meta-data/")).thenReturn(
				LinkFetchResult.securityBlocked(LinkFetchFailureReason.PRIVATE_ADDRESS, "Blocked by outbound link " +
						"policy."));

		// When:
		Map<String, Object> result = serviceUnderTest().fetch("http://169.254.169.254/latest/meta-data/");

		// Then:
		assertThat(result.get("error")).isEqualTo("Blocked by outbound link policy.");
		assertThat(result.get("title")).isEqualTo("");
		verify(securityMetrics).ssrfBlocked(SsrfBlockReason.PRIVATE_ADDRESS);
	}

	@Test
	void fetchShouldNotRecordSsrfBlockedMetricForAnOrdinaryNetworkFailureTest() {
		// Given:
		when(linkFetchClient.fetch("https://unreachable.example.com")).thenReturn(
				LinkFetchResult.failure("HTTP 404"));

		// When:
		Map<String, Object> result = serviceUnderTest().fetch("https://unreachable.example.com");

		// Then:
		assertThat(result.get("error")).isEqualTo("HTTP 404");
		verify(securityMetrics, never()).ssrfBlocked(org.mockito.ArgumentMatchers.any());
	}

	@Test
	void toSsrfBlockReasonShouldMapEveryExternalReasonToItsMatchingSecurityMetricReasonTest() {
		// When-Then:
		assertThat(serviceUnderTest().toSsrfBlockReason(LinkFetchFailureReason.DISALLOWED_SCHEME))
				.isEqualTo(SsrfBlockReason.DISALLOWED_SCHEME);
		assertThat(serviceUnderTest().toSsrfBlockReason(LinkFetchFailureReason.USERINFO_PRESENT))
				.isEqualTo(SsrfBlockReason.USERINFO_PRESENT);
		assertThat(serviceUnderTest().toSsrfBlockReason(LinkFetchFailureReason.DISALLOWED_PORT))
				.isEqualTo(SsrfBlockReason.DISALLOWED_PORT);
		assertThat(serviceUnderTest().toSsrfBlockReason(LinkFetchFailureReason.PRIVATE_ADDRESS))
				.isEqualTo(SsrfBlockReason.PRIVATE_ADDRESS);
		assertThat(serviceUnderTest().toSsrfBlockReason(LinkFetchFailureReason.REDIRECT_LIMIT_EXCEEDED))
				.isEqualTo(SsrfBlockReason.REDIRECT_LIMIT_EXCEEDED);
		assertThat(serviceUnderTest().toSsrfBlockReason(LinkFetchFailureReason.RESPONSE_TOO_LARGE))
				.isEqualTo(SsrfBlockReason.RESPONSE_TOO_LARGE);
		assertThat(serviceUnderTest().toSsrfBlockReason(LinkFetchFailureReason.UNSUPPORTED_CONTENT_TYPE))
				.isEqualTo(SsrfBlockReason.UNSUPPORTED_CONTENT_TYPE);
		assertThat(serviceUnderTest().toSsrfBlockReason(LinkFetchFailureReason.TIMEOUT))
				.isEqualTo(SsrfBlockReason.TIMEOUT);
	}

	@Test
	void extractMainTextShouldTruncateVeryLongTextAt10000CharactersTest() {
		// Given:
		String longParagraph = "word ".repeat(3000);
		String html = "<article><p>" + longParagraph + "</p></article>";
		when(linkFetchClient.fetch("https://example.com/long")).thenReturn(
				LinkFetchResult.success(html, "text/html"));

		// When:
		Map<String, Object> result = serviceUnderTest().fetch("https://example.com/long");

		// Then:
		String extractedText = (String) result.get("extractedText");
		assertThat(extractedText).endsWith("...");
		assertThat(extractedText.length()).isEqualTo(10003);
	}
}
