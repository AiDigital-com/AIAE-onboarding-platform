package com.aidigital.aionboarding.domain.material.repositories;

import com.aidigital.aionboarding.domain.material.entities.Material;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exercises {@link MaterialRepositoryImpl#searchSummaries} and its shared {@code countMatching}
 * helper against a real H2/PostgreSQL-mode schema, entirely within {@code domain} (see
 * domain/pom.xml for why not Testcontainers here).
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class MaterialRepositoryImplIntegrationTest {

	@Autowired
	private MaterialRepository materialRepository;

	@Test
	void shouldReturnBoundedProjectionPageWithHasTextFlagTest() {
		// Given: one material with body text, one without
		materialRepository.save(material("With body", "some content here"));
		materialRepository.save(material("Empty body", ""));

		// When
		Specification<Material> matchAll = (root, query, cb) -> cb.conjunction();
		Page<MaterialSearchSummaryProjection> page = materialRepository.searchSummaries(matchAll, PageRequest.of(0, 10));

		// Then
		assertThat(page.getTotalElements()).isEqualTo(2);
		assertThat(page.getContent())
				.filteredOn(p -> p.title().equals("With body"))
				.extracting(MaterialSearchSummaryProjection::hasText)
				.containsExactly(true);
		assertThat(page.getContent())
				.filteredOn(p -> p.title().equals("Empty body"))
				.extracting(MaterialSearchSummaryProjection::hasText)
				.containsExactly(false);
	}

	@Test
	void shouldFilterByTitleSpecificationTest() {
		// Given: two materials with different titles
		materialRepository.save(material("Alpha", "content"));
		materialRepository.save(material("Beta", "content"));
		Specification<Material> titledAlpha = (root, query, cb) -> cb.equal(root.get("title"), "Alpha");

		// When
		Page<MaterialSearchSummaryProjection> page = materialRepository.searchSummaries(titledAlpha, PageRequest.of(0, 10));

		// Then
		assertThat(page.getContent()).extracting(MaterialSearchSummaryProjection::title).containsExactly("Alpha");
		assertThat(page.getTotalElements()).isEqualTo(1);
	}

	private Material material(String title, String textContent) {
		LocalDateTime now = LocalDateTime.of(2026, 1, 1, 0, 0);
		Material material = new Material();
		material.setTitle(title);
		material.setDescription("description");
		material.setTextContent(textContent);
		material.setCoverImageStorageKey("");
		material.setCoverImageOriginalName("");
		material.setCoverImageMimeType("");
		material.setTags(List.of());
		material.setCreatedBy("tester");
		material.setCreatedAt(now);
		material.setUpdatedAt(now);
		return material;
	}
}
