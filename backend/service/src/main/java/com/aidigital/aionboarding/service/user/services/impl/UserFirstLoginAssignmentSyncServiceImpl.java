package com.aidigital.aionboarding.service.user.services.impl;

import com.aidigital.aionboarding.service.roadmap.services.RoadmapAutoAssignmentService;
import com.aidigital.aionboarding.service.user.services.UserFirstLoginAssignmentSyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserFirstLoginAssignmentSyncServiceImpl implements UserFirstLoginAssignmentSyncService {

	private final RoadmapAutoAssignmentService roadmapAutoAssignmentService;

	@Override
	@Transactional
	public void onUserCreated(Long userId) {
		roadmapAutoAssignmentService.enrollNewUser(userId);
	}
}
