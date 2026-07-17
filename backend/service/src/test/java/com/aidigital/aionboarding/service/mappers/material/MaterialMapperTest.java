package com.aidigital.aionboarding.service.mappers.material;

import com.aidigital.aionboarding.domain.common.dictionary.MaterialFileKindCode;
import com.aidigital.aionboarding.domain.common.dictionary.entities.MaterialFileKind;
import com.aidigital.aionboarding.domain.material.entities.Material;
import com.aidigital.aionboarding.domain.material.entities.MaterialFile;
import com.aidigital.aionboarding.domain.material.entities.MaterialLink;
import com.aidigital.aionboarding.domain.material.entities.MaterialYoutubeUrl;
import com.aidigital.aionboarding.domain.material.repositories.MaterialFileSummaryProjection;
import com.aidigital.aionboarding.domain.material.repositories.MaterialLinkSummaryProjection;
import com.aidigital.aionboarding.domain.material.repositories.MaterialSearchSummaryProjection;
import com.aidigital.aionboarding.domain.user.entities.User;
import com.aidigital.aionboarding.service.material.models.MaterialAttachmentInput;
import com.aidigital.aionboarding.service.material.models.MaterialFileRecord;
import com.aidigital.aionboarding.service.material.models.MaterialFileSummaryRecord;
import com.aidigital.aionboarding.service.material.models.MaterialLinkAssetRecord;
import com.aidigital.aionboarding.service.material.models.MaterialLinkSummaryRecord;
import com.aidigital.aionboarding.service.material.models.MaterialOpenAiUploadRecord;
import com.aidigital.aionboarding.service.material.models.MaterialRecord;
import com.aidigital.aionboarding.service.material.models.MaterialSearchSummaryRecord;
import com.aidigital.aionboarding.service.material.models.MaterialYoutubeVideoRecord;
import com.aidigital.aionboarding.service.material.services.MaterialLinkService.PreparedLinkRecord;
import com.aidigital.aionboarding.service.material.services.MaterialYoutubeService.PreparedYoutubeRecord;
import com.aidigital.aionboarding.service.material.validation.MaterialPayloadValidator.ValidatedMaterialPayload;
import org.instancio.Instancio;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.instancio.Select.field;

class MaterialMapperTest {

	private final MaterialMapper mapper = new MaterialMapperImpl();

	@Nested
	class ToNewMaterial {

		@Test
		void shouldMapAllFieldsFromPayloadAndMetadataTest() {
			// Given:
			ValidatedMaterialPayload payload = Instancio.of(ValidatedMaterialPayload.class)
					.set(field("title"), "Material title")
					.set(field("description"), "Material description")
					.set(field("text"), "Material text")
					.set(field("tags"), List.of("tag1", "tag2"))
					.create();
			User createdByUser = Instancio.create(User.class);
			LocalDateTime timestamp = LocalDateTime.parse("2026-07-15T10:00:00");

			// When:
			Material result = mapper.toNewMaterial(
					payload,
					"cover-key",
					"cover-name.png",
					"image/png",
					"Creator",
					createdByUser,
					timestamp
			);

			// Then:
			assertThat(result.getTitle()).isEqualTo("Material title");
			assertThat(result.getDescription()).isEqualTo("Material description");
			assertThat(result.getTextContent()).isEqualTo("Material text");
			assertThat(result.getTags()).containsExactly("tag1", "tag2");
			assertThat(result.getCoverImageStorageKey()).isEqualTo("cover-key");
			assertThat(result.getCoverImageOriginalName()).isEqualTo("cover-name.png");
			assertThat(result.getCoverImageMimeType()).isEqualTo("image/png");
			assertThat(result.getCreatedBy()).isEqualTo("Creator");
			assertThat(result.getCreatedByUser()).isSameAs(createdByUser);
			assertThat(result.getCreatedAt()).isEqualTo(timestamp);
			assertThat(result.getUpdatedAt()).isEqualTo(timestamp);
		}

		@Test
		void shouldReturnNullWhenAllArgumentsAreNullTest() {
			// When:
			Material result = mapper.toNewMaterial(null, null, null, null, null, null, null);

			// Then:
			assertThat(result).isNull();
		}

		@Test
		void shouldConvertNullPayloadFieldsToEmptyStringsTest() {
			// Given:
			ValidatedMaterialPayload payload = Instancio.of(ValidatedMaterialPayload.class)
					.set(field("title"), null)
					.set(field("description"), null)
					.set(field("text"), null)
					.set(field("tags"), null)
					.create();

			// When:
			Material result = mapper.toNewMaterial(
					payload,
					null,
					null,
					null,
					null,
					null,
					null
			);

			// Then:
			assertThat(result.getTitle()).isEmpty();
			assertThat(result.getDescription()).isEmpty();
			assertThat(result.getTextContent()).isEmpty();
			assertThat(result.getCoverImageStorageKey()).isEmpty();
			assertThat(result.getCoverImageOriginalName()).isEmpty();
			assertThat(result.getCoverImageMimeType()).isEmpty();
			assertThat(result.getCreatedBy()).isEmpty();
		}
	}

	@Nested
	class UpdateMaterial {

		@Test
		void shouldApplyPayloadFieldsOntoExistingMaterialTest() {
			// Given:
			Material material = Instancio.create(Material.class);
			material.setTags(new ArrayList<>(List.of("old")));
			ValidatedMaterialPayload payload = Instancio.of(ValidatedMaterialPayload.class)
					.set(field("title"), "Updated title")
					.set(field("description"), "Updated description")
					.set(field("text"), "Updated text")
					.set(field("tags"), List.of("new1", "new2"))
					.create();
			LocalDateTime timestamp = LocalDateTime.parse("2026-07-15T11:00:00");

			// When:
			mapper.updateMaterial(
					material,
					payload,
					"new-cover-key",
					"new-cover-name.png",
					"image/png",
					timestamp
			);

			// Then:
			assertThat(material.getTitle()).isEqualTo("Updated title");
			assertThat(material.getDescription()).isEqualTo("Updated description");
			assertThat(material.getTextContent()).isEqualTo("Updated text");
			assertThat(material.getTags()).containsExactly("new1", "new2");
			assertThat(material.getCoverImageStorageKey()).isEqualTo("new-cover-key");
			assertThat(material.getCoverImageOriginalName()).isEqualTo("new-cover-name.png");
			assertThat(material.getCoverImageMimeType()).isEqualTo("image/png");
			assertThat(material.getUpdatedAt()).isEqualTo(timestamp);
		}

		@Test
		void shouldReturnEarlyWhenAllArgumentsAreNullTest() {
			// Given:
			Material material = Instancio.create(Material.class);
			String originalTitle = material.getTitle();

			// When:
			mapper.updateMaterial(material, null, null, null, null, null);

			// Then:
			assertThat(material.getTitle()).isEqualTo(originalTitle);
		}

		@Test
		void shouldReplaceNullTagsListWhenMaterialTagsAreNullTest() {
			// Given:
			Material material = Instancio.create(Material.class);
			material.setTags(null);
			ValidatedMaterialPayload payload = Instancio.of(ValidatedMaterialPayload.class)
					.set(field("tags"), List.of("tag"))
					.create();

			// When:
			mapper.updateMaterial(material, payload, null, null, null, null);

			// Then:
			assertThat(material.getTags()).containsExactly("tag");
		}
	}

	@Nested
	class ToNewMaterialFile {

		@Test
		void shouldMapAllFieldsTest() {
			// Given:
			Material material = Instancio.create(Material.class);
			MaterialFileKind kind = Instancio.create(MaterialFileKind.class);
			LocalDateTime createdAt = LocalDateTime.parse("2026-07-15T10:00:00");

			// When:
			MaterialFile result = mapper.toNewMaterialFile(
					material,
					kind,
					"file.pdf",
					"storage-key",
					"application/pdf",
					2048L,
					"openai-id",
					"purpose",
					"status",
					"error",
					LocalDateTime.parse("2026-07-15T09:00:00"),
					createdAt
			);

			// Then:
			assertThat(result.getMaterial()).isSameAs(material);
			assertThat(result.getKind()).isSameAs(kind);
			assertThat(result.getOriginalName()).isEqualTo("file.pdf");
			assertThat(result.getStorageKey()).isEqualTo("storage-key");
			assertThat(result.getMimeType()).isEqualTo("application/pdf");
			assertThat(result.getSizeBytes()).isEqualTo(2048L);
			assertThat(result.getOpenaiFileId()).isEqualTo("openai-id");
			assertThat(result.getOpenaiFilePurpose()).isEqualTo("purpose");
			assertThat(result.getOpenaiFileStatus()).isEqualTo("status");
			assertThat(result.getOpenaiFileError()).isEqualTo("error");
			assertThat(result.getOpenaiUploadedAt()).isEqualTo("2026-07-15T09:00:00");
			assertThat(result.getCreatedAt()).isEqualTo(createdAt);
		}

		@Test
		void shouldReturnNullWhenAllArgumentsAreNullTest() {
			// When:
			MaterialFile result = mapper.toNewMaterialFile(
					null, null, null, null, null, null, null, null, null, null, null, null
			);

			// Then:
			assertThat(result).isNull();
		}
	}

	@Nested
	class UpdateMaterialFile {

		@Test
		void shouldApplyAllFieldsOntoExistingFileTest() {
			// Given:
			MaterialFile file = Instancio.create(MaterialFile.class);
			MaterialFileKind kind = Instancio.create(MaterialFileKind.class);

			// When:
			mapper.updateMaterialFile(
					file,
					kind,
					"updated.pdf",
					"updated-key",
					"application/pdf",
					4096L,
					"updated-openai-id",
					"updated-purpose",
					"updated-status",
					"updated-error",
					LocalDateTime.parse("2026-07-15T12:00:00")
			);

			// Then:
			assertThat(file.getKind()).isSameAs(kind);
			assertThat(file.getOriginalName()).isEqualTo("updated.pdf");
			assertThat(file.getStorageKey()).isEqualTo("updated-key");
			assertThat(file.getMimeType()).isEqualTo("application/pdf");
			assertThat(file.getSizeBytes()).isEqualTo(4096L);
			assertThat(file.getOpenaiFileId()).isEqualTo("updated-openai-id");
			assertThat(file.getOpenaiFilePurpose()).isEqualTo("updated-purpose");
			assertThat(file.getOpenaiFileStatus()).isEqualTo("updated-status");
			assertThat(file.getOpenaiFileError()).isEqualTo("updated-error");
			assertThat(file.getOpenaiUploadedAt()).isEqualTo("2026-07-15T12:00:00");
		}

		@Test
		void shouldReturnEarlyWhenAllArgumentsAreNullTest() {
			// Given:
			MaterialFile file = Instancio.create(MaterialFile.class);
			String originalName = file.getOriginalName();

			// When:
			mapper.updateMaterialFile(file, null, null, null, null, null, null, null, null, null, null);

			// Then:
			assertThat(file.getOriginalName()).isEqualTo(originalName);
		}
	}

	@Nested
	class ToMaterialFileKindReference {

		@Test
		void shouldCreateReferenceWithOnlyIdTest() {
			// When:
			MaterialFileKind result = mapper.toMaterialFileKindReference(42L);

			// Then:
			assertThat(result.getId()).isEqualTo(42L);
			assertThat(result.getCode()).isNull();
		}

		@Test
		void shouldReturnNullWhenIdIsNullTest() {
			// When:
			MaterialFileKind result = mapper.toMaterialFileKindReference(null);

			// Then:
			assertThat(result).isNull();
		}
	}

	@Nested
	class ToNewMaterialLink {

		@Test
		void shouldMapAllFieldsFromPreparedRecordTest() {
			// Given:
			Material material = Instancio.create(Material.class);
			PreparedLinkRecord record = Instancio.of(PreparedLinkRecord.class)
					.set(field("url"), "https://example.com")
					.set(field("title"), "Example")
					.set(field("description"), "Description")
					.set(field("imageUrl"), "https://image.png")
					.set(field("siteName"), "Example site")
					.set(field("extractedText"), "Extracted")
					.set(field("metadataError"), "Error")
					.create();

			// When:
			MaterialLink result = mapper.toNewMaterialLink(material, record, 3);

			// Then:
			assertThat(result.getMaterial()).isSameAs(material);
			assertThat(result.getUrl()).isEqualTo("https://example.com");
			assertThat(result.getTitle()).isEqualTo("Example");
			assertThat(result.getDescription()).isEqualTo("Description");
			assertThat(result.getImageUrl()).isEqualTo("https://image.png");
			assertThat(result.getSiteName()).isEqualTo("Example site");
			assertThat(result.getExtractedText()).isEqualTo("Extracted");
			assertThat(result.getMetadataError()).isEqualTo("Error");
			assertThat(result.getSortOrder()).isEqualTo(3);
		}

		@Test
		void shouldReturnNullWhenMaterialAndRecordAreNullTest() {
			// When:
			MaterialLink result = mapper.toNewMaterialLink(null, null, 0);

			// Then:
			assertThat(result).isNull();
		}
	}

	@Nested
	class ToNewMaterialYoutubeUrl {

		@Test
		void shouldMapAllFieldsFromPreparedRecordTest() {
			// Given:
			Material material = Instancio.create(Material.class);
			PreparedYoutubeRecord record = Instancio.of(PreparedYoutubeRecord.class)
					.set(field("url"), "https://youtu.be/abc")
					.set(field("title"), "YouTube title")
					.set(field("authorName"), "Author")
					.set(field("authorUrl"), "https://author")
					.set(field("thumbnailUrl"), "https://thumb.jpg")
					.set(field("thumbnailWidth"), 120)
					.set(field("thumbnailHeight"), 90)
					.set(field("providerName"), "YouTube")
					.set(field("metadataError"), "")
					.create();

			// When:
			MaterialYoutubeUrl result = mapper.toNewMaterialYoutubeUrl(material, record, 1);

			// Then:
			assertThat(result.getMaterial()).isSameAs(material);
			assertThat(result.getUrl()).isEqualTo("https://youtu.be/abc");
			assertThat(result.getTitle()).isEqualTo("YouTube title");
			assertThat(result.getAuthorName()).isEqualTo("Author");
			assertThat(result.getAuthorUrl()).isEqualTo("https://author");
			assertThat(result.getThumbnailUrl()).isEqualTo("https://thumb.jpg");
			assertThat(result.getThumbnailWidth()).isEqualTo(120);
			assertThat(result.getThumbnailHeight()).isEqualTo(90);
			assertThat(result.getProviderName()).isEqualTo("YouTube");
			assertThat(result.getMetadataError()).isEmpty();
			assertThat(result.getSortOrder()).isEqualTo(1);
		}

		@Test
		void shouldReturnNullWhenMaterialAndRecordAreNullTest() {
			// When:
			MaterialYoutubeUrl result = mapper.toNewMaterialYoutubeUrl(null, null, 0);

			// Then:
			assertThat(result).isNull();
		}
	}

	@Nested
	class ToYoutubeVideoRecord {

		@Test
		void shouldMapFieldsAndConvertNullsToEmptyStringsTest() {
			// Given:
			MaterialYoutubeUrl item = Instancio.create(MaterialYoutubeUrl.class);
			item.setTitle(null);
			item.setAuthorName(null);
			item.setAuthorUrl(null);
			item.setThumbnailUrl(null);
			item.setProviderName(null);
			item.setMetadataError(null);

			// When:
			MaterialYoutubeVideoRecord result = mapper.toYoutubeVideoRecord(item);

			// Then:
			assertThat(result.url()).isEqualTo(item.getUrl());
			assertThat(result.title()).isEmpty();
			assertThat(result.authorName()).isEmpty();
			assertThat(result.authorUrl()).isEmpty();
			assertThat(result.thumbnailUrl()).isEmpty();
			assertThat(result.thumbnailWidth()).isEqualTo(item.getThumbnailWidth());
			assertThat(result.thumbnailHeight()).isEqualTo(item.getThumbnailHeight());
			assertThat(result.providerName()).isEmpty();
			assertThat(result.metadataError()).isEmpty();
		}
	}

	@Nested
	class ToLinkAssetRecord {

		@Test
		void shouldMapFieldsAndConvertNullsToEmptyStringsTest() {
			// Given:
			MaterialLink item = Instancio.create(MaterialLink.class);
			item.setTitle(null);
			item.setDescription(null);
			item.setImageUrl(null);
			item.setSiteName(null);
			item.setExtractedText(null);
			item.setMetadataError(null);

			// When:
			MaterialLinkAssetRecord result = mapper.toLinkAssetRecord(item);

			// Then:
			assertThat(result.url()).isEqualTo(item.getUrl());
			assertThat(result.title()).isEmpty();
			assertThat(result.description()).isEmpty();
			assertThat(result.imageUrl()).isEmpty();
			assertThat(result.siteName()).isEmpty();
			assertThat(result.extractedText()).isEmpty();
			assertThat(result.metadataError()).isEmpty();
		}
	}

	@Nested
	class ToFileRecord {

		@Test
		void shouldMapFieldsAndUseNullFallbacksTest() {
			// Given:
			MaterialFile item = Instancio.create(MaterialFile.class);
			item.setSizeBytes(null);
			item.setKind(null);
			item.setOpenaiFileId(null);
			item.setOpenaiFilePurpose(null);
			item.setOpenaiFileStatus(null);
			item.setOpenaiFileError(null);

			// When:
			MaterialFileRecord result = mapper.toFileRecord(item);

			// Then:
			assertThat(result.id()).isEqualTo(item.getId());
			assertThat(result.name()).isEqualTo(item.getOriginalName());
			assertThat(result.storageKey()).isEqualTo(item.getStorageKey());
			assertThat(result.mimeType()).isEqualTo(item.getMimeType());
			assertThat(result.size()).isEqualTo(0L);
			assertThat(result.kind()).isEqualTo(MaterialFileKindCode.FILE);
			assertThat(result.openaiFileId()).isEmpty();
			assertThat(result.openaiFilePurpose()).isEmpty();
			assertThat(result.openaiFileStatus()).isEmpty();
			assertThat(result.openaiFileError()).isEmpty();
			assertThat(result.openaiUploadedAt()).isEqualTo(item.getOpenaiUploadedAt());
		}

		@Test
		void shouldUseKindCodeWhenKindIsPresentTest() {
			// Given:
			MaterialFile item = Instancio.create(MaterialFile.class);
			MaterialFileKind kind = new MaterialFileKind();
			kind.setCode(MaterialFileKindCode.IMAGE);
			item.setKind(kind);
			item.setSizeBytes(1024L);
			item.setOpenaiFileId("openai-1");

			// When:
			MaterialFileRecord result = mapper.toFileRecord(item);

			// Then:
			assertThat(result.kind()).isEqualTo(MaterialFileKindCode.IMAGE);
			assertThat(result.size()).isEqualTo(1024L);
			assertThat(result.openaiFileId()).isEqualTo("openai-1");
		}
	}

	@Nested
	class ToRecord {

		@Test
		void shouldAssembleMaterialRecordWithChildrenTest() {
			// Given:
			Material material = Instancio.create(Material.class);
			material.setDescription(null);
			material.setTextContent(null);
			material.setCoverImageStorageKey(null);
			material.setCoverImageOriginalName(null);
			material.setCoverImageMimeType(null);
			material.setCreatedByUser(null);
			material.setCreatedBy(null);
			material.setTags(null);

			MaterialYoutubeUrl youtube = Instancio.create(MaterialYoutubeUrl.class);
			youtube.setUrl("https://youtu.be/abc");
			youtube.setTitle(null);

			MaterialLink link = Instancio.create(MaterialLink.class);
			link.setUrl("https://example.com");
			link.setTitle(null);

			MaterialFile file = Instancio.create(MaterialFile.class);
			file.setKind(null);
			file.setSizeBytes(null);

			// When:
			MaterialRecord result = mapper.toRecord(material, List.of(youtube), List.of(link), List.of(file), 5L);

			// Then:
			assertThat(result.id()).isEqualTo(material.getId());
			assertThat(result.title()).isEqualTo(material.getTitle());
			assertThat(result.description()).isEmpty();
			assertThat(result.text()).isEmpty();
			assertThat(result.coverImageStorageKey()).isEmpty();
			assertThat(result.coverImageOriginalName()).isEmpty();
			assertThat(result.coverImageMimeType()).isEmpty();
			assertThat(result.createdByUserId()).isNull();
			assertThat(result.createdBy()).isEmpty();
			assertThat(result.tags()).isEmpty();
			assertThat(result.usageCount()).isEqualTo(5L);
			assertThat(result.youtubeUrls()).containsExactly("https://youtu.be/abc");
			assertThat(result.youtubeVideos()).hasSize(1);
			assertThat(result.links()).containsExactly("https://example.com");
			assertThat(result.linkAssets()).hasSize(1);
			assertThat(result.attachments()).hasSize(1);
		}
	}

	@Nested
	class ToLinkSummaryRecord {

		@Test
		void shouldMapProjectionFieldsAndConvertNullsToEmptyStringsTest() {
			// Given:
			MaterialLinkSummaryProjection projection = Instancio.of(MaterialLinkSummaryProjection.class)
					.set(field("url"), "https://example.com")
					.set(field("title"), null)
					.set(field("description"), null)
					.set(field("imageUrl"), null)
					.set(field("siteName"), null)
					.create();

			// When:
			MaterialLinkSummaryRecord result = mapper.toLinkSummaryRecord(projection);

			// Then:
			assertThat(result.url()).isEqualTo("https://example.com");
			assertThat(result.title()).isEmpty();
			assertThat(result.description()).isEmpty();
			assertThat(result.imageUrl()).isEmpty();
			assertThat(result.siteName()).isEmpty();
		}
	}

	@Nested
	class ToFileSummaryRecord {

		@Test
		void shouldMapProjectionFieldsAndUseNullFallbacksTest() {
			// Given:
			MaterialFileSummaryProjection projection = Instancio.of(MaterialFileSummaryProjection.class)
					.set(field("id"), 7L)
					.set(field("originalName"), "file.pdf")
					.set(field("storageKey"), "key")
					.set(field("mimeType"), "application/pdf")
					.set(field("sizeBytes"), null)
					.set(field("kindCode"), null)
					.create();

			// When:
			MaterialFileSummaryRecord result = mapper.toFileSummaryRecord(projection);

			// Then:
			assertThat(result.id()).isEqualTo(7L);
			assertThat(result.name()).isEqualTo("file.pdf");
			assertThat(result.storageKey()).isEqualTo("key");
			assertThat(result.mimeType()).isEqualTo("application/pdf");
			assertThat(result.size()).isEqualTo(0L);
			assertThat(result.kind()).isEqualTo(MaterialFileKindCode.FILE);
		}
	}

	@Nested
	class ToSearchSummaryRecord {

		@Test
		void shouldAssembleSearchSummaryWithChildrenTest() {
			// Given:
			MaterialSearchSummaryProjection base = Instancio.of(MaterialSearchSummaryProjection.class)
					.set(field("id"), 1L)
					.set(field("title"), "Title")
					.set(field("description"), null)
					.set(field("hasText"), true)
					.set(field("coverImageStorageKey"), null)
					.set(field("coverImageOriginalName"), null)
					.set(field("coverImageMimeType"), null)
					.set(field("createdBy"), null)
					.set(field("tags"), null)
					.create();

			MaterialYoutubeUrl youtube = Instancio.create(MaterialYoutubeUrl.class);
			youtube.setUrl("https://youtu.be/abc");

			MaterialLinkSummaryProjection linkProjection = Instancio.of(MaterialLinkSummaryProjection.class)
					.set(field("url"), "https://example.com")
					.create();

			MaterialFileSummaryProjection fileProjection = Instancio.of(MaterialFileSummaryProjection.class)
					.set(field("sizeBytes"), null)
					.set(field("kindCode"), null)
					.create();

			// When:
			MaterialSearchSummaryRecord result = mapper.toSearchSummaryRecord(
					base,
					List.of(youtube),
					List.of(linkProjection),
					List.of(fileProjection),
					3L
			);

			// Then:
			assertThat(result.id()).isEqualTo(1L);
			assertThat(result.title()).isEqualTo("Title");
			assertThat(result.description()).isEmpty();
			assertThat(result.hasText()).isTrue();
			assertThat(result.createdBy()).isEmpty();
			assertThat(result.tags()).isEmpty();
			assertThat(result.usageCount()).isEqualTo(3L);
			assertThat(result.youtubeUrls()).containsExactly(youtube.getUrl());
			assertThat(result.links()).containsExactly("https://example.com");
			assertThat(result.linkAssets()).hasSize(1);
			assertThat(result.attachments()).hasSize(1);
		}
	}

	@Nested
	class ToAttachmentInput {

		@Test
		void shouldMapEntityFieldsPreservingOpenaiFileIdNullabilityTest() {
			// Given:
			MaterialFile file = Instancio.create(MaterialFile.class);
			file.setKind(null);
			file.setOpenaiFileId(null);
			file.setOpenaiFilePurpose(null);
			file.setOpenaiFileStatus(null);
			file.setOpenaiFileError(null);
			file.setSizeBytes(null);

			// When:
			MaterialAttachmentInput result = mapper.toAttachmentInput(file);

			// Then:
			assertThat(result.id()).isEqualTo(file.getId());
			assertThat(result.originalName()).isEqualTo(file.getOriginalName());
			assertThat(result.storageKey()).isEqualTo(file.getStorageKey());
			assertThat(result.mimeType()).isEqualTo(file.getMimeType());
			assertThat(result.sizeBytes()).isEqualTo(0L);
			assertThat(result.kind()).isEqualTo(MaterialFileKindCode.FILE);
			assertThat(result.openaiFileId()).isNull();
			assertThat(result.openaiFilePurpose()).isEmpty();
			assertThat(result.openaiFileStatus()).isEmpty();
			assertThat(result.openaiFileError()).isEmpty();
			assertThat(result.openaiUploadedAt()).isEqualTo(file.getOpenaiUploadedAt());
		}
	}

	@Nested
	class ToOpenAiUploadRecord {

		@Test
		void shouldMapUploadFieldsAndConvertOpenaiFileIdNullToEmptyTest() {
			// Given:
			MaterialFile file = Instancio.create(MaterialFile.class);
			file.setOpenaiFileId(null);
			file.setOpenaiFilePurpose(null);
			file.setOpenaiFileStatus(null);
			file.setOpenaiFileError(null);

			// When:
			MaterialOpenAiUploadRecord result = mapper.toOpenAiUploadRecord(file);

			// Then:
			assertThat(result.id()).isEqualTo(file.getId());
			assertThat(result.openaiFileId()).isEmpty();
			assertThat(result.openaiFilePurpose()).isEmpty();
			assertThat(result.openaiFileStatus()).isEmpty();
			assertThat(result.openaiFileError()).isEmpty();
			assertThat(result.openaiUploadedAt()).isEqualTo(file.getOpenaiUploadedAt());
		}
	}

	@Nested
	class NullToEmpty {

		@Test
		void shouldReturnEmptyStringForNullValueTest() {
			// When:
			String result = mapper.nullToEmpty(null);

			// Then:
			assertThat(result).isEmpty();
		}

		@Test
		void shouldReturnStringValueForNonNullValueTest() {
			// When:
			String result = mapper.nullToEmpty(123);

			// Then:
			assertThat(result).isEqualTo("123");
		}
	}
}
