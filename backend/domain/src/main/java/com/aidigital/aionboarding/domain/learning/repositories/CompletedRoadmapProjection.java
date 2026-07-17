package com.aidigital.aionboarding.domain.learning.repositories;

/**
 * Projection for roadmaps that become fully completed after a lesson completion update.
 */
public interface CompletedRoadmapProjection {

    Long getId();

    String getTitle();
}
