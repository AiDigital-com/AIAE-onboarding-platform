import type { components } from "@/shared/api/generated/schema";

export type RoadmapV1 = components["schemas"]["RoadmapV1"];
export type RoadmapLessonV1 = components["schemas"]["RoadmapLessonV1"];

export interface RoadmapLessonView extends RoadmapLessonV1 {
    isCompleted?: boolean | null;
}

/** UI view model — API may omit lessons/enrollment until backend enrichment lands. */
export interface RoadmapView extends RoadmapV1 {
    lessons: RoadmapLessonView[];
}

export function toRoadmapView(
    roadmap: RoadmapV1 & Partial<RoadmapView>,
): RoadmapView {
    return {
        ...roadmap,
        lessons: roadmap.lessons ?? [],
        tags: roadmap.tags ?? [],
        isEnrolled: roadmap.isEnrolled,
        enrolledAt: roadmap.enrolledAt ?? null,
        viewerCanManage: roadmap.viewerCanManage ?? false,
    };
}

export function isEnrolledRoadmap(roadmap: RoadmapView): boolean {
    if (typeof roadmap.isEnrolled === "boolean") {
        return roadmap.isEnrolled;
    }

    return Boolean(roadmap.enrolledAt);
}
