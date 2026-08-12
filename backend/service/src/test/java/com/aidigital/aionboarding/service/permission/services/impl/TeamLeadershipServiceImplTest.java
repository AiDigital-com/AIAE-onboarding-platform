package com.aidigital.aionboarding.service.permission.services.impl;

import com.aidigital.aionboarding.service.group.services.entity.GroupLeadEntityService;
import com.aidigital.aionboarding.service.group.services.entity.GroupMemberEntityService;
import com.aidigital.aionboarding.service.team.services.entity.TeamEntityService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TeamLeadershipServiceImplTest {

	@Mock
	private TeamEntityService teamEntityService;
	@Mock
	private GroupLeadEntityService groupLeadEntityService;
	@Mock
	private GroupMemberEntityService groupMemberEntityService;

	@InjectMocks
	private TeamLeadershipServiceImpl service;

	@Test
	void isTeamLeadForMemberShouldReturnTrueViaLegacyTeamMembershipTest() {
		// Given:
		Long leadUserId = 40L;
		Long memberUserId = 41L;
		when(teamEntityService.existsByIdLeadUserIdAndIdMemberUserId(leadUserId, memberUserId)).thenReturn(true);

		// When:
		boolean result = service.isTeamLeadForMember(leadUserId, memberUserId);

		// Then:
		assertThat(result).isTrue();
	}

	@Test
	void isTeamLeadForMemberShouldReturnTrueWhenMemberBelongsToALedGroupTest() {
		// Given:
		Long leadUserId = 42L;
		Long memberUserId = 43L;
		when(teamEntityService.existsByIdLeadUserIdAndIdMemberUserId(leadUserId, memberUserId)).thenReturn(false);
		when(groupLeadEntityService.findGroupIdsByLeadUserId(leadUserId)).thenReturn(Set.of(100L, 200L));
		when(groupMemberEntityService.findGroupIdsByMemberUserId(memberUserId)).thenReturn(Set.of(200L, 300L));

		// When:
		boolean result = service.isTeamLeadForMember(leadUserId, memberUserId);

		// Then:
		assertThat(result).isTrue();
	}

	@Test
	void isTeamLeadForMemberShouldReturnFalseWhenLeadHasNoLedGroupsTest() {
		// Given:
		Long leadUserId = 44L;
		Long memberUserId = 45L;
		when(teamEntityService.existsByIdLeadUserIdAndIdMemberUserId(leadUserId, memberUserId)).thenReturn(false);
		when(groupLeadEntityService.findGroupIdsByLeadUserId(leadUserId)).thenReturn(Set.of());

		// When:
		boolean result = service.isTeamLeadForMember(leadUserId, memberUserId);

		// Then:
		assertThat(result).isFalse();
		verify(groupMemberEntityService, never()).findGroupIdsByMemberUserId(memberUserId);
	}

	@Test
	void isTeamLeadForMemberShouldReturnFalseWhenMemberIsNotInAnyLedGroupTest() {
		// Given:
		Long leadUserId = 46L;
		Long memberUserId = 47L;
		when(teamEntityService.existsByIdLeadUserIdAndIdMemberUserId(leadUserId, memberUserId)).thenReturn(false);
		when(groupLeadEntityService.findGroupIdsByLeadUserId(leadUserId)).thenReturn(Set.of(100L));
		when(groupMemberEntityService.findGroupIdsByMemberUserId(memberUserId)).thenReturn(Set.of(200L));

		// When:
		boolean result = service.isTeamLeadForMember(leadUserId, memberUserId);

		// Then:
		assertThat(result).isFalse();
	}
}
