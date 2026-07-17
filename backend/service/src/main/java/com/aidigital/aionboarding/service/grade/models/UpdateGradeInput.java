package com.aidigital.aionboarding.service.grade.models;

/**
 * Input for renaming an existing grade dictionary value.
 *
 * @param name new display name
 */
public record UpdateGradeInput(String name) {
}
