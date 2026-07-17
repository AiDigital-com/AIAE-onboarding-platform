package com.aidigital.aionboarding.service.lesson.util;

import com.aidigital.aionboarding.service.lesson.models.ChatTurn;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class LessonTextUtil {

	private static final int MAX_HISTORY_MESSAGES = 8;

	public String normalizeText(String value) {
		if (value == null) {
			return "";
		}
		return value
				.replace("\r\n", "\n")
				.replaceAll("[ \\t]+", " ")
				.replaceAll("\n{3,}", "\n\n")
				.trim();
	}

	public String compactText(String value) {
		if (value == null) {
			return "";
		}
		return value
				.replace("\r\n", "\n")
				.replaceAll("(?i)<script[\\s\\S]*?</script>", " ")
				.replaceAll("(?i)<style[\\s\\S]*?</style>", " ")
				.replaceAll("<[^>]+>", " ")
				.replace("&nbsp;", " ")
				.replace("&amp;", "&")
				.replace("&lt;", "<")
				.replace("&gt;", ">")
				.replace("&quot;", "\"")
				.replace("&#039;", "'")
				.replaceAll("[ \\t]+", " ")
				.replaceAll("\n{3,}", "\n\n")
				.trim();
	}

	public String truncateText(String value, int limit) {
		String text = compactText(value);
		if (text.length() <= limit) {
			return text;
		}
		return text.substring(0, limit) + "\n\n[CONTENT TRUNCATED]";
	}

	public List<Map<String, Object>> normalizeHistory(List<ChatTurn> history) {
		if (history == null) {
			return List.of();
		}
		List<Map<String, Object>> normalized = new ArrayList<>();
		for (ChatTurn item : history) {
			if (item == null) {
				continue;
			}
			String role = item.role();
			if (!"user".equals(role) && !"assistant".equals(role)) {
				continue;
			}
			String content = compactText(item.content());
			if (content.length() > 2000) {
				content = content.substring(0, 2000);
			}
			if (content.isBlank()) {
				continue;
			}
			normalized.add(Map.of("role", role, "content", content));
		}
		int from = Math.max(0, normalized.size() - MAX_HISTORY_MESSAGES);
		return normalized.subList(from, normalized.size());
	}
}
