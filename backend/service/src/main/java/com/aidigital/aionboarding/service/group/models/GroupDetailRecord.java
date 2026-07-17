package com.aidigital.aionboarding.service.group.models;

import com.aidigital.aionboarding.service.user.models.UserRecord;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Full group detail. Members are paged separately through
 * {@link com.aidigital.aionboarding.service.group.services.GroupService#listGroupMembers} rather
 * than embedded here, so a group with many members never loads them all at once.
 *
 * @param id          group primary key
 * @param name        display name
 * @param description optional description
 * @param leads       every lead assigned to this group
 * @param createdAt   creation timestamp
 * @param updatedAt   last update timestamp
 */
public record GroupDetailRecord(
    Long id,
    String name,
    String description,
    List<UserRecord> leads,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
}
