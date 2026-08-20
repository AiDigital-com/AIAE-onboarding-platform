import type { LessonsQueryParams } from "./useLessonsQuery";
import type { MaterialsQueryParams } from "./useMaterialsQuery";
import type { RoadmapsQueryParams } from "./useRoadmapsQuery";

type SortAndDirection = Pick<MaterialsQueryParams, "sort" | "direction">;

/** Maps the UI's material sort option to the server's sort field + direction. */
export function materialSortToParams(sort: string): SortAndDirection {
    switch (sort) {
        case "oldest":
            return { sort: "createdAt", direction: "asc" };
        case "az":
            return { sort: "title", direction: "asc" };
        case "za":
            return { sort: "title", direction: "desc" };
        case "popular":
            return { sort: "usageCount", direction: "desc" };
        default:
            return { sort: "createdAt", direction: "desc" };
    }
}

/** Maps the UI's lesson/roadmap sort option (no "popular") to sort field + direction. */
export function titleSortToParams(sort: string): Pick<LessonsQueryParams, "sort" | "direction"> {
    switch (sort) {
        case "oldest":
            return { sort: "createdAt", direction: "asc" };
        case "az":
            return { sort: "title", direction: "asc" };
        case "za":
            return { sort: "title", direction: "desc" };
        default:
            return { sort: "createdAt", direction: "desc" };
    }
}

/**
 * Maps the UI's lesson visibility filter to status/publicationStatus query params.
 * "published" (labeled Public in the UI) restricts to ready and published lessons.
 * "private" (labeled Private) restricts to the private (assigned-only) publication state —
 * a lesson can only be published once it is ready, so this also covers not-yet-published
 * ready lessons. "archived" is unchanged. This filter only narrows visibility; the server's
 * own visibility rule always additionally restricts a non-manager viewer to public lessons
 * plus lessons assigned to them.
 */
export function lessonStatusFilterToParams(
    status: string,
): Pick<LessonsQueryParams, "status" | "publicationStatus"> {
    switch (status) {
        case "published":
            return { status: "ready", publicationStatus: "published" };
        case "archived":
            return { publicationStatus: "archived" };
        case "private":
            return { publicationStatus: "private" };
        default:
            return {};
    }
}

/** Maps the UI's lesson activity filter to activityType/hasActivities query params. */
export function lessonActivityFilterToParams(
    activity: string,
): Pick<LessonsQueryParams, "activityType" | "hasActivities"> {
    switch (activity) {
        case "quiz":
            return { activityType: "quiz" };
        case "flashcards":
            return { activityType: "flashcards" };
        case "no-activities":
            return { hasActivities: false };
        default:
            return {};
    }
}

/**
 * Maps the UI's enrollment filter to an assignedToMe param for the "enrolled" case.
 * "not-enrolled" has no server-side negation; callers must filter loaded items client-side.
 */
export function enrollmentFilterToAssignedToMe(enrollment: string): { assignedToMe?: true } {
    return enrollment === "enrolled" ? { assignedToMe: true } : {};
}

export type { LessonsQueryParams, MaterialsQueryParams, RoadmapsQueryParams };
