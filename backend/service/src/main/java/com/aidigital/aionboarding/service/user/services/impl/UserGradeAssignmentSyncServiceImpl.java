package com.aidigital.aionboarding.service.user.services.impl;

import com.aidigital.aionboarding.service.roadmap.services.RoadmapGroupAssignmentSyncService;
import com.aidigital.aionboarding.service.user.services.UserGradeAssignmentSyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserGradeAssignmentSyncServiceImpl implements UserGradeAssignmentSyncService {

	private final RoadmapGroupAssignmentSyncService roadmapGroupAssignmentSyncService;

	@Override
	@Transactional
	public void onGradeChanged(Long userId, Long newGradeId) {
		roadmapGroupAssignmentSyncService.syncUserGradeChange(userId, newGradeId);
	}
}
