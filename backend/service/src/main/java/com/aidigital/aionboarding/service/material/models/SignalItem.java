package com.aidigital.aionboarding.service.material.models;

/**
 * A single example or caveat sentence extracted from a source material.
 *
 * @param sourceNumber the 1-based source number of the originating material
 * @param text         the extracted sentence text
 */
public record SignalItem(
    int sourceNumber,
    String text
) { }
