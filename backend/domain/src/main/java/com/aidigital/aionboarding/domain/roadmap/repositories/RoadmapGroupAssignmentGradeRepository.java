package com.aidigital.aionboarding.domain.roadmap.repositories;

import com.aidigital.aionboarding.domain.roadmap.entities.RoadmapGroupAssignmentGrade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RoadmapGroupAssignmentGradeRepository
    extends JpaRepository<RoadmapGroupAssignmentGrade, RoadmapGroupAssignmentGrade.RoadmapGroupAssignmentGradeId> {

    @Query("""
        SELECT g
        FROM RoadmapGroupAssignmentGrade g
        JOIN FETCH g.grade
        WHERE g.id.assignmentId = :assignmentId
        """)
    List<RoadmapGroupAssignmentGrade> findByIdAssignmentId(@Param("assignmentId") Long assignmentId);

    void deleteByIdAssignmentId(Long assignmentId);
}
