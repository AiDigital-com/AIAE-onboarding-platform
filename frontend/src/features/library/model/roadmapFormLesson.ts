import type { LibraryLesson } from "../api/types";

/**
 * A lesson selected into the roadmap create/edit form.
 *
 * Sourced either from the full lesson catalog ({@link LibraryLesson}, which always carries real
 * publication state), or - before that catalog query resolves, or for a lesson the catalog no
 * longer returns - from the roadmap's own lesson payload (`RoadmapLessonV1`). That payload does
 * not carry publication state at all (only id/title/description/status/createdAt/sortOrder), so
 * `publicationStatus`/`isPublished`/`isArchived` are optional here and left absent rather than
 * defaulted to a specific state the server never actually reported.
 */
export type RoadmapFormLesson = Omit<LibraryLesson, "publicationStatus" | "isPublished" | "isArchived"> & {
    publicationStatus?: LibraryLesson["publicationStatus"];
    isPublished?: boolean;
    isArchived?: boolean;
};
