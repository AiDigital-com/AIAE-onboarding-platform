package com.aidigital.aionboarding.domain.permission.repositories;

import com.aidigital.aionboarding.domain.permission.entities.UserPermissionOverride;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface UserPermissionOverrideRepository
        extends JpaRepository<UserPermissionOverride, UserPermissionOverride.UserPermissionOverrideId> {

    List<UserPermissionOverride> findByIdUserId(Long userId);

    List<UserPermissionOverride> findByIdUserIdIn(Collection<Long> userIds);

    void deleteByIdUserId(Long userId);
}
