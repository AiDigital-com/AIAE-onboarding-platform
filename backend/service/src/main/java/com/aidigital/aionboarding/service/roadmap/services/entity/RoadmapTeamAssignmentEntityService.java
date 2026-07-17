package com.aidigital.aionboarding.service.roadmap.services.entity;

import com.aidigital.aionboarding.domain.roadmap.entities.RoadmapTeamAssignment;
import com.aidigital.aionboarding.domain.roadmap.repositories.RoadmapTeamAssignmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Short-transaction CRUD helpers for the {@link RoadmapTeamAssignment} entity.
 * <p>
 * This is the only service that may inject {@link RoadmapTeamAssignmentRepository} directly.
 * All other services that require roadmap-team-assignment data must depend on this service.
 */
@Service
@RequiredArgsConstructor
public class RoadmapTeamAssignmentEntityService {

    private final RoadmapTeamAssignmentRepository roadmapTeamAssignmentRepository;

    /**
     * Loads every standing team assignment for a roadmap, with each assignment's lead user fetched.
     *
     * @param roadmapId roadmap primary key
     * @return matching assignments
     */
    @Transactional(readOnly = true)
    public List<RoadmapTeamAssignment> findByRoadmapId(Long roadmapId) {
        return roadmapTeamAssignmentRepository.findByRoadmapId(roadmapId);
    }

    /**
     * Loads every standing roadmap assignment for a team.
     *
     * @param leadUserId team lead primary key identifying the team
     * @return matching assignments
     */
    @Transactional(readOnly = true)
    public List<RoadmapTeamAssignment> findByLeadUserId(Long leadUserId) {
        return roadmapTeamAssignmentRepository.findByLeadUserId(leadUserId);
    }

    /**
     * Loads one roadmap's standing assignment to one team, if present.
     *
     * @param roadmapId  roadmap primary key
     * @param leadUserId team lead primary key identifying the team
     * @return the matching assignment, when present
     */
    @Transactional(readOnly = true)
    public Optional<RoadmapTeamAssignment> findByRoadmapIdAndLeadUserId(Long roadmapId, Long leadUserId) {
        return roadmapTeamAssignmentRepository.findByRoadmapIdAndLeadUserId(roadmapId, leadUserId);
    }

    /**
     * Persists a roadmap-team assignment.
     *
     * @param assignment the assignment to save
     * @return the saved {@link RoadmapTeamAssignment}
     */
    @Transactional
    public RoadmapTeamAssignment save(RoadmapTeamAssignment assignment) {
        return roadmapTeamAssignmentRepository.save(assignment);
    }

    /**
     * Deletes the standing assignment between one roadmap and one team, if present.
     *
     * @param roadmapId  roadmap primary key
     * @param leadUserId team lead primary key identifying the team
     */
    @Transactional
    public void deleteByRoadmapIdAndLeadUserId(Long roadmapId, Long leadUserId) {
        roadmapTeamAssignmentRepository.deleteByRoadmapIdAndLeadUserId(roadmapId, leadUserId);
    }
}
