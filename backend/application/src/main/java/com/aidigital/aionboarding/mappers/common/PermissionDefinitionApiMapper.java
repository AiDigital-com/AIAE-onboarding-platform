package com.aidigital.aionboarding.mappers.common;

import com.aidigital.aionboarding.api.v1.model.PermissionDefinitionMetaV1;
import com.aidigital.aionboarding.config.ApplicationMapperConfig;
import java.util.List;
import org.mapstruct.Mapper;

/**
 * MapStruct mapper that exposes the permission-definition catalog. The catalog itself is owned by
 * the injected {@link PermissionDefinitionRegistry} bean, passed in explicitly by callers (MapStruct's
 * Spring-managed generated implementation for this interface takes no constructor arguments, so the
 * registry cannot be constructor-injected into the mapper itself).
 */
@Mapper(config = ApplicationMapperConfig.class)
public interface PermissionDefinitionApiMapper {

    /**
     * Returns the full catalog of permission definitions from the given registry.
     *
     * @param permissionDefinitionRegistry the catalog of permission definitions
     * @return the permission definitions, in their canonical order
     */
    default List<PermissionDefinitionMetaV1> allDefinitions(PermissionDefinitionRegistry permissionDefinitionRegistry) {
        return permissionDefinitionRegistry.all();
    }
}
