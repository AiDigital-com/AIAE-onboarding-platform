package com.aidigital.aionboarding.service.teamdashboard.services.entity;

import com.aidigital.aionboarding.domain.common.dictionary.LessonPublicationStatusCode;
import com.aidigital.aionboarding.domain.common.dictionary.LessonStatusCode;
import com.aidigital.aionboarding.domain.teamdashboard.repositories.MemberStatsProjection;
import com.aidigital.aionboarding.domain.teamdashboard.repositories.RoadmapStatsProjection;
import com.aidigital.aionboarding.domain.teamdashboard.repositories.TeamDashboardRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TeamDashboardQueryEntityServiceTest {

	@Mock
	private TeamDashboardRepository teamDashboardRepository;

	@InjectMocks
	private TeamDashboardQueryEntityService teamDashboardQueryEntityService;

	@Test
	void findMemberStatsShouldForwardTheLearnableLessonCodesToTheRepositoryTest() {
		// Given: the caller only supplies member ids — the learnable-lesson codes that keep a
		// member's roadmap-lesson count aligned with what roadmap fan-out actually grants are
		// resolved here, not by the caller.
		List<Long> memberIds = List.of(1L, 2L);
		List<MemberStatsProjection> repositoryResult = List.of();
		when(teamDashboardRepository.findMemberStats(
				memberIds, LessonStatusCode.READY, LessonPublicationStatusCode.PUBLISHED, LessonPublicationStatusCode.PRIVATE
		)).thenReturn(repositoryResult);

		// When:
		List<MemberStatsProjection> result = teamDashboardQueryEntityService.findMemberStats(memberIds);

		// Then:
		assertThat(result).isSameAs(repositoryResult);
		verify(teamDashboardRepository).findMemberStats(
				memberIds, LessonStatusCode.READY, LessonPublicationStatusCode.PUBLISHED, LessonPublicationStatusCode.PRIVATE
		);
	}

	@Test
	void findRoadmapStatsShouldForwardTheLearnableLessonCodesToTheRepositoryTest() {
		// Given: same requirement as findMemberStats above, for the roadmap-level aggregate.
		List<Long> memberIds = List.of(10L, 20L);
		List<RoadmapStatsProjection> repositoryResult = List.of();
		when(teamDashboardRepository.findRoadmapStats(
				memberIds, LessonStatusCode.READY, LessonPublicationStatusCode.PUBLISHED, LessonPublicationStatusCode.PRIVATE
		)).thenReturn(repositoryResult);

		// When:
		List<RoadmapStatsProjection> result = teamDashboardQueryEntityService.findRoadmapStats(memberIds);

		// Then:
		assertThat(result).isSameAs(repositoryResult);
		verify(teamDashboardRepository).findRoadmapStats(
				memberIds, LessonStatusCode.READY, LessonPublicationStatusCode.PUBLISHED, LessonPublicationStatusCode.PRIVATE
		);
	}
}
