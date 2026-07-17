import type { components } from "@/shared/api/generated/schema";

export type LessonSummaryV1 = components["schemas"]["LessonSummaryV1"];
export type LessonV1 = components["schemas"]["LessonV1"];
export type LessonActivityV1 = components["schemas"]["LessonActivityV1"];
export type ActivityAttemptV1 = components["schemas"]["ActivityAttemptV1"];
export type CompletedRoadmapSummaryV1 = components["schemas"]["CompletedRoadmapSummaryV1"];
export type LessonEnrollmentV1 = components["schemas"]["LessonEnrollmentV1"];
export type LessonRoadmapContextV1 = components["schemas"]["LessonRoadmapContextV1"];
export type MyLessonSummaryV1 = components["schemas"]["MyLessonSummaryV1"];
export type PageInfoV1 = components["schemas"]["PageInfoV1"];

/**
 * Card model used by My Lessons grid. Bounded by design: content fields are short previews
 * (not the full lesson body), and activity counts replace full activity payloads — mirrors
 * {@link MyLessonSummaryV1}.
 */
export interface EnrolledLessonCard {
    id: number;
    title: string;
    description?: string;
    status: string;
    publicationStatus: string;
    isPublished: boolean;
    isArchived: boolean;
    contentHtmlPreview?: string;
    contentMarkdownPreview?: string;
    createdBy: string;
    createdAt: string;
    updatedAt: string;
    enrolledAt?: string;
    isCompleted: boolean;
    isEnrolled: boolean;
    tags: string[];
    flashcardCount: number;
    quizCount: number;
    hasTeacherVideo: boolean;
    coverImageStorageKey?: string;
}

export interface RoadmapLessonContext {
    title: string;
    lessonNumber: number;
}

export interface LessonNavigationItem {
    id: number;
    title: string;
}

export interface LessonNavigation {
    previous: LessonNavigationItem | null;
    next: LessonNavigationItem | null;
}

export interface TeacherVideoView {
    videoUrl?: string;
    thumbnailUrl?: string;
}

/** OpenAPI read endpoints still missing from the Spring backend contract. */
export const BLOCKED_LESSON_ENDPOINTS = {
    myLessons:
        "GET /api/v1/learning/my-lessons — enrolled lessons with isCompleted, activities, tags, cover",
    lessonDetail: "GET /api/v1/lessons/{id} — full lesson for reader",
    lessonActivities: "GET /api/v1/lessons/{id}/activities — learner activities with progress",
    lessonActivity: "GET /api/v1/lessons/{id}/activities/{activityId} — activity payload + attempts",
    lessonEnrollment: "GET /api/v1/lessons/{id}/enrollment — enrollment gate for reader",
} as const;
