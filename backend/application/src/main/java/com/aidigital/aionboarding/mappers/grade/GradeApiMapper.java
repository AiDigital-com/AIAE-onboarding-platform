package com.aidigital.aionboarding.mappers.grade;

import com.aidigital.aionboarding.api.v1.model.GradeResponseV1;
import com.aidigital.aionboarding.api.v1.model.GradeV1;
import com.aidigital.aionboarding.api.v1.model.GradesListResponseV1;
import com.aidigital.aionboarding.config.ApplicationMapperConfig;
import com.aidigital.aionboarding.service.grade.models.GradeRecord;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

@Mapper(config = ApplicationMapperConfig.class)
public interface GradeApiMapper {

    @Mapping(target = "isActive", source = "active")
    GradeV1 toGradeV1(GradeRecord grade);

    @Mapping(target = "grade", source = "grade")
    GradeResponseV1 toGradeResponseV1(GradeRecord grade);

    /**
     * Builds the grades list response. Declared over {@link Page} rather than {@link List}
     * directly: MapStruct cannot generate a bean-mapping method whose sole parameter is a bare
     * {@code java.util} iterable type.
     *
     * @param grades the grades, wrapped in a page
     * @return the grades list response
     */
    @Mapping(target = "grades", expression = "java(grades.getContent().stream().map(this::toGradeV1).toList())")
    GradesListResponseV1 toGradesListResponseV1(Page<GradeRecord> grades);

    /**
     * Builds the grades list response from a plain list.
     *
     * @param grades the grades
     * @return the grades list response
     */
    default GradesListResponseV1 toGradesListResponseV1(List<GradeRecord> grades) {
        return toGradesListResponseV1(new PageImpl<>(grades));
    }
}
