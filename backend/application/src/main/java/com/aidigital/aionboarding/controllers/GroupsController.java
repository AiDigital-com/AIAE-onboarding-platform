package com.aidigital.aionboarding.controllers;

import com.aidigital.aionboarding.api.v1.GroupsApi;
import com.aidigital.aionboarding.api.v1.model.AddGroupLeadRequestV1;
import com.aidigital.aionboarding.api.v1.model.AddGroupLeadResponseV1;
import com.aidigital.aionboarding.api.v1.model.AddGroupMemberRequestV1;
import com.aidigital.aionboarding.api.v1.model.AddGroupMemberResponseV1;
import com.aidigital.aionboarding.api.v1.model.CreateGroupRequestV1;
import com.aidigital.aionboarding.api.v1.model.GroupCandidateUsersListResponseV1;
import com.aidigital.aionboarding.api.v1.model.GroupMembersListResponseV1;
import com.aidigital.aionboarding.api.v1.model.GroupOrgStatsResponseV1;
import com.aidigital.aionboarding.api.v1.model.GroupResponseV1;
import com.aidigital.aionboarding.api.v1.model.GroupsListResponseV1;
import com.aidigital.aionboarding.api.v1.model.OkResponseV1;
import com.aidigital.aionboarding.api.v1.model.RoadmapGroupAssignmentsListResponseV1;
import com.aidigital.aionboarding.api.v1.model.UpdateGroupRequestV1;
import com.aidigital.aionboarding.mappers.group.GroupApiMapper;
import com.aidigital.aionboarding.mappers.roadmap.RoadmapGroupAssignmentApiMapper;
import com.aidigital.aionboarding.mappers.user.UserApiMapper;
import com.aidigital.aionboarding.service.common.security.AppUser;
import com.aidigital.aionboarding.service.group.models.CreateGroupInput;
import com.aidigital.aionboarding.service.group.models.UpdateGroupInput;
import com.aidigital.aionboarding.service.group.services.GroupMembershipService;
import com.aidigital.aionboarding.service.group.services.GroupService;
import com.aidigital.aionboarding.service.roadmap.services.RoadmapGroupAssignmentService;
import com.aidigital.aionboarding.service.user.models.UserRecord;
import com.aidigital.aionboarding.support.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class GroupsController implements GroupsApi {

	private final CurrentUserSupport currentUser;
	private final GroupService groupService;
	private final GroupMembershipService groupMembershipService;
	private final RoadmapGroupAssignmentService roadmapGroupAssignmentService;
	private final GroupApiMapper groupApiMapper;
	private final UserApiMapper userApiMapper;
	private final RoadmapGroupAssignmentApiMapper roadmapGroupAssignmentApiMapper;
	private final ApiResponses apiResponses;

	@Override
	@Transactional(readOnly = true)
	public ResponseEntity<GroupsListResponseV1> listGroups(String search, Integer page, Integer size) {
		AppUser viewer = currentUser.requireUser();
		int pageIndex = page == null ? 0 : page;
		int pageSize = size == null ? 20 : size;
		return ResponseEntity.ok(groupApiMapper.toGroupsListResponseV1(groupService.listGroups(viewer, search,
				pageIndex, pageSize)));
	}

	@Override
	@Transactional(readOnly = true)
	public ResponseEntity<GroupOrgStatsResponseV1> getGroupOrgStats() {
		AppUser viewer = currentUser.requireUser();
		return ResponseEntity.ok(groupApiMapper.toGroupOrgStatsResponseV1(groupService.getOrgStats(viewer)));
	}

	@Override
	@Transactional
	public ResponseEntity<GroupResponseV1> createGroup(CreateGroupRequestV1 request) {
		AppUser viewer = currentUser.requireUser();
		return ResponseEntity.ok(groupApiMapper.toGroupResponseV1(
				groupService.createGroup(viewer, new CreateGroupInput(request.getName(), request.getDescription()))
		));
	}

	@Override
	@Transactional(readOnly = true)
	public ResponseEntity<GroupResponseV1> getGroup(Long id) {
		AppUser viewer = currentUser.requireUser();
		return ResponseEntity.ok(groupApiMapper.toGroupResponseV1(groupService.getGroup(viewer, id)));
	}

	@Override
	@Transactional
	public ResponseEntity<GroupResponseV1> updateGroup(Long id, UpdateGroupRequestV1 request) {
		AppUser viewer = currentUser.requireUser();
		return ResponseEntity.ok(groupApiMapper.toGroupResponseV1(
				groupService.updateGroup(viewer, id, new UpdateGroupInput(request.getName(), request.getDescription()))
		));
	}

	@Override
	@Transactional
	public ResponseEntity<OkResponseV1> deleteGroup(Long id) {
		AppUser viewer = currentUser.requireUser();
		groupService.deleteGroup(viewer, id);
		return ResponseEntity.ok(apiResponses.ok());
	}

	@Override
	@Transactional(readOnly = true)
	public ResponseEntity<GroupMembersListResponseV1> listGroupMembers(Long id, String search, Integer page,
																	   Integer size) {
		AppUser viewer = currentUser.requireUser();
		int pageIndex = page == null ? 0 : page;
		int pageSize = size == null ? 20 : size;
		return ResponseEntity.ok(groupApiMapper.toGroupMembersListResponseV1(
				groupService.listGroupMembers(viewer, id, search, pageIndex, pageSize)
		));
	}

	@Override
	@Transactional(readOnly = true)
	public ResponseEntity<GroupCandidateUsersListResponseV1> listGroupCandidateUsers(
			Long id,
			Boolean forLeads,
			String search,
			Integer page,
			Integer size
	) {
		AppUser viewer = currentUser.requireUser();
		int pageIndex = page == null ? 0 : page;
		int pageSize = size == null ? 20 : size;
		boolean leadsOnly = Boolean.TRUE.equals(forLeads);
		Page<UserRecord> candidates = groupService.listCandidateUsers(viewer, id, leadsOnly, search, pageIndex,
				pageSize);
		return ResponseEntity.ok(groupApiMapper.toGroupCandidateUsersListResponseV1(
				candidates.stream().map(userApiMapper::toUserSummaryV1).toList(),
				candidates
		));
	}

	@Override
	@Transactional(readOnly = true)
	public ResponseEntity<RoadmapGroupAssignmentsListResponseV1> listGroupRoadmapAssignments(Long id) {
		AppUser viewer = currentUser.requireUser();
		return ResponseEntity.ok(roadmapGroupAssignmentApiMapper.toRoadmapGroupAssignmentsListResponseV1(
				roadmapGroupAssignmentService.listAssignmentsForGroup(viewer, id)
		));
	}

	@Override
	@Transactional
	public ResponseEntity<AddGroupMemberResponseV1> addGroupMember(Long id, AddGroupMemberRequestV1 request) {
		AppUser viewer = currentUser.requireUser();
		return ResponseEntity.ok(groupApiMapper.toAddGroupMemberResponseV1(
				groupMembershipService.addMember(viewer, id, request.getMemberUserId())
		));
	}

	@Override
	@Transactional
	public ResponseEntity<OkResponseV1> removeGroupMember(Long id, Long userId) {
		AppUser viewer = currentUser.requireUser();
		groupMembershipService.removeMember(viewer, id, userId);
		return ResponseEntity.ok(apiResponses.ok());
	}

	@Override
	@Transactional
	public ResponseEntity<AddGroupLeadResponseV1> addGroupLead(Long id, AddGroupLeadRequestV1 request) {
		AppUser viewer = currentUser.requireUser();
		return ResponseEntity.ok(groupApiMapper.toAddGroupLeadResponseV1(
				groupMembershipService.addLead(viewer, id, request.getLeadUserId())
		));
	}

	@Override
	@Transactional
	public ResponseEntity<OkResponseV1> removeGroupLead(Long id, Long userId) {
		AppUser viewer = currentUser.requireUser();
		groupMembershipService.removeLead(viewer, id, userId);
		return ResponseEntity.ok(apiResponses.ok());
	}
}
