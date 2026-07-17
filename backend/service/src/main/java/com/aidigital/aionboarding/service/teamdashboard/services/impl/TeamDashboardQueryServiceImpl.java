package com.aidigital.aionboarding.service.teamdashboard.services.impl;

import com.aidigital.aionboarding.domain.teamdashboard.repositories.IndividualRoadmapLessonProjection;
import com.aidigital.aionboarding.domain.teamdashboard.repositories.StandaloneLessonProjection;
import com.aidigital.aionboarding.service.teamdashboard.models.TeamDashboardIndividualLessonRecord;
import com.aidigital.aionboarding.service.teamdashboard.models.TeamDashboardIndividualRoadmapRecord;
import com.aidigital.aionboarding.service.teamdashboard.services.TeamDashboardQueryService;
import com.aidigital.aionboarding.service.teamdashboard.services.entity.TeamDashboardQueryEntityService;
import com.aidigital.aionboarding.service.teamdashboard.support.TeamDashboardRecordAssembler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
public class TeamDashboardQueryServiceImpl implements TeamDashboardQueryService {

    private final TeamDashboardQueryEntityService teamDashboardQueryEntityService;
    private final TeamDashboardRecordAssembler teamDashboardMapper;

    @Override
    @Transactional(readOnly = true)
    public Map<String, List<TeamDashboardIndividualRoadmapRecord>> getIndividualRoadmapDetailsByMemberIds(List<Long> memberIds) {
        if (memberIds == null || memberIds.isEmpty()) {
            return Map.of();
        }

        Map<Long, List<IndividualRoadmapLessonProjection>> roadmapRowsByMemberId = groupByMemberId(
            teamDashboardQueryEntityService.findIndividualRoadmapLessons(memberIds),
            IndividualRoadmapLessonProjection::getMemberId
        );
        Map<Long, List<StandaloneLessonProjection>> standaloneRowsByMemberId = groupByMemberId(
            teamDashboardQueryEntityService.findStandaloneLessons(memberIds),
            StandaloneLessonProjection::getMemberId
        );

        Map<String, List<TeamDashboardIndividualRoadmapRecord>> detailsByMemberId = new LinkedHashMap<>();
        for (Long memberId : memberIds) {
            List<TeamDashboardIndividualRoadmapRecord> groups = buildRoadmapGroups(
                roadmapRowsByMemberId.getOrDefault(memberId, List.of())
            );
            appendStandaloneLessons(standaloneRowsByMemberId.getOrDefault(memberId, List.of()), groups);
            detailsByMemberId.put(String.valueOf(memberId), groups);
        }
        return detailsByMemberId;
    }

    <T> Map<Long, List<T>> groupByMemberId(List<T> rows, Function<T, Long> memberIdExtractor) {
        Map<Long, List<T>> rowsByMemberId = new LinkedHashMap<>();
        for (T row : rows) {
            rowsByMemberId.computeIfAbsent(memberIdExtractor.apply(row), ignored -> new ArrayList<>()).add(row);
        }
        return rowsByMemberId;
    }

    List<TeamDashboardIndividualRoadmapRecord> buildRoadmapGroups(List<IndividualRoadmapLessonProjection> rows) {
        Map<Long, List<TeamDashboardIndividualLessonRecord>> lessonsByRoadmapId = new LinkedHashMap<>();
        Map<Long, String> titleByRoadmapId = new LinkedHashMap<>();
        Map<Long, LocalDateTime> enrolledAtByRoadmapId = new LinkedHashMap<>();

        for (IndividualRoadmapLessonProjection row : rows) {
            titleByRoadmapId.putIfAbsent(row.getRoadmapId(), row.getRoadmapTitle());
            enrolledAtByRoadmapId.putIfAbsent(row.getRoadmapId(), row.getRoadmapEnrolledAt());
            lessonsByRoadmapId.computeIfAbsent(row.getRoadmapId(), ignored -> new ArrayList<>()).add(
                teamDashboardMapper.toIndividualLessonRecord(
                    row.getLessonId(),
                    row.getLessonTitle(),
                    row.getCompletedAt(),
                    row.getAvgScore(),
                    resolveLessonActivityAt(row)
                )
            );
        }

        List<TeamDashboardIndividualRoadmapRecord> groups = new ArrayList<>();
        for (Map.Entry<Long, List<TeamDashboardIndividualLessonRecord>> entry : lessonsByRoadmapId.entrySet()) {
            groups.add(teamDashboardMapper.enrichIndividualRoadmap(
                String.valueOf(entry.getKey()),
                titleByRoadmapId.get(entry.getKey()),
                enrolledAtByRoadmapId.get(entry.getKey()),
                entry.getValue()
            ));
        }
        return groups;
    }

    void appendStandaloneLessons(List<StandaloneLessonProjection> standaloneRows, List<TeamDashboardIndividualRoadmapRecord> groups) {
        if (standaloneRows.isEmpty()) {
            return;
        }
        List<TeamDashboardIndividualLessonRecord> lessons = new ArrayList<>();
        for (StandaloneLessonProjection row : standaloneRows) {
            lessons.add(teamDashboardMapper.toIndividualLessonRecord(
                row.getLessonId(),
                row.getLessonTitle(),
                row.getCompletedAt(),
                row.getAvgScore(),
                row.getCompletedAt() != null
                    ? row.getCompletedAt()
                    : row.getLastQuizAt() != null ? row.getLastQuizAt() : row.getEnrolledAt()
            ));
        }
        groups.add(teamDashboardMapper.enrichIndividualRoadmap(
            "standalone-lessons",
            "Individual lessons",
            null,
            lessons
        ));
    }

    LocalDateTime resolveLessonActivityAt(IndividualRoadmapLessonProjection row) {
        if (row.getCompletedAt() != null) {
            return row.getCompletedAt();
        }
        if (row.getLastQuizAt() != null) {
            return row.getLastQuizAt();
        }
        if (row.getEnrolledAt() != null) {
            return row.getEnrolledAt();
        }
        return row.getRoadmapEnrolledAt();
    }
}
