import { useInfiniteQuery } from "@tanstack/react-query";
import { apiClient } from "@/shared/api/client";
import { getApiErrorMessage } from "@/shared/api/apiError";
import type { EnrolledLessonCard } from "./types";
import type { MyLessonSummaryV1, PageInfoV1 } from "./types";
import { lessonsKeys } from "./queryKeys";

const PAGE_SIZE = 20;

function emptyPageInfo(requestedPage: number): PageInfoV1 {
    return {
        page: requestedPage,
        size: PAGE_SIZE,
        totalElements: 0,
        totalPages: 0,
        hasNext: false,
        hasPrevious: false,
    };
}

function toEnrolledLessonCard(item: MyLessonSummaryV1): EnrolledLessonCard {
    return {
        id: item.id,
        title: item.title,
        description: item.description ?? "",
        status: item.status,
        publicationStatus: item.publicationStatus,
        isPublished: item.publicationStatus === "published",
        isArchived: item.publicationStatus === "archived",
        contentHtmlPreview: item.contentHtmlPreview ?? "",
        contentMarkdownPreview: item.contentMarkdownPreview ?? "",
        createdBy: item.createdBy ?? "AI Onboarding",
        createdAt: item.createdAt ?? "",
        updatedAt: item.updatedAt ?? "",
        enrolledAt: item.enrollment.enrolledAt,
        isCompleted: item.enrollment.isCompleted,
        isEnrolled: true,
        tags: item.tags ?? [],
        flashcardCount: item.activityCounts.flashcardCount,
        quizCount: item.activityCounts.quizCount,
        hasTeacherVideo: item.hasTeacherVideo,
        coverImageStorageKey: item.coverImageStorageKey ?? "",
    };
}

/**
 * Loads the current user's enrolled lessons for My Lessons grids, page-by-page — incomplete
 * lessons first, then newest-enrolled-first. Call fetchNextPage() to load more.
 */
export function useMyLessonsQuery() {
    return useInfiniteQuery({
        queryKey: lessonsKeys.my(),
        initialPageParam: 0,
        getNextPageParam: (lastPage: { items: EnrolledLessonCard[]; page: PageInfoV1 }) =>
            lastPage.page.hasNext ? lastPage.page.page + 1 : undefined,
        queryFn: async ({ pageParam }: { pageParam: number }) => {
            const { data, error } = await apiClient.GET("/api/v1/learning/my-lessons", {
                params: { query: { page: pageParam, size: PAGE_SIZE } },
            });

            if (error) {
                throw new Error(getApiErrorMessage(error, "Failed to load my lessons."));
            }

            return {
                items: (data?.lessons ?? []).map(toEnrolledLessonCard),
                page: data?.page ?? emptyPageInfo(pageParam),
            };
        },
        retry: false,
    });
}
