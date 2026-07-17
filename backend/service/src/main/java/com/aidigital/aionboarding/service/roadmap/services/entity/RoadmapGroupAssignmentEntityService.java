package com.aidigital.aionboarding.service.roadmap.services.entity;

import com.aidigital.aionboarding.domain.roadmap.entities.RoadmapGroupAssignment;
import com.aidigital.aionboarding.domain.roadmap.entities.RoadmapGroupAssignmentGrade;
import com.aidigital.aionboarding.domain.roadmap.repositories.RoadmapGroupAssignmentGradeRepository;
import com.aidigital.aionboarding.domain.roadmap.repositories.RoadmapGroupAssignmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Short-transaction CRUD helpers for the {@link RoadmapGroupAssignment} aggregate, including its
 * {@link RoadmapGroupAssignmentGrade} filter rows.
 * <p>
 * This is the only service that may inject {@link RoadmapGroupAssignmentRepository} or
 * {@link RoadmapGroupAssignmentGradeRepository} directly. All other services that require
 * roadmap-group-assignment data must depend on this service.
 */
@Service
@RequiredArgsConstructor
public class RoadmapGroupAssignmentEntityService {

    private final RoadmapGroupAssignmentRepository roadmapGroupAssignmentRepository;
    private final RoadmapGroupAssignmentGradeRepository roadmapGroupAssignmentGradeRepository;

    /**
     * Loads every standing group assignment for a roadmap, with each assignment's group fetched.
     *
     * @param roadmapId roadmap primary key
     * @return matching assignments
     */
    @Transactional(readOnly = true)
    public List<RoadmapGroupAssignment> findByRoadmapId(Long roadmapId) {
        return roadmapGroupAssignmentRepository.findByRoadmapId(roadmapId);
    }

    /**
     * Loads every standing assignment for one group, with each assignment's roadmap fetched.
     *
     * @param groupId group primary key
     * @return matching assignments
     */
    @Transactional(readOnly = true)
    public List<RoadmapGroupAssignment> findByGroupId(Long groupId) {
        return roadmapGroupAssignmentRepository.findByGroupId(groupId);
    }

    /**
     * Loads one roadmap's standing assignment to one group, if present.
     *
     * @param roadmapId roadmap primary key
     * @param groupId   group primary key
     * @return the matching assignment, when present
     */
    @Transactional(readOnly = true)
    public Optional<RoadmapGroupAssignment> findByRoadmapIdAndGroupId(Long roadmapId, Long groupId) {
        return roadmapGroupAssignmentRepository.findByRoadmapIdAndGroupId(roadmapId, groupId);
    }

    /**
     * Loads every standing assignment for any of the given groups.
     *
     * @param groupIds group primary keys
     * @return matching assignments
     */
    @Transactional(readOnly = true)
    public List<RoadmapGroupAssignment> findByGroupIdIn(Collection<Long> groupIds) {
        if (groupIds == null || groupIds.isEmpty()) {
            return List.of();
        }
        return roadmapGroupAssignmentRepository.findByGroupIdIn(groupIds);
    }

    /**
     * Persists a roadmap-group assignment.
     *
     * @param assignment the assignment to save
     * @return the saved {@link RoadmapGroupAssignment}
     */
    @Transactional
    public RoadmapGroupAssignment save(RoadmapGroupAssignment assignment) {
        return roadmapGroupAssignmentRepository.save(assignment);
    }

    /**
     * Deletes the standing assignment between one roadmap and one group, if present.
     *
     * @param roadmapId roadmap primary key
     * @param groupId   group primary key
     */
    @Transactional
    public void deleteByRoadmapIdAndGroupId(Long roadmapId, Long groupId) {
        roadmapGroupAssignmentRepository.deleteByRoadmapIdAndGroupId(roadmapId, groupId);
    }

    /**
     * Loads the grade filter rows for an assignment, with each grade fetched.
     *
     * @param assignmentId assignment primary key
     * @return matching grade filter rows
     */
    @Transactional(readOnly = true)
    public List<RoadmapGroupAssignmentGrade> findGradesByAssignmentId(Long assignmentId) {
        return roadmapGroupAssignmentGradeRepository.findByIdAssignmentId(assignmentId);
    }

    /**
     * Replaces an assignment's grade filter rows.
     *
     * @param assignmentId assignment primary key
     * @param grades       new grade filter rows to persist (already built for this assignment)
     */
    @Transactional
    public void replaceGrades(Long assignmentId, List<RoadmapGroupAssignmentGrade> grades) {
        roadmapGroupAssignmentGradeRepository.deleteByIdAssignmentId(assignmentId);
        if (!grades.isEmpty()) {
            roadmapGroupAssignmentGradeRepository.saveAll(grades);
        }
    }
}
