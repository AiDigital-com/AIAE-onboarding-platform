package com.aidigital.aionboarding.service.lesson.services.impl;

import com.aidigital.aionboarding.domain.common.dictionary.LessonPublicationStatusCode;
import com.aidigital.aionboarding.domain.common.dictionary.LessonStatusCode;
import com.aidigital.aionboarding.domain.lesson.entities.Lesson;
import com.aidigital.aionboarding.domain.lesson.entities.LessonAssistantConversation;
import com.aidigital.aionboarding.service.common.error.AppException;
import com.aidigital.aionboarding.service.common.error.ErrorReason;
import com.aidigital.aionboarding.service.common.observability.SecurityMetrics;
import com.aidigital.aionboarding.service.common.observability.enums.ContinuationRejectionReason;
import com.aidigital.aionboarding.service.common.security.AppUser;
import com.aidigital.aionboarding.service.common.time.CurrentTime;
import com.aidigital.aionboarding.service.lesson.enums.LessonAssistantPreset;
import com.aidigital.aionboarding.service.lesson.prompt.LessonAssistantPromptBuilder;
import com.aidigital.aionboarding.service.lesson.models.AskLessonResultRecord;
import com.aidigital.aionboarding.service.lesson.models.ChatTurn;
import com.aidigital.aionboarding.service.lesson.models.LessonAssistantConversationRecord;
import com.aidigital.aionboarding.service.learning.services.entity.LearningEnrollmentEntityService;
import com.aidigital.aionboarding.service.lesson.services.LessonAssistantService;
import com.aidigital.aionboarding.service.lesson.services.entity.LessonAssistantConversationEntityService;
import com.aidigital.aionboarding.service.lesson.services.entity.LessonEntityService;
import com.aidigital.aionboarding.service.lesson.support.LessonAssistantConversationAssembler;
import com.aidigital.aionboarding.service.lesson.support.LessonRecordAssembler;
import com.aidigital.aionboarding.service.lessongen.model.GeneratedContentResult;
import com.aidigital.aionboarding.service.lessongen.model.LessonGenPrompt;
import com.aidigital.aionboarding.service.lessongen.services.LessonGenService;
import com.aidigital.aionboarding.service.material.services.MaterialPreparationService;
import com.aidigital.aionboarding.service.permission.PermissionKeys;
import com.aidigital.aionboarding.service.permission.services.PermissionService;
import com.aidigital.aionboarding.service.user.services.entity.UserEntityService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class LessonAssistantServiceImpl implements LessonAssistantService {

    private static final Logger LOG = LoggerFactory.getLogger(LessonAssistantServiceImpl.class);

    private static final int MAX_QUESTION_LENGTH = 2000;
    private static final String META_ATTACHED_FILE_COUNT = "attachedFileCount";
    private static final String META_RESPONSE_ID = "responseId";

    private final LessonEntityService lessonEntityService;
    private final UserEntityService userEntityService;
    private final LearningEnrollmentEntityService learningEnrollmentEntityService;
    private final PermissionService permissionService;
    private final MaterialPreparationService materialPreparationService;
    private final LessonRecordAssembler lessonMapper;
    private final LessonGenService lessonGenService;
    private final LessonAssistantPromptBuilder lessonAssistantPromptBuilder;
    private final LessonAssistantConversationEntityService lessonAssistantConversationEntityService;
    private final LessonAssistantConversationAssembler lessonAssistantConversationAssembler;
    private final SecurityMetrics securityMetrics;
    private final CurrentTime currentTime;

    /**
     * Answers a learner's question about a lesson using the configured AI provider.
     * <p>
     * This method has no {@code @Transactional} annotation so the OpenAI network call inside
     * {@code lessonGenService.generateLessonContent} runs outside any DB transaction. The lesson
     * is loaded via {@link LessonEntityService#findByIdWithFetches(Long)} (not
     * {@code getReference}), which eagerly initialises {@code status}/{@code publicationStatus}/
     * {@code createdByUser} via JOIN FETCH inside its own short transaction — required because
     * nothing else keeps a Hibernate session open for those lazy associations to resolve later.
     * <p>
     * Whether this is a follow-up turn, and which provider response to continue from, is read
     * only from the caller's own saved conversation row (never from client input), so a browser
     * can neither select nor inject another conversation's continuation state.
     *
     * @param viewer   the authenticated learner asking the question
     * @param lessonId lesson identifier
     * @param question the learner's question
     * @param history  prior chat turns for context
     * @param preset   assistant response mode
     * @return the assistant's answer and generation metadata
     * @throws AppException C001 if the lesson is missing, not enrolled, or not ready/published
     * @throws AppException C002 if the question is blank or too long
     * @throws AppException C003 if the AI provider returns an incomplete or empty result
     */
    @Override
    public AskLessonResultRecord ask(
        AppUser viewer, Long lessonId, String question, List<ChatTurn> history, LessonAssistantPreset preset) {
        permissionService.requirePermission(viewer, PermissionKeys.LEARNING_ASK);
        String normalizedQuestion = question == null ? "" : question.trim();
        if (normalizedQuestion.isBlank()) {
            throw new AppException(ErrorReason.C002, "Question is required.");
        }
        if (normalizedQuestion.length() > MAX_QUESTION_LENGTH) {
            throw new AppException(ErrorReason.C002, "Question is too long.");
        }

        learningEnrollmentEntityService.findUserLessonByUserIdAndLessonId(viewer.internalId(), lessonId)
            .orElseThrow(() -> new AppException(ErrorReason.C001, lessonId));

        Lesson lesson = lessonEntityService.findByIdWithFetches(lessonId);
        if (!LessonStatusCode.READY.equals(lesson.getStatus().getCode())
            || !LessonPublicationStatusCode.PUBLISHED.equals(lesson.getPublicationStatus().getCode())) {
            throw new AppException(ErrorReason.C001, lessonId);
        }

        Optional<LessonAssistantConversation> savedConversation =
            lessonAssistantConversationEntityService.findByUserIdAndLessonId(viewer.internalId(), lessonId);
        String storedPreviousResponseId = savedConversation
            .map(LessonAssistantConversation::getLastResponseId)
            .filter(id -> id != null && !id.isBlank())
            .orElse(null);
        boolean isFollowUp = storedPreviousResponseId != null;

        Map<String, Object> lessonMap = lessonMapper.toDetailMap(lesson);
        // Follow-up turns chain onto the stored OpenAI response, which already retains the lesson
        // content and materials, so there is no need to re-run material preparation here.
        Map<String, Object> preparedMaterials = isFollowUp
            ? Map.of()
            : materialPreparationService.prepareForLesson(lessonId).toLegacyMap();
        Map<String, Object> prompt = lessonAssistantPromptBuilder.buildLessonAssistantPrompt(
            lessonMap, preparedMaterials, normalizedQuestion, history, preset, isFollowUp);

        Object instructionsVal = prompt.get("instructions");
        Object inputVal = prompt.get("input");
        if (instructionsVal == null || inputVal == null) {
            throw new AppException(ErrorReason.C003, "Prompt builder returned incomplete prompt: missing instructions or input.");
        }

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rawFileInputs =
            (List<Map<String, Object>>) prompt.getOrDefault("fileInputs", List.of());

        LessonGenPrompt genPrompt = new LessonGenPrompt(
            (String) prompt.get("version"),
            (String) prompt.get("cacheKey"),
            instructionsVal.toString(),
            inputVal.toString(),
            rawFileInputs,
            true,
            storedPreviousResponseId
        );

        GeneratedContentResult result = lessonGenService.generateLessonContent(genPrompt);
        String answer = result.content();
        if (answer == null || answer.isBlank()) {
            throw new AppException(ErrorReason.C003, "OpenAI returned an empty answer.");
        }

        Map<String, Object> metadata = new LinkedHashMap<>(result.metadata());
        metadata.put(META_ATTACHED_FILE_COUNT, rawFileInputs.size());

        String trimmedAnswer = answer.trim();
        saveConversationTurn(
            viewer.internalId(), lessonId, normalizedQuestion, trimmedAnswer, preset, metadata, savedConversation);

        Map<String, Object> publicMetadata = new LinkedHashMap<>(metadata);
        publicMetadata.remove(META_RESPONSE_ID);
        return new AskLessonResultRecord(trimmedAnswer, publicMetadata);
    }

    @Override
    public LessonAssistantConversationRecord getConversation(AppUser viewer, Long lessonId) {
        permissionService.requirePermission(viewer, PermissionKeys.LEARNING_ASK);
        learningEnrollmentEntityService.findUserLessonByUserIdAndLessonId(viewer.internalId(), lessonId)
            .orElseThrow(() -> new AppException(ErrorReason.C001, lessonId));

        return lessonAssistantConversationEntityService.findByUserIdAndLessonId(viewer.internalId(), lessonId)
            .map(lessonAssistantConversationAssembler::toRecord)
            .orElseGet(() -> new LessonAssistantConversationRecord(List.of(), LessonAssistantPreset.REGULAR.value()));
    }

    @Override
    public void clearConversation(AppUser viewer, Long lessonId) {
        permissionService.requirePermission(viewer, PermissionKeys.LEARNING_ASK);
        lessonAssistantConversationEntityService.deleteByUserIdAndLessonId(viewer.internalId(), lessonId);
    }

    /**
     * Appends the learner's question and the assistant's answer to their saved conversation for
     * this lesson, creating it on the first turn.
     * <p>
     * Two browser tabs can both load the same conversation, both call OpenAI, and then both try
     * to save; the entity's optimistic-locking version detects that race at save time. Since the
     * answer was already generated and already returned to whichever tab requested it, a lost
     * race here degrades gracefully: the turn just does not get appended to the persisted
     * history, rather than the caller seeing an error. This is retried once against freshly
     * reloaded state (which also naturally handles the conversation having been cleared out from
     * under this turn), then given up on as a permanent conflict.
     *
     * @param priorConversation the conversation loaded at the start of {@code ask()}, before the
     *                          OpenAI call, used as the base to append onto for the first attempt
     * @param userId internal user primary key
     * @param lessonId lesson primary key
     * @param question learner question
     * @param answer assistant answer
     * @param preset assistant response mode active for this turn
     * @param metadata generation metadata, expected to carry the OpenAI response id
     */
    void saveConversationTurn(
        Long userId, Long lessonId, String question, String answer, LessonAssistantPreset preset,
        Map<String, Object> metadata, Optional<LessonAssistantConversation> priorConversation
    ) {
        try {
            persistTurn(userId, lessonId, question, answer, preset, metadata, priorConversation);
        } catch (ObjectOptimisticLockingFailureException firstConflict) {
            Optional<LessonAssistantConversation> latest =
                lessonAssistantConversationEntityService.findByUserIdAndLessonId(userId, lessonId);
            try {
                persistTurn(userId, lessonId, question, answer, preset, metadata, latest);
            } catch (ObjectOptimisticLockingFailureException secondConflict) {
                securityMetrics.invalidContinuation(ContinuationRejectionReason.STALE_VERSION);
                LOG.warn(
                    "Lesson assistant conversation save lost a concurrent-tab race twice; "
                        + "this turn was not persisted to history. userId={} lessonId={}", userId, lessonId);
            }
        }
    }

    /**
     * Builds and saves the updated conversation entity for one turn, starting from the given
     * base state.
     *
     * @param userId internal user primary key
     * @param lessonId lesson primary key
     * @param question learner question
     * @param answer assistant answer
     * @param preset assistant response mode active for this turn
     * @param metadata generation metadata, expected to carry the OpenAI response id
     * @param base the conversation state to append this turn onto, or empty to start a new one
     * @throws ObjectOptimisticLockingFailureException if {@code base} is stale relative to the
     *     currently persisted row
     */
    void persistTurn(
        Long userId, Long lessonId, String question, String answer, LessonAssistantPreset preset,
        Map<String, Object> metadata, Optional<LessonAssistantConversation> base
    ) {
        LessonAssistantConversation conversation = base.orElseGet(LessonAssistantConversation::new);
        LocalDateTime now = currentTime.utcDateTime();
        if (conversation.getId() == null) {
            conversation.setUser(userEntityService.getReference(userId));
            conversation.setLesson(lessonEntityService.getReference(lessonId));
            conversation.setCreatedAt(now);
        }
        conversation.setMessages(
            lessonAssistantConversationAssembler.appendTurn(conversation.getMessages(), question, answer));
        conversation.setPreset(preset.value());
        Object responseIdValue = metadata.get(META_RESPONSE_ID);
        String responseId = responseIdValue == null ? null : responseIdValue.toString();
        conversation.setLastResponseId(responseId == null || responseId.isBlank() ? null : responseId);
        conversation.setUpdatedAt(now);
        lessonAssistantConversationEntityService.save(conversation);
    }
}
