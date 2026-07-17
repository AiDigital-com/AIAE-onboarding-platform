package com.aidigital.aionboarding.service.lesson.support;

import com.aidigital.aionboarding.domain.common.dictionary.DictionaryEntity;
import com.aidigital.aionboarding.domain.common.dictionary.LessonAssetKindCode;
import com.aidigital.aionboarding.domain.common.dictionary.LessonContentFormatCode;
import com.aidigital.aionboarding.domain.common.dictionary.LessonPublicationStatusCode;
import com.aidigital.aionboarding.domain.common.dictionary.LessonStatusCode;
import com.aidigital.aionboarding.domain.common.dictionary.entities.LessonAssetKind;
import com.aidigital.aionboarding.domain.common.dictionary.entities.LessonContentFormat;
import com.aidigital.aionboarding.domain.common.dictionary.entities.LessonPublicationStatus;
import com.aidigital.aionboarding.domain.common.dictionary.entities.LessonStatus;
import com.aidigital.aionboarding.domain.common.entities.IdAwareEntity;
import com.aidigital.aionboarding.domain.lesson.entities.Lesson;
import com.aidigital.aionboarding.domain.lesson.entities.LessonAsset;
import com.aidigital.aionboarding.domain.lesson.entities.LessonMaterial;
import com.aidigital.aionboarding.domain.lesson.repositories.LessonSearchSummaryProjection;
import com.aidigital.aionboarding.service.lesson.models.LessonAssetRecord;
import com.aidigital.aionboarding.service.lesson.models.LessonDetailRecord;
import com.aidigital.aionboarding.service.lesson.models.LessonSearchSummaryRecord;
import com.aidigital.aionboarding.service.lesson.models.LessonSummaryRecord;
import com.aidigital.aionboarding.service.lesson.models.RevisionBriefRecord;
import com.aidigital.aionboarding.service.lesson.models.RevisionHistoryItemRecord;
import com.aidigital.aionboarding.service.lesson.models.TeacherVideoRecord;
import com.aidigital.aionboarding.service.lesson.services.entity.LessonAssetEntityService;
import com.aidigital.aionboarding.service.lesson.services.entity.LessonEntityService;
import com.aidigital.aionboarding.service.material.models.MaterialRecord;
import com.aidigital.aionboarding.service.material.services.MaterialRecordQueryService;
import org.instancio.Instancio;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.instancio.Select.field;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LessonRecordAssemblerTest {

	@Mock
	private LessonEntityService lessonEntityService;
	@Mock
	private LessonAssetEntityService lessonAssetEntityService;
	@Mock
	private MaterialRecordQueryService materialRecordQueryService;

	@InjectMocks
	private LessonRecordAssembler assembler;

	@Test
	void shouldMapAllFieldsToSummaryRecordTest() {
		// Given:
		Lesson lesson = Instancio.of(Lesson.class)
				.set(field(IdAwareEntity.class, "id"), 1L)
				.set(field("title"), "Lesson")
				.set(field("description"), "Description")
				.set(field("status"), Instancio.of(LessonStatus.class)
						.set(field(DictionaryEntity.class, "code"), LessonStatusCode.READY)
						.create())
				.set(field("publicationStatus"), Instancio.of(LessonPublicationStatus.class)
						.set(field(DictionaryEntity.class, "code"), LessonPublicationStatusCode.PUBLISHED)
						.create())
				.set(field("contentFormat"), Instancio.of(LessonContentFormat.class)
						.set(field(DictionaryEntity.class, "code"), LessonContentFormatCode.MARKDOWN)
						.create())
				.set(field("userInstructions"), "")
				.set(field("depth"), "standard")
				.set(field("tone"), "neutral")
				.set(field("desiredFormat"), "article")
				.set(field("contentMarkdown"), "content")
				.set(field("contentHtml"), "<p>content</p>")
				.set(field("coverImageStorageKey"), "")
				.set(field("coverImageOriginalName"), "")
				.set(field("coverImageMimeType"), "")
				.set(field("generationMetadata"), Map.of())
				.set(field("revisionHistory"), List.of())
				.set(field("errorMessage"), "")
				.set(field("createdBy"), "Teacher")
				.set(field("tags"), List.of())
				.set(field("createdAt"), LocalDateTime.parse("2026-07-05T10:00:00"))
				.set(field("updatedAt"), LocalDateTime.parse("2026-07-05T10:01:00"))
				.set(field("publishedAt"), LocalDateTime.parse("2026-07-05T10:02:00"))
				.ignore(field("createdByUser"))
				.create();

		// When:
		LessonSummaryRecord result = assembler.toSummaryRecord(lesson);

		// Then:
		assertThat(result.id()).isEqualTo(1L);
		assertThat(result.title()).isEqualTo("Lesson");
		assertThat(result.status()).isEqualTo(LessonStatusCode.READY);
		assertThat(result.publicationStatus()).isEqualTo(LessonPublicationStatusCode.PUBLISHED);
	}

	@Test
	void shouldHandleNullTagsInSummaryRecordTest() {
		// Given:
		Lesson lesson = Instancio.of(Lesson.class)
				.set(field(IdAwareEntity.class, "id"), 1L)
				.set(field("title"), "Lesson")
				.set(field("description"), "Description")
				.set(field("status"), Instancio.of(LessonStatus.class)
						.set(field(DictionaryEntity.class, "code"), LessonStatusCode.READY)
						.create())
				.set(field("publicationStatus"), Instancio.of(LessonPublicationStatus.class)
						.set(field(DictionaryEntity.class, "code"), LessonPublicationStatusCode.PUBLISHED)
						.create())
				.set(field("contentFormat"), Instancio.of(LessonContentFormat.class)
						.set(field(DictionaryEntity.class, "code"), LessonContentFormatCode.MARKDOWN)
						.create())
				.set(field("userInstructions"), "")
				.set(field("depth"), "standard")
				.set(field("tone"), "neutral")
				.set(field("desiredFormat"), "article")
				.set(field("contentMarkdown"), "content")
				.set(field("contentHtml"), "<p>content</p>")
				.set(field("coverImageStorageKey"), "")
				.set(field("coverImageOriginalName"), "")
				.set(field("coverImageMimeType"), "")
				.set(field("generationMetadata"), Map.of())
				.set(field("revisionHistory"), List.of())
				.set(field("errorMessage"), "")
				.set(field("createdBy"), "Teacher")
				.set(field("tags"), null)
				.set(field("createdAt"), LocalDateTime.parse("2026-07-05T10:00:00"))
				.set(field("updatedAt"), LocalDateTime.parse("2026-07-05T10:01:00"))
				.set(field("publishedAt"), LocalDateTime.parse("2026-07-05T10:02:00"))
				.ignore(field("createdByUser"))
				.create();

		// When:
		LessonSummaryRecord result = assembler.toSummaryRecord(lesson);

		// Then:
		assertThat(result.tags()).isEmpty();
	}

	@Test
	void shouldMapAllFieldsFromProjectionToListItemRecordTest() {
		// Given:
		LessonSearchSummaryProjection projection = Instancio.of(LessonSearchSummaryProjection.class)
				.set(field("id"), 2L)
				.set(field("title"), "Search Title")
				.set(field("statusCode"), LessonStatusCode.READY)
				.set(field("publicationStatusCode"), LessonPublicationStatusCode.PUBLISHED)
				.set(field("contentHtmlPreview"), "<p>preview</p>")
				.set(field("contentMarkdownPreview"), "preview")
				.set(field("coverImageStorageKey"), "key")
				.set(field("coverImageOriginalName"), "orig.png")
				.set(field("coverImageMimeType"), "image/png")
				.set(field("tags"), List.of("tag1", "tag2"))
				.set(field("createdBy"), "Author")
				.set(field("createdAt"), LocalDateTime.parse("2026-07-05T10:00:00"))
				.set(field("updatedAt"), LocalDateTime.parse("2026-07-05T10:01:00"))
				.create();

		// When:
		LessonSearchSummaryRecord result = assembler.toListItemRecord(projection);

		// Then:
		assertThat(result.id()).isEqualTo(2L);
		assertThat(result.tags()).containsExactly("tag1", "tag2");
	}

	@Test
	void shouldHandleNullTagsInListItemRecordTest() {
		// Given:
		LessonSearchSummaryProjection projection = Instancio.of(LessonSearchSummaryProjection.class)
				.set(field("id"), 2L)
				.set(field("title"), "T")
				.set(field("statusCode"), LessonStatusCode.READY)
				.set(field("publicationStatusCode"), "draft")
				.set(field("contentHtmlPreview"), "p")
				.set(field("contentMarkdownPreview"), "p")
				.set(field("coverImageStorageKey"), "")
				.set(field("coverImageOriginalName"), "")
				.set(field("coverImageMimeType"), "")
				.set(field("tags"), null)
				.set(field("createdBy"), "A")
				.set(field("createdAt"), LocalDateTime.parse("2026-07-05T10:00:00"))
				.set(field("updatedAt"), LocalDateTime.parse("2026-07-05T10:01:00"))
				.create();

		// When:
		LessonSearchSummaryRecord result = assembler.toListItemRecord(projection);

		// Then:
		assertThat(result.tags()).isEmpty();
	}

	@Test
	void shouldBatchLoadSourceReferencesInLessonMaterialOrderForDetailRecordTest() {
		// Given:
		Lesson lesson = Instancio.of(Lesson.class)
				.set(field(IdAwareEntity.class, "id"), 1L)
				.set(field("title"), "Lesson")
				.set(field("description"), "Description")
				.set(field("status"), Instancio.of(LessonStatus.class)
						.set(field(DictionaryEntity.class, "code"), LessonStatusCode.READY)
						.create())
				.set(field("publicationStatus"), Instancio.of(LessonPublicationStatus.class)
						.set(field(DictionaryEntity.class, "code"), LessonPublicationStatusCode.PUBLISHED)
						.create())
				.set(field("contentFormat"), Instancio.of(LessonContentFormat.class)
						.set(field(DictionaryEntity.class, "code"), LessonContentFormatCode.MARKDOWN)
						.create())
				.set(field("userInstructions"), "")
				.set(field("depth"), "standard")
				.set(field("tone"), "neutral")
				.set(field("desiredFormat"), "article")
				.set(field("contentMarkdown"), "content")
				.set(field("contentHtml"), "<p>content</p>")
				.set(field("coverImageStorageKey"), "")
				.set(field("coverImageOriginalName"), "")
				.set(field("coverImageMimeType"), "")
				.set(field("generationMetadata"), Map.of())
				.set(field("revisionHistory"), List.of())
				.set(field("errorMessage"), "")
				.set(field("createdBy"), "Teacher")
				.set(field("tags"), List.of())
				.set(field("createdAt"), LocalDateTime.parse("2026-07-05T10:00:00"))
				.set(field("updatedAt"), LocalDateTime.parse("2026-07-05T10:01:00"))
				.set(field("publishedAt"), LocalDateTime.parse("2026-07-05T10:02:00"))
				.ignore(field("createdByUser"))
				.create();
		MaterialRecord first = Instancio.of(MaterialRecord.class)
				.set(field("id"), 10L)
				.set(field("title"), "First")
				.create();
		MaterialRecord second = Instancio.of(MaterialRecord.class)
				.set(field("id"), 20L)
				.set(field("title"), "Second")
				.create();
		LessonMaterial.LessonMaterialId firstId = Instancio.of(LessonMaterial.LessonMaterialId.class)
				.set(field("lessonId"), 1L)
				.set(field("materialId"), 10L)
				.create();
		LessonMaterial.LessonMaterialId secondId = Instancio.of(LessonMaterial.LessonMaterialId.class)
				.set(field("lessonId"), 1L)
				.set(field("materialId"), 20L)
				.create();
		LessonMaterial firstMaterial = Instancio.of(LessonMaterial.class)
				.set(field("id"), firstId)
				.set(field("sortOrder"), 0)
				.set(field("createdAt"), LocalDateTime.parse("2026-07-05T10:00:00"))
				.ignore(field("lesson"))
				.ignore(field("material"))
				.create();
		LessonMaterial secondMaterial = Instancio.of(LessonMaterial.class)
				.set(field("id"), secondId)
				.set(field("sortOrder"), 1)
				.set(field("createdAt"), LocalDateTime.parse("2026-07-05T10:00:00"))
				.ignore(field("lesson"))
				.ignore(field("material"))
				.create();
		when(lessonEntityService.findLessonMaterialsByLessonId(1L))
				.thenReturn(List.of(secondMaterial, firstMaterial));
		when(materialRecordQueryService.loadMaterialRecordsByIds(List.of(20L, 10L)))
				.thenReturn(List.of(second, first));
		when(lessonAssetEntityService.findByLessonIdOrderByCreatedAtAsc(1L)).thenReturn(List.of());

		// When:
		LessonDetailRecord detail = assembler.toDetailRecord(lesson);

		// Then:
		assertThat(detail.materialIds()).containsExactly(20L, 10L);
		assertThat(detail.sourceReferences()).containsExactly(second, first);
		verify(materialRecordQueryService, never()).countLessonUsage(10L);
	}

	@Test
	void shouldMapAllFieldsToDetailRecordWithExplicitArgsTest() {
		// Given:
		Lesson lesson = Instancio.of(Lesson.class)
				.set(field(IdAwareEntity.class, "id"), 1L)
				.set(field("title"), "Lesson")
				.set(field("description"), "Description")
				.set(field("status"), Instancio.of(LessonStatus.class)
						.set(field(DictionaryEntity.class, "code"), LessonStatusCode.READY)
						.create())
				.set(field("publicationStatus"), Instancio.of(LessonPublicationStatus.class)
						.set(field(DictionaryEntity.class, "code"), LessonPublicationStatusCode.PUBLISHED)
						.create())
				.set(field("contentFormat"), Instancio.of(LessonContentFormat.class)
						.set(field(DictionaryEntity.class, "code"), LessonContentFormatCode.MARKDOWN)
						.create())
				.set(field("userInstructions"), "")
				.set(field("depth"), "standard")
				.set(field("tone"), "neutral")
				.set(field("desiredFormat"), "article")
				.set(field("contentMarkdown"), "content")
				.set(field("contentHtml"), "<p>content</p>")
				.set(field("coverImageStorageKey"), "")
				.set(field("coverImageOriginalName"), "")
				.set(field("coverImageMimeType"), "")
				.set(field("generationMetadata"), Map.of())
				.set(field("revisionHistory"), List.of())
				.set(field("errorMessage"), "")
				.set(field("createdBy"), "Teacher")
				.set(field("tags"), List.of())
				.set(field("createdAt"), LocalDateTime.parse("2026-07-05T10:00:00"))
				.set(field("updatedAt"), LocalDateTime.parse("2026-07-05T10:01:00"))
				.set(field("publishedAt"), LocalDateTime.parse("2026-07-05T10:02:00"))
				.ignore(field("createdByUser"))
				.create();
		List<Long> materialIds = List.of(10L);
		List<MaterialRecord> sourceRefs = List.of(Instancio.of(MaterialRecord.class)
				.set(field("id"), 10L)
				.set(field("title"), "Mat")
				.create());
		List<LessonAssetRecord> assets = List.of();

		// When:
		LessonDetailRecord result = assembler.toDetailRecord(lesson, materialIds, sourceRefs, assets);

		// Then:
		assertThat(result.id()).isEqualTo(1L);
		assertThat(result.isPublished()).isTrue();
	}

	@Test
	void shouldMapAllFieldsToAssetRecordTest() {
		// Given:
		Lesson lesson = Instancio.of(Lesson.class)
				.set(field(IdAwareEntity.class, "id"), 1L)
				.set(field("title"), "Lesson")
				.set(field("description"), "Description")
				.set(field("status"), Instancio.of(LessonStatus.class)
						.set(field(DictionaryEntity.class, "code"), LessonStatusCode.READY)
						.create())
				.set(field("publicationStatus"), Instancio.of(LessonPublicationStatus.class)
						.set(field(DictionaryEntity.class, "code"), LessonPublicationStatusCode.PUBLISHED)
						.create())
				.set(field("contentFormat"), Instancio.of(LessonContentFormat.class)
						.set(field(DictionaryEntity.class, "code"), LessonContentFormatCode.MARKDOWN)
						.create())
				.set(field("userInstructions"), "")
				.set(field("depth"), "standard")
				.set(field("tone"), "neutral")
				.set(field("desiredFormat"), "article")
				.set(field("contentMarkdown"), "content")
				.set(field("contentHtml"), "<p>content</p>")
				.set(field("coverImageStorageKey"), "")
				.set(field("coverImageOriginalName"), "")
				.set(field("coverImageMimeType"), "")
				.set(field("generationMetadata"), Map.of())
				.set(field("revisionHistory"), List.of())
				.set(field("errorMessage"), "")
				.set(field("createdBy"), "Teacher")
				.set(field("tags"), List.of())
				.set(field("createdAt"), LocalDateTime.parse("2026-07-05T10:00:00"))
				.set(field("updatedAt"), LocalDateTime.parse("2026-07-05T10:01:00"))
				.set(field("publishedAt"), LocalDateTime.parse("2026-07-05T10:02:00"))
				.ignore(field("createdByUser"))
				.create();
		LessonAsset asset = Instancio.of(LessonAsset.class)
				.set(field(IdAwareEntity.class, "id"), 100L)
				.set(field("lesson"), lesson)
				.set(field("kind"), Instancio.of(LessonAssetKind.class)
						.set(field(DictionaryEntity.class, "code"), LessonAssetKindCode.LINK)
						.create())
				.set(field("title"), "Asset Title")
				.set(field("originalName"), "")
				.set(field("url"), "https://example.com")
				.set(field("description"), "desc")
				.set(field("imageUrl"), "https://img.com")
				.set(field("siteName"), "Example")
				.set(field("storageKey"), "sk")
				.set(field("mimeType"), "text/html")
				.set(field("sizeBytes"), 1024L)
				.set(field("metadata"), Map.of("k", "v"))
				.set(field("createdAt"), LocalDateTime.parse("2026-07-05T10:00:00"))
				.create();

		// When:
		LessonAssetRecord result = assembler.toAssetRecord(asset);

		// Then:
		assertThat(result.id()).isEqualTo(100L);
		assertThat(result.kind()).isEqualTo(LessonAssetKindCode.LINK);
		assertThat(result.siteName()).isEqualTo("Example");
		assertThat(result.size()).isEqualTo(1024L);
	}

	@Test
	void shouldUseOriginalNameWhenTitleIsEmptyInAssetRecordTest() {
		// Given:
		Lesson lesson = Instancio.of(Lesson.class)
				.set(field(IdAwareEntity.class, "id"), 1L)
				.set(field("title"), "Lesson")
				.set(field("description"), "Description")
				.set(field("status"), Instancio.of(LessonStatus.class)
						.set(field(DictionaryEntity.class, "code"), LessonStatusCode.READY)
						.create())
				.set(field("publicationStatus"), Instancio.of(LessonPublicationStatus.class)
						.set(field(DictionaryEntity.class, "code"), LessonPublicationStatusCode.PUBLISHED)
						.create())
				.set(field("contentFormat"), Instancio.of(LessonContentFormat.class)
						.set(field(DictionaryEntity.class, "code"), LessonContentFormatCode.MARKDOWN)
						.create())
				.set(field("userInstructions"), "")
				.set(field("depth"), "standard")
				.set(field("tone"), "neutral")
				.set(field("desiredFormat"), "article")
				.set(field("contentMarkdown"), "content")
				.set(field("contentHtml"), "<p>content</p>")
				.set(field("coverImageStorageKey"), "")
				.set(field("coverImageOriginalName"), "")
				.set(field("coverImageMimeType"), "")
				.set(field("generationMetadata"), Map.of())
				.set(field("revisionHistory"), List.of())
				.set(field("errorMessage"), "")
				.set(field("createdBy"), "Teacher")
				.set(field("tags"), List.of())
				.set(field("createdAt"), LocalDateTime.parse("2026-07-05T10:00:00"))
				.set(field("updatedAt"), LocalDateTime.parse("2026-07-05T10:01:00"))
				.set(field("publishedAt"), LocalDateTime.parse("2026-07-05T10:02:00"))
				.ignore(field("createdByUser"))
				.create();
		LessonAsset asset = Instancio.of(LessonAsset.class)
				.set(field(IdAwareEntity.class, "id"), 100L)
				.set(field("lesson"), lesson)
				.set(field("kind"), Instancio.of(LessonAssetKind.class)
						.set(field(DictionaryEntity.class, "code"), LessonAssetKindCode.LINK)
						.create())
				.set(field("title"), "")
				.set(field("originalName"), "original.pdf")
				.set(field("url"), "https://example.com")
				.set(field("description"), "")
				.set(field("imageUrl"), "")
				.set(field("siteName"), "")
				.set(field("storageKey"), "")
				.set(field("mimeType"), "")
				.set(field("sizeBytes"), 0L)
				.set(field("metadata"), Map.of())
				.set(field("createdAt"), LocalDateTime.parse("2026-07-05T10:00:00"))
				.create();

		// When:
		LessonAssetRecord result = assembler.toAssetRecord(asset);

		// Then:
		assertThat(result.name()).isEqualTo("original.pdf");
	}

	@Nested
	class ToDetailMap {

		@Test
		void shouldIncludeCoreLessonFieldsInDetailMapTest() {
			// Given:
			Lesson lesson = Instancio.of(Lesson.class)
					.set(field(IdAwareEntity.class, "id"), 1L)
					.set(field("title"), "Lesson")
					.set(field("description"), "Description")
					.set(field("status"), Instancio.of(LessonStatus.class)
							.set(field(DictionaryEntity.class, "code"), LessonStatusCode.READY)
							.create())
					.set(field("publicationStatus"), Instancio.of(LessonPublicationStatus.class)
							.set(field(DictionaryEntity.class, "code"), LessonPublicationStatusCode.PUBLISHED)
							.create())
					.set(field("contentFormat"), Instancio.of(LessonContentFormat.class)
							.set(field(DictionaryEntity.class, "code"), LessonContentFormatCode.MARKDOWN)
							.create())
					.set(field("userInstructions"), "")
					.set(field("depth"), "standard")
					.set(field("tone"), "neutral")
					.set(field("desiredFormat"), "article")
					.set(field("contentMarkdown"), "content")
					.set(field("contentHtml"), "<p>content</p>")
					.set(field("coverImageStorageKey"), "")
					.set(field("coverImageOriginalName"), "")
					.set(field("coverImageMimeType"), "")
					.set(field("generationMetadata"), Map.of())
					.set(field("revisionHistory"), List.of())
					.set(field("errorMessage"), "")
					.set(field("createdBy"), "Teacher")
					.set(field("tags"), List.of())
					.set(field("createdAt"), LocalDateTime.parse("2026-07-05T10:00:00"))
					.set(field("updatedAt"), LocalDateTime.parse("2026-07-05T10:01:00"))
					.set(field("publishedAt"), LocalDateTime.parse("2026-07-05T10:02:00"))
					.ignore(field("createdByUser"))
					.create();
			when(lessonEntityService.findLessonMaterialsByLessonId(1L)).thenReturn(List.of());
			when(lessonAssetEntityService.findByLessonIdOrderByCreatedAtAsc(1L)).thenReturn(List.of());

			// When:
			Map<String, Object> map = assembler.toDetailMap(lesson);

			// Then:
			assertThat(map).containsKeys("id", "title", "description", "status", "publicationStatus",
					"isPublished", "isArchived");
			assertThat(map.get("id")).isEqualTo(1L);
		}

		@Test
		void shouldHandleNullTagsAndMetadataInDetailMapTest() {
			// Given:
			Lesson lesson = Instancio.of(Lesson.class)
					.set(field(IdAwareEntity.class, "id"), 1L)
					.set(field("title"), "Lesson")
					.set(field("description"), "Description")
					.set(field("status"), Instancio.of(LessonStatus.class)
							.set(field(DictionaryEntity.class, "code"), LessonStatusCode.READY)
							.create())
					.set(field("publicationStatus"), Instancio.of(LessonPublicationStatus.class)
							.set(field(DictionaryEntity.class, "code"), LessonPublicationStatusCode.PUBLISHED)
							.create())
					.set(field("contentFormat"), Instancio.of(LessonContentFormat.class)
							.set(field(DictionaryEntity.class, "code"), LessonContentFormatCode.MARKDOWN)
							.create())
					.set(field("userInstructions"), "")
					.set(field("depth"), "standard")
					.set(field("tone"), "neutral")
					.set(field("desiredFormat"), "article")
					.set(field("contentMarkdown"), "content")
					.set(field("contentHtml"), "<p>content</p>")
					.set(field("coverImageStorageKey"), "")
					.set(field("coverImageOriginalName"), "")
					.set(field("coverImageMimeType"), "")
					.set(field("generationMetadata"), null)
					.set(field("revisionHistory"), null)
					.set(field("errorMessage"), "")
					.set(field("createdBy"), "Teacher")
					.set(field("tags"), null)
					.set(field("createdAt"), LocalDateTime.parse("2026-07-05T10:00:00"))
					.set(field("updatedAt"), LocalDateTime.parse("2026-07-05T10:01:00"))
					.set(field("publishedAt"), LocalDateTime.parse("2026-07-05T10:02:00"))
					.ignore(field("createdByUser"))
					.create();
			when(lessonEntityService.findLessonMaterialsByLessonId(1L)).thenReturn(List.of());
			when(lessonAssetEntityService.findByLessonIdOrderByCreatedAtAsc(1L)).thenReturn(List.of());

			// When:
			Map<String, Object> map = assembler.toDetailMap(lesson);

			// Then:
			assertThat((List<?>) map.get("tags")).isEmpty();
		}
	}

	@Nested
	class RevisionHistory {

		@Test
		void shouldReturnEmptyRevisionHistoryRecordsForNullInputTest() {
			// Given:
			List<Object> history = null;

			// When:
			List<RevisionHistoryItemRecord> result = assembler.toRevisionHistoryRecords(history);

			// Then:
			assertThat(result).isEmpty();
		}

		@Test
		void shouldParseMapItemsToRevisionHistoryRecordsTest() {
			// Given:
			Map<String, Object> item = new LinkedHashMap<>();
			item.put("revisedAt", "2026-07-01");
			item.put("revisionRequest", "Update content");
			item.put("selectedOptions", List.of("opt1"));
			Map<String, Object> brief = new LinkedHashMap<>();
			brief.put("changeScope", "minor");
			brief.put("userIntent", "clarify");
			brief.put("editInstructions", List.of("fix grammar"));
			brief.put("preserveRules", List.of("keep tone"));
			brief.put("riskNotes", List.of("none"));
			item.put("revisionBrief", brief);

			// When:
			List<RevisionHistoryItemRecord> result = assembler.toRevisionHistoryRecords(List.of(item));

			// Then:
			assertThat(result).hasSize(1);
			assertThat(result.get(0).revisionBrief().changeScope()).isEqualTo("minor");
		}

		@Test
		void shouldReturnDefaultRevisionBriefRecordForNullInputTest() {
			// Given:
			Map<String, Object> brief = null;

			// When:
			RevisionBriefRecord result = assembler.toRevisionBriefRecord(brief);

			// Then:
			assertThat(result.changeScope()).isEqualTo("substantial");
		}

		@Test
		void shouldRoundTripRevisionBriefMapTest() {
			// Given:
			RevisionBriefRecord brief = Instancio.of(RevisionBriefRecord.class)
					.set(field("changeScope"), "minor")
					.set(field("userIntent"), "clarify")
					.set(field("editInstructions"), List.of("fix"))
					.set(field("preserveRules"), List.of("keep"))
					.set(field("riskNotes"), List.of("none"))
					.create();

			// When:
			Map<String, Object> map = assembler.toRevisionBriefMap(brief);

			// Then:
			assertThat(map).containsEntry("changeScope", "minor");
		}
	}

	@Nested
	class TeacherVideo {

		@Test
		void shouldReturnNullTeacherVideoRecordForNullInputTest() {
			// Given:
			Map<String, Object> map = null;

			// When:
			TeacherVideoRecord result = assembler.toTeacherVideoRecord(map);

			// Then:
			assertThat(result).isNull();
		}

		@Test
		void shouldReturnNullTeacherVideoRecordForEmptyMapTest() {
			// Given:
			Map<String, Object> map = Map.of();

			// When:
			TeacherVideoRecord result = assembler.toTeacherVideoRecord(map);

			// Then:
			assertThat(result).isNull();
		}

		@Test
		void shouldMapFieldsToTeacherVideoRecordTest() {
			// Given:
			Map<String, Object> map = new LinkedHashMap<>();
			map.put("provider", "heygen");
			map.put("prompt", "Generate video");
			map.put("videoId", "vid-1");
			map.put("status", "completed");
			map.put("durationLimitSeconds", 60);

			// When:
			TeacherVideoRecord result = assembler.toTeacherVideoRecord(map);

			// Then:
			assertThat(result.provider()).isEqualTo("heygen");
			assertThat(result.videoId()).isEqualTo("vid-1");
		}

		@Test
		void shouldHandleNullTeacherVideoMapTest() {
			// Given:
			TeacherVideoRecord record = null;

			// When:
			Map<String, Object> result = assembler.toTeacherVideoMap(record);

			// Then:
			assertThat(result).isEmpty();
		}

		@Test
		void shouldReturnNullNormalizedTeacherVideoRecordForNullInputTest() {
			// Given:
			TeacherVideoRecord record = null;

			// When:
			TeacherVideoRecord result = assembler.normalizeTeacherVideoRecord(record, "2026-07-01");

			// Then:
			assertThat(result).isNull();
		}

		@Test
		void shouldSetProviderAndCheckedAtInNormalizedTeacherVideoRecordTest() {
			// Given:
			TeacherVideoRecord record = Instancio.of(TeacherVideoRecord.class)
					.set(field("provider"), null)
					.set(field("prompt"), "prompt")
					.set(field("avatarId"), null)
					.set(field("voiceId"), null)
					.set(field("sessionId"), null)
					.set(field("videoId"), null)
					.set(field("status"), "generating")
					.set(field("createdAt"), "2026-07-01")
					.set(field("durationLimitSeconds"), 30)
					.set(field("checkedAt"), null)
					.set(field("videoUrl"), null)
					.set(field("thumbnailUrl"), null)
					.set(field("duration"), null)
					.set(field("completedAt"), null)
					.set(field("failedAt"), null)
					.create();

			// When:
			TeacherVideoRecord result = assembler.normalizeTeacherVideoRecord(record, "2026-07-02");

			// Then:
			assertThat(result.provider()).isEqualTo("heygen");
			assertThat(result.checkedAt()).isEqualTo("2026-07-02");
		}
	}

	@Nested
	class UtilityMethods {

		@Test
		void shouldReturnEmptyStringValForNullInputTest() {
			// Given:
			Object value = null;

			// When:
			String result = assembler.stringVal(value);

			// Then:
			assertThat(result).isEmpty();
		}

		@Test
		void shouldReturnFirstNonBlankValueTest() {
			// When:
			String result = assembler.firstNonBlank("", null, "  ", "hello");

			// Then:
			assertThat(result).isEqualTo("hello");
		}

		@Test
		void shouldReturnEmptyWhenAllBlankValuesTest() {
			// When:
			String result = assembler.firstNonBlank("", null, "  ");

			// Then:
			assertThat(result).isEmpty();
		}

		@Test
		void shouldReturnEmptyMapValForNullInputTest() {
			// Given:
			Object raw = null;

			// When:
			Map<String, Object> result = assembler.mapVal(raw);

			// Then:
			assertThat(result).isEmpty();
		}

		@Test
		void shouldReturnEmptyStringListForNonListInputTest() {
			// Given:
			Object raw = "not a list";

			// When:
			List<String> result = assembler.stringList(raw);

			// Then:
			assertThat(result).isEmpty();
		}

		@Test
		void shouldConvertItemsToStringListTest() {
			// Given:
			List<Object> raw = Arrays.asList("a", 42, null);

			// When:
			List<String> result = assembler.stringList(raw);

			// Then:
			assertThat(result).containsExactly("a", "42");
		}
	}

	@Nested
	class OrderedMaterialRecords {

		@Test
		void shouldReturnEmptyOrderedMaterialRecordsForEmptyIdsTest() {
			// Given:
			List<Long> orderedIds = List.of();

			// When:
			List<MaterialRecord> result = assembler.orderedMaterialRecords(orderedIds);

			// Then:
			assertThat(result).isEmpty();
		}

		@Test
		void shouldPreserveOrderInOrderedMaterialRecordsTest() {
			// Given:
			MaterialRecord first = Instancio.of(MaterialRecord.class)
					.set(field("id"), 20L)
					.set(field("title"), "Second")
					.create();
			MaterialRecord second = Instancio.of(MaterialRecord.class)
					.set(field("id"), 10L)
					.set(field("title"), "First")
					.create();
			when(materialRecordQueryService.loadMaterialRecordsByIds(List.of(10L, 20L)))
					.thenReturn(List.of(second, first));

			// When:
			List<MaterialRecord> result = assembler.orderedMaterialRecords(List.of(10L, 20L));

			// Then:
			assertThat(result.get(0).title()).isEqualTo("First");
			assertThat(result.get(1).title()).isEqualTo("Second");
		}

		@Test
		void shouldSkipMissingRecordsInOrderedMaterialRecordsTest() {
			// Given:
			when(materialRecordQueryService.loadMaterialRecordsByIds(List.of(10L, 20L)))
					.thenReturn(List.of(Instancio.of(MaterialRecord.class)
							.set(field("id"), 10L)
							.set(field("title"), "Only")
							.create()));

			// When:
			List<MaterialRecord> result = assembler.orderedMaterialRecords(List.of(10L, 20L));

			// Then:
			assertThat(result).hasSize(1);
		}
	}
}
