package com.aidigital.aionboarding.service.group.models;

import com.aidigital.aionboarding.service.user.models.UserRecord;

import java.time.LocalDateTime;

/**
 * A group member row, pairing the member's user profile (including grade) with when they joined.
 *
 * @param user     member's user profile, including current grade fields
 * @param joinedAt timestamp the membership was created
 */
public record GroupMemberRecord(UserRecord user, LocalDateTime joinedAt) {

}
