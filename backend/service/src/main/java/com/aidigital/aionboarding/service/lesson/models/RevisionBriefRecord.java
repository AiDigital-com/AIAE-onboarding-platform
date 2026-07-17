package com.aidigital.aionboarding.service.lesson.models;

import java.util.List;

public record RevisionBriefRecord(
    String changeScope,
    String userIntent,
    List<String> editInstructions,
    List<String> preserveRules,
    List<String> riskNotes
) { }
