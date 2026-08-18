package com.aidigital.aionboarding.service.roadmap.support;

import com.aidigital.aionboarding.domain.grade.entities.Grade;
import com.aidigital.aionboarding.domain.group.entities.Group;
import com.aidigital.aionboarding.domain.group.entities.GroupLead;
import com.aidigital.aionboarding.domain.roadmap.entities.Roadmap;
import com.aidigital.aionboarding.domain.roadmap.entities.RoadmapGroupAssignment;
import com.aidigital.aionboarding.domain.roadmap.entities.RoadmapGroupAssignmentGrade;
import com.aidigital.aionboarding.domain.user.entities.User;
import com.aidigital.aionboarding.service.common.error.AppException;
import com.aidigital.aionboarding.service.common.error.ErrorReason;
import com.aidigital.aionboarding.service.common.security.AppUser;
import com.aidigital.aionboarding.service.common.time.CurrentTime;
import com.aidigital.aionboarding.service.grade.models.GradeRecord;
import com.aidigital.aionboarding.service.grade.services.entity.GradeEntityService;
import com.aidigital.aionboarding.service.group.services.entity.GroupLeadEntityService;
import com.aidigital.aionboarding.service.group.services.entity.GroupMemberEntityService;
import com.aidigital.aionboarding.service.mappers.user.UserRecordMapper;
import com.aidigital.aionboarding.service.roadmap.models.RoadmapGroupAssignmentPreviewRecord;
import com.aidigital.aionboarding.service.roadmap.models.RoadmapGroupAssignmentRecord;
import com.aidigital.aionboarding.service.roadmap.services.entity.RoadmapGroupAssignmentEntityService;
import com.aidigital.aionboarding.service.user.models.UserRecord;
import com.aidigital.aionboarding.service.user.services.entity.UserEntityService;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Builds and assembles {@link RoadmapGroupAssignment} entities and their API record shape:
 * grade lookup/validation, grade-row construction, member-count previews, and entity<->record
 * conversion for {@code RoadmapGroupAssignmentServiceImpl}.
 */
@Component
@RequiredArgsConstructor
public class RoadmapGroupAssignmentRecordAssembler {

    private final RoadmapGroupAssignmentEntityService roadmapGroupAssignmentEntityService;
    private final GroupLeadEntityService groupLeadEntityService;
    private final GroupMemberEntityService groupMemberEntityService;
    private final GradeEntityService gradeEntityService;
    private final UserRecordMapper userMapper;
    private final UserEntityService userEntityService;
    private final CurrentTime currentTime;

    /**
     * Creates and persists a new, unassigned group-roadmap assignment.
     *
     * @param actor   user performing the assignment, or {@code null} for a system actor
     * @param roadmap the roadmap being assigned
     * @param group   the group it is assigned to
     * @return the persisted assignment
     */
    public RoadmapGroupAssignment createAssignment(AppUser actor, Roadmap roadmap, Group group) {
        RoadmapGroupAssignment assignment = new RoadmapGroupAssignment();
        assignment.setRoadmap(roadmap);
        assignment.setGroup(group);
        assignment.setAssignedByUser(actor == null || actor.internalId() == null ? null :
                userEntityService.getReference(actor.internalId()));
        LocalDateTime now = currentTime.utcDateTime();
        assignment.setCreatedAt(now);
        assignment.setUpdatedAt(now);
        return roadmapGroupAssignmentEntityService.save(assignment);
    }

    /**
     * Stamps an assignment's updated-at timestamp to now, in place.
     *
     * @param assignment the assignment being re-saved
     */
    public void touchUpdatedAt(RoadmapGroupAssignment assignment) {
        assignment.setUpdatedAt(currentTime.utcDateTime());
    }

    /**
     * Resolves and validates that every requested grade id exists.
     *
     * @param gradeIds requested grade filter ids, or {@code null}/empty for no filter
     * @return grades keyed by id, in request order with duplicates removed
     * @throws AppException with reason {@code C001} when any grade id does not exist
     */
    public Map<Long, Grade> requireExistingGrades(List<Long> gradeIds) {
        if (gradeIds == null || gradeIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, Grade> grades = new LinkedHashMap<>();
        for (Long gradeId : new LinkedHashSet<>(gradeIds)) {
            Grade grade = gradeEntityService.findById(gradeId)
                    .orElseThrow(() -> new AppException(ErrorReason.C001, "Grade not found: " + gradeId));
            grades.put(gradeId, grade);
        }
        return grades;
    }

    /**
     * Builds the join-table rows linking an assignment to its grade filters.
     *
     * @param assignment the assignment the rows belong to
     * @param grades     the grade filters to link
     * @return the constructed (not yet persisted) rows
     */
    public List<RoadmapGroupAssignmentGrade> toGradeRows(RoadmapGroupAssignment assignment, Collection<Grade> grades) {
        List<RoadmapGroupAssignmentGrade> rows = new ArrayList<>();
        for (Grade grade : grades) {
            RoadmapGroupAssignmentGrade row = new RoadmapGroupAssignmentGrade();
            RoadmapGroupAssignmentGrade.RoadmapGroupAssignmentGradeId id =
                    new RoadmapGroupAssignmentGrade.RoadmapGroupAssignmentGradeId();
            id.setAssignmentId(assignment.getId());
            id.setGradeId(grade.getId());
            row.setId(id);
            row.setAssignment(assignment);
            row.setGrade(grade);
            rows.add(row);
        }
        return rows;
    }

    /**
     * Loads an assignment's current grade filters, keyed by grade id.
     *
     * @param assignment the assignment to load grade filters for
     * @return grades keyed by id
     */
    public Map<Long, Grade> gradesOf(RoadmapGroupAssignment assignment) {
        Map<Long, Grade> grades = new LinkedHashMap<>();
        for (RoadmapGroupAssignmentGrade row :
                roadmapGroupAssignmentEntityService.findGradesByAssignmentId(assignment.getId())) {
            grades.put(row.getGrade().getId(), row.getGrade());
        }
        return grades;
    }

    /**
     * Assembles the API record for an assignment, including its group's leads, matched grade
     * filters, and member counts.
     *
     * @param assignment the assignment to assemble
     * @param grades     the assignment's grade filters
     * @return the assembled record
     */
    public RoadmapGroupAssignmentRecord toRecord(RoadmapGroupAssignment assignment, Map<Long, Grade> grades) {
        Long groupId = assignment.getGroup().getId();
        List<UserRecord> leads = groupLeadEntityService.findByGroupIdIn(List.of(groupId)).stream()
                .map(GroupLead::getLeadUser)
                .map(userMapper::toRecord)
                .toList();
        List<GradeRecord> gradeFilters = grades.values().stream()
                .map(grade -> new GradeRecord(grade.getId(), grade.getCode(), grade.getName(), grade.getDisplayOrder(),
                        grade.getIsActive()))
                .toList();
        long membersMatchedCount = grades.isEmpty()
                ? groupMemberEntityService.countByGroupId(groupId)
                : groupMemberEntityService.findByGroupIdAndMemberGradeIdIn(groupId, grades.keySet()).size();
        long membersWithoutGradeCount = groupMemberEntityService.countMembersWithoutGrade(groupId);
        User assignedBy = assignment.getAssignedByUser();

        return new RoadmapGroupAssignmentRecord(
                assignment.getId(),
                assignment.getRoadmap().getId(),
                assignment.getRoadmap().getTitle(),
                groupId,
                assignment.getGroup().getName(),
                leads,
                gradeFilters,
                membersMatchedCount,
                membersWithoutGradeCount,
                assignedBy == null ? null : assignedBy.getId(),
                assignedBy == null ? null : assignedBy.getName(),
                assignment.getCreatedAt()
        );
    }

    /**
     * Computes how many of a group's members would be matched by a prospective grade filter,
     * without creating or modifying any assignment.
     *
     * @param groupId  the group being previewed
     * @param gradeIds requested grade filter ids, or {@code null}/empty for no filter
     * @return the preview counts
     * @throws AppException with reason {@code C001} when any grade id does not exist
     */
    public RoadmapGroupAssignmentPreviewRecord previewAssignment(Long groupId, List<Long> gradeIds) {
        Map<Long, Grade> grades = requireExistingGrades(gradeIds);
        long groupMembersCount = groupMemberEntityService.countByGroupId(groupId);
        long membersWithoutGradeCount = groupMemberEntityService.countMembersWithoutGrade(groupId);
        long membersMatchedCount = grades.isEmpty()
                ? groupMembersCount
                : groupMemberEntityService.findByGroupIdAndMemberGradeIdIn(groupId, grades.keySet()).size();
        return new RoadmapGroupAssignmentPreviewRecord(groupMembersCount, membersMatchedCount,
                membersWithoutGradeCount);
    }
}
