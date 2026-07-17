package com.aidigital.aionboarding.roadmap;

import com.aidigital.aionboarding.domain.roadmap.entities.Roadmap;
import com.aidigital.aionboarding.domain.roadmap.repositories.RoadmapRepository;
import com.aidigital.aionboarding.service.common.mapping.TagsFilterSupport;
import com.aidigital.aionboarding.service.roadmap.models.RoadmapListQuery;
import com.aidigital.aionboarding.service.roadmap.models.RoadmapSortField;
import com.aidigital.aionboarding.service.roadmap.support.RoadmapSpecificationBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies {@code roadmapRepository.count(specification)} — the bounded count-only path —
 * against real PostgreSQL, reusing the same
 * {@link RoadmapSpecificationBuilder} specification as the full search so the tab-count total and
 * the active-tab list can never silently drift apart.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers(disabledWithoutDocker = true)
class RoadmapSearchCountRepositoryIntegrationTest {

	@Container
	@ServiceConnection
	static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

	@Autowired
	private RoadmapRepository roadmapRepository;

	private final RoadmapSpecificationBuilder specificationBuilder =
			new RoadmapSpecificationBuilder(new TagsFilterSupport(new ObjectMapper()));

	@Test
	void countShouldMatchTheNumberOfRoadmapsMatchingTheTagFilterTest() {
		// Given: two roadmaps tagged "design", one untagged
		roadmapRepository.save(roadmap("Design Basics", List.of("design")));
		roadmapRepository.save(roadmap("Design Advanced", List.of("design")));
		roadmapRepository.save(roadmap("Unrelated", List.of()));

		RoadmapListQuery query = new RoadmapListQuery(
				null, List.of("design"), null, null, RoadmapSortField.CREATED_AT, Sort.Direction.DESC
		);
		Specification<Roadmap> specification = specificationBuilder.build(query, 1L);

		// When:
		long total = roadmapRepository.count(specification);

		// Then:
		assertThat(total).isEqualTo(2L);
	}

	private Roadmap roadmap(String title, List<String> tags) {
		Roadmap roadmap = new Roadmap();
		roadmap.setTitle(title);
		roadmap.setDescription("description");
		roadmap.setTags(tags);
		roadmap.setCreatedBy("tester");
		LocalDateTime now = LocalDateTime.of(2026, 1, 1, 0, 0);
		roadmap.setCreatedAt(now);
		roadmap.setUpdatedAt(now);
		return roadmap;
	}
}
