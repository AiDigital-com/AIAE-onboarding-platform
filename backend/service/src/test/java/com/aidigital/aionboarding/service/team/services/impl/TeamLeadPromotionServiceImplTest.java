package com.aidigital.aionboarding.service.team.services.impl;

import com.aidigital.aionboarding.domain.common.dictionary.UserRoleCode;
import com.aidigital.aionboarding.domain.common.dictionary.entities.UserRole;
import com.aidigital.aionboarding.domain.user.entities.User;
import com.aidigital.aionboarding.service.common.dictionary.DictionaryLookupService;
import com.aidigital.aionboarding.service.common.time.CurrentTime;
import com.aidigital.aionboarding.service.mappers.user.UserRecordMapper;
import com.aidigital.aionboarding.service.team.services.entity.TeamEntityService;
import com.aidigital.aionboarding.service.user.models.UserRecord;
import com.aidigital.aionboarding.service.user.services.entity.UserEntityService;
import org.instancio.Instancio;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.instancio.Select.field;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TeamLeadPromotionServiceImplTest {

	@Mock
	private UserEntityService userEntityService;
	@Mock
	private TeamEntityService teamEntityService;
	@Mock
	private DictionaryLookupService dictionaryLookupService;
	@Mock
	private UserRecordMapper userMapper;
	@Mock
	private CurrentTime currentTime;

	@InjectMocks
	private TeamLeadPromotionServiceImpl service;

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
			org.mockito.Mockito.verifyNoInteractions(dictionaryLookupService);
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
}
