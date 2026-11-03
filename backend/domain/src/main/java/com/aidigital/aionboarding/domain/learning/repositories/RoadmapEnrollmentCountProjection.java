package com.aidigital.aionboarding.domain.learning.repositories;

/**
 * Number of learners enrolled in one roadmap, from a grouped count query.
 *
 * @param roadmapId roadmap primary key
 * @param count     enrolled learner count
 */
public record RoadmapEnrollmentCountProjection(Long roadmapId, Long count) { }
