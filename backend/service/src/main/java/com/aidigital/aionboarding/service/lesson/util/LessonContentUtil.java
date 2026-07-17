package com.aidigital.aionboarding.service.lesson.util;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class LessonContentUtil {

	private static final Pattern HTML_TAG_PATTERN = Pattern.compile("<\\/?[a-z][\\s\\S]*>", Pattern.CASE_INSENSITIVE);
	private static final Pattern H1_PATTERN = Pattern.compile("<h1[^>]*>([\\s\\S]*?)</h1>", Pattern.CASE_INSENSITIVE);

	public boolean looksLikeHtml(String value) {
		if (value == null || value.isBlank()) {
			return false;
		}
		return HTML_TAG_PATTERN.matcher(value).find();
	}

	public String extractHtmlTitle(String html) {
		if (html == null || html.isBlank()) {
			return "";
		}
		Matcher matcher = H1_PATTERN.matcher(html);
		if (!matcher.find()) {
			return "";
		}
		String title = matcher.group(1)
				.replaceAll("<[^>]+>", " ")
				.replaceAll("\\s+", " ")
				.trim();
		if (title.length() > 120) {
			return title.substring(0, 117) + "...";
		}
		return title;
	}

	public String markdownToHtml(String markdown) {
		if (markdown == null) {
			return "";
		}
		String[] lines = markdown.replace("\r\n", "\n").split("\n", -1);
		List<String> output = new ArrayList<>();
		List<String> listItems = new ArrayList<>();
		boolean orderedList = false;
		List<String> paragraph = new ArrayList<>();

		for (String line : lines) {
			String trimmed = line.trim();
			if (trimmed.isEmpty()) {
				flushParagraph(output, paragraph);
				flushList(output, listItems, orderedList);
				continue;
			}

			Matcher heading = Pattern.compile("^(#{1,4})\\s+(.+)$").matcher(trimmed);
			if (heading.matches()) {
				flushParagraph(output, paragraph);
				flushList(output, listItems, orderedList);
				int level = heading.group(1).length();
				output.add("<h" + level + ">" + formatInlineMarkdown(heading.group(2)) + "</h" + level + ">");
				continue;
			}

			Matcher unordered = Pattern.compile("^[-*]\\s+(.+)$").matcher(trimmed);
			if (unordered.matches()) {
				flushParagraph(output, paragraph);
				if (!listItems.isEmpty() && orderedList) {
					flushList(output, listItems, orderedList);
				}
				orderedList = false;
				listItems.add(unordered.group(1));
				continue;
			}

			Matcher ordered = Pattern.compile("^\\d+\\.\\s+(.+)$").matcher(trimmed);
			if (ordered.matches()) {
				flushParagraph(output, paragraph);
				if (!listItems.isEmpty() && !orderedList) {
					flushList(output, listItems, orderedList);
				}
				orderedList = true;
				listItems.add(ordered.group(1));
				continue;
			}

			if (trimmed.startsWith("> ")) {
				flushParagraph(output, paragraph);
				flushList(output, listItems, orderedList);
				output.add("<blockquote><p>" + formatInlineMarkdown(trimmed.substring(2)) + "</p></blockquote>");
				continue;
			}

			paragraph.add(trimmed);
		}

		flushParagraph(output, paragraph);
		flushList(output, listItems, orderedList);
		return String.join("\n", output);
	}

	void flushParagraph(List<String> output, List<String> paragraph) {
		if (paragraph.isEmpty()) {
			return;
		}
		output.add("<p>" + formatInlineMarkdown(String.join(" ", paragraph)) + "</p>");
		paragraph.clear();
	}

	void flushList(List<String> output, List<String> listItems, boolean ordered) {
		if (listItems.isEmpty()) {
			return;
		}
		String tag = ordered ? "ol" : "ul";
		output.add("<" + tag + ">");
		for (String item : listItems) {
			output.add("<li>" + formatInlineMarkdown(item) + "</li>");
		}
		output.add("</" + tag + ">");
		listItems.clear();
	}

	String formatInlineMarkdown(String value) {
		return escapeHtml(value)
				.replaceAll("\\*\\*(.+?)\\*\\*", "<strong>$1</strong>")
				.replaceAll("\\*(.+?)\\*", "<em>$1</em>")
				.replaceAll("`(.+?)`", "<code>$1</code>");
	}

	String escapeHtml(String value) {
		return value
				.replace("&", "&amp;")
				.replace("<", "&lt;")
				.replace(">", "&gt;")
				.replace("\"", "&quot;")
				.replace("'", "&#039;");
	}
}
