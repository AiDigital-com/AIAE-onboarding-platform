package com.aidigital.aionboarding.service.lessongen.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Model overrides for lesson generation and activity generation.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "app.lessongen")
public class LessonGenProperties {

    /** OpenAI model used for activity generation; defaults to {@code gpt-4o-mini}. */
    private String activityModel = "gpt-4o-mini";
    /** OpenAI mini model used for lightweight generation tasks; defaults to {@code gpt-4o-mini}. */
    private String miniModel = "gpt-4o-mini";
    /** OpenAI model used to compress long YouTube transcripts; defaults to {@code gpt-4o-mini}. */
    private String transcriptCompressionModel = "gpt-4o-mini";
    /** OpenAI model used by the revision planner step; defaults to {@code gpt-4o-mini}. */
    private String revisionPlannerModel = "gpt-4o-mini";
    /**
     * OpenAI model for initial lesson generation, overridable via {@code OPENAI_LESSON_MODEL}
     * or {@code OPENAI_MINI_MODEL} env vars.
     */
    private String model = "gpt-4o-mini";
}
