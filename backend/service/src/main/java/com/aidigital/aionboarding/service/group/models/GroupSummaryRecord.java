package com.aidigital.aionboarding.service.group.models;

import com.aidigital.aionboarding.service.user.models.UserRecord;

import java.time.LocalDateTime;
import java.util.List;

/**
 * A group list row: enough to render name, leads, and member counts without loading the full
 * member list.
 *
 * @param id                       group primary key
 * @param name                     display name
 * @param description              optional description
 * @param leads                    every lead assigned to this group
 * @param membersCount             total member count
 * @param membersWithoutGradeCount members with no grade assigned
 * @param createdAt                creation timestamp
 * @param updatedAt                last update timestamp
 */
public record GroupSummaryRecord(
		Long id,
		String name,
		String description,
		List<UserRecord> leads,
		long membersCount,
		long membersWithoutGradeCount,
		LocalDateTime createdAt,
		LocalDateTime updatedAt
) {

}
