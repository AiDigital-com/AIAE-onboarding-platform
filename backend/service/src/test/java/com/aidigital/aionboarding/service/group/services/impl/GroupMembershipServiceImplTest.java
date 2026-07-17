package com.aidigital.aionboarding.service.group.services.impl;

import com.aidigital.aionboarding.domain.common.dictionary.UserRoleCode;
import com.aidigital.aionboarding.domain.common.dictionary.entities.UserRole;
import com.aidigital.aionboarding.domain.group.entities.Group;
import com.aidigital.aionboarding.domain.group.entities.GroupLead;
import com.aidigital.aionboarding.domain.group.entities.GroupMember;
import com.aidigital.aionboarding.domain.user.entities.User;
import com.aidigital.aionboarding.service.common.error.AppException;
import com.aidigital.aionboarding.service.common.security.AppUser;
import com.aidigital.aionboarding.service.common.time.CurrentTime;
import com.aidigital.aionboarding.service.group.services.entity.GroupEntityService;
import com.aidigital.aionboarding.service.group.services.entity.GroupLeadEntityService;
import com.aidigital.aionboarding.service.group.services.entity.GroupMemberEntityService;
import com.aidigital.aionboarding.service.mappers.user.UserRecordMapper;
import com.aidigital.aionboarding.service.permission.PermissionKeys;
import com.aidigital.aionboarding.service.permission.services.PermissionService;
import com.aidigital.aionboarding.service.roadmap.services.RoadmapGroupAssignmentSyncService;
import com.aidigital.aionboarding.service.user.models.UserRecord;
import com.aidigital.aionboarding.service.user.services.entity.UserEntityService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GroupMembershipServiceImplTest {

	@Mock
	private GroupEntityService groupEntityService;
	@Mock
	private GroupMemberEntityService groupMemberEntityService;
	@Mock
	private GroupLeadEntityService groupLeadEntityService;
	@Mock
	private UserEntityService userEntityService;
	@Mock
	private RoadmapGroupAssignmentSyncService roadmapGroupAssignmentSyncService;
	@Mock
	private PermissionService permissionService;
	@Mock
	private UserRecordMapper userMapper;
	@Mock
	private CurrentTime currentTime;

	@InjectMocks
	private GroupMembershipServiceImpl service;

	@Test
	void shouldAddMemberWhenNotAlreadyMemberTest() {
		// Given:
		AppUser viewer = new AppUser(1L, "clerk-1", "admin@test.com", "Admin", "admin", "Admin", null, null, null);
		Group group = new Group();
		group.setId(10L);
		User member = new User();
		member.setId(20L);
		UserRecord record = new UserRecord(20L, "clerk-20", "Member", "member@test.com", "member", null, null, null,
				null, null, null);
		when(groupEntityService.findById(10L)).thenReturn(Optional.of(group));
		when(userEntityService.findById(20L)).thenReturn(Optional.of(member));
		when(groupMemberEntityService.existsByGroupIdAndMemberUserId(10L, 20L)).thenReturn(false);
		when(currentTime.utcDateTime()).thenReturn(LocalDateTime.parse("2026-07-15T10:00:00"));
		when(groupMemberEntityService.save(any(GroupMember.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(userMapper.toRecord(eq(member))).thenReturn(record);

		// When:
		UserRecord result = service.addMember(viewer, 10L, 20L);

		// Then:
		assertThat(result).isSameAs(record);
		verify(permissionService).requirePermission(viewer, PermissionKeys.GROUPS_MANAGE);
		verify(roadmapGroupAssignmentSyncService).syncNewGroupMember(10L, 20L);
		ArgumentCaptor<GroupMember> captor = ArgumentCaptor.forClass(GroupMember.class);
		verify(groupMemberEntityService).save(captor.capture());
		assertThat(captor.getValue().getId().getGroupId()).isEqualTo(10L);
		assertThat(captor.getValue().getId().getMemberUserId()).isEqualTo(20L);
		assertThat(captor.getValue().getCreatedAt()).isEqualTo("2026-07-15T10:00:00");
	}

	@Test
	void shouldRejectAddingExistingMemberTest() {
		// Given:
		AppUser viewer = new AppUser(1L, "clerk-1", "admin@test.com", "Admin", "admin", "Admin", null, null, null);
		Group group = new Group();
		group.setId(10L);
		User member = new User();
		member.setId(20L);
		when(groupEntityService.findById(10L)).thenReturn(Optional.of(group));
		when(userEntityService.findById(20L)).thenReturn(Optional.of(member));
		when(groupMemberEntityService.existsByGroupIdAndMemberUserId(10L, 20L)).thenReturn(true);

		// When-Then:
		assertThatThrownBy(() -> service.addMember(viewer, 10L, 20L))
				.isInstanceOf(AppException.class)
				.hasMessageContaining("already a member");
	}

	@Test
	void shouldRemoveMemberTest() {
		// Given:
		AppUser viewer = new AppUser(1L, "clerk-1", "admin@test.com", "Admin", "admin", "Admin", null, null, null);

		// When:
		service.removeMember(viewer, 10L, 20L);

		// Then:
		verify(permissionService).requirePermission(viewer, PermissionKeys.GROUPS_MANAGE);
		ArgumentCaptor<GroupMember.GroupMemberId> captor = ArgumentCaptor.forClass(GroupMember.GroupMemberId.class);
		verify(groupMemberEntityService).deleteById(captor.capture());
		assertThat(captor.getValue().getGroupId()).isEqualTo(10L);
		assertThat(captor.getValue().getMemberUserId()).isEqualTo(20L);
	}

	@Test
	void shouldAddLeadWhenAdminTest() {
		// Given:
		AppUser viewer = new AppUser(1L, "clerk-1", "admin@test.com", "Admin", "admin", "Admin", null, null, null);
		Group group = new Group();
		group.setId(10L);
		User lead = new User();
		lead.setId(30L);
		UserRole adminRole = new UserRole();
		adminRole.setCode(UserRoleCode.ADMIN);
		lead.setRole(adminRole);
		UserRecord record = new UserRecord(30L, "clerk-30", "Lead", "lead@test.com", "admin", null, null, null,
				null, null, null);
		when(groupEntityService.findById(10L)).thenReturn(Optional.of(group));
		when(userEntityService.findById(30L)).thenReturn(Optional.of(lead));
		when(groupLeadEntityService.existsByGroupIdAndLeadUserId(10L, 30L)).thenReturn(false);
		when(currentTime.utcDateTime()).thenReturn(LocalDateTime.parse("2026-07-15T10:00:00"));
		when(groupLeadEntityService.save(any(GroupLead.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(userMapper.toRecord(eq(lead))).thenReturn(record);

		// When:
		UserRecord result = service.addLead(viewer, 10L, 30L);

		// Then:
		assertThat(result).isSameAs(record);
		ArgumentCaptor<GroupLead> leadCaptor = ArgumentCaptor.forClass(GroupLead.class);
		verify(groupLeadEntityService).save(leadCaptor.capture());
		assertThat(leadCaptor.getValue().getId().getGroupId()).isEqualTo(10L);
		assertThat(leadCaptor.getValue().getId().getLeadUserId()).isEqualTo(30L);
	}

	@Test
	void shouldRejectLeadWithoutAdminOrTeamLeadRoleTest() {
		// Given:
		AppUser viewer = new AppUser(1L, "clerk-1", "admin@test.com", "Admin", "admin", "Admin", null, null, null);
		Group group = new Group();
		group.setId(10L);
		User lead = new User();
		lead.setId(30L);
		UserRole memberRole = new UserRole();
		memberRole.setCode(UserRoleCode.MEMBER);
		lead.setRole(memberRole);
		when(groupEntityService.findById(10L)).thenReturn(Optional.of(group));
		when(userEntityService.findById(30L)).thenReturn(Optional.of(lead));

		// When-Then:
		assertThatThrownBy(() -> service.addLead(viewer, 10L, 30L))
				.isInstanceOf(AppException.class)
				.hasMessageContaining("must be an admin or team lead");
	}

	@Test
	void shouldRejectAddingExistingLeadTest() {
		// Given:
		AppUser viewer = new AppUser(1L, "clerk-1", "admin@test.com", "Admin", "admin", "Admin", null, null, null);
		Group group = new Group();
		group.setId(10L);
		User lead = new User();
		lead.setId(30L);
		UserRole teamLeadRole = new UserRole();
		teamLeadRole.setCode(UserRoleCode.TEAMLEAD);
		lead.setRole(teamLeadRole);
		when(groupEntityService.findById(10L)).thenReturn(Optional.of(group));
		when(userEntityService.findById(30L)).thenReturn(Optional.of(lead));
		when(groupLeadEntityService.existsByGroupIdAndLeadUserId(10L, 30L)).thenReturn(true);

		// When-Then:
		assertThatThrownBy(() -> service.addLead(viewer, 10L, 30L))
				.isInstanceOf(AppException.class)
				.hasMessageContaining("already a lead");
	}

	@Test
	void shouldRemoveLeadTest() {
		// Given:
		AppUser viewer = new AppUser(1L, "clerk-1", "admin@test.com", "Admin", "admin", "Admin", null, null, null);

		// When:
		service.removeLead(viewer, 10L, 30L);

		// Then:
		verify(permissionService).requirePermission(viewer, PermissionKeys.GROUPS_MANAGE);
		ArgumentCaptor<GroupLead.GroupLeadId> captor = ArgumentCaptor.forClass(GroupLead.GroupLeadId.class);
		verify(groupLeadEntityService).deleteById(captor.capture());
		assertThat(captor.getValue().getGroupId()).isEqualTo(10L);
		assertThat(captor.getValue().getLeadUserId()).isEqualTo(30L);
	}

	@Test
	void shouldRejectWhenGroupNotFoundTest() {
		// Given:
		AppUser viewer = new AppUser(1L, "clerk-1", "admin@test.com", "Admin", "admin", "Admin", null, null, null);
		when(groupEntityService.findById(99L)).thenReturn(Optional.empty());

		// When-Then:
		assertThatThrownBy(() -> service.addMember(viewer, 99L, 20L))
				.isInstanceOf(AppException.class)
				.hasMessageContaining("Group not found");
	}

	@Test
	void shouldRejectWhenUserNotFoundTest() {
		// Given:
		AppUser viewer = new AppUser(1L, "clerk-1", "admin@test.com", "Admin", "admin", "Admin", null, null, null);
		Group group = new Group();
		group.setId(10L);
		when(groupEntityService.findById(10L)).thenReturn(Optional.of(group));
		when(userEntityService.findById(99L)).thenReturn(Optional.empty());

		// When-Then:
		assertThatThrownBy(() -> service.addMember(viewer, 10L, 99L))
				.isInstanceOf(AppException.class);
	}
}
