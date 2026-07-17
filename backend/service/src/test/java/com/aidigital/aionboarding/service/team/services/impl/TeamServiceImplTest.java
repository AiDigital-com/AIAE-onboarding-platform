package com.aidigital.aionboarding.service.team.services.impl;

import com.aidigital.aionboarding.domain.common.dictionary.UserRoleCode;
import com.aidigital.aionboarding.domain.common.dictionary.entities.UserRole;
import com.aidigital.aionboarding.domain.group.entities.GroupMember;
import com.aidigital.aionboarding.domain.team.entities.TeamMember;
import com.aidigital.aionboarding.domain.user.entities.User;
import com.aidigital.aionboarding.service.common.dictionary.DictionaryLookupService;
import com.aidigital.aionboarding.service.common.error.AppException;
import com.aidigital.aionboarding.service.common.security.AppUser;
import com.aidigital.aionboarding.service.common.time.CurrentTime;
import com.aidigital.aionboarding.service.group.services.entity.GroupLeadEntityService;
import com.aidigital.aionboarding.service.group.services.entity.GroupMemberEntityService;
import com.aidigital.aionboarding.service.mappers.user.UserRecordMapper;
import com.aidigital.aionboarding.service.team.models.TeamRecord;
import com.aidigital.aionboarding.service.team.services.entity.TeamEntityService;
import com.aidigital.aionboarding.service.user.models.UserRecord;
import com.aidigital.aionboarding.service.user.services.entity.UserEntityService;
import org.instancio.Instancio;
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
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.instancio.Select.field;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TeamServiceImplTest {

	@Mock
	private UserEntityService userEntityService;
	@Mock
	private TeamEntityService teamEntityService;
	@Mock
	private GroupLeadEntityService groupLeadEntityService;
	@Mock
	private GroupMemberEntityService groupMemberEntityService;
	@Mock
	private DictionaryLookupService dictionaryLookupService;
	@Mock
	private UserRecordMapper userMapper;
	@Mock
	private CurrentTime currentTime;

	@InjectMocks
	private TeamServiceImpl service;

	private GroupMember groupMemberOf(Long groupId, User memberUser) {
		GroupMember gm = new GroupMember();
		GroupMember.GroupMemberId id = new GroupMember.GroupMemberId();
		id.setGroupId(groupId);
		id.setMemberUserId(memberUser.getId());
		gm.setId(id);
		gm.setMemberUser(memberUser);
		return gm;
	}

	private TeamMember teamMemberOf(Long leadUserId, User memberUser) {
		TeamMember tm = new TeamMember();
		TeamMember.TeamMemberId id = new TeamMember.TeamMemberId();
		id.setLeadUserId(leadUserId);
		id.setMemberUserId(memberUser.getId());
		tm.setId(id);
		tm.setMemberUser(memberUser);
		return tm;
	}

	private User userWithIdAndRole(Long id, String roleCode) {
		User user = Instancio.of(User.class).set(field(User::getId), id).create();
		UserRole role = new UserRole();
		role.setCode(roleCode);
		user.setRole(role);
		return user;
	}

	private UserRole roleOf(String code) {
		UserRole role = new UserRole();
		role.setCode(code);
		return role;
	}

	private AppUser adminUser() {
		return new AppUser(1L, "clerk-admin", "admin@test.com", "Admin User", "admin", "Admin", null, null, null);
	}

	private AppUser teamLeadUser() {
		return new AppUser(2L, "clerk-lead", "lead@test.com", "Lead User", "teamlead", "Lead", null, null, null);
	}

	private AppUser memberUser() {
		return new AppUser(3L, "clerk-member", "member@test.com", "Member User", "member", "Member", null, null,
				null);
	}

	@Nested
	class GetTeamCandidateUsers {

		@Test
		void getTeamCandidateUsersShouldReturnAllUsersExceptSelfForAdminCallerTest() {
			// Given:
			AppUser caller = adminUser();
			User user = Instancio.create(User.class);
			UserRecord userRecord = new UserRecord(2L, "clerk-other", "Other", "other@test.com", "member", null, null,
					null, null, null, null);
			when(userEntityService.findAllExcluding(1L)).thenReturn(List.of(user));
			when(userMapper.toRecord(user)).thenReturn(userRecord);

			// When:
			List<UserRecord> result = service.getTeamCandidateUsers(caller);

			// Then:
			assertThat(result).hasSize(1);
			verify(userEntityService).findAllExcluding(1L);
			verify(userEntityService, never()).findTeamCandidates(any(), any());
		}

		@Test
		void getTeamCandidateUsersShouldReturnNonAdminUsersExceptSelfForTeamLeadCallerTest() {
			// Given:
			AppUser caller = teamLeadUser();
			User user = Instancio.create(User.class);
			UserRecord userRecord = new UserRecord(3L, "clerk-member", "Member", "member@test.com", "member", null,
					null, null, null, null, null);
			when(userEntityService.findTeamCandidates(2L, UserRoleCode.ADMIN)).thenReturn(List.of(user));
			when(userMapper.toRecord(user)).thenReturn(userRecord);

			// When:
			List<UserRecord> result = service.getTeamCandidateUsers(caller);

			// Then:
			assertThat(result).hasSize(1);
			verify(userEntityService).findTeamCandidates(2L, UserRoleCode.ADMIN);
			verify(userEntityService, never()).findAllExcluding(any());
		}

		@Test
		void getTeamCandidateUsersShouldReturnEmptyListForMemberCallerTest() {
			// Given:
			AppUser caller = memberUser();

			// When:
			List<UserRecord> result = service.getTeamCandidateUsers(caller);

			// Then:
			assertThat(result).isEmpty();
			verify(userEntityService, never()).findAllExcluding(any());
			verify(userEntityService, never()).findTeamCandidates(any(), any());
		}

		@Test
		void getTeamCandidateUsersShouldReturnEmptyListForNullCallerTest() {
			// When:
			List<UserRecord> result = service.getTeamCandidateUsers(null);

			// Then:
			assertThat(result).isEmpty();
		}
	}

	@Nested
	class GetTeamCandidateUsersPaged {

		@Test
		void shouldDelegateToExcludingPagedQueryForAdminCallerTest() {
			// Given:
			AppUser caller = adminUser();
			Pageable pageable = PageRequest.of(0, 20);
			User user = Instancio.create(User.class);
			UserRecord userRecord = new UserRecord(2L, "clerk-other", "Other", "other@test.com", "member", null, null,
					null, null, null, null);
			Page<User> page = new PageImpl<>(List.of(user), pageable, 1);
			when(userEntityService.findAllExcluding(1L, "ann", pageable)).thenReturn(page);
			when(userMapper.toRecord(user)).thenReturn(userRecord);

			// When:
			Page<UserRecord> result = service.getTeamCandidateUsers(caller, "ann", pageable);

			// Then:
			assertThat(result.getContent()).containsExactly(userRecord);
			verify(userEntityService).findAllExcluding(1L, "ann", pageable);
			verify(userEntityService, never()).findTeamCandidates(any(), any(), any(), any());
		}

		@Test
		void shouldDelegateToTeamCandidatesPagedQueryForTeamLeadCallerTest() {
			// Given:
			AppUser caller = teamLeadUser();
			Pageable pageable = PageRequest.of(0, 20);
			User user = Instancio.create(User.class);
			UserRecord userRecord = new UserRecord(3L, "clerk-member", "Member", "member@test.com", "member", null,
					null, null, null, null, null);
			Page<User> page = new PageImpl<>(List.of(user), pageable, 1);
			when(userEntityService.findTeamCandidates(2L, UserRoleCode.ADMIN, "ann", pageable)).thenReturn(page);
			when(userMapper.toRecord(user)).thenReturn(userRecord);

			// When:
			Page<UserRecord> result = service.getTeamCandidateUsers(caller, "ann", pageable);

			// Then:
			assertThat(result.getContent()).containsExactly(userRecord);
			verify(userEntityService).findTeamCandidates(2L, UserRoleCode.ADMIN, "ann", pageable);
		}

		@Test
		void shouldReturnEmptyPageForMemberCallerTest() {
			// Given:
			AppUser caller = memberUser();
			Pageable pageable = PageRequest.of(0, 20);

			// When:
			Page<UserRecord> result = service.getTeamCandidateUsers(caller, "ann", pageable);

			// Then:
			assertThat(result.getContent()).isEmpty();
			verify(userEntityService, never()).findAllExcluding(any(), any(), any());
			verify(userEntityService, never()).findTeamCandidates(any(), any(), any(), any());
		}

		@Test
		void shouldReturnEmptyPageForNullCallerTest() {
			// Given:
			Pageable pageable = PageRequest.of(0, 20);

			// When:
			Page<UserRecord> result = service.getTeamCandidateUsers(null, "ann", pageable);

			// Then:
			assertThat(result.getContent()).isEmpty();
		}
	}

	@Nested
	class GetAssignableLearningUsers {

		@Test
		void getAssignableLearningUsersShouldReturnAllUsersExceptSelfForAdminCallerTest() {
			// Given:
			AppUser caller = adminUser();
			User user = Instancio.create(User.class);
			UserRecord userRecord = new UserRecord(5L, "clerk-other", "Other", "other@test.com", "member", null, null,
					null, null, null, null);
			when(userEntityService.findAll()).thenReturn(List.of(user));
			when(userMapper.toRecord(user)).thenReturn(userRecord);

			// When:
			List<UserRecord> result = service.getAssignableLearningUsers(caller);

			// Then:
			assertThat(result).containsExactly(userRecord);
		}

		@Test
		void getAssignableLearningUsersShouldReturnDeduplicatedMembersAcrossLedGroupsForTeamLeadTest() {
			// Given:
			AppUser caller = teamLeadUser();
			User memberOne = Instancio.of(User.class).set(field(User::getId), 10L).create();
			User memberTwo = Instancio.of(User.class).set(field(User::getId), 11L).create();
			UserRecord recordOne = new UserRecord(10L, "clerk-10", "Member One", "one@test.com", "member", null, null,
					null, null, null, null);
			UserRecord recordTwo = new UserRecord(11L, "clerk-11", "Member Two", "two@test.com", "member", null, null,
					null, null, null, null);

			when(groupLeadEntityService.findGroupIdsByLeadUserId(2L)).thenReturn(Set.of(100L, 200L));
			when(groupMemberEntityService.findByGroupIdIn(Set.of(100L, 200L))).thenReturn(List.of(
					groupMemberOf(100L, memberOne),
					groupMemberOf(200L, memberOne),
					groupMemberOf(200L, memberTwo)
			));
			when(userMapper.toRecord(memberOne)).thenReturn(recordOne);
			when(userMapper.toRecord(memberTwo)).thenReturn(recordTwo);

			// When:
			List<UserRecord> result = service.getAssignableLearningUsers(caller);

			// Then:
			assertThat(result).containsExactlyInAnyOrder(recordOne, recordTwo);
		}

		@Test
		void getAssignableLearningUsersShouldExcludeTheLeadThemselfFromLedGroupMembersTest() {
			// Given:
			AppUser caller = teamLeadUser();
			User lead = Instancio.of(User.class).set(field(User::getId), 2L).create();

			when(groupLeadEntityService.findGroupIdsByLeadUserId(2L)).thenReturn(Set.of(100L));
			when(groupMemberEntityService.findByGroupIdIn(Set.of(100L))).thenReturn(List.of(groupMemberOf(100L,
					lead)));

			// When:
			List<UserRecord> result = service.getAssignableLearningUsers(caller);

			// Then:
			assertThat(result).isEmpty();
			verify(userMapper, never()).toRecord(any(User.class));
		}

		@Test
		void getAssignableLearningUsersShouldReturnEmptyListForMemberCallerTest() {
			// Given:
			AppUser caller = memberUser();

			// When:
			List<UserRecord> result = service.getAssignableLearningUsers(caller);

			// Then:
			assertThat(result).isEmpty();
		}

		@Test
		void getAssignableLearningUsersShouldReturnEmptyListForNullCallerTest() {
			// When:
			List<UserRecord> result = service.getAssignableLearningUsers(null);

			// Then:
			assertThat(result).isEmpty();
		}
	}

	@Nested
	class GetAssignableLearningUsersPaged {

		@Test
		void shouldDelegateToBoundedExcludingQueryForAdminCallerTest() {
			// Given:
			AppUser caller = adminUser();
			Pageable pageable = PageRequest.of(0, 20);
			User user = Instancio.create(User.class);
			UserRecord userRecord = new UserRecord(5L, "clerk-5", "Other", "other@test.com", "member", null, null,
					null, null, null, null);
			Page<User> userPage = new PageImpl<>(List.of(user), pageable, 1);
			when(userEntityService.findAllExcluding(1L, "ann", pageable)).thenReturn(userPage);
			when(userMapper.toRecord(user)).thenReturn(userRecord);

			// When:
			Page<UserRecord> result = service.getAssignableLearningUsers(caller, "ann", pageable);

			// Then:
			assertThat(result.getContent()).containsExactly(userRecord);
			verify(groupLeadEntityService, never()).findGroupIdsByLeadUserId(any());
		}

		@Test
		void shouldResolveLedGroupIdsThenDelegateToBoundedGroupMemberQueryForTeamLeadCallerTest() {
			// Given:
			AppUser caller = teamLeadUser();
			Pageable pageable = PageRequest.of(0, 20);
			Set<Long> groupIds = Set.of(100L, 200L);
			User member = Instancio.of(User.class).set(field(User::getId), 10L).create();
			UserRecord memberRecord = new UserRecord(10L, "clerk-10", "Member", "member@test.com", "member", null,
					null, null, null, null, null);
			Page<User> memberPage = new PageImpl<>(List.of(member), pageable, 1);
			when(groupLeadEntityService.findGroupIdsByLeadUserId(2L)).thenReturn(groupIds);
			when(userEntityService.findGroupMemberUsersByGroupIdIn(groupIds, 2L, "mem", pageable))
					.thenReturn(memberPage);
			when(userMapper.toRecord(member)).thenReturn(memberRecord);

			// When:
			Page<UserRecord> result = service.getAssignableLearningUsers(caller, "mem", pageable);

			// Then: bounded to the lead's own groups via SQL, never loading every group member into Java
			assertThat(result.getContent()).containsExactly(memberRecord);
			verify(userEntityService).findGroupMemberUsersByGroupIdIn(groupIds, 2L, "mem", pageable);
			verify(groupMemberEntityService, never()).findByGroupIdIn(any());
		}

		@Test
		void shouldReturnEmptyPageForMemberCallerTest() {
			// Given:
			AppUser caller = memberUser();
			Pageable pageable = PageRequest.of(0, 20);

			// When:
			Page<UserRecord> result = service.getAssignableLearningUsers(caller, null, pageable);

			// Then:
			assertThat(result.getContent()).isEmpty();
			verify(groupLeadEntityService, never()).findGroupIdsByLeadUserId(any());
		}

		@Test
		void shouldReturnEmptyPageForNullCallerTest() {
			// Given:
			Pageable pageable = PageRequest.of(0, 20);

			// When:
			Page<UserRecord> result = service.getAssignableLearningUsers(null, null, pageable);

			// Then:
			assertThat(result.getContent()).isEmpty();
		}
	}

	@Nested
	class GetTeams {

		@Test
		void shouldBoundMembershipLookupToResolvedLeadIdsTest() {
			// Given:
			User lead = Instancio.of(User.class).set(field(User::getId), 2L).create();
			User member = Instancio.of(User.class).set(field(User::getId), 10L).create();
			UserRecord leadRecord = new UserRecord(2L, "clerk-2", "Lead", "lead@test.com", "teamlead", null, null,
					null, null, null, null);
			UserRecord memberRecord = new UserRecord(10L, "clerk-10", "Member", "member@test.com", "member", null,
					null, null, null, null, null);
			when(userEntityService.findByRoleCodeIn(List.of(UserRoleCode.ADMIN, UserRoleCode.TEAMLEAD)))
					.thenReturn(List.of(lead));
			when(teamEntityService.findByLeadUserIdIn(List.of(2L))).thenReturn(List.of(teamMemberOf(2L, member)));
			when(userMapper.toRecord(lead)).thenReturn(leadRecord);
			when(userMapper.toRecord(member)).thenReturn(memberRecord);

			// When:
			List<TeamRecord> result = service.getTeams();

			// Then: membership lookup is bounded to exactly the resolved lead IDs, not every lead
			assertThat(result).containsExactly(new TeamRecord(leadRecord, List.of(memberRecord)));
			verify(teamEntityService).findByLeadUserIdIn(List.of(2L));
		}
	}

	@Nested
	class GetTeamsPaged {

		@Test
		void shouldBoundMembershipLookupToTheCurrentLeadPageForAdminCallerTest() {
			// Given:
			AppUser caller = adminUser();
			Pageable pageable = PageRequest.of(0, 20);
			User lead = Instancio.of(User.class).set(field(User::getId), 7L).create();
			User member = Instancio.of(User.class).set(field(User::getId), 11L).create();
			UserRecord leadRecord = new UserRecord(7L, "clerk-7", "Lead", "lead7@test.com", "teamlead", null, null,
					null, null, null, null);
			UserRecord memberRecord = new UserRecord(11L, "clerk-11", "Member", "member@test.com", "member", null,
					null, null, null, null, null);
			Page<User> leadPage = new PageImpl<>(List.of(lead), pageable, 1);
			when(userEntityService.findByRoleCodeIn(List.of(UserRoleCode.ADMIN, UserRoleCode.TEAMLEAD), "q", pageable))
					.thenReturn(leadPage);
			when(teamEntityService.findByLeadUserIdIn(List.of(7L))).thenReturn(List.of(teamMemberOf(7L, member)));
			when(userMapper.toRecord(lead)).thenReturn(leadRecord);
			when(userMapper.toRecord(member)).thenReturn(memberRecord);

			// When:
			Page<TeamRecord> result = service.getTeams(caller, "q", pageable);

			// Then: membership lookup is bounded to the current page's lead IDs only
			assertThat(result.getContent()).containsExactly(new TeamRecord(leadRecord, List.of(memberRecord)));
			verify(teamEntityService).findByLeadUserIdIn(List.of(7L));
		}

		@Test
		void shouldReturnSingleLeadPageForTeamLeadCallerTest() {
			// Given:
			AppUser caller = teamLeadUser();
			Pageable pageable = PageRequest.of(0, 20);
			User lead = userWithIdAndRole(2L, UserRoleCode.TEAMLEAD);
			lead.setName("Lead");
			lead.setEmail("lead@test.com");
			UserRecord leadRecord = new UserRecord(2L, "clerk-2", "Lead", "lead@test.com", "teamlead", null, null,
					null, null, null, null);
			when(userEntityService.findById(2L)).thenReturn(Optional.of(lead));
			when(userMapper.toRecord(lead)).thenReturn(leadRecord);

			// When:
			Page<TeamRecord> result = service.getTeams(caller, "lead", pageable);

			// Then:
			assertThat(result.getContent()).containsExactly(new TeamRecord(leadRecord, List.of()));
			verify(teamEntityService).findByLeadUserIdIn(List.of(2L));
			verify(teamEntityService, never()).findByLeadUserIdIn(List.of());
		}

		@Test
		void shouldReturnEmptyPageWhenTeamLeadDoesNotMatchQueryTest() {
			// Given:
			AppUser caller = teamLeadUser();
			Pageable pageable = PageRequest.of(0, 20);
			User lead = userWithIdAndRole(2L, UserRoleCode.TEAMLEAD);
			lead.setName("Lead");
			lead.setEmail("lead@test.com");
			when(userEntityService.findById(2L)).thenReturn(Optional.of(lead));

			// When:
			Page<TeamRecord> result = service.getTeams(caller, "zzz", pageable);

			// Then:
			assertThat(result.getContent()).isEmpty();
		}

		@Test
		void shouldReturnEmptyPageForMemberCallerTest() {
			// Given:
			AppUser caller = memberUser();
			Pageable pageable = PageRequest.of(0, 20);

			// When:
			Page<TeamRecord> result = service.getTeams(caller, null, pageable);

			// Then:
			assertThat(result.getContent()).isEmpty();
			verify(teamEntityService, never()).findByLeadUserIdIn(any());
		}
	}

	@Nested
	class GetUserByIdAndEmail {

		@Test
		void shouldReturnUserRecordByIdWhenPresentTest() {
			// Given:
			User user = userWithIdAndRole(5L, UserRoleCode.MEMBER);
			UserRecord record = new UserRecord(5L, "clerk-5", "Member", "member@test.com", "member", null, null, null,
					null, null, null);
			when(userEntityService.findById(5L)).thenReturn(Optional.of(user));
			when(userMapper.toRecord(user)).thenReturn(record);

			// When:
			Optional<UserRecord> result = service.getUserById(5L);

			// Then:
			assertThat(result).hasValue(record);
		}

		@Test
		void shouldReturnEmptyByIdWhenMissingTest() {
			// Given:
			when(userEntityService.findById(5L)).thenReturn(Optional.empty());

			// When:
			Optional<UserRecord> result = service.getUserById(5L);

			// Then:
			assertThat(result).isEmpty();
		}

		@Test
		void shouldReturnUserRecordByEmailWithNormalizedCasingTest() {
			// Given:
			User user = userWithIdAndRole(5L, UserRoleCode.MEMBER);
			user.setEmail("Member@Test.COM");
			UserRecord record = new UserRecord(5L, "clerk-5", "Member", "member@test.com", "member", null, null, null,
					null, null, null);
			when(userEntityService.findByEmail("member@test.com")).thenReturn(Optional.of(user));
			when(userMapper.toRecord(user)).thenReturn(record);

			// When:
			Optional<UserRecord> result = service.getUserByEmail("  Member@Test.COM  ");

			// Then:
			assertThat(result).hasValue(record);
		}

		@Test
		void shouldReturnEmptyByEmailWhenMissingTest() {
			// Given:
			when(userEntityService.findByEmail("member@test.com")).thenReturn(Optional.empty());

			// When:
			Optional<UserRecord> result = service.getUserByEmail("member@test.com");

			// Then:
			assertThat(result).isEmpty();
		}
	}

	@Nested
	class PromoteTeamLeadByEmail {

		@Test
		void shouldReturnEmptyWhenUserNotFoundTest() {
			// Given:
			when(userEntityService.findByEmail("member@test.com")).thenReturn(Optional.empty());

			// When:
			Optional<UserRecord> result = service.promoteTeamLeadByEmail("member@test.com");

			// Then:
			assertThat(result).isEmpty();
			verifyNoInteractions(dictionaryLookupService);
		}

		@Test
		void shouldNotChangeRoleWhenUserAlreadyAdminTest() {
			// Given:
			User admin = userWithIdAndRole(5L, UserRoleCode.ADMIN);
			LocalDateTime now = LocalDateTime.of(2026, 1, 1, 0, 0);
			UserRecord record = new UserRecord(5L, "clerk-5", "Admin", "admin@test.com", "admin", null, null, null,
					null, null, null);
			when(userEntityService.findByEmail("admin@test.com")).thenReturn(Optional.of(admin));
			when(currentTime.utcDateTime()).thenReturn(now);
			when(userEntityService.save(admin)).thenReturn(admin);
			when(userMapper.toRecord(admin)).thenReturn(record);

			// When:
			Optional<UserRecord> result = service.promoteTeamLeadByEmail("admin@test.com");

			// Then:
			assertThat(result).hasValue(record);
			assertThat(admin.getRole().getCode()).isEqualTo(UserRoleCode.ADMIN);
			assertThat(admin.getUpdatedAt()).isEqualTo(now);
			verify(dictionaryLookupService, never()).getUserRoleReference(any());
		}

		@Test
		void shouldPromoteMemberToTeamLeadTest() {
			// Given:
			User member = userWithIdAndRole(5L, UserRoleCode.MEMBER);
			UserRole teamLeadRole = roleOf(UserRoleCode.TEAMLEAD);
			LocalDateTime now = LocalDateTime.of(2026, 1, 1, 0, 0);
			UserRecord record = new UserRecord(5L, "clerk-5", "Lead", "lead@test.com", "teamlead", null, null, null,
					null, null, null);
			when(userEntityService.findByEmail("member@test.com")).thenReturn(Optional.of(member));
			when(dictionaryLookupService.getUserRoleReference(UserRoleCode.TEAMLEAD)).thenReturn(teamLeadRole);
			when(currentTime.utcDateTime()).thenReturn(now);
			when(userEntityService.save(member)).thenReturn(member);
			when(userMapper.toRecord(member)).thenReturn(record);

			// When:
			Optional<UserRecord> result = service.promoteTeamLeadByEmail("member@test.com");

			// Then:
			assertThat(result).hasValue(record);
			assertThat(member.getRole()).isSameAs(teamLeadRole);
			assertThat(member.getUpdatedAt()).isEqualTo(now);
		}
	}

	@Nested
	class DemoteTeamLeadByEmail {

		@Test
		void shouldReturnEmptyWhenUserNotFoundTest() {
			// Given:
			when(userEntityService.findByEmail("lead@test.com")).thenReturn(Optional.empty());

			// When:
			Optional<UserRecord> result = service.demoteTeamLeadByEmail("lead@test.com");

			// Then:
			assertThat(result).isEmpty();
		}

		@Test
		void shouldReturnEmptyWhenUserIsNotTeamLeadTest() {
			// Given:
			User member = userWithIdAndRole(5L, UserRoleCode.MEMBER);
			when(userEntityService.findByEmail("member@test.com")).thenReturn(Optional.of(member));

			// When:
			Optional<UserRecord> result = service.demoteTeamLeadByEmail("member@test.com");

			// Then:
			assertThat(result).isEmpty();
			verify(teamEntityService, never()).deleteByIdLeadUserId(any());
		}

		@Test
		void shouldDemoteTeamLeadToMemberAndDeleteTeamMembershipsTest() {
			// Given:
			User lead = userWithIdAndRole(5L, UserRoleCode.TEAMLEAD);
			UserRole memberRole = roleOf(UserRoleCode.MEMBER);
			LocalDateTime now = LocalDateTime.of(2026, 1, 1, 0, 0);
			UserRecord record = new UserRecord(5L, "clerk-5", "Member", "lead@test.com", "member", null, null, null,
					null, null, null);
			when(userEntityService.findByEmail("lead@test.com")).thenReturn(Optional.of(lead));
			when(dictionaryLookupService.getUserRoleReference(UserRoleCode.MEMBER)).thenReturn(memberRole);
			when(currentTime.utcDateTime()).thenReturn(now);
			when(userEntityService.save(lead)).thenReturn(lead);
			when(userMapper.toRecord(lead)).thenReturn(record);

			// When:
			Optional<UserRecord> result = service.demoteTeamLeadByEmail("lead@test.com");

			// Then:
			assertThat(result).hasValue(record);
			assertThat(lead.getRole()).isSameAs(memberRole);
			assertThat(lead.getUpdatedAt()).isEqualTo(now);
			verify(teamEntityService).deleteByIdLeadUserId(5L);
		}
	}

	@Nested
	class AddTeamMember {

		@Test
		void shouldAddMemberByUserIdWhenLeadIsAdminTest() {
			// Given:
			Long leadUserId = 1L;
			Long memberUserId = 5L;
			User lead = userWithIdAndRole(leadUserId, UserRoleCode.ADMIN);
			User member = userWithIdAndRole(memberUserId, UserRoleCode.MEMBER);
			member.setName("Member");
			UserRecord record = new UserRecord(memberUserId, "clerk-5", "Member", "member@test.com", "member", null,
					null, null, null, null, null);
			LocalDateTime now = LocalDateTime.of(2026, 1, 1, 0, 0);
			when(userEntityService.findById(leadUserId)).thenReturn(Optional.of(lead));
			when(userEntityService.findById(memberUserId)).thenReturn(Optional.of(member));
			when(userEntityService.getReference(leadUserId)).thenReturn(lead);
			when(currentTime.utcDateTime()).thenReturn(now);
			when(userMapper.toRecord(member)).thenReturn(record);

			// When:
			UserRecord result = service.addTeamMember(leadUserId, memberUserId, null);

			// Then:
			assertThat(result).isSameAs(record);
			ArgumentCaptor<TeamMember> captor = ArgumentCaptor.forClass(TeamMember.class);
			verify(teamEntityService).save(captor.capture());
			assertThat(captor.getValue().getId().getLeadUserId()).isEqualTo(leadUserId);
			assertThat(captor.getValue().getId().getMemberUserId()).isEqualTo(memberUserId);
			assertThat(captor.getValue().getAddedAt()).isEqualTo(now);
		}

		@Test
		void shouldAddMemberByEmailOrNameWhenUserIdNotProvidedTest() {
			// Given:
			Long leadUserId = 1L;
			User lead = userWithIdAndRole(leadUserId, UserRoleCode.ADMIN);
			User member = userWithIdAndRole(5L, UserRoleCode.MEMBER);
			member.setName("Member");
			UserRecord record = new UserRecord(5L, "clerk-5", "Member", "member@test.com", "member", null, null, null,
					null, null, null);
			LocalDateTime now = LocalDateTime.of(2026, 1, 1, 0, 0);
			when(userEntityService.findById(leadUserId)).thenReturn(Optional.of(lead));
			when(userEntityService.findByEmail("member@test.com")).thenReturn(Optional.of(member));
			when(userEntityService.getReference(leadUserId)).thenReturn(lead);
			when(currentTime.utcDateTime()).thenReturn(now);
			when(userMapper.toRecord(member)).thenReturn(record);

			// When:
			UserRecord result = service.addTeamMember(leadUserId, null, "member@test.com");

			// Then:
			assertThat(result).isSameAs(record);
			verify(teamEntityService).save(any(TeamMember.class));
		}

		@Test
		void shouldThrowWhenLeadIsNotAdminOrTeamLeadTest() {
			// Given:
			Long leadUserId = 1L;
			User lead = userWithIdAndRole(leadUserId, UserRoleCode.MEMBER);
			when(userEntityService.findById(leadUserId)).thenReturn(Optional.of(lead));

			// When-Then:
			assertThatThrownBy(() -> service.addTeamMember(leadUserId, 5L, null))
					.isInstanceOf(AppException.class)
					.hasMessageContaining("Team lead must be admin or team lead");
		}

		@Test
		void shouldThrowWhenMemberNotFoundTest() {
			// Given:
			Long leadUserId = 1L;
			User lead = userWithIdAndRole(leadUserId, UserRoleCode.ADMIN);
			when(userEntityService.findById(leadUserId)).thenReturn(Optional.of(lead));
			when(userEntityService.findById(5L)).thenReturn(Optional.empty());

			// When-Then:
			assertThatThrownBy(() -> service.addTeamMember(leadUserId, 5L, null))
					.isInstanceOf(AppException.class)
					.hasMessageContaining("member");
		}

		@Test
		void shouldThrowWhenLeadAddsThemselfTest() {
			// Given:
			Long leadUserId = 1L;
			User lead = userWithIdAndRole(leadUserId, UserRoleCode.ADMIN);
			when(userEntityService.findById(leadUserId)).thenReturn(Optional.of(lead));
			when(userEntityService.findById(leadUserId)).thenReturn(Optional.of(lead));

			// When-Then:
			assertThatThrownBy(() -> service.addTeamMember(leadUserId, leadUserId, null))
					.isInstanceOf(AppException.class)
					.hasMessageContaining("cannot be added to their own team");
		}
	}

	@Nested
	class RemoveTeamMember {

		@Test
		void shouldReturnTrueAndDeleteWhenMembershipExistsTest() {
			// Given:
			Long leadUserId = 1L;
			Long memberUserId = 5L;
			User lead = userWithIdAndRole(leadUserId, UserRoleCode.ADMIN);
			TeamMember.TeamMemberId id = new TeamMember.TeamMemberId();
			id.setLeadUserId(leadUserId);
			id.setMemberUserId(memberUserId);
			when(userEntityService.findById(leadUserId)).thenReturn(Optional.of(lead));
			when(teamEntityService.existsById(id)).thenReturn(true);

			// When:
			boolean result = service.removeTeamMember(leadUserId, memberUserId);

			// Then:
			assertThat(result).isTrue();
			verify(teamEntityService).deleteById(id);
		}

		@Test
		void shouldReturnFalseWhenMembershipMissingTest() {
			// Given:
			Long leadUserId = 1L;
			Long memberUserId = 5L;
			User lead = userWithIdAndRole(leadUserId, UserRoleCode.ADMIN);
			TeamMember.TeamMemberId id = new TeamMember.TeamMemberId();
			id.setLeadUserId(leadUserId);
			id.setMemberUserId(memberUserId);
			when(userEntityService.findById(leadUserId)).thenReturn(Optional.of(lead));
			when(teamEntityService.existsById(id)).thenReturn(false);

			// When:
			boolean result = service.removeTeamMember(leadUserId, memberUserId);

			// Then:
			assertThat(result).isFalse();
			verify(teamEntityService, never()).deleteById(id);
		}
	}

	@Nested
	class ResolveByEmailOrName {

		@Test
		void shouldReturnNullForNullOrBlankValueTest() {
			// When:
			User nullResult = service.resolveByEmailOrName(null);
			User blankResult = service.resolveByEmailOrName("   ");

			// Then:
			assertThat(nullResult).isNull();
			assertThat(blankResult).isNull();
		}

		@Test
		void shouldResolveByEmailFirstTest() {
			// Given:
			User user = userWithIdAndRole(5L, UserRoleCode.MEMBER);
			when(userEntityService.findByEmail("member@test.com")).thenReturn(Optional.of(user));

			// When:
			User result = service.resolveByEmailOrName("member@test.com");

			// Then:
			assertThat(result).isSameAs(user);
			verify(userEntityService, never()).findByNameIgnoreCase(any());
		}

		@Test
		void shouldFallbackToNameSearchWhenEmailNotFoundTest() {
			// Given:
			User user = userWithIdAndRole(5L, UserRoleCode.MEMBER);
			when(userEntityService.findByEmail("member@test.com")).thenReturn(Optional.empty());
			when(userEntityService.findByNameIgnoreCase("member@test.com")).thenReturn(Optional.of(user));

			// When:
			User result = service.resolveByEmailOrName("member@test.com");

			// Then:
			assertThat(result).isSameAs(user);
		}
	}

	@Nested
	class SingleVisibleLeadPage {

		@Test
		void shouldReturnEmptyPageWhenLeadNotFoundTest() {
			// Given:
			Pageable pageable = PageRequest.of(0, 20);
			when(userEntityService.findById(2L)).thenReturn(Optional.empty());

			// When:
			Page<User> result = service.singleVisibleLeadPage(2L, "lead", pageable);

			// Then:
			assertThat(result.getContent()).isEmpty();
		}

		@Test
		void shouldReturnEmptyPageWhenLeadDoesNotMatchQueryTest() {
			// Given:
			Pageable pageable = PageRequest.of(0, 20);
			User lead = userWithIdAndRole(2L, UserRoleCode.TEAMLEAD);
			lead.setName("Lead");
			lead.setEmail("lead@test.com");
			when(userEntityService.findById(2L)).thenReturn(Optional.of(lead));

			// When:
			Page<User> result = service.singleVisibleLeadPage(2L, "zzz", pageable);

			// Then:
			assertThat(result.getContent()).isEmpty();
		}

		@Test
		void shouldReturnEmptyContentForOffsetGreaterThanZeroTest() {
			// Given:
			Pageable pageable = PageRequest.of(1, 20);
			User lead = userWithIdAndRole(2L, UserRoleCode.TEAMLEAD);
			lead.setName("Lead");
			lead.setEmail("lead@test.com");
			when(userEntityService.findById(2L)).thenReturn(Optional.of(lead));

			// When:
			Page<User> result = service.singleVisibleLeadPage(2L, "lead", pageable);

			// Then:
			assertThat(result.getContent()).isEmpty();
			assertThat(result.getTotalElements()).isEqualTo(1L);
		}
	}

	@Nested
	class MatchesUserSearch {

		@Test
		void shouldReturnTrueForNullOrBlankQueryTest() {
			// Given:
			User user = userWithIdAndRole(1L, UserRoleCode.MEMBER);
			user.setName("Alice");
			user.setEmail("alice@test.com");

			// When:
			boolean nullResult = service.matchesUserSearch(user, null);
			boolean blankResult = service.matchesUserSearch(user, "  ");

			// Then:
			assertThat(nullResult).isTrue();
			assertThat(blankResult).isTrue();
		}

		@Test
		void shouldMatchByNameOrEmailTest() {
			// Given:
			User user = userWithIdAndRole(1L, UserRoleCode.MEMBER);
			user.setName("Alice Smith");
			user.setEmail("alice.smith@test.com");

			// When:
			boolean byName = service.matchesUserSearch(user, "alice");
			boolean byEmail = service.matchesUserSearch(user, "test.com");
			boolean noMatch = service.matchesUserSearch(user, "bob");

			// Then:
			assertThat(byName).isTrue();
			assertThat(byEmail).isTrue();
			assertThat(noMatch).isFalse();
		}
	}
}
