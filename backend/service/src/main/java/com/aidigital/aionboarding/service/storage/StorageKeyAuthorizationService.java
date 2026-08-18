package com.aidigital.aionboarding.service.storage;

import com.aidigital.aionboarding.domain.learning.repositories.UserLessonRepository;
import com.aidigital.aionboarding.domain.lesson.repositories.LessonAssetRepository;
import com.aidigital.aionboarding.domain.lesson.repositories.LessonRepository;
import com.aidigital.aionboarding.domain.material.repositories.MaterialRepository;
import com.aidigital.aionboarding.domain.user.repositories.UserRepository;
import com.aidigital.aionboarding.service.common.error.AppException;
import com.aidigital.aionboarding.service.common.error.ErrorReason;
import com.aidigital.aionboarding.service.common.security.AppUser;
import com.aidigital.aionboarding.service.material.services.MaterialFileService;
import com.aidigital.aionboarding.service.permission.PermissionKeys;
import com.aidigital.aionboarding.service.permission.services.PermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Authorizes access to a presigned storage key by looking up which entity
 * owns that key and applying the appropriate access rule.
 *
 * <p>Lookup order (first match wins):
 * <ol>
 *   <li>MaterialFile — requires LESSONS_MANAGE or admin
 *   <li>LessonAsset — requires LESSONS_MANAGE, admin, or enrollment in the owning lesson
 *   <li>User.avatarStorageKey — any authenticated user
 *   <li>Lesson.coverImageStorageKey — any authenticated user
 *   <li>Material.coverImageStorageKey — any authenticated user
 *   <li>No match — throws AppException(C004)
 * </ol>
 *
 * <p>This is a deliberate cross-entity authorization lookup spanning six entities. The
 * {@code MaterialFile} step goes through {@link MaterialFileService#existsByStorageKey}
 * rather than {@code MaterialFileRepository} directly, per the entity-service boundary rule
 * (P11a). The other five repositories (lesson asset, user, lesson, material, user-lesson) are
 * still injected directly here and are a known, separate, wider instance of the same rule
 * gap — recorded in the P11a migration-log row rather than fixed in that pass.
 */
@Service
@RequiredArgsConstructor
public class StorageKeyAuthorizationService {

    private final MaterialFileService materialFileService;
    private final LessonAssetRepository lessonAssetRepository;
    private final UserRepository userRepository;
    private final LessonRepository lessonRepository;
    private final MaterialRepository materialRepository;
    private final PermissionService permissionService;
    private final UserLessonRepository userLessonRepository;

    @Transactional(readOnly = true)
    public void requireAccess(AppUser viewer, String storageKey) {
        // Step 0: guard — blank key can never match any real entity
        if (storageKey == null || storageKey.isBlank()) {
            throw new AppException(ErrorReason.C004);
        }

        // Step 1: MaterialFile — requires LESSONS_MANAGE or admin
        if (materialFileService.existsByStorageKey(storageKey)) {
            if (!viewer.isAdmin() && !permissionService.userHasPermission(viewer, PermissionKeys.LESSONS_MANAGE)) {
                throw new AppException(ErrorReason.C004);
            }
            return;
        }

        // Step 2: LessonAsset — requires LESSONS_MANAGE, admin, or enrollment
        Optional<Long> lessonId = lessonAssetRepository.findLessonIdByStorageKey(storageKey);
        if (lessonId.isPresent()) {
            if (!viewer.isAdmin() && !permissionService.userHasPermission(viewer, PermissionKeys.LESSONS_MANAGE)) {
                // check enrollment
                if (userLessonRepository.findByUserIdAndLessonId(viewer.internalId(), lessonId.get()).isEmpty()) {
                    throw new AppException(ErrorReason.C004);
                }
            }
            return;
        }

        // Step 3: User avatar — any authenticated user
        if (userRepository.existsByAvatarStorageKey(storageKey)) {
            return;
        }

        // Step 4: Lesson cover image — any authenticated user
        if (lessonRepository.existsByCoverImageStorageKey(storageKey)) {
            return;
        }

        // Step 5: Material cover image — any authenticated user
        if (materialRepository.existsByCoverImageStorageKey(storageKey)) {
            return;
        }

        // Step 6: no match — deny (403, not 404, to avoid key existence leak)
        throw new AppException(ErrorReason.C004);
    }
}
