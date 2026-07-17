package com.aidigital.aionboarding.service.lesson.support;

import com.aidigital.aionboarding.domain.lesson.entities.Lesson;
import com.aidigital.aionboarding.service.common.security.AppUser;
import com.aidigital.aionboarding.service.lesson.models.LessonDetailRecord;
import com.aidigital.aionboarding.service.lesson.models.LessonRoadmapContextRecord;
import com.aidigital.aionboarding.service.lesson.models.TeacherVideoRecord;
import com.aidigital.aionboarding.service.lesson.services.LessonRoadmapContextService;
import com.aidigital.aionboarding.service.teachervideo.services.TeacherVideoRefreshService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class LessonDetailEnricher {

    private final LessonRecordAssembler lessonMapper;
    private final TeacherVideoRefreshService teacherVideoRefreshService;
    private final LessonRoadmapContextService lessonRoadmapContextService;

    /**
     * Builds a lesson detail record with refreshed teacher-video data and roadmap context.
     *
     * @param viewer authenticated viewer
     * @param lesson source lesson entity
     * @return enriched lesson detail record
     */
    public LessonDetailRecord toEnrichedDetailRecord(AppUser viewer, Lesson lesson) {
        Lesson refreshedLesson = refreshTeacherVideoIfNeeded(lesson);
        LessonDetailRecord detail = lessonMapper.toDetailRecord(refreshedLesson);
        LessonRoadmapContextRecord roadmapCtx = lessonRoadmapContextService.buildContext(viewer, refreshedLesson);
        if (roadmapCtx == null) {
            return detail;
        }
        return withRoadmapContext(detail, roadmapCtx);
    }

    /**
     * Refreshes teacher-video URL metadata when the lesson has a refreshable teacher video.
     *
     * @param lesson source lesson entity
     * @return original or refreshed lesson entity
     */
    Lesson refreshTeacherVideoIfNeeded(Lesson lesson) {
        Map<String, Object> metadata = lesson.getGenerationMetadata();
        if (metadata == null) {
            return lesson;
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> tvMap = (Map<String, Object>) metadata.get("teacherVideo");
        TeacherVideoRecord teacherVideo = lessonMapper.toTeacherVideoRecord(tvMap);
        if (teacherVideo == null || stringVal(teacherVideo.videoId()).isBlank()) {
            return lesson;
        }
        TeacherVideoRefreshService.RefreshResult refreshResult =
            teacherVideoRefreshService.refreshTeacherVideoIfNeeded(lesson, teacherVideo, false);
        return refreshResult.lesson();
    }

    /**
     * Copies a lesson detail record while replacing its roadmap context.
     *
     * @param detail source detail record
     * @param roadmapCtx roadmap context to attach
     * @return detail record with roadmap context
     */
    LessonDetailRecord withRoadmapContext(LessonDetailRecord detail, LessonRoadmapContextRecord roadmapCtx) {
        return new LessonDetailRecord(
            detail.id(), detail.title(), detail.description(), detail.status(),
            detail.publicationStatus(), detail.isPublished(), detail.isArchived(),
            detail.userInstructions(), detail.depth(), detail.tone(), detail.desiredFormat(),
            detail.contentFormat(), detail.contentMarkdown(), detail.contentHtml(),
            detail.coverImageStorageKey(), detail.coverImageOriginalName(), detail.coverImageMimeType(),
            detail.tags(), detail.generationMetadata(), detail.revisionHistory(),
            detail.errorMessage(), detail.createdBy(), detail.createdByUserId(),
            detail.createdAt(), detail.updatedAt(), detail.publishedAt(),
            detail.materialIds(), detail.sourceReferences(), detail.lessonAssets(),
            roadmapCtx
        );
    }

    /**
     * Converts a nullable value to a non-null string.
     *
     * @param value raw value
     * @return string value or empty string
     */
    String stringVal(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
