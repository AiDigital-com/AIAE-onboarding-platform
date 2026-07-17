package com.aidigital.aionboarding.mappers.roadmap;

import com.aidigital.aionboarding.api.v1.model.CreateRoadmapRequestV1;
import com.aidigital.aionboarding.api.v1.model.RoadmapLessonV1;
import com.aidigital.aionboarding.api.v1.model.RoadmapResponseV1;
import com.aidigital.aionboarding.api.v1.model.RoadmapV1;
import com.aidigital.aionboarding.api.v1.model.RoadmapsListResponseV1;
import com.aidigital.aionboarding.api.v1.model.SearchRoadmapsV1;
import com.aidigital.aionboarding.api.v1.model.UpdateRoadmapRequestV1;
import com.aidigital.aionboarding.config.ApplicationMapperConfig;
import com.aidigital.aionboarding.mappers.common.LessonGenerationStatusApiMapper;
import com.aidigital.aionboarding.mappers.common.PageInfoApiMapper;
import com.aidigital.aionboarding.mappers.common.SortDirectionApiMapper;
import com.aidigital.aionboarding.service.roadmap.models.CreateRoadmapInput;
import com.aidigital.aionboarding.service.roadmap.models.RoadmapLessonRecord;
import com.aidigital.aionboarding.service.roadmap.models.RoadmapListQuery;
import com.aidigital.aionboarding.service.roadmap.models.RoadmapRecord;
import com.aidigital.aionboarding.service.roadmap.models.RoadmapSortField;
import com.aidigital.aionboarding.service.roadmap.models.UpdateRoadmapInput;
import java.util.List;
import java.util.Optional;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;

@Mapper(
    config = ApplicationMapperConfig.class,
    uses = {
        LessonGenerationStatusApiMapper.class,
        RoadmapSortFieldApiMapper.class,
        SortDirectionApiMapper.class
    }
)
public interface RoadmapApiMapper extends PageInfoApiMapper {

    RoadmapV1 toRoadmapV1(RoadmapRecord roadmap);

    RoadmapLessonV1 toRoadmapLessonV1(RoadmapLessonRecord lesson);

    @Mapping(target = "roadmap", source = ".")
    RoadmapResponseV1 toRoadmapResponseV1(RoadmapRecord roadmap);

    default RoadmapsListResponseV1 toRoadmapsListResponseV1(Page<RoadmapRecord> roadmaps) {
        RoadmapsListResponseV1 response = new RoadmapsListResponseV1();
        response.setRoadmaps(roadmaps.getContent().stream().map(this::toRoadmapV1).toList());
        response.setPage(toPageInfoV1(roadmaps));
        return response;
    }

    default RoadmapListQuery toRoadmapListQuery(SearchRoadmapsV1 request) {
        return new RoadmapListQuery(
            request == null ? null : request.getQuery(),
            request == null ? null : request.getTags(),
            request == null ? null : request.getCreatedByUserId(),
            request == null ? null : request.getAssignedToMe(),
            roadmapSortField(request),
            sortDirection(request)
        );
    }

    default int page(SearchRoadmapsV1 request) {
        return request == null || request.getPage() == null ? 0 : request.getPage();
    }

    default int size(SearchRoadmapsV1 request) {
        return request == null || request.getSize() == null ? 20 : request.getSize();
    }

    default RoadmapSortField roadmapSortField(SearchRoadmapsV1 request) {
        if (request == null || request.getSort() == null) {
            return RoadmapSortField.CREATED_AT;
        }
        return switch (request.getSort()) {
            case CREATED_AT -> RoadmapSortField.CREATED_AT;
            case UPDATED_AT -> RoadmapSortField.UPDATED_AT;
            case TITLE -> RoadmapSortField.TITLE;
        };
    }

    default Sort.Direction sortDirection(SearchRoadmapsV1 request) {
        return request != null && request.getDirection() == com.aidigital.aionboarding.api.v1.model.SortDirectionV1.ASC
            ? Sort.Direction.ASC
            : Sort.Direction.DESC;
    }

    CreateRoadmapInput toCreateRoadmapInput(CreateRoadmapRequestV1 request);

    @Mapping(target = "title", expression = "java(optionalString(request.getTitle()))")
    @Mapping(target = "description", expression = "java(optionalString(request.getDescription()))")
    @Mapping(target = "lessonIds", expression = "java(optionalList(request.getLessonIds()))")
    @Mapping(target = "tags", expression = "java(optionalList(request.getTags()))")
    UpdateRoadmapInput toUpdateRoadmapInput(UpdateRoadmapRequestV1 request);

    default Optional<String> optionalString(String value) {
        return Optional.ofNullable(value);
    }

    default <T> Optional<List<T>> optionalList(List<T> value) {
        return Optional.ofNullable(value);
    }
}
