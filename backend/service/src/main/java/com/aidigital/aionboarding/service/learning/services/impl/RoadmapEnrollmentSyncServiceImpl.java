package com.aidigital.aionboarding.service.learning.services.impl;

import com.aidigital.aionboarding.service.learning.models.CompletedRoadmapRecord;
import com.aidigital.aionboarding.service.learning.services.entity.LearningEnrollmentEntityService;
import com.aidigital.aionboarding.service.learning.services.RoadmapEnrollmentSyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RoadmapEnrollmentSyncServiceImpl implements RoadmapEnrollmentSyncService {

    private final LearningEnrollmentEntityService learningEnrollmentEntityService;

    @Override
    @Transactional(readOnly = true)
    public List<CompletedRoadmapRecord> getCompletedRoadmapsForUserLesson(Long userId, Long lessonId) {
        return learningEnrollmentEntityService.findCompletedRoadmapsForUserLesson(userId, lessonId);
    }
}
