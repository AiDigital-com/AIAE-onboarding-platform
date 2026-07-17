package com.aidigital.aionboarding.service.lesson.models;

import java.util.List;

public record RevisionHistoryEntryRecord(
    String revisedAt,
    String revisionRequest,
    List<String> selectedOptions,
    RevisionBriefRecord revisionBrief,
    LessonRevisionPromptRecord plannerPrompt,
    LessonRevisionPromptRecord writerPrompt,
    RevisionProviderMetadataRecord planner,
    RevisionProviderMetadataRecord writer
) { }
