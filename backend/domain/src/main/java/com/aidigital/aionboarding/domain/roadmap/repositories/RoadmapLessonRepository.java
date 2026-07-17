package com.aidigital.aionboarding.domain.roadmap.repositories;

import com.aidigital.aionboarding.domain.roadmap.entities.RoadmapLesson;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RoadmapLessonRepository extends JpaRepository<RoadmapLesson, RoadmapLesson.RoadmapLessonId> {

    List<RoadmapLesson> findByIdRoadmapIdOrderBySortOrderAsc(Long roadmapId);

    List<RoadmapLesson> findByIdRoadmapIdInOrderBySortOrderAsc(List<Long> roadmapIds);

    List<RoadmapLesson> findByIdLessonId(Long lessonId);

    void deleteByIdRoadmapId(Long roadmapId);

    @Query("SELECT rl FROM RoadmapLesson rl " +
           "JOIN FETCH rl.lesson l " +
           "JOIN FETCH l.status " +
           "WHERE rl.id.roadmapId IN :roadmapIds " +
           "ORDER BY rl.sortOrder ASC")
    List<RoadmapLesson> findAllByRoadmapIdsWithLessons(@Param("roadmapIds") List<Long> roadmapIds);
}
