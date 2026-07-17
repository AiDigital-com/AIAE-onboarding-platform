package com.aidigital.aionboarding.domain.group.repositories;

import com.aidigital.aionboarding.domain.group.entities.GroupMember;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface GroupMemberRepository extends JpaRepository<GroupMember, GroupMember.GroupMemberId> {

    @Query("""
        SELECT gm
        FROM GroupMember gm
        JOIN FETCH gm.memberUser member
        JOIN FETCH member.role
        LEFT JOIN FETCH member.grade
        WHERE gm.id.groupId = :groupId
        """)
    List<GroupMember> findByIdGroupId(@Param("groupId") Long groupId);

    @Query(
        value = """
            SELECT gm
            FROM GroupMember gm
            JOIN FETCH gm.memberUser member
            JOIN FETCH member.role
            LEFT JOIN FETCH member.grade
            WHERE gm.id.groupId = :groupId
              AND (:search IS NULL OR LOWER(member.name) LIKE :search OR LOWER(member.email) LIKE :search)
            """,
        countQuery = """
            SELECT COUNT(gm)
            FROM GroupMember gm
            JOIN gm.memberUser member
            WHERE gm.id.groupId = :groupId
              AND (:search IS NULL OR LOWER(member.name) LIKE :search OR LOWER(member.email) LIKE :search)
            """
    )
    Page<GroupMember> findByIdGroupId(@Param("groupId") Long groupId, @Param("search") String search, Pageable pageable);

    @Query("SELECT gm.id.groupId FROM GroupMember gm WHERE gm.id.memberUserId = :memberUserId")
    Set<Long> findGroupIdsByIdMemberUserId(@Param("memberUserId") Long memberUserId);

    @Query("""
        SELECT gm
        FROM GroupMember gm
        JOIN FETCH gm.memberUser member
        JOIN FETCH member.role
        LEFT JOIN FETCH member.grade
        WHERE gm.id.groupId IN :groupIds
        """)
    List<GroupMember> findByIdGroupIdIn(@Param("groupIds") Collection<Long> groupIds);

    @Query("""
        SELECT gm
        FROM GroupMember gm
        JOIN FETCH gm.memberUser member
        JOIN FETCH member.role
        LEFT JOIN FETCH member.grade
        """)
    List<GroupMember> findAllWithMembers();

    /**
     * Loads every member of the given group whose current grade id is one of {@code gradeIds}.
     * Used to resolve grade-filtered standing roadmap assignments.
     */
    @Query("""
        SELECT gm
        FROM GroupMember gm
        JOIN gm.memberUser member
        WHERE gm.id.groupId = :groupId
          AND member.grade.id IN :gradeIds
        """)
    List<GroupMember> findByIdGroupIdAndMemberGradeIdIn(
        @Param("groupId") Long groupId,
        @Param("gradeIds") Collection<Long> gradeIds
    );

    boolean existsByIdGroupIdAndIdMemberUserId(Long groupId, Long memberUserId);

    long countByIdGroupId(Long groupId);

    @Query("""
        SELECT COUNT(gm)
        FROM GroupMember gm
        JOIN gm.memberUser member
        WHERE gm.id.groupId = :groupId
          AND member.grade IS NULL
        """)
    long countMembersWithoutGrade(@Param("groupId") Long groupId);

    @Query("""
        SELECT gm.id.groupId, COUNT(gm)
        FROM GroupMember gm
        WHERE gm.id.groupId IN :groupIds
        GROUP BY gm.id.groupId
        """)
    List<Object[]> countByIdGroupIdIn(@Param("groupIds") Collection<Long> groupIds);

    @Query("""
        SELECT gm.id.groupId, COUNT(gm)
        FROM GroupMember gm
        JOIN gm.memberUser member
        WHERE gm.id.groupId IN :groupIds
          AND member.grade IS NULL
        GROUP BY gm.id.groupId
        """)
    List<Object[]> countMembersWithoutGradeByIdGroupIdIn(@Param("groupIds") Collection<Long> groupIds);

    @Query("SELECT COUNT(DISTINCT gm.id.memberUserId) FROM GroupMember gm")
    long countDistinctMemberUsers();

    @Query("SELECT COUNT(DISTINCT gm.id.memberUserId) FROM GroupMember gm WHERE gm.id.groupId IN :groupIds")
    long countDistinctMemberUsersByIdGroupIdIn(@Param("groupIds") Collection<Long> groupIds);
}
