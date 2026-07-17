package com.aidigital.aionboarding.mappers.material;

import com.aidigital.aionboarding.api.v1.model.CreateMaterialRequestV1;
import com.aidigital.aionboarding.api.v1.model.CreateMaterialResponseV1;
import com.aidigital.aionboarding.api.v1.model.MaterialAttachmentInputV1;
import com.aidigital.aionboarding.api.v1.model.MaterialFileSummaryV1;
import com.aidigital.aionboarding.api.v1.model.MaterialFileV1;
import com.aidigital.aionboarding.api.v1.model.MaterialLinkAssetV1;
import com.aidigital.aionboarding.api.v1.model.MaterialLinkPreviewV1;
import com.aidigital.aionboarding.api.v1.model.MaterialResponseV1;
import com.aidigital.aionboarding.api.v1.model.MaterialSummaryV1;
import com.aidigital.aionboarding.api.v1.model.MaterialV1;
import com.aidigital.aionboarding.api.v1.model.MaterialYoutubeVideoV1;
import com.aidigital.aionboarding.api.v1.model.MaterialsListResponseV1;
import com.aidigital.aionboarding.api.v1.model.SearchMaterialsV1;
import com.aidigital.aionboarding.api.v1.model.UpdateMaterialRequestV1;
import com.aidigital.aionboarding.api.v1.model.UploadedFileResponseV1;
import com.aidigital.aionboarding.config.ApplicationMapperConfig;
import com.aidigital.aionboarding.mappers.common.MaterialAssetKindApiMapper;
import com.aidigital.aionboarding.mappers.common.PageInfoApiMapper;
import com.aidigital.aionboarding.mappers.common.SortDirectionApiMapper;
import com.aidigital.aionboarding.service.material.models.CreateMaterialInput;
import com.aidigital.aionboarding.service.material.models.MaterialAttachmentInput;
import com.aidigital.aionboarding.service.material.models.MaterialFileRecord;
import com.aidigital.aionboarding.service.material.models.MaterialFileSummaryRecord;
import com.aidigital.aionboarding.service.material.models.MaterialListQuery;
import com.aidigital.aionboarding.service.material.models.MaterialLinkAssetRecord;
import com.aidigital.aionboarding.service.material.models.MaterialLinkSummaryRecord;
import com.aidigital.aionboarding.service.material.models.MaterialRecord;
import com.aidigital.aionboarding.service.material.models.MaterialSearchSummaryRecord;
import com.aidigital.aionboarding.service.material.models.MaterialSortField;
import com.aidigital.aionboarding.service.material.models.MaterialYoutubeVideoRecord;
import com.aidigital.aionboarding.service.material.models.UpdateMaterialInput;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;

@Mapper(
    config = ApplicationMapperConfig.class,
    uses = {
        MaterialAssetKindApiMapper.class,
        MaterialSortFieldApiMapper.class,
        SortDirectionApiMapper.class
    }
)
public interface MaterialApiMapper extends PageInfoApiMapper {

    MaterialYoutubeVideoV1 toMaterialYoutubeVideoV1(MaterialYoutubeVideoRecord video);

    MaterialLinkAssetV1 toMaterialLinkAssetV1(MaterialLinkAssetRecord link);

    @Mapping(target = "kind", source = "kind")
    MaterialFileV1 toMaterialFileV1(MaterialFileRecord file);

    MaterialV1 toMaterialV1(MaterialRecord material);

    @Mapping(target = "material", source = ".")
    MaterialResponseV1 toMaterialResponseV1(MaterialRecord material);

    MaterialLinkPreviewV1 toMaterialLinkPreviewV1(MaterialLinkSummaryRecord link);

    @Mapping(target = "kind", source = "kind")
    MaterialFileSummaryV1 toMaterialFileSummaryV1(MaterialFileSummaryRecord file);

    MaterialSummaryV1 toMaterialSummaryV1(MaterialSearchSummaryRecord material);

    default MaterialsListResponseV1 toMaterialsListResponseV1(Page<MaterialSearchSummaryRecord> materials) {
        MaterialsListResponseV1 response = new MaterialsListResponseV1();
        response.setMaterials(materials.getContent().stream().map(this::toMaterialSummaryV1).toList());
        response.setPage(toPageInfoV1(materials));
        return response;
    }

    default MaterialListQuery toMaterialListQuery(SearchMaterialsV1 request) {
        return new MaterialListQuery(
            request == null ? null : request.getQuery(),
            request == null ? null : request.getTags(),
            request == null ? null : request.getCreatedByUserId(),
            request == null ? null : request.getHasAttachments(),
            request == null ? null : request.getHasYoutube(),
            request == null ? null : request.getHasLinks(),
            materialSortField(request),
            sortDirection(request)
        );
    }

    default int page(SearchMaterialsV1 request) {
        return request == null || request.getPage() == null ? 0 : request.getPage();
    }

    default int size(SearchMaterialsV1 request) {
        return request == null || request.getSize() == null ? 20 : request.getSize();
    }

    default MaterialSortField materialSortField(SearchMaterialsV1 request) {
        if (request == null || request.getSort() == null) {
            return MaterialSortField.CREATED_AT;
        }
        return switch (request.getSort()) {
            case CREATED_AT -> MaterialSortField.CREATED_AT;
            case UPDATED_AT -> MaterialSortField.UPDATED_AT;
            case TITLE -> MaterialSortField.TITLE;
            case USAGE_COUNT -> MaterialSortField.USAGE_COUNT;
        };
    }

    default Sort.Direction sortDirection(SearchMaterialsV1 request) {
        return request != null && request.getDirection() == com.aidigital.aionboarding.api.v1.model.SortDirectionV1.ASC
            ? Sort.Direction.ASC
            : Sort.Direction.DESC;
    }

    @Mapping(target = "ok", constant = "true")
    @Mapping(target = "material", source = ".")
    CreateMaterialResponseV1 toCreateMaterialResponseV1(MaterialRecord material);

    UploadedFileResponseV1 toUploadedFileResponseV1(
        String storageKey,
        String originalName,
        String mimeType,
        long sizeBytes
    );

    CreateMaterialInput toCreateMaterialInput(CreateMaterialRequestV1 request);

    UpdateMaterialInput toUpdateMaterialInput(UpdateMaterialRequestV1 request);

    @Mapping(target = "kind", source = "kind")
    MaterialAttachmentInput toMaterialAttachmentInput(MaterialAttachmentInputV1 attachment);
}
