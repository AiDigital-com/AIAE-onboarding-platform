package com.aidigital.aionboarding.service.user.services.impl;

import com.aidigital.aionboarding.service.common.time.CurrentTime;
import com.aidigital.aionboarding.domain.common.dictionary.UserRoleCode;
import com.aidigital.aionboarding.domain.common.dictionary.entities.UserRole;
import com.aidigital.aionboarding.domain.grade.entities.Grade;
import com.aidigital.aionboarding.domain.user.entities.User;
import com.aidigital.aionboarding.service.common.dictionary.DictionaryLookupService;
import com.aidigital.aionboarding.service.common.error.AppException;
import com.aidigital.aionboarding.service.common.error.ErrorReason;
import com.aidigital.aionboarding.service.common.security.AppUser;
import com.aidigital.aionboarding.service.grade.services.entity.GradeEntityService;
import com.aidigital.aionboarding.service.group.support.GroupAccessPolicy;
import com.aidigital.aionboarding.service.mappers.user.UserRecordMapper;
import com.aidigital.aionboarding.service.permission.models.PermissionSnapshotRecord;
import com.aidigital.aionboarding.service.permission.services.PermissionService;
import com.aidigital.aionboarding.service.storage.StorageService;
import com.aidigital.aionboarding.service.team.services.TeamService;
import com.aidigital.aionboarding.service.user.models.AdminUserStatsRecord;
import com.aidigital.aionboarding.service.user.models.UserRecord;
import com.aidigital.aionboarding.service.user.services.UserGradeAssignmentSyncService;
import com.aidigital.aionboarding.service.user.services.UserService;
import com.aidigital.aionboarding.service.user.services.entity.UserEntityService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserEntityService userEntityService;
    private final DictionaryLookupService dictionaryLookupService;
    private final GradeEntityService gradeEntityService;
    private final GroupAccessPolicy groupAccessPolicy;
    private final UserGradeAssignmentSyncService userGradeAssignmentSyncService;
    private final UserRecordMapper userMapper;
    private final TeamService teamService;
    private final PermissionService permissionService;
    private final StorageService storageService;
    private final CurrentTime currentTime;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AppUser resolveOrCreateFromClerk(String clerkUserId, String email, String fullName) {
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        Optional<User> existing = userEntityService.findByClerkUserId(clerkUserId)
            .or(() -> userEntityService.findByEmail(normalizedEmail));
        if (existing.isEmpty()) {
            // createUser already persists the new row; nothing else to backfill on it.
            return userMapper.toAppUser(createUser(clerkUserId, normalizedEmail, fullName));
        }

        User user = existing.get();
        boolean changed = false;
        if (user.getClerkUserId() == null || user.getClerkUserId().isBlank()) {
            user.setClerkUserId(clerkUserId);
            changed = true;
        }
        if (!normalizedEmail.equals(user.getEmail())) {
            user.setEmail(normalizedEmail);
            changed = true;
        }
        if (fullName != null && !fullName.isBlank() && (user.getName() == null || user.getName().isBlank())) {
            user.setName(fullName.trim());
            changed = true;
        }

        if (!changed) {
            return userMapper.toAppUser(user);
        }
        user.setUpdatedAt(currentTime.utcDateTime());
        return userMapper.toAppUser(userEntityService.save(user));
    }

    @Override
    @Transactional
    public UserRecord updateProfile(AppUser viewer, String name, String position, String avatarStorageKey, String avatarColor) {
        User user = userEntityService.findById(viewer.internalId())
            .orElseThrow(() -> new AppException(ErrorReason.C001, viewer.internalId()));
        if (name != null && !name.isBlank()) {
            user.setName(name.trim());
        }
        if (position != null) {
            user.setPosition(position);
        }
        if (avatarStorageKey != null) {
            // Verifies the caller actually owns this object (e.g. from a prior uploadMyAvatar
            // call) rather than persisting an arbitrary client-supplied storage key unchecked.
            // Skipped when unchanged or blank (clearing the avatar) — neither references a new
            // object needing ownership proof.
            if (!avatarStorageKey.isBlank() && !avatarStorageKey.equals(user.getAvatarStorageKey())) {
                storageService.requireOwnership(viewer, avatarStorageKey);
            }
            user.setAvatarStorageKey(avatarStorageKey);
        }
        if (avatarColor != null) {
            user.setAvatarColor(avatarColor);
        }
        user.setUpdatedAt(currentTime.utcDateTime());
        return userMapper.toRecord(userEntityService.save(user));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserRecord> findById(Long id) {
        return userEntityService.findById(id).map(userMapper::toRecord);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserRecord> findByEmail(String email) {
        return userEntityService.findByEmail(email.trim().toLowerCase(Locale.ROOT)).map(userMapper::toRecord);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserRecord> getAllUsers() {
        return userEntityService.findAll().stream()
            .sorted(Comparator
                .comparing((User u) -> roleOrder(u.getRole().getCode()))
                .thenComparing(User::getName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                .thenComparing(User::getEmail, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
            .map(userMapper::toRecord)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserRecord> listUsers(String roleCode, String search, int page, int size) {
        return userEntityService.search(roleCode, search, PageRequest.of(page, size)).map(userMapper::toRecord);
    }

    @Override
    @Transactional(readOnly = true)
    public AdminUserStatsRecord getAdminUserStats() {
        List<UserRecord> teamLeads = userEntityService.findByRoleCodeIn(List.of(UserRoleCode.TEAMLEAD)).stream()
            .map(userMapper::toRecord)
            .toList();
        int totalPermissionsEnabled = permissionService.snapshotForUsers(teamLeads).values().stream()
            .mapToInt(this::countEnabledPermissions)
            .sum();
        return new AdminUserStatsRecord(
            (int) userEntityService.count(),
            (int) userEntityService.countByRoleCode(UserRoleCode.ADMIN),
            teamLeads.size(),
            totalPermissionsEnabled
        );
    }

    /**
     * Counts how many effective permission flags are enabled in a permission snapshot.
     *
     * @param snapshot the permission snapshot to count
     * @return number of enabled effective permission flags
     */
    int countEnabledPermissions(PermissionSnapshotRecord snapshot) {
        return (int) snapshot.effective().values().stream().filter(Boolean::booleanValue).count();
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserRecord> listAssignableUsers(AppUser viewer) {
        return teamService.getAssignableLearningUsers(viewer);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserRecord> listAssignableUsers(AppUser viewer, String query, Pageable pageable) {
        return teamService.getAssignableLearningUsers(viewer, query, pageable);
    }

    @Override
    @Transactional
    public UserRecord updateGrade(AppUser viewer, Long userId, Long gradeId) {
        if (!groupAccessPolicy.canEditMemberGrade(viewer, userId)) {
            throw new AppException(ErrorReason.C004, "You can edit grades only for members of groups you lead.");
        }
        User user = userEntityService.findById(userId)
            .orElseThrow(() -> new AppException(ErrorReason.C001, userId));
        Long previousGradeId = user.getGrade() == null ? null : user.getGrade().getId();

        if (gradeId == null) {
            user.setGrade(null);
        } else {
            Grade grade = gradeEntityService.findById(gradeId)
                .orElseThrow(() -> new AppException(ErrorReason.C001, "Grade not found: " + gradeId));
            user.setGrade(grade);
        }
        user.setUpdatedAt(currentTime.utcDateTime());
        UserRecord updated = userMapper.toRecord(userEntityService.save(user));

        if (!Objects.equals(previousGradeId, gradeId)) {
            userGradeAssignmentSyncService.onGradeChanged(userId, gradeId);
        }
        return updated;
    }

    @Override
    @Transactional
    public UserRecord assignRole(AppUser viewer, Long userId, String roleCode) {
        if (viewer == null || !viewer.isAdmin()) {
            throw new AppException(ErrorReason.C004, "Only an admin can assign roles.");
        }
        User user = userEntityService.findById(userId)
            .orElseThrow(() -> new AppException(ErrorReason.C001, userId));
        if (roleCode.equals(user.getRole().getCode())) {
            return userMapper.toRecord(user);
        }
        UserRole role = dictionaryLookupService.getUserRoleReference(roleCode);
        user.setRole(role);
        user.setUpdatedAt(currentTime.utcDateTime());
        UserRecord updated = userMapper.toRecord(userEntityService.save(user));
        // Overrides recorded under the previous role (e.g. a team lead narrowing a member down to
        // "Restricted") are absolute booleans, not diffs, so they would otherwise silently carry
        // over and suppress or leak permissions under the new role's defaults.
        permissionService.resetOverrides(userId);
        return updated;
    }

    User createUser(String clerkUserId, String email, String fullName) {
        UserRole memberRole = dictionaryLookupService.getUserRoleReference(UserRoleCode.MEMBER);
        LocalDateTime now = currentTime.utcDateTime();
        User user = new User();
        user.setClerkUserId(clerkUserId);
        user.setEmail(email);
        user.setName(fullName == null || fullName.isBlank() ? email : fullName.trim());
        user.setRole(memberRole);
        user.setPosition("");
        user.setAvatarStorageKey("");
        user.setAvatarColor("");
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        return userEntityService.save(user);
    }

    int roleOrder(String roleCode) {
        return switch (roleCode) {
            case UserRoleCode.ADMIN -> 1;
            case UserRoleCode.TEAMLEAD -> 2;
            default -> 3;
        };
    }
}
