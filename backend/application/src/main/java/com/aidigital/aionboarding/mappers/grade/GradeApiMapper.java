package com.aidigital.aionboarding.mappers.grade;

import com.aidigital.aionboarding.api.v1.model.GradeResponseV1;
import com.aidigital.aionboarding.api.v1.model.GradeV1;
import com.aidigital.aionboarding.api.v1.model.GradesListResponseV1;
import com.aidigital.aionboarding.config.ApplicationMapperConfig;
import com.aidigital.aionboarding.service.grade.models.GradeRecord;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = ApplicationMapperConfig.class)
public interface GradeApiMapper {

    @Mapping(target = "isActive", source = "active")
    GradeV1 toGradeV1(GradeRecord grade);

    @Mapping(target = "grade", source = "grade")
    GradeResponseV1 toGradeResponseV1(GradeRecord grade);

    default GradesListResponseV1 toGradesListResponseV1(List<GradeRecord> grades) {
        GradesListResponseV1 response = new GradesListResponseV1();
        response.setGrades(grades.stream().map(this::toGradeV1).toList());
        return response;
    }
}
