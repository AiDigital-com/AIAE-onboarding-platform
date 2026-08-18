package com.aidigital.aionboarding.mappers.lessonactivity;

import com.aidigital.aionboarding.api.v1.model.ActivityAttemptV1;
import com.aidigital.aionboarding.api.v1.model.ActivityProgressDetailV1;
import com.aidigital.aionboarding.api.v1.model.ActivityProgressResponseV1;
import com.aidigital.aionboarding.api.v1.model.ActivityProgressV1;
import com.aidigital.aionboarding.api.v1.model.ActivityPromptV1;
import com.aidigital.aionboarding.api.v1.model.ActivityResponseV1;
import com.aidigital.aionboarding.api.v1.model.GenerateActivityRequestV1;
import com.aidigital.aionboarding.api.v1.model.LessonActivitiesResponseV1;
import com.aidigital.aionboarding.api.v1.model.LessonActivityDetailResponseV1;
import com.aidigital.aionboarding.api.v1.model.LessonActivityLessonV1;
import com.aidigital.aionboarding.api.v1.model.LessonActivityV1;
import com.aidigital.aionboarding.api.v1.model.SubmitActivityProgressRequestV1;
import com.aidigital.aionboarding.api.v1.model.UpdateActivityRequestV1;
import com.aidigital.aionboarding.api.v1.model.QuizAttemptResultItemV1;
import com.aidigital.aionboarding.config.ApplicationMapperConfig;
import com.aidigital.aionboarding.mappers.common.LessonActivityTypeApiMapper;
import com.aidigital.aionboarding.mappers.common.LessonGenerationStatusApiMapper;
import com.aidigital.aionboarding.mappers.common.LessonVisibilityApiMapper;
import com.aidigital.aionboarding.mappers.common.PreparationStatusApiMapper;
import com.aidigital.aionboarding.mappers.common.QuizQuestionTypeApiMapper;
import com.aidigital.aionboarding.service.lessonactivity.models.ActivityAttemptRecord;
import com.aidigital.aionboarding.service.lessonactivity.models.ActivityProgressRecord;
import com.aidigital.aionboarding.service.lessonactivity.models.ActivityProgressViewRecord;
import com.aidigital.aionboarding.service.lessonactivity.models.ActivityPromptRecord;
import com.aidigital.aionboarding.service.lessonactivity.models.GenerateActivityResultRecord;
import com.aidigital.aionboarding.service.lessonactivity.models.LessonActivityRecord;
import com.aidigital.aionboarding.service.lessonactivity.models.LessonActivityWithAttemptsRecord;
import com.aidigital.aionboarding.service.lessonactivity.models.LessonWithActivitiesRecord;
import com.aidigital.aionboarding.service.lessonactivity.models.QuizAnswerResultRecord;
import com.aidigital.aionboarding.service.lessonactivity.models.SubmitActivityProgressInput;
import com.aidigital.aionboarding.service.lessonactivity.models.SubmitActivityProgressResultRecord;
import com.aidigital.aionboarding.service.lessonactivity.models.UpdateActivityResultRecord;
import com.aidigital.aionboarding.service.lessonactivity.models.UpdateActivityInput;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.data.domain.Page;

@Mapper(
    config = ApplicationMapperConfig.class,
    uses = {
        PreparationStatusApiMapper.class,
        LessonActivityTypeApiMapper.class,
        LessonGenerationStatusApiMapper.class,
        LessonVisibilityApiMapper.class,
        QuizQuestionTypeApiMapper.class
    }
)
public interface LessonActivityApiMapper {

    UpdateActivityInput toUpdateActivityInput(UpdateActivityRequestV1 request);

    @Mapping(target = "status", source = "status")
    ActivityProgressV1 toActivityProgressV1(ActivityProgressViewRecord progress);

    @Mapping(target = "type", source = "type")
    LessonActivityV1 toLessonActivityV1(LessonActivityRecord activity);

    ActivityPromptV1 toActivityPromptV1(ActivityPromptRecord prompt);

    @Mapping(target = "status", source = "status")
    @Mapping(target = "publicationStatus", source = "publicationStatus")
    @Mapping(target = "activities", source = "activities")
    LessonActivityLessonV1 toLessonActivityLessonV1(LessonWithActivitiesRecord lesson);

    @Mapping(target = "activity", source = "activity")
    @Mapping(target = "prompt", source = "prompt")
    @Mapping(target = "lesson", ignore = true)
    ActivityResponseV1 toActivityResponseV1(GenerateActivityResultRecord result);

    @Mapping(target = "activity", source = "activity")
    @Mapping(target = "lesson", source = "lesson")
    @Mapping(target = "prompt", ignore = true)
    ActivityResponseV1 toActivityResponseV1(UpdateActivityResultRecord result);

    @Mapping(target = "activity", source = "activity")
    @Mapping(target = "attempts", source = "attempts")
    LessonActivityDetailResponseV1 toLessonActivityDetailResponseV1(LessonActivityWithAttemptsRecord result);

    /**
     * Builds the lesson activities response. Declared over {@link Page} rather than
     * {@link java.util.List} directly: MapStruct cannot generate a bean-mapping method whose
     * sole parameter is a bare {@code java.util} iterable type.
     *
     * @param activities the activities, wrapped in a page
     * @return the lesson activities response
     */
    @Mapping(target = "activities", expression = "java(activities.getContent().stream().map(this::toLessonActivityV1).toList())")
    LessonActivitiesResponseV1 toLessonActivitiesResponseV1(Page<LessonActivityRecord> activities);

    /**
     * Builds the lesson activities response from a plain list.
     *
     * @param activities the activities, or {@code null}
     * @return the lesson activities response
     */
    default LessonActivitiesResponseV1 toLessonActivitiesResponseV1(java.util.List<LessonActivityRecord> activities) {
        return toLessonActivitiesResponseV1(
                new org.springframework.data.domain.PageImpl<>(activities == null ? java.util.List.of() : activities));
    }

    @Mapping(target = "status", source = "status")
    ActivityProgressDetailV1 toActivityProgressDetailV1(ActivityProgressRecord progress);

    QuizAttemptResultItemV1 toQuizAttemptResultItemV1(QuizAnswerResultRecord result);

    @Mapping(target = "type", source = "type")
    @Mapping(target = "submittedAnswers", source = "submittedAnswers")
    ActivityAttemptV1 toActivityAttemptV1(ActivityAttemptRecord attempt);

    default java.util.List<Object> mapSubmittedAnswers(java.util.List<java.util.List<String>> answers) {
        if (answers == null) {
            return java.util.List.of();
        }
        return new java.util.ArrayList<>(answers);
    }

    @Mapping(target = "progress", source = "progress")
    @Mapping(target = "activities", source = "activities")
    @Mapping(target = "enrollment", source = "enrollment")
    @Mapping(target = "attempt", source = "attempt")
    @Mapping(target = "completedRoadmaps", source = "completedRoadmaps")
    ActivityProgressResponseV1 toActivityProgressResponseV1(SubmitActivityProgressResultRecord result);

    /**
     * Resolves an activity-generation request's optional type filter to its wire value.
     *
     * @param request the generate-activity request
     * @return the requested type's wire value, or {@code null} when unset
     */
    default String activityType(GenerateActivityRequestV1 request) {
        return request.getType() == null ? null : request.getType().getValue();
    }

    /**
     * Converts a submit-activity-progress request to the typed service input, replacing the
     * untyped {@code Map<String, Object>} the request body was previously handed through as.
     *
     * @param request the submit-activity-progress request
     * @return the typed service input
     */
    default SubmitActivityProgressInput toSubmitActivityProgressInput(SubmitActivityProgressRequestV1 request) {
        return new SubmitActivityProgressInput(
            request.getType() == null ? null : request.getType().getValue(),
            request.getAnswers(),
            request.getReviewedCards()
        );
    }
}
