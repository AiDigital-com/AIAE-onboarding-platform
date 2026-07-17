package com.aidigital.aionboarding.service.common.dictionary;

import com.aidigital.aionboarding.domain.common.dictionary.DictionaryEntity;
import com.aidigital.aionboarding.domain.common.dictionary.entities.ActivityProgressStatus;
import com.aidigital.aionboarding.domain.common.dictionary.entities.ActivityType;
import com.aidigital.aionboarding.domain.common.dictionary.entities.LessonAssetKind;
import com.aidigital.aionboarding.domain.common.dictionary.entities.UserRole;
import com.aidigital.aionboarding.domain.common.dictionary.repositories.ActivityProgressStatusRepository;
import com.aidigital.aionboarding.domain.common.dictionary.repositories.ActivityTypeRepository;
import com.aidigital.aionboarding.domain.common.dictionary.repositories.LessonAssetKindRepository;
import com.aidigital.aionboarding.domain.common.dictionary.repositories.LessonContentFormatRepository;
import com.aidigital.aionboarding.domain.common.dictionary.repositories.LessonPublicationStatusRepository;
import com.aidigital.aionboarding.domain.common.dictionary.repositories.LessonStatusRepository;
import com.aidigital.aionboarding.domain.common.dictionary.repositories.MaterialFileKindRepository;
import com.aidigital.aionboarding.domain.common.dictionary.repositories.UserRoleRepository;
import com.aidigital.aionboarding.service.common.error.AppException;
import com.aidigital.aionboarding.service.common.error.ErrorReason;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
public class DictionaryLookupService {

	private final UserRoleRepository userRoleRepository;
	private final LessonStatusRepository lessonStatusRepository;
	private final LessonPublicationStatusRepository lessonPublicationStatusRepository;
	private final LessonContentFormatRepository lessonContentFormatRepository;
	private final ActivityTypeRepository activityTypeRepository;
	private final ActivityProgressStatusRepository activityProgressStatusRepository;
	private final LessonAssetKindRepository lessonAssetKindRepository;
	private final MaterialFileKindRepository materialFileKindRepository;

	private final Map<String, Long> cache = new ConcurrentHashMap<>();

	public Long userRoleId(String code) {
		return lookup("user_role:" + code, userRoleRepository::findByCode, code);
	}

	public Long lessonStatusId(String code) {
		return lookup("lesson_status:" + code, lessonStatusRepository::findByCode, code);
	}

	public Long lessonPublicationStatusId(String code) {
		return lookup("lesson_publication_status:" + code, lessonPublicationStatusRepository::findByCode, code);
	}

	public Long lessonContentFormatId(String code) {
		return lookup("lesson_content_format:" + code, lessonContentFormatRepository::findByCode, code);
	}

	public Long activityTypeId(String code) {
		return lookup("activity_type:" + code, activityTypeRepository::findByCode, code);
	}

	public Long activityProgressStatusId(String code) {
		return lookup("activity_progress_status:" + code, activityProgressStatusRepository::findByCode, code);
	}

	public Long lessonAssetKindId(String code) {
		return lookup("lesson_asset_kind:" + code, lessonAssetKindRepository::findByCode, code);
	}

	public Long materialFileKindId(String code) {
		return lookup("material_file_kind:" + code, materialFileKindRepository::findByCode, code);
	}

	/**
	 * Loads the {@link UserRole} entity for the given code, throwing if it does not exist.
	 * <p>
	 * Unlike {@link #userRoleId(String)} (which returns only the cached primary key), this
	 * method returns the full entity reference required by call sites that assign a role
	 * directly onto a {@code User} (e.g. {@code user.setRole(UserRole)}).
	 *
	 * @param code the {@code user_role.code} value to resolve
	 * @return the matching {@link UserRole} entity
	 * @throws AppException C001 if no user role with the given code exists
	 */
	public UserRole getUserRoleReference(String code) {
		return userRoleRepository.findByCode(code)
				.orElseThrow(() -> new AppException(ErrorReason.C001, "user_role:" + code));
	}

	/**
	 * Loads the {@link ActivityType} entity for the given code, throwing if it does not exist.
	 * <p>
	 * Unlike {@link #activityTypeId(String)} (which returns only the cached primary key), this
	 * method returns the full entity reference required by call sites that assign an activity
	 * type directly onto a {@code LessonActivity} (e.g. {@code activity.setType(ActivityType)}).
	 *
	 * @param code the {@code activity_type.code} value to resolve
	 * @return the matching {@link ActivityType} entity
	 * @throws AppException C001 if no activity type with the given code exists
	 */
	public ActivityType getActivityTypeReference(String code) {
		return activityTypeRepository.findByCode(code)
				.orElseThrow(() -> new AppException(ErrorReason.C001, code));
	}

	/**
	 * Loads the {@link ActivityProgressStatus} entity for the given code, throwing if it does
	 * not exist.
	 * <p>
	 * Unlike {@link #activityProgressStatusId(String)} (which returns only the cached primary
	 * key), this method returns the full entity reference required by call sites that assign a
	 * progress status directly onto a {@code UserLessonActivityProgress}
	 * (e.g. {@code progress.setStatus(ActivityProgressStatus)}).
	 *
	 * @param code the {@code activity_progress_status.code} value to resolve
	 * @return the matching {@link ActivityProgressStatus} entity
	 * @throws AppException C001 if no activity progress status with the given code exists
	 */
	public ActivityProgressStatus getActivityProgressStatusReference(String code) {
		return activityProgressStatusRepository.findByCode(code)
				.orElseThrow(() -> new AppException(ErrorReason.C001, code));
	}

	/**
	 * Loads the {@link LessonAssetKind} entity for the given code, throwing if it does not exist.
	 * <p>
	 * Unlike {@link #lessonAssetKindId(String)} (which returns only the cached primary key), this
	 * method returns the full entity reference required by call sites that assign a kind
	 * directly onto a {@code LessonAsset} (e.g. {@code lessonAsset.setKind(LessonAssetKind)}).
	 *
	 * @param code the {@code lesson_asset_kind.code} value to resolve
	 * @return the matching {@link LessonAssetKind} entity
	 * @throws AppException C001 if no lesson asset kind with the given code exists
	 */
	public LessonAssetKind getLessonAssetKindReference(String code) {
		return lessonAssetKindRepository.findByCode(code)
				.orElseThrow(() -> new AppException(ErrorReason.C001, "lesson_asset_kind:" + code));
	}

	<T extends DictionaryEntity> Long lookup(
			String cacheKey,
			Function<String, java.util.Optional<T>> finder,
			String code
	) {
		return cache.computeIfAbsent(cacheKey, k -> finder.apply(code)
				.map(DictionaryEntity::getId)
				.orElseThrow(() -> new AppException(ErrorReason.C001, "dictionary:" + code)));
	}
}
