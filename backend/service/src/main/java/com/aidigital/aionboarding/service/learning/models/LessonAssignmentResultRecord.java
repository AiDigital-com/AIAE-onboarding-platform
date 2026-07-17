package com.aidigital.aionboarding.service.learning.models;

import java.util.List;

public record LessonAssignmentResultRecord(boolean ok, List<LessonAssignmentEnrollmentRecord> enrollments) { }
