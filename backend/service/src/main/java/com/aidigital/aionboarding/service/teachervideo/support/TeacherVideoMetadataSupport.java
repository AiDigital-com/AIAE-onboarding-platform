package com.aidigital.aionboarding.service.teachervideo.support;

import com.aidigital.aionboarding.domain.lesson.entities.Lesson;
import com.aidigital.aionboarding.service.common.mapping.TextValueNormalizer;
import com.aidigital.aionboarding.service.lesson.models.TeacherVideoRecord;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Shared teacher-video metadata helpers.
 */
@Component
@RequiredArgsConstructor
public class TeacherVideoMetadataSupport {

    private static final Set<String> ACTIVE_STATUSES = Set.of("pending", "processing", "generating");

    private final TextValueNormalizer textValueNormalizer;

    /**
     * Returns a mutable copy of lesson generation metadata.
     *
     * @param lesson lesson entity
     * @return mutable metadata map
     */
    public Map<String, Object> mutableMetadata(Lesson lesson) {
        return lesson.getGenerationMetadata() == null
            ? new HashMap<>()
            : new HashMap<>(lesson.getGenerationMetadata());
    }

    /**
     * Checks whether a provider status means teacher-video generation is still active.
     *
     * @param status raw provider status
     * @return {@code true} when the status is active
     */
    public boolean isActiveStatus(String status) {
        return ACTIVE_STATUSES.contains(textValueNormalizer.raw(status));
    }

    /**
     * Checks whether a teacher-video record exists and is currently active.
     *
     * @param teacherVideo teacher-video record
     * @return {@code true} when the teacher video is active
     */
    public boolean hasActiveTeacherVideo(TeacherVideoRecord teacherVideo) {
        return teacherVideo != null && isActiveStatus(teacherVideo.status());
    }
}
