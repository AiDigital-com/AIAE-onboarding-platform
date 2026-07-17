package com.aidigital.aionboarding.service.lesson.models;

/**
 * A prior chat message supplied to the lesson assistant.
 *
 * @param role    message author role
 * @param content message content
 */
public record ChatTurn(String role, String content) {
}
