package com.aidigital.aionboarding.service.lesson.services;

import com.aidigital.aionboarding.domain.lesson.entities.Lesson;
import com.aidigital.aionboarding.service.common.security.AppUser;
import com.aidigital.aionboarding.service.lesson.models.CreateLessonInput;

/**
 * Handles the full creation-and-generation flow for a new AI-generated theoretical lesson.
 * <p>
 * The implementation prepares source materials, persists the lesson draft and its material
 * links, runs the AI generation pipeline, and transitions the lesson to {@code ready} or
 * {@code failed} status — all within the caller's transaction boundary.
 */
public interface LessonInitialGenerationService {

    /**
     * Creates a lesson draft, runs AI generation synchronously, and marks it {@code ready}
     * or {@code failed} depending on the outcome.
     *
     * @param viewer authenticated user creating the lesson
     * @param input  creation parameters: optional title override, user instructions,
     *               depth/tone/format preferences, and the list of material IDs to use
     *               as generation sources
     * @return persisted {@link Lesson} entity — status is {@code ready} on success,
     *         {@code failed} when the AI call errors (exception is swallowed and
     *         captured in {@code errorMessage} / {@code generationMetadata})
     */
    Lesson generate(AppUser viewer, CreateLessonInput input);
}
