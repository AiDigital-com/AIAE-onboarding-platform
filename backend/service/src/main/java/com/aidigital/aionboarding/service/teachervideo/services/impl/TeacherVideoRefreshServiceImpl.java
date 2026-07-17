package com.aidigital.aionboarding.service.teachervideo.services.impl;

import com.aidigital.aionboarding.domain.lesson.entities.Lesson;
import com.aidigital.aionboarding.external.heygen.HeyGenClient;
import com.aidigital.aionboarding.external.heygen.HeyGenExternalException;
import com.aidigital.aionboarding.external.heygen.model.HeyGenVideoStatus;
import com.aidigital.aionboarding.service.common.error.AppException;
import com.aidigital.aionboarding.service.common.error.ErrorReason;
import com.aidigital.aionboarding.service.common.mapping.TextValueNormalizer;
import com.aidigital.aionboarding.service.common.time.CurrentTime;
import com.aidigital.aionboarding.service.lesson.models.TeacherVideoRecord;
import com.aidigital.aionboarding.service.lesson.services.entity.LessonEntityService;
import com.aidigital.aionboarding.service.lesson.support.LessonRecordAssembler;
import com.aidigital.aionboarding.service.teachervideo.services.TeacherVideoRefreshService;
import com.aidigital.aionboarding.service.teachervideo.support.TeacherVideoMetadataSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TeacherVideoRefreshServiceImpl implements TeacherVideoRefreshService {

	private static final long TEACHER_VIDEO_REFRESH_BUFFER_MS = 6L * 60L * 60L * 1000L;

	private final LessonEntityService lessonEntityService;
	private final LessonRecordAssembler lessonMapper;
	private final HeyGenClient heyGenClient;
	private final TeacherVideoMetadataSupport teacherVideoMetadataSupport;
	private final TextValueNormalizer textValueNormalizer;
	private final CurrentTime currentTime;

	@Override
	public RefreshResult refreshTeacherVideoIfNeeded(
			Lesson lesson,
			TeacherVideoRecord teacherVideo,
			boolean force
	) {
		if (!shouldRefreshTeacherVideoUrl(teacherVideo, force)) {
			return new RefreshResult(lesson, teacherVideo);
		}
		try {
			HeyGenVideoStatus video = heyGenClient.getVideoStatus(textValueNormalizer.raw(teacherVideo.videoId()));
			String checkedAt = currentTime.instantString();
			TeacherVideoRecord nextTeacherVideo = new TeacherVideoRecord(
					teacherVideo.provider(),
					teacherVideo.prompt(),
					teacherVideo.avatarId(),
					teacherVideo.voiceId(),
					teacherVideo.sessionId(),
					teacherVideo.videoId(),
					video.status(),
					teacherVideo.createdAt(),
					teacherVideo.durationLimitSeconds(),
					checkedAt,
					textValueNormalizer.firstNonBlankRaw(video.videoUrl(), teacherVideo.videoUrl()),
					textValueNormalizer.firstNonBlankRaw(video.thumbnailUrl(), teacherVideo.thumbnailUrl()),
					video.duration() == null ? teacherVideo.duration() : video.duration(),
					"completed".equals(video.status()) && teacherVideo.completedAt() == null
							? checkedAt
							: teacherVideo.completedAt(),
					"failed".equals(video.status()) && teacherVideo.failedAt() == null
							? checkedAt
							: teacherVideo.failedAt()
			);

			Map<String, Object> metadata = teacherVideoMetadataSupport.mutableMetadata(lesson);
			metadata.put("teacherVideo", lessonMapper.toTeacherVideoMap(nextTeacherVideo));
			lesson.setGenerationMetadata(metadata);
			lesson.setUpdatedAt(currentTime.utcDateTime());
			return new RefreshResult(lessonEntityService.save(lesson), nextTeacherVideo);
		} catch (HeyGenExternalException ex) {
			throw new AppException(ErrorReason.C003, ex.getMessage());
		}
	}

	/**
	 * Decides whether the teacher-video signed URL should be refreshed.
	 *
	 * @param teacherVideo current teacher-video record
	 * @param force        whether refresh was explicitly requested
	 * @return {@code true} when a provider refresh should run
	 */
	boolean shouldRefreshTeacherVideoUrl(TeacherVideoRecord teacherVideo, boolean force) {
		String videoId = textValueNormalizer.raw(teacherVideo.videoId());
		if (videoId.isBlank()) {
			return false;
		}
		if (force
				|| textValueNormalizer.raw(teacherVideo.videoUrl()).isBlank()
				|| teacherVideoMetadataSupport.hasActiveTeacherVideo(teacherVideo)) {
			return true;
		}
		Long expiresAt = getSignedUrlExpiresAt(textValueNormalizer.raw(teacherVideo.videoUrl()));
		return expiresAt != null && expiresAt <= System.currentTimeMillis() + TEACHER_VIDEO_REFRESH_BUFFER_MS;
	}

	/**
	 * Extracts the signed URL expiration timestamp from a provider URL.
	 *
	 * @param url signed provider URL
	 * @return expiration timestamp in milliseconds, or {@code null}
	 */
	Long getSignedUrlExpiresAt(String url) {
		if (url == null || url.isBlank()) {
			return null;
		}
		try {
			String expires = URI.create(url).getQuery();
			if (expires == null) {
				return null;
			}
			for (String part : expires.split("&")) {
				if (part.startsWith("Expires=")) {
					long seconds = Long.parseLong(part.substring("Expires=".length()));
					return seconds * 1000L;
				}
			}
			return null;
		} catch (Exception ex) {
			return null;
		}
	}

}
