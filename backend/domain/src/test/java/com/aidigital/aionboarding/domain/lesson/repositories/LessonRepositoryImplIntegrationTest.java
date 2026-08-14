package com.aidigital.aionboarding.domain.lesson.repositories;

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
 * Exercises {@link LessonRepositoryImpl#searchSummaries} and its shared {@code countMatching}
 * helper — the Criteria-API projection query reused for both the bounded Library summary list
 * and its matching count — against a real H2/PostgreSQL-mode schema, entirely within
 * {@code domain} (see domain/pom.xml for why not Testcontainers here).
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class LessonRepositoryImplIntegrationTest {

	@Autowired
	private LessonRepository lessonRepository;

	@Autowired
	private LessonStatusRepository lessonStatusRepository;

	@Autowired
	private LessonPublicationStatusRepository lessonPublicationStatusRepository;

	@Autowired
	private LessonContentFormatRepository lessonContentFormatRepository;

	@Test
	void shouldReturnBoundedProjectionPageMatchingSpecificationTest() {
		// Given: two ready lessons and one draft lesson
		LessonStatus readyStatus = lessonStatusRepository.save(status(LessonStatusCode.READY));
		LessonStatus draftStatus = lessonStatusRepository.save(status(LessonStatusCode.DRAFT));
		LessonPublicationStatus publishedStatus = lessonPublicationStatusRepository.save(publicationStatus());
		LessonContentFormat contentFormat = lessonContentFormatRepository.save(contentFormat());

		lessonRepository.save(lesson("Ready One", readyStatus, publishedStatus, contentFormat));
		lessonRepository.save(lesson("Ready Two", readyStatus, publishedStatus, contentFormat));
		lessonRepository.save(lesson("Still Draft", draftStatus, publishedStatus, contentFormat));

		Specification<Lesson> onlyReady =
				(root, query, cb) -> cb.equal(root.get("status").get("id"), readyStatus.getId());

		// When
		Page<LessonSearchSummaryProjection> page = lessonRepository.searchSummaries(onlyReady, PageRequest.of(0, 10));

		// Then: only the two ready lessons are returned, with projection fields populated
		assertThat(page.getTotalElements()).isEqualTo(2);
		assertThat(page.getContent()).extracting(LessonSearchSummaryProjection::title)
				.containsExactlyInAnyOrder("Ready One", "Ready Two");
		assertThat(page.getContent()).allSatisfy(p -> {
			assertThat(p.statusCode()).isEqualTo(LessonStatusCode.READY);
			assertThat(p.contentHtmlPreview()).isEqualTo("<p>content</p>");
		});
	}

	@Test
	void shouldRespectPageSizeWhileCountingAllMatchesTest() {
		// Given: three ready lessons, page size 1
		LessonStatus readyStatus = lessonStatusRepository.save(status(LessonStatusCode.READY));
		LessonPublicationStatus publishedStatus = lessonPublicationStatusRepository.save(publicationStatus());
		LessonContentFormat contentFormat = lessonContentFormatRepository.save(contentFormat());
		lessonRepository.save(lesson("A", readyStatus, publishedStatus, contentFormat));
		lessonRepository.save(lesson("B", readyStatus, publishedStatus, contentFormat));
		lessonRepository.save(lesson("C", readyStatus, publishedStatus, contentFormat));

		// When
		Specification<Lesson> matchAll = (root, query, cb) -> cb.conjunction();
		Page<LessonSearchSummaryProjection> page = lessonRepository.searchSummaries(matchAll, PageRequest.of(0, 1));

		// Then: one row on the page, but the total count reflects all matches
		assertThat(page.getContent()).hasSize(1);
		assertThat(page.getTotalElements()).isEqualTo(3);
	}

	private LessonStatus status(String code) {
		LessonStatus status = new LessonStatus();
		status.setCode(code);
		status.setName(code);
		status.setDisplayOrder(1);
		status.setIsActive(true);
		return status;
	}

	private LessonPublicationStatus publicationStatus() {
		LessonPublicationStatus status = new LessonPublicationStatus();
		status.setCode(LessonPublicationStatusCode.PUBLISHED);
		status.setName("Published");
		status.setDisplayOrder(1);
		status.setIsActive(true);
		return status;
	}

	private LessonContentFormat contentFormat() {
		LessonContentFormat format = new LessonContentFormat();
		format.setCode(LessonContentFormatCode.MARKDOWN);
		format.setName("Markdown");
		format.setDisplayOrder(1);
		format.setIsActive(true);
		return format;
	}

	private Lesson lesson(
			String title, LessonStatus status, LessonPublicationStatus publicationStatus, LessonContentFormat contentFormat
	) {
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
		lesson.setContentMarkdown("content");
		lesson.setContentHtml("<p>content</p>");
		lesson.setCoverImageStorageKey("");
		lesson.setCoverImageOriginalName("");
		lesson.setCoverImageMimeType("");
		lesson.setGenerationMetadata(java.util.Map.of());
		lesson.setRevisionHistory(List.of());
		lesson.setErrorMessage("");
		lesson.setPublicationStatus(publicationStatus);
		lesson.setCreatedBy("tester");
		lesson.setTags(List.of());
		lesson.setCreatedAt(now);
		lesson.setUpdatedAt(now);
		return lesson;
	}
}
