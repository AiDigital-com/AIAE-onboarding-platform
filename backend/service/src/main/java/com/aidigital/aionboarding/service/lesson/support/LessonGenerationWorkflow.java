package com.aidigital.aionboarding.service.lesson.support;

import com.aidigital.aionboarding.domain.lesson.entities.Lesson;
import com.aidigital.aionboarding.external.openai.model.OpenAiFileInput;
import com.aidigital.aionboarding.service.common.error.AppException;
import com.aidigital.aionboarding.service.common.error.ErrorReason;
import com.aidigital.aionboarding.service.common.time.CurrentTime;
import com.aidigital.aionboarding.service.lesson.models.CreateLessonInput;
import com.aidigital.aionboarding.service.lesson.services.entity.LessonEntityService;
import com.aidigital.aionboarding.service.lesson.util.LessonContentUtil;
import com.aidigital.aionboarding.service.lessongen.model.GeneratedContentResult;
import com.aidigital.aionboarding.service.lessongen.model.LessonGenPrompt;
import com.aidigital.aionboarding.service.lessongen.prompt.LessonPromptBuilder;
import com.aidigital.aionboarding.service.lessongen.prompt.LessonPromptConstants;
import com.aidigital.aionboarding.service.lessongen.services.LessonGenService;
import com.aidigital.aionboarding.service.material.models.PreparedMaterialsResult;
import com.aidigital.aionboarding.service.material.services.MaterialOpenAiFilePreparationService;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Prepares OpenAI file inputs, builds the generation prompt, marks the lesson generating, then
 * runs the un-transacted OpenAI call and persists the outcome.
 * <p>
 * No method on this class is {@code @Transactional}, and none may become so: the OpenAI network
 * call inside {@link #run} must never run inside an open database transaction. Keeping this
 * workflow in its own small, single-purpose, entirely non-transactional class (rather than as a
 * method mixed into a larger {@code @Service}) keeps that invariant visible and auditable.
 */
@Component
@RequiredArgsConstructor
public class LessonGenerationWorkflow {

    private static final String PROVIDER_OPENAI = "openai";
    private static final String META_PROMPT_VERSION = "promptVersion";
    private static final String META_PREPARED_MATERIALS = "preparedMaterials";
    private static final String META_ATTACHED_FILES = "attachedFiles";
    private static final String META_PROVIDER = "provider";
    private static final String META_FAILED_AT = "failedAt";
    private static final String FILE_INPUT_TYPE = "type";
    private static final String FILE_INPUT_ID = "file_id";

    private final LessonEntityService lessonEntityService;
    private final MaterialOpenAiFilePreparationService materialOpenAiFilePreparationService;
    private final LessonPromptBuilder lessonPromptBuilder;
    private final LessonGenService lessonGenService;
    private final LessonContentUtil lessonContentUtil;
    private final CurrentTime currentTime;

    /**
     * Runs the theoretical-lesson AI generation workflow for a freshly created draft lesson.
     *
     * @param lesson      the draft lesson to generate content for
     * @param condensed   prepared (and, if needed, condensed) source material
     * @param draftInput  the draft creation input (instructions, depth, tone, format)
     * @param materialIds source material ids, used to prepare OpenAI file attachments
     * @param draftTitle  fallback title used if the generated content has none
     * @return the lesson in its final {@code ready} state
     * @throws AppException with reason {@code C003} when the AI provider call fails; the lesson
     *                       is marked failed first so the failure is visible to the requester
     */
    public Lesson run(
        Lesson lesson,
        PreparedMaterialsResult condensed,
        CreateLessonInput draftInput,
        List<Long> materialIds,
        String draftTitle
    ) {
        List<OpenAiFileInput> preparedFileInputs = materialOpenAiFilePreparationService.prepareFileInputs(materialIds);
        List<Map<String, Object>> attachedFiles = preparedFileInputs.stream()
            .map(fi -> Map.<String, Object>of(FILE_INPUT_TYPE, fi.type(), FILE_INPUT_ID, fi.fileId()))
            .toList();

        String userInstructions = stringVal(draftInput.instructions());
        LessonGenPrompt prompt = lessonPromptBuilder.buildTheoreticalLessonPrompt(
            condensed,
            userInstructions,
            draftInput.depth(),
            draftInput.tone(),
            draftInput.desiredFormat(),
            attachedFiles
        );

        Map<String, Object> serializedPreparedMaterials = condensed.toLegacyMap();
        Map<String, Object> generatingMeta = new LinkedHashMap<>();
        generatingMeta.put(META_PROMPT_VERSION, LessonPromptConstants.LESSON_PROMPT_VERSION);
        generatingMeta.put(META_PREPARED_MATERIALS, serializedPreparedMaterials);
        generatingMeta.put(META_ATTACHED_FILES, attachedFiles);
        lesson = lessonEntityService.markGenerating(lesson, generatingMeta);

        try {
            GeneratedContentResult result = lessonGenService.generateLessonContent(prompt);
            String rawContent = result.content();
            boolean isHtml = lessonContentUtil.looksLikeHtml(rawContent);
            String contentHtml = isHtml ? rawContent : lessonContentUtil.markdownToHtml(rawContent);

            String extractedTitle = lessonContentUtil.extractHtmlTitle(contentHtml);
            if (extractedTitle == null || extractedTitle.isBlank()) {
                extractedTitle = draftTitle;
            }

            Map<String, Object> readyMeta = new LinkedHashMap<>();
            if (result.metadata() != null) {
                readyMeta.putAll(result.metadata());
            }
            readyMeta.put(META_PREPARED_MATERIALS, serializedPreparedMaterials);
            readyMeta.put(META_ATTACHED_FILES, attachedFiles);

            return lessonEntityService.markReady(
                lesson,
                extractedTitle,
                contentHtml,
                isHtml ? "" : rawContent,
                readyMeta
            );
        } catch (Exception ex) {
            String errorMessage = ex.getMessage() == null ? "Lesson generation failed" : ex.getMessage();
            Map<String, Object> failedMeta = new LinkedHashMap<>();
            failedMeta.put(META_PROVIDER, PROVIDER_OPENAI);
            failedMeta.put(META_PROMPT_VERSION, LessonPromptConstants.LESSON_PROMPT_VERSION);
            failedMeta.put(META_PREPARED_MATERIALS, serializedPreparedMaterials);
            failedMeta.put(META_ATTACHED_FILES, attachedFiles);
            failedMeta.put(META_FAILED_AT, currentTime.utcDateTime().toString());
            lessonEntityService.markFailed(lesson, errorMessage, failedMeta);
            throw new AppException(ErrorReason.C003, "Lesson generation failed", ex);
        }
    }

    /**
     * Returns a trimmed string from any object, or empty string for null.
     *
     * @param value input value
     * @return trimmed value, or empty string when {@code null}
     */
    String stringVal(String value) {
        return value == null ? "" : value.trim();
    }
}
