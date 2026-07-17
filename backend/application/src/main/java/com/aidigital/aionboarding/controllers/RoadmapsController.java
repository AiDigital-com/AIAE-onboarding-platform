package com.aidigital.aionboarding.controllers;

import com.aidigital.aionboarding.api.v1.RoadmapsApi;
import com.aidigital.aionboarding.api.v1.model.AssignRoadmapToGroupRequestV1;
import com.aidigital.aionboarding.api.v1.model.AssignRoadmapToTeamRequestV1;
import com.aidigital.aionboarding.api.v1.model.AssignmentRequestV1;
import com.aidigital.aionboarding.api.v1.model.AssignmentResponseV1;
import com.aidigital.aionboarding.api.v1.model.CountResponseV1;
import com.aidigital.aionboarding.api.v1.model.CreateRoadmapRequestV1;
import com.aidigital.aionboarding.api.v1.model.EnrollmentResponseV1;
import com.aidigital.aionboarding.api.v1.model.LearningAssigneesResponseV1;
import com.aidigital.aionboarding.api.v1.model.OkIdResponseV1;
import com.aidigital.aionboarding.api.v1.model.OkResponseV1;
import com.aidigital.aionboarding.api.v1.model.RoadmapGroupAssignmentPreviewRequestV1;
import com.aidigital.aionboarding.api.v1.model.RoadmapGroupAssignmentPreviewResponseV1;
import com.aidigital.aionboarding.api.v1.model.RoadmapGroupAssignmentResponseV1;
import com.aidigital.aionboarding.api.v1.model.RoadmapGroupAssignmentsListResponseV1;
import com.aidigital.aionboarding.api.v1.model.RoadmapResponseV1;
import com.aidigital.aionboarding.api.v1.model.RoadmapTeamAssignmentResponseV1;
import com.aidigital.aionboarding.api.v1.model.RoadmapTeamAssignmentsListResponseV1;
import com.aidigital.aionboarding.api.v1.model.RoadmapsListResponseV1;
import com.aidigital.aionboarding.api.v1.model.SearchRoadmapsV1;
import com.aidigital.aionboarding.api.v1.model.UpdateRoadmapRequestV1;
import com.aidigital.aionboarding.mappers.learning.LearningApiMapper;
import com.aidigital.aionboarding.mappers.roadmap.RoadmapApiMapper;
import com.aidigital.aionboarding.mappers.roadmap.RoadmapGroupAssignmentApiMapper;
import com.aidigital.aionboarding.service.common.security.AppUser;
import com.aidigital.aionboarding.service.learning.services.LearningService;
import com.aidigital.aionboarding.service.permission.PermissionKeys;
import com.aidigital.aionboarding.service.roadmap.services.RoadmapGroupAssignmentService;
import com.aidigital.aionboarding.service.roadmap.services.RoadmapService;
import com.aidigital.aionboarding.support.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class RoadmapsController implements RoadmapsApi {

	private final CurrentUserSupport currentUser;
	private final RoadmapService roadmapService;
	private final LearningService learningService;
	private final RoadmapGroupAssignmentService roadmapGroupAssignmentService;
	private final RoadmapApiMapper roadmapApiMapper;
	private final LearningApiMapper learningApiMapper;
	private final RoadmapGroupAssignmentApiMapper roadmapGroupAssignmentApiMapper;
	private final ApiResponses apiResponses;

	@Override
	@Transactional(readOnly = true)
	public ResponseEntity<RoadmapsListResponseV1> searchRoadmaps(SearchRoadmapsV1 request) {
		AppUser viewer = currentUser.requireUser();
		return ResponseEntity.ok(
				roadmapApiMapper.toRoadmapsListResponseV1(
						roadmapService.getAllRoadmaps(
								viewer,
								roadmapApiMapper.toRoadmapListQuery(request),
								roadmapApiMapper.page(request),
								roadmapApiMapper.size(request)
						))
		);
	}

	@Override
	@Transactional(readOnly = true)
	public ResponseEntity<CountResponseV1> countRoadmaps(SearchRoadmapsV1 request) {
		AppUser viewer = currentUser.requireUser();
		long totalElements = roadmapService.countRoadmaps(viewer, roadmapApiMapper.toRoadmapListQuery(request));
		return ResponseEntity.ok(roadmapApiMapper.toCountResponseV1(totalElements));
	}

	@Override
	@PreAuthorize("@perm.has('" + PermissionKeys.ROADMAPS_CREATE + "')")
	@Transactional
	public ResponseEntity<RoadmapResponseV1> createRoadmap(CreateRoadmapRequestV1 request) {
		AppUser viewer = currentUser.requireUser();
		return ResponseEntity.status(HttpStatus.CREATED).body(roadmapApiMapper.toRoadmapResponseV1(
				roadmapService.createRoadmap(viewer, roadmapApiMapper.toCreateRoadmapInput(request))));
	}

	@Override
	@PreAuthorize("@perm.has('" + PermissionKeys.ROADMAPS_MANAGE + "')")
	@Transactional
	public ResponseEntity<RoadmapResponseV1> updateRoadmap(Long id, UpdateRoadmapRequestV1 request) {
		AppUser viewer = currentUser.requireUser();
		return ResponseEntity.ok(roadmapApiMapper.toRoadmapResponseV1(
				roadmapService.updateRoadmap(viewer, id, roadmapApiMapper.toUpdateRoadmapInput(request))));
	}

	@Override
	@PreAuthorize("@perm.has('" + PermissionKeys.ROADMAPS_MANAGE + "')")
	@Transactional
	public ResponseEntity<OkIdResponseV1> deleteRoadmap(Long id) {
		roadmapService.deleteRoadmap(currentUser.requireUser(), id);
		return ResponseEntity.ok(apiResponses.okId(id));
	}

	@Override
	@PreAuthorize("@perm.has('" + PermissionKeys.LEARNING_ASSIGN + "')")
	@Transactional
	public ResponseEntity<AssignmentResponseV1> assignRoadmap(Long id, AssignmentRequestV1 request) {
		return ResponseEntity.ok(learningApiMapper.toRoadmapAssignmentResponseV1(
				learningService.assignRoadmap(currentUser.requireUser(), id, request.getUserIds())
		));
	}

	@Override
	@PreAuthorize("@perm.has('" + PermissionKeys.LEARNING_ASSIGN + "')")
	@Transactional(readOnly = true)
	public ResponseEntity<LearningAssigneesResponseV1> listRoadmapAssignees(Long id) {
		return ResponseEntity.ok(learningApiMapper.toLearningAssigneesResponseV1(
				learningService.listRoadmapAssignees(currentUser.requireUser(), id)
		));
	}

	@Override
	@PreAuthorize("@perm.has('" + PermissionKeys.LEARNING_ASSIGN + "')")
	@Transactional
	public ResponseEntity<OkResponseV1> revokeRoadmapAssignments(Long id, AssignmentRequestV1 request) {
		learningService.revokeRoadmapAssignments(currentUser.requireUser(), id, request.getUserIds());
		return ResponseEntity.ok(apiResponses.ok());
	}

	@Override
	@PreAuthorize("@perm.has('" + PermissionKeys.LEARNING_ENROLL + "')")
	@Transactional
	public ResponseEntity<EnrollmentResponseV1> enrollRoadmap(Long id) {
		return ResponseEntity.ok(learningApiMapper.toRoadmapEnrollmentResponseV1(
				learningService.enrollRoadmap(currentUser.requireUser(), id)
		));
	}

	@Override
	@PreAuthorize("@perm.has('" + PermissionKeys.LEARNING_ENROLL + "')")
	@Transactional
	public ResponseEntity<OkResponseV1> unenrollRoadmap(Long id) {
		learningService.unenrollRoadmap(currentUser.requireUser(), id);
		return ResponseEntity.ok(apiResponses.ok());
	}

	@Override
	@PreAuthorize("@perm.has('" + PermissionKeys.LEARNING_ASSIGN + "')")
	@Transactional(readOnly = true)
	public ResponseEntity<RoadmapTeamAssignmentsListResponseV1> listRoadmapTeamAssignments(Long id) {
		return ResponseEntity.ok(learningApiMapper.toRoadmapTeamAssignmentsListResponseV1(
				learningService.getRoadmapTeamAssignments(currentUser.requireUser(), id)
		));
	}

	@Override
	@PreAuthorize("@perm.has('" + PermissionKeys.LEARNING_ASSIGN + "')")
	@Transactional
	public ResponseEntity<RoadmapTeamAssignmentResponseV1> assignRoadmapToTeam(Long id,
																			   AssignRoadmapToTeamRequestV1 request) {
		return ResponseEntity.ok(learningApiMapper.toRoadmapTeamAssignmentResponseV1(
				learningService.assignRoadmapToGroup(currentUser.requireUser(), id, request.getLeadUserId())
		));
	}

	@Override
	@PreAuthorize("@perm.has('" + PermissionKeys.LEARNING_ASSIGN + "')")
	@Transactional
	public ResponseEntity<OkResponseV1> unassignRoadmapFromTeam(Long id, Long leadUserId) {
		learningService.unassignRoadmapFromGroup(currentUser.requireUser(), id, leadUserId);
		return ResponseEntity.ok(apiResponses.ok());
	}

	@Override
	@PreAuthorize("@perm.has('" + PermissionKeys.LEARNING_ASSIGN + "')")
	@Transactional(readOnly = true)
	public ResponseEntity<RoadmapGroupAssignmentsListResponseV1> listRoadmapGroupAssignments(Long id) {
		return ResponseEntity.ok(roadmapGroupAssignmentApiMapper.toRoadmapGroupAssignmentsListResponseV1(
				roadmapGroupAssignmentService.listAssignments(currentUser.requireUser(), id)
		));
	}

	@Override
	@PreAuthorize("@perm.has('" + PermissionKeys.LEARNING_ASSIGN + "')")
	@Transactional
	public ResponseEntity<RoadmapGroupAssignmentResponseV1> assignRoadmapToGroup(Long id,
																				 AssignRoadmapToGroupRequestV1 request) {
		return ResponseEntity.ok(roadmapGroupAssignmentApiMapper.toRoadmapGroupAssignmentResponseV1(
				roadmapGroupAssignmentService.assignRoadmapToGroup(currentUser.requireUser(), id, request.getGroupId()
						, request.getGradeIds())
		));
	}

	@Override
	@PreAuthorize("@perm.has('" + PermissionKeys.LEARNING_ASSIGN + "')")
	@Transactional(readOnly = true)
	public ResponseEntity<RoadmapGroupAssignmentPreviewResponseV1> previewRoadmapGroupAssignment(Long id,
																								 RoadmapGroupAssignmentPreviewRequestV1 request) {
		return ResponseEntity.ok(roadmapGroupAssignmentApiMapper.toRoadmapGroupAssignmentPreviewResponseV1(
				roadmapGroupAssignmentService.previewAssignment(currentUser.requireUser(), request.getGroupId(),
						request.getGradeIds())
		));
	}

	@Override
	@PreAuthorize("@perm.has('" + PermissionKeys.LEARNING_ASSIGN + "')")
	@Transactional
	public ResponseEntity<OkResponseV1> unassignRoadmapFromGroup(Long id, Long groupId) {
		roadmapGroupAssignmentService.unassignRoadmapFromGroup(currentUser.requireUser(), id, groupId);
		return ResponseEntity.ok(apiResponses.ok());
	}
}
