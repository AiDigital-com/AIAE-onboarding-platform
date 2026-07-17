package com.aidigital.aionboarding.service.roadmap.services.impl;

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
import com.aidigital.aionboarding.service.group.services.entity.GroupEntityService;
import com.aidigital.aionboarding.service.group.services.entity.GroupLeadEntityService;
import com.aidigital.aionboarding.service.group.services.entity.GroupMemberEntityService;
import com.aidigital.aionboarding.service.group.support.GroupAccessPolicy;
import com.aidigital.aionboarding.service.learning.models.RoadmapAssignmentEnrollmentRecord;
import com.aidigital.aionboarding.service.mappers.user.UserRecordMapper;
import com.aidigital.aionboarding.service.permission.PermissionKeys;
import com.aidigital.aionboarding.service.permission.services.PermissionService;
import com.aidigital.aionboarding.service.roadmap.models.RoadmapGroupAssignmentPreviewRecord;
import com.aidigital.aionboarding.service.roadmap.models.RoadmapGroupAssignmentRecord;
import com.aidigital.aionboarding.service.roadmap.models.RoadmapGroupAssignmentResultRecord;
import com.aidigital.aionboarding.service.roadmap.services.RoadmapGroupAssignmentService;
import com.aidigital.aionboarding.service.roadmap.services.RoadmapGroupAssignmentSyncService;
import com.aidigital.aionboarding.service.roadmap.services.entity.RoadmapEntityService;
import com.aidigital.aionboarding.service.roadmap.services.entity.RoadmapGroupAssignmentEntityService;
import com.aidigital.aionboarding.service.user.models.UserRecord;
import com.aidigital.aionboarding.service.user.services.entity.UserEntityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RoadmapGroupAssignmentServiceImpl implements RoadmapGroupAssignmentService {

	private final RoadmapGroupAssignmentEntityService roadmapGroupAssignmentEntityService;
	private final RoadmapGroupAssignmentSyncService roadmapGroupAssignmentSyncService;
	private final RoadmapEntityService roadmapEntityService;
	private final GroupEntityService groupEntityService;
	private final GroupLeadEntityService groupLeadEntityService;
	private final GroupMemberEntityService groupMemberEntityService;
	private final GradeEntityService gradeEntityService;
	private final GroupAccessPolicy groupAccessPolicy;
	private final PermissionService permissionService;
	private final UserRecordMapper userMapper;
	private final UserEntityService userEntityService;
	private final CurrentTime currentTime;

	@Override
	@Transactional
	public RoadmapGroupAssignmentResultRecord assignRoadmapToGroup(AppUser actor, Long roadmapId, Long groupId,
																   List<Long> gradeIds) {
		permissionService.requirePermission(actor, PermissionKeys.LEARNING_ASSIGN);
		requireCanManage(actor, groupId, "You can assign roadmaps only to groups you lead.");
		Roadmap roadmap = roadmapEntityService.getReference(roadmapId);
		Group group = requireGroup(groupId);
		Map<Long, Grade> grades = requireExistingGrades(gradeIds);

		RoadmapGroupAssignment assignment = roadmapGroupAssignmentEntityService.findByRoadmapIdAndGroupId(roadmapId,
						groupId)
				.orElseGet(() -> createAssignment(actor, roadmap, group));
		assignment.setUpdatedAt(currentTime.utcDateTime());
		roadmapGroupAssignmentEntityService.save(assignment);
		roadmapGroupAssignmentEntityService.replaceGrades(assignment.getId(), toGradeRows(assignment,
				grades.values()));

		List<RoadmapAssignmentEnrollmentRecord> enrollments =
				roadmapGroupAssignmentSyncService.syncGroupRoadmapEnrollment(groupId, roadmapId, grades.keySet());

		return new RoadmapGroupAssignmentResultRecord(true, toRecord(assignment, grades), enrollments);
	}

	RoadmapGroupAssignment createAssignment(AppUser actor, Roadmap roadmap, Group group) {
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

	@Override
	@Transactional
	public void unassignRoadmapFromGroup(AppUser actor, Long roadmapId, Long groupId) {
		permissionService.requirePermission(actor, PermissionKeys.LEARNING_ASSIGN);
		requireCanManage(actor, groupId, "You can unassign roadmaps only from groups you lead.");
		roadmapGroupAssignmentEntityService.deleteByRoadmapIdAndGroupId(roadmapId, groupId);
	}

	@Override
	@Transactional(readOnly = true)
	public List<RoadmapGroupAssignmentRecord> listAssignments(AppUser viewer, Long roadmapId) {
		permissionService.requirePermission(viewer, PermissionKeys.LEARNING_ASSIGN);
		return roadmapGroupAssignmentEntityService.findByRoadmapId(roadmapId).stream()
				.filter(assignment -> groupAccessPolicy.canManageGroup(viewer, assignment.getGroup().getId()))
				.map(assignment -> toRecord(assignment, gradesOf(assignment)))
				.toList();
	}

	@Override
	@Transactional(readOnly = true)
	public List<RoadmapGroupAssignmentRecord> listAssignmentsForGroup(AppUser viewer, Long groupId) {
		permissionService.requirePermission(viewer, PermissionKeys.LEARNING_ASSIGN);
		requireCanManage(viewer, groupId, "You can view roadmap assignments only for groups you lead.");
		return roadmapGroupAssignmentEntityService.findByGroupId(groupId).stream()
				.map(assignment -> toRecord(assignment, gradesOf(assignment)))
				.toList();
	}

	@Override
	@Transactional(readOnly = true)
	public RoadmapGroupAssignmentPreviewRecord previewAssignment(AppUser viewer, Long groupId, List<Long> gradeIds) {
		requireCanManage(viewer, groupId, "You can preview assignments only for groups you lead.");
		Map<Long, Grade> grades = requireExistingGrades(gradeIds);
		long groupMembersCount = groupMemberEntityService.countByGroupId(groupId);
		long membersWithoutGradeCount = groupMemberEntityService.countMembersWithoutGrade(groupId);
		long membersMatchedCount = grades.isEmpty()
				? groupMembersCount
				: groupMemberEntityService.findByGroupIdAndMemberGradeIdIn(groupId, grades.keySet()).size();
		return new RoadmapGroupAssignmentPreviewRecord(groupMembersCount, membersMatchedCount,
				membersWithoutGradeCount);
	}

	void requireCanManage(AppUser viewer, Long groupId, String forbiddenMessage) {
		if (!groupAccessPolicy.canManageGroup(viewer, groupId)) {
			throw new AppException(ErrorReason.C004, forbiddenMessage);
		}
	}

	Group requireGroup(Long groupId) {
		return groupEntityService.findById(groupId).orElseThrow(() -> new AppException(ErrorReason.C001, "Group not " +
				"found."));
	}

	Map<Long, Grade> requireExistingGrades(List<Long> gradeIds) {
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

	List<RoadmapGroupAssignmentGrade> toGradeRows(RoadmapGroupAssignment assignment, Collection<Grade> grades) {
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

	Map<Long, Grade> gradesOf(RoadmapGroupAssignment assignment) {
		Map<Long, Grade> grades = new LinkedHashMap<>();
		for (RoadmapGroupAssignmentGrade row :
				roadmapGroupAssignmentEntityService.findGradesByAssignmentId(assignment.getId())) {
			grades.put(row.getGrade().getId(), row.getGrade());
		}
		return grades;
	}

	RoadmapGroupAssignmentRecord toRecord(RoadmapGroupAssignment assignment, Map<Long, Grade> grades) {
		Long groupId = assignment.getGroup().getId();
		List<UserRecord> leads = groupLeadEntityService.findByGroupIdIn(List.of(groupId)).stream()
				.map(GroupLead::getLeadUser)
				.map(userMapper::toRecord)
				.toList();
		List<GradeRecord> gradeFilters = grades.values().stream()
				.map(grade -> new GradeRecord(grade.getId(), grade.getCode(), grade.getName(), grade.getDisplayOrder()
						, grade.getIsActive()))
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
}
