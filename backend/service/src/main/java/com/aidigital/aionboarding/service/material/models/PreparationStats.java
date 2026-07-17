package com.aidigital.aionboarding.service.material.models;

/**
 * Material preparation statistics.
 *
 * @param materialCount         number of selected materials
 * @param combinedTextCharacters total characters of combined source text
 */
public record PreparationStats(
    int materialCount,
    int combinedTextCharacters
) { }
