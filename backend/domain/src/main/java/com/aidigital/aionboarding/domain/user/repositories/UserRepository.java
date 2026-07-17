package com.aidigital.aionboarding.domain.user.repositories;

import com.aidigital.aionboarding.domain.user.entities.User;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.hibernate.jpa.SpecHints;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

	/**
	 * Lock-wait timeout for pessimistic write queries, in milliseconds.
	 */
	String LOCK_TIMEOUT = "10000";

	@Query("SELECT u FROM User u JOIN FETCH u.role WHERE u.clerkUserId = :clerkUserId")
	Optional<User> findByClerkUserId(@Param("clerkUserId") String clerkUserId);

	/**
	 * Loads a user and takes a pessimistic write lock on the row, held for the rest of the
	 * caller's transaction. Used to serialize concurrent full-replace operations on a user's
	 * data (e.g. permission overrides) that cannot be protected by optimistic locking on a
	 * single entity, since they read-delete-reinsert a whole related row set. Bounded by
	 * {@link #LOCK_TIMEOUT} so a stuck competing transaction cannot block this one forever.
	 */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@QueryHints({@QueryHint(name = SpecHints.HINT_SPEC_QUERY_TIMEOUT, value = LOCK_TIMEOUT)})
	@Query("SELECT u FROM User u WHERE u.id = :id")
	Optional<User> findByIdForUpdate(@Param("id") Long id);

	@Query("SELECT u FROM User u JOIN FETCH u.role WHERE u.email = :email")
	Optional<User> findByEmail(@Param("email") String email);

	@Override
	@Query("SELECT u FROM User u JOIN FETCH u.role")
	List<User> findAll();

	@Query("SELECT COUNT(u) > 0 FROM User u WHERE u.avatarStorageKey = :storageKey AND u.avatarStorageKey IS NOT NULL " +
			"AND u.avatarStorageKey <> ''")
	boolean existsByAvatarStorageKey(@Param("storageKey") String storageKey);

	@Query("SELECT u FROM User u JOIN FETCH u.role WHERE u.id != :excludeId")
	List<User> findAllExcluding(@Param("excludeId") Long excludeId);

	@Query(
			value = "SELECT u FROM User u JOIN FETCH u.role WHERE u.id != :excludeId",
			countQuery = "SELECT count(u) FROM User u WHERE u.id != :excludeId"
	)
	Page<User> findAllExcluding(@Param("excludeId") Long excludeId, Pageable pageable);

	@Query(
			value = """
					SELECT u
					FROM User u
					JOIN FETCH u.role role
					WHERE u.id != :excludeId
					  AND (lower(u.name) LIKE concat('%', lower(:query), '%')
					       OR lower(u.email) LIKE concat('%', lower(:query), '%'))
					""",
			countQuery = """
					SELECT count(u)
					FROM User u
					WHERE u.id != :excludeId
					  AND (lower(u.name) LIKE concat('%', lower(:query), '%')
					       OR lower(u.email) LIKE concat('%', lower(:query), '%'))
					"""
	)
	Page<User> findAllExcluding(
			@Param("excludeId") Long excludeId,
			@Param("query") String query,
			Pageable pageable
	);

	@Query("SELECT u FROM User u JOIN FETCH u.role WHERE u.role.code IN :roleCodes")
	List<User> findByRoleCodeIn(@Param("roleCodes") Collection<String> roleCodes);

	@Query(
			value = "SELECT u FROM User u JOIN FETCH u.role role WHERE role.code IN :roleCodes",
			countQuery = "SELECT count(u) FROM User u JOIN u.role role WHERE role.code IN :roleCodes"
	)
	Page<User> findByRoleCodeIn(@Param("roleCodes") Collection<String> roleCodes, Pageable pageable);

	@Query(
			value = """
					SELECT u
					FROM User u
					JOIN FETCH u.role role
					WHERE role.code IN :roleCodes
					  AND (lower(u.name) LIKE concat('%', lower(:query), '%')
					       OR lower(u.email) LIKE concat('%', lower(:query), '%'))
					""",
			countQuery = """
					SELECT count(u)
					FROM User u
					JOIN u.role role
					WHERE role.code IN :roleCodes
					  AND (lower(u.name) LIKE concat('%', lower(:query), '%')
					       OR lower(u.email) LIKE concat('%', lower(:query), '%'))
					"""
	)
	Page<User> findByRoleCodeIn(
			@Param("roleCodes") Collection<String> roleCodes,
			@Param("query") String query,
			Pageable pageable
	);

	@Query("SELECT u FROM User u JOIN FETCH u.role WHERE lower(u.name) = lower(:name)")
	Optional<User> findByNameIgnoreCase(@Param("name") String name);

	@Query("SELECT u FROM User u JOIN FETCH u.role WHERE u.role.code != :excludeRoleCode AND u.id != :excludeId")
	List<User> findTeamCandidates(@Param("excludeId") Long excludeId,
								  @Param("excludeRoleCode") String excludeRoleCode);

	@Query(
			value = """
					SELECT u
					FROM User u
					JOIN FETCH u.role role
					WHERE role.code != :excludeRoleCode
					  AND u.id != :excludeId
					""",
			countQuery = """
					SELECT count(u)
					FROM User u
					JOIN u.role role
					WHERE role.code != :excludeRoleCode
					  AND u.id != :excludeId
					"""
	)
	Page<User> findTeamCandidates(
			@Param("excludeId") Long excludeId,
			@Param("excludeRoleCode") String excludeRoleCode,
			Pageable pageable
	);

	@Query(
			value = """
					SELECT u
					FROM User u
					JOIN FETCH u.role role
					WHERE role.code != :excludeRoleCode
					  AND u.id != :excludeId
					  AND (lower(u.name) LIKE concat('%', lower(:query), '%')
					       OR lower(u.email) LIKE concat('%', lower(:query), '%'))
					""",
			countQuery = """
					SELECT count(u)
					FROM User u
					JOIN u.role role
					WHERE role.code != :excludeRoleCode
					  AND u.id != :excludeId
					  AND (lower(u.name) LIKE concat('%', lower(:query), '%')
					       OR lower(u.email) LIKE concat('%', lower(:query), '%'))
					"""
	)
	Page<User> findTeamCandidates(
			@Param("excludeId") Long excludeId,
			@Param("excludeRoleCode") String excludeRoleCode,
			@Param("query") String query,
			Pageable pageable
	);

	@Query(
			value = """
					SELECT u
					FROM User u
					JOIN FETCH u.role role
					WHERE u.id NOT IN :excludeIds
					  AND (:search IS NULL OR lower(u.name) LIKE :search OR lower(u.email) LIKE :search)
					""",
			countQuery = """
					SELECT count(u)
					FROM User u
					WHERE u.id NOT IN :excludeIds
					  AND (:search IS NULL OR lower(u.name) LIKE :search OR lower(u.email) LIKE :search)
					"""
	)
	Page<User> findGroupMemberCandidates(
			@Param("excludeIds") Collection<Long> excludeIds,
			@Param("search") String search,
			Pageable pageable
	);

	@Query(
			value = """
					SELECT u
					FROM User u
					JOIN FETCH u.role role
					WHERE role.code IN :roleCodes
					  AND u.id NOT IN :excludeIds
					  AND (:search IS NULL OR lower(u.name) LIKE :search OR lower(u.email) LIKE :search)
					""",
			countQuery = """
					SELECT count(u)
					FROM User u
					JOIN u.role role
					WHERE role.code IN :roleCodes
					  AND u.id NOT IN :excludeIds
					  AND (:search IS NULL OR lower(u.name) LIKE :search OR lower(u.email) LIKE :search)
					"""
	)
	Page<User> findGroupLeadCandidates(
			@Param("roleCodes") Collection<String> roleCodes,
			@Param("excludeIds") Collection<Long> excludeIds,
			@Param("search") String search,
			Pageable pageable
	);

	/**
	 * Loads a page of users who are a member of any of the given groups, excluding one user
	 * (the caller resolving their own team's roster). Each matching user appears at most once
	 * even when they belong to more than one of the given groups, since membership is checked
	 * via an {@code IN} subquery rather than a join to {@code group_members} that would
	 * otherwise multiply rows per group.
	 */
	@Query(
			value = """
					SELECT u
					FROM User u
					JOIN FETCH u.role role
					WHERE u.id IN (SELECT gm.id.memberUserId FROM GroupMember gm WHERE gm.id.groupId IN :groupIds)
					  AND u.id != :excludeId
					  AND (:search IS NULL OR lower(u.name) LIKE :search OR lower(u.email) LIKE :search)
					""",
			countQuery = """
					SELECT count(u)
					FROM User u
					WHERE u.id IN (SELECT gm.id.memberUserId FROM GroupMember gm WHERE gm.id.groupId IN :groupIds)
					  AND u.id != :excludeId
					  AND (:search IS NULL OR lower(u.name) LIKE :search OR lower(u.email) LIKE :search)
					"""
	)
	Page<User> findGroupMemberUsersByGroupIdIn(
			@Param("groupIds") Collection<Long> groupIds,
			@Param("excludeId") Long excludeId,
			@Param("search") String search,
			Pageable pageable
	);

	@Query(
			value = """
					SELECT u
					FROM User u
					JOIN FETCH u.role role
					WHERE (:roleCode IS NULL OR role.code = :roleCode)
					  AND (:search IS NULL OR lower(u.name) LIKE :search OR lower(u.email) LIKE :search)
					ORDER BY CASE role.code WHEN 'admin' THEN 0 WHEN 'teamlead' THEN 1 ELSE 2 END, lower(u.name), lower(u.email)
					""",
			countQuery = """
					SELECT count(u)
					FROM User u
					JOIN u.role role
					WHERE (:roleCode IS NULL OR role.code = :roleCode)
					  AND (:search IS NULL OR lower(u.name) LIKE :search OR lower(u.email) LIKE :search)
					"""
	)
	Page<User> search(
			@Param("roleCode") String roleCode,
			@Param("search") String search,
			Pageable pageable
	);

	long countByRole_Code(String roleCode);
}
