package com.aidigital.aionboarding.service.material.models;

import org.springframework.data.domain.Sort;

import java.util.List;

/**
 * Typed filter and sort parameters for a paged material list query.
 *
 * @param searchText free-text search matched against title, description, body text, creator name,
 *     and tags; {@code null} to skip
 * @param tags restricts results to materials tagged with every given value; {@code null} or empty to skip
 * @param createdByUserId restricts results to materials created by this internal user id; {@code null} to skip
 * @param hasAttachments restricts results by presence of file attachments; {@code null} to skip
 * @param hasYoutube restricts results by presence of YouTube links; {@code null} to skip
 * @param hasLinks restricts results by presence of web links; {@code null} to skip
 * @param sortField whitelisted field to sort by
 * @param direction sort direction
 */
public record MaterialListQuery(
    String searchText,
    List<String> tags,
    Long createdByUserId,
    Boolean hasAttachments,
    Boolean hasYoutube,
    Boolean hasLinks,
    MaterialSortField sortField,
    Sort.Direction direction
) {
}
