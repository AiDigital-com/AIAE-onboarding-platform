package com.aidigital.aionboarding.service.teachervideo.support;

import com.aidigital.aionboarding.domain.lesson.entities.Lesson;
import com.aidigital.aionboarding.external.heygen.HeyGenClient;
import com.aidigital.aionboarding.external.heygen.HeyGenExternalException;
import com.aidigital.aionboarding.external.heygen.model.HeyGenTeacherVideoResult;
import com.aidigital.aionboarding.service.common.error.AppException;
import com.aidigital.aionboarding.service.common.error.ErrorReason;
import com.aidigital.aionboarding.service.common.time.CurrentTime;
import com.aidigital.aionboarding.service.lesson.models.TeacherVideoRecord;
import com.aidigital.aionboarding.service.lesson.models.TeacherVideoResultRecord;
import com.aidigital.aionboarding.service.lesson.services.entity.LessonEntityService;
import com.aidigital.aionboarding.service.lesson.support.LessonRecordAssembler;
import com.aidigital.aionboarding.service.material.services.MaterialPreparationService;
import com.aidigital.aionboarding.service.teachervideo.prompt.TeacherVideoPromptBuilder;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Runs the teacher-video creation workflow: rejects a duplicate in-flight request, prepares the
 * lesson context for the prompt, calls HeyGen, and persists the outcome.
 */
@Component
@RequiredArgsConstructor
public class TeacherVideoCreationWorkflow {

    private final LessonEntityService lessonEntityService;
    private final MaterialPreparationService materialPreparationService;
    private final LessonRecordAssembler lessonMapper;
    private final HeyGenClient heyGenClient;
    private final TeacherVideoPromptBuilder teacherVideoPromptBuilder;
    private final TeacherVideoMetadataSupport teacherVideoMetadataSupport;
    private final CurrentTime currentTime;

    /**
     * Creates a teacher video for a lesson already verified ready/manageable/non-empty.
     *
     * @param lesson   the lesson to create a teacher video for
     * @param lessonId lesson primary key, used for material preparation
     * @return the creation result record
     * @throws AppException C006 if a teacher video is already being generated for this lesson
     * @throws AppException C003 if the HeyGen call fails
     */
    public TeacherVideoResultRecord create(Lesson lesson, Long lessonId) {
        Map<String, Object> metadata = teacherVideoMetadataSupport.mutableMetadata(lesson);
        rejectIfActiveGeneration(metadata);
        Map<String, Object> lessonMap = prepareLessonForPrompt(lesson, lessonId, metadata);
        return requestTeacherVideo(lesson, metadata, lessonMap);
    }

    /**
     * Calls HeyGen to create the teacher video and persists the result to the lesson metadata.
     *
     * @param lesson    the lesson to associate the video with
     * @param metadata  mutable copy of the lesson's current generation metadata
     * @param lessonMap full lesson context map passed to the prompt builder
     * @return the creation result record
     * @throws AppException C003 if the HeyGen call fails
     */
    TeacherVideoResultRecord requestTeacherVideo(
            Lesson lesson,
            Map<String, Object> metadata,
            Map<String, Object> lessonMap
    ) {
        String prompt = teacherVideoPromptBuilder.buildTeacherVideoPrompt(lessonMap);
        try {
            HeyGenTeacherVideoResult result = heyGenClient.createTeacherVideo(prompt);
            String checkedAt = currentTime.instantString();
            TeacherVideoRecord teacherVideo = new TeacherVideoRecord(
                    "heygen",
                    prompt,
                    result.avatarId(),
                    result.voiceId(),
                    result.sessionId(),
                    result.videoId(),
                    result.status(),
                    currentTime.instantString(),
                    teacherVideoPromptBuilder.durationLimitSeconds(),
                    checkedAt,
                    "",
                    "",
                    null,
                    null,
                    null
            );

            metadata.put("teacherVideo", lessonMapper.toTeacherVideoMap(teacherVideo));
            lesson.setGenerationMetadata(metadata);
            lesson.setUpdatedAt(currentTime.utcDateTime());
            Lesson saved = lessonEntityService.save(lesson);

            return new TeacherVideoResultRecord(
                    lessonMapper.normalizeTeacherVideoRecord(teacherVideo, checkedAt),
                    lessonMapper.toDetailRecord(saved)
            );
        } catch (HeyGenExternalException ex) {
            throw new AppException(ErrorReason.C003, ex.getMessage());
        }
    }

    /**
     * Prepares the lesson context map for the teacher video prompt, injecting prepared materials.
     *
     * @param lesson   the lesson entity
     * @param lessonId lesson primary key used for material preparation
     * @param metadata mutable metadata map to update with prepared materials
     * @return the enriched lesson context map
     */
    Map<String, Object> prepareLessonForPrompt(Lesson lesson, Long lessonId, Map<String, Object> metadata) {
        Map<String, Object> lessonMap = lessonMapper.toDetailMap(lesson);
        Map<String, Object> preparedMaterials = materialPreparationService.prepareForLesson(lessonId).toLegacyMap();
        metadata.put("preparedMaterials", preparedMaterials);
        lessonMap.put("generationMetadata", metadata);
        return lessonMap;
    }

    /**
     * Guards against starting a new teacher video while one is already active.
     *
     * @param metadata the lesson's current generation metadata
     * @throws AppException C006 if a video is already being generated
     */
    void rejectIfActiveGeneration(Map<String, Object> metadata) {
        TeacherVideoRecord existingTeacherVideo = lessonMapper.toTeacherVideoRecord(castMap(metadata.get("teacherVideo"
        )));
        if (teacherVideoMetadataSupport.hasActiveTeacherVideo(existingTeacherVideo)) {
            throw new AppException(ErrorReason.C006, "A teacher video is already being generated for this lesson.");
        }
    }

    /**
     * Safely casts a raw object to a {@code Map<String, Object>}, returning an empty map for non-maps.
     *
     * @param value raw object
     * @return cast map, or an empty {@link LinkedHashMap}
     */
    @SuppressWarnings("unchecked")
    Map<String, Object> castMap(Object value) {
        if (value instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return new LinkedHashMap<>();
    }
}
