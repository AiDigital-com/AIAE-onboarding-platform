package com.aidigital.aionboarding.service.teachervideo.services.impl;

import com.aidigital.aionboarding.domain.common.dictionary.LessonStatusCode;
import com.aidigital.aionboarding.domain.lesson.entities.Lesson;
import com.aidigital.aionboarding.service.common.error.AppException;
import com.aidigital.aionboarding.service.common.error.ErrorReason;
import com.aidigital.aionboarding.service.common.security.AppUser;
import com.aidigital.aionboarding.service.common.time.CurrentTime;
import com.aidigital.aionboarding.service.lesson.models.TeacherVideoDeleteResultRecord;
import com.aidigital.aionboarding.service.lesson.models.TeacherVideoRecord;
import com.aidigital.aionboarding.service.lesson.models.TeacherVideoResultRecord;
import com.aidigital.aionboarding.service.lesson.services.entity.LessonEntityService;
import com.aidigital.aionboarding.service.lesson.support.LessonRecordAssembler;
import com.aidigital.aionboarding.service.permission.PermissionKeys;
import com.aidigital.aionboarding.service.permission.services.PermissionService;
import com.aidigital.aionboarding.service.teachervideo.services.TeacherVideoRefreshService;
import com.aidigital.aionboarding.service.teachervideo.services.TeacherVideoService;
import com.aidigital.aionboarding.service.teachervideo.support.TeacherVideoCreationWorkflow;
import com.aidigital.aionboarding.service.teachervideo.support.TeacherVideoMetadataSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TeacherVideoServiceImpl implements TeacherVideoService {

	private final LessonEntityService lessonEntityService;
	private final PermissionService permissionService;
	private final LessonRecordAssembler lessonMapper;
	private final TeacherVideoRefreshService teacherVideoRefreshService;
	private final TeacherVideoMetadataSupport teacherVideoMetadataSupport;
	private final TeacherVideoCreationWorkflow teacherVideoCreationWorkflow;
	private final CurrentTime currentTime;

	@Override
	@Transactional
	public TeacherVideoResultRecord create(AppUser viewer, Long lessonId) {
		permissionService.requirePermission(viewer, PermissionKeys.LESSONS_MANAGE);
		requireAdmin(viewer);

		Lesson lesson = requireReadyLessonWithContent(viewer, lessonId);
		return teacherVideoCreationWorkflow.create(lesson, lessonId);
	}

	@Override
	@Transactional
	public TeacherVideoResultRecord getStatus(AppUser viewer, Long lessonId) {
		permissionService.requirePermission(viewer, PermissionKeys.LESSONS_MANAGE);
		requireAdmin(viewer);

		Lesson lesson = lessonEntityService.getReference(lessonId);
		Map<String, Object> metadata = lesson.getGenerationMetadata() == null
				? Map.of()
				: lesson.getGenerationMetadata();
		TeacherVideoRecord teacherVideo = lessonMapper.toTeacherVideoRecord(castMap(metadata.get("teacherVideo")));
		if (teacherVideo == null || teacherVideo.videoId() == null || teacherVideo.videoId().isBlank()) {
			throw new AppException(ErrorReason.C001, "No teacher video has been requested for this lesson.");
		}

		TeacherVideoRefreshService.RefreshResult refreshResult =
				teacherVideoRefreshService.refreshTeacherVideoIfNeeded(lesson, teacherVideo, true);
		return new TeacherVideoResultRecord(
				lessonMapper.normalizeTeacherVideoRecord(refreshResult.teacherVideo(), currentTime.instantString()),
				lessonMapper.toDetailRecord(refreshResult.lesson())
		);
	}

	@Override
	@Transactional
	public TeacherVideoDeleteResultRecord delete(AppUser viewer, Long lessonId) {
		permissionService.requirePermission(viewer, PermissionKeys.LESSONS_MANAGE);
		requireAdmin(viewer);

		Lesson lesson = lessonEntityService.getReference(lessonId);
		Map<String, Object> metadata = teacherVideoMetadataSupport.mutableMetadata(lesson);
		metadata.remove("teacherVideo");
		lesson.setGenerationMetadata(metadata);
		lesson.setUpdatedAt(currentTime.utcDateTime());
		Lesson saved = lessonEntityService.save(lesson);

		return new TeacherVideoDeleteResultRecord(lessonMapper.toDetailRecord(saved));
	}

	/**
	 * Loads the lesson and verifies it is READY with non-empty content and that the viewer can manage it.
	 *
	 * @param viewer   authenticated user
	 * @param lessonId lesson primary key
	 * @return the loaded {@link Lesson}
	 * @throws AppException C001 if not found; C004 if not manageable; C002 if not ready or empty
	 */
	Lesson requireReadyLessonWithContent(AppUser viewer, Long lessonId) {
		Lesson lesson = lessonEntityService.getReference(lessonId);
		if (!permissionService.canManageExistingLesson(viewer,
				lesson.getCreatedByUser() == null ? null : lesson.getCreatedByUser().getId())) {
			throw new AppException(ErrorReason.C004);
		}
		if (!LessonStatusCode.READY.equals(lesson.getStatus().getCode())) {
			throw new AppException(ErrorReason.C002, "Only ready lessons can have teacher videos.");
		}
		if ((lesson.getContentHtml() == null || lesson.getContentHtml().isBlank())
				&& (lesson.getContentMarkdown() == null || lesson.getContentMarkdown().isBlank())) {
			throw new AppException(ErrorReason.C002, "Lesson content is empty.");
		}
		return lesson;
	}

	/**
	 * Checks that the viewer has admin role.
	 *
	 * @param viewer authenticated user
	 * @throws AppException C004 if the viewer is not an admin
	 */
	void requireAdmin(AppUser viewer) {
		if (!viewer.isAdmin()) {
			throw new AppException(ErrorReason.C004);
		}
	}

	/**
	 * Safely casts a raw object to a {@code Map<String, Object>}, returning an empty map for non-maps.
	 *
	 * @param value raw object
	 * @return cast map, or an empty {@link LinkedHashMap}
	 */
	@SuppressWarnings("unchecked")
	Map<String, Object> castMap(Object value) {
		if (value instanceof Map<?, ?> map) {
			return (Map<String, Object>) map;
		}
		return new LinkedHashMap<>();
	}

}
