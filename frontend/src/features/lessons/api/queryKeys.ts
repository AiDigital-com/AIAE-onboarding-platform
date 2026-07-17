export const lessonsKeys = {
    all: ["lessons"] as const,
    my: () => [...lessonsKeys.all, "my"] as const,
    /** Canonical key for GET /api/v1/lessons/{id} — shared by lessonReader and activityPlayer. */
    detail: (lessonId: string) => ["lessons", "detail", lessonId] as const,
};

export const lessonReaderKeys = {
    all: ["lesson-reader"] as const,
    /** Re-routes to the canonical lesson detail key. */
    detail: (lessonId: string) => lessonsKeys.detail(lessonId),
};

export const activityPlayerKeys = {
    all: ["activities"] as const,
    detail: (lessonId: string, activityId: string) =>
        ["activities", "detail", lessonId, activityId] as const,
};
