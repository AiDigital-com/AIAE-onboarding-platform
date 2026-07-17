package com.aidigital.aionboarding.service.lesson.models;

import com.aidigital.aionboarding.domain.roadmap.entities.RoadmapLesson;
import java.util.List;

/**
 * The enrolled roadmap that contains a viewed lesson, and its ordered roadmap-lesson rows.
 *
 * @param roadmapId the matched roadmap's primary key
 * @param lessons   the matched roadmap's roadmap-lesson rows, in sort order
 */
public record MatchedRoadmapRecord(Long roadmapId, List<RoadmapLesson> lessons) { }
