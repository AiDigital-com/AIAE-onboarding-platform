package com.aidigital.aionboarding.service.team.services.impl;

import com.aidigital.aionboarding.domain.common.dictionary.UserRoleCode;
import com.aidigital.aionboarding.domain.common.dictionary.entities.UserRole;
import com.aidigital.aionboarding.domain.user.entities.User;
import com.aidigital.aionboarding.service.common.dictionary.DictionaryLookupService;
import com.aidigital.aionboarding.service.common.time.CurrentTime;
import com.aidigital.aionboarding.service.mappers.user.UserRecordMapper;
import com.aidigital.aionboarding.service.team.services.TeamLeadPromotionService;
import com.aidigital.aionboarding.service.team.services.entity.TeamEntityService;
import com.aidigital.aionboarding.service.user.models.UserRecord;
import com.aidigital.aionboarding.service.user.services.entity.UserEntityService;
import java.util.Locale;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TeamLeadPromotionServiceImpl implements TeamLeadPromotionService {

    private final UserEntityService userEntityService;
    private final TeamEntityService teamEntityService;
    private final DictionaryLookupService dictionaryLookupService;
    private final UserRecordMapper userMapper;
    private final CurrentTime currentTime;

    @Override
    @Transactional
    public Optional<UserRecord> promoteTeamLeadByEmail(String email) {
        return userEntityService.findByEmail(email.trim().toLowerCase(Locale.ROOT)).map(user -> {
            if (!UserRoleCode.ADMIN.equals(user.getRole().getCode())) {
                UserRole teamLead = dictionaryLookupService.getUserRoleReference(UserRoleCode.TEAMLEAD);
                user.setRole(teamLead);
            }
            user.setUpdatedAt(currentTime.utcDateTime());
            return userMapper.toRecord(userEntityService.save(user));
        });
    }

    @Override
    @Transactional
    public Optional<UserRecord> demoteTeamLeadByEmail(String email) {
        Optional<User> userOpt = userEntityService.findByEmail(email.trim().toLowerCase(Locale.ROOT))
            .filter(u -> UserRoleCode.TEAMLEAD.equals(u.getRole().getCode()));
        if (userOpt.isEmpty()) {
            return Optional.empty();
        }
        User user = userOpt.get();
        UserRole member = dictionaryLookupService.getUserRoleReference(UserRoleCode.MEMBER);
        user.setRole(member);
        user.setUpdatedAt(currentTime.utcDateTime());
        teamEntityService.deleteByIdLeadUserId(user.getId());
        return Optional.of(userMapper.toRecord(userEntityService.save(user)));
    }
}
