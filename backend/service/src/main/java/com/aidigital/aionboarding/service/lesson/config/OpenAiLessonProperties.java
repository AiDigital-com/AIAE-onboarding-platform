package com.aidigital.aionboarding.service.lesson.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.lesson.openai")
public class OpenAiLessonProperties {

    private String revisionPlannerModel = "gpt-4o-mini";
    private String lessonAssistantModel = "gpt-4o-mini";
    private String miniModel = "gpt-4o-mini";
    private String nanoModel = "gpt-4o-mini";

    /**
     * Returns the revision planner model, falling back to the mini model when unset.
     *
     * @return the resolved model identifier
     */
    public String resolveRevisionPlannerModel() {
        if (revisionPlannerModel != null && !revisionPlannerModel.isBlank()) {
            return revisionPlannerModel;
        }
        return miniModel;
    }

    /**
     * Returns the lesson-assistant model, falling back to the nano then mini model when unset.
     *
     * @return the resolved model identifier
     */
    public String resolveLessonAssistantModel() {
        if (lessonAssistantModel != null && !lessonAssistantModel.isBlank()) {
            return lessonAssistantModel;
        }
        if (nanoModel != null && !nanoModel.isBlank()) {
            return nanoModel;
        }
        return miniModel;
    }
}
