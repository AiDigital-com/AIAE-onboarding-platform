package com.aidigital.aionboarding.domain.roadmap.repositories;

import com.aidigital.aionboarding.domain.roadmap.entities.RoadmapTeamAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RoadmapTeamAssignmentRepository extends JpaRepository<RoadmapTeamAssignment, Long> {

	@Query("""
			SELECT a
			FROM RoadmapTeamAssignment a
			JOIN FETCH a.leadUser lead
			WHERE a.roadmap.id = :roadmapId
			""")
	List<RoadmapTeamAssignment> findByRoadmapId(@Param("roadmapId") Long roadmapId);

	List<RoadmapTeamAssignment> findByLeadUserId(Long leadUserId);

	Optional<RoadmapTeamAssignment> findByRoadmapIdAndLeadUserId(Long roadmapId, Long leadUserId);

	void deleteByRoadmapIdAndLeadUserId(Long roadmapId, Long leadUserId);
}
