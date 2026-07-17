package com.aidigital.aionboarding.material;

import com.aidigital.aionboarding.domain.material.entities.Material;
import com.aidigital.aionboarding.domain.material.repositories.MaterialRepository;
import com.aidigital.aionboarding.service.common.mapping.TagsFilterSupport;
import com.aidigital.aionboarding.service.material.models.MaterialListQuery;
import com.aidigital.aionboarding.service.material.models.MaterialSortField;
import com.aidigital.aionboarding.service.material.support.MaterialSpecificationBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies {@link MaterialSpecificationBuilder}'s JSONB tags containment predicate against a real
 * PostgreSQL instance. H2 does not implement the {@code jsonb_contains}/{@code jsonb(text)}
 * functions this predicate relies on, so this check would silently pass with H2 even if the
 * containment literal were built incorrectly (as it previously was, via {@code to_jsonb(text)}).
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers(disabledWithoutDocker = true)
class MaterialTagFilterIntegrationTest {

	@Container
	@ServiceConnection
	static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

	@Autowired
	private MaterialRepository materialRepository;

	private final MaterialSpecificationBuilder specificationBuilder =
			new MaterialSpecificationBuilder(new TagsFilterSupport(new ObjectMapper()));

	@Test
	void buildShouldFilterMaterialsByTagsContainmentAgainstRealPostgresJsonbTest() {
		// Given:
		materialRepository.save(material("Design basics", List.of("design", "ux")));
		materialRepository.save(material("Backend deep dive", List.of("backend", "java")));
		materialRepository.save(material("Full stack overview", List.of("design", "backend")));

		MaterialListQuery filter = new MaterialListQuery(
				null, List.of("design"), null, null, null, null, MaterialSortField.TITLE, Sort.Direction.ASC
		);
		Specification<Material> specification = specificationBuilder.build(filter);

		// When:
		Page<Material> result = materialRepository.findAll(specification, PageRequest.of(0, 10));

		// Then:
		assertThat(result.getContent()).extracting(Material::getTitle)
				.containsExactly("Design basics", "Full stack overview");
	}

	@Test
	void buildShouldMatchFreeTextSearchAgainstTagsTest() {
		// Given:
		materialRepository.save(material("Design basics", List.of("design", "ux")));
		materialRepository.save(material("Backend deep dive", List.of("backend", "java")));

		MaterialListQuery filter = new MaterialListQuery(
				"design", null, null, null, null, null, MaterialSortField.TITLE, Sort.Direction.ASC
		);
		Specification<Material> specification = specificationBuilder.build(filter);

		// When:
		Page<Material> result = materialRepository.findAll(specification, PageRequest.of(0, 10));

		// Then:
		assertThat(result.getContent()).extracting(Material::getTitle).containsExactly("Design basics");
	}

	@Test
	void buildShouldNotMatchFreeTextSearchAgainstTextContentTest() {
		// Given: free-text search deliberately excludes text_content
		// (see MaterialSpecificationBuilder's comment) — a term appearing only in the body must
		// not surface a match, while the same term in the title still does.
		materialRepository.save(material("Introductory overview", List.of()));
		Material bodyOnlyMatch = material("Unrelated title", List.of());
		bodyOnlyMatch.setTextContent("this body mentions uniqueterm right here");
		materialRepository.save(bodyOnlyMatch);
		materialRepository.save(material("Contains uniqueterm in the title", List.of()));

		MaterialListQuery filter = new MaterialListQuery(
				"uniqueterm", null, null, null, null, null, MaterialSortField.TITLE, Sort.Direction.ASC
		);
		Specification<Material> specification = specificationBuilder.build(filter);

		// When:
		Page<Material> result = materialRepository.findAll(specification, PageRequest.of(0, 10));

		// Then:
		assertThat(result.getContent()).extracting(Material::getTitle)
				.containsExactly("Contains uniqueterm in the title");
	}

	@Test
	void buildShouldRequireEveryFilterTagToBePresentTest() {
		// Given:
		materialRepository.save(material("Design basics", List.of("design", "ux")));
		materialRepository.save(material("Full stack overview", List.of("design", "backend")));

		MaterialListQuery filter = new MaterialListQuery(
				null, List.of("design", "ux"), null, null, null, null, MaterialSortField.TITLE, Sort.Direction.ASC
		);
		Specification<Material> specification = specificationBuilder.build(filter);

		// When:
		Page<Material> result = materialRepository.findAll(specification, PageRequest.of(0, 10));

		// Then:
		assertThat(result.getContent()).extracting(Material::getTitle).containsExactly("Design basics");
	}

	private Material material(String title, List<String> tags) {
		Material material = new Material();
		material.setTitle(title);
		material.setDescription("description");
		material.setTextContent("content");
		material.setCoverImageStorageKey("key");
		material.setCoverImageOriginalName("name.png");
		material.setCoverImageMimeType("image/png");
		material.setTags(tags);
		material.setCreatedBy("tester");
		material.setCreatedAt(LocalDateTime.now());
		material.setUpdatedAt(LocalDateTime.now());
		return material;
	}
}
