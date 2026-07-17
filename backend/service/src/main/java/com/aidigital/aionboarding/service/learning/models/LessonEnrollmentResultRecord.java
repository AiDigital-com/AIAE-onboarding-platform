package com.aidigital.aionboarding.service.learning.models;

import java.util.List;

public record LessonEnrollmentResultRecord(
    boolean ok,
    LessonEnrollmentRecord enrollment,
    List<CompletedRoadmapRecord> completedRoadmaps
) { }
