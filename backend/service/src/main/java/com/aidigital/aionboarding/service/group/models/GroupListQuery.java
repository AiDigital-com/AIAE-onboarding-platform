package com.aidigital.aionboarding.service.group.models;

import java.util.Set;

/**
 * Typed filter for a paged group list query.
 *
 * @param searchText          matched against the group name and its leads' name/email; {@code null} to skip
 * @param restrictToGroupIds  when non-null, limits results to these group ids (used to scope a
 *                            Team Lead viewer to groups they lead); {@code null} means unrestricted (Admin)
 */
public record GroupListQuery(String searchText, Set<Long> restrictToGroupIds) {
}
