package com.aidigital.aionboarding.service.roadmap.models;

import java.util.List;

public record CreateRoadmapInput(String title, String description, List<Long> lessonIds, List<String> tags) {

}
