package com.aidigital.aionboarding.service.lesson.services.impl;

import com.aidigital.aionboarding.service.common.error.AppException;
import com.aidigital.aionboarding.service.common.error.ErrorReason;
import com.aidigital.aionboarding.service.common.security.AppUser;
import com.aidigital.aionboarding.service.lesson.enums.LessonAssistantPreset;
import com.aidigital.aionboarding.service.lesson.models.AskLessonResultRecord;
import com.aidigital.aionboarding.service.lesson.models.ChatTurn;
import com.aidigital.aionboarding.service.lesson.models.LessonAssistantConversationRecord;
import com.aidigital.aionboarding.service.learning.services.entity.LearningEnrollmentEntityService;
import com.aidigital.aionboarding.service.lesson.services.LessonAssistantService;
import com.aidigital.aionboarding.service.lesson.services.entity.LessonAssistantConversationEntityService;
import com.aidigital.aionboarding.service.lesson.support.LessonAssistantAnswerWorkflow;
import com.aidigital.aionboarding.service.lesson.support.LessonAssistantConversationAssembler;
import com.aidigital.aionboarding.service.permission.PermissionKeys;
import com.aidigital.aionboarding.service.permission.services.PermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LessonAssistantServiceImpl implements LessonAssistantService {

    private final LearningEnrollmentEntityService learningEnrollmentEntityService;
    private final PermissionService permissionService;
    private final LessonAssistantConversationEntityService lessonAssistantConversationEntityService;
    private final LessonAssistantConversationAssembler lessonAssistantConversationAssembler;
    private final LessonAssistantAnswerWorkflow lessonAssistantAnswerWorkflow;

    @Override
    public AskLessonResultRecord ask(
        AppUser viewer, Long lessonId, String question, List<ChatTurn> history, LessonAssistantPreset preset) {
        permissionService.requirePermission(viewer, PermissionKeys.LEARNING_ASK);
        return lessonAssistantAnswerWorkflow.ask(viewer, lessonId, question, history, preset);
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
}
