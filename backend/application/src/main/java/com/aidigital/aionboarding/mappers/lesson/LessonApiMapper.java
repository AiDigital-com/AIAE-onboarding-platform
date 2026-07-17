package com.aidigital.aionboarding.mappers.lesson;

import com.aidigital.aionboarding.api.v1.model.AddLessonAssetRequestV1;
import com.aidigital.aionboarding.api.v1.model.AskLessonResponseV1;
import com.aidigital.aionboarding.api.v1.model.ChatMessageV1;
import com.aidigital.aionboarding.api.v1.model.LessonAssistantConversationResponseV1;
import com.aidigital.aionboarding.api.v1.model.LessonRoadmapContextV1;
import com.aidigital.aionboarding.api.v1.model.CreateLessonRequestV1;
import com.aidigital.aionboarding.api.v1.model.LessonAssetResponseV1;
import com.aidigital.aionboarding.api.v1.model.LessonAssetV1;
import com.aidigital.aionboarding.api.v1.model.LessonContentFormatV1;
import com.aidigital.aionboarding.api.v1.model.LessonDetailResponseV1;
import com.aidigital.aionboarding.api.v1.model.LessonGenerationStatusResponseV1;
import com.aidigital.aionboarding.api.v1.model.LessonResponseV1;
import com.aidigital.aionboarding.api.v1.model.LessonSummaryV1;
import com.aidigital.aionboarding.api.v1.model.LessonV1;
import com.aidigital.aionboarding.api.v1.model.LessonsListResponseV1;
import com.aidigital.aionboarding.api.v1.model.RevisionBriefV1;
import com.aidigital.aionboarding.api.v1.model.RevisionHistoryItemV1;
import com.aidigital.aionboarding.api.v1.model.ReviseLessonRequestV1;
import com.aidigital.aionboarding.api.v1.model.SearchLessonsV1;
import com.aidigital.aionboarding.api.v1.model.TeacherVideoResponseV1;
import com.aidigital.aionboarding.api.v1.model.TeacherVideoV1;
import com.aidigital.aionboarding.api.v1.model.UpdateLessonContentRequestV1;
import com.aidigital.aionboarding.api.v1.model.UploadedFileResponseV1;
import com.aidigital.aionboarding.config.ApplicationMapperConfig;
import com.aidigital.aionboarding.mappers.common.ChatRoleApiMapper;
import com.aidigital.aionboarding.mappers.common.LessonAssistantPresetApiMapper;
import com.aidigital.aionboarding.mappers.common.LessonContentFormatApiMapper;
import com.aidigital.aionboarding.mappers.common.LessonGenerationStatusApiMapper;
import com.aidigital.aionboarding.mappers.common.LessonRevisionKindApiMapper;
import com.aidigital.aionboarding.mappers.common.LessonVisibilityApiMapper;
import com.aidigital.aionboarding.mappers.common.MaterialInputTypeApiMapper;
import com.aidigital.aionboarding.mappers.common.MaterialTypeApiMapper;
import com.aidigital.aionboarding.mappers.common.PageInfoApiMapper;
import com.aidigital.aionboarding.mappers.common.TeacherVideoProviderApiMapper;
import com.aidigital.aionboarding.mappers.learning.LessonEnrollmentApiMapper;
import com.aidigital.aionboarding.mappers.lessonactivity.LessonActivityApiMapper;
import com.aidigital.aionboarding.mappers.material.MaterialApiMapper;
import com.aidigital.aionboarding.service.lesson.enums.LessonCreationModeV1;
import com.aidigital.aionboarding.service.lesson.models.AskLessonResultRecord;
import com.aidigital.aionboarding.service.lesson.models.ChatTurn;
import com.aidigital.aionboarding.service.lesson.models.CreateLessonAssetInput;
import com.aidigital.aionboarding.service.lesson.models.LessonAssistantConversationRecord;
import com.aidigital.aionboarding.service.lesson.models.CreateLessonInput;
import com.aidigital.aionboarding.service.lesson.models.LessonAssetDeleteResultRecord;
import com.aidigital.aionboarding.service.lesson.models.LessonAssetRecord;
import com.aidigital.aionboarding.service.lesson.models.LessonAssetResultRecord;
import com.aidigital.aionboarding.service.lesson.models.LessonDetailRecord;
import com.aidigital.aionboarding.service.lesson.models.LessonDetailResultRecord;
import com.aidigital.aionboarding.service.lesson.models.LessonListQuery;
import com.aidigital.aionboarding.service.lesson.models.LessonRoadmapContextRecord;
import com.aidigital.aionboarding.service.lesson.models.LessonSearchSummaryRecord;
import com.aidigital.aionboarding.service.lesson.models.LessonSummaryRecord;
import com.aidigital.aionboarding.service.lesson.models.LessonSortField;
import com.aidigital.aionboarding.service.lesson.models.RevisionBriefRecord;
import com.aidigital.aionboarding.service.lesson.models.RevisionHistoryItemRecord;
import com.aidigital.aionboarding.service.lesson.models.RevisionResultRecord;
import com.aidigital.aionboarding.service.lesson.models.ReviseLessonInput;
import com.aidigital.aionboarding.service.lesson.models.TeacherVideoRecord;
import com.aidigital.aionboarding.service.lesson.models.TeacherVideoResultRecord;
import com.aidigital.aionboarding.service.lesson.models.UpdateLessonContentInput;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;

@Mapper(
    config = ApplicationMapperConfig.class,
    uses = {
        MaterialTypeApiMapper.class,
        MaterialInputTypeApiMapper.class,
        LessonGenerationStatusApiMapper.class,
        LessonVisibilityApiMapper.class,
        LessonContentFormatApiMapper.class,
        LessonRevisionKindApiMapper.class,
        TeacherVideoProviderApiMapper.class,
        LessonActivityApiMapper.class,
        LessonEnrollmentApiMapper.class,
        MaterialApiMapper.class,
        ChatRoleApiMapper.class,
        LessonAssistantPresetApiMapper.class
    }
)
public interface LessonApiMapper extends PageInfoApiMapper {

    @Mapping(target = "kind", source = "kind")
    LessonAssetV1 toLessonAssetV1(LessonAssetRecord asset);

    ChatMessageV1 toChatMessageV1(ChatTurn turn);

    LessonAssistantConversationResponseV1 toLessonAssistantConversationResponseV1(LessonAssistantConversationRecord conversation);

    @Mapping(target = "status", source = "status")
    @Mapping(target = "publicationStatus", source = "publicationStatus")
    @Mapping(target = "isEnrolled", ignore = true)
    LessonSummaryV1 toLessonSummaryV1(LessonSearchSummaryRecord lesson);

    @Mapping(target = "status", source = "statusCode")
    LessonGenerationStatusResponseV1 toLessonGenerationStatusResponseV1(String statusCode);

    @Mapping(target = "revisedAt", source = "revisedAt")
    @Mapping(target = "revisionBrief.changeScope", source = "revisionBrief.changeScope")
    RevisionHistoryItemV1 toRevisionHistoryItemV1(RevisionHistoryItemRecord item);

    @Mapping(target = "changeScope", source = "changeScope")
    RevisionBriefV1 toRevisionBriefV1(RevisionBriefRecord brief);

    @Mapping(target = "status", source = "status")
    @Mapping(target = "publicationStatus", source = "publicationStatus")
    @Mapping(target = "contentFormat", source = "contentFormat")
    @Mapping(target = "revisionHistory", source = "revisionHistory")
    @Mapping(target = "coverImageStorageKey", source = "coverImageStorageKey")
    @Mapping(target = "coverImageOriginalName", source = "coverImageOriginalName")
    @Mapping(target = "coverImageMimeType", source = "coverImageMimeType")
    @Mapping(target = "viewerCanAccessPrivate", constant = "false")
    LessonV1 toLessonV1(LessonDetailRecord lesson);

    @Mapping(target = "lesson", source = ".")
    @Mapping(target = "revisionBrief", ignore = true)
    LessonResponseV1 toLessonResponseV1(LessonSummaryRecord lesson);

    @Mapping(target = "lesson", source = ".")
    @Mapping(target = "revisionBrief", ignore = true)
    LessonResponseV1 toLessonResponseV1(LessonDetailRecord lesson);

    @Mapping(target = "lesson", source = "lesson")
    @Mapping(target = "revisionBrief", source = "revisionBrief")
    LessonResponseV1 toLessonResponseV1(RevisionResultRecord result);

    @Mapping(target = "lesson", source = "lesson")
    @Mapping(target = "activities", source = "activities")
    @Mapping(target = "enrollment", source = "enrollment")
    @Mapping(target = "roadmapContext", source = "lesson.roadmapContext")
    LessonDetailResponseV1 toLessonDetailResponseV1(LessonDetailResultRecord result);

    LessonRoadmapContextV1 toRoadmapContextV1(LessonRoadmapContextRecord record);

    default LessonsListResponseV1 toLessonsListResponseV1(Page<LessonSearchSummaryRecord> lessons) {
        LessonsListResponseV1 response = new LessonsListResponseV1();
        response.setLessons(lessons.getContent().stream().map(this::toLessonSummaryV1).toList());
        response.setPage(toPageInfoV1(lessons));
        return response;
    }

    default LessonListQuery toLessonListQuery(SearchLessonsV1 request) {
        return new LessonListQuery(
            request == null ? null : request.getQuery(),
            request == null ? null : request.getTags(),
            request == null || request.getStatus() == null ? null : request.getStatus().getValue(),
            request == null || request.getPublicationStatus() == null ? null : request.getPublicationStatus().getValue(),
            request == null ? null : request.getCreatedByUserId(),
            request == null ? null : request.getAssignedToMe(),
            request == null ? null : request.getReadyOnly(),
            request == null || request.getActivityType() == null ? null : request.getActivityType().getValue(),
            request == null ? null : request.getHasActivities(),
            lessonSortField(request),
            sortDirection(request)
        );
    }

    default int page(SearchLessonsV1 request) {
        return request == null || request.getPage() == null ? 0 : request.getPage();
    }

    default int size(SearchLessonsV1 request) {
        return request == null || request.getSize() == null ? 20 : request.getSize();
    }

    default LessonSortField lessonSortField(SearchLessonsV1 request) {
        if (request == null || request.getSort() == null) {
            return LessonSortField.CREATED_AT;
        }
        return switch (request.getSort()) {
            case CREATED_AT -> LessonSortField.CREATED_AT;
            case UPDATED_AT -> LessonSortField.UPDATED_AT;
            case TITLE -> LessonSortField.TITLE;
        };
    }

    default Sort.Direction sortDirection(SearchLessonsV1 request) {
        return request != null && request.getDirection() == com.aidigital.aionboarding.api.v1.model.SortDirectionV1.ASC
            ? Sort.Direction.ASC
            : Sort.Direction.DESC;
    }

    @Mapping(target = "mode", expression = "java(toLessonCreationMode(request.getAction()))")
    CreateLessonInput toCreateLessonInput(CreateLessonRequestV1 request);

    @Mapping(target = "title", expression = "java(optionalString(request.getTitle()))")
    @Mapping(target = "contentMarkdown", expression = "java(optionalString(request.getContentMarkdown()))")
    @Mapping(target = "contentHtml", expression = "java(optionalString(request.getContentHtml()))")
    @Mapping(target = "tags", expression = "java(java.util.Optional.ofNullable(request.getTags()))")
    @Mapping(target = "coverImageStorageKey", expression = "java(optionalString(request.getCoverImageStorageKey()))")
    @Mapping(target = "coverImageOriginalName", expression = "java(optionalString(request.getCoverImageOriginalName()))")
    @Mapping(target = "coverImageMimeType", expression = "java(optionalString(request.getCoverImageMimeType()))")
    UpdateLessonContentInput toUpdateLessonContentInput(UpdateLessonContentRequestV1 request);

    ReviseLessonInput toReviseLessonInput(ReviseLessonRequestV1 request);

    @Mapping(target = "kind", source = "kind")
    CreateLessonAssetInput toCreateLessonAssetInput(AddLessonAssetRequestV1 request);

    AskLessonResponseV1 toAskLessonResponseV1(AskLessonResultRecord result);

    @Mapping(target = "asset", source = "asset")
    @Mapping(target = "lesson", source = "lesson")
    LessonAssetResponseV1 toLessonAssetResponseV1(LessonAssetResultRecord result);

    @Mapping(target = "lesson", source = "lesson")
    @Mapping(target = "revisionBrief", ignore = true)
    LessonResponseV1 toLessonResponseV1(LessonAssetDeleteResultRecord result);

    @Mapping(target = "provider", source = "provider")
    @Mapping(target = "createdAt", source = "createdAt")
    @Mapping(target = "checkedAt", source = "checkedAt")
    @Mapping(target = "completedAt", source = "completedAt")
    @Mapping(target = "failedAt", source = "failedAt")
    @Mapping(target = "duration", source = "duration")
    TeacherVideoV1 toTeacherVideoV1(TeacherVideoRecord teacherVideo);

    @Mapping(target = "teacherVideo", source = "teacherVideo")
    @Mapping(target = "lesson", source = "lesson")
    TeacherVideoResponseV1 toTeacherVideoResponseV1(TeacherVideoResultRecord result);

    UploadedFileResponseV1 toUploadedFileResponseV1(String storageKey, String originalName, String mimeType, long sizeBytes);

    @Mapping(target = "status", source = "status")
    @Mapping(target = "publicationStatus", source = "publicationStatus")
    @Mapping(target = "isPublished", expression = "java(\"published\".equals(lesson.publicationStatus()))")
    @Mapping(target = "isArchived", expression = "java(\"archived\".equals(lesson.publicationStatus()))")
    @Mapping(target = "tags", source = "tags")
    @Mapping(target = "generationMetadata", expression = "java(new java.util.LinkedHashMap<>())")
    @Mapping(target = "revisionHistory", expression = "java(java.util.List.of())")
    @Mapping(target = "materialIds", expression = "java(java.util.List.of())")
    @Mapping(target = "sourceReferences", expression = "java(java.util.List.of())")
    @Mapping(target = "lessonAssets", expression = "java(java.util.List.of())")
    @Mapping(target = "description", ignore = true)
    @Mapping(target = "userInstructions", ignore = true)
    @Mapping(target = "depth", ignore = true)
    @Mapping(target = "tone", ignore = true)
    @Mapping(target = "desiredFormat", ignore = true)
    @Mapping(target = "contentFormat", expression = "java(com.aidigital.aionboarding.api.v1.model.LessonContentFormatV1.MARKDOWN)")
    @Mapping(target = "coverImageStorageKey", source = "coverImageStorageKey")
    @Mapping(target = "coverImageOriginalName", source = "coverImageOriginalName")
    @Mapping(target = "coverImageMimeType", source = "coverImageMimeType")
    @Mapping(target = "publishedAt", ignore = true)
    @Mapping(target = "createdByUserId", ignore = true)
    @Mapping(target = "viewerCanAccessPrivate", constant = "false")
    @Mapping(target = "errorMessage", constant = "")
    LessonV1 toLessonV1(LessonSummaryRecord lesson);

    default Optional<String> optionalString(String value) {
        return Optional.ofNullable(value);
    }

    /**
     * Maps the generated OpenAPI action enum to the service-layer creation mode.
     *
     * @param action the API request action enum
     * @return the service creation mode, or {@code null} if the action is unset
     */
    default LessonCreationModeV1 toLessonCreationMode(com.aidigital.aionboarding.api.v1.model.LessonCreationModeV1 action) {
        if (action == null) {
            return null;
        }
        return switch (action) {
            case GENERATE -> LessonCreationModeV1.GENERATE;
            case CREATE_MANUAL -> LessonCreationModeV1.CREATE_MANUAL;
        };
    }

    default LocalDateTime parseDateTime(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(raw);
        } catch (DateTimeParseException ignored) {
            try {
                return OffsetDateTime.parse(raw).toLocalDateTime();
            } catch (DateTimeParseException ignoredAgain) {
                try {
                    return Instant.parse(raw).atOffset(ZoneOffset.UTC).toLocalDateTime();
                } catch (DateTimeParseException ignoredOnceMore) {
                    return null;
                }
            }
        }
    }

    default BigDecimal toBigDecimal(Object raw) {
        if (raw == null) {
            return null;
        }
        if (raw instanceof BigDecimal decimal) {
            return decimal;
        }
        if (raw instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        try {
            return new BigDecimal(String.valueOf(raw));
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
