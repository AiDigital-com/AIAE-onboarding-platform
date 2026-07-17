package com.aidigital.aionboarding.service.material.models;

import java.util.List;

/**
 * Detected overlaps across prepared materials.
 *
 * @param duplicateTitles titles shared by multiple materials
 * @param duplicateUrls   URLs shared by multiple materials
 */
public record OverlapNotes(
		List<DuplicateTitle> duplicateTitles,
		List<DuplicateUrl> duplicateUrls
) {

}
