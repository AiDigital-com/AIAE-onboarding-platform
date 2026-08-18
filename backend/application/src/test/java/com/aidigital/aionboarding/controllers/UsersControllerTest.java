package com.aidigital.aionboarding.controllers;

import com.aidigital.aionboarding.api.v1.model.AvatarUploadResponseV1;
import com.aidigital.aionboarding.api.v1.model.UpdateProfileRequestV1;
import com.aidigital.aionboarding.api.v1.model.UpdateUserGradeRequestV1;
import com.aidigital.aionboarding.api.v1.model.UpdateUserGradeResponseV1;
import com.aidigital.aionboarding.api.v1.model.UserProfileV1;
import com.aidigital.aionboarding.mappers.user.UserApiMapper;
import com.aidigital.aionboarding.service.common.error.AppException;
import com.aidigital.aionboarding.service.common.error.ErrorReason;
import com.aidigital.aionboarding.service.common.security.AppUser;
import com.aidigital.aionboarding.service.storage.enums.UploadPurpose;
import com.aidigital.aionboarding.service.user.models.UserRecord;
import com.aidigital.aionboarding.service.user.services.UserService;
import com.aidigital.aionboarding.support.ApiResponses;
import com.aidigital.aionboarding.support.MultipartFileUploadSupport;
import org.instancio.Instancio;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsersControllerTest {

	@Mock
	private CurrentUserSupport currentUser;
	@Mock
	private UserService userService;
	@Mock
	private MultipartFileUploadSupport multipartFileUploadSupport;
	@Mock
	private UserApiMapper userApiMapper;
	@Mock
	private ApiResponses apiResponses;

	@InjectMocks
	private UsersController controller;

	@Test
	void shouldGetMyProfileTest() {
		// Given:
		AppUser viewer = viewer();
		UserRecord record = Instancio.create(UserRecord.class);
		UserProfileV1 expectedBody = Instancio.create(UserProfileV1.class);
		when(currentUser.requireUser()).thenReturn(viewer);
		when(userService.findById(1L)).thenReturn(Optional.of(record));
		when(userApiMapper.toUserProfileV1(record)).thenReturn(expectedBody);

		// When:
		ResponseEntity<UserProfileV1> response = controller.getMyProfile();

		// Then:
		assertThat(response.getBody()).isSameAs(expectedBody);
	}

	@Test
	void shouldThrowWhenUserRecordNotFoundTest() {
		// Given:
		AppUser viewer = viewer();
		when(currentUser.requireUser()).thenReturn(viewer);
		when(userService.findById(1L)).thenReturn(Optional.empty());

		// When / Then:
		assertThatThrownBy(() -> controller.getMyProfile())
				.isInstanceOf(AppException.class)
				.satisfies(ex -> assertThat(((AppException) ex).getCode()).isEqualTo("C000"));
	}

	@Test
	void shouldUpdateMyProfileTest() {
		// Given:
		AppUser viewer = viewer();
		UpdateProfileRequestV1 request = new UpdateProfileRequestV1()
				.name("New Name")
				.position("Developer")
				.avatarStorageKey("avatar-key")
				.avatarColor("blue");
		UserRecord updated = Instancio.create(UserRecord.class);
		UserProfileV1 expectedBody = Instancio.create(UserProfileV1.class);
		when(currentUser.requireUser()).thenReturn(viewer);
		when(userService.updateProfile(viewer, "New Name", "Developer", "avatar-key", "blue")).thenReturn(updated);
		when(userApiMapper.toUserProfileV1(updated)).thenReturn(expectedBody);

		// When:
		ResponseEntity<UserProfileV1> response = controller.updateMyProfile(request);

		// Then:
		assertThat(response.getBody()).isSameAs(expectedBody);
	}

	@Test
	void shouldUploadMyAvatarTest() {
		// Given: the null/empty check and the streaming/IOException boilerplate now live in
		// MultipartFileUploadSupport, which owns the collaborator-specific validation and
		// try/catch a controller may not express — both are covered by its own test
		AppUser viewer = viewer();
		MultipartFile file = mock(MultipartFile.class);
		AvatarUploadResponseV1 expectedBody = Instancio.create(AvatarUploadResponseV1.class);
		when(currentUser.requireUser()).thenReturn(viewer);
		when(multipartFileUploadSupport.putBuffered(viewer, UploadPurpose.AVATAR, file, "avatar"))
				.thenReturn("stored-avatar-key");
		when(apiResponses.avatarUpload("stored-avatar-key")).thenReturn(expectedBody);

		// When:
		ResponseEntity<AvatarUploadResponseV1> response = controller.uploadMyAvatar(file);

		// Then:
		assertThat(response.getBody()).isSameAs(expectedBody);
		verify(multipartFileUploadSupport).requireNonEmpty(file, "Avatar file is required.");
	}

	@Test
	void shouldPropagateRejectionOfAMissingAvatarFileFromTheUploadSupportTest() {
		// Given:
		AppUser viewer = viewer();
		MultipartFile file = mock(MultipartFile.class);
		when(currentUser.requireUser()).thenReturn(viewer);
		doThrow(new AppException(ErrorReason.C002, "Avatar file is required."))
				.when(multipartFileUploadSupport).requireNonEmpty(file, "Avatar file is required.");

		// When / Then:
		assertThatThrownBy(() -> controller.uploadMyAvatar(file))
				.isInstanceOf(AppException.class)
				.satisfies(ex -> assertThat(((AppException) ex).getCode()).isEqualTo("C002"));
	}

	@Test
	void shouldUpdateUserGradeTest() {
		// Given:
		AppUser viewer = viewer();
		UpdateUserGradeRequestV1 request = new UpdateUserGradeRequestV1().gradeId(42L);
		UserRecord updated = Instancio.create(UserRecord.class);
		UpdateUserGradeResponseV1 expectedBody = Instancio.create(UpdateUserGradeResponseV1.class);
		when(currentUser.requireUser()).thenReturn(viewer);
		when(userService.updateGrade(viewer, 5L, 42L)).thenReturn(updated);
		when(userApiMapper.toUpdateUserGradeResponseV1(updated)).thenReturn(expectedBody);

		// When:
		ResponseEntity<UpdateUserGradeResponseV1> response = controller.updateUserGrade(5L, request);

		// Then:
		assertThat(response.getBody()).isSameAs(expectedBody);
	}

	AppUser viewer() {
		return new AppUser(1L, "clerk-1", "user@test.com", "User", "member", "User", null, null, null);
	}
}
