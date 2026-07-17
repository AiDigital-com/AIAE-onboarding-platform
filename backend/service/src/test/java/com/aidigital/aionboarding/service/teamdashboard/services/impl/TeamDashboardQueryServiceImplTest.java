package com.aidigital.aionboarding.service.teamdashboard.services.impl;

import com.aidigital.aionboarding.domain.teamdashboard.repositories.IndividualRoadmapLessonProjection;
import com.aidigital.aionboarding.domain.teamdashboard.repositories.StandaloneLessonProjection;
import com.aidigital.aionboarding.service.teamdashboard.models.TeamDashboardIndividualLessonRecord;
import com.aidigital.aionboarding.service.teamdashboard.models.TeamDashboardIndividualRoadmapRecord;
import com.aidigital.aionboarding.service.teamdashboard.services.entity.TeamDashboardQueryEntityService;
import com.aidigital.aionboarding.service.teamdashboard.support.TeamDashboardRecordAssembler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TeamDashboardQueryServiceImplTest {

	@Mock
	private TeamDashboardQueryEntityService teamDashboardQueryEntityService;
	@Mock
	private TeamDashboardRecordAssembler teamDashboardMapper;

	@InjectMocks
	private TeamDashboardQueryServiceImpl service;

	@Test
	void getIndividualRoadmapDetailsByMemberIdsShouldReturnEmptyMapWhenMemberIdsIsNullTest() {
		// Given:
		List<Long> memberIds = null;

		// When:
		Map<String, List<TeamDashboardIndividualRoadmapRecord>> result =
				service.getIndividualRoadmapDetailsByMemberIds(memberIds);

		// Then:
		assertThat(result).isEmpty();
	}

	@Test
	void getIndividualRoadmapDetailsByMemberIdsShouldReturnEmptyMapWhenMemberIdsIsEmptyTest() {
		// Given:
		List<Long> memberIds = List.of();

		// When:
		Map<String, List<TeamDashboardIndividualRoadmapRecord>> result =
				service.getIndividualRoadmapDetailsByMemberIds(memberIds);

		// Then:
		assertThat(result).isEmpty();
	}

	@Test
	void getIndividualRoadmapDetailsByMemberIdsShouldGroupLessonsByRoadmapAndMemberAndAppendStandaloneLessonsTest() {
		// Given:
		Long memberId = 7L;
		Long otherMemberId = 8L;
		List<Long> memberIds = List.of(memberId, otherMemberId);
		LocalDateTime enrolledAt = LocalDateTime.of(2026, 1, 1, 0, 0);
		LocalDateTime completedAt = LocalDateTime.of(2026, 1, 2, 0, 0);

		IndividualRoadmapLessonProjection roadmapLessonRow = roadmapLessonProjection(
				memberId, 100L, "Roadmap A", enrolledAt, 200L, "Lesson 1", completedAt, enrolledAt, 90, null
		);
		when(teamDashboardQueryEntityService.findIndividualRoadmapLessons(eq(memberIds)))
				.thenReturn(List.of(roadmapLessonRow));

		TeamDashboardIndividualLessonRecord roadmapLessonRecord = new TeamDashboardIndividualLessonRecord(
				"200", "Lesson 1", "completed", 90, "2 days ago"
		);
		when(teamDashboardMapper.toIndividualLessonRecord(200L, "Lesson 1", completedAt, 90, completedAt))
				.thenReturn(roadmapLessonRecord);

		TeamDashboardIndividualRoadmapRecord roadmapGroup = new TeamDashboardIndividualRoadmapRecord(
				"100", "Roadmap A", enrolledAt, List.of(roadmapLessonRecord), 1, 1, 100
		);
		when(teamDashboardMapper.enrichIndividualRoadmap("100", "Roadmap A", enrolledAt, List.of(roadmapLessonRecord)))
				.thenReturn(roadmapGroup);

		StandaloneLessonProjection standaloneRow = standaloneLessonProjection(memberId, 300L, "Standalone Lesson",
				null, enrolledAt, null, null);
		when(teamDashboardQueryEntityService.findStandaloneLessons(eq(memberIds)))
				.thenReturn(List.of(standaloneRow));

		TeamDashboardIndividualLessonRecord standaloneLessonRecord = new TeamDashboardIndividualLessonRecord(
				"300", "Standalone Lesson", "in-progress", null, "just now"
		);
		when(teamDashboardMapper.toIndividualLessonRecord(300L, "Standalone Lesson", null, null, enrolledAt))
				.thenReturn(standaloneLessonRecord);

		TeamDashboardIndividualRoadmapRecord standaloneGroup = new TeamDashboardIndividualRoadmapRecord(
				"standalone-lessons", "Individual lessons", null, List.of(standaloneLessonRecord), 1, 0, 0
		);
		when(teamDashboardMapper.enrichIndividualRoadmap("standalone-lessons", "Individual lessons", null,
				List.of(standaloneLessonRecord)))
				.thenReturn(standaloneGroup);

		// When:
		Map<String, List<TeamDashboardIndividualRoadmapRecord>> result =
				service.getIndividualRoadmapDetailsByMemberIds(memberIds);

		// Then:
		assertThat(result).containsOnlyKeys("7", "8");
		assertThat(result.get("7")).containsExactly(roadmapGroup, standaloneGroup);
		assertThat(result.get("8")).isEmpty();
	}

	private IndividualRoadmapLessonProjection roadmapLessonProjection(
			Long memberId,
			Long roadmapId,
			String roadmapTitle,
			LocalDateTime roadmapEnrolledAt,
			Long lessonId,
			String lessonTitle,
			LocalDateTime completedAt,
			LocalDateTime enrolledAt,
			Integer avgScore,
			LocalDateTime lastQuizAt
	) {
		return new IndividualRoadmapLessonProjection() {
			@Override
			public Long getMemberId() {
				return memberId;
			}

			@Override
			public Long getRoadmapId() {
				return roadmapId;
			}

			@Override
			public String getRoadmapTitle() {
				return roadmapTitle;
			}

			@Override
			public LocalDateTime getRoadmapEnrolledAt() {
				return roadmapEnrolledAt;
			}

			@Override
			public Integer getSortOrder() {
				return 0;
			}

			@Override
			public Long getLessonId() {
				return lessonId;
			}

			@Override
			public String getLessonTitle() {
				return lessonTitle;
			}

			@Override
			public LocalDateTime getCompletedAt() {
				return completedAt;
			}

			@Override
			public LocalDateTime getEnrolledAt() {
				return enrolledAt;
			}

			@Override
			public Integer getAvgScore() {
				return avgScore;
			}

			@Override
			public LocalDateTime getLastQuizAt() {
				return lastQuizAt;
			}
		};
	}

	private StandaloneLessonProjection standaloneLessonProjection(
			Long memberId,
			Long lessonId,
			String lessonTitle,
			LocalDateTime completedAt,
			LocalDateTime enrolledAt,
			Integer avgScore,
			LocalDateTime lastQuizAt
	) {
		return new StandaloneLessonProjection() {
			@Override
			public Long getMemberId() {
				return memberId;
			}

			@Override
			public Long getLessonId() {
				return lessonId;
			}

			@Override
			public String getLessonTitle() {
				return lessonTitle;
			}

			@Override
			public LocalDateTime getCompletedAt() {
				return completedAt;
			}

			@Override
			public LocalDateTime getEnrolledAt() {
				return enrolledAt;
			}

			@Override
			public Integer getAvgScore() {
				return avgScore;
			}

			@Override
			public LocalDateTime getLastQuizAt() {
				return lastQuizAt;
			}
		};
	}
}
