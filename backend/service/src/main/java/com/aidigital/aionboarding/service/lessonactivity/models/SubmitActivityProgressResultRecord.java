package com.aidigital.aionboarding.service.lessonactivity.models;

import com.aidigital.aionboarding.service.learning.models.CompletedRoadmapRecord;
import com.aidigital.aionboarding.service.learning.models.LessonEnrollmentRecord;

import java.util.List;

public record SubmitActivityProgressResultRecord(
		boolean ok,
		ActivityProgressRecord progress,
		List<LessonActivityRecord> activities,
		LessonEnrollmentRecord enrollment,
		boolean lessonCompleted,
		ActivityAttemptRecord attempt,
		List<CompletedRoadmapRecord> completedRoadmaps
) {

}
