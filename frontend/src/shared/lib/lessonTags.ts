export const suggestedLessonTags = [
    "Google SEM",
    "Meta",
    "Programmatic",
    "Company",
    "Finance",
    "Invoicing",
    "Analytics",
    "Reporting",
    "Sales",
    "Operations",
    "Client Success",
    "Compliance",
] as const;

/** Soft limit for author-entered tags (input stops here; display ellipsizes longer existing tags). */
export const MAX_LESSON_TAG_LENGTH = 30;

export const MAX_LESSON_TAGS = 12;

/** Props for freeSolo tag text fields so typing/paste cannot exceed the soft max. */
export const lessonTagFieldInputProps = {
    maxLength: MAX_LESSON_TAG_LENGTH,
} as const;

export function clampLessonTag(tag: string): string {
    return tag.trim().replace(/\s+/g, " ").slice(0, MAX_LESSON_TAG_LENGTH);
}

export function normalizeLessonTagInput(tags: unknown = []): string[] {
    if (!Array.isArray(tags)) {
        return [];
    }

    return [
        ...new Set(
            tags
                .map((tag) => (typeof tag === "string" ? clampLessonTag(tag) : ""))
                .filter(Boolean),
        ),
    ].slice(0, MAX_LESSON_TAGS);
}
