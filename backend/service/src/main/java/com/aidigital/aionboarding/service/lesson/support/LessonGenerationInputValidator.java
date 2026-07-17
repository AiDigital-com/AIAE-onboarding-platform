package com.aidigital.aionboarding.service.lesson.support;

import com.aidigital.aionboarding.domain.material.entities.Material;
import com.aidigital.aionboarding.service.common.error.AppException;
import com.aidigital.aionboarding.service.common.error.ErrorReason;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Validates and normalizes lesson-generation inputs before draft creation, covering
 * material-id normalization, manual-mode validation, AI-path material usability
 * checks, and draft-title derivation.
 */
@Component
public class LessonGenerationInputValidator {

	/**
	 * Returns a deduplicated copy of materialIds preserving insertion order.
	 */
	public List<Long> deduplicatePreserveOrder(List<Long> materialIds) {
		Set<Long> seen = new LinkedHashSet<>();
		List<Long> result = new ArrayList<>();
		for (Long id : materialIds) {
			if (seen.add(id)) {
				result.add(id);
			}
		}
		return result;
	}

	/**
	 * Validates that a manual-mode lesson request has a usable title and content.
	 *
	 * @param title       raw manual lesson title
	 * @param contentHtml raw manual lesson content
	 * @throws AppException V001 if the title or content is blank
	 */
	public void validateManualLesson(String title, String contentHtml) {
		if (title == null || title.isBlank() || contentHtml == null || contentHtml.isBlank()) {
			throw new AppException(ErrorReason.V001,
					"Manual lesson requires a title and non-empty content.");
		}
	}

	/**
	 * Validates that the resolved materials are usable for generation.
	 *
	 * @throws AppException V001 if validation fails
	 */
	public void validateMaterialsUsable(List<Long> requestedIds, List<Material> materials, String instructions) {
		boolean hasInstructions = instructions != null && !instructions.isBlank();
		if (requestedIds.isEmpty()) {
			if (!hasInstructions) {
				throw new AppException(ErrorReason.V001,
						"Select at least one material or describe what the lesson should be about.");
			}
			return;
		}
		if (materials.isEmpty() && !hasInstructions) {
			throw new AppException(ErrorReason.V001,
					"Select at least one material or describe what the lesson should be about.");
		}
		for (Material material : materials) {
			if (!isMaterialUsable(material)) {
				throw new AppException(ErrorReason.V001,
						"Material " + material.getId() + " has no usable content.");
			}
		}
	}

	/**
	 * Returns true if the material has at least one non-blank text field.
	 */
	boolean isMaterialUsable(Material material) {
		if (material == null) {
			return false;
		}
		return isNonBlank(material.getTitle()) || isNonBlank(material.getDescription()) || isNonBlank(material.getTextContent());
	}

	/**
	 * Builds a human-readable draft title from material titles.
	 */
	public String buildDraftTitle(List<Material> materials) {
		List<String> titles = materials.stream()
				.map(Material::getTitle)
				.filter(this::isNonBlank)
				.toList();

		switch (titles.size()) {
			case 0:
				return "Generating theoretical lesson";
			case 1:
				return titles.get(0);
			case 2:
				return titles.get(0) + " + " + titles.get(1);
			default:
				return titles.get(0) + " and " + (titles.size() - 1) + " more materials";
		}
	}

	/**
	 * Returns true if the string is non-null and non-blank.
	 */
	boolean isNonBlank(String value) {
		return value != null && !value.isBlank();
	}
}
