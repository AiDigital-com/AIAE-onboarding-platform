package com.aidigital.aionboarding.controllers;

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
import com.aidigital.aionboarding.api.v1.model.UserSummaryV1;
import com.aidigital.aionboarding.mappers.group.GroupApiMapper;
import com.aidigital.aionboarding.mappers.roadmap.RoadmapGroupAssignmentApiMapper;
import com.aidigital.aionboarding.mappers.user.UserApiMapper;
import com.aidigital.aionboarding.service.common.security.AppUser;
import com.aidigital.aionboarding.service.group.models.CreateGroupInput;
import com.aidigital.aionboarding.service.group.models.GroupDetailRecord;
import com.aidigital.aionboarding.service.group.models.GroupMemberRecord;
import com.aidigital.aionboarding.service.group.models.GroupOrgStatsRecord;
import com.aidigital.aionboarding.service.group.models.GroupSummaryRecord;
import com.aidigital.aionboarding.service.group.models.UpdateGroupInput;
import com.aidigital.aionboarding.service.group.services.GroupMembershipService;
import com.aidigital.aionboarding.service.group.services.GroupService;
import com.aidigital.aionboarding.service.roadmap.models.RoadmapGroupAssignmentRecord;
import com.aidigital.aionboarding.service.roadmap.services.RoadmapGroupAssignmentService;
import com.aidigital.aionboarding.service.user.models.UserRecord;
import com.aidigital.aionboarding.support.ApiResponses;
import org.instancio.Instancio;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.instancio.Select.field;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GroupsControllerTest {

	@Mock
	private CurrentUserSupport currentUser;
	@Mock
	private GroupService groupService;
	@Mock
	private GroupMembershipService groupMembershipService;
	@Mock
	private RoadmapGroupAssignmentService roadmapGroupAssignmentService;
	@Mock
	private GroupApiMapper groupApiMapper;
	@Mock
	private UserApiMapper userApiMapper;
	@Mock
	private RoadmapGroupAssignmentApiMapper roadmapGroupAssignmentApiMapper;
	@Mock
	private ApiResponses apiResponses;

	@InjectMocks
	private GroupsController controller;

	@Test
	void shouldListGroupsWithDefaultPaginationTest() {
		// Given:
		AppUser viewer = viewer();
		Page<GroupSummaryRecord> page = mock(Page.class);
		GroupsListResponseV1 expectedBody = Instancio.create(GroupsListResponseV1.class);
		when(currentUser.requireUser()).thenReturn(viewer);
		when(groupService.listGroups(viewer, "search", 0, 20)).thenReturn(page);
		when(groupApiMapper.toGroupsListResponseV1(page)).thenReturn(expectedBody);

		// When:
		ResponseEntity<GroupsListResponseV1> response = controller.listGroups("search", null, null);

		// Then:
		assertThat(response.getBody()).isSameAs(expectedBody);
	}

	@Test
	void shouldGetGroupOrgStatsTest() {
		// Given:
		AppUser viewer = viewer();
		GroupOrgStatsRecord stats = Instancio.create(GroupOrgStatsRecord.class);
		GroupOrgStatsResponseV1 expectedBody = Instancio.create(GroupOrgStatsResponseV1.class);
		when(currentUser.requireUser()).thenReturn(viewer);
		when(groupService.getOrgStats(viewer)).thenReturn(stats);
		when(groupApiMapper.toGroupOrgStatsResponseV1(stats)).thenReturn(expectedBody);

		// When:
		ResponseEntity<GroupOrgStatsResponseV1> response = controller.getGroupOrgStats();

		// Then:
		assertThat(response.getBody()).isSameAs(expectedBody);
	}

	@Test
	void shouldCreateGroupFromRequestTest() {
		// Given:
		AppUser viewer = viewer();
		CreateGroupRequestV1 request = Instancio.of(CreateGroupRequestV1.class)
				.set(field("name"), "Engineering")
				.set(field("description"), "Team")
				.create();
		GroupDetailRecord created = Instancio.create(GroupDetailRecord.class);
		GroupResponseV1 expectedBody = Instancio.create(GroupResponseV1.class);
		when(currentUser.requireUser()).thenReturn(viewer);
		when(groupService.createGroup(viewer, new CreateGroupInput("Engineering", "Team"))).thenReturn(created);
		when(groupApiMapper.toGroupResponseV1(created)).thenReturn(expectedBody);

		// When:
		ResponseEntity<GroupResponseV1> response = controller.createGroup(request);

		// Then:
		assertThat(response.getBody()).isSameAs(expectedBody);
	}

	@Test
	void shouldGetGroupByIdTest() {
		// Given:
		AppUser viewer = viewer();
		GroupDetailRecord group = Instancio.create(GroupDetailRecord.class);
		GroupResponseV1 expectedBody = Instancio.create(GroupResponseV1.class);
		when(currentUser.requireUser()).thenReturn(viewer);
		when(groupService.getGroup(viewer, 5L)).thenReturn(group);
		when(groupApiMapper.toGroupResponseV1(group)).thenReturn(expectedBody);

		// When:
		ResponseEntity<GroupResponseV1> response = controller.getGroup(5L);

		// Then:
		assertThat(response.getBody()).isSameAs(expectedBody);
	}

	@Test
	void shouldUpdateGroupFromRequestTest() {
		// Given:
		AppUser viewer = viewer();
		UpdateGroupRequestV1 request = Instancio.of(UpdateGroupRequestV1.class)
				.set(field("name"), "Updated")
				.set(field("description"), "New desc")
				.create();
		GroupDetailRecord updated = Instancio.create(GroupDetailRecord.class);
		GroupResponseV1 expectedBody = Instancio.create(GroupResponseV1.class);
		when(currentUser.requireUser()).thenReturn(viewer);
		when(groupService.updateGroup(viewer, 5L, new UpdateGroupInput("Updated", "New desc"))).thenReturn(updated);
		when(groupApiMapper.toGroupResponseV1(updated)).thenReturn(expectedBody);

		// When:
		ResponseEntity<GroupResponseV1> response = controller.updateGroup(5L, request);

		// Then:
		assertThat(response.getBody()).isSameAs(expectedBody);
	}

	@Test
	void shouldDeleteGroupAndReturnOkTest() {
		// Given:
		AppUser viewer = viewer();
		OkResponseV1 expectedBody = Instancio.create(OkResponseV1.class);
		when(currentUser.requireUser()).thenReturn(viewer);
		when(apiResponses.ok()).thenReturn(expectedBody);

		// When:
		ResponseEntity<OkResponseV1> response = controller.deleteGroup(5L);

		// Then:
		assertThat(response.getBody()).isSameAs(expectedBody);
	}

	@Test
	void shouldListGroupMembersWithDefaultPaginationTest() {
		// Given:
		AppUser viewer = viewer();
		Page<GroupMemberRecord> page = mock(Page.class);
		GroupMembersListResponseV1 expectedBody = Instancio.create(GroupMembersListResponseV1.class);
		when(currentUser.requireUser()).thenReturn(viewer);
		when(groupService.listGroupMembers(viewer, 5L, "search", 0, 20)).thenReturn(page);
		when(groupApiMapper.toGroupMembersListResponseV1(page)).thenReturn(expectedBody);

		// When:
		ResponseEntity<GroupMembersListResponseV1> response = controller.listGroupMembers(5L, "search", null, null);

		// Then:
		assertThat(response.getBody()).isSameAs(expectedBody);
	}

	@Test
	void shouldListLeadCandidateUsersForGroupTest() {
		// Given:
		AppUser viewer = viewer();
		UserRecord candidate = Instancio.create(UserRecord.class);
		Page<UserRecord> candidates = new PageImpl<>(List.of(candidate));
		UserSummaryV1 summary = Instancio.create(UserSummaryV1.class);
		GroupCandidateUsersListResponseV1 expectedBody = Instancio.create(GroupCandidateUsersListResponseV1.class);
		when(currentUser.requireUser()).thenReturn(viewer);
		when(groupService.listCandidateUsers(viewer, 5L, true, "search", 0, 20)).thenReturn(candidates);
		when(userApiMapper.toUserSummaryV1(candidate)).thenReturn(summary);
		when(groupApiMapper.toGroupCandidateUsersListResponseV1(anyList(),
				org.mockito.ArgumentMatchers.eq(candidates)))
				.thenReturn(expectedBody);

		// When:
		ResponseEntity<GroupCandidateUsersListResponseV1> response =
				controller.listGroupCandidateUsers(5L, true, "search", null, null);

		// Then:
		assertThat(response.getBody()).isSameAs(expectedBody);
	}

	@Test
	void shouldListMemberCandidateUsersForGroupWhenForLeadsIsFalseTest() {
		// Given:
		AppUser viewer = viewer();
		UserRecord candidate = Instancio.create(UserRecord.class);
		Page<UserRecord> candidates = new PageImpl<>(List.of(candidate));
		UserSummaryV1 summary = Instancio.create(UserSummaryV1.class);
		GroupCandidateUsersListResponseV1 expectedBody = Instancio.create(GroupCandidateUsersListResponseV1.class);
		when(currentUser.requireUser()).thenReturn(viewer);
		when(groupService.listCandidateUsers(viewer, 5L, false, "search", 0, 20)).thenReturn(candidates);
		when(userApiMapper.toUserSummaryV1(candidate)).thenReturn(summary);
		when(groupApiMapper.toGroupCandidateUsersListResponseV1(anyList(),
				org.mockito.ArgumentMatchers.eq(candidates)))
				.thenReturn(expectedBody);

		// When:
		ResponseEntity<GroupCandidateUsersListResponseV1> response =
				controller.listGroupCandidateUsers(5L, false, "search", null, null);

		// Then:
		assertThat(response.getBody()).isSameAs(expectedBody);
	}

	@Test
	void shouldListGroupRoadmapAssignmentsTest() {
		// Given:
		AppUser viewer = viewer();
		List<RoadmapGroupAssignmentRecord> assignments = List.of();
		RoadmapGroupAssignmentsListResponseV1 expectedBody =
				Instancio.create(RoadmapGroupAssignmentsListResponseV1.class);
		when(currentUser.requireUser()).thenReturn(viewer);
		when(roadmapGroupAssignmentService.listAssignmentsForGroup(viewer, 5L)).thenReturn(assignments);
		when(roadmapGroupAssignmentApiMapper.toRoadmapGroupAssignmentsListResponseV1(assignments))
				.thenReturn(expectedBody);

		// When:
		ResponseEntity<RoadmapGroupAssignmentsListResponseV1> response = controller.listGroupRoadmapAssignments(5L);

		// Then:
		assertThat(response.getBody()).isSameAs(expectedBody);
	}

	@Test
	void shouldAddGroupMemberTest() {
		// Given:
		AppUser viewer = viewer();
		AddGroupMemberRequestV1 request = Instancio.of(AddGroupMemberRequestV1.class)
				.set(field("memberUserId"), 7L)
				.create();
		UserRecord member = Instancio.create(UserRecord.class);
		AddGroupMemberResponseV1 expectedBody = Instancio.create(AddGroupMemberResponseV1.class);
		when(currentUser.requireUser()).thenReturn(viewer);
		when(groupMembershipService.addMember(viewer, 5L, 7L)).thenReturn(member);
		when(groupApiMapper.toAddGroupMemberResponseV1(member)).thenReturn(expectedBody);

		// When:
		ResponseEntity<AddGroupMemberResponseV1> response = controller.addGroupMember(5L, request);

		// Then:
		assertThat(response.getBody()).isSameAs(expectedBody);
	}

	@Test
	void shouldRemoveGroupMemberAndReturnOkTest() {
		// Given:
		AppUser viewer = viewer();
		OkResponseV1 expectedBody = Instancio.create(OkResponseV1.class);
		when(currentUser.requireUser()).thenReturn(viewer);
		when(apiResponses.ok()).thenReturn(expectedBody);

		// When:
		ResponseEntity<OkResponseV1> response = controller.removeGroupMember(5L, 7L);

		// Then:
		assertThat(response.getBody()).isSameAs(expectedBody);
	}

	@Test
	void shouldAddGroupLeadTest() {
		// Given:
		AppUser viewer = viewer();
		AddGroupLeadRequestV1 request = Instancio.of(AddGroupLeadRequestV1.class)
				.set(field("leadUserId"), 8L)
				.create();
		UserRecord lead = Instancio.create(UserRecord.class);
		AddGroupLeadResponseV1 expectedBody = Instancio.create(AddGroupLeadResponseV1.class);
		when(currentUser.requireUser()).thenReturn(viewer);
		when(groupMembershipService.addLead(viewer, 5L, 8L)).thenReturn(lead);
		when(groupApiMapper.toAddGroupLeadResponseV1(lead)).thenReturn(expectedBody);

		// When:
		ResponseEntity<AddGroupLeadResponseV1> response = controller.addGroupLead(5L, request);

		// Then:
		assertThat(response.getBody()).isSameAs(expectedBody);
	}

	@Test
	void shouldRemoveGroupLeadAndReturnOkTest() {
		// Given:
		AppUser viewer = viewer();
		OkResponseV1 expectedBody = Instancio.create(OkResponseV1.class);
		when(currentUser.requireUser()).thenReturn(viewer);
		when(apiResponses.ok()).thenReturn(expectedBody);

		// When:
		ResponseEntity<OkResponseV1> response = controller.removeGroupLead(5L, 8L);

		// Then:
		assertThat(response.getBody()).isSameAs(expectedBody);
	}

	AppUser viewer() {
		return Instancio.of(AppUser.class)
				.set(field("internalId"), 1L)
				.set(field("clerkUserId"), "clerk-1")
				.set(field("email"), "v@t.com")
				.set(field("fullName"), "V")
				.set(field("roleCode"), "member")
				.set(field("name"), "V")
				.set(field("position"), null)
				.set(field("avatarStorageKey"), null)
				.set(field("avatarColor"), null)
				.create();
	}
}
