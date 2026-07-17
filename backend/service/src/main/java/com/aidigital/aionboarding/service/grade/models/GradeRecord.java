package com.aidigital.aionboarding.service.grade.models;

/**
 * A configurable grade dictionary entry.
 *
 * @param id           grade primary key
 * @param code         stable internal code, derived once from the name at creation time
 * @param name         display name, editable by Admin
 * @param displayOrder sort order for listing
 * @param active       {@code false} when the grade has been deactivated (soft-deleted)
 */
public record GradeRecord(Long id, String code, String name, Integer displayOrder, boolean active) {

}
