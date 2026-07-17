package com.aidigital.aionboarding.service.teachervideo.services.impl;

import com.aidigital.aionboarding.domain.common.dictionary.LessonStatusCode;
import com.aidigital.aionboarding.domain.lesson.entities.Lesson;
import com.aidigital.aionboarding.external.heygen.HeyGenClient;
import com.aidigital.aionboarding.external.heygen.HeyGenExternalException;
import com.aidigital.aionboarding.external.heygen.model.HeyGenTeacherVideoResult;
import com.aidigital.aionboarding.service.common.error.AppException;
import com.aidigital.aionboarding.service.common.error.ErrorReason;
import com.aidigital.aionboarding.service.common.security.AppUser;
import com.aidigital.aionboarding.service.common.time.CurrentTime;
import com.aidigital.aionboarding.service.lesson.models.TeacherVideoDeleteResultRecord;
import com.aidigital.aionboarding.service.lesson.models.TeacherVideoRecord;
import com.aidigital.aionboarding.service.lesson.models.TeacherVideoResultRecord;
import com.aidigital.aionboarding.service.lesson.services.entity.LessonEntityService;
import com.aidigital.aionboarding.service.lesson.support.LessonRecordAssembler;
import com.aidigital.aionboarding.service.material.services.MaterialPreparationService;
import com.aidigital.aionboarding.service.permission.PermissionKeys;
import com.aidigital.aionboarding.service.permission.services.PermissionService;
import com.aidigital.aionboarding.service.teachervideo.prompt.TeacherVideoPromptBuilder;
import com.aidigital.aionboarding.service.teachervideo.services.TeacherVideoRefreshService;
import com.aidigital.aionboarding.service.teachervideo.services.TeacherVideoService;
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
	private final MaterialPreparationService materialPreparationService;
	private final LessonRecordAssembler lessonMapper;
	private final HeyGenClient heyGenClient;
	private final TeacherVideoPromptBuilder teacherVideoPromptBuilder;
	private final TeacherVideoRefreshService teacherVideoRefreshService;
	private final TeacherVideoMetadataSupport teacherVideoMetadataSupport;
	private final CurrentTime currentTime;

	@Override
	@Transactional
	public TeacherVideoResultRecord create(AppUser viewer, Long lessonId) {
		permissionService.requirePermission(viewer, PermissionKeys.LESSONS_MANAGE);
		requireAdmin(viewer);

		Lesson lesson = requireReadyLessonWithContent(viewer, lessonId);
		Map<String, Object> metadata = teacherVideoMetadataSupport.mutableMetadata(lesson);
		rejectIfActiveGeneration(metadata);

		Map<String, Object> lessonMap = prepareLessonForPrompt(lesson, lessonId, metadata);
		return requestTeacherVideo(lesson, metadata, lessonMap);
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
	 * Calls HeyGen to create the teacher video and persists the result to the lesson metadata.
	 *
	 * @param lesson    the lesson to associate the video with
	 * @param metadata  mutable copy of the lesson's current generation metadata
	 * @param lessonMap full lesson context map passed to the prompt builder
	 * @return the creation result record
	 * @throws AppException C003 if the HeyGen call fails
	 */
	TeacherVideoResultRecord requestTeacherVideo(
			Lesson lesson,
			Map<String, Object> metadata,
			Map<String, Object> lessonMap
	) {
		String prompt = teacherVideoPromptBuilder.buildTeacherVideoPrompt(lessonMap);
		try {
			HeyGenTeacherVideoResult result = heyGenClient.createTeacherVideo(prompt);
			String checkedAt = currentTime.instantString();
			TeacherVideoRecord teacherVideo = new TeacherVideoRecord(
					"heygen",
					prompt,
					result.avatarId(),
					result.voiceId(),
					result.sessionId(),
					result.videoId(),
					result.status(),
					currentTime.instantString(),
					teacherVideoPromptBuilder.durationLimitSeconds(),
					checkedAt,
					"",
					"",
					null,
					null,
					null
			);

			metadata.put("teacherVideo", lessonMapper.toTeacherVideoMap(teacherVideo));
			lesson.setGenerationMetadata(metadata);
			lesson.setUpdatedAt(currentTime.utcDateTime());
			Lesson saved = lessonEntityService.save(lesson);

			return new TeacherVideoResultRecord(
					lessonMapper.normalizeTeacherVideoRecord(teacherVideo, checkedAt),
					lessonMapper.toDetailRecord(saved)
			);
		} catch (HeyGenExternalException ex) {
			throw new AppException(ErrorReason.C003, ex.getMessage());
		}
	}

	/**
	 * Prepares the lesson context map for the teacher video prompt, injecting prepared materials.
	 *
	 * @param lesson   the lesson entity
	 * @param lessonId lesson primary key used for material preparation
	 * @param metadata mutable metadata map to update with prepared materials
	 * @return the enriched lesson context map
	 */
	Map<String, Object> prepareLessonForPrompt(Lesson lesson, Long lessonId, Map<String, Object> metadata) {
		Map<String, Object> lessonMap = lessonMapper.toDetailMap(lesson);
		Map<String, Object> preparedMaterials = materialPreparationService.prepareForLesson(lessonId).toLegacyMap();
		metadata.put("preparedMaterials", preparedMaterials);
		lessonMap.put("generationMetadata", metadata);
		return lessonMap;
	}

	/**
	 * Guards against starting a new teacher video while one is already active.
	 *
	 * @param metadata the lesson's current generation metadata
	 * @throws AppException C006 if a video is already being generated
	 */
	void rejectIfActiveGeneration(Map<String, Object> metadata) {
		TeacherVideoRecord existingTeacherVideo = lessonMapper.toTeacherVideoRecord(castMap(metadata.get("teacherVideo"
		)));
		if (teacherVideoMetadataSupport.hasActiveTeacherVideo(existingTeacherVideo)) {
			throw new AppException(ErrorReason.C006, "A teacher video is already being generated for this lesson.");
		}
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
