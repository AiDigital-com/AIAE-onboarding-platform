package com.aidigital.aionboarding.service.lesson.services.impl;

import com.aidigital.aionboarding.domain.lesson.entities.Lesson;
import com.aidigital.aionboarding.domain.material.entities.Material;
import com.aidigital.aionboarding.service.common.security.AppUser;
import com.aidigital.aionboarding.service.lesson.enums.LessonCreationModeV1;
import com.aidigital.aionboarding.service.lesson.models.CreateLessonInput;
import com.aidigital.aionboarding.service.lesson.services.LessonInitialGenerationService;
import com.aidigital.aionboarding.service.lesson.services.entity.LessonEntityService;
import com.aidigital.aionboarding.service.lesson.support.LessonGenerationInputValidator;
import com.aidigital.aionboarding.service.lesson.support.LessonGenerationTranscriptCondenser;
import com.aidigital.aionboarding.service.lesson.support.LessonGenerationWorkflow;
import com.aidigital.aionboarding.service.material.models.PreparedMaterialsResult;
import com.aidigital.aionboarding.service.material.services.MaterialPreparationService;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Orchestrates lesson creation and initial AI generation.
 * <p>
 * The controller/service transaction boundary was intentionally removed: this class calls
 * short-transaction helpers on {@link LessonEntityService} and must not hold a database
 * transaction across OpenAI network calls. The un-transacted OpenAI call itself lives in
 * {@link LessonGenerationWorkflow}, kept as its own small, entirely non-transactional class so
 * that invariant stays visible and auditable there.
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

    private final LessonEntityService lessonEntityService;
    private final MaterialPreparationService materialPreparationService;
    private final LessonGenerationInputValidator inputValidator;
    private final LessonGenerationTranscriptCondenser transcriptCondenser;
    private final LessonGenerationWorkflow lessonGenerationWorkflow;

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

        return lessonGenerationWorkflow.run(lesson, condensed, draftInput, materialIds, draftTitle);
    }

    /**
     * Returns the first non-blank value from the varargs list, or empty string if none.
     *
     * @param values candidate values in priority order
     * @return the first non-blank, trimmed value, or empty string when none are non-blank
     */
    String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "";
    }
}
