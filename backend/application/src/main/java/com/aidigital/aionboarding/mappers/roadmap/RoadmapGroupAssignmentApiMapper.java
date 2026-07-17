package com.aidigital.aionboarding.mappers.roadmap;

import com.aidigital.aionboarding.api.v1.model.RoadmapGroupAssignmentPreviewResponseV1;
import com.aidigital.aionboarding.api.v1.model.RoadmapGroupAssignmentPreviewV1;
import com.aidigital.aionboarding.api.v1.model.RoadmapGroupAssignmentResponseV1;
import com.aidigital.aionboarding.api.v1.model.RoadmapGroupAssignmentV1;
import com.aidigital.aionboarding.api.v1.model.RoadmapGroupAssignmentsListResponseV1;
import com.aidigital.aionboarding.config.ApplicationMapperConfig;
import com.aidigital.aionboarding.mappers.grade.GradeApiMapper;
import com.aidigital.aionboarding.mappers.learning.LearningApiMapper;
import com.aidigital.aionboarding.mappers.user.UserApiMapper;
import com.aidigital.aionboarding.service.roadmap.models.RoadmapGroupAssignmentPreviewRecord;
import com.aidigital.aionboarding.service.roadmap.models.RoadmapGroupAssignmentRecord;
import com.aidigital.aionboarding.service.roadmap.models.RoadmapGroupAssignmentResultRecord;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = ApplicationMapperConfig.class, uses = { UserApiMapper.class, GradeApiMapper.class, LearningApiMapper.class })
public interface RoadmapGroupAssignmentApiMapper {

    RoadmapGroupAssignmentV1 toRoadmapGroupAssignmentV1(RoadmapGroupAssignmentRecord assignment);

    @Mapping(target = "assignment", source = "assignment")
    @Mapping(target = "enrollments", source = "enrollments")
    RoadmapGroupAssignmentResponseV1 toRoadmapGroupAssignmentResponseV1(RoadmapGroupAssignmentResultRecord result);

    default RoadmapGroupAssignmentsListResponseV1 toRoadmapGroupAssignmentsListResponseV1(List<RoadmapGroupAssignmentRecord> assignments) {
        RoadmapGroupAssignmentsListResponseV1 response = new RoadmapGroupAssignmentsListResponseV1();
        response.setAssignments(assignments.stream().map(this::toRoadmapGroupAssignmentV1).toList());
        return response;
    }

    RoadmapGroupAssignmentPreviewV1 toRoadmapGroupAssignmentPreviewV1(RoadmapGroupAssignmentPreviewRecord preview);

    @Mapping(target = "preview", source = "preview")
    RoadmapGroupAssignmentPreviewResponseV1 toRoadmapGroupAssignmentPreviewResponseV1(RoadmapGroupAssignmentPreviewRecord preview);
}
