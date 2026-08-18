package com.aidigital.aionboarding.controllers;

import com.aidigital.aionboarding.api.v1.UsersApi;
import com.aidigital.aionboarding.api.v1.model.AvatarUploadResponseV1;
import com.aidigital.aionboarding.api.v1.model.UpdateProfileRequestV1;
import com.aidigital.aionboarding.api.v1.model.UpdateUserGradeRequestV1;
import com.aidigital.aionboarding.api.v1.model.UpdateUserGradeResponseV1;
import com.aidigital.aionboarding.api.v1.model.UserProfileV1;
import com.aidigital.aionboarding.mappers.user.UserApiMapper;
import com.aidigital.aionboarding.service.common.security.AppUser;
import com.aidigital.aionboarding.service.storage.enums.UploadPurpose;
import com.aidigital.aionboarding.service.user.models.UserRecord;
import com.aidigital.aionboarding.service.user.services.UserService;
import com.aidigital.aionboarding.support.ApiResponses;
import com.aidigital.aionboarding.support.MultipartFileUploadSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
public class UsersController implements UsersApi {

	private final CurrentUserSupport currentUser;
	private final UserService userService;
	private final MultipartFileUploadSupport multipartFileUploadSupport;
	private final UserApiMapper userApiMapper;
	private final ApiResponses apiResponses;

	@Override
	@Transactional(readOnly = true)
	public ResponseEntity<UserProfileV1> getMyProfile() {
		AppUser viewer = currentUser.requireUser();
		UserRecord user = userService.findById(viewer.internalId())
				.orElseThrow(() -> new com.aidigital.aionboarding.service.common.error.AppException(
						com.aidigital.aionboarding.service.common.error.ErrorReason.C000,
						"User record not found."
				));
		return ResponseEntity.ok(userApiMapper.toUserProfileV1(user));
	}

	@Override
	@Transactional
	public ResponseEntity<UserProfileV1> updateMyProfile(UpdateProfileRequestV1 request) {
		AppUser viewer = currentUser.requireUser();
		UserRecord updated = userService.updateProfile(
				viewer,
				request.getName(),
				request.getPosition(),
				request.getAvatarStorageKey(),
				request.getAvatarColor()
		);
		return ResponseEntity.ok(userApiMapper.toUserProfileV1(updated));
	}

	@Override
	@Transactional
	public ResponseEntity<AvatarUploadResponseV1> uploadMyAvatar(MultipartFile file) {
		AppUser viewer = currentUser.requireUser();
		multipartFileUploadSupport.requireNonEmpty(file, "Avatar file is required.");
		String storageKey = multipartFileUploadSupport.putBuffered(viewer, UploadPurpose.AVATAR, file, "avatar");
		return ResponseEntity.ok(apiResponses.avatarUpload(storageKey));
	}

	@Override
	@Transactional
	public ResponseEntity<UpdateUserGradeResponseV1> updateUserGrade(Long id, UpdateUserGradeRequestV1 request) {
		AppUser viewer = currentUser.requireUser();
		UserRecord updated = userService.updateGrade(viewer, id, request.getGradeId());
		return ResponseEntity.ok(userApiMapper.toUpdateUserGradeResponseV1(updated));
	}
}
