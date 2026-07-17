package com.aidigital.aionboarding.service.lesson.util;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.stream.Collectors;

@Component
public class OpenAiPromptCacheUtil {

	public String buildOpenAIPromptCacheKey(String prefix, Object... parts) {
		return build(prefix, parts);
	}

	public String build(String prefix, Object... parts) {
		String safePrefix = String.valueOf(prefix == null ? "prompt" : prefix)
				.toLowerCase()
				.replaceAll("[^a-z0-9_-]", "-");
		if (safePrefix.length() > 32) {
			safePrefix = safePrefix.substring(0, 32);
		}

		String joined = Arrays.stream(parts)
				.filter(part -> part != null && !String.valueOf(part).isBlank())
				.map(String::valueOf)
				.collect(Collectors.joining(":"));

		String fingerprint = sha256(joined).substring(0, 24);
		return safePrefix + ":" + fingerprint;
	}

	String sha256(String value) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
			StringBuilder builder = new StringBuilder(hash.length * 2);
			for (byte b : hash) {
				builder.append(String.format("%02x", b));
			}
			return builder.toString();
		} catch (NoSuchAlgorithmException ex) {
			throw new IllegalStateException("SHA-256 not available", ex);
		}
	}
}
