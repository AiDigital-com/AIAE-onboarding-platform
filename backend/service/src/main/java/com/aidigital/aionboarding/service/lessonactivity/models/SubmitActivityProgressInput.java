package com.aidigital.aionboarding.service.lessonactivity.models;

import java.util.List;

/**
 * Typed request to submit progress for one lesson activity, replacing the untyped
 * {@code Map<String, Object>} the OpenAPI request body was previously handed through as.
 *
 * @param type          activity type code being submitted ({@code quiz} or {@code flashcards})
 * @param answers       quiz answers in question order; each entry is the option(s) selected for
 *                      that question. Unused for flashcards.
 * @param reviewedCards number of flashcards reviewed. Unused for quizzes.
 */
public record SubmitActivityProgressInput(
    String type,
    List<List<String>> answers,
    Integer reviewedCards
) { }
