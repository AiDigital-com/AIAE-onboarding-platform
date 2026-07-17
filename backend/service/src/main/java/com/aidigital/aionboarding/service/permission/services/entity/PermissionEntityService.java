package com.aidigital.aionboarding.service.permission.services.entity;

import com.aidigital.aionboarding.domain.permission.entities.UserPermissionOverride;
import com.aidigital.aionboarding.domain.permission.repositories.UserPermissionOverrideRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;

/**
 * Short-transaction CRUD helpers for the {@link UserPermissionOverride} entity.
 * <p>
 * This is the only service that may inject {@link UserPermissionOverrideRepository} directly.
 * All other services that require permission-override data must depend on this service.
 */
@Service
@RequiredArgsConstructor
public class PermissionEntityService {

	private final UserPermissionOverrideRepository overrideRepository;

	/**
	 * Loads all permission overrides for a single user.
	 *
	 * @param userId the user primary key
	 * @return every {@link UserPermissionOverride} row for the given user
	 */
	@Transactional(readOnly = true)
	public List<UserPermissionOverride> findByIdUserId(Long userId) {
		return overrideRepository.findByIdUserId(userId);
	}

	/**
	 * Loads all permission overrides for a set of users.
	 *
	 * @param userIds the user primary keys
	 * @return every {@link UserPermissionOverride} row for the given users
	 */
	@Transactional(readOnly = true)
	public List<UserPermissionOverride> findByIdUserIdIn(Collection<Long> userIds) {
		return overrideRepository.findByIdUserIdIn(userIds);
	}

	/**
	 * Deletes all permission overrides for a single user.
	 *
	 * @param userId the user primary key
	 */
	@Transactional
	public void deleteByIdUserId(Long userId) {
		overrideRepository.deleteByIdUserId(userId);
	}

	/**
	 * Persists a permission override row.
	 *
	 * @param row the permission override to save
	 * @return the saved {@link UserPermissionOverride}
	 */
	@Transactional
	public UserPermissionOverride save(UserPermissionOverride row) {
		return overrideRepository.save(row);
	}
}
