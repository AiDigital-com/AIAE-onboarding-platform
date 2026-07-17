package com.aidigital.aionboarding.service.lessonactivity.models;

/**
 * Bounded per-lesson activity counts by type, for card-level summaries (e.g. "2 quizzes")
 * instead of full activity content.
 */
public record LessonActivityCountsRecord(int flashcardCount, int quizCount) {

}
