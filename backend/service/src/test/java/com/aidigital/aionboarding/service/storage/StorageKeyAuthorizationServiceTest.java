package com.aidigital.aionboarding.service.storage;

import com.aidigital.aionboarding.domain.learning.repositories.UserLessonRepository;
import com.aidigital.aionboarding.domain.lesson.repositories.LessonAssetRepository;
import com.aidigital.aionboarding.domain.lesson.repositories.LessonRepository;
import com.aidigital.aionboarding.domain.material.entities.MaterialFile;
import com.aidigital.aionboarding.domain.material.repositories.MaterialFileRepository;
import com.aidigital.aionboarding.domain.material.repositories.MaterialRepository;
import com.aidigital.aionboarding.domain.user.repositories.UserRepository;
import com.aidigital.aionboarding.service.common.error.AppException;
import com.aidigital.aionboarding.service.common.error.ErrorReason;
import com.aidigital.aionboarding.service.common.security.AppUser;
import com.aidigital.aionboarding.service.permission.PermissionKeys;
import com.aidigital.aionboarding.service.permission.services.PermissionService;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StorageKeyAuthorizationServiceTest {

	@Mock
	private MaterialFileRepository materialFileRepository;
	@Mock
	private LessonAssetRepository lessonAssetRepository;
	@Mock
	private UserRepository userRepository;
	@Mock
	private LessonRepository lessonRepository;
	@Mock
	private MaterialRepository materialRepository;
	@Mock
	private PermissionService permissionService;
	@Mock
	private UserLessonRepository userLessonRepository;

	@InjectMocks
	private StorageKeyAuthorizationService service;

	// -------------------------------------------------------------------------
	// Setup helpers
	// -------------------------------------------------------------------------

	private AppUser adminUser() {
		return new AppUser(1L, "clerk-admin", "admin@test.com", "Admin User", "admin", "Admin", null, null, null);
	}

	private AppUser managerUser() {
		return new AppUser(2L, "clerk-manager", "manager@test.com", "Manager User", "teamlead", "Manager", null, null,
				null);
	}

	private AppUser learnerUser() {
		return new AppUser(3L, "clerk-learner", "learner@test.com", "Learner User", "member", "Learner", null, null,
				null);
	}

	// -------------------------------------------------------------------------
	// Nested test groups
	// -------------------------------------------------------------------------

	@Nested
	class BlankKeyTests {

		@Test
		void requireAccess_blankStorageKey_throwsC004WithoutRepositoryCall() {
			AppUser viewer = learnerUser();

			// Execution
			AppException ex = assertThrows(AppException.class, () -> service.requireAccess(viewer, "   "));

			// Verification
			assertEquals(ErrorReason.C004.getCode(), ex.getCode());
			verify(materialFileRepository, never()).findByStorageKey(anyString());
			verify(lessonAssetRepository, never()).findLessonIdByStorageKey(anyString());
			verify(userRepository, never()).existsByAvatarStorageKey(anyString());
			verify(lessonRepository, never()).existsByCoverImageStorageKey(anyString());
			verify(materialRepository, never()).existsByCoverImageStorageKey(anyString());
		}

		@Test
		void requireAccess_nullStorageKey_throwsC004WithoutRepositoryCall() {
			AppUser viewer = learnerUser();

			// Execution
			AppException ex = assertThrows(AppException.class, () -> service.requireAccess(viewer, null));

			// Verification
			assertEquals(ErrorReason.C004.getCode(), ex.getCode());
			verify(materialFileRepository, never()).findByStorageKey(anyString());
			verify(lessonAssetRepository, never()).findLessonIdByStorageKey(anyString());
		}
	}

	@Nested
	class MaterialFileAccessTests {

		private static final String KEY = "uploads/material-file-key";

		@Test
		void requireAccess_materialFileMatch_adminUser_passes() {
			when(materialFileRepository.findByStorageKey(KEY)).thenReturn(Optional.of(new MaterialFile()));
			AppUser admin = adminUser();

			// Execution + Verification
			assertDoesNotThrow(() -> service.requireAccess(admin, KEY));
		}

		@Test
		void requireAccess_materialFileMatch_userWithLessonsManage_passes() {
			when(materialFileRepository.findByStorageKey(KEY)).thenReturn(Optional.of(new MaterialFile()));
			AppUser manager = managerUser();
			when(permissionService.userHasPermission(manager, PermissionKeys.LESSONS_MANAGE)).thenReturn(true);

			// Execution + Verification
			assertDoesNotThrow(() -> service.requireAccess(manager, KEY));
		}

		@Test
		void requireAccess_materialFileMatch_userWithoutLessonsManageAndNotAdmin_throwsC004() {
			when(materialFileRepository.findByStorageKey(KEY)).thenReturn(Optional.of(new MaterialFile()));
			AppUser learner = learnerUser();
			when(permissionService.userHasPermission(learner, PermissionKeys.LESSONS_MANAGE)).thenReturn(false);

			// Execution
			AppException ex = assertThrows(AppException.class, () -> service.requireAccess(learner, KEY));

			// Verification
			assertEquals(ErrorReason.C004.getCode(), ex.getCode());
		}
	}

	@Nested
	class LessonAssetAccessTests {

		private static final String KEY = "uploads/lesson-asset-key";
		private static final long LESSON_ID = 42L;

		@Test
		void requireAccess_lessonAssetMatch_userWithLessonsManage_passes() {
			when(materialFileRepository.findByStorageKey(KEY)).thenReturn(Optional.empty());
			when(lessonAssetRepository.findLessonIdByStorageKey(KEY)).thenReturn(Optional.of(LESSON_ID));
			AppUser manager = managerUser();
			when(permissionService.userHasPermission(manager, PermissionKeys.LESSONS_MANAGE)).thenReturn(true);

			// Execution + Verification
			assertDoesNotThrow(() -> service.requireAccess(manager, KEY));
			verify(userLessonRepository, never()).findByUserIdAndLessonId(anyLong(), anyLong());
		}

		@Test
		void requireAccess_lessonAssetMatch_enrolledLearner_passes() {
			when(materialFileRepository.findByStorageKey(KEY)).thenReturn(Optional.empty());
			when(lessonAssetRepository.findLessonIdByStorageKey(KEY)).thenReturn(Optional.of(LESSON_ID));
			AppUser learner = learnerUser();
			when(permissionService.userHasPermission(learner, PermissionKeys.LESSONS_MANAGE)).thenReturn(false);
			when(userLessonRepository.findByUserIdAndLessonId(learner.internalId(), LESSON_ID))
					.thenReturn(Optional.of(new com.aidigital.aionboarding.domain.learning.entities.UserLesson()));

			// Execution + Verification
			assertDoesNotThrow(() -> service.requireAccess(learner, KEY));
		}

		@Test
		void requireAccess_lessonAssetMatch_nonEnrolledLearnerWithoutLessonsManage_throwsC004() {
			when(materialFileRepository.findByStorageKey(KEY)).thenReturn(Optional.empty());
			when(lessonAssetRepository.findLessonIdByStorageKey(KEY)).thenReturn(Optional.of(LESSON_ID));
			AppUser learner = learnerUser();
			when(permissionService.userHasPermission(learner, PermissionKeys.LESSONS_MANAGE)).thenReturn(false);
			when(userLessonRepository.findByUserIdAndLessonId(learner.internalId(), LESSON_ID))
					.thenReturn(Optional.empty());

			// Execution
			AppException ex = assertThrows(AppException.class, () -> service.requireAccess(learner, KEY));

			// Verification
			assertEquals(ErrorReason.C004.getCode(), ex.getCode());
		}
	}

	@Nested
	class PublicKeyAccessTests {

		private static final String KEY = "uploads/public-key";

		@Test
		void requireAccess_userAvatarMatch_anyAuthenticatedUser_passes() {
			when(materialFileRepository.findByStorageKey(KEY)).thenReturn(Optional.empty());
			when(lessonAssetRepository.findLessonIdByStorageKey(KEY)).thenReturn(Optional.empty());
			when(userRepository.existsByAvatarStorageKey(KEY)).thenReturn(true);
			AppUser learner = learnerUser();

			// Execution + Verification
			assertDoesNotThrow(() -> service.requireAccess(learner, KEY));
		}

		@Test
		void requireAccess_lessonCoverMatch_anyAuthenticatedUser_passes() {
			when(materialFileRepository.findByStorageKey(KEY)).thenReturn(Optional.empty());
			when(lessonAssetRepository.findLessonIdByStorageKey(KEY)).thenReturn(Optional.empty());
			when(userRepository.existsByAvatarStorageKey(KEY)).thenReturn(false);
			when(lessonRepository.existsByCoverImageStorageKey(KEY)).thenReturn(true);
			AppUser learner = learnerUser();

			// Execution + Verification
			assertDoesNotThrow(() -> service.requireAccess(learner, KEY));
		}

		@Test
		void requireAccess_materialCoverMatch_anyAuthenticatedUser_passes() {
			when(materialFileRepository.findByStorageKey(KEY)).thenReturn(Optional.empty());
			when(lessonAssetRepository.findLessonIdByStorageKey(KEY)).thenReturn(Optional.empty());
			when(userRepository.existsByAvatarStorageKey(KEY)).thenReturn(false);
			when(lessonRepository.existsByCoverImageStorageKey(KEY)).thenReturn(false);
			when(materialRepository.existsByCoverImageStorageKey(KEY)).thenReturn(true);
			AppUser learner = learnerUser();

			// Execution + Verification
			assertDoesNotThrow(() -> service.requireAccess(learner, KEY));
		}

		@Test
		void requireAccess_noEntityMatchesStorageKey_throwsC004() {
			when(materialFileRepository.findByStorageKey(KEY)).thenReturn(Optional.empty());
			when(lessonAssetRepository.findLessonIdByStorageKey(KEY)).thenReturn(Optional.empty());
			when(userRepository.existsByAvatarStorageKey(KEY)).thenReturn(false);
			when(lessonRepository.existsByCoverImageStorageKey(KEY)).thenReturn(false);
			when(materialRepository.existsByCoverImageStorageKey(KEY)).thenReturn(false);
			AppUser learner = learnerUser();

			// Execution
			AppException ex = assertThrows(AppException.class, () -> service.requireAccess(learner, KEY));

			// Verification
			assertEquals(ErrorReason.C004.getCode(), ex.getCode());
		}
	}
}
