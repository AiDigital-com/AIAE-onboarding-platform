package com.aidigital.aionboarding.service.teamdashboard.services.impl;

import com.aidigital.aionboarding.domain.common.dictionary.UserRoleCode;
import com.aidigital.aionboarding.service.common.security.AppUser;
import com.aidigital.aionboarding.service.common.time.CurrentTime;
import com.aidigital.aionboarding.service.teamdashboard.models.TeamDashboardIndividualRoadmapRecord;
import com.aidigital.aionboarding.service.teamdashboard.models.TeamDashboardKpisRecord;
import com.aidigital.aionboarding.service.teamdashboard.models.TeamDashboardPeriod;
import com.aidigital.aionboarding.service.teamdashboard.models.TeamDashboardRecord;
import com.aidigital.aionboarding.service.teamdashboard.models.TeamDashboardScope;
import com.aidigital.aionboarding.service.teamdashboard.services.TeamDashboardQueryService;
import com.aidigital.aionboarding.service.teamdashboard.services.entity.TeamDashboardQueryEntityService;
import com.aidigital.aionboarding.service.teamdashboard.support.TeamDashboardRecordAssembler;
import com.aidigital.aionboarding.service.teamdashboard.support.TeamDashboardScopeResolver;
import com.aidigital.aionboarding.service.user.models.UserRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TeamDashboardServiceImplTest {

	@Mock
	private TeamDashboardQueryEntityService teamDashboardQueryEntityService;
	@Mock
	private TeamDashboardScopeResolver teamDashboardScopeResolver;
	@Mock
	private TeamDashboardQueryService teamDashboardQueryService;
	@Mock
	private TeamDashboardRecordAssembler teamDashboardMapper;

	@Spy
	private CurrentTime currentTime = new CurrentTime();

	@InjectMocks
	private TeamDashboardServiceImpl service;

	@Test
	void getTeamDashboardDataShouldReturnEmptyStatListsWhenScopeHasNoMembersTest() {
		// Given:
		AppUser viewer = new AppUser(1L, "clerk-lead", "lead@test.com", "Lead", UserRoleCode.TEAMLEAD, "Lead", null,
				null, null);
		TeamDashboardScope emptyScope = new TeamDashboardScope("My team", null, List.of());
		when(teamDashboardScopeResolver.resolveVisibleTeamScope(viewer)).thenReturn(emptyScope);
		TeamDashboardKpisRecord kpis = new TeamDashboardKpisRecord(0, 0, null);
		when(teamDashboardMapper.toKpisRecord(List.of())).thenReturn(kpis);

		// When:
		TeamDashboardRecord result = service.getTeamDashboardData(viewer, TeamDashboardPeriod.WEEK);

		// Then:
		assertThat(result.teamName()).isEqualTo("My team");
		assertThat(result.members()).isEmpty();
		assertThat(result.roadmaps()).isEmpty();
		assertThat(result.weekly()).isEmpty();
		assertThat(result.lowConfidenceLessons()).isEmpty();
		assertThat(result.recentActivity()).isEmpty();
		assertThat(result.individualRoadmapsByMemberId()).isEmpty();
		assertThat(result.kpis()).isSameAs(kpis);
		verifyNoInteractions(teamDashboardQueryEntityService);
	}

	@Test
	void getTeamDashboardDataShouldAssembleRecordFromQueryResultsWhenScopeHasMembersTest() {
		// Given:
		AppUser viewer = new AppUser(1L, "clerk-lead", "lead@test.com", "Lead", UserRoleCode.TEAMLEAD, "Lead", null,
				null, null);
		UserRecord lead = new UserRecord(1L, "clerk-lead", "Lead", "lead@test.com", UserRoleCode.TEAMLEAD, null, null,
				null, null, null, null);
		UserRecord member = new UserRecord(2L, "clerk-member", "Member", "member@test.com", UserRoleCode.MEMBER, null,
				null, null, null, null, null);
		TeamDashboardScope scope = new TeamDashboardScope("My team", 1L, List.of(lead, member));
		when(teamDashboardScopeResolver.resolveVisibleTeamScope(viewer)).thenReturn(scope);

		when(teamDashboardQueryEntityService.findMemberStats(List.of(1L, 2L))).thenReturn(List.of());
		when(teamDashboardQueryEntityService.findRoadmapStats(List.of(1L, 2L))).thenReturn(List.of());
		when(teamDashboardQueryEntityService.findWeeklyActivity(List.of(1L, 2L))).thenReturn(List.of());
		when(teamDashboardQueryEntityService.findLowConfidenceLessons(List.of(1L, 2L), 1L)).thenReturn(List.of());
		when(teamDashboardQueryEntityService.findRecentActivity(List.of(1L, 2L))).thenReturn(List.of());

		List<TeamDashboardIndividualRoadmapRecord> leadRoadmaps = List.of();
		List<TeamDashboardIndividualRoadmapRecord> memberRoadmaps = List.of();
		when(teamDashboardQueryService.getIndividualRoadmapDetailsByMemberIds(List.of(1L, 2L)))
				.thenReturn(Map.of("1", leadRoadmaps, "2", memberRoadmaps));

		TeamDashboardKpisRecord kpis = new TeamDashboardKpisRecord(0, 0, null);
		when(teamDashboardMapper.toKpisRecord(List.of())).thenReturn(kpis);

		// When:
		TeamDashboardRecord result = service.getTeamDashboardData(viewer, TeamDashboardPeriod.WEEK);

		// Then:
		assertThat(result.teamName()).isEqualTo("My team");
		assertThat(result.individualRoadmapsByMemberId()).containsOnlyKeys("1", "2");
		assertThat(result.individualRoadmapsByMemberId().get("1")).isSameAs(leadRoadmaps);
		assertThat(result.individualRoadmapsByMemberId().get("2")).isSameAs(memberRoadmaps);
		assertThat(result.kpis()).isSameAs(kpis);
		verify(teamDashboardQueryEntityService).findMemberStats(List.of(1L, 2L));
		verify(teamDashboardQueryEntityService).findRoadmapStats(List.of(1L, 2L));
		verify(teamDashboardQueryEntityService).findWeeklyActivity(List.of(1L, 2L));
		verify(teamDashboardQueryEntityService).findLowConfidenceLessons(List.of(1L, 2L), 1L);
		verify(teamDashboardQueryEntityService).findRecentActivity(List.of(1L, 2L));
	}
}
