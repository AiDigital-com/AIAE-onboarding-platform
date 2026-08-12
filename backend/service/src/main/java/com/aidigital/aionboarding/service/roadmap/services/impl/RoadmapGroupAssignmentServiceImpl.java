package com.aidigital.aionboarding.service.roadmap.services.impl;

import com.aidigital.aionboarding.domain.grade.entities.Grade;
import com.aidigital.aionboarding.domain.group.entities.Group;
import com.aidigital.aionboarding.domain.roadmap.entities.Roadmap;
import com.aidigital.aionboarding.domain.roadmap.entities.RoadmapGroupAssignment;
import com.aidigital.aionboarding.service.common.error.AppException;
import com.aidigital.aionboarding.service.common.error.ErrorReason;
import com.aidigital.aionboarding.service.common.security.AppUser;
import com.aidigital.aionboarding.service.group.services.entity.GroupEntityService;
import com.aidigital.aionboarding.service.group.support.GroupAccessPolicy;
import com.aidigital.aionboarding.service.learning.models.RoadmapAssignmentEnrollmentRecord;
import com.aidigital.aionboarding.service.permission.PermissionKeys;
import com.aidigital.aionboarding.service.permission.services.PermissionService;
import com.aidigital.aionboarding.service.roadmap.models.RoadmapGroupAssignmentPreviewRecord;
import com.aidigital.aionboarding.service.roadmap.models.RoadmapGroupAssignmentRecord;
import com.aidigital.aionboarding.service.roadmap.models.RoadmapGroupAssignmentResultRecord;
import com.aidigital.aionboarding.service.roadmap.services.RoadmapGroupAssignmentService;
import com.aidigital.aionboarding.service.roadmap.services.RoadmapGroupAssignmentSyncService;
import com.aidigital.aionboarding.service.roadmap.services.entity.RoadmapEntityService;
import com.aidigital.aionboarding.service.roadmap.services.entity.RoadmapGroupAssignmentEntityService;
import com.aidigital.aionboarding.service.roadmap.support.RoadmapGroupAssignmentRecordAssembler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RoadmapGroupAssignmentServiceImpl implements RoadmapGroupAssignmentService {

	private final RoadmapGroupAssignmentEntityService roadmapGroupAssignmentEntityService;
	private final RoadmapGroupAssignmentSyncService roadmapGroupAssignmentSyncService;
	private final RoadmapEntityService roadmapEntityService;
	private final GroupEntityService groupEntityService;
	private final GroupAccessPolicy groupAccessPolicy;
	private final PermissionService permissionService;
	private final RoadmapGroupAssignmentRecordAssembler roadmapGroupAssignmentRecordAssembler;

	@Override
	@Transactional
	public RoadmapGroupAssignmentResultRecord assignRoadmapToGroup(AppUser actor, Long roadmapId, Long groupId,
																   List<Long> gradeIds) {
		permissionService.requirePermission(actor, PermissionKeys.LEARNING_ASSIGN);
		requireCanManage(actor, groupId, "You can assign roadmaps only to groups you lead.");
		Roadmap roadmap = roadmapEntityService.getReference(roadmapId);
		Group group = requireGroup(groupId);
		Map<Long, Grade> grades = roadmapGroupAssignmentRecordAssembler.requireExistingGrades(gradeIds);

		RoadmapGroupAssignment assignment = roadmapGroupAssignmentEntityService.findByRoadmapIdAndGroupId(roadmapId,
						groupId)
				.orElseGet(() -> roadmapGroupAssignmentRecordAssembler.createAssignment(actor, roadmap, group));
		roadmapGroupAssignmentRecordAssembler.touchUpdatedAt(assignment);
		roadmapGroupAssignmentEntityService.save(assignment);
		roadmapGroupAssignmentEntityService.replaceGrades(assignment.getId(),
				roadmapGroupAssignmentRecordAssembler.toGradeRows(assignment, grades.values()));

		List<RoadmapAssignmentEnrollmentRecord> enrollments =
				roadmapGroupAssignmentSyncService.syncGroupRoadmapEnrollment(groupId, roadmapId, grades.keySet());

		return new RoadmapGroupAssignmentResultRecord(
				true, roadmapGroupAssignmentRecordAssembler.toRecord(assignment, grades), enrollments);
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
				.map(assignment -> roadmapGroupAssignmentRecordAssembler.toRecord(assignment,
						roadmapGroupAssignmentRecordAssembler.gradesOf(assignment)))
				.toList();
	}

	@Override
	@Transactional(readOnly = true)
	public List<RoadmapGroupAssignmentRecord> listAssignmentsForGroup(AppUser viewer, Long groupId) {
		permissionService.requirePermission(viewer, PermissionKeys.LEARNING_ASSIGN);
		requireCanManage(viewer, groupId, "You can view roadmap assignments only for groups you lead.");
		return roadmapGroupAssignmentEntityService.findByGroupId(groupId).stream()
				.map(assignment -> roadmapGroupAssignmentRecordAssembler.toRecord(assignment,
						roadmapGroupAssignmentRecordAssembler.gradesOf(assignment)))
				.toList();
	}

	@Override
	@Transactional(readOnly = true)
	public RoadmapGroupAssignmentPreviewRecord previewAssignment(AppUser viewer, Long groupId, List<Long> gradeIds) {
		requireCanManage(viewer, groupId, "You can preview assignments only for groups you lead.");
		return roadmapGroupAssignmentRecordAssembler.previewAssignment(groupId, gradeIds);
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
}
