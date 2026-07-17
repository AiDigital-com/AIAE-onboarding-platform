package com.aidigital.aionboarding.service.material.validation;

import com.aidigital.aionboarding.service.common.error.AppException;
import com.aidigital.aionboarding.service.common.error.ErrorReason;
import com.aidigital.aionboarding.service.lesson.util.LessonTagUtil;
import com.aidigital.aionboarding.service.material.models.CreateMaterialInput;
import com.aidigital.aionboarding.service.material.models.MaterialAttachmentInput;
import com.aidigital.aionboarding.service.material.models.UpdateMaterialInput;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Validates and normalizes material create/update payloads before persistence.
 */
@Component
@RequiredArgsConstructor
public class MaterialPayloadValidator {

	private final LessonTagUtil lessonTagUtil;

	/**
	 * Validates a create-material request payload.
	 *
	 * @param input raw create input
	 * @return normalized payload ready for persistence
	 * @throws AppException {@link ErrorReason#C002} when title is blank or no content source is provided
	 */
	public ValidatedMaterialPayload validateCreateInput(CreateMaterialInput input) {
		return validatePayload(
				input.title(),
				input.description(),
				input.text(),
				input.youtubeUrls(),
				input.links(),
				input.attachments(),
				input.tags()
		);
	}

	/**
	 * Validates an update-material request payload.
	 *
	 * @param input raw update input
	 * @return normalized payload ready for persistence
	 * @throws AppException {@link ErrorReason#C002} when title is blank or no content source is provided
	 */
	public ValidatedMaterialPayload validateUpdateInput(UpdateMaterialInput input) {
		return validatePayload(
				input.title(),
				input.description(),
				input.text(),
				input.youtubeUrls(),
				input.links(),
				input.attachments(),
				input.tags()
		);
	}

	ValidatedMaterialPayload validatePayload(
			String title,
			String description,
			String text,
			List<String> youtubeUrls,
			List<String> links,
			List<MaterialAttachmentInput> attachments,
			List<String> tags
	) {
		String normalizedTitle = stringVal(title).trim();
		String normalizedDescription = stringVal(description).trim();
		String normalizedText = stringVal(text).trim();
		List<String> normalizedYoutubeUrls = normalizeStringList(youtubeUrls);
		List<String> normalizedLinks = normalizeStringList(links);
		List<MaterialAttachmentInput> normalizedAttachments = attachments == null ? List.of() : attachments;
		List<String> normalizedTags = lessonTagUtil.normalizeLessonTagInput(tags);

		boolean hasAnyContent = !normalizedYoutubeUrls.isEmpty()
				|| !normalizedLinks.isEmpty()
				|| !normalizedText.isBlank()
				|| !normalizedAttachments.isEmpty();

		if (normalizedTitle.isBlank()) {
			throw new AppException(ErrorReason.C002, "Title is required.");
		}
		if (!hasAnyContent) {
			throw new AppException(ErrorReason.C002, "At least one content source is required.");
		}

		return new ValidatedMaterialPayload(
				normalizedTitle,
				normalizedDescription,
				normalizedText,
				normalizedYoutubeUrls,
				normalizedLinks,
				normalizedAttachments,
				normalizedTags
		);
	}

	List<String> normalizeStringList(List<String> values) {
		if (values == null) {
			return List.of();
		}
		return values.stream()
				.map(this::stringVal)
				.map(String::trim)
				.filter(value -> !value.isBlank())
				.toList();
	}

	String stringVal(Object value) {
		return value == null ? "" : String.valueOf(value);
	}

	/**
	 * Normalized material payload produced by validation.
	 */
	public record ValidatedMaterialPayload(
			String title,
			String description,
			String text,
			List<String> youtubeUrls,
			List<String> links,
			List<MaterialAttachmentInput> attachments,
			List<String> tags
	) {

	}
}
