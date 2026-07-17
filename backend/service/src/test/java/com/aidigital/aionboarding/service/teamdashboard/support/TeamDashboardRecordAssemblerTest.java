package com.aidigital.aionboarding.service.teamdashboard.support;

import com.aidigital.aionboarding.domain.teamdashboard.repositories.LowConfidenceLessonProjection;
import com.aidigital.aionboarding.domain.teamdashboard.repositories.MemberStatsProjection;
import com.aidigital.aionboarding.domain.teamdashboard.repositories.RecentActivityProjection;
import com.aidigital.aionboarding.domain.teamdashboard.repositories.RoadmapStatsProjection;
import com.aidigital.aionboarding.domain.teamdashboard.repositories.WeeklyActivityProjection;
import com.aidigital.aionboarding.service.common.mapping.ScalarValueReader;
import com.aidigital.aionboarding.service.common.time.CurrentTime;
import com.aidigital.aionboarding.service.teamdashboard.models.TeamDashboardIndividualLessonRecord;
import com.aidigital.aionboarding.service.teamdashboard.models.TeamDashboardIndividualRoadmapRecord;
import com.aidigital.aionboarding.service.teamdashboard.models.TeamDashboardKpisRecord;
import com.aidigital.aionboarding.service.teamdashboard.models.TeamDashboardLowConfidenceAttemptItemRecord;
import com.aidigital.aionboarding.service.teamdashboard.models.TeamDashboardLowConfidenceLessonRecord;
import com.aidigital.aionboarding.service.teamdashboard.models.TeamDashboardMemberRecord;
import com.aidigital.aionboarding.service.teamdashboard.models.TeamDashboardMemberRoadmapRecord;
import com.aidigital.aionboarding.service.teamdashboard.models.TeamDashboardPeriod;
import com.aidigital.aionboarding.service.teamdashboard.models.TeamDashboardPeriodCountsRecord;
import com.aidigital.aionboarding.service.teamdashboard.models.TeamDashboardQuizByPeriodRecord;
import com.aidigital.aionboarding.service.teamdashboard.models.TeamDashboardRecentActivityRecord;
import com.aidigital.aionboarding.service.teamdashboard.models.TeamDashboardRoadmapStatRecord;
import com.aidigital.aionboarding.service.teamdashboard.models.TeamDashboardWeeklyActivityRecord;
import com.aidigital.aionboarding.service.teamdashboard.util.TeamDashboardSupport;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.instancio.Instancio;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.instancio.Select.field;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TeamDashboardRecordAssemblerTest {

	private final TeamDashboardRecordAssembler assembler = new TeamDashboardRecordAssembler(
			new TeamDashboardSupport(new CurrentTime()), new ObjectMapper(), new ScalarValueReader());

	@Nested
	class ToMemberRecord {

		@Test
		void shouldAssembleInProgressMemberWithMultipleRoadmapsAndTeamLeadFlagTest() {
			// Given: a lead member with two roadmaps, half progress, and a distinct value per
			// period so the WEEK/MONTH/QUARTER switches can be proven to pick the right field
			MemberStatsProjection row = mock(MemberStatsProjection.class);
			when(row.getId()).thenReturn(7L);
			when(row.getName()).thenReturn("Jane Doe");
			when(row.getEmail()).thenReturn("jane@x.com");
			when(row.getRole()).thenReturn("Member");
			when(row.getPosition()).thenReturn("Engineer");
			when(row.getAvatarStorageKey()).thenReturn("avatar-key");
			when(row.getAvatarColor()).thenReturn("#123456");
			when(row.getRoadmapCount()).thenReturn(2);
			when(row.getRoadmapIds()).thenReturn(List.of("r1", "r2"));
			when(row.getRoadmapTitles()).thenReturn(List.of("Title1", "Title2"));
			when(row.getRoadmapLessonCount()).thenReturn(10);
			when(row.getRoadmapCompletedCount()).thenReturn(5);
			when(row.getCompletedWeek()).thenReturn(1);
			when(row.getCompletedMonth()).thenReturn(3);
			when(row.getCompletedQuarter()).thenReturn(5);
			when(row.getOpenAssignments()).thenReturn(2);
			when(row.getAvgQuizScoreWeek()).thenReturn(70);
			when(row.getAvgQuizScoreMonth()).thenReturn(85);
			when(row.getAvgQuizScoreQuarter()).thenReturn(95);
			LocalDateTime lastActiveAt = LocalDateTime.now(ZoneOffset.UTC).minusDays(10);
			when(row.getLastActiveAt()).thenReturn(lastActiveAt);

			// When:
			TeamDashboardMemberRecord record = assembler.toMemberRecord(row, 0, TeamDashboardPeriod.MONTH, 7L);

			// Then:
			assertThat(record.id()).isEqualTo("7");
			assertThat(record.role()).isEqualTo("Engineer");
			assertThat(record.isTeamLead()).isTrue();
			assertThat(record.roadmap()).isEqualTo("2 active roadmaps");
			assertThat(record.roadmapId()).isEqualTo("r1");
			assertThat(record.roadmapCount()).isEqualTo(2);
			assertThat(record.roadmaps()).containsExactly(
					new TeamDashboardMemberRoadmapRecord("r1", "Title1"),
					new TeamDashboardMemberRoadmapRecord("r2", "Title2"));
			assertThat(record.roadmapLessonCount()).isEqualTo(10);
			assertThat(record.roadmapCompletedCount()).isEqualTo(5);
			assertThat(record.progress()).isEqualTo(50);
			assertThat(record.completedInPeriod()).isEqualTo(3);
			assertThat(record.completedByPeriod()).isEqualTo(new TeamDashboardPeriodCountsRecord(1, 3, 5));
			assertThat(record.openAssignments()).isEqualTo(2);
			assertThat(record.quiz()).isEqualTo(85);
			assertThat(record.quizByPeriod()).isEqualTo(new TeamDashboardQuizByPeriodRecord(70, 85, 95));
			assertThat(record.status()).isEqualTo("in-progress");
			assertThat(record.lastActiveAt()).isEqualTo(lastActiveAt);
			assertThat(record.lastActive()).matches("\\d+ d ago");
			assertThat(record.avatarBg()).isEqualTo("#123456");
			assertThat(record.avatarStorageKey()).isEqualTo("avatar-key");
			assertThat(record.avatarColor()).isEqualTo("#123456");
		}

		@Test
		void shouldMarkStatusDoneAndFallBackToRoleAndTeamColorWhenProgressReachesOneHundredTest() {
			// Given: completed equals total (100% progress), no position/avatar override, no
			// lead assigned, and a single roadmap with no title so it falls back to "Untitled
			// roadmap"
			MemberStatsProjection row = mock(MemberStatsProjection.class);
			when(row.getId()).thenReturn(42L);
			when(row.getName()).thenReturn("Bob");
			when(row.getEmail()).thenReturn("bob@x.com");
			when(row.getRole()).thenReturn("TeamMember");
			when(row.getPosition()).thenReturn(null);
			when(row.getAvatarStorageKey()).thenReturn(null);
			when(row.getAvatarColor()).thenReturn(null);
			when(row.getRoadmapCount()).thenReturn(1);
			when(row.getRoadmapIds()).thenReturn(List.of("r5"));
			when(row.getRoadmapTitles()).thenReturn(List.of());
			when(row.getRoadmapLessonCount()).thenReturn(4);
			when(row.getRoadmapCompletedCount()).thenReturn(4);
			when(row.getCompletedWeek()).thenReturn(9);
			when(row.getCompletedMonth()).thenReturn(8);
			when(row.getCompletedQuarter()).thenReturn(7);
			when(row.getOpenAssignments()).thenReturn(null);
			when(row.getAvgQuizScoreWeek()).thenReturn(11);
			when(row.getAvgQuizScoreMonth()).thenReturn(22);
			when(row.getAvgQuizScoreQuarter()).thenReturn(null);
			when(row.getLastActiveAt()).thenReturn(null);

			// When: leadId is null and period is QUARTER, proving the quarter-specific fields
			// are read (7 and null, not the week/month values)
			TeamDashboardMemberRecord record = assembler.toMemberRecord(row, 1, TeamDashboardPeriod.QUARTER, null);

			// Then:
			assertThat(record.role()).isEqualTo("TeamMember");
			assertThat(record.isTeamLead()).isFalse();
			assertThat(record.roadmap()).isEqualTo("Untitled roadmap");
			assertThat(record.roadmaps()).containsExactly(new TeamDashboardMemberRoadmapRecord("r5", "Untitled " +
					"roadmap"));
			assertThat(record.progress()).isEqualTo(100);
			assertThat(record.status()).isEqualTo("done");
			assertThat(record.completedInPeriod()).isEqualTo(7);
			assertThat(record.openAssignments()).isEqualTo(0);
			assertThat(record.quiz()).isNull();
			assertThat(record.quizByPeriod()).isEqualTo(new TeamDashboardQuizByPeriodRecord(11, 22, null));
			assertThat(record.lastActive()).isEqualTo("no activity");
			assertThat(record.avatarBg()).isEqualTo(TeamDashboardSupport.TEAM_COLORS.get(1));
			assertThat(record.avatarStorageKey()).isEqualTo("");
			assertThat(record.avatarColor()).isEqualTo("");
		}

		@Test
		void shouldReturnNotStartedStatusAndZeroProgressWhenNoLessonsAssignedAndNoActivityTest() {
			// Given: no roadmap lessons at all (avoids the division-by-zero branch), a blank
			// position/avatar color, no roadmaps, and a lead id that does not match the row
			MemberStatsProjection row = mock(MemberStatsProjection.class);
			when(row.getId()).thenReturn(3L);
			when(row.getName()).thenReturn("Ann");
			when(row.getEmail()).thenReturn("ann@x.com");
			when(row.getRole()).thenReturn("Lead");
			when(row.getPosition()).thenReturn("");
			when(row.getAvatarStorageKey()).thenReturn("");
			when(row.getAvatarColor()).thenReturn("");
			when(row.getRoadmapCount()).thenReturn(0);
			when(row.getRoadmapIds()).thenReturn(List.of());
			when(row.getRoadmapTitles()).thenReturn(List.of());
			when(row.getRoadmapLessonCount()).thenReturn(null);
			when(row.getRoadmapCompletedCount()).thenReturn(null);
			when(row.getCompletedWeek()).thenReturn(2);
			when(row.getCompletedMonth()).thenReturn(4);
			when(row.getCompletedQuarter()).thenReturn(6);
			when(row.getOpenAssignments()).thenReturn(1);
			when(row.getAvgQuizScoreWeek()).thenReturn(30);
			when(row.getAvgQuizScoreMonth()).thenReturn(40);
			when(row.getAvgQuizScoreQuarter()).thenReturn(50);
			when(row.getLastActiveAt()).thenReturn(null);

			// When: period is WEEK, proving the week-specific fields (2 and 30) are read
			TeamDashboardMemberRecord record = assembler.toMemberRecord(row, 4, TeamDashboardPeriod.WEEK, 999L);

			// Then:
			assertThat(record.role()).isEqualTo("Lead");
			assertThat(record.isTeamLead()).isFalse();
			assertThat(record.roadmap()).isEqualTo("No roadmap assigned");
			assertThat(record.roadmapId()).isEqualTo("");
			assertThat(record.roadmaps()).isEmpty();
			assertThat(record.roadmapLessonCount()).isEqualTo(0);
			assertThat(record.roadmapCompletedCount()).isEqualTo(0);
			assertThat(record.progress()).isEqualTo(0);
			assertThat(record.completedInPeriod()).isEqualTo(2);
			assertThat(record.quiz()).isEqualTo(30);
			assertThat(record.status()).isEqualTo("not-started");
			assertThat(record.lastActive()).isEqualTo("no activity");
			assertThat(record.avatarBg()).isEqualTo(TeamDashboardSupport.TEAM_COLORS.get(4));
			assertThat(record.avatarStorageKey()).isEqualTo("");
			assertThat(record.avatarColor()).isEqualTo("");
		}

		@Test
		void shouldMarkInProgressWhenActivityRecordedButNoLessonsCompletedYetTest() {
			// Given: completed count is 0 but lastActiveAt is set, so hasStarted becomes true
			// through the timestamp branch rather than the completed-count branch; also a single
			// roadmap with a non-empty title
			MemberStatsProjection row = mock(MemberStatsProjection.class);
			when(row.getId()).thenReturn(15L);
			when(row.getName()).thenReturn("Sam");
			when(row.getEmail()).thenReturn("sam@x.com");
			when(row.getRole()).thenReturn("Contributor");
			when(row.getPosition()).thenReturn("Designer");
			when(row.getAvatarStorageKey()).thenReturn("key99");
			when(row.getAvatarColor()).thenReturn("#abcabc");
			when(row.getRoadmapCount()).thenReturn(1);
			when(row.getRoadmapIds()).thenReturn(List.of("r9"));
			when(row.getRoadmapTitles()).thenReturn(List.of("Roadmap Nine"));
			when(row.getRoadmapLessonCount()).thenReturn(5);
			when(row.getRoadmapCompletedCount()).thenReturn(0);
			when(row.getCompletedWeek()).thenReturn(4);
			when(row.getCompletedMonth()).thenReturn(6);
			when(row.getCompletedQuarter()).thenReturn(8);
			when(row.getOpenAssignments()).thenReturn(0);
			when(row.getAvgQuizScoreWeek()).thenReturn(null);
			when(row.getAvgQuizScoreMonth()).thenReturn(null);
			when(row.getAvgQuizScoreQuarter()).thenReturn(null);
			LocalDateTime recentActivity = LocalDateTime.now(ZoneOffset.UTC).minusHours(1);
			when(row.getLastActiveAt()).thenReturn(recentActivity);

			// When:
			TeamDashboardMemberRecord record = assembler.toMemberRecord(row, 2, TeamDashboardPeriod.MONTH, null);

			// Then:
			assertThat(record.roadmapCompletedCount()).isEqualTo(0);
			assertThat(record.progress()).isEqualTo(0);
			assertThat(record.status()).isEqualTo("in-progress");
			assertThat(record.roadmap()).isEqualTo("Roadmap Nine");
			assertThat(record.completedInPeriod()).isEqualTo(6);
			assertThat(record.quiz()).isNull();
			assertThat(record.lastActive()).matches("\\d+ (min|h) ago");
		}

		@Test
		void shouldBuildRoadmapsListWithUntitledFallbackWhenTitlesListIsShorterThanIdsListTest() {
			// Given: three roadmap ids but only one title, and a blank (whitespace-only)
			// position/avatar color so isBlank() handling is proven, plus an index beyond the
			// team color palette size to prove the modulo wrap-around
			MemberStatsProjection row = mock(MemberStatsProjection.class);
			when(row.getId()).thenReturn(88L);
			when(row.getName()).thenReturn("Kim");
			when(row.getEmail()).thenReturn("kim@x.com");
			when(row.getRole()).thenReturn("Analyst");
			when(row.getPosition()).thenReturn(" ");
			when(row.getAvatarStorageKey()).thenReturn(null);
			when(row.getAvatarColor()).thenReturn("   ");
			when(row.getRoadmapCount()).thenReturn(3);
			when(row.getRoadmapIds()).thenReturn(List.of("id1", "id2", "id3"));
			when(row.getRoadmapTitles()).thenReturn(List.of("OnlyOneTitle"));
			when(row.getRoadmapLessonCount()).thenReturn(6);
			when(row.getRoadmapCompletedCount()).thenReturn(3);
			when(row.getCompletedWeek()).thenReturn(1);
			when(row.getCompletedMonth()).thenReturn(1);
			when(row.getCompletedQuarter()).thenReturn(1);
			when(row.getOpenAssignments()).thenReturn(5);
			when(row.getAvgQuizScoreWeek()).thenReturn(60);
			when(row.getAvgQuizScoreMonth()).thenReturn(60);
			when(row.getAvgQuizScoreQuarter()).thenReturn(60);
			when(row.getLastActiveAt()).thenReturn(null);

			// When: index 10 with a palette of 8 colors should wrap around to index 2
			TeamDashboardMemberRecord record = assembler.toMemberRecord(row, 10, TeamDashboardPeriod.WEEK, null);

			// Then:
			assertThat(record.role()).isEqualTo("Analyst");
			assertThat(record.roadmap()).isEqualTo("3 active roadmaps");
			assertThat(record.roadmapId()).isEqualTo("id1");
			assertThat(record.roadmaps()).containsExactly(
					new TeamDashboardMemberRoadmapRecord("id1", "OnlyOneTitle"),
					new TeamDashboardMemberRoadmapRecord("id2", "Untitled roadmap"),
					new TeamDashboardMemberRoadmapRecord("id3", "Untitled roadmap"));
			assertThat(record.avatarBg()).isEqualTo(TeamDashboardSupport.TEAM_COLORS.get(2));
			assertThat(record.avatarColor()).isEqualTo("   ");
			assertThat(record.avatarStorageKey()).isEqualTo("");
		}
	}

	@Nested
	class ToRoadmapStatRecord {

		@Test
		void shouldMapAllFieldsAndResolveTeamColorByIndexTest() {
			// Given:
			RoadmapStatsProjection row = mock(RoadmapStatsProjection.class);
			when(row.getId()).thenReturn(100L);
			when(row.getTitle()).thenReturn("Onboarding Roadmap");
			when(row.getLearners()).thenReturn(12);
			when(row.getLessonCount()).thenReturn(6);
			when(row.getAvgProgress()).thenReturn(45);

			// When:
			TeamDashboardRoadmapStatRecord record = assembler.toRoadmapStatRecord(row, 2);

			// Then:
			assertThat(record.id()).isEqualTo("100");
			assertThat(record.name()).isEqualTo("Onboarding Roadmap");
			assertThat(record.learners()).isEqualTo(12);
			assertThat(record.lessonCount()).isEqualTo(6);
			assertThat(record.progress()).isEqualTo(45);
			assertThat(record.color()).isEqualTo(TeamDashboardSupport.TEAM_COLORS.get(2));
		}

		@Test
		void shouldFallBackToZeroForNullNumericFieldsTest() {
			// Given:
			RoadmapStatsProjection row = mock(RoadmapStatsProjection.class);
			when(row.getId()).thenReturn(1L);
			when(row.getTitle()).thenReturn("Bare Roadmap");
			when(row.getLearners()).thenReturn(null);
			when(row.getLessonCount()).thenReturn(null);
			when(row.getAvgProgress()).thenReturn(null);

			// When:
			TeamDashboardRoadmapStatRecord record = assembler.toRoadmapStatRecord(row, 0);

			// Then:
			assertThat(record.learners()).isEqualTo(0);
			assertThat(record.lessonCount()).isEqualTo(0);
			assertThat(record.progress()).isEqualTo(0);
			assertThat(record.color()).isEqualTo(TeamDashboardSupport.TEAM_COLORS.get(0));
		}

		@Test
		void shouldWrapTeamColorIndexUsingModuloWhenIndexExceedsPaletteSizeTest() {
			// Given: index 9 with an 8-color palette should wrap to index 1
			RoadmapStatsProjection row = mock(RoadmapStatsProjection.class);
			when(row.getId()).thenReturn(2L);
			when(row.getTitle()).thenReturn("Wrapped Roadmap");
			when(row.getLearners()).thenReturn(3);
			when(row.getLessonCount()).thenReturn(3);
			when(row.getAvgProgress()).thenReturn(80);

			// When:
			TeamDashboardRoadmapStatRecord record = assembler.toRoadmapStatRecord(row, 9);

			// Then:
			assertThat(record.color()).isEqualTo(TeamDashboardSupport.TEAM_COLORS.get(1));
		}
	}

	@Nested
	class ToWeeklyActivityRecord {

		@Test
		void shouldFormatWeekLabelAndMapCountsTest() {
			// Given:
			WeeklyActivityProjection row = mock(WeeklyActivityProjection.class);
			when(row.getUserId()).thenReturn(55L);
			when(row.getWeekStart()).thenReturn(LocalDateTime.of(2026, 3, 5, 0, 0));
			when(row.getLessons()).thenReturn(3);
			when(row.getQuizzes()).thenReturn(2);

			// When:
			TeamDashboardWeeklyActivityRecord record = assembler.toWeeklyActivityRecord(row);

			// Then:
			assertThat(record.userId()).isEqualTo("55");
			assertThat(record.label()).isEqualTo("Mar 05");
			assertThat(record.lessons()).isEqualTo(3);
			assertThat(record.quizzes()).isEqualTo(2);
		}

		@Test
		void shouldReturnEmptyLabelAndZeroCountsWhenFieldsAreNullTest() {
			// Given:
			WeeklyActivityProjection row = mock(WeeklyActivityProjection.class);
			when(row.getUserId()).thenReturn(1L);
			when(row.getWeekStart()).thenReturn(null);
			when(row.getLessons()).thenReturn(null);
			when(row.getQuizzes()).thenReturn(null);

			// When:
			TeamDashboardWeeklyActivityRecord record = assembler.toWeeklyActivityRecord(row);

			// Then:
			assertThat(record.label()).isEqualTo("");
			assertThat(record.lessons()).isEqualTo(0);
			assertThat(record.quizzes()).isEqualTo(0);
		}
	}

	@Nested
	class ToLowConfidenceLessonRecord {

		@Test
		void shouldMapAggregateFieldsAndParseAttemptItemsTest() {
			// Given:
			LowConfidenceLessonProjection row = mock(LowConfidenceLessonProjection.class);
			when(row.getId()).thenReturn(200L);
			when(row.getTitle()).thenReturn("Lesson X");
			when(row.getAttempts()).thenReturn(10);
			when(row.getLearners()).thenReturn(5);
			when(row.getAvgScore()).thenReturn(60);
			when(row.getAttemptsExcludingLead()).thenReturn(8);
			when(row.getLearnersExcludingLead()).thenReturn(4);
			when(row.getAvgScoreExcludingLead()).thenReturn(55);
			when(row.getAttemptItems()).thenReturn("""
					[{"id":"att1","userId":"10","userName":"John","score":75}]""");

			// When:
			TeamDashboardLowConfidenceLessonRecord record = assembler.toLowConfidenceLessonRecord(row);

			// Then:
			assertThat(record.id()).isEqualTo("200");
			assertThat(record.lesson()).isEqualTo("Lesson X");
			assertThat(record.attempts()).isEqualTo(10);
			assertThat(record.learners()).isEqualTo(5);
			assertThat(record.avgScore()).isEqualTo(60);
			assertThat(record.attemptsExcludingLead()).isEqualTo(8);
			assertThat(record.learnersExcludingLead()).isEqualTo(4);
			assertThat(record.avgScoreExcludingLead()).isEqualTo(55);
			assertThat(record.attemptItems()).hasSize(1);
			assertThat(record.attemptItems().getFirst().id()).isEqualTo("att1");
			assertThat(record.attemptItems().getFirst().score()).isEqualTo(75);
		}

		@Test
		void shouldReturnEmptyAttemptItemsWhenJsonIsMalformedTest() {
			// Given:
			LowConfidenceLessonProjection row = mock(LowConfidenceLessonProjection.class);
			when(row.getId()).thenReturn(201L);
			when(row.getTitle()).thenReturn("Lesson Y");
			when(row.getAttempts()).thenReturn(0);
			when(row.getLearners()).thenReturn(0);
			when(row.getAvgScore()).thenReturn(0);
			when(row.getAttemptsExcludingLead()).thenReturn(0);
			when(row.getLearnersExcludingLead()).thenReturn(0);
			when(row.getAvgScoreExcludingLead()).thenReturn(0);
			when(row.getAttemptItems()).thenReturn("not-json-at-all");

			// When:
			TeamDashboardLowConfidenceLessonRecord record = assembler.toLowConfidenceLessonRecord(row);

			// Then:
			assertThat(record.attemptItems()).isEmpty();
		}
	}

	@Nested
	class ToRecentActivityRecord {

		@Test
		void shouldFormatQuizActivityWithScoreAndPreferOwnAvatarColorTest() {
			// Given: a quiz activity with its own avatar color, so the fallback map is ignored
			RecentActivityProjection row = mock(RecentActivityProjection.class);
			when(row.getKind()).thenReturn("quiz");
			when(row.getUserId()).thenReturn(10L);
			when(row.getWho()).thenReturn("John");
			when(row.getWhat()).thenReturn("Java Basics Quiz");
			when(row.getScore()).thenReturn(new BigDecimal("85"));
			when(row.getPassed()).thenReturn(true);
			LocalDateTime happenedAt = LocalDateTime.of(2026, 1, 1, 10, 0, 0);
			when(row.getHappenedAt()).thenReturn(happenedAt);
			when(row.getAvatarColor()).thenReturn("#abc");
			when(row.getAvatarStorageKey()).thenReturn("key1");
			long expectedEpoch = happenedAt.toInstant(ZoneOffset.UTC).toEpochMilli();

			// When:
			TeamDashboardRecentActivityRecord record = assembler.toRecentActivityRecord(
					row, 3, Map.of(10L, "#fallback"));

			// Then:
			assertThat(record.id()).isEqualTo("quiz-3-" + expectedEpoch);
			assertThat(record.userId()).isEqualTo("10");
			assertThat(record.who()).isEqualTo("John");
			assertThat(record.action()).isEqualTo("scored");
			assertThat(record.what()).isEqualTo("85% on Java Basics Quiz");
			assertThat(record.kind()).isEqualTo("quiz");
			assertThat(record.score()).isEqualTo(85);
			assertThat(record.passed()).isTrue();
			assertThat(record.avatarBg()).isEqualTo("#abc");
			assertThat(record.avatarStorageKey()).isEqualTo("key1");
			assertThat(record.avatarColor()).isEqualTo("#abc");
		}

		@Test
		void shouldDefaultQuizScoreAndLabelToQuizWhenScoreAndWhatAreAbsentTest() {
			// Given: a quiz activity with no score and a blank "what", so both fall back
			RecentActivityProjection row = mock(RecentActivityProjection.class);
			when(row.getKind()).thenReturn("quiz");
			when(row.getUserId()).thenReturn(11L);
			when(row.getWho()).thenReturn("Ann");
			when(row.getWhat()).thenReturn("");
			when(row.getScore()).thenReturn(null);
			when(row.getPassed()).thenReturn(null);
			when(row.getHappenedAt()).thenReturn(null);
			when(row.getAvatarColor()).thenReturn(null);
			when(row.getAvatarStorageKey()).thenReturn(null);

			// When:
			TeamDashboardRecentActivityRecord record = assembler.toRecentActivityRecord(
					row, 0, Map.of());

			// Then:
			assertThat(record.id()).isEqualTo("quiz-0-0");
			assertThat(record.what()).isEqualTo("0% on quiz");
			assertThat(record.score()).isNull();
			assertThat(record.passed()).isNull();
			assertThat(record.avatarBg()).isNull();
			assertThat(record.avatarStorageKey()).isEqualTo("");
			assertThat(record.avatarColor()).isEqualTo("");
		}

		@Test
		void shouldDefaultQuizLabelToQuizWhenWhatIsNullRatherThanBlankTest() {
			// Given: a quiz activity where "what" is null itself (not just blank), exercising
			// the null-check half of the "what == null || what.isBlank()" condition separately
			// from the isBlank() half already covered above
			RecentActivityProjection row = mock(RecentActivityProjection.class);
			when(row.getKind()).thenReturn("quiz");
			when(row.getUserId()).thenReturn(12L);
			when(row.getWho()).thenReturn("Kai");
			when(row.getWhat()).thenReturn(null);
			when(row.getScore()).thenReturn(new BigDecimal("40"));
			when(row.getPassed()).thenReturn(false);
			when(row.getHappenedAt()).thenReturn(null);
			when(row.getAvatarColor()).thenReturn(null);
			when(row.getAvatarStorageKey()).thenReturn(null);

			// When:
			TeamDashboardRecentActivityRecord record = assembler.toRecentActivityRecord(
					row, 0, Map.of());

			// Then:
			assertThat(record.what()).isEqualTo("40% on quiz");
		}

		@Test
		void shouldFormatNonQuizActivityAsFinishedAndFallBackToProvidedAvatarColorMapTest() {
			// Given: a non-quiz ("lesson") activity with no own avatar color, falling back to
			// the caller-supplied color map keyed by user id
			RecentActivityProjection row = mock(RecentActivityProjection.class);
			when(row.getKind()).thenReturn("lesson");
			when(row.getUserId()).thenReturn(20L);
			when(row.getWho()).thenReturn("Max");
			when(row.getWhat()).thenReturn("Intro Lesson");
			when(row.getScore()).thenReturn(null);
			when(row.getPassed()).thenReturn(null);
			when(row.getHappenedAt()).thenReturn(null);
			when(row.getAvatarColor()).thenReturn("");
			when(row.getAvatarStorageKey()).thenReturn(null);

			// When:
			TeamDashboardRecentActivityRecord record = assembler.toRecentActivityRecord(
					row, 5, Map.of(20L, "#fallback-color"));

			// Then:
			assertThat(record.action()).isEqualTo("finished");
			assertThat(record.what()).isEqualTo("Intro Lesson");
			assertThat(record.id()).isEqualTo("lesson-5-0");
			assertThat(record.avatarBg()).isEqualTo("#fallback-color");
		}
	}

	@Nested
	class ToIndividualLessonRecord {

		@Test
		void shouldMarkCompletedAndKeepScoreWhenCompletedAtIsPresentTest() {
			// When:
			TeamDashboardIndividualLessonRecord record = assembler.toIndividualLessonRecord(
					500L, "Lesson A", LocalDateTime.now(ZoneOffset.UTC).minusDays(1), 88,
					LocalDateTime.now(ZoneOffset.UTC).minusDays(5));

			// Then:
			assertThat(record.id()).isEqualTo("500");
			assertThat(record.title()).isEqualTo("Lesson A");
			assertThat(record.state()).isEqualTo("completed");
			assertThat(record.score()).isEqualTo(88);
			assertThat(record.when()).matches("\\d+ d ago");
		}

		@Test
		void shouldMarkInProgressAndReturnNoActivityWhenCompletedAtAndWhenSourceAreNullTest() {
			// When:
			TeamDashboardIndividualLessonRecord record = assembler.toIndividualLessonRecord(
					501L, "Lesson B", null, null, null);

			// Then:
			assertThat(record.state()).isEqualTo("in-progress");
			assertThat(record.score()).isNull();
			assertThat(record.when()).isEqualTo("no activity");
		}
	}

	@Nested
	class EnrichIndividualRoadmap {

		@Test
		void shouldReturnZeroProgressAndZeroCountsWhenLessonsListIsEmptyTest() {
			// When:
			TeamDashboardIndividualRoadmapRecord record = assembler.enrichIndividualRoadmap(
					"r1", "Roadmap A", LocalDateTime.of(2026, 1, 1, 0, 0), List.of());

			// Then:
			assertThat(record.lessonCount()).isEqualTo(0);
			assertThat(record.completedCount()).isEqualTo(0);
			assertThat(record.progress()).isEqualTo(0);
		}

		@Test
		void shouldRoundProgressBasedOnCompletedLessonsOutOfTotalTest() {
			// Given: 2 of 3 lessons completed (66.67% rounds to 67)
			List<TeamDashboardIndividualLessonRecord> lessons = List.of(
					new TeamDashboardIndividualLessonRecord("1", "L1", "completed", 90, "1 d ago"),
					new TeamDashboardIndividualLessonRecord("2", "L2", "completed", 80, "2 d ago"),
					new TeamDashboardIndividualLessonRecord("3", "L3", "in-progress", null, "no activity"));

			// When:
			TeamDashboardIndividualRoadmapRecord record = assembler.enrichIndividualRoadmap(
					"r2", "Roadmap B", LocalDateTime.of(2026, 2, 1, 0, 0), lessons);

			// Then:
			assertThat(record.lessonCount()).isEqualTo(3);
			assertThat(record.completedCount()).isEqualTo(2);
			assertThat(record.progress()).isEqualTo(67);
		}

		@Test
		void shouldReturnFullProgressWhenAllLessonsAreCompletedTest() {
			// Given:
			List<TeamDashboardIndividualLessonRecord> lessons = List.of(
					new TeamDashboardIndividualLessonRecord("1", "L1", "completed", 100, "1 d ago"));

			// When:
			TeamDashboardIndividualRoadmapRecord record = assembler.enrichIndividualRoadmap(
					"r3", "Roadmap C", null, lessons);

			// Then:
			assertThat(record.progress()).isEqualTo(100);
			assertThat(record.enrolledAt()).isNull();
		}
	}

	@Nested
	class ToKpisRecord {

		@Test
		void shouldReturnZeroKpisAndNullAvgQuizScoreWhenMembersListIsEmptyTest() {
			// When:
			TeamDashboardKpisRecord record = assembler.toKpisRecord(List.of());

			// Then:
			assertThat(record.activeRoadmaps()).isEqualTo(0);
			assertThat(record.lessonsCompleted()).isEqualTo(0);
			assertThat(record.avgQuizScore()).isNull();
		}

		@Test
		void shouldDedupeRoadmapIdsFilterBlankIdsAndAverageOnlyNonNullQuizScoresTest() {
			// Given: member1 and member2 share roadmap "r1" (deduped), member2 also has a
			// blank-id and null-id roadmap (both filtered out), and member2's null quiz score is
			// excluded from the average while member1/member3's scores (80 and 81) average to
			// 80.5, which rounds up to 81
			TeamDashboardMemberRecord member1 = Instancio.of(TeamDashboardMemberRecord.class)
					.set(field(TeamDashboardMemberRecord::quiz), 80)
					.set(field(TeamDashboardMemberRecord::completedInPeriod), 3)
					.set(field(TeamDashboardMemberRecord::roadmaps), List.of(
							new TeamDashboardMemberRoadmapRecord("r1", "Title1"),
							new TeamDashboardMemberRoadmapRecord("r2", "Title2")))
					.create();
			TeamDashboardMemberRecord member2 = Instancio.of(TeamDashboardMemberRecord.class)
					.set(field(TeamDashboardMemberRecord::quiz), null)
					.set(field(TeamDashboardMemberRecord::completedInPeriod), 2)
					.set(field(TeamDashboardMemberRecord::roadmaps), List.of(
							new TeamDashboardMemberRoadmapRecord("r1", "Title1Duplicate"),
							new TeamDashboardMemberRoadmapRecord("", "Blank"),
							new TeamDashboardMemberRoadmapRecord(null, "NullId")))
					.create();
			TeamDashboardMemberRecord member3 = Instancio.of(TeamDashboardMemberRecord.class)
					.set(field(TeamDashboardMemberRecord::quiz), 81)
					.set(field(TeamDashboardMemberRecord::completedInPeriod), 0)
					.set(field(TeamDashboardMemberRecord::roadmaps), List.<TeamDashboardMemberRoadmapRecord>of())
					.create();

			// When:
			TeamDashboardKpisRecord record = assembler.toKpisRecord(List.of(member1, member2, member3));

			// Then:
			assertThat(record.activeRoadmaps()).isEqualTo(2);
			assertThat(record.lessonsCompleted()).isEqualTo(5);
			assertThat(record.avgQuizScore()).isEqualTo(81);
		}
	}

	@Nested
	class ParseAttemptItems {

		@Test
		void shouldReturnEmptyListWhenJsonIsNullTest() {
			// When-Then:
			assertThat(assembler.parseAttemptItems(null)).isEmpty();
		}

		@Test
		void shouldReturnEmptyListWhenJsonIsBlankTest() {
			// When-Then:
			assertThat(assembler.parseAttemptItems("   ")).isEmpty();
		}

		@Test
		void shouldMapEachItemAndApplyDefaultsForMissingFieldsTest() {
			// Given: one fully populated item and one item with only an id, so defaults apply
			// to every other field of the second item
			String json = """
					[
					  {"id":"a1","userId":"1","userName":"Ann","userEmail":"ann@x.com",
					   "avatarStorageKey":"k1","avatarColor":"#111","activityId":"act1",
					   "activityTitle":"Q1","attemptNumber":1,"score":90,"passed":true,
					   "correctCount":9,"totalCount":10,"createdAt":"2026-02-01T08:00:00"},
					  {"id":"a2"}
					]""";

			// When:
			List<TeamDashboardLowConfidenceAttemptItemRecord> items = assembler.parseAttemptItems(json);

			// Then:
			assertThat(items).hasSize(2);
			TeamDashboardLowConfidenceAttemptItemRecord first = items.get(0);
			assertThat(first.id()).isEqualTo("a1");
			assertThat(first.userName()).isEqualTo("Ann");
			assertThat(first.score()).isEqualTo(90);
			assertThat(first.passed()).isTrue();
			assertThat(first.correctCount()).isEqualTo(9);
			assertThat(first.totalCount()).isEqualTo(10);
			assertThat(first.createdAt()).isEqualTo(LocalDateTime.of(2026, 2, 1, 8, 0, 0));

			TeamDashboardLowConfidenceAttemptItemRecord second = items.get(1);
			assertThat(second.id()).isEqualTo("a2");
			assertThat(second.userId()).isEqualTo("");
			assertThat(second.attemptNumber()).isEqualTo(0);
			assertThat(second.score()).isEqualTo(0);
			assertThat(second.passed()).isFalse();
			assertThat(second.createdAt()).isNull();
		}

		@Test
		void shouldReturnEmptyListWhenJsonIsSyntacticallyInvalidTest() {
			// When-Then:
			assertThat(assembler.parseAttemptItems("not-json-at-all")).isEmpty();
		}

		@Test
		void shouldReturnEmptyListWhenJsonIsAnObjectInsteadOfAnArrayTest() {
			// When-Then:
			assertThat(assembler.parseAttemptItems("{}")).isEmpty();
		}
	}

	@Nested
	class ToAttemptItemRecord {

		@Test
		void shouldMapAllFieldsFromAWellFormedItemMapTest() {
			// Given:
			Map<String, Object> item = new LinkedHashMap<>();
			item.put("id", "item-1");
			item.put("userId", "9");
			item.put("userName", "Nina");
			item.put("userEmail", "nina@x.com");
			item.put("avatarStorageKey", "key-9");
			item.put("avatarColor", "#222222");
			item.put("activityId", "act-9");
			item.put("activityTitle", "Quiz 9");
			item.put("attemptNumber", 3);
			item.put("score", 55);
			item.put("passed", false);
			item.put("correctCount", 4);
			item.put("totalCount", 8);
			item.put("createdAt", LocalDateTime.of(2026, 3, 3, 12, 0, 0));

			// When:
			TeamDashboardLowConfidenceAttemptItemRecord record = assembler.toAttemptItemRecord(item);

			// Then:
			assertThat(record.id()).isEqualTo("item-1");
			assertThat(record.userId()).isEqualTo("9");
			assertThat(record.userName()).isEqualTo("Nina");
			assertThat(record.userEmail()).isEqualTo("nina@x.com");
			assertThat(record.avatarStorageKey()).isEqualTo("key-9");
			assertThat(record.avatarColor()).isEqualTo("#222222");
			assertThat(record.activityId()).isEqualTo("act-9");
			assertThat(record.activityTitle()).isEqualTo("Quiz 9");
			assertThat(record.attemptNumber()).isEqualTo(3);
			assertThat(record.score()).isEqualTo(55);
			assertThat(record.passed()).isFalse();
			assertThat(record.correctCount()).isEqualTo(4);
			assertThat(record.totalCount()).isEqualTo(8);
			assertThat(record.createdAt()).isEqualTo(LocalDateTime.of(2026, 3, 3, 12, 0, 0));
		}

		@Test
		void shouldApplyDefaultsWhenItemMapIsEmptyTest() {
			// When:
			TeamDashboardLowConfidenceAttemptItemRecord record = assembler.toAttemptItemRecord(Map.of());

			// Then:
			assertThat(record.id()).isEqualTo("");
			assertThat(record.userId()).isEqualTo("");
			assertThat(record.attemptNumber()).isEqualTo(0);
			assertThat(record.score()).isEqualTo(0);
			assertThat(record.passed()).isFalse();
			assertThat(record.correctCount()).isEqualTo(0);
			assertThat(record.totalCount()).isEqualTo(0);
			assertThat(record.createdAt()).isNull();
		}
	}
}
