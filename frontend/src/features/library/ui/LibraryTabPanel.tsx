import { Button } from "@/shared/ui/Button";
import { PanelEmptyState } from "@/shared/ui/PanelEmptyState";
import { LessonsGrid } from "./LessonsGrid";
import { LessonsLoadingState } from "./LessonsLoadingState";
import { MaterialsGrid } from "./MaterialsGrid";
import { MaterialsLoadingState } from "./MaterialsLoadingState";
import { RoadmapsGrid } from "./RoadmapsGrid";
import type { LibraryLesson, LibraryMaterial, LibraryRoadmap } from "../api/types";

interface LibraryTabPanelProps {
    activeTab: string;
    materials?: LibraryMaterial[];
    totalMaterials?: number;
    hasActiveMaterialSearch?: boolean;
    lessons?: LibraryLesson[];
    totalLessons?: number;
    hasActiveLessonFilters?: boolean;
    roadmaps?: LibraryRoadmap[];
    totalRoadmaps?: number;
    hasActiveRoadmapFilters?: boolean;
    isHydrated?: boolean;
    hasMore?: boolean;
    isLoadingMore?: boolean;
    onLoadMore?: () => void;
    onOpenMaterial?: (material: LibraryMaterial) => void;
    onResetMaterialSearch?: () => void;
    onOpenLesson?: (lesson: LibraryLesson) => void;
    onEnrollLesson?: (lesson: LibraryLesson) => void;
    onUnenrollLesson?: (lesson: LibraryLesson) => void;
    onAssignLesson?: (lesson: LibraryLesson) => void;
    onResetLessonFilters?: () => void;
    onResetRoadmapFilters?: () => void;
    onEnrollRoadmap?: (roadmap: LibraryRoadmap) => void;
    onUnenrollRoadmap?: (roadmap: LibraryRoadmap) => void;
    onAssignRoadmap?: (roadmap: LibraryRoadmap) => void;
    onAssignRoadmapToTeam?: (roadmap: LibraryRoadmap) => void;
    onOpenRoadmap?: (roadmap: LibraryRoadmap) => void;
    canAssignLearning?: boolean;
}

function LoadMoreRow({
    hasMore,
    isLoadingMore,
    onLoadMore,
}: {
    hasMore: boolean;
    isLoadingMore: boolean;
    onLoadMore?: () => void;
}) {
    if (!hasMore) {
        return null;
    }

    return (
        <div className="library-load-more">
            <Button variant="secondary" onClick={onLoadMore} disabled={isLoadingMore}>
                {isLoadingMore ? "Loading…" : "Load more"}
            </Button>
        </div>
    );
}

export function LibraryTabPanel({
    activeTab,
    materials = [],
    totalMaterials = materials.length,
    hasActiveMaterialSearch = false,
    lessons = [],
    totalLessons = lessons.length,
    hasActiveLessonFilters = false,
    roadmaps = [],
    totalRoadmaps = roadmaps.length,
    hasActiveRoadmapFilters = false,
    isHydrated = true,
    hasMore = false,
    isLoadingMore = false,
    onLoadMore,
    onOpenMaterial,
    onResetMaterialSearch,
    onOpenLesson,
    onEnrollLesson,
    onUnenrollLesson,
    onAssignLesson,
    onResetLessonFilters,
    onResetRoadmapFilters,
    onEnrollRoadmap,
    onUnenrollRoadmap,
    onAssignRoadmap,
    onAssignRoadmapToTeam,
    onOpenRoadmap,
    canAssignLearning = false,
}: LibraryTabPanelProps) {
    if (activeTab === "materials") {
        if (!isHydrated) {
            return <MaterialsLoadingState />;
        }

        if (materials.length === 0 && hasActiveMaterialSearch) {
            return (
                <PanelEmptyState
                    title="No materials match this search"
                    description="Try a different search term, remove a tag, or reset the filters."
                    actionLabel="Reset filters"
                    onAction={onResetMaterialSearch}
                />
            );
        }

        if (totalMaterials === 0) {
            return (
                <PanelEmptyState
                    title="No materials yet"
                    description="Add YouTube videos, files, links, text notes, images, or combine several source types inside one material."
                />
            );
        }

        return (
            <>
                <MaterialsGrid materials={materials} onOpenMaterial={onOpenMaterial!} />
                <LoadMoreRow hasMore={hasMore} isLoadingMore={isLoadingMore} onLoadMore={onLoadMore} />
            </>
        );
    }

    if (activeTab === "lessons") {
        if (!isHydrated) {
            return <LessonsLoadingState showAction />;
        }

        if (lessons.length === 0 && hasActiveLessonFilters) {
            return (
                <PanelEmptyState
                    title="No lessons match these filters"
                    description="Try a different search term, remove a tag, or reset the filters."
                    actionLabel="Reset filters"
                    onAction={onResetLessonFilters}
                />
            );
        }

        if (totalLessons === 0) {
            return (
                <PanelEmptyState
                    title="No lessons yet"
                    description="Click Create Lesson to generate a theoretical lesson from existing materials."
                />
            );
        }

        return (
            <>
                <LessonsGrid
                    lessons={lessons}
                    onOpenLesson={onOpenLesson}
                    onEnrollLesson={onEnrollLesson}
                    onUnenrollLesson={onUnenrollLesson}
                    onAssignLesson={onAssignLesson}
                    showEnrollmentAction
                    canAssignLearning={canAssignLearning}
                />
                <LoadMoreRow hasMore={hasMore} isLoadingMore={isLoadingMore} onLoadMore={onLoadMore} />
            </>
        );
    }

    if (activeTab === "roadmaps") {
        if (!isHydrated) {
            return <MaterialsLoadingState />;
        }

        if (roadmaps.length === 0 && hasActiveRoadmapFilters) {
            return (
                <PanelEmptyState
                    title="No roadmaps match these filters"
                    description="Try a different search term, remove a tag, or reset the filters."
                    actionLabel="Reset filters"
                    onAction={onResetRoadmapFilters}
                />
            );
        }

        if (totalRoadmaps === 0) {
            return (
                <PanelEmptyState
                    title="No roadmaps yet"
                    description="Click Create Roadmap to assemble a learning path from existing lessons."
                />
            );
        }

        return (
            <>
                <RoadmapsGrid
                    roadmaps={roadmaps}
                    onEnrollRoadmap={onEnrollRoadmap}
                    onUnenrollRoadmap={onUnenrollRoadmap}
                    onAssignRoadmap={onAssignRoadmap}
                    onAssignRoadmapToTeam={onAssignRoadmapToTeam}
                    onOpenRoadmap={onOpenRoadmap}
                    canAssignLearning={canAssignLearning}
                />
                <LoadMoreRow hasMore={hasMore} isLoadingMore={isLoadingMore} onLoadMore={onLoadMore} />
            </>
        );
    }

    return (
        <PanelEmptyState
            title="Nothing here yet"
            description="Choose another library tab to continue."
        />
    );
}
