import type { components } from "@/shared/api/generated/schema";

export type MaterialV1 = components["schemas"]["MaterialV1"];
export type MaterialFileV1 = components["schemas"]["MaterialFileV1"];
export type MaterialSummaryV1 = components["schemas"]["MaterialSummaryV1"];
export type LessonSummaryV1 = components["schemas"]["LessonSummaryV1"];
export type LessonV1 = components["schemas"]["LessonV1"];
export type RoadmapV1 = components["schemas"]["RoadmapV1"];
export type RoadmapLessonV1 = components["schemas"]["RoadmapLessonV1"];
export type AssignableUserV1 = components["schemas"]["AssignableUserSummaryV1"];
export type PageInfoV1 = components["schemas"]["PageInfoV1"];
export type QuizQuestionTypeV1 = components["schemas"]["QuizQuestionTypeV1"];

/** A single fetched page of items plus the server's pagination metadata. */
export interface PagedResult<T> {
    items: T[];
    page: PageInfoV1;
}

/** Library list item — extends the bounded search summary with UI fields and detail-only overrides. */
export interface LibraryMaterial extends MaterialSummaryV1 {
    attachments: MaterialFileV1[];
    coverImageStorageKey?: string;
    coverImageOriginalName?: string;
    coverImageMimeType?: string;
    /** Full body text, present only once the material detail dialog's separate fetch resolves. */
    text?: string;
}

export interface LessonActivitySummary {
    id: number;
    type: "quiz" | "flashcards" | string;
    title?: string;
    itemCount?: number;
}

/** Library list item — extends OpenAPI summary with UI fields from the legacy app. */
export interface LibraryLesson extends LessonSummaryV1 {
    description?: string;
    isPublished?: boolean;
    isArchived?: boolean;
    isEnrolled?: boolean;
    activities?: LessonActivitySummary[];
    generationMetadata?: Record<string, unknown>;
    materialIds?: number[];
    sourceReferences?: MaterialV1[];
    createdByUserId?: number | null;
    coverImageStorageKey?: string;
    errorMessage?: string;
    viewerCanManage?: boolean;
    viewerCanGenerateTeacherVideo?: boolean;
    lessonAssets?: LessonV1["lessonAssets"];
    /** Full content, present only once the lesson detail dialog's separate fetch resolves. */
    contentHtml?: string;
    contentMarkdown?: string;
}

export interface RoadmapLessonSummary extends RoadmapLessonV1 {
    isCompleted?: boolean;
}

/** Library roadmap card — extends OpenAPI with enrollment/progress fields. */
export interface LibraryRoadmap extends RoadmapV1 {
    lessons?: RoadmapLessonSummary[];
}
