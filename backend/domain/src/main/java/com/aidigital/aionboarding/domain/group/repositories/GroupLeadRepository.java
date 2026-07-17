package com.aidigital.aionboarding.domain.group.repositories;

import com.aidigital.aionboarding.domain.group.entities.GroupLead;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface GroupLeadRepository extends JpaRepository<GroupLead, GroupLead.GroupLeadId> {

    @Query("""
        SELECT gl
        FROM GroupLead gl
        JOIN FETCH gl.leadUser lead
        JOIN FETCH lead.role
        WHERE gl.id.groupId IN :groupIds
        """)
    List<GroupLead> findByIdGroupIdIn(@Param("groupIds") Collection<Long> groupIds);

    @Query("SELECT gl.id.groupId FROM GroupLead gl WHERE gl.id.leadUserId = :leadUserId")
    Set<Long> findGroupIdsByIdLeadUserId(@Param("leadUserId") Long leadUserId);

    boolean existsByIdGroupIdAndIdLeadUserId(Long groupId, Long leadUserId);

    long countByIdGroupId(Long groupId);

    @Query("SELECT COUNT(DISTINCT gl.id.leadUserId) FROM GroupLead gl")
    long countDistinctLeadUsers();

    @Query("SELECT COUNT(DISTINCT gl.id.leadUserId) FROM GroupLead gl WHERE gl.id.groupId IN :groupIds")
    long countDistinctLeadUsersByIdGroupIdIn(@Param("groupIds") Collection<Long> groupIds);
}
