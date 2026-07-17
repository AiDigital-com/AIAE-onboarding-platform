package com.aidigital.aionboarding.service.roadmap.services.entity;

import com.aidigital.aionboarding.domain.roadmap.entities.Roadmap;
import com.aidigital.aionboarding.domain.roadmap.entities.RoadmapLesson;
import com.aidigital.aionboarding.domain.roadmap.repositories.RoadmapLessonRepository;
import com.aidigital.aionboarding.domain.roadmap.repositories.RoadmapRepository;
import com.aidigital.aionboarding.service.common.error.AppException;
import com.aidigital.aionboarding.service.common.error.ErrorReason;
import com.aidigital.aionboarding.service.roadmap.models.RoadmapListQuery;
import com.aidigital.aionboarding.service.roadmap.models.RoadmapSortField;
import com.aidigital.aionboarding.service.roadmap.support.RoadmapSpecificationBuilder;
import org.instancio.Instancio;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.instancio.Select.field;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoadmapEntityServiceTest {

	@Mock
	private RoadmapRepository roadmapRepository;
	@Mock
	private RoadmapLessonRepository roadmapLessonRepository;
	@Mock
	private RoadmapSpecificationBuilder roadmapSpecificationBuilder;

	@InjectMocks
	private RoadmapEntityService roadmapEntityService;

	@Test
	void getReferenceShouldReturnRoadmapWhenFoundTest() {
		// Given:
		Long roadmapId = 10L;
		Roadmap roadmap = Instancio.of(Roadmap.class)
				.set(field(Roadmap::getId), roadmapId)
				.create();
		when(roadmapRepository.findById(roadmapId)).thenReturn(Optional.of(roadmap));

		// When:
		Roadmap result = roadmapEntityService.getReference(roadmapId);

		// Then:
		assertThat(result).isSameAs(roadmap);
	}

	@Test
	void getReferenceShouldThrowAppExceptionC001WhenNotFoundTest() {
		// Given:
		Long roadmapId = 99L;
		when(roadmapRepository.findById(roadmapId)).thenReturn(Optional.empty());

		// When-Then:
		assertThatThrownBy(() -> roadmapEntityService.getReference(roadmapId))
				.isInstanceOf(AppException.class)
				.satisfies(ex -> assertThat(((AppException) ex).getCode()).isEqualTo(ErrorReason.C001.name()));
	}

	@Test
	void searchShouldBuildSpecificationAndDelegateToRepositoryTest() {
		// Given:
		Roadmap roadmap = Instancio.of(Roadmap.class)
				.set(field(Roadmap::getId), 1L)
				.create();
		RoadmapListQuery query = new RoadmapListQuery(
				null, null, null, null, RoadmapSortField.CREATED_AT, Sort.Direction.DESC
		);
		@SuppressWarnings("unchecked")
		Specification<Roadmap> specification = (Specification<Roadmap>) org.mockito.Mockito.mock(Specification.class);
		Page<Roadmap> page = new PageImpl<>(List.of(roadmap));
		when(roadmapSpecificationBuilder.build(query, 7L)).thenReturn(specification);
		when(roadmapRepository.findAll(eq(specification), any(Pageable.class))).thenReturn(page);

		// When:
		Page<Roadmap> result = roadmapEntityService.search(query, 7L, 0, 20);

		// Then:
		assertThat(result.getContent()).containsExactly(roadmap);
		verify(roadmapSpecificationBuilder).build(query, 7L);
	}

	@Test
	void countRoadmapsShouldBuildSpecificationAndDelegateToRepositoryCountTest() {
		// Given:
		RoadmapListQuery query = new RoadmapListQuery(
				null, null, null, null, RoadmapSortField.CREATED_AT, Sort.Direction.DESC
		);
		@SuppressWarnings("unchecked")
		Specification<Roadmap> specification = (Specification<Roadmap>) org.mockito.Mockito.mock(Specification.class);
		when(roadmapSpecificationBuilder.build(query, 7L)).thenReturn(specification);
		when(roadmapRepository.count(specification)).thenReturn(4L);

		// When:
		long result = roadmapEntityService.countRoadmaps(query, 7L);

		// Then:
		assertThat(result).isEqualTo(4L);
		verify(roadmapSpecificationBuilder).build(query, 7L);
	}

	@Test
	void saveShouldPersistRoadmapAndReturnSavedEntityTest() {
		// Given:
		Roadmap roadmap = Instancio.of(Roadmap.class)
				.set(field(Roadmap::getId), 5L)
				.create();
		when(roadmapRepository.save(roadmap)).thenReturn(roadmap);

		// When:
		Roadmap result = roadmapEntityService.save(roadmap);

		// Then:
		assertThat(result).isSameAs(roadmap);
		verify(roadmapRepository, times(1)).save(roadmap);
	}

	@Test
	void deleteShouldDelegateToRepositoryTest() {
		// Given:
		Roadmap roadmap = Instancio.of(Roadmap.class)
				.set(field(Roadmap::getId), 7L)
				.create();

		// When:
		roadmapEntityService.delete(roadmap);

		// Then:
		verify(roadmapRepository, times(1)).delete(roadmap);
	}

	@Test
	void findAllByRoadmapIdsWithLessonsShouldReturnRepositoryResultTest() {
		// Given:
		List<Long> roadmapIds = List.of(1L, 2L);
		RoadmapLesson roadmapLesson = Instancio.create(RoadmapLesson.class);
		when(roadmapLessonRepository.findAllByRoadmapIdsWithLessons(eq(roadmapIds)))
				.thenReturn(List.of(roadmapLesson));

		// When:
		List<RoadmapLesson> result = roadmapEntityService.findAllByRoadmapIdsWithLessons(roadmapIds);

		// Then:
		assertThat(result).containsExactly(roadmapLesson);
	}

	@Test
	void findByIdRoadmapIdOrderBySortOrderAscShouldReturnRepositoryResultTest() {
		// Given:
		Long roadmapId = 3L;
		RoadmapLesson roadmapLesson = Instancio.create(RoadmapLesson.class);
		when(roadmapLessonRepository.findByIdRoadmapIdOrderBySortOrderAsc(roadmapId))
				.thenReturn(List.of(roadmapLesson));

		// When:
		List<RoadmapLesson> result = roadmapEntityService.findByIdRoadmapIdOrderBySortOrderAsc(roadmapId);

		// Then:
		assertThat(result).containsExactly(roadmapLesson);
	}

	@Test
	void findByIdLessonIdShouldReturnRepositoryResultTest() {
		// Given:
		Long lessonId = 4L;
		RoadmapLesson roadmapLesson = Instancio.create(RoadmapLesson.class);
		when(roadmapLessonRepository.findByIdLessonId(lessonId)).thenReturn(List.of(roadmapLesson));

		// When:
		List<RoadmapLesson> result = roadmapEntityService.findByIdLessonId(lessonId);

		// Then:
		assertThat(result).containsExactly(roadmapLesson);
	}

	@Test
	void saveRoadmapLessonShouldPersistAndReturnSavedEntityTest() {
		// Given:
		RoadmapLesson roadmapLesson = Instancio.create(RoadmapLesson.class);
		when(roadmapLessonRepository.save(roadmapLesson)).thenReturn(roadmapLesson);

		// When:
		RoadmapLesson result = roadmapEntityService.saveRoadmapLesson(roadmapLesson);

		// Then:
		assertThat(result).isSameAs(roadmapLesson);
		verify(roadmapLessonRepository, times(1)).save(roadmapLesson);
	}

	@Test
	void deleteByIdRoadmapIdShouldDelegateToRepositoryTest() {
		// Given:
		Long roadmapId = 8L;

		// When:
		roadmapEntityService.deleteByIdRoadmapId(roadmapId);

		// Then:
		verify(roadmapLessonRepository, times(1)).deleteByIdRoadmapId(roadmapId);
	}
}
