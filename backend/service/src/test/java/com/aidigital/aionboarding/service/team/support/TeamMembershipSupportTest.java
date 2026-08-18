package com.aidigital.aionboarding.service.team.support;

import com.aidigital.aionboarding.domain.common.dictionary.UserRoleCode;
import com.aidigital.aionboarding.domain.common.dictionary.entities.UserRole;
import com.aidigital.aionboarding.domain.team.entities.TeamMember;
import com.aidigital.aionboarding.domain.user.entities.User;
import com.aidigital.aionboarding.service.common.error.AppException;
import com.aidigital.aionboarding.service.common.time.CurrentTime;
import com.aidigital.aionboarding.service.mappers.user.UserRecordMapper;
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

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.instancio.Select.field;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TeamMembershipSupportTest {

	@Mock
	private UserEntityService userEntityService;
	@Mock
	private TeamEntityService teamEntityService;
	@Mock
	private UserRecordMapper userMapper;
	@Mock
	private CurrentTime currentTime;

	@InjectMocks
	private TeamMembershipSupport support;

	private User userWithIdAndRole(Long id, String roleCode) {
		User user = Instancio.of(User.class).set(field(User::getId), id).create();
		UserRole role = new UserRole();
		role.setCode(roleCode);
		user.setRole(role);
		return user;
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
			UserRecord result = support.addTeamMember(leadUserId, memberUserId, null);

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
			UserRecord result = support.addTeamMember(leadUserId, null, "member@test.com");

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
			assertThatThrownBy(() -> support.addTeamMember(leadUserId, 5L, null))
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
			assertThatThrownBy(() -> support.addTeamMember(leadUserId, 5L, null))
					.isInstanceOf(AppException.class)
					.hasMessageContaining("member");
		}

		@Test
		void shouldThrowWhenLeadAddsThemselfTest() {
			// Given:
			Long leadUserId = 1L;
			User lead = userWithIdAndRole(leadUserId, UserRoleCode.ADMIN);
			when(userEntityService.findById(leadUserId)).thenReturn(Optional.of(lead));

			// When-Then:
			assertThatThrownBy(() -> support.addTeamMember(leadUserId, leadUserId, null))
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
			boolean result = support.removeTeamMember(leadUserId, memberUserId);

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
			boolean result = support.removeTeamMember(leadUserId, memberUserId);

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
			User nullResult = support.resolveByEmailOrName(null);
			User blankResult = support.resolveByEmailOrName("   ");

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
			User result = support.resolveByEmailOrName("member@test.com");

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
			User result = support.resolveByEmailOrName("member@test.com");

			// Then:
			assertThat(result).isSameAs(user);
		}
	}
}
