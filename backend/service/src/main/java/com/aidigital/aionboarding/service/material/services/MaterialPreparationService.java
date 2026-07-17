package com.aidigital.aionboarding.service.material.services;

import com.aidigital.aionboarding.service.material.models.PreparedMaterialsResult;

import java.util.List;

/**
 * Loads and prepares lesson materials into a typed, prompt-ready result.
 */
public interface MaterialPreparationService {

	/**
	 * Loads and prepares all materials linked to a lesson in sort order.
	 *
	 * @param lessonId lesson identifier
	 * @return typed prepared materials result including materials, source references,
	 * extracted terms, signals, overlaps, and statistics
	 * @throws IllegalStateException when linked materials exceed MVP limits (8 materials or
	 *                               80,000 combined text characters)
	 */
	PreparedMaterialsResult prepareForLesson(Long lessonId);

	/**
	 * Loads and prepares an explicit list of materials by identifier.
	 *
	 * @param materialIds material identifiers to include; unknown ids are skipped
	 * @return typed prepared materials result including materials, source references,
	 * extracted terms, signals, overlaps, and statistics
	 * @throws IllegalStateException when the selection exceeds MVP limits (8 materials or
	 *                               80,000 combined text characters)
	 */
	PreparedMaterialsResult prepareForMaterialIds(List<Long> materialIds);
}
