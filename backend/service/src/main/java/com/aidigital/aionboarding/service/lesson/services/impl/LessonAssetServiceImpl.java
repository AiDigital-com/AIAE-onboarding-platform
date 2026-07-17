package com.aidigital.aionboarding.service.lesson.services.impl;

import com.aidigital.aionboarding.domain.lesson.entities.Lesson;
import com.aidigital.aionboarding.domain.lesson.entities.LessonAsset;
import com.aidigital.aionboarding.service.common.error.AppException;
import com.aidigital.aionboarding.service.common.error.ErrorReason;
import com.aidigital.aionboarding.service.common.security.AppUser;
import com.aidigital.aionboarding.service.common.time.CurrentTime;
import com.aidigital.aionboarding.service.lesson.models.CreateLessonAssetInput;
import com.aidigital.aionboarding.service.lesson.models.LessonAssetDeleteResultRecord;
import com.aidigital.aionboarding.service.lesson.models.LessonAssetResultRecord;
import com.aidigital.aionboarding.service.lesson.services.LessonAssetService;
import com.aidigital.aionboarding.service.lesson.services.entity.LessonAssetEntityService;
import com.aidigital.aionboarding.service.lesson.services.entity.LessonEntityService;
import com.aidigital.aionboarding.service.lesson.support.LessonAssetDraftBuilder;
import com.aidigital.aionboarding.service.lesson.support.LessonRecordAssembler;
import com.aidigital.aionboarding.service.permission.PermissionKeys;
import com.aidigital.aionboarding.service.permission.services.PermissionService;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class LessonAssetServiceImpl implements LessonAssetService {

    private final CurrentTime currentTime;
    private final LessonEntityService lessonEntityService;
    private final LessonAssetEntityService lessonAssetEntityService;
    private final PermissionService permissionService;
    private final LessonAssetDraftBuilder lessonAssetDraftBuilder;
    private final LessonRecordAssembler lessonMapper;

    @Override
    @Transactional
    public LessonAssetResultRecord createAsset(AppUser viewer, Long lessonId, CreateLessonAssetInput input) {
        permissionService.requirePermission(viewer, PermissionKeys.LESSONS_MANAGE_ASSETS);
        Lesson lesson = requireManageableLesson(viewer, lessonId);
        String kind = lessonAssetDraftBuilder.resolveAssetKind(input);
        return new LessonAssetResultRecord(
            lessonMapper.toAssetRecord(lessonAssetDraftBuilder.persistAsset(viewer, lesson, input, kind)),
            lessonMapper.toDetailRecord(lesson)
        );
    }

    @Override
    @Transactional
    public LessonAssetDeleteResultRecord deleteAsset(AppUser viewer, Long lessonId, Long assetId) {
        permissionService.requirePermission(viewer, PermissionKeys.LESSONS_MANAGE_ASSETS);
        Lesson lesson = requireManageableLesson(viewer, lessonId);
        LessonAsset asset = lessonAssetEntityService.findById(assetId)
            .orElseThrow(() -> new AppException(ErrorReason.C001, "Lesson asset not found."));
        if (!lessonId.equals(asset.getLesson().getId())) {
            throw new AppException(ErrorReason.C001, "Lesson asset not found.");
        }

        removeInlineMediaReference(lesson, asset.getStorageKey());
        lessonAssetEntityService.delete(asset);
        return new LessonAssetDeleteResultRecord(lessonMapper.toDetailRecord(lesson));
    }

    /**
     * Loads the lesson and verifies the viewer can manage lesson assets for it.
     *
     * @param viewer   authenticated viewer
     * @param lessonId lesson identifier
     * @return manageable lesson
     */
    Lesson requireManageableLesson(AppUser viewer, Long lessonId) {
        Lesson lesson = lessonEntityService.getReference(lessonId);
        if (!permissionService.canManageExistingLesson(viewer,
            lesson.getCreatedByUser() == null ? null : lesson.getCreatedByUser().getId())) {
            throw new AppException(ErrorReason.C004);
        }
        return lesson;
    }

    private void removeInlineMediaReference(Lesson lesson, String storageKey) {
        if (!StringUtils.hasText(storageKey) || !StringUtils.hasText(lesson.getContentHtml())) {
            return;
        }

        Pattern mediaTag = Pattern.compile(
            "<(?:img|video)\\b(?=[^>]*\\bdata-storage-key\\s*=\\s*(['\"])"
                + Pattern.quote(storageKey)
                + "\\1)[^>]*(?:>\\s*</video>|>)",
            Pattern.CASE_INSENSITIVE
        );
        String nextHtml = mediaTag.matcher(lesson.getContentHtml()).replaceAll("");
        if (!nextHtml.equals(lesson.getContentHtml())) {
            lesson.setContentHtml(nextHtml);
            lesson.setUpdatedAt(currentTime.utcDateTime());
            lessonEntityService.save(lesson);
        }
    }
}
