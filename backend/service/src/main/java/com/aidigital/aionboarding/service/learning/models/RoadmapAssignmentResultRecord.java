package com.aidigital.aionboarding.service.learning.models;

import java.util.List;

public record RoadmapAssignmentResultRecord(boolean ok, List<RoadmapAssignmentEnrollmentRecord> enrollments) { }
