package com.aidigital.aionboarding.controllers;

import com.aidigital.aionboarding.api.v1.TeamsApi;
import com.aidigital.aionboarding.api.v1.model.AddTeamMemberRequestV1;
import com.aidigital.aionboarding.api.v1.model.AddTeamMemberResponseV1;
import com.aidigital.aionboarding.api.v1.model.OkResponseV1;
import com.aidigital.aionboarding.api.v1.model.RemoveTeamMemberRequestV1;
import com.aidigital.aionboarding.api.v1.model.TeamsResponseV1;
import com.aidigital.aionboarding.mappers.team.TeamApiMapper;
import com.aidigital.aionboarding.support.ApiResponses;
import com.aidigital.aionboarding.service.common.error.AppException;
import com.aidigital.aionboarding.service.common.error.ErrorReason;
import com.aidigital.aionboarding.service.common.security.AppUser;
import com.aidigital.aionboarding.service.learning.services.LearningService;
import com.aidigital.aionboarding.service.permission.PermissionKeys;
import com.aidigital.aionboarding.service.permission.services.PermissionService;
import com.aidigital.aionboarding.service.team.services.TeamService;
import com.aidigital.aionboarding.service.user.models.UserRecord;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class TeamsController implements TeamsApi {

    private final CurrentUserSupport currentUser;
    private final TeamService teamService;
    private final LearningService learningService;
    private final PermissionService permissionService;
    private final TeamApiMapper teamApiMapper;
    private final ApiResponses apiResponses;

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<TeamsResponseV1> listTeams(
        Integer teamsPage,
        Integer teamsSize,
        String teamsQuery,
        Integer usersPage,
        Integer usersSize,
        String usersQuery
    ) {
        AppUser viewer = currentUser.requireUser();
        return ResponseEntity.ok(teamApiMapper.toTeamsResponseV1(
            teamService.getTeams(viewer, teamsQuery, pageable(teamsPage, teamsSize)),
            teamService.getTeamCandidateUsers(viewer, usersQuery, pageable(usersPage, usersSize)),
            permissionService.getUserPermissionMap(viewer)
        ));
    }

    @Override
    @PreAuthorize("@perm.has('" + PermissionKeys.TEAMS_MANAGE_MEMBERS + "')")
    @Transactional
    public ResponseEntity<AddTeamMemberResponseV1> addTeamMember(Long leadId, AddTeamMemberRequestV1 request) {
        AppUser viewer = currentUser.requireUser();
        if (!permissionService.canManageTeam(viewer, leadId)) {
            throw new AppException(ErrorReason.C004);
        }
        teamService.getUserById(leadId).orElseThrow(() -> new AppException(ErrorReason.C001, leadId));
        String memberRef = request.getMember() != null ? request.getMember() : request.getEmail();
        UserRecord member = teamService.addTeamMember(leadId, request.getMemberId(), memberRef);
        learningService.syncNewTeamMemberEnrollments(leadId, member.id());
        return ResponseEntity.ok(teamApiMapper.toAddTeamMemberResponseV1(member));
    }

    @Override
    @PreAuthorize("@perm.has('" + PermissionKeys.TEAMS_MANAGE_MEMBERS + "')")
    @Transactional
    public ResponseEntity<OkResponseV1> removeTeamMember(Long leadId, RemoveTeamMemberRequestV1 request) {
        AppUser viewer = currentUser.requireUser();
        if (!permissionService.canManageTeam(viewer, leadId)) {
            throw new AppException(ErrorReason.C004);
        }
        teamService.removeTeamMember(leadId, request.getMemberId());
        return ResponseEntity.ok(apiResponses.ok());
    }

    @Override
    @PreAuthorize("@perm.has('" + PermissionKeys.TEAMS_MANAGE_MEMBERS + "')")
    @Transactional
    public ResponseEntity<OkResponseV1> removeTeamMemberById(Long leadId, Long memberId) {
        AppUser viewer = currentUser.requireUser();
        if (!permissionService.canManageTeam(viewer, leadId)) {
            throw new AppException(ErrorReason.C004);
        }
        teamService.removeTeamMember(leadId, memberId);
        return ResponseEntity.ok(apiResponses.ok());
    }

    Pageable pageable(Integer page, Integer size) {
        int normalizedPage = page == null || page < 0 ? 0 : page;
        int normalizedSize = size == null ? 20 : Math.min(Math.max(size, 1), 100);
        return PageRequest.of(normalizedPage, normalizedSize, Sort.by(
            Sort.Order.asc("name").ignoreCase(),
            Sort.Order.asc("email").ignoreCase()
        ));
    }
}
