package com.aidigital.aionboarding.service.roadmap.models;

import java.util.List;
import java.util.Optional;

public record UpdateRoadmapInput(Optional<String> title, Optional<String> description,
                                 Optional<List<Long>> lessonIds, Optional<List<String>> tags) {

}
