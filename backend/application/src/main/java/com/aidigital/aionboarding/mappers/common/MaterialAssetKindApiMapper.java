package com.aidigital.aionboarding.mappers.common;

import com.aidigital.aionboarding.api.v1.model.MaterialAssetKindV1;
import com.aidigital.aionboarding.config.ApplicationMapperConfig;
import org.mapstruct.Mapper;

@Mapper(config = ApplicationMapperConfig.class)
public interface MaterialAssetKindApiMapper {

    default MaterialAssetKindV1 mapMaterialAssetKind(String kind) {
        if (kind == null || kind.isBlank()) {
            return MaterialAssetKindV1.FILE;
        }
        try {
            return MaterialAssetKindV1.fromValue(kind);
        } catch (IllegalArgumentException ex) {
            return MaterialAssetKindV1.FILE;
        }
    }

    default String fromMaterialAssetKind(MaterialAssetKindV1 kind) {
        return kind == null ? null : kind.getValue();
    }
}
