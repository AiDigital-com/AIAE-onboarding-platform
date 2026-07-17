package com.aidigital.aionboarding.service.lessonactivity.models;

import java.util.List;

/**
 * Typed manager input for replacing quiz questions or flashcards.
 *
 * @param title activity title
 * @param items quiz questions; ignored for flashcard activities
 * @param cards flashcards; ignored for quiz activities
 */
public record UpdateActivityInput(
    String title,
    List<QuizItemRecord> items,
    List<FlashcardRecord> cards
) { }
