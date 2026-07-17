package com.aidigital.aionboarding.domain.team.repositories;

import com.aidigital.aionboarding.domain.team.entities.TeamMember;
import com.aidigital.aionboarding.domain.user.entities.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface TeamMemberRepository extends JpaRepository<TeamMember, TeamMember.TeamMemberId> {

	boolean existsByIdLeadUserIdAndIdMemberUserId(Long leadUserId, Long memberUserId);

	void deleteByIdLeadUserId(Long leadUserId);

	@Query("""
			SELECT tm
			FROM TeamMember tm
			JOIN FETCH tm.memberUser member
			JOIN FETCH member.role
			WHERE tm.id.leadUserId = :leadUserId
			""")
	List<TeamMember> findByLeadUserIdWithMember(@Param("leadUserId") Long leadUserId);

	/**
	 * Loads team memberships for a bounded set of lead user IDs, with member users eagerly
	 * fetched. Used to build a lead-to-members map restricted to the leads currently being
	 * displayed (e.g. one results page) rather than every membership in the workspace.
	 */
	@Query("""
			SELECT tm
			FROM TeamMember tm
			JOIN FETCH tm.memberUser member
			JOIN FETCH member.role
			WHERE tm.id.leadUserId IN :leadUserIds
			""")
	List<TeamMember> findByLeadUserIdIn(@Param("leadUserIds") Collection<Long> leadUserIds);

	@Query("""
			SELECT tm.id.memberUserId
			FROM TeamMember tm
			WHERE tm.id.leadUserId = :leadUserId
			  AND tm.id.memberUserId IN :memberUserIds
			""")
	Set<Long> findMemberUserIdsByLeadUserIdAndMemberUserIds(
			@Param("leadUserId") Long leadUserId,
			@Param("memberUserIds") Collection<Long> memberUserIds
	);

	@Query(
			value = """
					SELECT member
					FROM TeamMember tm
					JOIN tm.memberUser member
					JOIN FETCH member.role role
					WHERE tm.id.leadUserId = :leadUserId
					ORDER BY lower(member.name), lower(member.email)
					""",
			countQuery = """
					SELECT count(member)
					FROM TeamMember tm
					JOIN tm.memberUser member
					WHERE tm.id.leadUserId = :leadUserId
					"""
	)
	Page<User> findMembersByLeadUserId(@Param("leadUserId") Long leadUserId, Pageable pageable);

	@Query(
			value = """
					SELECT member
					FROM TeamMember tm
					JOIN tm.memberUser member
					JOIN FETCH member.role role
					WHERE tm.id.leadUserId = :leadUserId
					  AND (lower(member.name) LIKE concat('%', lower(:query), '%')
					       OR lower(member.email) LIKE concat('%', lower(:query), '%'))
					ORDER BY lower(member.name), lower(member.email)
					""",
			countQuery = """
					SELECT count(member)
					FROM TeamMember tm
					JOIN tm.memberUser member
					WHERE tm.id.leadUserId = :leadUserId
					  AND (lower(member.name) LIKE concat('%', lower(:query), '%')
					       OR lower(member.email) LIKE concat('%', lower(:query), '%'))
					"""
	)
	Page<User> findMembersByLeadUserId(
			@Param("leadUserId") Long leadUserId,
			@Param("query") String query,
			Pageable pageable
	);
}
