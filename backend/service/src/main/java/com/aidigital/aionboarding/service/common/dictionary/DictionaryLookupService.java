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

import java.util.function.Function;

/**
 * Resolves dictionary codes (e.g. {@code "admin"}) to their primary keys/entities.
 *
 * <p>M3 (see {@code .claude/agent_docs/distributed_cache.md}): this class used to keep its own
 * bean-level {@code ConcurrentHashMap<String, Long>}, duplicating what the 8 dictionary entities'
 * Hibernate L2 regions and their {@code findByCode} query-cache regions already cache — and doing
 * so outside the {@code cache-management} invalidation protocol. Every {@code findByCode} call
 * below is already {@code @QueryHints(HINT_CACHEABLE)}-annotated on its repository (see
 * {@code ehcache.xml}'s {@code findXByCode} regions), so removing the extra map does not add a
 * database round trip on repeated lookups — it removes a second, unmanaged cache instance.
 */
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

	/**
	 * Resolves a user role code to its primary key.
	 *
	 * @param code the {@code user_role.code} value to resolve
	 * @return the matching primary key
	 */
	public Long userRoleId(String code) {
		return lookup(userRoleRepository::findByCode, code);
	}

	/**
	 * Resolves a lesson status code to its primary key.
	 *
	 * @param code the {@code lesson_status.code} value to resolve
	 * @return the matching primary key
	 */
	public Long lessonStatusId(String code) {
		return lookup(lessonStatusRepository::findByCode, code);
	}

	/**
	 * Resolves a lesson publication status code to its primary key.
	 *
	 * @param code the {@code lesson_publication_status.code} value to resolve
	 * @return the matching primary key
	 */
	public Long lessonPublicationStatusId(String code) {
		return lookup(lessonPublicationStatusRepository::findByCode, code);
	}

	/**
	 * Resolves a lesson content format code to its primary key.
	 *
	 * @param code the {@code lesson_content_format.code} value to resolve
	 * @return the matching primary key
	 */
	public Long lessonContentFormatId(String code) {
		return lookup(lessonContentFormatRepository::findByCode, code);
	}

	/**
	 * Resolves an activity type code to its primary key.
	 *
	 * @param code the {@code activity_type.code} value to resolve
	 * @return the matching primary key
	 */
	public Long activityTypeId(String code) {
		return lookup(activityTypeRepository::findByCode, code);
	}

	/**
	 * Resolves an activity progress status code to its primary key.
	 *
	 * @param code the {@code activity_progress_status.code} value to resolve
	 * @return the matching primary key
	 */
	public Long activityProgressStatusId(String code) {
		return lookup(activityProgressStatusRepository::findByCode, code);
	}

	/**
	 * Resolves a lesson asset kind code to its primary key.
	 *
	 * @param code the {@code lesson_asset_kind.code} value to resolve
	 * @return the matching primary key
	 */
	public Long lessonAssetKindId(String code) {
		return lookup(lessonAssetKindRepository::findByCode, code);
	}

	/**
	 * Resolves a material file kind code to its primary key.
	 *
	 * @param code the {@code material_file_kind.code} value to resolve
	 * @return the matching primary key
	 */
	public Long materialFileKindId(String code) {
		return lookup(materialFileKindRepository::findByCode, code);
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

	/**
	 * Resolves a dictionary code to its primary key through the given repository finder.
	 *
	 * <p>Package-private (not private) so it stays spyable per {@code .claude/rules/20-tests.md}.
	 * Deliberately uncached at this level: {@code finder} is always a {@code @QueryHints
	 * (HINT_CACHEABLE)} repository method, so Hibernate's own L2/query cache already serves
	 * repeated lookups without a bean-level map duplicating it.
	 *
	 * @param finder the repository's cacheable {@code findByCode} method reference
	 * @param code the dictionary code to resolve
	 * @param <T> the dictionary entity type
	 * @return the matching primary key
	 * @throws AppException C001 if no dictionary row with the given code exists
	 */
	<T extends DictionaryEntity> Long lookup(Function<String, java.util.Optional<T>> finder, String code) {
		return finder.apply(code)
				.map(DictionaryEntity::getId)
				.orElseThrow(() -> new AppException(ErrorReason.C001, "dictionary:" + code));
	}
}
