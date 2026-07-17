package com.aidigital.aionboarding.mappers.common;

import com.aidigital.aionboarding.api.v1.model.TeacherVideoProviderV1;
import com.aidigital.aionboarding.config.ApplicationMapperConfig;
import org.mapstruct.Mapper;

@Mapper(config = ApplicationMapperConfig.class)
public interface TeacherVideoProviderApiMapper {

    default TeacherVideoProviderV1 mapTeacherVideoProvider(String provider) {
        if (provider == null || provider.isBlank()) {
            return TeacherVideoProviderV1.HEYGEN;
        }
        try {
            return TeacherVideoProviderV1.fromValue(provider);
        } catch (IllegalArgumentException ex) {
            return TeacherVideoProviderV1.HEYGEN;
        }
    }

    default String fromTeacherVideoProvider(TeacherVideoProviderV1 provider) {
        return provider == null ? null : provider.getValue();
    }
}
