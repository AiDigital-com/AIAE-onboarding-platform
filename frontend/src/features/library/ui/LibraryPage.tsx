import { lazy, Suspense, useEffect, useMemo, useRef, useState } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { useSearchParams } from "react-router-dom";
import { lessonDetailQueryOptions } from "@/features/lessons/api/lessonDetailQueryOptions";
import { materialDetailQueryOptions } from "../api/materialDetailQueryOptions";
import { useHasPermission } from "@/shared/auth/useHasPermission";
import { useCurrentUser } from "@/shared/auth/useCurrentUser";
import { useTaskTray } from "@/shared/context/TaskTrayContext";
import { useDebounce } from "@/shared/hooks/useDebounce";
import { suggestedLessonTags } from "@/shared/lib/lessonTags";
import { Dialog } from "@/shared/ui/Dialog";
import { LoadingBlock } from "@/shared/ui/LoadingBlock";
import { Toast, type ToastSeverity } from "@/shared/ui/Toast";
import { useAssignableUsersQuery } from "../api/useAssignableUsersQuery";
import {
    useLearningAssigneesQuery,
    useLearningAssignmentMutation,
    useRevokeLearningAssignmentMutation,
} from "../api/useLearningAssignmentMutation";
import {
    useAssignRoadmapToGroupMutation,
    useUnassignRoadmapFromGroupMutation,
} from "../api/useRoadmapGroupAssignmentMutations";
import { useRoadmapGroupAssignmentsQuery } from "../api/useRoadmapGroupAssignmentsQuery";
import {
    useCreateMaterialMutation,
    useDeleteMaterialMutation,
    useUpdateMaterialMutation,
    useUploadMaterialFileMutation,
} from "../api/useMaterialMutations";
import { useMaterialsQuery } from "../api/useMaterialsQuery";
import { useMaterialsCountQuery } from "../api/useMaterialsCountQuery";
import { useLessonEnrollmentMutation } from "../api/useLessonMutations";
import { useLessonsQuery } from "../api/useLessonsQuery";
import { useLessonsCountQuery } from "../api/useLessonsCountQuery";
import { useGeneratingLessonsPolling } from "../api/useGeneratingLessonsPolling";
import {
    useCreateRoadmapMutation,
    useDeleteRoadmapMutation,
    useRoadmapEnrollmentMutation,
    useUpdateRoadmapMutation,
} from "../api/useRoadmapMutations";
import { useRoadmapsQuery } from "../api/useRoadmapsQuery";
import { useRoadmapsCountQuery } from "../api/useRoadmapsCountQuery";
import { libraryQueryKeys } from "../api/queryKeys";
import {
    enrollmentFilterToAssignedToMe,
    lessonActivityFilterToParams,
    lessonStatusFilterToParams,
    materialSortToParams,
    titleSortToParams,
} from "../api/queryParamMappers";
import type { LibraryLesson, LibraryMaterial, LibraryRoadmap } from "../api/types";
import { LearningAssignmentDialog } from "./LearningAssignmentDialog";
import { LessonLibraryFilters } from "./LessonLibraryFilters";
import { LibraryTabPanel } from "./LibraryTabPanel";
import { LibraryTabs } from "./LibraryTabs";
import { LibraryToolbar } from "./LibraryToolbar";
import { MaterialDetailsDialog } from "./MaterialDetailsDialog";
import { MaterialLibrarySearch } from "./MaterialLibrarySearch";
import { RoadmapFormDialog } from "./RoadmapFormDialog";
import { RoadmapLibraryFilters } from "./RoadmapLibraryFilters";
import { RoadmapGroupAssignmentDialog } from "./RoadmapGroupAssignmentDialog";
import { UploadMaterialDialog } from "./UploadMaterialDialog";
import "./library.css";

// Lazy-loaded: both pull in the Tiptap editor (SimpleEditor), the single
// largest contributor to the Library route's bundle. Splitting them out means the Tiptap
// tree only downloads when a user actually opens a lesson or the create-lesson dialog,
// instead of on every Library page visit.
const CreateLessonDialog = lazy(() =>
    import("./CreateLessonDialog").then((module) => ({ default: module.CreateLessonDialog })),
);
const LessonDetailsDialog = lazy(() =>
    import("./LessonDetailsDialog").then((module) => ({ default: module.LessonDetailsDialog })),
);

/** Stable Suspense fallback for the lazy dialogs above: same backdrop shell, no layout jump. */
function DialogLoadingFallback() {
    return (
        <Dialog open size="lg">
            <LoadingBlock label="Loading editor…" />
        </Dialog>
    );
}

/** True only when the viewer has manage permission and owns the lesson (or is admin). */
function canManageLessonObject(
    user: { id: number; role?: string } | null | undefined,
    lesson: { createdByUserId?: number | null } | null | undefined,
    hasManagePermission: boolean,
): boolean {
    if (!user || !lesson || !hasManagePermission) {
        return false;
    }
    if (user.role === "admin") {
        return true;
    }
    const ownerId = Number(lesson.createdByUserId);
    const userId = Number(user.id);
    return Number.isFinite(ownerId) && Number.isFinite(userId) && ownerId === userId;
}

const PICKER_PAGE_SIZE = 100;
const ASSIGNMENT_PAGE_SIZE = 20;

interface MaterialFormState {
    title: string;
    description: string;
    youtubeInput: string;
    youtubeUrls: string[];
    links: string;
    text: string;
    tags: string[];
    existingAttachments: LibraryMaterial["attachments"];
    newAttachments: File[];
    coverImageStorageKey: string;
    coverImageOriginalName: string;
    coverImageMimeType: string;
}

interface AssignmentDialogState {
    open: boolean;
    itemType: "lesson" | "roadmap";
    item: LibraryLesson | LibraryRoadmap | null;
    selectedUserIds: number[];
    selectedRevokeUserIds: number[];
}

interface GroupAssignmentDialogState {
    open: boolean;
    roadmap: LibraryRoadmap | null;
    pendingGroupId: number | null;
}

/** Library hub — materials, lessons, and roadmaps with filters and dialogs. */
export function LibraryPage() {
    const queryClient = useQueryClient();
    const hasPermission = useHasPermission();
    const { user: currentUser } = useCurrentUser();
    const { addTask, updateTask } = useTaskTray();

    const [searchParams, setSearchParams] = useSearchParams();
    const initialTab = searchParams.get("tab") ?? "materials";
    const [activeTab, setActiveTab] = useState(initialTab);
    const activeTabRef = useRef(initialTab);
    activeTabRef.current = activeTab;
    useEffect(() => {
        const fromUrl = searchParams.get("tab");
        const valid = ["materials", "lessons", "roadmaps"];
        if (fromUrl && valid.includes(fromUrl) && fromUrl !== activeTabRef.current) {
            setActiveTab(fromUrl);
        }
        // Clean the param after consuming so it doesn't persist across unrelated navigations.
        if (fromUrl) {
            const next = new URLSearchParams(searchParams);
            next.delete("tab");
            setSearchParams(next, { replace: true });
        }
    }, [searchParams, setSearchParams]);

    const handleTabChange = (tab: string) => {
        setActiveTab(tab);
    };

    const uploadMaterialFile = useUploadMaterialFileMutation();
    const createMaterial = useCreateMaterialMutation();
    const updateMaterial = useUpdateMaterialMutation();
    const deleteMaterial = useDeleteMaterialMutation();
    const lessonEnrollment = useLessonEnrollmentMutation();
    const roadmapEnrollment = useRoadmapEnrollmentMutation();
    const createRoadmap = useCreateRoadmapMutation();
    const updateRoadmap = useUpdateRoadmapMutation();
    const deleteRoadmap = useDeleteRoadmapMutation();
    const [isUploadDialogOpen, setIsUploadDialogOpen] = useState(false);
    const [isLessonDialogOpen, setIsLessonDialogOpen] = useState(false);
    const [isRoadmapDialogOpen, setIsRoadmapDialogOpen] = useState(false);
    const [editingMaterial, setEditingMaterial] = useState<LibraryMaterial | null>(null);
    const [editingRoadmap, setEditingRoadmap] = useState<LibraryRoadmap | null>(null);
    const [selectedMaterial, setSelectedMaterial] = useState<LibraryMaterial | null>(null);
    const [selectedLesson, setSelectedLesson] = useState<LibraryLesson | null>(null);
    const [materialSearchQuery, setMaterialSearchQuery] = useState("");
    const [materialSort, setMaterialSort] = useState("recent");
    const [materialSelectedTags, setMaterialSelectedTags] = useState<string[]>([]);
    const [areMaterialFiltersOpen, setAreMaterialFiltersOpen] = useState(false);
    const [lessonSearchQuery, setLessonSearchQuery] = useState("");
    const [lessonSort, setLessonSort] = useState("recent");
    const [lessonStatusFilter, setLessonStatusFilter] = useState("all");
    const [lessonSelectedTags, setLessonSelectedTags] = useState<string[]>([]);
    const [lessonActivityFilter, setLessonActivityFilter] = useState("all");
    const [lessonEnrollmentFilter, setLessonEnrollmentFilter] = useState("all");
    const [areLessonFiltersOpen, setAreLessonFiltersOpen] = useState(false);
    const [roadmapSearchQuery, setRoadmapSearchQuery] = useState("");
    const [roadmapSort, setRoadmapSort] = useState("recent");
    const [roadmapSelectedTags, setRoadmapSelectedTags] = useState<string[]>([]);
    const [roadmapEnrollmentFilter, setRoadmapEnrollmentFilter] = useState("all");
    const [areRoadmapFiltersOpen, setAreRoadmapFiltersOpen] = useState(false);
    const [isSavingMaterial, setIsSavingMaterial] = useState(false);
    const [isSavingRoadmap, setIsSavingRoadmap] = useState(false);
    const [isDeletingMaterial, setIsDeletingMaterial] = useState(false);
    const [isDeletingRoadmap, setIsDeletingRoadmap] = useState(false);
    const [assignmentDialog, setAssignmentDialog] = useState<AssignmentDialogState>({
        open: false,
        itemType: "lesson",
        item: null,
        selectedUserIds: [],
        selectedRevokeUserIds: [],
    });
    const [assignmentUserSearchQuery, setAssignmentUserSearchQuery] = useState("");
    const [groupAssignmentDialog, setGroupAssignmentDialog] = useState<GroupAssignmentDialogState>({
        open: false,
        roadmap: null,
        pendingGroupId: null,
    });
    const [materialFormResetKey, setMaterialFormResetKey] = useState(0);
    const [toast, setToast] = useState<{ open: boolean; message: string; severity: ToastSeverity }>({
        open: false,
        message: "",
        severity: "success",
    });

    const canCreateMaterials = hasPermission("materials.create");
    const canEditMaterials = hasPermission("materials.edit");
    const canDeleteMaterials = hasPermission("materials.delete");
    const canCreateLessons = hasPermission("lessons.create");
    const canManageLessons = hasPermission("lessons.manage");
    const canManageLessonActivities = hasPermission("lessons.manage_activities");
    const canPublishArchiveLessons = hasPermission("lessons.publish_archive");
    const canCreateRoadmaps = hasPermission("roadmaps.create");
    const canAssignLearning = hasPermission("learning.assign");

    const debouncedAssignmentUserSearchQuery = useDebounce(assignmentUserSearchQuery, 300);

    const assignableUsersQuery = useAssignableUsersQuery(canAssignLearning && assignmentDialog.open, {
        page: 0,
        size: ASSIGNMENT_PAGE_SIZE,
        query: debouncedAssignmentUserSearchQuery.trim() || undefined,
    });
    const lessonAssignment = useLearningAssignmentMutation("lesson");
    const roadmapAssignment = useLearningAssignmentMutation("roadmap");
    const isSavingAssignment = lessonAssignment.isPending || roadmapAssignment.isPending;
    const assignmentAssigneesQuery = useLearningAssigneesQuery(
        assignmentDialog.itemType,
        assignmentDialog.item?.id ?? null,
        canAssignLearning && assignmentDialog.open,
    );
    const revokeAssignment = useRevokeLearningAssignmentMutation(assignmentDialog.itemType);

    const roadmapGroupAssignmentsQuery = useRoadmapGroupAssignmentsQuery(
        groupAssignmentDialog.roadmap?.id ?? null,
        canAssignLearning && groupAssignmentDialog.open,
    );
    const assignRoadmapToGroup = useAssignRoadmapToGroupMutation();
    const unassignRoadmapFromGroup = useUnassignRoadmapFromGroupMutation();

    // Debounce free-text search so typing doesn't fire a request per keystroke.
    const debouncedMaterialSearchQuery = useDebounce(materialSearchQuery, 300);
    const debouncedLessonSearchQuery = useDebounce(lessonSearchQuery, 300);
    const debouncedRoadmapSearchQuery = useDebounce(roadmapSearchQuery, 300);

    const materialsParams = useMemo(
        () => ({
            query: debouncedMaterialSearchQuery.trim() || undefined,
            tags: materialSelectedTags.length > 0 ? materialSelectedTags : undefined,
            ...materialSortToParams(materialSort),
        }),
        [debouncedMaterialSearchQuery, materialSelectedTags, materialSort],
    );
    const lessonsParams = useMemo(
        () => ({
            query: debouncedLessonSearchQuery.trim() || undefined,
            tags: lessonSelectedTags.length > 0 ? lessonSelectedTags : undefined,
            ...lessonStatusFilterToParams(lessonStatusFilter),
            ...lessonActivityFilterToParams(lessonActivityFilter),
            ...enrollmentFilterToAssignedToMe(lessonEnrollmentFilter),
            ...titleSortToParams(lessonSort),
        }),
        [
            debouncedLessonSearchQuery,
            lessonSelectedTags,
            lessonStatusFilter,
            lessonActivityFilter,
            lessonEnrollmentFilter,
            lessonSort,
        ],
    );
    const roadmapsParams = useMemo(
        () => ({
            query: debouncedRoadmapSearchQuery.trim() || undefined,
            tags: roadmapSelectedTags.length > 0 ? roadmapSelectedTags : undefined,
            ...enrollmentFilterToAssignedToMe(roadmapEnrollmentFilter),
            ...titleSortToParams(roadmapSort),
        }),
        [debouncedRoadmapSearchQuery, roadmapSelectedTags, roadmapEnrollmentFilter, roadmapSort],
    );

    // Only the active tab's full list is fetched; the other two tabs' grids stay unmounted and
    // unfetched until selected. Tab-count badges are covered separately by the cheap count-only
    // queries below, which stay enabled for every tab regardless of which list is active.
    const materialsQuery = useMaterialsQuery(materialsParams, { enabled: activeTab === "materials" });
    const lessonsQuery = useLessonsQuery(lessonsParams, { enabled: activeTab === "lessons" });
    const roadmapsQuery = useRoadmapsQuery(roadmapsParams, { enabled: activeTab === "roadmaps" });

    const materialsCountQuery = useMaterialsCountQuery(materialsParams);
    const lessonsCountQuery = useLessonsCountQuery(lessonsParams);
    const roadmapsCountQuery = useRoadmapsCountQuery(roadmapsParams);

    // Unfiltered, single-page (size 100) catalogs for the lesson/roadmap creation pickers —
    // independent of the Library tab's active filters, only fetched while their dialog is open.
    const allMaterialsQuery = useMaterialsQuery(
        { size: PICKER_PAGE_SIZE },
        { enabled: isLessonDialogOpen },
    );
    const allLessonsQuery = useLessonsQuery(
        { size: PICKER_PAGE_SIZE, readyOnly: true, publicationStatus: "published" },
        { enabled: isRoadmapDialogOpen },
    );

    const materials = materialsQuery.data?.pages.flatMap((page) => page.items) ?? [];
    const lessons = lessonsQuery.data?.pages.flatMap((page) => page.items) ?? [];
    const roadmaps = roadmapsQuery.data?.pages.flatMap((page) => page.items) ?? [];

    // Only meaningful while the Lessons tab is active (lessons is otherwise empty), matching the
    // "no polling for inactive tabs" rule — resumes automatically once the tab is revisited.
    const generatingLessonIds = lessons
        .filter((lesson) => lesson.status === "generating")
        .map((lesson) => lesson.id);
    useGeneratingLessonsPolling(generatingLessonIds);

    // The server has no "not enrolled" negation filter; approximate it over loaded items.
    const visibleLessons =
        lessonEnrollmentFilter === "not-enrolled" ? lessons.filter((lesson) => !lesson.isEnrolled) : lessons;
    const visibleRoadmaps =
        roadmapEnrollmentFilter === "not-enrolled"
            ? roadmaps.filter((roadmap) => !roadmap.isEnrolled)
            : roadmaps;

    // Sourced from the cheap count-only queries (accurate on every tab) rather than the active
    // tab's full-search total, so switching tabs never shows a stale count as if it were exact.
    const materialsTotal = materialsCountQuery.data ?? 0;
    const lessonsTotal = lessonsCountQuery.data ?? 0;
    const roadmapsTotal = roadmapsCountQuery.data ?? 0;

    const pickerMaterials = allMaterialsQuery.data?.pages[0]?.items ?? [];
    const pickerLessons = allLessonsQuery.data?.pages[0]?.items ?? [];

    const showToast = (message: string, severity: ToastSeverity = "success") => {
        setToast({ open: true, message, severity });
    };

    useEffect(() => {
        if (materialsQuery.error) {
            showToast(
                materialsQuery.error instanceof Error
                    ? materialsQuery.error.message
                    : "Failed to load materials.",
                "error",
            );
        }
    }, [materialsQuery.error]);

    useEffect(() => {
        if (lessonsQuery.error) {
            showToast(
                lessonsQuery.error instanceof Error ? lessonsQuery.error.message : "Failed to load lessons.",
                "error",
            );
        }
    }, [lessonsQuery.error]);

    useEffect(() => {
        if (roadmapsQuery.error) {
            showToast(
                roadmapsQuery.error instanceof Error ? roadmapsQuery.error.message : "Failed to load roadmaps.",
                "error",
            );
        }
    }, [roadmapsQuery.error]);

    useEffect(() => {
        if (assignableUsersQuery.error) {
            showToast(
                assignableUsersQuery.error instanceof Error
                    ? assignableUsersQuery.error.message
                    : "Failed to load team members.",
                "error",
            );
        }
    }, [assignableUsersQuery.error]);

    useEffect(() => {
        setSelectedLesson((current) => {
            if (!current) {
                return current;
            }

            const refreshed = lessons.find((lesson) => lesson.id === current.id);
            if (!refreshed) {
                return current;
            }

            return {
                ...current,
                ...refreshed,
                activities: current.activities,
                generationMetadata: current.generationMetadata,
                materialIds: current.materialIds,
                sourceReferences: current.sourceReferences,
                lessonAssets: current.lessonAssets,
                viewerCanManage: current.viewerCanManage,
                viewerCanGenerateTeacherVideo: current.viewerCanGenerateTeacherVideo,
            };
        });
    }, [lessons]);

    // Search, tag, status, activity, and sort filters are now applied server-side (see
    // materialsParams/lessonsParams/roadmapsParams above); "available tags" reflect only the
    // currently loaded page(s), not the full catalog, since there is no distinct-tags endpoint.
    const materialAvailableTags = useMemo(() => {
        const tagSet = new Set<string>(suggestedLessonTags);

        materials.forEach((material) => {
            (Array.isArray(material.tags) ? material.tags : []).forEach((tag) => {
                if (tag) {
                    tagSet.add(tag);
                }
            });
        });

        return [...tagSet].sort((a, b) => a.localeCompare(b));
    }, [materials]);

    const hasActiveMaterialSearch =
        materialSearchQuery.trim().length > 0 || materialSelectedTags.length > 0;

    const resetMaterialFilters = () => {
        setMaterialSearchQuery("");
        setMaterialSelectedTags([]);
    };

    const lessonAvailableTags = useMemo(() => {
        const tagSet = new Set<string>();

        lessons.forEach((lesson) => {
            (Array.isArray(lesson.tags) ? lesson.tags : []).forEach((tag) => {
                if (tag) {
                    tagSet.add(tag);
                }
            });
        });

        return [...tagSet].sort((a, b) => a.localeCompare(b));
    }, [lessons]);

    const hasActiveLessonFilters =
        lessonSearchQuery.trim().length > 0 ||
        lessonStatusFilter !== "all" ||
        lessonSelectedTags.length > 0 ||
        lessonActivityFilter !== "all" ||
        lessonEnrollmentFilter !== "all";

    const resetLessonFilters = () => {
        setLessonSearchQuery("");
        setLessonStatusFilter("all");
        setLessonSelectedTags([]);
        setLessonActivityFilter("all");
        setLessonEnrollmentFilter("all");
    };

    const roadmapAvailableTags = useMemo(() => {
        const tagSet = new Set<string>(suggestedLessonTags);

        roadmaps.forEach((roadmap) => {
            (Array.isArray(roadmap.tags) ? roadmap.tags : []).forEach((tag) => {
                if (tag) {
                    tagSet.add(tag);
                }
            });
        });

        return [...tagSet].sort((a, b) => a.localeCompare(b));
    }, [roadmaps]);

    const hasActiveRoadmapFilters =
        roadmapSearchQuery.trim().length > 0 ||
        roadmapSelectedTags.length > 0 ||
        roadmapEnrollmentFilter !== "all";

    const resetRoadmapFilters = () => {
        setRoadmapSearchQuery("");
        setRoadmapSelectedTags([]);
        setRoadmapEnrollmentFilter("all");
    };

    const showCreatedLessonInFilters = (lesson: LibraryLesson) => {
        const isArchived = lesson.publicationStatus === "archived" || lesson.isArchived;
        const isVisibleAsReady = lesson.status === "ready" && lesson.isPublished && !isArchived;

        handleTabChange("lessons");
        setLessonSearchQuery("");
        setLessonSelectedTags([]);
        setLessonActivityFilter("all");
        setLessonEnrollmentFilter("all");
        setLessonStatusFilter(isVisibleAsReady ? "ready" : "draft");
    };

    const handlePrimaryAction = () => {
        if (activeTab === "materials") {
            if (!canCreateMaterials) {
                return;
            }

            setEditingMaterial(null);
            setIsUploadDialogOpen(true);
            return;
        }

        if (activeTab === "lessons") {
            if (!canCreateLessons) {
                return;
            }

            setIsLessonDialogOpen(true);
            return;
        }

        if (activeTab === "roadmaps") {
            if (!canCreateRoadmaps) {
                return;
            }

            setEditingRoadmap(null);
            setIsRoadmapDialogOpen(true);
        }
    };

    const handleCloseUploadDialog = () => {
        if (isSavingMaterial) {
            return;
        }

        setEditingMaterial(null);
        setIsUploadDialogOpen(false);
    };

    const handleOpenMaterial = async (material: LibraryMaterial) => {
        // Open immediately with the bounded summary, then hydrate with full-fidelity content.
        setSelectedMaterial(material);
        try {
            // revalidateIfStale: an invalidated detail entry has no active observer while the
            // dialog is closed, so without it ensureQueryData would serve the stale snapshot.
            const data = await queryClient.ensureQueryData({
                ...materialDetailQueryOptions(String(material.id)),
                revalidateIfStale: true,
            });

            if (data?.material) {
                setSelectedMaterial((current) =>
                    current && current.id === material.id ? { ...material, ...data.material } : current,
                );
            }
        } catch (error) {
            showToast(error instanceof Error ? error.message : "Failed to load material details.", "error");
        }
    };

    const handleOpenSourceMaterial = async (materialId: number) => {
        // Fetches by id directly rather than looking the material up in the Materials tab's own
        // list query, which is only fetched while that tab is active (see materialsQuery above) —
        // a lesson's source-material reference must open regardless of which tab is showing.
        try {
            const data = await queryClient.ensureQueryData({
                ...materialDetailQueryOptions(String(materialId)),
                revalidateIfStale: true,
            });

            if (!data?.material) {
                showToast("Source material is no longer available.", "warning");
                return;
            }

            setSelectedMaterial({
                ...data.material,
                hasText: Boolean(data.material.text),
                attachments: data.material.attachments ?? [],
            });
        } catch (error) {
            showToast(error instanceof Error ? error.message : "Failed to load source material.", "error");
        }
    };

    const handleOpenLesson = async (lesson: LibraryLesson) => {
        // Open immediately with a safe manage flag so Member never flashes creator controls.
        setSelectedLesson({
            ...lesson,
            viewerCanManage: canManageLessonObject(currentUser, lesson, canManageLessons),
        });
        try {
            // revalidateIfStale: an invalidated detail entry has no active observer while the
            // dialog is closed, so without it ensureQueryData would serve the stale snapshot.
            const data = await queryClient.ensureQueryData({
                ...lessonDetailQueryOptions(String(lesson.id)),
                revalidateIfStale: true,
            });

            if (data?.lesson) {
                const detailLesson = data.lesson as unknown as LibraryLesson;
                setSelectedLesson({
                    ...lesson,
                    ...detailLesson,
                    activities: data.activities ?? [],
                    viewerCanManage: canManageLessonObject(currentUser, detailLesson, canManageLessons),
                });
            }
        } catch (error) {
            showToast(error instanceof Error ? error.message : "Failed to load lesson details.", "error");
        }
    };

    const handleLessonGenerated = async (lesson: LibraryLesson) => {
        await queryClient.invalidateQueries({ queryKey: libraryQueryKeys.lessons });
        showCreatedLessonInFilters(lesson);
        setIsLessonDialogOpen(false);
        showToast("Lesson generated successfully.");
    };

    const handleLessonUpdated = async (updatedLesson: LibraryLesson, options: { silent?: boolean } = {}) => {
        setSelectedLesson((current) => {
            if (!current || current.id !== updatedLesson.id) {
                return updatedLesson;
            }

            return {
                ...current,
                ...updatedLesson,
                tags: updatedLesson.tags ?? current.tags,
                activities: updatedLesson.activities ?? current.activities,
                generationMetadata: updatedLesson.generationMetadata ?? current.generationMetadata,
                materialIds: updatedLesson.materialIds ?? current.materialIds,
                sourceReferences: updatedLesson.sourceReferences ?? current.sourceReferences,
                lessonAssets: updatedLesson.lessonAssets ?? current.lessonAssets,
                createdByUserId: updatedLesson.createdByUserId ?? current.createdByUserId,
                viewerCanManage: current.viewerCanManage,
                viewerCanGenerateTeacherVideo: current.viewerCanGenerateTeacherVideo,
            };
        });

        if (options.silent) {
            return;
        }

        showToast("Lesson updated successfully.");
    };

    const handleLessonDeleted = async (lessonId: number) => {
        setSelectedLesson(null);
        showToast("Lesson deleted successfully.");
        void lessonId;
    };

    const handleEnrollLesson = async (lesson: LibraryLesson) => {
        try {
            await lessonEnrollment.enroll.mutateAsync(lesson.id);
            showToast("Lesson added to My Lessons.");
        } catch (error) {
            showToast(
                error instanceof Error ? error.message : "Failed to add lesson to My Lessons.",
                "error",
            );
        }
    };

    const handleUnenrollLesson = async (lesson: LibraryLesson) => {
        try {
            await lessonEnrollment.unenroll.mutateAsync(lesson.id);
            showToast("Lesson removed from My Lessons.");
        } catch (error) {
            showToast(
                error instanceof Error ? error.message : "Failed to remove lesson from My Lessons.",
                "error",
            );
        }
    };

    const handleOpenAssignmentDialog = (itemType: "lesson" | "roadmap", item: LibraryLesson | LibraryRoadmap) => {
        if (!canAssignLearning) {
            return;
        }

        setAssignmentDialog({
            open: true,
            itemType,
            item,
            selectedUserIds: [],
            selectedRevokeUserIds: [],
        });
        setAssignmentUserSearchQuery("");
    };

    const handleCloseAssignmentDialog = () => {
        if (isSavingAssignment || revokeAssignment.isPending) {
            return;
        }

        setAssignmentDialog({
            open: false,
            itemType: "lesson",
            item: null,
            selectedUserIds: [],
            selectedRevokeUserIds: [],
        });
        setAssignmentUserSearchQuery("");
    };

    const handleToggleAssignmentUser = (userId: number) => {
        setAssignmentDialog((prev) => {
            const selectedUserIds = prev.selectedUserIds.includes(userId)
                ? prev.selectedUserIds.filter((id) => id !== userId)
                : [...prev.selectedUserIds, userId];

            return { ...prev, selectedUserIds };
        });
    };

    const handleToggleAllAssignmentUsers = () => {
        const assignedUserIds = new Set((assignmentAssigneesQuery.data ?? []).map((assignee) => assignee.userId));
        const users = (assignableUsersQuery.data ?? []).filter((user) => !assignedUserIds.has(user.id));

        setAssignmentDialog((prev) => {
            const allSelected = users.length > 0 && prev.selectedUserIds.length === users.length;

            return {
                ...prev,
                selectedUserIds: allSelected ? [] : users.map((user) => user.id),
            };
        });
    };

    const handleToggleRevokeUser = (userId: number) => {
        setAssignmentDialog((prev) => {
            const selectedRevokeUserIds = prev.selectedRevokeUserIds.includes(userId)
                ? prev.selectedRevokeUserIds.filter((id) => id !== userId)
                : [...prev.selectedRevokeUserIds, userId];

            return { ...prev, selectedRevokeUserIds };
        });
    };

    const handleToggleAllRevokeUsers = () => {
        const assignees = assignmentAssigneesQuery.data ?? [];

        setAssignmentDialog((prev) => {
            const allSelected = assignees.length > 0 && prev.selectedRevokeUserIds.length === assignees.length;

            return {
                ...prev,
                selectedRevokeUserIds: allSelected ? [] : assignees.map((assignee) => assignee.userId),
            };
        });
    };

    const handleRevokeSelectedAssignments = async () => {
        const { itemType, item, selectedRevokeUserIds } = assignmentDialog;
        if (!item || selectedRevokeUserIds.length === 0) {
            return;
        }

        const noun = itemType === "roadmap" ? "Roadmap" : "Lesson";
        const revokedCount = selectedRevokeUserIds.length;

        try {
            await revokeAssignment.mutateAsync({ itemId: item.id, userIds: selectedRevokeUserIds });
            setAssignmentDialog((prev) => ({ ...prev, selectedRevokeUserIds: [] }));
            showToast(
                `${noun} assignment revoked for ${revokedCount} team member${revokedCount === 1 ? "" : "s"}.`,
            );
        } catch (error) {
            console.error(`Failed to revoke ${itemType} assignments:`, error);
            showToast(
                error instanceof Error ? error.message : `Failed to revoke ${itemType} assignment.`,
                "error",
            );
        }
    };

    const handleSubmitAssignment = async () => {
        const { itemType, item, selectedUserIds } = assignmentDialog;

        if (!item || selectedUserIds.length === 0) {
            return;
        }

        const noun = itemType === "roadmap" ? "Roadmap" : "Lesson";

        try {
            if (itemType === "roadmap") {
                await roadmapAssignment.mutateAsync({ itemId: item.id, userIds: selectedUserIds });
            } else {
                await lessonAssignment.mutateAsync({ itemId: item.id, userIds: selectedUserIds });
            }

            setAssignmentDialog({
                open: false,
                itemType: "lesson",
                item: null,
                selectedUserIds: [],
                selectedRevokeUserIds: [],
            });
            setAssignmentUserSearchQuery("");

            showToast(
                `${noun} assigned to ${selectedUserIds.length} team member${selectedUserIds.length === 1 ? "" : "s"}.`,
            );
        } catch (error) {
            showToast(
                error instanceof Error ? error.message : `Failed to assign ${itemType}.`,
                "error",
            );
        }
    };

    const handleOpenGroupAssignmentDialog = (roadmap: LibraryRoadmap) => {
        if (!canAssignLearning) {
            return;
        }

        setGroupAssignmentDialog({ open: true, roadmap, pendingGroupId: null });
    };

    const handleCloseGroupAssignmentDialog = () => {
        if (groupAssignmentDialog.pendingGroupId !== null) {
            return;
        }

        setGroupAssignmentDialog({ open: false, roadmap: null, pendingGroupId: null });
    };

    const handleAssignRoadmapToGroup = async (groupId: number, gradeIds: number[]) => {
        const roadmap = groupAssignmentDialog.roadmap;
        if (!roadmap) {
            return;
        }

        setGroupAssignmentDialog((prev) => ({ ...prev, pendingGroupId: groupId }));

        try {
            await assignRoadmapToGroup.mutateAsync({ roadmapId: roadmap.id, groupId, gradeIds });
            showToast("Roadmap assigned to team.");
        } catch (error) {
            showToast(
                error instanceof Error ? error.message : "Failed to update team assignment.",
                "error",
            );
        } finally {
            setGroupAssignmentDialog((prev) => ({ ...prev, pendingGroupId: null }));
        }
    };

    const handleRevokeRoadmapFromGroup = async (groupId: number) => {
        const roadmap = groupAssignmentDialog.roadmap;
        if (!roadmap) {
            return;
        }

        setGroupAssignmentDialog((prev) => ({ ...prev, pendingGroupId: groupId }));

        try {
            await unassignRoadmapFromGroup.mutateAsync({ roadmapId: roadmap.id, groupId });
            showToast("Roadmap unassigned from team.");
        } catch (error) {
            showToast(
                error instanceof Error ? error.message : "Failed to update team assignment.",
                "error",
            );
        } finally {
            setGroupAssignmentDialog((prev) => ({ ...prev, pendingGroupId: null }));
        }
    };

    const handleSaveRoadmap = async (formData: {
        title: string;
        description: string;
        tags: string[];
        lessonIds: number[];
    }) => {
        const roadmapBeingEdited = editingRoadmap;

        try {
            setIsSavingRoadmap(true);

            if (roadmapBeingEdited) {
                await updateRoadmap.mutateAsync({
                    id: roadmapBeingEdited.id,
                    payload: formData,
                });
            } else {
                await createRoadmap.mutateAsync(formData);
            }

            setEditingRoadmap(null);
            setIsRoadmapDialogOpen(false);
            showToast(
                roadmapBeingEdited ? "Roadmap updated successfully." : "Roadmap created successfully.",
            );
        } catch (error) {
            showToast(
                error instanceof Error
                    ? error.message
                    : roadmapBeingEdited
                      ? "Failed to update roadmap."
                      : "Failed to create roadmap.",
                "error",
            );
        } finally {
            setIsSavingRoadmap(false);
        }
    };

    const handleOpenRoadmap = (roadmap: LibraryRoadmap) => {
        if (!roadmap?.viewerCanManage) {
            return;
        }

        setEditingRoadmap(roadmap);
        setIsRoadmapDialogOpen(true);
    };

    const handleDeleteRoadmap = async (roadmap: LibraryRoadmap) => {
        if (isDeletingRoadmap) {
            return;
        }

        const shouldDelete = window.confirm(
            `Delete "${roadmap.title}"? This will remove the roadmap for everyone, but it will not delete the lessons.`,
        );

        if (!shouldDelete) {
            return;
        }

        try {
            setIsDeletingRoadmap(true);
            await deleteRoadmap.mutateAsync(roadmap.id);
            setEditingRoadmap(null);
            setIsRoadmapDialogOpen(false);
            showToast("Roadmap deleted successfully.");
        } catch (error) {
            showToast(error instanceof Error ? error.message : "Failed to delete roadmap.", "error");
        } finally {
            setIsDeletingRoadmap(false);
        }
    };

    const handleEnrollRoadmap = async (roadmap: LibraryRoadmap) => {
        try {
            await roadmapEnrollment.enroll.mutateAsync(roadmap.id);
            showToast("Roadmap added to your learning plan.");
        } catch (error) {
            showToast(error instanceof Error ? error.message : "Failed to subscribe to roadmap.", "error");
        }
    };

    const handleUnenrollRoadmap = async (roadmap: LibraryRoadmap) => {
        try {
            await roadmapEnrollment.unenroll.mutateAsync(roadmap.id);
            showToast("Roadmap removed from your learning plan.");
        } catch (error) {
            showToast(
                error instanceof Error ? error.message : "Failed to unsubscribe from roadmap.",
                "error",
            );
        }
    };

    const hydrateLessonSourceAttachments = (lesson: LibraryLesson | null): LibraryLesson | null => {
        if (!lesson) {
            return null;
        }

        const sourceReferences = (
            lesson.generationMetadata as {
                preparedMaterials?: { sourceReferences?: Array<{ id: number }> };
            }
        )?.preparedMaterials?.sourceReferences;

        if (!Array.isArray(sourceReferences)) {
            return lesson;
        }

        const materialsById = new Map(materials.map((material) => [material.id, material]));

        return {
            ...lesson,
            generationMetadata: {
                ...lesson.generationMetadata,
                preparedMaterials: {
                    ...(lesson.generationMetadata as { preparedMaterials?: Record<string, unknown> })
                        ?.preparedMaterials,
                    sourceReferences: sourceReferences.map((source) => {
                        const material = materialsById.get(source.id);

                        if (!material) {
                            return source;
                        }

                        return {
                            ...source,
                            attachments: (material.attachments || []).map((attachment) => {
                                return {
                                    id: attachment.id,
                                    name: attachment.name,
                                    storageKey: attachment.storageKey,
                                    mimeType: attachment.mimeType,
                                    kind: attachment.kind,
                                    size: attachment.size,
                                    previewUrl: "",
                                    openaiFileId: attachment.openaiFileId,
                                    openaiFileStatus: attachment.openaiFileStatus,
                                };
                            }),
                        };
                    }),
                },
            },
        };
    };

    const handleEditMaterial = async (material: LibraryMaterial | null) => {
        if (!material) {
            return;
        }

        setSelectedMaterial(null);
        // The edit form needs full-fidelity attachments (including OpenAI upload state) so saving
        // unmodified attachments doesn't overwrite their metadata with the bounded summary's blanks —
        // fetch detail before opening, regardless of whether the details dialog already resolved it.
        try {
            const data = await queryClient.ensureQueryData({
                ...materialDetailQueryOptions(String(material.id)),
                revalidateIfStale: true,
            });
            setEditingMaterial(data?.material ? { ...material, ...data.material } : material);
        } catch (error) {
            showToast(error instanceof Error ? error.message : "Failed to load material details.", "error");
            setEditingMaterial(material);
        }
        setIsUploadDialogOpen(true);
    };

    const uploadNewAttachments = async (files: File[] = []) => {
        const uploadedAttachments = [];

        for (const file of files) {
            if (file.size > 10 * 1024 * 1024) {
                throw new Error(`"${file.name}" is too large — files must be under 10 MB.`);
            }
            const uploadData = await uploadMaterialFile.mutateAsync(file);

            uploadedAttachments.push({
                originalName: file.name,
                storageKey: uploadData?.storageKey,
                mimeType: file.type || "application/octet-stream",
                sizeBytes: file.size,
                kind: (file.type.startsWith("image/") ? "image" : "file") as "file" | "image",
            });
        }

        return uploadedAttachments;
    };

    const handleSaveMaterial = async (formData: MaterialFormState) => {
        const materialBeingEdited = editingMaterial;
        const isEditMode = Boolean(materialBeingEdited);
        const taskId = addTask({
            title: isEditMode ? "Updating material" : "Creating material",
            description: formData.title.trim() || "Preparing material sources",
        });

        setIsUploadDialogOpen(false);
        setEditingMaterial(null);

        try {
            setIsSavingMaterial(true);
            updateTask(taskId, {
                description: formData.newAttachments?.length
                    ? `Uploading ${formData.newAttachments.length} file(s)...`
                    : "Saving sources and metadata...",
            });

            const uploadedAttachments = await uploadNewAttachments(formData.newAttachments || []);
            const retainedAttachments = (formData.existingAttachments || []).map((attachment) => ({
                id: attachment.id,
                originalName: attachment.name,
                storageKey: attachment.storageKey,
                mimeType: attachment.mimeType || "",
                sizeBytes: attachment.size || 0,
                kind: attachment.kind,
                openaiFileId: attachment.openaiFileId || "",
                openaiFilePurpose: attachment.openaiFilePurpose || "",
                openaiFileStatus: attachment.openaiFileStatus || "",
                openaiFileError: attachment.openaiFileError || "",
                openaiUploadedAt: attachment.openaiUploadedAt || null,
            }));

            const payload = {
                title: formData.title.trim(),
                description: formData.description.trim(),
                youtubeUrls: formData.youtubeUrls,
                links: formData.links
                    .split("\n")
                    .map((item) => item.trim())
                    .filter(Boolean),
                text: formData.text.trim(),
                tags: formData.tags || [],
                attachments: [...retainedAttachments, ...uploadedAttachments],
                coverImageStorageKey: formData.coverImageStorageKey || "",
                coverImageOriginalName: formData.coverImageOriginalName || "",
                coverImageMimeType: formData.coverImageMimeType || "",
            };

            if (materialBeingEdited) {
                await updateMaterial.mutateAsync({ id: materialBeingEdited.id, payload });
            } else {
                await createMaterial.mutateAsync(payload);
            }

            updateTask(taskId, { description: "Refreshing library..." });
            await queryClient.invalidateQueries({ queryKey: libraryQueryKeys.materials });
            updateTask(taskId, {
                status: "success",
                description: materialBeingEdited
                    ? "Material updated successfully."
                    : "Material created successfully.",
            });

            showToast(
                materialBeingEdited ? "Material updated successfully." : "Material saved successfully.",
            );

            if (!materialBeingEdited) {
                setMaterialFormResetKey((prev) => prev + 1);
            }
        } catch (error) {
            showToast(error instanceof Error ? error.message : "Failed to save material.", "error");
            updateTask(taskId, {
                status: "error",
                description: error instanceof Error ? error.message : "Failed to save material.",
            });
        } finally {
            setIsSavingMaterial(false);
        }
    };

    const handleDeleteMaterial = async (material: LibraryMaterial) => {
        try {
            setIsDeletingMaterial(true);
            await deleteMaterial.mutateAsync(material.id);
            setSelectedMaterial((prev) => (prev?.id === material.id ? null : prev));
            showToast("Material deleted successfully.");
        } catch (error) {
            showToast(error instanceof Error ? error.message : "Failed to delete material.", "error");
        } finally {
            setIsDeletingMaterial(false);
        }
    };

    const hydratedSelectedLesson = hydrateLessonSourceAttachments(selectedLesson);

    return (
        <div className="library-page">
            <LibraryToolbar
                activeTab={activeTab}
                onPrimaryAction={handlePrimaryAction}
                canCreateByTab={{
                    materials: canCreateMaterials,
                    lessons: canCreateLessons,
                    roadmaps: canCreateRoadmaps,
                }}
            />

            <LibraryTabs
                activeTab={activeTab}
                onTabChange={handleTabChange}
                counts={{
                    // undefined (not 0) while the count query hasn't resolved yet, so the badge
                    // can show "unknown" instead of a confidently wrong zero.
                    materials: materialsCountQuery.isSuccess ? materialsTotal : undefined,
                    lessons: lessonsCountQuery.isSuccess ? lessonsTotal : undefined,
                    roadmaps: roadmapsCountQuery.isSuccess ? roadmapsTotal : undefined,
                }}
                actionSlot={
                    activeTab === "materials" && (materialsTotal > 0 || hasActiveMaterialSearch) ? (
                        <MaterialLibrarySearch
                            query={materialSearchQuery}
                            onQueryChange={setMaterialSearchQuery}
                            sort={materialSort}
                            onSortChange={setMaterialSort}
                            selectedTags={materialSelectedTags}
                            onSelectedTagsChange={setMaterialSelectedTags}
                            availableTags={materialAvailableTags}
                            totalCount={materialsTotal}
                            resultCount={materialsTotal}
                            hasActiveFilters={hasActiveMaterialSearch}
                            filtersOpen={areMaterialFiltersOpen}
                            onToggleFilters={() => setAreMaterialFiltersOpen((prev) => !prev)}
                            onReset={resetMaterialFilters}
                        />
                    ) : activeTab === "lessons" && (lessonsTotal > 0 || hasActiveLessonFilters) ? (
                        <LessonLibraryFilters
                            query={lessonSearchQuery}
                            onQueryChange={setLessonSearchQuery}
                            status={lessonStatusFilter}
                            onStatusChange={setLessonStatusFilter}
                            selectedTags={lessonSelectedTags}
                            onSelectedTagsChange={setLessonSelectedTags}
                            availableTags={lessonAvailableTags}
                            activity={lessonActivityFilter}
                            onActivityChange={setLessonActivityFilter}
                            enrollment={lessonEnrollmentFilter}
                            onEnrollmentChange={setLessonEnrollmentFilter}
                            sort={lessonSort}
                            onSortChange={setLessonSort}
                            totalCount={lessonsTotal}
                            resultCount={visibleLessons.length}
                            hasActiveFilters={hasActiveLessonFilters}
                            filtersOpen={areLessonFiltersOpen}
                            onToggleFilters={() => setAreLessonFiltersOpen((prev) => !prev)}
                            onReset={resetLessonFilters}
                        />
                    ) : activeTab === "roadmaps" && (roadmapsTotal > 0 || hasActiveRoadmapFilters) ? (
                        <RoadmapLibraryFilters
                            query={roadmapSearchQuery}
                            onQueryChange={setRoadmapSearchQuery}
                            selectedTags={roadmapSelectedTags}
                            onSelectedTagsChange={setRoadmapSelectedTags}
                            availableTags={roadmapAvailableTags}
                            enrollment={roadmapEnrollmentFilter}
                            onEnrollmentChange={setRoadmapEnrollmentFilter}
                            sort={roadmapSort}
                            onSortChange={setRoadmapSort}
                            totalCount={roadmapsTotal}
                            resultCount={visibleRoadmaps.length}
                            hasActiveFilters={hasActiveRoadmapFilters}
                            filtersOpen={areRoadmapFiltersOpen}
                            onToggleFilters={() => setAreRoadmapFiltersOpen((prev) => !prev)}
                            onReset={resetRoadmapFilters}
                        />
                    ) : null
                }
            />

            <div key={activeTab} className="library-panel-fade">
                <LibraryTabPanel
                    activeTab={activeTab}
                    materials={materials}
                    totalMaterials={materialsTotal}
                    hasActiveMaterialSearch={hasActiveMaterialSearch}
                    onResetMaterialSearch={resetMaterialFilters}
                    lessons={visibleLessons}
                    totalLessons={lessonsTotal}
                    hasActiveLessonFilters={hasActiveLessonFilters}
                    onResetLessonFilters={resetLessonFilters}
                    roadmaps={visibleRoadmaps}
                    totalRoadmaps={roadmapsTotal}
                    hasActiveRoadmapFilters={hasActiveRoadmapFilters}
                    onResetRoadmapFilters={resetRoadmapFilters}
                    isHydrated={
                        activeTab === "lessons"
                            ? !lessonsQuery.isLoading
                            : activeTab === "roadmaps"
                              ? !roadmapsQuery.isLoading
                              : !materialsQuery.isLoading
                    }
                    hasMore={
                        activeTab === "lessons"
                            ? Boolean(lessonsQuery.hasNextPage)
                            : activeTab === "roadmaps"
                              ? Boolean(roadmapsQuery.hasNextPage)
                              : Boolean(materialsQuery.hasNextPage)
                    }
                    isLoadingMore={
                        activeTab === "lessons"
                            ? lessonsQuery.isFetchingNextPage
                            : activeTab === "roadmaps"
                              ? roadmapsQuery.isFetchingNextPage
                              : materialsQuery.isFetchingNextPage
                    }
                    onLoadMore={() => {
                        if (activeTab === "lessons") {
                            void lessonsQuery.fetchNextPage();
                        } else if (activeTab === "roadmaps") {
                            void roadmapsQuery.fetchNextPage();
                        } else {
                            void materialsQuery.fetchNextPage();
                        }
                    }}
                    onOpenMaterial={handleOpenMaterial}
                    onOpenLesson={handleOpenLesson}
                    onEnrollLesson={handleEnrollLesson}
                    onUnenrollLesson={handleUnenrollLesson}
                    onAssignLesson={(lesson) => handleOpenAssignmentDialog("lesson", lesson)}
                    onEnrollRoadmap={handleEnrollRoadmap}
                    onUnenrollRoadmap={handleUnenrollRoadmap}
                    onAssignRoadmap={(roadmap) => handleOpenAssignmentDialog("roadmap", roadmap)}
                    onAssignRoadmapToTeam={handleOpenGroupAssignmentDialog}
                    onOpenRoadmap={handleOpenRoadmap}
                    canAssignLearning={canAssignLearning}
                />
            </div>

            <UploadMaterialDialog
                key={editingMaterial ? `edit-${editingMaterial.id}` : "create-material"}
                open={isUploadDialogOpen}
                onClose={handleCloseUploadDialog}
                onSave={handleSaveMaterial}
                onValidationError={(message) => showToast(message, "error")}
                isSaving={isSavingMaterial}
                mode={editingMaterial ? "edit" : "create"}
                initialMaterial={editingMaterial}
                resetKey={materialFormResetKey}
            />

            {isLessonDialogOpen && (
                <Suspense fallback={<DialogLoadingFallback />}>
                    <CreateLessonDialog
                        open={isLessonDialogOpen}
                        materials={pickerMaterials}
                        onClose={() => setIsLessonDialogOpen(false)}
                        onLessonGenerated={handleLessonGenerated}
                    />
                </Suspense>
            )}

            <RoadmapFormDialog
                key={editingRoadmap ? `edit-${editingRoadmap.id}` : "create-roadmap"}
                open={isRoadmapDialogOpen}
                lessons={pickerLessons}
                isSaving={isSavingRoadmap}
                isDeleting={isDeletingRoadmap}
                mode={editingRoadmap ? "edit" : "create"}
                initialRoadmap={editingRoadmap}
                onClose={() => {
                    if (isSavingRoadmap) {
                        return;
                    }

                    setEditingRoadmap(null);
                    setIsRoadmapDialogOpen(false);
                }}
                onSave={handleSaveRoadmap}
                onDelete={handleDeleteRoadmap}
                onValidationError={(message) => showToast(message, "error")}
            />

            <MaterialDetailsDialog
                key={selectedMaterial?.id || "material-details"}
                open={Boolean(selectedMaterial)}
                material={selectedMaterial}
                isDeleting={isDeletingMaterial}
                allowDelete={!selectedLesson}
                canEdit={canEditMaterials}
                canDelete={canDeleteMaterials && !selectedLesson}
                onClose={() => setSelectedMaterial(null)}
                onDelete={handleDeleteMaterial}
                onEdit={() => handleEditMaterial(selectedMaterial)}
            />

            {selectedLesson && (
                <Suspense fallback={<DialogLoadingFallback />}>
                    <LessonDetailsDialog
                        key={selectedLesson.id}
                        open={Boolean(selectedLesson)}
                        lesson={hydratedSelectedLesson}
                        onClose={() => setSelectedLesson(null)}
                        onOpenSourceMaterial={handleOpenSourceMaterial}
                        onLessonDeleted={handleLessonDeleted}
                        onLessonUpdated={handleLessonUpdated}
                        canPublish={canPublishArchiveLessons}
                        onValidationError={(message) => showToast(message, "error")}
                        canManageLesson={canManageLessonObject(currentUser, selectedLesson, canManageLessons)}
                        canManageActivities={canManageLessonObject(
                            currentUser,
                            selectedLesson,
                            canManageLessonActivities,
                        )}
                    />
                </Suspense>
            )}

            <LearningAssignmentDialog
                open={assignmentDialog.open}
                item={assignmentDialog.item}
                itemType={assignmentDialog.itemType}
                users={assignableUsersQuery.data ?? []}
                assignees={assignmentAssigneesQuery.data ?? []}
                isLoadingAssignees={assignmentAssigneesQuery.isLoading}
                searchQuery={assignmentUserSearchQuery}
                selectedUserIds={assignmentDialog.selectedUserIds}
                selectedRevokeUserIds={assignmentDialog.selectedRevokeUserIds}
                isLoading={assignableUsersQuery.isLoading}
                isSaving={isSavingAssignment}
                isRevoking={revokeAssignment.isPending}
                onClose={handleCloseAssignmentDialog}
                onSearchChange={setAssignmentUserSearchQuery}
                onToggleUser={handleToggleAssignmentUser}
                onToggleAll={handleToggleAllAssignmentUsers}
                onAssign={handleSubmitAssignment}
                onToggleRevokeUser={handleToggleRevokeUser}
                onToggleAllRevoke={handleToggleAllRevokeUsers}
                onRevokeSelected={() => void handleRevokeSelectedAssignments()}
            />

            <RoadmapGroupAssignmentDialog
                open={groupAssignmentDialog.open}
                roadmap={groupAssignmentDialog.roadmap}
                assignments={roadmapGroupAssignmentsQuery.data ?? []}
                isLoadingAssignments={roadmapGroupAssignmentsQuery.isLoading}
                pendingGroupId={groupAssignmentDialog.pendingGroupId}
                onClose={handleCloseGroupAssignmentDialog}
                onAssign={handleAssignRoadmapToGroup}
                onRevoke={handleRevokeRoadmapFromGroup}
            />

            <Toast
                open={toast.open}
                message={toast.message}
                severity={toast.severity}
                onClose={() => setToast((prev) => ({ ...prev, open: false }))}
            />
        </div>
    );
}
