package com.aidigital.aionboarding.service.lesson.services.impl;

import com.aidigital.aionboarding.domain.lesson.entities.Lesson;
import com.aidigital.aionboarding.domain.material.entities.Material;
import com.aidigital.aionboarding.external.openai.model.OpenAiFileInput;
import com.aidigital.aionboarding.service.common.error.AppException;
import com.aidigital.aionboarding.service.common.error.ErrorReason;
import com.aidigital.aionboarding.service.common.security.AppUser;
import com.aidigital.aionboarding.service.common.time.CurrentTime;
import com.aidigital.aionboarding.service.lesson.enums.LessonCreationModeV1;
import com.aidigital.aionboarding.service.lesson.models.CreateLessonInput;
import com.aidigital.aionboarding.service.lesson.services.LessonInitialGenerationService;
import com.aidigital.aionboarding.service.lesson.services.entity.LessonEntityService;
import com.aidigital.aionboarding.service.lesson.support.LessonGenerationInputValidator;
import com.aidigital.aionboarding.service.lesson.support.LessonGenerationTranscriptCondenser;
import com.aidigital.aionboarding.service.lesson.util.LessonContentUtil;
import com.aidigital.aionboarding.service.lessongen.model.GeneratedContentResult;
import com.aidigital.aionboarding.service.lessongen.model.LessonGenPrompt;
import com.aidigital.aionboarding.service.lessongen.prompt.LessonPromptBuilder;
import com.aidigital.aionboarding.service.lessongen.prompt.LessonPromptConstants;
import com.aidigital.aionboarding.service.lessongen.services.LessonGenService;
import com.aidigital.aionboarding.service.material.models.PreparedMaterialsResult;
import com.aidigital.aionboarding.service.material.services.MaterialOpenAiFilePreparationService;
import com.aidigital.aionboarding.service.material.services.MaterialPreparationService;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Orchestrates lesson creation and initial AI generation.
 * <p>
 * The controller/service transaction boundary was intentionally removed: this
 * class calls short-transaction helpers on {@link LessonEntityService} and must
 * not hold a database transaction across OpenAI network calls. {@link #runGenerationWorkflow}
 * carries the un-transacted OpenAI call and stays on this class (rather than a
 * collaborator) so the transaction-boundary invariant remains visible here.
 */
@Service
@RequiredArgsConstructor
public class LessonInitialGenerationServiceImpl implements LessonInitialGenerationService {

    private static final String DEFAULT_DEPTH = "standard";
    private static final String DEFAULT_TONE = "clear";
    private static final String DEFAULT_FORMAT = "structured theoretical lesson";
    private static final String DEFAULT_THEORETICAL_DESCRIPTION = "Generated theoretical lesson from prompt instructions.";
    private static final String MATERIAL_DESCRIPTION_PREFIX = "Generated theoretical lesson from ";
    private static final String MATERIAL_DESCRIPTION_SUFFIX = " material(s).";
    private static final String PROVIDER_OPENAI = "openai";
    private static final String META_PROMPT_VERSION = "promptVersion";
    private static final String META_PREPARED_MATERIALS = "preparedMaterials";
    private static final String META_ATTACHED_FILES = "attachedFiles";
    private static final String META_PROVIDER = "provider";
    private static final String META_FAILED_AT = "failedAt";
    private static final String FILE_INPUT_TYPE = "type";
    private static final String FILE_INPUT_ID = "file_id";

    private final CurrentTime currentTime;
    private final LessonEntityService lessonEntityService;
    private final MaterialPreparationService materialPreparationService;
    private final MaterialOpenAiFilePreparationService materialOpenAiFilePreparationService;
    private final LessonPromptBuilder lessonPromptBuilder;
    private final LessonGenService lessonGenService;
    private final LessonContentUtil lessonContentUtil;
    private final LessonGenerationInputValidator inputValidator;
    private final LessonGenerationTranscriptCondenser transcriptCondenser;

    /** Generates or manually creates a lesson based on the input mode. */
    @Override
    public Lesson generate(AppUser viewer, CreateLessonInput input) {
        LessonCreationModeV1 mode = input.mode() == null
            ? LessonCreationModeV1.GENERATE
            : input.mode();
        List<Long> materialIds = input.materialIds() == null
            ? List.of()
            : input.materialIds().stream().filter(Objects::nonNull).toList();
        materialIds = inputValidator.deduplicatePreserveOrder(materialIds);

        if (mode == LessonCreationModeV1.CREATE_MANUAL) {
            inputValidator.validateManualLesson(input.title(), input.contentHtml());
            return lessonEntityService.createManualLesson(viewer, input, materialIds);
        }

        List<Material> materials = lessonEntityService.findMaterialsByIds(materialIds);
        inputValidator.validateMaterialsUsable(materialIds, materials, input.instructions());

        String draftTitle = inputValidator.buildDraftTitle(materials);
        String draftDescription = materialIds.isEmpty()
            ? DEFAULT_THEORETICAL_DESCRIPTION
            : MATERIAL_DESCRIPTION_PREFIX + materialIds.size() + MATERIAL_DESCRIPTION_SUFFIX;

        CreateLessonInput draftInput = new CreateLessonInput(
            draftTitle,
            input.instructions(),
            firstNonBlank(input.depth(), DEFAULT_DEPTH),
            firstNonBlank(input.tone(), DEFAULT_TONE),
            firstNonBlank(input.desiredFormat(), DEFAULT_FORMAT),
            input.materialIds(),
            input.tags() == null ? new ArrayList<>() : new ArrayList<>(input.tags()),
            draftDescription,
            null,
            input.mode()
        );

        Lesson lesson = lessonEntityService.createDraft(viewer, draftInput, materialIds);
        PreparedMaterialsResult prepared = materialPreparationService.prepareForMaterialIds(materialIds);
        PreparedMaterialsResult condensed = transcriptCondenser.condense(prepared);

        return runGenerationWorkflow(lesson, condensed, draftInput, materialIds, draftTitle);
    }

    /**
     * Prepares OpenAI file inputs, builds the generation prompt, marks the lesson
     * generating, then runs the un-transacted OpenAI call and persists the outcome.
     * No {@code @Transactional} wraps this method because the OpenAI network call
     * must never run inside an open database transaction.
     */
    Lesson runGenerationWorkflow(
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

            lesson = lessonEntityService.markReady(
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

        return lesson;
    }

    /** Returns the first non-blank value from the varargs list, or empty string if none. */
    String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "";
    }

    /** Returns a trimmed string from any object, or empty string for null. */
    String stringVal(String value) {
        return value == null ? "" : value.trim();
    }
}
