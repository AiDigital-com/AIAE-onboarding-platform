package com.aidigital.aionboarding.service.lessonactivity.models;

import java.util.List;

public record LessonActivityWithAttemptsRecord(
		LessonActivityRecord activity,
		List<ActivityAttemptRecord> attempts
) {

}
