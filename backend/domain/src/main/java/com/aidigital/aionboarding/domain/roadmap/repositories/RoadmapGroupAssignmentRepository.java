package com.aidigital.aionboarding.domain.roadmap.repositories;

import com.aidigital.aionboarding.domain.roadmap.entities.RoadmapGroupAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface RoadmapGroupAssignmentRepository extends JpaRepository<RoadmapGroupAssignment, Long> {

    @Query("""
        SELECT a
        FROM RoadmapGroupAssignment a
        JOIN FETCH a.group g
        WHERE a.roadmap.id = :roadmapId
        """)
    List<RoadmapGroupAssignment> findByRoadmapId(@Param("roadmapId") Long roadmapId);

    Optional<RoadmapGroupAssignment> findByRoadmapIdAndGroupId(Long roadmapId, Long groupId);

    @Query("""
        SELECT a
        FROM RoadmapGroupAssignment a
        WHERE a.group.id IN :groupIds
        """)
    List<RoadmapGroupAssignment> findByGroupIdIn(@Param("groupIds") Collection<Long> groupIds);

    @Query("""
        SELECT a
        FROM RoadmapGroupAssignment a
        JOIN FETCH a.roadmap r
        WHERE a.group.id = :groupId
        """)
    List<RoadmapGroupAssignment> findByGroupId(@Param("groupId") Long groupId);

    void deleteByRoadmapIdAndGroupId(Long roadmapId, Long groupId);
}
