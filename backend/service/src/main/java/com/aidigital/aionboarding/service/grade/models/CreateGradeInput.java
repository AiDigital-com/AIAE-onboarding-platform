package com.aidigital.aionboarding.service.grade.models;

/**
 * Input for creating a new grade dictionary value.
 *
 * @param name display name; the internal code is derived from it
 */
public record CreateGradeInput(String name) {
}
