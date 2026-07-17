package com.aidigital.aionboarding.mappers.roadmap;

import com.aidigital.aionboarding.api.v1.model.RoadmapSortFieldV1;
import com.aidigital.aionboarding.service.roadmap.models.RoadmapSortField;
import org.instancio.Instancio;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RoadmapSortFieldApiMapperImplTest {

	private final RoadmapSortFieldApiMapperImpl roadmapSortFieldApiMapperImpl = new RoadmapSortFieldApiMapperImpl();

	@Test
	void shouldToRoadmapSortFieldRoadmapSortFieldV1Test() {
		// Given:
		RoadmapSortFieldV1 sort = Instancio.create(RoadmapSortFieldV1.class);

		// When:
		RoadmapSortField actualResult = roadmapSortFieldApiMapperImpl.toRoadmapSortField(sort);

		// Then:
		assertThat(actualResult).isNotNull();
	}

}