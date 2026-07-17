package com.aidigital.aionboarding.service.lessonactivity.models;

import com.aidigital.aionboarding.service.learning.models.LessonEnrollmentRecord;

import java.util.List;

public record ActivityCompletionResultRecord(
		ActivityProgressRecord progress,
		List<LessonActivityRecord> activities,
		boolean lessonCompleted,
		LessonEnrollmentRecord enrollment,
		ActivityAttemptRecord attempt
) {

}
