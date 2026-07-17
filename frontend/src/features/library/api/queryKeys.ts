import { roadmapsKeys } from "@/features/roadmaps/api/queryKeys";

export const libraryQueryKeys = {
    materials: ["library", "materials"] as const,
    materialDetail: (materialId: string) => ["library", "materials", "detail", materialId] as const,
    lessons: ["library", "lessons"] as const,
    /** Unified with roadmapsKeys.all — both features share one ["roadmaps"] cache entry. */
    roadmaps: roadmapsKeys.all,
    assignees: ["learning", "assignees"] as const,
    lessonAssignees: (lessonId: number) => ["learning", "lesson-assignees", lessonId] as const,
    roadmapAssignees: (roadmapId: number) => ["learning", "roadmap-assignees", roadmapId] as const,
};
