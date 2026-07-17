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
import com.aidigital.aionboarding.service.common.mapping.ServiceMapperConfig;
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
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.time.LocalDateTime;
import java.util.List;

@Mapper(config = ServiceMapperConfig.class, implementationName = "MaterialMapperImpl")
public interface MaterialMapper {

	/**
	 * Creates a material entity from the validated payload and request-owned metadata.
	 *
	 * @param payload                validated material payload
	 * @param coverImageStorageKey   normalized cover image storage key
	 * @param coverImageOriginalName normalized cover image original name
	 * @param coverImageMimeType     normalized cover image MIME type
	 * @param createdBy              display name of the creator
	 * @param createdByUser          creator entity reference
	 * @param timestamp              UTC timestamp used for create and update fields
	 * @return unsaved material entity
	 */
	@BeanMapping(ignoreByDefault = true)
	@Mapping(target = "title", source = "payload.title")
	@Mapping(target = "description", source = "payload.description")
	@Mapping(target = "textContent", source = "payload.text")
	@Mapping(target = "tags", source = "payload.tags")
	@Mapping(target = "coverImageStorageKey", source = "coverImageStorageKey")
	@Mapping(target = "coverImageOriginalName", source = "coverImageOriginalName")
	@Mapping(target = "coverImageMimeType", source = "coverImageMimeType")
	@Mapping(target = "createdBy", source = "createdBy")
	@Mapping(target = "createdByUser", source = "createdByUser")
	@Mapping(target = "createdAt", source = "timestamp")
	@Mapping(target = "updatedAt", source = "timestamp")
	Material toNewMaterial(ValidatedMaterialPayload payload, String coverImageStorageKey,
	                       String coverImageOriginalName, String coverImageMimeType, String createdBy,
	                       User createdByUser, LocalDateTime timestamp);

	/**
	 * Applies validated mutable material fields onto an existing material entity.
	 *
	 * @param material               target material entity
	 * @param payload                validated material payload
	 * @param coverImageStorageKey   normalized cover image storage key
	 * @param coverImageOriginalName normalized cover image original name
	 * @param coverImageMimeType     normalized cover image MIME type
	 * @param timestamp              UTC update timestamp
	 */
	@BeanMapping(ignoreByDefault = true)
	@Mapping(target = "title", source = "payload.title")
	@Mapping(target = "description", source = "payload.description")
	@Mapping(target = "textContent", source = "payload.text")
	@Mapping(target = "tags", source = "payload.tags")
	@Mapping(target = "coverImageStorageKey", source = "coverImageStorageKey")
	@Mapping(target = "coverImageOriginalName", source = "coverImageOriginalName")
	@Mapping(target = "coverImageMimeType", source = "coverImageMimeType")
	@Mapping(target = "updatedAt", source = "timestamp")
	void updateMaterial(@MappingTarget Material material, ValidatedMaterialPayload payload,
	                    String coverImageStorageKey, String coverImageOriginalName,
	                    String coverImageMimeType, LocalDateTime timestamp);

	/**
	 * Creates a material file entity from a normalized attachment payload.
	 *
	 * @param material          owning material
	 * @param kind              file kind reference
	 * @param originalName      normalized original file name
	 * @param storageKey        normalized storage key
	 * @param mimeType          normalized MIME type
	 * @param sizeBytes         normalized file size
	 * @param openaiFileId      optional OpenAI file ID
	 * @param openaiFilePurpose normalized OpenAI file purpose
	 * @param openaiFileStatus  normalized OpenAI file status
	 * @param openaiFileError   normalized OpenAI file error
	 * @param openaiUploadedAt  optional OpenAI upload timestamp
	 * @param createdAt         UTC creation timestamp
	 * @return unsaved material file entity
	 */
	@BeanMapping(ignoreByDefault = true)
	@Mapping(target = "material", source = "material")
	@Mapping(target = "kind", source = "kind")
	@Mapping(target = "originalName", source = "originalName")
	@Mapping(target = "storageKey", source = "storageKey")
	@Mapping(target = "mimeType", source = "mimeType")
	@Mapping(target = "sizeBytes", source = "sizeBytes")
	@Mapping(target = "openaiFileId", source = "openaiFileId")
	@Mapping(target = "openaiFilePurpose", source = "openaiFilePurpose")
	@Mapping(target = "openaiFileStatus", source = "openaiFileStatus")
	@Mapping(target = "openaiFileError", source = "openaiFileError")
	@Mapping(target = "openaiUploadedAt", source = "openaiUploadedAt")
	@Mapping(target = "createdAt", source = "createdAt")
	MaterialFile toNewMaterialFile(Material material, MaterialFileKind kind, String originalName,
	                               String storageKey, String mimeType, Long sizeBytes, String openaiFileId,
	                               String openaiFilePurpose, String openaiFileStatus, String openaiFileError,
	                               LocalDateTime openaiUploadedAt, LocalDateTime createdAt);

	/**
	 * Applies normalized attachment values onto an existing material file entity.
	 *
	 * @param file              target material file entity
	 * @param kind              file kind reference
	 * @param originalName      normalized original file name
	 * @param storageKey        normalized storage key
	 * @param mimeType          normalized MIME type
	 * @param sizeBytes         normalized file size
	 * @param openaiFileId      optional OpenAI file ID
	 * @param openaiFilePurpose normalized OpenAI file purpose
	 * @param openaiFileStatus  normalized OpenAI file status
	 * @param openaiFileError   normalized OpenAI file error
	 * @param openaiUploadedAt  optional OpenAI upload timestamp
	 */
	@BeanMapping(ignoreByDefault = true)
	@Mapping(target = "kind", source = "kind")
	@Mapping(target = "originalName", source = "originalName")
	@Mapping(target = "storageKey", source = "storageKey")
	@Mapping(target = "mimeType", source = "mimeType")
	@Mapping(target = "sizeBytes", source = "sizeBytes")
	@Mapping(target = "openaiFileId", source = "openaiFileId")
	@Mapping(target = "openaiFilePurpose", source = "openaiFilePurpose")
	@Mapping(target = "openaiFileStatus", source = "openaiFileStatus")
	@Mapping(target = "openaiFileError", source = "openaiFileError")
	@Mapping(target = "openaiUploadedAt", source = "openaiUploadedAt")
	void updateMaterialFile(@MappingTarget MaterialFile file, MaterialFileKind kind, String originalName,
	                        String storageKey, String mimeType, Long sizeBytes, String openaiFileId,
	                        String openaiFilePurpose, String openaiFileStatus, String openaiFileError,
	                        LocalDateTime openaiUploadedAt);

	/**
	 * Creates a material-file-kind reference with only the identifier populated.
	 *
	 * @param id material file kind identifier
	 * @return material file kind reference
	 */
	@BeanMapping(ignoreByDefault = true)
	@Mapping(target = "id", source = "id")
	MaterialFileKind toMaterialFileKindReference(Long id);

	/**
	 * Creates a material link entity from prepared link metadata.
	 *
	 * @param material  owning material
	 * @param record    prepared link metadata
	 * @param sortOrder display sort order
	 * @return unsaved material link entity
	 */
	@BeanMapping(ignoreByDefault = true)
	@Mapping(target = "material", source = "material")
	@Mapping(target = "url", source = "record.url")
	@Mapping(target = "sortOrder", source = "sortOrder")
	@Mapping(target = "title", source = "record.title")
	@Mapping(target = "description", source = "record.description")
	@Mapping(target = "imageUrl", source = "record.imageUrl")
	@Mapping(target = "siteName", source = "record.siteName")
	@Mapping(target = "extractedText", source = "record.extractedText")
	@Mapping(target = "metadataError", source = "record.metadataError")
	MaterialLink toNewMaterialLink(Material material, PreparedLinkRecord record, int sortOrder);

	/**
	 * Creates a material YouTube URL entity from prepared YouTube metadata.
	 *
	 * @param material  owning material
	 * @param record    prepared YouTube metadata
	 * @param sortOrder display sort order
	 * @return unsaved material YouTube URL entity
	 */
	@BeanMapping(ignoreByDefault = true)
	@Mapping(target = "material", source = "material")
	@Mapping(target = "url", source = "record.url")
	@Mapping(target = "sortOrder", source = "sortOrder")
	@Mapping(target = "title", source = "record.title")
	@Mapping(target = "authorName", source = "record.authorName")
	@Mapping(target = "authorUrl", source = "record.authorUrl")
	@Mapping(target = "thumbnailUrl", source = "record.thumbnailUrl")
	@Mapping(target = "thumbnailWidth", source = "record.thumbnailWidth")
	@Mapping(target = "thumbnailHeight", source = "record.thumbnailHeight")
	@Mapping(target = "providerName", source = "record.providerName")
	@Mapping(target = "metadataError", source = "record.metadataError")
	MaterialYoutubeUrl toNewMaterialYoutubeUrl(Material material, PreparedYoutubeRecord record, int sortOrder);

	/**
	 * Maps a YouTube URL entity to the service record exposed with material details.
	 *
	 * @param item YouTube URL entity
	 * @return YouTube video record
	 */
	default MaterialYoutubeVideoRecord toYoutubeVideoRecord(MaterialYoutubeUrl item) {
		return new MaterialYoutubeVideoRecord(
				item.getUrl(),
				nullToEmpty(item.getTitle()),
				nullToEmpty(item.getAuthorName()),
				nullToEmpty(item.getAuthorUrl()),
				nullToEmpty(item.getThumbnailUrl()),
				item.getThumbnailWidth(),
				item.getThumbnailHeight(),
				nullToEmpty(item.getProviderName()),
				nullToEmpty(item.getMetadataError())
		);
	}

	/**
	 * Maps a material link entity to the service record exposed with material details.
	 *
	 * @param item material link entity
	 * @return link asset record
	 */
	default MaterialLinkAssetRecord toLinkAssetRecord(MaterialLink item) {
		return new MaterialLinkAssetRecord(
				item.getUrl(),
				nullToEmpty(item.getTitle()),
				nullToEmpty(item.getDescription()),
				nullToEmpty(item.getImageUrl()),
				nullToEmpty(item.getSiteName()),
				nullToEmpty(item.getExtractedText()),
				nullToEmpty(item.getMetadataError())
		);
	}

	/**
	 * Maps a material file entity to the service record exposed with material details.
	 *
	 * @param item material file entity
	 * @return material file record
	 */
	default MaterialFileRecord toFileRecord(MaterialFile item) {
		return new MaterialFileRecord(
				item.getId(),
				item.getOriginalName(),
				item.getStorageKey(),
				item.getMimeType(),
				item.getSizeBytes() == null ? 0L : item.getSizeBytes(),
				item.getKind() == null ? MaterialFileKindCode.FILE : item.getKind().getCode(),
				item.getOpenaiFileId() == null ? "" : item.getOpenaiFileId(),
				nullToEmpty(item.getOpenaiFilePurpose()),
				nullToEmpty(item.getOpenaiFileStatus()),
				nullToEmpty(item.getOpenaiFileError()),
				item.getOpenaiUploadedAt()
		);
	}

	default MaterialRecord toRecord(
			Material material,
			List<MaterialYoutubeUrl> youtubeUrls,
			List<MaterialLink> links,
			List<MaterialFile> files,
			long usageCount
	) {
		return new MaterialRecord(
				material.getId(),
				material.getTitle(),
				material.getDescription() == null ? "" : material.getDescription(),
				material.getTextContent() == null ? "" : material.getTextContent(),
				nullToEmpty(material.getCoverImageStorageKey()),
				nullToEmpty(material.getCoverImageOriginalName()),
				nullToEmpty(material.getCoverImageMimeType()),
				material.getCreatedByUser() == null ? null : material.getCreatedByUser().getId(),
				material.getCreatedBy() == null ? "" : material.getCreatedBy(),
				material.getTags() == null ? List.of() : material.getTags(),
				material.getCreatedAt(),
				material.getUpdatedAt(),
				usageCount,
				youtubeUrls.stream().map(MaterialYoutubeUrl::getUrl).toList(),
				youtubeUrls.stream().map(this::toYoutubeVideoRecord).toList(),
				links.stream().map(MaterialLink::getUrl).toList(),
				links.stream().map(this::toLinkAssetRecord).toList(),
				files.stream().map(this::toFileRecord).toList()
		);
	}

	/**
	 * Maps a bounded link preview projection to its service-layer record.
	 *
	 * @param item lean per-link projection selected directly by the repository
	 * @return link preview record
	 */
	default MaterialLinkSummaryRecord toLinkSummaryRecord(MaterialLinkSummaryProjection item) {
		return new MaterialLinkSummaryRecord(
				item.url(),
				nullToEmpty(item.title()),
				nullToEmpty(item.description()),
				nullToEmpty(item.imageUrl()),
				nullToEmpty(item.siteName())
		);
	}

	/**
	 * Maps a bounded attachment summary projection to its service-layer record.
	 *
	 * @param item lean per-attachment projection selected directly by the repository
	 * @return attachment summary record
	 */
	default MaterialFileSummaryRecord toFileSummaryRecord(MaterialFileSummaryProjection item) {
		return new MaterialFileSummaryRecord(
				item.id(),
				item.originalName(),
				item.storageKey(),
				item.mimeType(),
				item.sizeBytes() == null ? 0L : item.sizeBytes(),
				item.kindCode() == null ? MaterialFileKindCode.FILE : item.kindCode()
		);
	}

	/**
	 * Assembles a bounded Library search summary from the base projection and batch-loaded
	 * children. YouTube metadata stays full-fidelity (not targeted by the summary contract);
	 * link and file children carry only their preview fields.
	 *
	 * @param base          lean base-material projection selected directly by the repository
	 * @param youtubeUrls   YouTube URL entities for the material
	 * @param linkPreviews  lean link preview projections for the material
	 * @param fileSummaries lean attachment summary projections for the material
	 * @param usageCount    number of lessons referencing the material
	 * @return assembled search summary record
	 */
	default MaterialSearchSummaryRecord toSearchSummaryRecord(
			MaterialSearchSummaryProjection base,
			List<MaterialYoutubeUrl> youtubeUrls,
			List<MaterialLinkSummaryProjection> linkPreviews,
			List<MaterialFileSummaryProjection> fileSummaries,
			long usageCount
	) {
		return new MaterialSearchSummaryRecord(
				base.id(),
				base.title(),
				base.description() == null ? "" : base.description(),
				base.hasText(),
				nullToEmpty(base.coverImageStorageKey()),
				nullToEmpty(base.coverImageOriginalName()),
				nullToEmpty(base.coverImageMimeType()),
				base.createdByUserId(),
				base.createdBy() == null ? "" : base.createdBy(),
				base.tags() == null ? List.of() : base.tags(),
				base.createdAt(),
				base.updatedAt(),
				usageCount,
				youtubeUrls.stream().map(MaterialYoutubeUrl::getUrl).toList(),
				youtubeUrls.stream().map(this::toYoutubeVideoRecord).toList(),
				linkPreviews.stream().map(MaterialLinkSummaryProjection::url).toList(),
				linkPreviews.stream().map(this::toLinkSummaryRecord).toList(),
				fileSummaries.stream().map(this::toFileSummaryRecord).toList()
		);
	}

	/**
	 * Maps a {@link MaterialFile} entity to a {@link MaterialAttachmentInput} carrying all OpenAI upload
	 * state fields. Null kind falls back to {@link MaterialFileKindCode#FILE}; {@code openaiFileId} is
	 * preserved as-is (null when never uploaded) so callers can distinguish the never-uploaded state.
	 *
	 * @param file source entity
	 * @return mapped attachment input
	 */
	default MaterialAttachmentInput toAttachmentInput(MaterialFile file) {
		return new MaterialAttachmentInput(
				file.getId(),
				file.getOriginalName(),
				file.getStorageKey(),
				file.getMimeType(),
				file.getSizeBytes() == null ? 0L : file.getSizeBytes(),
				file.getKind() == null ? MaterialFileKindCode.FILE : file.getKind().getCode(),
				file.getOpenaiFileId(),
				nullToEmpty(file.getOpenaiFilePurpose()),
				nullToEmpty(file.getOpenaiFileStatus()),
				nullToEmpty(file.getOpenaiFileError()),
				file.getOpenaiUploadedAt()
		);
	}

	/**
	 * Maps OpenAI upload state from a material file entity.
	 *
	 * @param file material file entity
	 * @return OpenAI upload record
	 */
	default MaterialOpenAiUploadRecord toOpenAiUploadRecord(MaterialFile file) {
		return new MaterialOpenAiUploadRecord(
				file.getId(),
				file.getOpenaiFileId() == null ? "" : file.getOpenaiFileId(),
				nullToEmpty(file.getOpenaiFilePurpose()),
				nullToEmpty(file.getOpenaiFileStatus()),
				nullToEmpty(file.getOpenaiFileError()),
				file.getOpenaiUploadedAt()
		);
	}

	/**
	 * Converts nullable values to non-null strings.
	 *
	 * @param value raw value
	 * @return string value or empty string
	 */
	default String nullToEmpty(Object value) {
		return value == null ? "" : String.valueOf(value);
	}
}
