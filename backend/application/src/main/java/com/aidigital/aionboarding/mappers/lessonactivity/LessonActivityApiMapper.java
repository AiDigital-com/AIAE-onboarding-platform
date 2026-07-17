package com.aidigital.aionboarding.mappers.lessonactivity;

import com.aidigital.aionboarding.api.v1.model.ActivityAttemptV1;
import com.aidigital.aionboarding.api.v1.model.ActivityProgressDetailV1;
import com.aidigital.aionboarding.api.v1.model.ActivityProgressResponseV1;
import com.aidigital.aionboarding.api.v1.model.ActivityProgressV1;
import com.aidigital.aionboarding.api.v1.model.ActivityPromptV1;
import com.aidigital.aionboarding.api.v1.model.ActivityResponseV1;
import com.aidigital.aionboarding.api.v1.model.LessonActivitiesResponseV1;
import com.aidigital.aionboarding.api.v1.model.LessonActivityDetailResponseV1;
import com.aidigital.aionboarding.api.v1.model.LessonActivityLessonV1;
import com.aidigital.aionboarding.api.v1.model.LessonActivityV1;
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
import com.aidigital.aionboarding.service.lessonactivity.models.SubmitActivityProgressResultRecord;
import com.aidigital.aionboarding.service.lessonactivity.models.UpdateActivityResultRecord;
import com.aidigital.aionboarding.service.lessonactivity.models.UpdateActivityInput;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

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

    default LessonActivitiesResponseV1 toLessonActivitiesResponseV1(java.util.List<LessonActivityRecord> activities) {
        LessonActivitiesResponseV1 response = new LessonActivitiesResponseV1();
        response.setActivities(activities == null ? java.util.List.of() : activities.stream().map(this::toLessonActivityV1).toList());
        return response;
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
}
