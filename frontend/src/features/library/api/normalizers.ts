import type { LibraryLesson, LibraryMaterial, LibraryRoadmap, PageInfoV1 } from "./types";
import type { components } from "@/shared/api/generated/schema";

type MaterialSummaryV1 = components["schemas"]["MaterialSummaryV1"];
type LessonSummaryV1 = components["schemas"]["LessonSummaryV1"];
type RoadmapV1 = components["schemas"]["RoadmapV1"];

/** Matches the backend's SQL-truncated content preview length (see LessonRepositoryImpl). */
const LESSON_CONTENT_PREVIEW_LENGTH = 500;

/**
 * Client-side mirror of the backend's truncated content preview, for a lesson object fabricated
 * locally (e.g. immediately after create/update) before the list cache is refetched with the
 * server-computed preview.
 */
export function toLessonContentPreview(value?: string | null): string {
    return (value ?? "").slice(0, LESSON_CONTENT_PREVIEW_LENGTH);
}

/** Fallback pagination metadata for a failed/empty response, echoing the requested page/size. */
export function emptyPageInfo(requestedPage = 0, requestedSize = 20): PageInfoV1 {
    return {
        page: requestedPage,
        size: requestedSize,
        totalElements: 0,
        totalPages: 0,
        hasNext: false,
        hasPrevious: false,
    };
}

/**
 * Normalizes a bounded search summary into the list-card shape. Attachments are widened to the
 * full attachment shape with blank OpenAI placeholders, since the summary contract intentionally
 * omits those fields — real values arrive once the detail dialog's separate fetch resolves.
 */
export function normalizeMaterial(material: MaterialSummaryV1): LibraryMaterial {
    return {
        ...material,
        attachments: (material.attachments ?? []).map((attachment) => ({
            ...attachment,
            openaiFileId: "",
            openaiFilePurpose: "",
            openaiFileStatus: "",
            openaiFileError: "",
            openaiUploadedAt: undefined,
        })),
    };
}

export function normalizeLesson(lesson: LessonSummaryV1): LibraryLesson {
    const publicationStatus = lesson.publicationStatus;
    const extended = lesson as LibraryLesson;

    return {
        ...lesson,
        description: extended.description ?? "",
        tags: extended.tags ?? [],
        isPublished: extended.isPublished ?? publicationStatus === "published",
        isArchived: extended.isArchived ?? publicationStatus === "archived",
        isEnrolled: extended.isEnrolled ?? false,
        activities: extended.activities ?? [],
        generationMetadata: extended.generationMetadata ?? {},
        coverImageStorageKey: extended.coverImageStorageKey ?? "",
        errorMessage: extended.errorMessage ?? "",
        viewerCanManage: extended.viewerCanManage ?? false,
        viewerCanGenerateTeacherVideo: extended.viewerCanGenerateTeacherVideo ?? false,
        lessonAssets: extended.lessonAssets ?? [],
    };
}

export function normalizeRoadmap(roadmap: RoadmapV1): LibraryRoadmap {
    const extended = roadmap as LibraryRoadmap;

    return {
        ...roadmap,
        lessonIds: extended.lessonIds ?? [],
        lessons: extended.lessons ?? [],
        isEnrolled: extended.isEnrolled ?? false,
        viewerCanManage: extended.viewerCanManage ?? false,
    };
}
