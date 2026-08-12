package com.aidigital.aionboarding.service.permission.services.impl;

import com.aidigital.aionboarding.service.group.services.entity.GroupLeadEntityService;
import com.aidigital.aionboarding.service.group.services.entity.GroupMemberEntityService;
import com.aidigital.aionboarding.service.permission.services.TeamLeadershipService;
import com.aidigital.aionboarding.service.team.services.entity.TeamEntityService;
import java.util.Collections;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TeamLeadershipServiceImpl implements TeamLeadershipService {

    private final TeamEntityService teamEntityService;
    private final GroupLeadEntityService groupLeadEntityService;
    private final GroupMemberEntityService groupMemberEntityService;

    @Override
    @Transactional(readOnly = true)
    public boolean isTeamLeadForMember(Long leadUserId, Long memberUserId) {
        if (teamEntityService.existsByIdLeadUserIdAndIdMemberUserId(leadUserId, memberUserId)) {
            return true;
        }
        Set<Long> ledGroupIds = groupLeadEntityService.findGroupIdsByLeadUserId(leadUserId);
        if (ledGroupIds.isEmpty()) {
            return false;
        }
        return !Collections.disjoint(ledGroupIds, groupMemberEntityService.findGroupIdsByMemberUserId(memberUserId));
    }
}
