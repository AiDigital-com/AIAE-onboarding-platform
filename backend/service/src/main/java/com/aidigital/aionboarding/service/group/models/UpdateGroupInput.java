package com.aidigital.aionboarding.service.group.models;

/**
 * Input for renaming/updating a group. Renaming changes display only — assignments, membership,
 * and progress are untouched.
 *
 * @param name        required display name, 3-100 characters, unique case-insensitively
 * @param description optional free-text description
 */
public record UpdateGroupInput(String name, String description) {

}
