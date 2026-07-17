package com.aidigital.aionboarding.service.teamdashboard.support;

import com.aidigital.aionboarding.domain.teamdashboard.repositories.MemberStatsProjection;
import com.aidigital.aionboarding.domain.teamdashboard.repositories.RecentActivityProjection;
import com.aidigital.aionboarding.domain.teamdashboard.repositories.RoadmapStatsProjection;
import com.aidigital.aionboarding.domain.teamdashboard.repositories.WeeklyActivityProjection;
import com.aidigital.aionboarding.service.common.mapping.ScalarValueReader;
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
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class TeamDashboardRecordAssembler {

	private final TeamDashboardSupport teamDashboardSupport;
	private final ObjectMapper objectMapper;
	private final ScalarValueReader scalarValueReader;

	public TeamDashboardMemberRecord toMemberRecord(
			MemberStatsProjection row,
			int index,
			TeamDashboardPeriod period,
			Long leadId
	) {
		List<String> roadmapTitles = teamDashboardSupport.toStringList(row.getRoadmapTitles());
		List<String> roadmapIds = teamDashboardSupport.toStringList(row.getRoadmapIds());
		int roadmapCount = teamDashboardSupport.toNumber(row.getRoadmapCount(), 0);
		int total = teamDashboardSupport.toNumber(row.getRoadmapLessonCount(), 0);
		int completed = teamDashboardSupport.toNumber(row.getRoadmapCompletedCount(), 0);
		int progress = total > 0 ? Math.round((completed * 100f) / total) : 0;
		boolean hasStarted = completed > 0 || row.getLastActiveAt() != null;
		String status = progress >= 100 ? "done" : hasStarted ? "in-progress" : "not-started";

		TeamDashboardPeriodCountsRecord completedByPeriod = new TeamDashboardPeriodCountsRecord(
				teamDashboardSupport.toNumber(row.getCompletedWeek(), 0),
				teamDashboardSupport.toNumber(row.getCompletedMonth(), 0),
				teamDashboardSupport.toNumber(row.getCompletedQuarter(), 0)
		);

		TeamDashboardQuizByPeriodRecord quizByPeriod = new TeamDashboardQuizByPeriodRecord(
				teamDashboardSupport.toNullableNumber(row.getAvgQuizScoreWeek()),
				teamDashboardSupport.toNullableNumber(row.getAvgQuizScoreMonth()),
				teamDashboardSupport.toNullableNumber(row.getAvgQuizScoreQuarter())
		);

		List<TeamDashboardMemberRoadmapRecord> roadmaps = new ArrayList<>();
		for (int roadmapIndex = 0; roadmapIndex < roadmapIds.size(); roadmapIndex++) {
			roadmaps.add(new TeamDashboardMemberRoadmapRecord(
					roadmapIds.get(roadmapIndex),
					roadmapIndex < roadmapTitles.size() ? roadmapTitles.get(roadmapIndex) : "Untitled roadmap"
			));
		}

		String roadmapLabel;
		if (roadmapCount == 0) {
			roadmapLabel = "No roadmap assigned";
		} else if (roadmapCount == 1) {
			roadmapLabel = roadmapTitles.isEmpty() ? "Untitled roadmap" : roadmapTitles.getFirst();
		} else {
			roadmapLabel = roadmapCount + " active roadmaps";
		}

		int completedInPeriod = switch (period) {
			case WEEK -> completedByPeriod.week();
			case MONTH -> completedByPeriod.month();
			case QUARTER -> completedByPeriod.quarter();
		};

		Integer quizForPeriod = switch (period) {
			case WEEK -> quizByPeriod.week();
			case MONTH -> quizByPeriod.month();
			case QUARTER -> quizByPeriod.quarter();
		};

		return new TeamDashboardMemberRecord(
				String.valueOf(row.getId()),
				row.getName(),
				row.getEmail(),
				row.getPosition() != null && !row.getPosition().isBlank() ? row.getPosition() : row.getRole(),
				leadId != null && leadId.equals(row.getId()),
				roadmapLabel,
				roadmapIds.isEmpty() ? "" : roadmapIds.getFirst(),
				roadmapCount,
				roadmaps,
				total,
				completed,
				progress,
				completedInPeriod,
				completedByPeriod,
				teamDashboardSupport.toNumber(row.getOpenAssignments(), 0),
				quizForPeriod,
				quizByPeriod,
				status,
				row.getLastActiveAt(),
				teamDashboardSupport.formatRelativeTime(row.getLastActiveAt()),
				row.getAvatarColor() != null && !row.getAvatarColor().isBlank()
						? row.getAvatarColor()
						: TeamDashboardSupport.TEAM_COLORS.get(index % TeamDashboardSupport.TEAM_COLORS.size()),
				row.getAvatarStorageKey() == null ? "" : row.getAvatarStorageKey(),
				row.getAvatarColor() == null ? "" : row.getAvatarColor()
		);
	}

	public TeamDashboardRoadmapStatRecord toRoadmapStatRecord(RoadmapStatsProjection row, int index) {
		return new TeamDashboardRoadmapStatRecord(
				String.valueOf(row.getId()),
				row.getTitle(),
				teamDashboardSupport.toNumber(row.getLearners(), 0),
				teamDashboardSupport.toNumber(row.getLessonCount(), 0),
				teamDashboardSupport.toNumber(row.getAvgProgress(), 0),
				TeamDashboardSupport.TEAM_COLORS.get(index % TeamDashboardSupport.TEAM_COLORS.size())
		);
	}

	public TeamDashboardWeeklyActivityRecord toWeeklyActivityRecord(WeeklyActivityProjection row) {
		return new TeamDashboardWeeklyActivityRecord(
				String.valueOf(row.getUserId()),
				teamDashboardSupport.formatWeekLabel(row.getWeekStart()),
				teamDashboardSupport.toNumber(row.getLessons(), 0),
				teamDashboardSupport.toNumber(row.getQuizzes(), 0)
		);
	}

	public TeamDashboardLowConfidenceLessonRecord toLowConfidenceLessonRecord(
			com.aidigital.aionboarding.domain.teamdashboard.repositories.LowConfidenceLessonProjection row
	) {
		return new TeamDashboardLowConfidenceLessonRecord(
				String.valueOf(row.getId()),
				row.getTitle(),
				teamDashboardSupport.toNumber(row.getAttempts(), 0),
				teamDashboardSupport.toNumber(row.getLearners(), 0),
				teamDashboardSupport.toNumber(row.getAvgScore(), 0),
				teamDashboardSupport.toNumber(row.getAttemptsExcludingLead(), 0),
				teamDashboardSupport.toNumber(row.getLearnersExcludingLead(), 0),
				teamDashboardSupport.toNumber(row.getAvgScoreExcludingLead(), 0),
				parseAttemptItems(row.getAttemptItems())
		);
	}

	public TeamDashboardRecentActivityRecord toRecentActivityRecord(
			RecentActivityProjection row,
			int index,
			Map<Long, String> fallbackColorByUserId
	) {
		String what = row.getWhat();
		String action;
		String displayWhat;
		if ("quiz".equals(row.getKind())) {
			action = "scored";
			int score = row.getScore() == null ? 0 : row.getScore().intValue();
			displayWhat = score + "% on " + (what == null || what.isBlank() ? "quiz" : what);
		} else {
			action = "finished";
			displayWhat = what;
		}

		long happenedEpoch = row.getHappenedAt() == null
				? 0L
				: row.getHappenedAt().toInstant(ZoneOffset.UTC).toEpochMilli();

		return new TeamDashboardRecentActivityRecord(
				row.getKind() + "-" + index + "-" + happenedEpoch,
				String.valueOf(row.getUserId()),
				row.getWho(),
				action,
				displayWhat,
				teamDashboardSupport.formatRelativeTime(row.getHappenedAt()),
				row.getKind(),
				row.getScore() == null ? null : row.getScore().intValue(),
				row.getPassed(),
				row.getAvatarColor() != null && !row.getAvatarColor().isBlank()
						? row.getAvatarColor()
						: fallbackColorByUserId.get(row.getUserId()),
				row.getAvatarStorageKey() == null ? "" : row.getAvatarStorageKey(),
				row.getAvatarColor() == null ? "" : row.getAvatarColor()
		);
	}

	public TeamDashboardIndividualLessonRecord toIndividualLessonRecord(
			Long lessonId,
			String title,
			LocalDateTime completedAt,
			Integer avgScore,
			LocalDateTime whenSource
	) {
		return new TeamDashboardIndividualLessonRecord(
				String.valueOf(lessonId),
				title,
				completedAt != null ? "completed" : "in-progress",
				teamDashboardSupport.toNullableNumber(avgScore),
				teamDashboardSupport.formatRelativeTime(whenSource)
		);
	}

	public TeamDashboardIndividualRoadmapRecord enrichIndividualRoadmap(
			String id,
			String title,
			LocalDateTime enrolledAt,
			List<TeamDashboardIndividualLessonRecord> lessons
	) {
		int lessonCount = lessons.size();
		int completedCount = (int) lessons.stream()
				.filter(lesson -> "completed".equals(lesson.state()))
				.count();
		int progress = lessonCount > 0 ? Math.round((completedCount * 100f) / lessonCount) : 0;
		return new TeamDashboardIndividualRoadmapRecord(
				id,
				title,
				enrolledAt,
				lessons,
				lessonCount,
				completedCount,
				progress
		);
	}

	public TeamDashboardKpisRecord toKpisRecord(List<TeamDashboardMemberRecord> members) {
		List<Integer> quizScores = members.stream()
				.map(TeamDashboardMemberRecord::quiz)
				.filter(java.util.Objects::nonNull)
				.toList();
		Integer avgQuizScore = quizScores.isEmpty()
				? null
				: (int) Math.round(quizScores.stream().mapToInt(Integer::intValue).average().orElse(0));

		java.util.Set<String> activeRoadmaps = new java.util.HashSet<>();
		for (TeamDashboardMemberRecord member : members) {
			for (TeamDashboardMemberRoadmapRecord roadmap : member.roadmaps()) {
				if (roadmap.id() != null && !roadmap.id().isBlank()) {
					activeRoadmaps.add(roadmap.id());
				}
			}
		}

		int lessonsCompleted = members.stream()
				.mapToInt(TeamDashboardMemberRecord::completedInPeriod)
				.sum();

		return new TeamDashboardKpisRecord(activeRoadmaps.size(), lessonsCompleted, avgQuizScore);
	}

	public List<TeamDashboardLowConfidenceAttemptItemRecord> parseAttemptItems(String json) {
		if (json == null || json.isBlank()) {
			return List.of();
		}
		try {
			List<Map<String, Object>> items = objectMapper.readValue(json, new TypeReference<>() {
			});
			return items.stream().map(this::toAttemptItemRecord).toList();
		} catch (Exception ignored) {
			return List.of();
		}
	}

	TeamDashboardLowConfidenceAttemptItemRecord toAttemptItemRecord(Map<String, Object> item) {
		return new TeamDashboardLowConfidenceAttemptItemRecord(
				scalarValueReader.stringVal(item.get("id")),
				scalarValueReader.stringVal(item.get("userId")),
				scalarValueReader.stringVal(item.get("userName")),
				scalarValueReader.stringVal(item.get("userEmail")),
				scalarValueReader.stringVal(item.get("avatarStorageKey")),
				scalarValueReader.stringVal(item.get("avatarColor")),
				scalarValueReader.stringVal(item.get("activityId")),
				scalarValueReader.stringVal(item.get("activityTitle")),
				scalarValueReader.intVal(item.get("attemptNumber"), 0),
				scalarValueReader.intVal(item.get("score"), 0),
				scalarValueReader.boolVal(item.get("passed"), false),
				scalarValueReader.intVal(item.get("correctCount"), 0),
				scalarValueReader.intVal(item.get("totalCount"), 0),
				scalarValueReader.dateTimeVal(item.get("createdAt"))
		);
	}
}
