package com.aidigital.aionboarding.service.mappers.roadmap;

import com.aidigital.aionboarding.domain.roadmap.entities.Roadmap;
import com.aidigital.aionboarding.service.common.mapping.ServiceMapperConfig;
import com.aidigital.aionboarding.service.roadmap.models.RoadmapRecord;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = ServiceMapperConfig.class, implementationName = "RoadmapMapperImpl")
public interface RoadmapMapper {

	@Mapping(target = "lessonIds", ignore = true)
	@Mapping(target = "lessons", ignore = true)
	@Mapping(target = "isEnrolled", ignore = true)
	@Mapping(target = "enrolledAt", ignore = true)
	@Mapping(target = "viewerCanManage", ignore = true)
	RoadmapRecord toRecord(Roadmap entity);
}
