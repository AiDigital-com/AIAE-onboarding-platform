package com.aidigital.aionboarding.controllers;

import com.aidigital.aionboarding.api.v1.LessonsApi;
import com.aidigital.aionboarding.api.v1.model.*;
import com.aidigital.aionboarding.mappers.lesson.LessonApiMapper;
import com.aidigital.aionboarding.mappers.lessonactivity.LessonActivityApiMapper;
import com.aidigital.aionboarding.mappers.learning.LearningApiMapper;
import com.aidigital.aionboarding.support.ApiResponses;
import com.aidigital.aionboarding.service.common.security.AppUser;
import com.aidigital.aionboarding.service.learning.services.LearningEnrollmentService;
import com.aidigital.aionboarding.service.learning.services.LearningService;
import com.aidigital.aionboarding.mappers.common.LessonAssistantPresetApiMapper;
import com.aidigital.aionboarding.service.lesson.enums.LessonStatusAction;
import com.aidigital.aionboarding.service.lesson.models.ChatTurn;
import com.aidigital.aionboarding.service.lesson.services.LessonAssistantService;
import com.aidigital.aionboarding.service.lesson.services.LessonRevisionService;
import com.aidigital.aionboarding.service.lesson.services.LessonService;
import com.aidigital.aionboarding.service.lessonactivity.services.LessonActivityService;
import com.aidigital.aionboarding.service.permission.PermissionKeys;
import com.aidigital.aionboarding.service.material.services.UploadValidator;
import com.aidigital.aionboarding.service.storage.StorageService;
import com.aidigital.aionboarding.service.storage.enums.UploadPurpose;
import com.aidigital.aionboarding.service.teachervideo.services.TeacherVideoService;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
public class LessonsController implements LessonsApi {

    private final CurrentUserSupport currentUser;
    private final LessonService lessonService;
    private final LessonRevisionService lessonRevisionService;
    private final LessonAssistantService lessonAssistantService;
    private final TeacherVideoService teacherVideoService;
    private final LessonActivityService lessonActivityService;
    private final LearningService learningService;
    private final LearningEnrollmentService learningEnrollmentService;
    private final StorageService storageService;
    private final UploadValidator uploadValidator;
    private final LessonApiMapper lessonApiMapper;
    private final LessonAssistantPresetApiMapper lessonAssistantPresetApiMapper;
    private final LessonActivityApiMapper lessonActivityApiMapper;
    private final LearningApiMapper learningApiMapper;
    private final ApiResponses apiResponses;

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<LessonsListResponseV1> searchLessons(SearchLessonsV1 request) {
        AppUser viewer = currentUser.requireUser();
        LessonsListResponseV1 response = lessonApiMapper.toLessonsListResponseV1(
            lessonService.getAllLessons(
                viewer,
                lessonApiMapper.toLessonListQuery(request),
                lessonApiMapper.page(request),
                lessonApiMapper.size(request)
            )
        );
        if (response.getLessons() != null && !response.getLessons().isEmpty()) {
            List<Long> pageLessonIds = response.getLessons().stream().map(LessonSummaryV1::getId).toList();
            Set<Long> enrolledIds = learningEnrollmentService.getEnrolledLessonIds(viewer.internalId(), pageLessonIds);
            response.getLessons().forEach(lesson -> lesson.setIsEnrolled(enrolledIds.contains(lesson.getId())));
        }
        return ResponseEntity.ok(response);
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<CountResponseV1> countLessons(SearchLessonsV1 request) {
        AppUser viewer = currentUser.requireUser();
        long totalElements = lessonService.countLessons(viewer, lessonApiMapper.toLessonListQuery(request));
        return ResponseEntity.ok(lessonApiMapper.toCountResponseV1(totalElements));
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<LessonGenerationStatusResponseV1> getLessonGenerationStatus(Long id) {
        AppUser viewer = currentUser.requireUser();
        return ResponseEntity.ok(
            lessonApiMapper.toLessonGenerationStatusResponseV1(lessonService.getLessonGenerationStatus(viewer, id)));
    }

    @Override
    public ResponseEntity<LessonDetailResponseV1> getLesson(Long id) {
        AppUser viewer = currentUser.requireUser();
        var lesson = lessonService.getLesson(viewer, id);
        var activities = lessonActivityService.getLessonActivities(viewer, id);
        var enrollment = learningEnrollmentService.findLessonEnrollment(viewer, id).orElse(null);
        return ResponseEntity.ok(lessonApiMapper.toLessonDetailResponseV1(
            new com.aidigital.aionboarding.service.lesson.models.LessonDetailResultRecord(lesson, activities, enrollment)));
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<LessonActivitiesResponseV1> getLessonActivities(Long id) {
        AppUser viewer = currentUser.requireUser();
        return ResponseEntity.ok(lessonActivityApiMapper.toLessonActivitiesResponseV1(
            lessonActivityService.getLessonActivities(viewer, id)));
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<LessonActivityDetailResponseV1> getLessonActivity(Long id, Long activityId) {
        AppUser viewer = currentUser.requireUser();
        return ResponseEntity.ok(lessonActivityApiMapper.toLessonActivityDetailResponseV1(
            lessonActivityService.getLessonActivity(viewer, id, activityId)));
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<EnrollmentResponseV1> getLessonEnrollment(Long id) {
        AppUser viewer = currentUser.requireUser();
        return ResponseEntity.ok(learningApiMapper.toLessonEnrollmentResponseV1(
            new com.aidigital.aionboarding.service.learning.models.LessonEnrollmentResultRecord(
                true, learningEnrollmentService.getLessonEnrollment(viewer, id), java.util.List.of())));
    }

    @Override
    @PreAuthorize("@perm.has('" + PermissionKeys.LESSONS_CREATE + "')")
    public ResponseEntity<LessonResponseV1> createLesson(CreateLessonRequestV1 request) {
        AppUser viewer = currentUser.requireUser();
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(lessonApiMapper.toLessonResponseV1(lessonService.createLesson(viewer, lessonApiMapper.toCreateLessonInput(request))));
    }

    @Override
    @PreAuthorize("@perm.has('" + PermissionKeys.LESSONS_MANAGE + "')")
    @Transactional
    public ResponseEntity<LessonResponseV1> updateLessonContent(Long id, UpdateLessonContentRequestV1 request) {
        AppUser viewer = currentUser.requireUser();
        return ResponseEntity.ok(lessonApiMapper.toLessonResponseV1(
            lessonService.updateLessonContent(viewer, id, lessonApiMapper.toUpdateLessonContentInput(request))));
    }

    @Override
    @PreAuthorize("@perm.has('" + PermissionKeys.LESSONS_PUBLISH_ARCHIVE + "')")
    @Transactional
    public ResponseEntity<LessonResponseV1> changeLessonStatus(Long id, ChangeLessonStatusRequestV1 request) {
        AppUser viewer = currentUser.requireUser();
        return ResponseEntity.ok(lessonApiMapper.toLessonResponseV1(
            lessonService.changeLessonStatus(viewer, id, LessonStatusAction.fromValue(request.getAction().getValue()))));
    }

    @Override
    @PreAuthorize("@perm.has('" + PermissionKeys.LESSONS_MANAGE + "')")
    @Transactional
    public ResponseEntity<OkIdResponseV1> deleteLesson(Long id) {
        lessonService.deleteLesson(currentUser.requireUser(), id);
        return ResponseEntity.ok(apiResponses.okId(id));
    }

    @Override
    @PreAuthorize("@perm.has('" + PermissionKeys.LESSONS_MANAGE_ASSETS + "')")
    public ResponseEntity<UploadUrlResponseV1> createLessonUploadUrl(UploadUrlRequestV1 request) {
        AppUser viewer = currentUser.requireUser();
        var presigned = storageService.presignPut(
            viewer, UploadPurpose.LESSON_ASSET, request.getFileName(), request.getContentType(), request.getSize());
        return ResponseEntity.ok(apiResponses.uploadUrl(presigned.uploadUrl(), presigned.storageKey()));
    }

    @Override
    @PreAuthorize("@perm.has('" + PermissionKeys.LESSONS_MANAGE_ASSETS + "')")
    public ResponseEntity<UploadedFileResponseV1> uploadLessonFile(MultipartFile file) {
        AppUser viewer = currentUser.requireUser();
        UploadValidator.UploadValidationRecord uploadMeta = uploadValidator.validate(file);
        try (java.io.InputStream content = file.getInputStream()) {
            String storageKey = storageService.putObjectStreaming(
                viewer, UploadPurpose.LESSON_ASSET, content, uploadMeta.sizeBytes(), uploadMeta.originalName(), uploadMeta.mimeType());
            return ResponseEntity.ok(lessonApiMapper.toUploadedFileResponseV1(
                storageKey, uploadMeta.originalName(), uploadMeta.mimeType(), uploadMeta.sizeBytes()));
        } catch (java.io.IOException ex) {
            throw new com.aidigital.aionboarding.service.common.error.AppException(
                com.aidigital.aionboarding.service.common.error.ErrorReason.C000, ex.getMessage());
        }
    }

    @Override
    @PreAuthorize("@perm.has('" + PermissionKeys.LESSONS_MANAGE + "')")
    // No @Transactional here: reviseLesson() runs two sequential AI calls (planner + writer) and
    // is deliberately designed with no ambient transaction (see LessonRevisionServiceImpl javadoc)
    // so a Hikari connection is never held across those network waits.
    public ResponseEntity<LessonResponseV1> reviseLesson(Long id, ReviseLessonRequestV1 request) {
        return ResponseEntity.ok(lessonApiMapper.toLessonResponseV1(
            lessonRevisionService.reviseLesson(currentUser.requireUser(), id, lessonApiMapper.toReviseLessonInput(request))
        ));
    }

    @Override
    @PreAuthorize("@perm.has('" + PermissionKeys.LESSONS_MANAGE + "')")
    // No @Transactional here: generateActivity() calls out to OpenAI; wrapping it here would hold
    // a Hikari connection for the duration of that network call (see LessonActivityManagementServiceImpl).
    public ResponseEntity<ActivityResponseV1> generateActivity(Long id, GenerateActivityRequestV1 request) {
        return ResponseEntity.ok(lessonActivityApiMapper.toActivityResponseV1(
            lessonActivityService.generateActivity(
                currentUser.requireUser(),
                id,
                request.getType() != null ? request.getType().getValue() : null,
                request.getCount())
        ));
    }

    @Override
    @PreAuthorize("@perm.has('" + PermissionKeys.LESSONS_MANAGE + "')")
    @Transactional
    public ResponseEntity<ActivityResponseV1> updateActivity(Long id, Long activityId, UpdateActivityRequestV1 request) {
        return ResponseEntity.ok(lessonActivityApiMapper.toActivityResponseV1(
            lessonActivityService.updateActivity(
                currentUser.requireUser(), id, activityId, lessonActivityApiMapper.toUpdateActivityInput(request))
        ));
    }

    @Override
    @PreAuthorize("@perm.has('" + PermissionKeys.LESSONS_MANAGE + "')")
    @Transactional
    public ResponseEntity<LessonActivitiesResponseV1> deleteLessonActivity(Long id, Long activityId) {
        return ResponseEntity.ok(lessonActivityApiMapper.toLessonActivitiesResponseV1(
            lessonActivityService.deleteActivity(currentUser.requireUser(), id, activityId)
        ));
    }

    @Override
    @PreAuthorize("@perm.has('" + PermissionKeys.LEARNING_COMPLETE + "')")
    @Transactional
    public ResponseEntity<ActivityProgressResponseV1> submitActivityProgress(
        Long id,
        Long activityId,
        SubmitActivityProgressRequestV1 request
    ) {
        return ResponseEntity.ok(lessonActivityApiMapper.toActivityProgressResponseV1(
            lessonActivityService.submitActivityProgress(
                currentUser.requireUser(), id, activityId, toSubmitActivityProgressRequest(request))
        ));
    }

    @Override
    @PreAuthorize("@perm.has('" + PermissionKeys.LEARNING_ENROLL + "')")
    @Transactional
    public ResponseEntity<OkResponseV1> resetActivityProgress(Long id, Long activityId) {
        lessonActivityService.resetActivityProgress(currentUser.requireUser(), id, activityId);
        return ResponseEntity.ok(apiResponses.ok());
    }

    @Override
    @PreAuthorize("@perm.has('" + PermissionKeys.LEARNING_ASK + "')")
    // No @Transactional here: ask() calls out to OpenAI; wrapping it here would hold a Hikari
    // connection for the duration of that network call (see LessonAssistantServiceImpl).
    public ResponseEntity<AskLessonResponseV1> askLessonAssistant(Long id, AskLessonRequestV1 request) {
        return ResponseEntity.ok(lessonApiMapper.toAskLessonResponseV1(
            lessonAssistantService.ask(
                currentUser.requireUser(),
                id,
                request.getQuestion(),
                toChatHistory(request.getHistory()),
                lessonAssistantPresetApiMapper.mapAssistantPreset(request.getPreset()))
        ));
    }

    @Override
    @PreAuthorize("@perm.has('" + PermissionKeys.LEARNING_ASK + "')")
    @Transactional(readOnly = true)
    public ResponseEntity<LessonAssistantConversationResponseV1> getLessonAssistantConversation(Long id) {
        return ResponseEntity.ok(lessonApiMapper.toLessonAssistantConversationResponseV1(
            lessonAssistantService.getConversation(currentUser.requireUser(), id)
        ));
    }

    @Override
    @PreAuthorize("@perm.has('" + PermissionKeys.LEARNING_ASK + "')")
    @Transactional
    public ResponseEntity<OkResponseV1> clearLessonAssistantConversation(Long id) {
        lessonAssistantService.clearConversation(currentUser.requireUser(), id);
        return ResponseEntity.ok(apiResponses.ok());
    }

    @Override
    @PreAuthorize("@perm.has('" + PermissionKeys.LESSONS_MANAGE_ASSETS + "')")
    @Transactional
    public ResponseEntity<LessonAssetResponseV1> addLessonAsset(Long id, AddLessonAssetRequestV1 request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(lessonApiMapper.toLessonAssetResponseV1(
            lessonService.createAsset(currentUser.requireUser(), id, lessonApiMapper.toCreateLessonAssetInput(request))
        ));
    }

    @Override
    @PreAuthorize("@perm.has('" + PermissionKeys.LESSONS_MANAGE_ASSETS + "')")
    @Transactional
    public ResponseEntity<LessonResponseV1> deleteLessonAsset(Long id, Long assetId) {
        return ResponseEntity.ok(lessonApiMapper.toLessonResponseV1(
            lessonService.deleteAsset(currentUser.requireUser(), id, assetId)
        ));
    }

    @Override
    @PreAuthorize("@perm.has('" + PermissionKeys.LEARNING_ASSIGN + "')")
    @Transactional
    public ResponseEntity<AssignmentResponseV1> assignLesson(Long id, AssignmentRequestV1 request) {
        return ResponseEntity.ok(learningApiMapper.toLessonAssignmentResponseV1(
            learningService.assignLesson(currentUser.requireUser(), id, request.getUserIds())
        ));
    }

    @Override
    @PreAuthorize("@perm.has('" + PermissionKeys.LEARNING_ASSIGN + "')")
    @Transactional(readOnly = true)
    public ResponseEntity<LearningAssigneesResponseV1> listLessonAssignees(Long id) {
        return ResponseEntity.ok(learningApiMapper.toLearningAssigneesResponseV1(
            learningService.listLessonAssignees(currentUser.requireUser(), id)
        ));
    }

    @Override
    @PreAuthorize("@perm.has('" + PermissionKeys.LEARNING_ASSIGN + "')")
    @Transactional
    public ResponseEntity<OkResponseV1> revokeLessonAssignments(Long id, AssignmentRequestV1 request) {
        learningService.revokeLessonAssignments(currentUser.requireUser(), id, request.getUserIds());
        return ResponseEntity.ok(apiResponses.ok());
    }

    @Override
    @PreAuthorize("@perm.has('" + PermissionKeys.LEARNING_ENROLL + "')")
    @Transactional
    public ResponseEntity<EnrollmentResponseV1> enrollLesson(Long id) {
        return ResponseEntity.ok(learningApiMapper.toLessonEnrollmentResponseV1(
            learningService.enrollLesson(currentUser.requireUser(), id)
        ));
    }

    @Override
    @PreAuthorize("@perm.has('" + PermissionKeys.LEARNING_ENROLL + "')")
    @Transactional
    public ResponseEntity<OkResponseV1> unenrollLesson(Long id) {
        learningService.unenrollLesson(currentUser.requireUser(), id);
        return ResponseEntity.ok(apiResponses.ok());
    }

    @Override
    @PreAuthorize("@perm.has('" + PermissionKeys.LEARNING_COMPLETE + "')")
    @Transactional
    public ResponseEntity<EnrollmentResponseV1> setLessonCompletion(Long id, SetLessonCompletionRequestV1 request) {
        return ResponseEntity.ok(learningApiMapper.toLessonEnrollmentResponseV1(
            learningService.setLessonCompletion(
                currentUser.requireUser(), id, Boolean.TRUE.equals(request.getCompleted()))
        ));
    }

    @Override
    @PreAuthorize("@perm.has('" + PermissionKeys.LESSONS_MANAGE + "')")
    @Transactional
    public ResponseEntity<TeacherVideoResponseV1> createTeacherVideo(Long id, Object body) {
        return ResponseEntity.status(HttpStatus.CREATED).body(lessonApiMapper.toTeacherVideoResponseV1(
            teacherVideoService.create(currentUser.requireUser(), id)
        ));
    }

    @Override
    @PreAuthorize("@perm.has('" + PermissionKeys.LESSONS_MANAGE + "')")
    @Transactional
    public ResponseEntity<TeacherVideoResponseV1> getTeacherVideo(Long id) {
        return ResponseEntity.ok(lessonApiMapper.toTeacherVideoResponseV1(
            teacherVideoService.getStatus(currentUser.requireUser(), id)
        ));
    }

    @Override
    @PreAuthorize("@perm.has('" + PermissionKeys.LESSONS_MANAGE + "')")
    @Transactional
    public ResponseEntity<OkResponseV1> deleteTeacherVideo(Long id) {
        teacherVideoService.delete(currentUser.requireUser(), id);
        return ResponseEntity.ok(apiResponses.ok());
    }

    Map<String, Object> toSubmitActivityProgressRequest(SubmitActivityProgressRequestV1 request) {
        Map<String, Object> map = new LinkedHashMap<>();
        if (request.getType() != null) {
            map.put("type", request.getType().getValue());
        }
        if (request.getAnswers() != null) {
            map.put("answers", request.getAnswers());
        }
        if (request.getReviewedCards() != null) {
            map.put("reviewedCards", request.getReviewedCards());
        }
        return map;
    }

    List<ChatTurn> toChatHistory(List<ChatMessageV1> history) {
        if (history == null) {
            return List.of();
        }
        return history.stream()
            .filter(Objects::nonNull)
            .map(message -> new ChatTurn(
                message.getRole() == null ? null : message.getRole().getValue(),
                message.getContent() == null ? "" : message.getContent()
            ))
            .toList();
    }
}
