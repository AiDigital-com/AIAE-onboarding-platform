package com.aidigital.aionboarding.service.team.support;

import com.aidigital.aionboarding.domain.common.dictionary.UserRoleCode;
import com.aidigital.aionboarding.domain.team.entities.TeamMember;
import com.aidigital.aionboarding.domain.user.entities.User;
import com.aidigital.aionboarding.service.common.error.AppException;
import com.aidigital.aionboarding.service.common.error.ErrorReason;
import com.aidigital.aionboarding.service.common.time.CurrentTime;
import com.aidigital.aionboarding.service.mappers.user.UserRecordMapper;
import com.aidigital.aionboarding.service.team.services.entity.TeamEntityService;
import com.aidigital.aionboarding.service.user.models.UserRecord;
import com.aidigital.aionboarding.service.user.services.entity.UserEntityService;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Adds and removes team memberships, resolving the member by id or email/name, for
 * {@code TeamServiceImpl}.
 */
@Component
@RequiredArgsConstructor
public class TeamMembershipSupport {

    private final UserEntityService userEntityService;
    private final TeamEntityService teamEntityService;
    private final UserRecordMapper userMapper;
    private final CurrentTime currentTime;

    /**
     * Adds a member to a team, resolving the member by id or email/name when needed.
     *
     * @param leadUserId team lead internal user id
     * @param memberUserId optional member internal user id; used when present
     * @param memberEmailOrName fallback email or display name used when {@code memberUserId} is {@code null}
     * @return added member user record
     * @throws AppException when the member cannot be resolved or a lead attempts to add themselves
     */
    public UserRecord addTeamMember(Long leadUserId, Long memberUserId, String memberEmailOrName) {
        requireLeadRole(leadUserId);
        User member = memberUserId != null
            ? userEntityService.findById(memberUserId).orElse(null)
            : resolveByEmailOrName(memberEmailOrName);
        if (member == null) {
            throw new AppException(ErrorReason.C001, "member");
        }
        if (member.getId().equals(leadUserId)) {
            throw new AppException(ErrorReason.C002, "A team lead cannot be added to their own team.");
        }
        TeamMember tm = new TeamMember();
        TeamMember.TeamMemberId id = new TeamMember.TeamMemberId();
        id.setLeadUserId(leadUserId);
        id.setMemberUserId(member.getId());
        tm.setId(id);
        tm.setLeadUser(userEntityService.getReference(leadUserId));
        tm.setMemberUser(member);
        tm.setAddedAt(currentTime.utcDateTime());
        teamEntityService.save(tm);
        return userMapper.toRecord(member);
    }

    /**
     * Removes a member from a team.
     *
     * @param leadUserId team lead internal user id
     * @param memberUserId member internal user id
     * @return {@code true} when the membership existed and was removed, otherwise {@code false}
     */
    public boolean removeTeamMember(Long leadUserId, Long memberUserId) {
        requireLeadRole(leadUserId);
        TeamMember.TeamMemberId id = new TeamMember.TeamMemberId();
        id.setLeadUserId(leadUserId);
        id.setMemberUserId(memberUserId);
        if (teamEntityService.existsById(id)) {
            teamEntityService.deleteById(id);
            return true;
        }
        return false;
    }

    /**
     * Asserts that a user is an admin or team lead, the only roles eligible to hold team members.
     *
     * @param leadUserId candidate lead's internal user id
     * @throws AppException with reason {@code C001} when the user does not exist, or {@code C002}
     *                       when their role is neither admin nor team lead
     */
    void requireLeadRole(Long leadUserId) {
        User lead = userEntityService.findById(leadUserId)
            .orElseThrow(() -> new AppException(ErrorReason.C001, "lead"));
        String roleCode = lead.getRole().getCode();
        if (!UserRoleCode.ADMIN.equals(roleCode) && !UserRoleCode.TEAMLEAD.equals(roleCode)) {
            throw new AppException(ErrorReason.C002, "Team lead must be admin or team lead.");
        }
    }

    /**
     * Resolves a user by email or, failing that, by exact case-insensitive display name.
     *
     * @param value raw email or display name input
     * @return the matching user, or {@code null} when blank or not found
     */
    User resolveByEmailOrName(String value) {
        String trimmed = value == null ? "" : value.trim();
        if (trimmed.isBlank()) {
            return null;
        }
        return userEntityService.findByEmail(trimmed.toLowerCase(Locale.ROOT))
            .or(() -> userEntityService.findByNameIgnoreCase(trimmed))
            .orElse(null);
    }
}
