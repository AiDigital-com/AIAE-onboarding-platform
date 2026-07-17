package com.aidigital.aionboarding.service.material.models;

import java.util.List;

/**
 * Example and caveat sentences extracted from prepared materials.
 *
 * @param examples example sentences that illustrate key concepts
 * @param caveats  caveat sentences that highlight warnings or edge cases
 */
public record SignalNotes(
		List<SignalItem> examples,
		List<SignalItem> caveats
) {

}
