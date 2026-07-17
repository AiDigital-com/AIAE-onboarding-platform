package com.aidigital.aionboarding.service.lesson.models;

import com.aidigital.aionboarding.service.learning.models.LessonEnrollmentRecord;
import com.aidigital.aionboarding.service.lessonactivity.models.LessonActivityRecord;

import java.util.List;

public record LessonDetailResultRecord(
		LessonDetailRecord lesson,
		List<LessonActivityRecord> activities,
		LessonEnrollmentRecord enrollment
) {

}
