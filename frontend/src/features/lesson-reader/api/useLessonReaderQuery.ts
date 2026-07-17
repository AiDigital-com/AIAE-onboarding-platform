import { useQuery } from "@tanstack/react-query";
import type {
    LessonActivityV1,
    LessonNavigation,
    LessonV1,
    RoadmapLessonContext,
} from "@/features/lessons/api/types";
import { lessonDetailQueryOptions, type LessonDetailResponseV1 } from "@/features/lessons/api/lessonDetailQueryOptions";

export interface LessonReaderData {
    lesson: LessonV1;
    activities: LessonActivityV1[];
    initialIsCompleted: boolean;
    roadmapContext: RoadmapLessonContext | null;
    lessonNavigation: LessonNavigation;
    sourceReferences: Array<Record<string, unknown>>;
}

function toLessonReaderData(response: LessonDetailResponseV1): LessonReaderData {
    const lesson = response.lesson as LessonV1;
    const activities = (response.activities ?? []) as LessonActivityV1[];
    const enrollment = response.enrollment;
    const preparedMaterials = (lesson.generationMetadata as {
        preparedMaterials?: { sourceReferences?: Array<Record<string, unknown>> };
    } | undefined)?.preparedMaterials;
    // Priority: typed contract (lesson.sourceReferences) → legacy generationMetadata fallback.
    const typedSourceReferences = (lesson as { sourceReferences?: Array<Record<string, unknown>> }).sourceReferences;
    const sourceReferences = Array.isArray(typedSourceReferences) && typedSourceReferences.length > 0
        ? typedSourceReferences
        : Array.isArray(preparedMaterials?.sourceReferences)
          ? preparedMaterials.sourceReferences
          : [];

    const apiRoadmap = response.roadmapContext;
    const roadmapContext: RoadmapLessonContext | null = apiRoadmap
        ? {
              title: apiRoadmap.roadmapTitle,
              lessonNumber: apiRoadmap.positionInRoadmap,
          }
        : null;
    const lessonNavigation: LessonNavigation = {
        previous: apiRoadmap?.previousLessonId
            ? { id: apiRoadmap.previousLessonId, title: "Previous lesson" }
            : null,
        next: apiRoadmap?.nextLessonId
            ? { id: apiRoadmap.nextLessonId, title: "Next lesson" }
            : null,
    };

    return {
        lesson,
        activities,
        initialIsCompleted: enrollment?.isCompleted ?? false,
        roadmapContext,
        lessonNavigation,
        sourceReferences,
    };
}

export function useLessonReaderQuery(lessonId: string) {
    return useQuery({
        ...lessonDetailQueryOptions(lessonId),
        select: (data) => (data ? toLessonReaderData(data) : null),
    });
}
