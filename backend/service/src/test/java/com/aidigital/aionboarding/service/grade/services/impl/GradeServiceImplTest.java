package com.aidigital.aionboarding.service.grade.services.impl;

import com.aidigital.aionboarding.domain.grade.entities.Grade;
import com.aidigital.aionboarding.service.common.error.AppException;
import com.aidigital.aionboarding.service.common.security.AppUser;
import com.aidigital.aionboarding.service.common.time.CurrentTime;
import com.aidigital.aionboarding.service.grade.models.CreateGradeInput;
import com.aidigital.aionboarding.service.grade.models.GradeRecord;
import com.aidigital.aionboarding.service.grade.models.UpdateGradeInput;
import com.aidigital.aionboarding.service.grade.services.entity.GradeEntityService;
import com.aidigital.aionboarding.service.permission.PermissionKeys;
import com.aidigital.aionboarding.service.permission.services.PermissionService;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GradeServiceImplTest {

	@Mock
	private GradeEntityService gradeEntityService;
	@Mock
	private PermissionService permissionService;
	@Mock
	private CurrentTime currentTime;

	@InjectMocks
	private GradeServiceImpl service;

	@Nested
	class Create {

		@Test
		void createShouldPersistGradeWithDerivedCodeAndNextDisplayOrderTest() {
			// Given:
			AppUser admin = new AppUser(1L, "clerk-admin", "admin@test.com", "Admin", "admin", "Admin", null, null,
					null);
			when(gradeEntityService.existsByNameIgnoreCase("Junior")).thenReturn(false);
			when(gradeEntityService.existsByCodeIgnoreCase("junior")).thenReturn(false);
			Grade existing = new Grade();
			existing.setDisplayOrder(2);
			when(gradeEntityService.findAllOrderByDisplayOrder()).thenReturn(List.of(existing));
			when(currentTime.utcDateTime()).thenReturn(LocalDateTime.of(2026, 1, 1, 0, 0));
			when(gradeEntityService.save(any(Grade.class))).thenAnswer(invocation -> invocation.getArgument(0));

			// When:
			GradeRecord result = service.create(admin, new CreateGradeInput("Junior"));

			// Then:
			ArgumentCaptor<Grade> captor = ArgumentCaptor.forClass(Grade.class);
			verify(gradeEntityService).save(captor.capture());
			assertThat(captor.getValue().getCode()).isEqualTo("junior");
			assertThat(captor.getValue().getName()).isEqualTo("Junior");
			assertThat(captor.getValue().getDisplayOrder()).isEqualTo(3);
			assertThat(captor.getValue().getIsActive()).isTrue();
			assertThat(result.code()).isEqualTo("junior");
		}

		@Test
		void createShouldRejectDuplicateNameTest() {
			// Given:
			AppUser admin = new AppUser(1L, "clerk-admin", "admin@test.com", "Admin", "admin", "Admin", null, null,
					null);
			when(gradeEntityService.existsByNameIgnoreCase("Junior")).thenReturn(true);

			// When-Then:
			assertThatThrownBy(() -> service.create(admin, new CreateGradeInput("Junior")))
					.isInstanceOf(AppException.class);
			verify(gradeEntityService, never()).save(any());
		}

		@Test
		void createShouldRequireGradesManagePermissionTest() {
			// Given:
			AppUser member = new AppUser(3L, "clerk-member", "member@test.com", "Member", "member", "Member", null,
					null, null);
			doThrow(new AppException(com.aidigital.aionboarding.service.common.error.ErrorReason.C004))
					.when(permissionService).requirePermission(member, PermissionKeys.GRADES_MANAGE);

			// When-Then:
			assertThatThrownBy(() -> service.create(member, new CreateGradeInput("Junior")))
					.isInstanceOf(AppException.class);
			verify(gradeEntityService, never()).save(any());
		}

		@Test
		void createShouldRejectBlankNameTest() {
			// Given:
			AppUser admin = new AppUser(1L, "clerk-admin", "admin@test.com", "Admin", "admin", "Admin", null, null,
					null);

			// When-Then:
			assertThatThrownBy(() -> service.create(admin, new CreateGradeInput("   ")))
					.isInstanceOf(AppException.class);
			verify(gradeEntityService, never()).save(any());
		}
	}

	@Nested
	class Update {

		@Test
		void updateShouldRenameGradeWhenNameIsUniqueTest() {
			// Given:
			AppUser admin = new AppUser(1L, "clerk-admin", "admin@test.com", "Admin", "admin", "Admin", null, null,
					null);
			Grade grade = new Grade();
			grade.setId(5L);
			grade.setCode("junior");
			grade.setName("Junior");
			grade.setDisplayOrder(1);
			grade.setIsActive(true);
			when(gradeEntityService.findById(5L)).thenReturn(Optional.of(grade));
			when(gradeEntityService.existsByNameIgnoreCase("Junior Dev")).thenReturn(false);
			when(currentTime.utcDateTime()).thenReturn(LocalDateTime.of(2026, 1, 1, 0, 0));
			when(gradeEntityService.save(any(Grade.class))).thenAnswer(invocation -> invocation.getArgument(0));

			// When:
			GradeRecord result = service.update(admin, 5L, new UpdateGradeInput("Junior Dev"));

			// Then:
			assertThat(result.name()).isEqualTo("Junior Dev");
			assertThat(result.code()).isEqualTo("junior");
		}

		@Test
		void updateShouldThrowWhenGradeIsMissingTest() {
			// Given:
			AppUser admin = new AppUser(1L, "clerk-admin", "admin@test.com", "Admin", "admin", "Admin", null, null,
					null);
			when(gradeEntityService.findById(5L)).thenReturn(Optional.empty());

			// When-Then:
			assertThatThrownBy(() -> service.update(admin, 5L, new UpdateGradeInput("Junior Dev")))
					.isInstanceOf(AppException.class);
		}
	}

	@Nested
	class Deactivate {

		@Test
		void deactivateShouldSetIsActiveFalseTest() {
			// Given:
			AppUser admin = new AppUser(1L, "clerk-admin", "admin@test.com", "Admin", "admin", "Admin", null, null,
					null);
			Grade grade = new Grade();
			grade.setId(5L);
			grade.setIsActive(true);
			when(gradeEntityService.findById(5L)).thenReturn(Optional.of(grade));
			when(currentTime.utcDateTime()).thenReturn(LocalDateTime.of(2026, 1, 1, 0, 0));

			// When:
			service.deactivate(admin, 5L);

			// Then:
			ArgumentCaptor<Grade> captor = ArgumentCaptor.forClass(Grade.class);
			verify(gradeEntityService).save(captor.capture());
			assertThat(captor.getValue().getIsActive()).isFalse();
		}
	}

	@Nested
	class Activate {

		@Test
		void activateShouldSetIsActiveTrueTest() {
			// Given:
			AppUser admin = new AppUser(1L, "clerk-admin", "admin@test.com", "Admin", "admin", "Admin", null, null,
					null);
			Grade grade = new Grade();
			grade.setId(5L);
			grade.setIsActive(false);
			when(gradeEntityService.findById(5L)).thenReturn(Optional.of(grade));
			when(currentTime.utcDateTime()).thenReturn(LocalDateTime.of(2026, 1, 1, 0, 0));

			// When:
			service.activate(admin, 5L);

			// Then:
			ArgumentCaptor<Grade> captor = ArgumentCaptor.forClass(Grade.class);
			verify(gradeEntityService).save(captor.capture());
			assertThat(captor.getValue().getIsActive()).isTrue();
		}
	}

	@Nested
	class ListAll {

		@Test
		void listAllShouldRequireGradesManagePermissionTest() {
			// Given:
			AppUser member = new AppUser(3L, "clerk-member", "member@test.com", "Member", "member", "Member", null,
					null, null);
			doThrow(new AppException(com.aidigital.aionboarding.service.common.error.ErrorReason.C004))
					.when(permissionService).requirePermission(member, PermissionKeys.GRADES_MANAGE);

			// When-Then:
			assertThatThrownBy(() -> service.listAll(member))
					.isInstanceOf(AppException.class);
		}
	}
}
