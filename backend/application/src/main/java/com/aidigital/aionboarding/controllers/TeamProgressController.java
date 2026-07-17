package com.aidigital.aionboarding.controllers;

import com.aidigital.aionboarding.api.v1.TeamProgressApi;
import com.aidigital.aionboarding.api.v1.model.DashboardPeriodV1;
import com.aidigital.aionboarding.api.v1.model.TeamDashboardV1;
import com.aidigital.aionboarding.mappers.teamdashboard.TeamDashboardApiMapper;
import com.aidigital.aionboarding.service.common.error.AppException;
import com.aidigital.aionboarding.service.common.error.ErrorReason;
import com.aidigital.aionboarding.service.common.security.AppUser;
import com.aidigital.aionboarding.service.teamdashboard.services.TeamDashboardService;
import com.aidigital.aionboarding.service.teamdashboard.util.TeamDashboardSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class TeamProgressController implements TeamProgressApi {

    private final CurrentUserSupport currentUser;
    private final TeamDashboardService teamDashboardService;
    private final TeamDashboardSupport teamDashboardSupport;
    private final TeamDashboardApiMapper teamDashboardApiMapper;

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<TeamDashboardV1> getTeamDashboardData(DashboardPeriodV1 period) {
        AppUser viewer = currentUser.requireUser();
        if (!viewer.isAdmin() && !viewer.isTeamLead()) {
            throw new AppException(ErrorReason.C004);
        }
        String periodCode = period == null ? null : period.getValue();
        return ResponseEntity.ok(teamDashboardApiMapper.toTeamDashboardV1(
            teamDashboardService.getTeamDashboardData(
                viewer,
                teamDashboardSupport.resolvePeriod(periodCode)
            )
        ));
    }
}
