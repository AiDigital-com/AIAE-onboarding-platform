package com.aidigital.aionboarding.service.lesson.models;

import com.aidigital.aionboarding.service.lesson.enums.LessonCreationModeV1;
import java.util.List;

public record CreateLessonInput(
    String title,
    String instructions,
    String depth,
    String tone,
    String desiredFormat,
    List<Long> materialIds,
    List<String> tags,
    String description,
    String contentHtml,
    LessonCreationModeV1 mode
) { }
