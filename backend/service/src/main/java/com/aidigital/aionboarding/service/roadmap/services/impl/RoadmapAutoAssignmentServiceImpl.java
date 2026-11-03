package com.aidigital.aionboarding.service.roadmap.services.impl;

import com.aidigital.aionboarding.domain.roadmap.entities.Roadmap;
import com.aidigital.aionboarding.service.learning.services.RoadmapEnrollmentService;
import com.aidigital.aionboarding.service.roadmap.services.RoadmapAutoAssignmentService;
import com.aidigital.aionboarding.service.roadmap.services.entity.RoadmapEntityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RoadmapAutoAssignmentServiceImpl implements RoadmapAutoAssignmentService {

	private final RoadmapEntityService roadmapEntityService;
	private final RoadmapEnrollmentService roadmapEnrollmentService;

	@Override
	@Transactional
	public void enrollNewUser(Long userId) {
		for (Roadmap roadmap : roadmapEntityService.findAutoAssignedToNewUsers()) {
			roadmapEnrollmentService.enrollUsersInRoadmapAsAutoAssigned(List.of(userId), roadmap.getId());
		}
	}
}
