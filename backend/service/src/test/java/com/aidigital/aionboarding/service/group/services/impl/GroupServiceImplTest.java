package com.aidigital.aionboarding.service.group.services.impl;

import com.aidigital.aionboarding.domain.group.entities.Group;
import com.aidigital.aionboarding.domain.group.entities.GroupLead;
import com.aidigital.aionboarding.domain.group.entities.GroupMember;
import com.aidigital.aionboarding.domain.user.entities.User;
import com.aidigital.aionboarding.service.common.error.AppException;
import com.aidigital.aionboarding.service.common.security.AppUser;
import com.aidigital.aionboarding.service.common.time.CurrentTime;
import com.aidigital.aionboarding.service.group.models.CreateGroupInput;
import com.aidigital.aionboarding.service.group.models.GroupDetailRecord;
import com.aidigital.aionboarding.service.group.models.GroupMemberRecord;
import com.aidigital.aionboarding.service.group.models.GroupOrgStatsRecord;
import com.aidigital.aionboarding.service.group.models.UpdateGroupInput;
import com.aidigital.aionboarding.service.group.services.entity.GroupEntityService;
import com.aidigital.aionboarding.service.group.services.entity.GroupLeadEntityService;
import com.aidigital.aionboarding.service.group.services.entity.GroupMemberEntityService;
import com.aidigital.aionboarding.service.group.support.GroupAccessPolicy;
import com.aidigital.aionboarding.service.group.support.GroupRecordAssembler;
import com.aidigital.aionboarding.service.group.support.GroupSpecificationBuilder;
import com.aidigital.aionboarding.service.mappers.user.UserRecordMapper;
import com.aidigital.aionboarding.service.permission.services.PermissionService;
import com.aidigital.aionboarding.service.user.models.UserRecord;
import com.aidigital.aionboarding.service.user.services.entity.UserEntityService;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GroupServiceImplTest {

	@Mock
	private GroupEntityService groupEntityService;
	@Mock
	private GroupLeadEntityService groupLeadEntityService;
	@Mock
	private GroupMemberEntityService groupMemberEntityService;
	@Mock
	private UserEntityService userEntityService;
	@Mock
	private GroupAccessPolicy groupAccessPolicy;
	@Mock
	private GroupSpecificationBuilder groupSpecificationBuilder;
	@Mock
	private GroupRecordAssembler groupRecordAssembler;
	@Mock
	private UserRecordMapper userMapper;
	@Mock
	private PermissionService permissionService;
	@Mock
	private CurrentTime currentTime;

	@InjectMocks
	private GroupServiceImpl service;

	@Nested
	class CreateGroup {

		@Test
		void createGroupShouldRejectNameShorterThanThreeCharactersTest() {
			// Given:
			AppUser admin = new AppUser(1L, "clerk-admin", "admin@test.com", "Admin", "admin", "Admin", null, null,
					null);

			// When-Then:
			assertThatThrownBy(() -> service.createGroup(admin, new CreateGroupInput("CS", "")))
					.isInstanceOf(AppException.class);
			verify(groupEntityService, never()).save(any());
		}

		@Test
		void createGroupShouldRejectNameLongerThanOneHundredCharactersTest() {
			// Given:
			AppUser admin = new AppUser(1L, "clerk-admin", "admin@test.com", "Admin", "admin", "Admin", null, null,
					null);
			String longName = "A".repeat(101);

			// When-Then:
			assertThatThrownBy(() -> service.createGroup(admin, new CreateGroupInput(longName, "")))
					.isInstanceOf(AppException.class);
			verify(groupEntityService, never()).save(any());
		}

		@Test
		void createGroupShouldRejectDuplicateNameCaseInsensitivelyTest() {
			// Given:
			AppUser admin = new AppUser(1L, "clerk-admin", "admin@test.com", "Admin", "admin", "Admin", null, null,
					null);
			when(groupEntityService.existsByNormalizedName("cs campaign")).thenReturn(true);

			// When-Then:
			assertThatThrownBy(() -> service.createGroup(admin, new CreateGroupInput("CS Campaign", "")))
					.isInstanceOf(AppException.class);
			verify(groupEntityService, never()).save(any());
		}

		@Test
		void createGroupShouldPersistGroupWithNormalizedNameTest() {
			// Given:
			AppUser admin = new AppUser(1L, "clerk-admin", "admin@test.com", "Admin", "admin", "Admin", null, null,
					null);
			when(groupEntityService.existsByNormalizedName("cs campaign")).thenReturn(false);
			when(userEntityService.getReference(1L)).thenReturn(null);
			when(currentTime.utcDateTime()).thenReturn(LocalDateTime.of(2026, 1, 1, 0, 0));
			when(groupEntityService.save(any(Group.class))).thenAnswer(invocation -> invocation.getArgument(0));
			GroupDetailRecord detail = new GroupDetailRecord(1L, "CS Campaign", "", List.of(), null, null);
			when(groupRecordAssembler.toDetailRecord(any(Group.class))).thenReturn(detail);

			// When:
			GroupDetailRecord result = service.createGroup(admin, new CreateGroupInput("CS Campaign", ""));

			// Then:
			assertThat(result.name()).isEqualTo("CS Campaign");
		}

		@Test
		void createGroupShouldNotAssignAdminCreatorAsLeadTest() {
			// Given:
			AppUser admin = new AppUser(1L, "clerk-admin", "admin@test.com", "Admin", "admin", "Admin", null, null,
					null);
			when(groupEntityService.existsByNormalizedName("cs campaign")).thenReturn(false);
			when(userEntityService.getReference(1L)).thenReturn(null);
			when(currentTime.utcDateTime()).thenReturn(LocalDateTime.of(2026, 1, 1, 0, 0));
			when(groupEntityService.save(any(Group.class))).thenAnswer(invocation -> invocation.getArgument(0));
			GroupDetailRecord detail = new GroupDetailRecord(1L, "CS Campaign", "", List.of(), null, null);
			when(groupRecordAssembler.toDetailRecord(any(Group.class))).thenReturn(detail);

			// When:
			service.createGroup(admin, new CreateGroupInput("CS Campaign", ""));

			// Then: admins see every group unrestricted, so no lead row is created.
			verify(groupLeadEntityService, never()).save(any());
		}

		@Test
		void createGroupShouldAssignTeamLeadCreatorAsLeadTest() {
			// Given:
			AppUser lead = new AppUser(2L, "clerk-lead", "lead@test.com", "Lead", "teamlead", "Lead", null, null,
					null);
			when(groupEntityService.existsByNormalizedName("cs campaign")).thenReturn(false);
			when(userEntityService.getReference(2L)).thenReturn(null);
			when(currentTime.utcDateTime()).thenReturn(LocalDateTime.of(2026, 1, 1, 0, 0));
			when(groupEntityService.save(any(Group.class))).thenAnswer(invocation -> {
				Group persisted = invocation.getArgument(0);
				persisted.setId(5L);
				return persisted;
			});
			GroupDetailRecord detail = new GroupDetailRecord(5L, "CS Campaign", "", List.of(), null, null);
			when(groupRecordAssembler.toDetailRecord(any(Group.class))).thenReturn(detail);

			// When:
			service.createGroup(lead, new CreateGroupInput("CS Campaign", ""));

			// Then: the creating team lead is recorded as a lead of the new group so
			// it appears in their scoped group list.
			ArgumentCaptor<GroupLead> captor = ArgumentCaptor.forClass(GroupLead.class);
			verify(groupLeadEntityService).save(captor.capture());
			GroupLead savedLead = captor.getValue();
			assertThat(savedLead.getId().getGroupId()).isEqualTo(5L);
			assertThat(savedLead.getId().getLeadUserId()).isEqualTo(2L);
			assertThat(savedLead.getCreatedAt()).isEqualTo(LocalDateTime.of(2026, 1, 1, 0, 0));
		}
	}

	@Nested
	class UpdateGroup {

		@Test
		void updateGroupShouldRejectWhenViewerCannotManageGroupTest() {
			// Given:
			AppUser member = new AppUser(3L, "clerk-member", "member@test.com", "Member", "member", "Member", null,
					null, null);
			Group group = new Group();
			group.setId(7L);
			when(groupEntityService.findById(7L)).thenReturn(Optional.of(group));
			when(groupAccessPolicy.canManageGroup(member, 7L)).thenReturn(false);

			// When-Then:
			assertThatThrownBy(() -> service.updateGroup(member, 7L, new UpdateGroupInput("New Name", "")))
					.isInstanceOf(AppException.class);
			verify(groupEntityService, never()).save(any());
		}

		@Test
		void updateGroupShouldAllowTeamLeadWhoLeadsTheGroupTest() {
			// Given:
			AppUser lead = new AppUser(2L, "clerk-lead", "lead@test.com", "Lead", "teamlead", "Lead", null, null,
					null);
			Group group = new Group();
			group.setId(7L);
			group.setName("Old Name");
			when(groupEntityService.findById(7L)).thenReturn(Optional.of(group));
			when(groupAccessPolicy.canManageGroup(lead, 7L)).thenReturn(true);
			when(groupEntityService.existsByNormalizedNameExcluding("new name", 7L)).thenReturn(false);
			when(currentTime.utcDateTime()).thenReturn(LocalDateTime.of(2026, 1, 1, 0, 0));
			when(groupEntityService.save(any(Group.class))).thenAnswer(invocation -> invocation.getArgument(0));
			GroupDetailRecord detail = new GroupDetailRecord(7L, "New Name", "", List.of(), null, null);
			when(groupRecordAssembler.toDetailRecord(any(Group.class))).thenReturn(detail);

			// When:
			GroupDetailRecord result = service.updateGroup(lead, 7L, new UpdateGroupInput("New Name", ""));

			// Then:
			assertThat(result.name()).isEqualTo("New Name");
		}
	}

	@Nested
	class ListGroupMembers {

		@Test
		void listGroupMembersShouldRejectWhenViewerCannotManageGroupTest() {
			// Given:
			AppUser member = new AppUser(3L, "clerk-member", "member@test.com", "Member", "member", "Member", null,
					null, null);
			Group group = new Group();
			group.setId(7L);
			when(groupEntityService.findById(7L)).thenReturn(Optional.of(group));
			when(groupAccessPolicy.canManageGroup(member, 7L)).thenReturn(false);

			// When-Then:
			assertThatThrownBy(() -> service.listGroupMembers(member, 7L, null, 0, 20))
					.isInstanceOf(AppException.class);
		}

		@Test
		void listGroupMembersShouldReturnPageFromAssemblerTest() {
			// Given:
			AppUser lead = new AppUser(2L, "clerk-lead", "lead@test.com", "Lead", "teamlead", "Lead", null, null,
					null);
			Group group = new Group();
			group.setId(7L);
			when(groupEntityService.findById(7L)).thenReturn(Optional.of(group));
			when(groupAccessPolicy.canManageGroup(lead, 7L)).thenReturn(true);
			Page<GroupMember> members = new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);
			when(groupMemberEntityService.findByGroupId(7L, "ana", PageRequest.of(0, 20))).thenReturn(members);
			Page<GroupMemberRecord> recordPage = new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);
			when(groupRecordAssembler.toMemberRecordPage(members)).thenReturn(recordPage);

			// When:
			Page<GroupMemberRecord> result = service.listGroupMembers(lead, 7L, "ana", 0, 20);

			// Then:
			assertThat(result).isSameAs(recordPage);
		}
	}

	@Nested
	class ListCandidateUsers {

		@Test
		void listCandidateUsersShouldRejectWhenViewerLacksGroupsManagePermissionTest() {
			// Given:
			AppUser lead = new AppUser(2L, "clerk-lead", "lead@test.com", "Lead", "teamlead", "Lead", null, null,
					null);
			org.mockito.Mockito.doThrow(new AppException(com.aidigital.aionboarding.service.common.error.ErrorReason.C004))
					.when(permissionService).requirePermission(lead,
							com.aidigital.aionboarding.service.permission.PermissionKeys.GROUPS_MANAGE);

			// When-Then:
			assertThatThrownBy(() -> service.listCandidateUsers(lead, 7L, false, null, 0, 20))
					.isInstanceOf(AppException.class);
			verify(userEntityService, never()).findGroupMemberCandidates(any(), any(), any());
		}

		@Test
		void listCandidateUsersShouldExcludeExistingMembersWhenNotForLeadsTest() {
			// Given:
			AppUser admin = new AppUser(1L, "clerk-admin", "admin@test.com", "Admin", "admin", "Admin", null, null,
					null);
			Group group = new Group();
			group.setId(7L);
			when(groupEntityService.findById(7L)).thenReturn(Optional.of(group));
			GroupMember existingMember = memberWithUserId(11L);
			when(groupMemberEntityService.findByGroupId(7L)).thenReturn(List.of(existingMember));
			User candidateUser = new User();
			candidateUser.setId(20L);
			Page<User> candidatePage = new PageImpl<>(List.of(candidateUser), PageRequest.of(0, 20), 1);
			when(userEntityService.findGroupMemberCandidates(Set.of(11L), "ana", PageRequest.of(0, 20)))
					.thenReturn(candidatePage);
			UserRecord candidateRecord = new UserRecord(20L, "clerk-20", "Ana", "ana@test.com", "member", null, null,
					null, null, null, null);
			when(userMapper.toRecord(candidateUser)).thenReturn(candidateRecord);

			// When:
			Page<UserRecord> result = service.listCandidateUsers(admin, 7L, false, "ana", 0, 20);

			// Then:
			assertThat(result.getContent()).containsExactly(candidateRecord);
		}

		@Test
		void listCandidateUsersShouldExcludeExistingLeadsWhenForLeadsTest() {
			// Given:
			AppUser admin = new AppUser(1L, "clerk-admin", "admin@test.com", "Admin", "admin", "Admin", null, null,
					null);
			Group group = new Group();
			group.setId(7L);
			when(groupEntityService.findById(7L)).thenReturn(Optional.of(group));
			when(groupLeadEntityService.findByGroupIdIn(List.of(7L))).thenReturn(List.of());
			Page<User> candidatePage = new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);
			when(userEntityService.findGroupLeadCandidates(eq(Set.of()), eq(null), eq(PageRequest.of(0, 20))))
					.thenReturn(candidatePage);

			// When:
			Page<UserRecord> result = service.listCandidateUsers(admin, 7L, true, null, 0, 20);

			// Then:
			assertThat(result.getContent()).isEmpty();
			verify(userEntityService, never()).findGroupMemberCandidates(any(), any(), any());
		}

		static GroupMember memberWithUserId(Long userId) {
			GroupMember member = new GroupMember();
			GroupMember.GroupMemberId id = new GroupMember.GroupMemberId();
			id.setMemberUserId(userId);
			id.setGroupId(7L);
			member.setId(id);
			return member;
		}
	}

	@Nested
	class GetOrgStats {

		@Test
		void getOrgStatsShouldReturnWorkspaceWideTotalsForAdminTest() {
			// Given:
			AppUser admin = new AppUser(1L, "clerk-admin", "admin@test.com", "Admin", "admin", "Admin", null, null,
					null);
			when(groupEntityService.count()).thenReturn(5L);
			when(groupMemberEntityService.countDistinctMemberUsers()).thenReturn(12L);
			when(groupLeadEntityService.countDistinctLeadUsers()).thenReturn(3L);

			// When:
			GroupOrgStatsRecord result = service.getOrgStats(admin);

			// Then:
			assertThat(result).isEqualTo(new GroupOrgStatsRecord(5, 12, 3));
			verify(groupEntityService, never()).existsByNormalizedName(any());
		}

		@Test
		void getOrgStatsShouldReturnTotalsScopedToLedGroupsForTeamLeadTest() {
			// Given:
			AppUser lead = new AppUser(2L, "clerk-lead", "lead@test.com", "Lead", "teamlead", "Lead", null, null,
					null);
			when(groupLeadEntityService.findGroupIdsByLeadUserId(2L)).thenReturn(Set.of(10L, 20L));
			when(groupMemberEntityService.countDistinctMemberUsers(Set.of(10L, 20L))).thenReturn(6L);
			when(groupLeadEntityService.countDistinctLeadUsers(Set.of(10L, 20L))).thenReturn(1L);

			// When:
			GroupOrgStatsRecord result = service.getOrgStats(lead);

			// Then:
			assertThat(result).isEqualTo(new GroupOrgStatsRecord(2, 6, 1));
		}

		@Test
		void getOrgStatsShouldReturnZeroCountsForPlainMemberTest() {
			// Given:
			AppUser member = new AppUser(3L, "clerk-member", "member@test.com", "Member", "member", "Member", null,
					null, null);

			// When:
			GroupOrgStatsRecord result = service.getOrgStats(member);

			// Then:
			assertThat(result).isEqualTo(new GroupOrgStatsRecord(0, 0, 0));
			verify(groupMemberEntityService, never()).countDistinctMemberUsers(any());
		}
	}

	@Nested
	class ResolveVisibleGroupIds {

		@Test
		void resolveVisibleGroupIdsShouldReturnNullForAdminTest() {
			// Given:
			AppUser admin = new AppUser(1L, "clerk-admin", "admin@test.com", "Admin", "admin", "Admin", null, null,
					null);

			// When:
			Set<Long> result = service.resolveVisibleGroupIds(admin);

			// Then:
			assertThat(result).isNull();
		}

		@Test
		void resolveVisibleGroupIdsShouldReturnLedGroupsForTeamLeadTest() {
			// Given:
			AppUser lead = new AppUser(2L, "clerk-lead", "lead@test.com", "Lead", "teamlead", "Lead", null, null,
					null);
			when(groupLeadEntityService.findGroupIdsByLeadUserId(2L)).thenReturn(Set.of(10L, 20L));

			// When:
			Set<Long> result = service.resolveVisibleGroupIds(lead);

			// Then:
			assertThat(result).containsExactlyInAnyOrder(10L, 20L);
		}

		@Test
		void resolveVisibleGroupIdsShouldReturnEmptySetForPlainMemberTest() {
			// Given:
			AppUser member = new AppUser(3L, "clerk-member", "member@test.com", "Member", "member", "Member", null,
					null, null);

			// When:
			Set<Long> result = service.resolveVisibleGroupIds(member);

			// Then:
			assertThat(result).isEmpty();
		}
	}
}
