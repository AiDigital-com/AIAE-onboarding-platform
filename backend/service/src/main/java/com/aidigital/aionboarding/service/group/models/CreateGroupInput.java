package com.aidigital.aionboarding.service.group.models;

/**
 * Input for creating a new group. The name must be explicitly provided — new groups are never
 * auto-named from a lead's name.
 *
 * @param name        required display name, 3-100 characters, unique case-insensitively
 * @param description optional free-text description
 */
public record CreateGroupInput(String name, String description) {
}
