package com.aidigital.aionboarding.service.permission.services.impl;

import com.aidigital.aionboarding.service.common.time.CurrentTime;
import com.aidigital.aionboarding.domain.common.dictionary.UserRoleCode;
import com.aidigital.aionboarding.domain.permission.entities.UserPermissionOverride;
import com.aidigital.aionboarding.service.common.error.AppException;
import com.aidigital.aionboarding.service.common.error.ErrorReason;
import com.aidigital.aionboarding.service.common.security.AppUser;
import com.aidigital.aionboarding.service.common.security.RequestAuthenticationCache;
import com.aidigital.aionboarding.service.permission.PermissionKeys;
import com.aidigital.aionboarding.service.permission.models.PermissionSnapshotRecord;
import com.aidigital.aionboarding.service.permission.services.PermissionService;
import com.aidigital.aionboarding.service.permission.services.entity.PermissionEntityService;
import com.aidigital.aionboarding.service.group.services.entity.GroupLeadEntityService;
import com.aidigital.aionboarding.service.group.services.entity.GroupMemberEntityService;
import com.aidigital.aionboarding.service.permission.support.PermissionDefaultsProvider;
import com.aidigital.aionboarding.service.team.services.entity.TeamEntityService;
import com.aidigital.aionboarding.service.user.models.UserRecord;
import com.aidigital.aionboarding.service.user.services.entity.UserEntityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PermissionServiceImpl implements PermissionService {

    private final PermissionEntityService permissionEntityService;
    private final TeamEntityService teamEntityService;
    private final GroupLeadEntityService groupLeadEntityService;
    private final GroupMemberEntityService groupMemberEntityService;
    private final UserEntityService userEntityService;
    private final PermissionDefaultsProvider permissionDefaultsProvider;
    private final CurrentTime currentTime;
    private final RequestAuthenticationCache requestAuthenticationCache;

    @Override
    public Map<String, Boolean> getRoleDefaults(String roleCode) {
        Map<String, Boolean> defaults = permissionDefaultsProvider.baseDefaults(roleCode);
        return PermissionKeys.ALL.stream()
            .collect(Collectors.toMap(k -> k, k -> Boolean.TRUE.equals(defaults.get(k)), (a, b) -> a, LinkedHashMap::new));
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Boolean> getUserPermissionMap(AppUser user) {
        if (user == null) {
            return getRoleDefaults(UserRoleCode.MEMBER);
        }

        Optional<Map<String, Boolean>> cached = requestAuthenticationCache.getPermissionMap(user.internalId());
        if (cached.isPresent()) {
            return cached.get();
        }

        Map<String, Boolean> map = new LinkedHashMap<>(getRoleDefaults(user.roleCode()));
        permissionEntityService.findByIdUserId(user.internalId()).forEach(row -> {
            if (PermissionKeys.ALL.contains(row.getId().getPermissionKey())) {
                map.put(row.getId().getPermissionKey(), Boolean.TRUE.equals(row.getAllowed()));
            }
        });
        requestAuthenticationCache.putPermissionMap(user.internalId(), map);
        return map;
    }

    @Override
    public boolean userHasPermission(AppUser user, String permissionKey) {
        if (user == null || !PermissionKeys.ALL.contains(permissionKey)) {
            return false;
        }
        return Boolean.TRUE.equals(getUserPermissionMap(user).get(permissionKey));
    }

    @Override
    public void requirePermission(AppUser user, String permissionKey) {
        if (!userHasPermission(user, permissionKey)) {
            throw new AppException(ErrorReason.C004);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, PermissionSnapshotRecord> snapshotForUsers(List<UserRecord> users) {
        List<Long> ids = users.stream().map(UserRecord::id).filter(Objects::nonNull).toList();
        Map<Long, List<UserPermissionOverride>> overridesByUser = ids.isEmpty()
            ? Map.of()
            : permissionEntityService.findByIdUserIdIn(ids).stream()
                .collect(Collectors.groupingBy(row -> row.getId().getUserId()));

        Map<Long, PermissionSnapshotRecord> result = new HashMap<>();
        for (UserRecord user : users) {
            Map<String, Boolean> effective = getRoleDefaults(user.roleCode());
            Map<String, Boolean> overrides = new LinkedHashMap<>();
            for (UserPermissionOverride row : overridesByUser.getOrDefault(user.id(), List.of())) {
                if (PermissionKeys.ALL.contains(row.getId().getPermissionKey())) {
                    effective.put(row.getId().getPermissionKey(), Boolean.TRUE.equals(row.getAllowed()));
                    overrides.put(row.getId().getPermissionKey(), Boolean.TRUE.equals(row.getAllowed()));
                }
            }
            result.put(user.id(), new PermissionSnapshotRecord(user.roleCode(), effective, overrides));
        }
        return result;
    }

    /**
     * Replaces a user's permission overrides.
     * <p>
     * Locks the target user's row for the duration of this transaction (rather than relying on
     * optimistic locking on {@link UserPermissionOverride} itself, which has no single row that
     * represents "the whole override set") so two admins/team-leads editing the same target
     * user's overrides concurrently are serialized instead of one submission silently vanishing
     * under the other's delete-then-reinsert.
     */
    @Override
    @Transactional
    public void setOverrides(AppUser actor, Long targetUserId, Map<String, Boolean> overrides) {
        var target = userEntityService.findByIdForUpdate(targetUserId)
            .orElseThrow(() -> new AppException(ErrorReason.C001, targetUserId));
        String targetRoleCode = target.getRole().getCode();
        validateDelegation(actor, targetRoleCode, targetUserId);

        Map<String, Boolean> effectiveOverrides = applyDelegationDenylist(actor, targetRoleCode, overrides);

        Map<String, Boolean> roleDefaults = getRoleDefaults(targetRoleCode);
        permissionEntityService.deleteByIdUserId(targetUserId);

        effectiveOverrides.entrySet().stream()
            .filter(e -> PermissionKeys.ALL.contains(e.getKey()))
            .filter(e -> !Objects.equals(Boolean.TRUE.equals(e.getValue()), roleDefaults.get(e.getKey())))
            .forEach(e -> {
                UserPermissionOverride row = new UserPermissionOverride();
                UserPermissionOverride.UserPermissionOverrideId pk =
                    new UserPermissionOverride.UserPermissionOverrideId();
                pk.setUserId(targetUserId);
                pk.setPermissionKey(e.getKey());
                row.setId(pk);
                row.setUser(target);
                row.setAllowed(Boolean.TRUE.equals(e.getValue()));
                row.setGrantedByUser(userEntityService.getReference(actor.internalId()));
                row.setUpdatedAt(currentTime.utcDateTime());
                permissionEntityService.save(row);
            });
    }

    @Override
    @Transactional
    public void resetOverrides(Long userId) {
        permissionEntityService.deleteByIdUserId(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isTeamLeadForMember(Long leadUserId, Long memberUserId) {
        if (teamEntityService.existsByIdLeadUserIdAndIdMemberUserId(leadUserId, memberUserId)) {
            return true;
        }
        Set<Long> ledGroupIds = groupLeadEntityService.findGroupIdsByLeadUserId(leadUserId);
        if (ledGroupIds.isEmpty()) {
            return false;
        }
        return !Collections.disjoint(ledGroupIds, groupMemberEntityService.findGroupIdsByMemberUserId(memberUserId));
    }

    @Override
    public boolean canManageExistingLesson(AppUser user, Long createdByUserId) {
        if (user == null) {
            return false;
        }
        return user.isAdmin() || Objects.equals(user.internalId(), createdByUserId);
    }

    @Override
    public boolean canManageRoadmap(AppUser user, Long authorUserId) {
        if (user == null) {
            return false;
        }
        if (user.isAdmin() || Objects.equals(user.internalId(), authorUserId)) {
            return true;
        }
        return user.isTeamLead() && authorUserId != null && isTeamLeadForMember(user.internalId(), authorUserId);
    }

    @Override
    public boolean canManageTeam(AppUser user, Long leadUserId) {
        if (user == null) {
            return false;
        }
        return user.isAdmin() || Objects.equals(user.internalId(), leadUserId);
    }

    void validateDelegation(AppUser actor, String targetRoleCode, Long targetUserId) {
        if (UserRoleCode.ADMIN.equals(targetRoleCode)) {
            throw new AppException(ErrorReason.C004);
        }
        if (actor.isAdmin()) {
            return;
        }
        if (actor.isTeamLead()) {
            if (!UserRoleCode.MEMBER.equals(targetRoleCode) || !isTeamLeadForMember(actor.internalId(), targetUserId)) {
                throw new AppException(ErrorReason.C004);
            }
            return;
        }
        throw new AppException(ErrorReason.C004);
    }

    Map<String, Boolean> applyDelegationDenylist(AppUser actor, String targetRoleCode, Map<String, Boolean> overrides) {
        Set<String> denylist = Set.of();
        if (actor.isAdmin() && UserRoleCode.TEAMLEAD.equals(targetRoleCode)) {
            denylist = PermissionKeys.ADMIN_TO_TEAMLEAD_DENYLIST;
        } else if (actor.isTeamLead() && UserRoleCode.MEMBER.equals(targetRoleCode)) {
            denylist = PermissionKeys.TEAMLEAD_TO_MEMBER_DENYLIST;
        }
        if (denylist.isEmpty()) {
            return overrides;
        }
        Map<String, Boolean> filtered = new LinkedHashMap<>(overrides);
        denylist.forEach(filtered::remove);
        return filtered;
    }
}
