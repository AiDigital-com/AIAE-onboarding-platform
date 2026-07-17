package com.aidigital.aionboarding.lesson;

import com.aidigital.aionboarding.domain.common.dictionary.LessonContentFormatCode;
import com.aidigital.aionboarding.domain.common.dictionary.LessonPublicationStatusCode;
import com.aidigital.aionboarding.domain.common.dictionary.LessonStatusCode;
import com.aidigital.aionboarding.domain.common.dictionary.entities.LessonContentFormat;
import com.aidigital.aionboarding.domain.common.dictionary.entities.LessonPublicationStatus;
import com.aidigital.aionboarding.domain.common.dictionary.entities.LessonStatus;
import com.aidigital.aionboarding.domain.common.dictionary.repositories.LessonContentFormatRepository;
import com.aidigital.aionboarding.domain.common.dictionary.repositories.LessonPublicationStatusRepository;
import com.aidigital.aionboarding.domain.common.dictionary.repositories.LessonStatusRepository;
import com.aidigital.aionboarding.domain.lesson.entities.Lesson;
import com.aidigital.aionboarding.domain.lesson.repositories.LessonRepository;
import com.aidigital.aionboarding.domain.lesson.repositories.LessonSearchSummaryProjection;
import com.aidigital.aionboarding.service.common.mapping.TagsFilterSupport;
import com.aidigital.aionboarding.service.lesson.models.LessonListQuery;
import com.aidigital.aionboarding.service.lesson.models.LessonSortField;
import com.aidigital.aionboarding.service.lesson.models.LessonVisibilityFilter;
import com.aidigital.aionboarding.service.lesson.support.LessonSpecificationBuilder;
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
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies {@link LessonRepository#searchSummaries} — the bounded Library search projection —
 * against real PostgreSQL: content is truncated to a short
 * preview rather than the full body, the reused {@link LessonSpecificationBuilder} visibility
 * predicate still applies, and the widened order-by condition (applies to every result shape
 * except the derived count query) still sorts a non-{@code Lesson.class} projection correctly.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers(disabledWithoutDocker = true)
class LessonSearchSummaryRepositoryIntegrationTest {

	@Container
	@ServiceConnection
	static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

	@Autowired
	private LessonRepository lessonRepository;

	@Autowired
	private LessonStatusRepository lessonStatusRepository;

	@Autowired
	private LessonPublicationStatusRepository lessonPublicationStatusRepository;

	@Autowired
	private LessonContentFormatRepository lessonContentFormatRepository;

	private final LessonSpecificationBuilder specificationBuilder =
			new LessonSpecificationBuilder(new TagsFilterSupport(new ObjectMapper()));

	@Test
	void searchSummariesShouldTruncateContentAndMapSummaryFieldsForAPublishedLessonTest() {
		// Given: a published lesson with content well over the 500-char preview bound
		String longHtml = "<p>" + "x".repeat(600) + "</p>";
		String longMarkdown = "m".repeat(600);
		Lesson lesson = lessonRepository.save(
				lesson("Deep Dive", LessonPublicationStatusCode.PUBLISHED, longHtml, longMarkdown, List.of("design")));
		LessonListQuery query = anyoneVisibleQuery();
		LessonVisibilityFilter visibility = new LessonVisibilityFilter(true, false, 1L);

		// When:
		Page<LessonSearchSummaryProjection> result = searchSummaries(query, visibility, 0, 20);

		// Then:
		assertThat(result.getContent()).hasSize(1);
		LessonSearchSummaryProjection summary = result.getContent().get(0);
		assertThat(summary.id()).isEqualTo(lesson.getId());
		assertThat(summary.title()).isEqualTo("Deep Dive");
		assertThat(summary.statusCode()).isEqualTo(LessonStatusCode.READY);
		assertThat(summary.publicationStatusCode()).isEqualTo(LessonPublicationStatusCode.PUBLISHED);
		assertThat(summary.contentHtmlPreview()).hasSize(500);
		assertThat(summary.contentMarkdownPreview()).hasSize(500);
		assertThat(summary.tags()).containsExactly("design");
		assertThat(summary.createdBy()).isEqualTo("tester");
	}

	@Test
	void searchSummariesShouldExcludePrivateLessonsForANonManagingViewerTest() {
		// Given: one published and one private lesson
		lessonRepository.save(lesson("Published", LessonPublicationStatusCode.PUBLISHED, "<p>a</p>", "a", List.of()));
		lessonRepository.save(lesson("Private", LessonPublicationStatusCode.PRIVATE, "<p>b</p>", "b", List.of()));
		LessonListQuery query = anyoneVisibleQuery();
		LessonVisibilityFilter nonManagingViewer = new LessonVisibilityFilter(false, false, 9L);

		// When:
		Page<LessonSearchSummaryProjection> result = searchSummaries(query, nonManagingViewer, 0, 20);

		// Then:
		assertThat(result.getContent()).extracting(LessonSearchSummaryProjection::title)
				.containsExactly("Published");
	}

	@Test
	void searchSummariesShouldSortByTitleAscendingForTheProjectionQueryTest() {
		// Given: three published lessons in non-alphabetical creation order
		lessonRepository.save(lesson("Charlie", LessonPublicationStatusCode.PUBLISHED, "<p>c</p>", "c", List.of()));
		lessonRepository.save(lesson("Alpha", LessonPublicationStatusCode.PUBLISHED, "<p>a</p>", "a", List.of()));
		lessonRepository.save(lesson("Bravo", LessonPublicationStatusCode.PUBLISHED, "<p>b</p>", "b", List.of()));
		LessonListQuery query = new LessonListQuery(
				null, null, null, null, null, null, null, null, null, LessonSortField.TITLE, Sort.Direction.ASC
		);
		LessonVisibilityFilter visibility = new LessonVisibilityFilter(true, false, 1L);

		// When: the projection's result type is not Lesson.class, exercising the widened order-by condition
		Page<LessonSearchSummaryProjection> result = searchSummaries(query, visibility, 0, 20);

		// Then:
		assertThat(result.getContent()).extracting(LessonSearchSummaryProjection::title)
				.containsExactly("Alpha", "Bravo", "Charlie");
	}

	@Test
	void searchSummariesShouldRespectPageSizeAndReportTotalElementsTest() {
		// Given: three published lessons
		for (int i = 0; i < 3; i++) {
			lessonRepository.save(lesson("Lesson " + i, LessonPublicationStatusCode.PUBLISHED, "<p>c</p>", "c",
					List.of()));
		}
		LessonListQuery query = anyoneVisibleQuery();
		LessonVisibilityFilter visibility = new LessonVisibilityFilter(true, false, 1L);

		// When: requesting a page of size 2
		Page<LessonSearchSummaryProjection> firstPage = searchSummaries(query, visibility, 0, 2);

		// Then:
		assertThat(firstPage.getContent()).hasSize(2);
		assertThat(firstPage.getTotalElements()).isEqualTo(3);
		assertThat(firstPage.hasNext()).isTrue();
	}

	@Test
	void countShouldMatchTheNumberOfLessonsVisibleToTheViewerTest() {
		// Given: one published, one private lesson
		lessonRepository.save(lesson("Published", LessonPublicationStatusCode.PUBLISHED, "<p>a</p>", "a", List.of()));
		lessonRepository.save(lesson("Private", LessonPublicationStatusCode.PRIVATE, "<p>b</p>", "b", List.of()));
		LessonListQuery query = anyoneVisibleQuery();
		LessonVisibilityFilter nonManagingViewer = new LessonVisibilityFilter(false, false, 9L);

		// When: counting via the same reused specification as the summary search
		Long readyStatusId = lessonStatusRepository.findByCode(LessonStatusCode.READY).orElseThrow().getId();
		Long publishedStatusId =
				lessonPublicationStatusRepository.findByCode(LessonPublicationStatusCode.PUBLISHED).orElseThrow().getId();
		Specification<Lesson> specification =
				specificationBuilder.build(query, nonManagingViewer, null, null, readyStatusId, publishedStatusId);
		long total = lessonRepository.count(specification);

		// Then: only the published lesson is visible and counted
		assertThat(total).isEqualTo(1L);
	}

	private Page<LessonSearchSummaryProjection> searchSummaries(
			LessonListQuery query, LessonVisibilityFilter visibility, int page, int size
	) {
		Long readyStatusId = lessonStatusRepository.findByCode(LessonStatusCode.READY).orElseThrow().getId();
		Long publishedStatusId =
				lessonPublicationStatusRepository.findByCode(LessonPublicationStatusCode.PUBLISHED).orElseThrow().getId();
		Specification<Lesson> specification =
				specificationBuilder.build(query, visibility, null, null, readyStatusId, publishedStatusId);
		return lessonRepository.searchSummaries(specification, PageRequest.of(page, size));
	}

	private LessonListQuery anyoneVisibleQuery() {
		return new LessonListQuery(
				null, null, null, null, null, null, null, null, null, LessonSortField.CREATED_AT, Sort.Direction.DESC
		);
	}

	private Lesson lesson(
			String title, String publicationStatusCode, String contentHtml, String contentMarkdown, List<String> tags
	) {
		LessonStatus status = lessonStatusRepository.findByCode(LessonStatusCode.READY).orElseThrow();
		LessonPublicationStatus publicationStatus =
				lessonPublicationStatusRepository.findByCode(publicationStatusCode).orElseThrow();
		LessonContentFormat contentFormat =
				lessonContentFormatRepository.findByCode(LessonContentFormatCode.MARKDOWN).orElseThrow();
		LocalDateTime now = LocalDateTime.of(2026, 1, 1, 0, 0);

		Lesson lesson = new Lesson();
		lesson.setTitle(title);
		lesson.setDescription("description");
		lesson.setStatus(status);
		lesson.setUserInstructions("");
		lesson.setDepth("standard");
		lesson.setTone("clear");
		lesson.setDesiredFormat("structured theoretical lesson");
		lesson.setContentFormat(contentFormat);
		lesson.setContentMarkdown(contentMarkdown);
		lesson.setContentHtml(contentHtml);
		lesson.setCoverImageStorageKey("");
		lesson.setCoverImageOriginalName("");
		lesson.setCoverImageMimeType("");
		lesson.setGenerationMetadata(Map.of());
		lesson.setRevisionHistory(List.of());
		lesson.setErrorMessage("");
		lesson.setPublicationStatus(publicationStatus);
		lesson.setCreatedBy("tester");
		lesson.setTags(tags);
		lesson.setCreatedAt(now);
		lesson.setUpdatedAt(now);
		return lesson;
	}
}
