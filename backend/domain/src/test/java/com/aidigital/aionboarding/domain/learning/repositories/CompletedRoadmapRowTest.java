package com.aidigital.aionboarding.domain.learning.repositories;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CompletedRoadmapRowTest {

	@Test
	void getIdAndGetTitleShouldDelegateToRecordComponentsTest() {
		// Given
		CompletedRoadmapRow row = new CompletedRoadmapRow(7L, "Roadmap title");

		// When / Then: the CompletedRoadmapProjection overrides delegate to the record components
		assertThat(row.getId()).isEqualTo(7L);
		assertThat(row.getTitle()).isEqualTo("Roadmap title");
	}
}
