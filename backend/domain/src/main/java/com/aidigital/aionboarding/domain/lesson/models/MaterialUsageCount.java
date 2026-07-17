package com.aidigital.aionboarding.domain.lesson.models;

/**
 * Number of lessons using a material.
 *
 * @param materialId material identifier
 * @param count      lesson usage count
 */
public record MaterialUsageCount(Long materialId, long count) {
}
