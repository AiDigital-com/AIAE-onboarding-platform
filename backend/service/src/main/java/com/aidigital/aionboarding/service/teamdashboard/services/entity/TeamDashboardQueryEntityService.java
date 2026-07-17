package com.aidigital.aionboarding.service.teamdashboard.services.entity;

import com.aidigital.aionboarding.domain.teamdashboard.repositories.IndividualRoadmapLessonProjection;
import com.aidigital.aionboarding.domain.teamdashboard.repositories.LowConfidenceLessonProjection;
import com.aidigital.aionboarding.domain.teamdashboard.repositories.MemberStatsProjection;
import com.aidigital.aionboarding.domain.teamdashboard.repositories.RecentActivityProjection;
import com.aidigital.aionboarding.domain.teamdashboard.repositories.RoadmapStatsProjection;
import com.aidigital.aionboarding.domain.teamdashboard.repositories.StandaloneLessonProjection;
import com.aidigital.aionboarding.domain.teamdashboard.repositories.TeamDashboardRepository;
import com.aidigital.aionboarding.domain.teamdashboard.repositories.WeeklyActivityProjection;
import com.aidigital.aionboarding.service.teamdashboard.util.TeamDashboardSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Read-only cross-entity aggregation query service for team dashboard native-SQL statistics.
 * <p>
 * This is the only service that may inject {@link TeamDashboardRepository} directly.
 * {@link TeamDashboardRepository} has no owned persisted entity and no CRUD; every method here
 * is a read-only delegation to one of its native-SQL aggregation queries, and this service
 * exposes no mutation methods.
 */
@Service
@RequiredArgsConstructor
public class TeamDashboardQueryEntityService {

	private final TeamDashboardRepository teamDashboardRepository;

	/**
	 * Loads per-member roadmap/lesson/quiz statistics for the given members.
	 *
	 * @param memberIds internal user ids to aggregate statistics for
	 * @return one {@link MemberStatsProjection} row per member
	 */
	@Transactional(readOnly = true)
	public List<MemberStatsProjection> findMemberStats(List<Long> memberIds) {
		return teamDashboardRepository.findMemberStats(memberIds);
	}

	/**
	 * Loads roadmap-level enrollment and progress statistics across the given members.
	 *
	 * @param memberIds internal user ids to aggregate statistics for
	 * @return up to the top 6 {@link RoadmapStatsProjection} rows ranked by learner count
	 */
	@Transactional(readOnly = true)
	public List<RoadmapStatsProjection> findRoadmapStats(List<Long> memberIds) {
		return teamDashboardRepository.findRoadmapStats(memberIds);
	}

	/**
	 * Loads weekly lesson/quiz completion counts for the given members over the trailing 8 weeks.
	 *
	 * @param memberIds internal user ids to aggregate statistics for
	 * @return one {@link WeeklyActivityProjection} row per member per week
	 */
	@Transactional(readOnly = true)
	public List<WeeklyActivityProjection> findWeeklyActivity(List<Long> memberIds) {
		return teamDashboardRepository.findWeeklyActivity(memberIds);
	}

	/**
	 * Loads quiz lessons with a below-threshold average score across the given members.
	 * <p>
	 * The {@code attempts}/{@code learners}/{@code avgScore} (and their {@code *ExcludingLead}
	 * counterparts) are aggregated over the full attempt history; only the embedded
	 * {@code attemptItems} sample used for drill-down display is bounded, per lesson, by
	 * {@link TeamDashboardSupport#LOW_CONFIDENCE_ATTEMPT_SAMPLE_SIZE}.
	 *
	 * @param memberIds internal user ids to aggregate statistics for
	 * @param leadId    internal user id of the viewing team lead, or {@code null} when the scope has
	 *                  no lead (for example an admin's organization-wide view); used to compute the
	 *                  lead-excluded KPI variants without a second query
	 * @return up to the top 4 {@link LowConfidenceLessonProjection} rows ranked by lowest average score
	 */
	@Transactional(readOnly = true)
	public List<LowConfidenceLessonProjection> findLowConfidenceLessons(List<Long> memberIds, Long leadId) {
		return teamDashboardRepository.findLowConfidenceLessons(
				memberIds, leadId, TeamDashboardSupport.LOW_CONFIDENCE_ATTEMPT_SAMPLE_SIZE
		);
	}

	/**
	 * Loads the most recent lesson-completion and quiz-attempt activity across the given members.
	 *
	 * @param memberIds internal user ids to aggregate statistics for
	 * @return up to the 12 most recent {@link RecentActivityProjection} rows
	 */
	@Transactional(readOnly = true)
	public List<RecentActivityProjection> findRecentActivity(List<Long> memberIds) {
		return teamDashboardRepository.findRecentActivity(memberIds);
	}

	/**
	 * Loads roadmap-enrolled lesson progress across the given members, grouped by roadmap.
	 *
	 * @param memberIds internal user ids to load roadmap lesson progress for
	 * @return one {@link IndividualRoadmapLessonProjection} row per member per roadmap lesson
	 */
	@Transactional(readOnly = true)
	public List<IndividualRoadmapLessonProjection> findIndividualRoadmapLessons(List<Long> memberIds) {
		return teamDashboardRepository.findIndividualRoadmapLessons(memberIds);
	}

	/**
	 * Loads lesson progress across the given members' lessons that are not part of any roadmap.
	 *
	 * @param memberIds internal user ids to load standalone lesson progress for
	 * @return one {@link StandaloneLessonProjection} row per member per standalone lesson
	 */
	@Transactional(readOnly = true)
	public List<StandaloneLessonProjection> findStandaloneLessons(List<Long> memberIds) {
		return teamDashboardRepository.findStandaloneLessons(memberIds);
	}
}
