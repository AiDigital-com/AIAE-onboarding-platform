package com.aidigital.aionboarding.service.lesson.util;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class LessonContentUtilTest {

	private final LessonContentUtil util = new LessonContentUtil();

	@Nested
	class LooksLikeHtml {

		@Test
		void shouldReturnFalseWhenValueIsNullTest() {
			// When-Then:
			assertThat(util.looksLikeHtml(null)).isFalse();
		}

		@Test
		void shouldReturnFalseWhenValueIsEmptyTest() {
			// When-Then:
			assertThat(util.looksLikeHtml("")).isFalse();
		}

		@Test
		void shouldReturnFalseWhenValueIsBlankTest() {
			// When-Then:
			assertThat(util.looksLikeHtml("   ")).isFalse();
		}

		@Test
		void shouldReturnFalseWhenValueHasNoHtmlTagsTest() {
			// Given: text containing angle brackets used as comparison operators, not tags
			String value = "3 < 5 and 5 > 3, plain text only";

			// When-Then:
			assertThat(util.looksLikeHtml(value)).isFalse();
		}

		@Test
		void shouldReturnTrueWhenValueContainsAnOpeningHtmlTagTest() {
			// When-Then:
			assertThat(util.looksLikeHtml("<p>Paragraph</p>")).isTrue();
		}

		@Test
		void shouldReturnTrueWhenValueContainsOnlyAClosingHtmlTagTest() {
			// When-Then:
			assertThat(util.looksLikeHtml("some text </div> more text")).isTrue();
		}
	}

	@Nested
	class ExtractHtmlTitle {

		@Test
		void shouldReturnEmptyStringWhenHtmlIsNullTest() {
			// When-Then:
			assertThat(util.extractHtmlTitle(null)).isEqualTo("");
		}

		@Test
		void shouldReturnEmptyStringWhenHtmlIsBlankTest() {
			// When-Then:
			assertThat(util.extractHtmlTitle("   ")).isEqualTo("");
		}

		@Test
		void shouldReturnEmptyStringWhenNoH1TagIsPresentTest() {
			// Given:
			String html = "<html><body><h2>Not an h1</h2><p>Body text</p></body></html>";

			// When-Then:
			assertThat(util.extractHtmlTitle(html)).isEqualTo("");
		}

		@Test
		void shouldStripNestedTagsAndCollapseWhitespaceFromH1ContentTest() {
			// Given: an h1 whose content contains nested markup and irregular whitespace
			String html = "<html><body><h1>Hello  <span>World</span>   Foo</h1><p>Body</p></body></html>";

			// When:
			String title = util.extractHtmlTitle(html);

			// Then:
			assertThat(title).isEqualTo("Hello World Foo");
		}

		@Test
		void shouldNotTruncateWhenTitleLengthIsExactlyOneHundredTwentyTest() {
			// Given: a title exactly at the truncation boundary (not exceeding it)
			String title120 = "A".repeat(120);
			String html = "<h1>" + title120 + "</h1>";

			// When:
			String result = util.extractHtmlTitle(html);

			// Then:
			assertThat(result).isEqualTo(title120);
			assertThat(result).hasSize(120);
		}

		@Test
		void shouldTruncateAndAppendEllipsisWhenTitleLengthExceedsOneHundredTwentyTest() {
			// Given: a title one character past the truncation boundary
			String title121 = "B".repeat(121);
			String html = "<h1>" + title121 + "</h1>";

			// When:
			String result = util.extractHtmlTitle(html);

			// Then:
			assertThat(result).isEqualTo("B".repeat(117) + "...");
			assertThat(result).hasSize(120);
		}

		@Test
		void shouldMatchH1CaseInsensitivelyTest() {
			// Given:
			String html = "<H1>Upper Case Tag</H1>";

			// When-Then:
			assertThat(util.extractHtmlTitle(html)).isEqualTo("Upper Case Tag");
		}
	}

	@Nested
	class MarkdownToHtml {

		@Test
		void shouldReturnEmptyStringWhenMarkdownIsNullTest() {
			// When-Then:
			assertThat(util.markdownToHtml(null)).isEqualTo("");
		}

		@Test
		void shouldReturnEmptyStringWhenMarkdownIsEmptyTest() {
			// When-Then:
			assertThat(util.markdownToHtml("")).isEqualTo("");
		}

		@Test
		void shouldWrapPlainTextLineInParagraphTagTest() {
			// When:
			String result = util.markdownToHtml("Hello world");

			// Then:
			assertThat(result).isEqualTo("<p>Hello world</p>");
		}

		@Test
		void shouldJoinConsecutiveNonBlankLinesIntoOneParagraphWithoutTrailingBlankLineTest() {
			// Given: two plain-text lines with no blank line or trailing blank line to force the
			// final end-of-input flush to run
			String markdown = "Line one\nLine two";

			// When:
			String result = util.markdownToHtml(markdown);

			// Then:
			assertThat(result).isEqualTo("<p>Line one Line two</p>");
		}

		@Test
		void shouldSeparateParagraphsDividedByABlankLineTest() {
			// Given:
			String markdown = "Para one line1\nPara one line2\n\nPara two";

			// When:
			String result = util.markdownToHtml(markdown);

			// Then:
			assertThat(result).isEqualTo("<p>Para one line1 Para one line2</p>\n<p>Para two</p>");
		}

		@Test
		void shouldNormalizeWindowsLineEndingsTest() {
			// Given: CRLF line endings that must be normalized to LF before splitting
			String markdown = "Line1\r\nLine2";

			// When:
			String result = util.markdownToHtml(markdown);

			// Then:
			assertThat(result).isEqualTo("<p>Line1 Line2</p>");
		}

		@Test
		void shouldConvertHeadingLevelsOneThroughFourTest() {
			// Given:
			String markdown = "# H1\n## H2\n### H3\n#### H4";

			// When:
			String result = util.markdownToHtml(markdown);

			// Then:
			assertThat(result).isEqualTo("<h1>H1</h1>\n<h2>H2</h2>\n<h3>H3</h3>\n<h4>H4</h4>");
		}

		@Test
		void shouldApplyInlineMarkdownFormattingWithinHeadingTextTest() {
			// When:
			String result = util.markdownToHtml("# Hello **World**");

			// Then:
			assertThat(result).isEqualTo("<h1>Hello <strong>World</strong></h1>");
		}

		@Test
		void shouldFallBackToParagraphWhenHeadingHasMoreThanFourHashesTest() {
			// Given: five leading hashes never satisfy the "#{1,4}" + whitespace heading pattern,
			// so the line falls through to plain paragraph handling
			String markdown = "##### Not a heading";

			// When:
			String result = util.markdownToHtml(markdown);

			// Then:
			assertThat(result).isEqualTo("<p>##### Not a heading</p>");
		}

		@Test
		void shouldGroupConsecutiveUnorderedListItemsIntoOneListTest() {
			// Given:
			String markdown = "- Item one\n- Item two";

			// When:
			String result = util.markdownToHtml(markdown);

			// Then:
			assertThat(result).isEqualTo("<ul>\n<li>Item one</li>\n<li>Item two</li>\n</ul>");
		}

		@Test
		void shouldSupportAsteriskBulletMarkerForUnorderedListsTest() {
			// When:
			String result = util.markdownToHtml("* Item one\n* Item two");

			// Then:
			assertThat(result).isEqualTo("<ul>\n<li>Item one</li>\n<li>Item two</li>\n</ul>");
		}

		@Test
		void shouldGroupConsecutiveOrderedListItemsIntoOneListTest() {
			// Given:
			String markdown = "1. First\n2. Second";

			// When:
			String result = util.markdownToHtml(markdown);

			// Then:
			assertThat(result).isEqualTo("<ol>\n<li>First</li>\n<li>Second</li>\n</ol>");
		}

		@Test
		void shouldFlushUnorderedListBeforeStartingOrderedListTest() {
			// Given: a switch from unordered to ordered items mid-stream must close out the
			// unordered list before opening the new ordered one
			String markdown = "- U1\n1. O1";

			// When:
			String result = util.markdownToHtml(markdown);

			// Then:
			assertThat(result).isEqualTo("<ul>\n<li>U1</li>\n</ul>\n<ol>\n<li>O1</li>\n</ol>");
		}

		@Test
		void shouldFlushOrderedListBeforeStartingUnorderedListTest() {
			// Given: the mirror-image switch, ordered to unordered
			String markdown = "1. O1\n- U1";

			// When:
			String result = util.markdownToHtml(markdown);

			// Then:
			assertThat(result).isEqualTo("<ol>\n<li>O1</li>\n</ol>\n<ul>\n<li>U1</li>\n</ul>");
		}

		@Test
		void shouldConvertBlockquoteLineTest() {
			// When:
			String result = util.markdownToHtml("> Quoted text");

			// Then:
			assertThat(result).isEqualTo("<blockquote><p>Quoted text</p></blockquote>");
		}

		@Test
		void shouldFlushPendingParagraphAndListBeforeAHeadingTest() {
			// Given: a paragraph and an open list must both be flushed once a heading line arrives
			String markdown = "Some text\n- Item one\n# Heading";

			// When:
			String result = util.markdownToHtml(markdown);

			// Then:
			assertThat(result).isEqualTo("<p>Some text</p>\n<ul>\n<li>Item one</li>\n</ul>\n<h1>Heading</h1>");
		}
	}

	@Nested
	class FlushParagraph {

		@Test
		void shouldDoNothingWhenParagraphIsEmptyTest() {
			// Given:
			List<String> output = new ArrayList<>();
			List<String> paragraph = new ArrayList<>();

			// When:
			util.flushParagraph(output, paragraph);

			// Then:
			assertThat(output).isEmpty();
			assertThat(paragraph).isEmpty();
		}

		@Test
		void shouldWrapJoinedParagraphLinesInPTagAndClearItTest() {
			// Given:
			List<String> output = new ArrayList<>();
			List<String> paragraph = new ArrayList<>(List.of("Hello", "World"));

			// When:
			util.flushParagraph(output, paragraph);

			// Then:
			assertThat(output).containsExactly("<p>Hello World</p>");
			assertThat(paragraph).isEmpty();
		}
	}

	@Nested
	class FlushList {

		@Test
		void shouldDoNothingWhenListItemsIsEmptyTest() {
			// Given:
			List<String> output = new ArrayList<>();
			List<String> listItems = new ArrayList<>();

			// When:
			util.flushList(output, listItems, true);

			// Then:
			assertThat(output).isEmpty();
		}

		@Test
		void shouldWrapItemsInOlTagsWhenOrderedIsTrueTest() {
			// Given:
			List<String> output = new ArrayList<>();
			List<String> listItems = new ArrayList<>(List.of("First", "Second"));

			// When:
			util.flushList(output, listItems, true);

			// Then:
			assertThat(output).containsExactly("<ol>", "<li>First</li>", "<li>Second</li>", "</ol>");
			assertThat(listItems).isEmpty();
		}

		@Test
		void shouldWrapItemsInUlTagsWhenOrderedIsFalseTest() {
			// Given:
			List<String> output = new ArrayList<>();
			List<String> listItems = new ArrayList<>(List.of("First", "Second"));

			// When:
			util.flushList(output, listItems, false);

			// Then:
			assertThat(output).containsExactly("<ul>", "<li>First</li>", "<li>Second</li>", "</ul>");
			assertThat(listItems).isEmpty();
		}
	}

	@Nested
	class FormatInlineMarkdown {

		@Test
		void shouldReturnEscapedTextUnchangedWhenNoMarkdownSyntaxPresentTest() {
			// When:
			String result = util.formatInlineMarkdown("Plain <text> & stuff");

			// Then:
			assertThat(result).isEqualTo("Plain &lt;text&gt; &amp; stuff");
		}

		@Test
		void shouldConvertBoldSyntaxTest() {
			// When-Then:
			assertThat(util.formatInlineMarkdown("**bold**")).isEqualTo("<strong>bold</strong>");
		}

		@Test
		void shouldConvertItalicSyntaxTest() {
			// When-Then:
			assertThat(util.formatInlineMarkdown("*italic*")).isEqualTo("<em>italic</em>");
		}

		@Test
		void shouldConvertInlineCodeSyntaxTest() {
			// When-Then:
			assertThat(util.formatInlineMarkdown("`code`")).isEqualTo("<code>code</code>");
		}

		@Test
		void shouldConvertMultipleBoldSegmentsNonGreedilyTest() {
			// Given: non-greedy matching must keep the two bold segments separate rather than
			// spanning from the first "**" to the last "**"
			String value = "**A** and **B**";

			// When:
			String result = util.formatInlineMarkdown(value);

			// Then:
			assertThat(result).isEqualTo("<strong>A</strong> and <strong>B</strong>");
		}

		@Test
		void shouldEscapeHtmlBeforeApplyingMarkdownFormattingTest() {
			// Given: HTML-special characters inside bold markdown syntax
			String value = "**<b>bold</b>**";

			// When:
			String result = util.formatInlineMarkdown(value);

			// Then:
			assertThat(result).isEqualTo("<strong>&lt;b&gt;bold&lt;/b&gt;</strong>");
		}

		@Test
		void shouldCombineBoldItalicAndCodeInASingleStringTest() {
			// When:
			String result = util.formatInlineMarkdown("**bold** and *italic* and `code`");

			// Then:
			assertThat(result).isEqualTo("<strong>bold</strong> and <em>italic</em> and <code>code</code>");
		}
	}

	@Nested
	class EscapeHtml {

		@Test
		void shouldEscapeAllFiveSpecialCharactersTest() {
			// When:
			String result = util.escapeHtml("&<>\"'");

			// Then:
			assertThat(result).isEqualTo("&amp;&lt;&gt;&quot;&#039;");
		}

		@Test
		void shouldReturnStringUnchangedWhenNoSpecialCharactersPresentTest() {
			// When-Then:
			assertThat(util.escapeHtml("Hello World 123")).isEqualTo("Hello World 123");
		}
	}
}
