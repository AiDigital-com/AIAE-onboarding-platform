import { useEffect, useMemo, useState } from "react";
import { Button } from "@/shared/ui/Button";
import { LessonEmptyState } from "@/shared/ui/LessonEmptyState";
import { LoadingBlock } from "@/shared/ui/LoadingBlock";
import { Toast, type ToastSeverity } from "@/shared/ui/Toast";
import { type EnrolledLessonCard } from "../api/types";
import { useMyLessonsQuery } from "../api/useMyLessonsQuery";
import { useUnenrollLessonMutation } from "../api/useUnenrollLessonMutation";
import { LessonsGrid } from "./LessonsGrid";
import { RemoveCompletedLessonDialog } from "./RemoveCompletedLessonDialog";
import "./my-lessons.css";

export function MyLessonsPanel() {
    const myLessonsQuery = useMyLessonsQuery();
    const initialLessons = useMemo(
        () => myLessonsQuery.data?.pages.flatMap((page) => page.items) ?? [],
        [myLessonsQuery.data],
    );
    const unenrollMutation = useUnenrollLessonMutation();
    const [lessons, setLessons] = useState<EnrolledLessonCard[]>(initialLessons);
    const [lessonPendingRemoval, setLessonPendingRemoval] = useState<EnrolledLessonCard | null>(null);
    const [toast, setToast] = useState<{ open: boolean; message: string; severity: ToastSeverity }>({
        open: false,
        message: "",
        severity: "success",
    });

    useEffect(() => {
        setLessons(initialLessons);
    }, [initialLessons]);

    const { completedLessons, incompleteLessons } = useMemo(() => {
        return lessons.reduce(
            (groups, lesson) => {
                if (lesson.isCompleted) {
                    groups.completedLessons.push(lesson);
                } else {
                    groups.incompleteLessons.push(lesson);
                }
                return groups;
            },
            {
                completedLessons: [] as EnrolledLessonCard[],
                incompleteLessons: [] as EnrolledLessonCard[],
            },
        );
    }, [lessons]);

    const removeLesson = async (lesson: EnrolledLessonCard) => {
        setLessons((prev) => prev.filter((item) => item.id !== lesson.id));

        try {
            await unenrollMutation.mutateAsync(lesson.id);
            setToast({
                open: true,
                message: "Lesson removed from My Lessons.",
                severity: "success",
            });
        } catch (error) {
            console.error("Failed to remove lesson from My Lessons:", error);
            setLessons((prev) =>
                [...prev, lesson].sort((a, b) => {
                    const left = new Date(b.enrolledAt || b.createdAt).getTime();
                    const right = new Date(a.enrolledAt || a.createdAt).getTime();
                    return left - right;
                }),
            );
            setToast({
                open: true,
                message: error instanceof Error ? error.message : "Failed to remove lesson from My Lessons.",
                severity: "error",
            });
        }
    };

    const handleUnenrollLesson = async (lesson: EnrolledLessonCard) => {
        if (lesson.isCompleted) {
            setLessonPendingRemoval(lesson);
            return;
        }

        await removeLesson(lesson);
    };

    if (myLessonsQuery.isLoading) {
        return <LoadingBlock label="Loading lessons" />;
    }

    return (
        <>
            {lessons.length === 0 ? (
                <LessonEmptyState
                    title="No lessons added yet"
                    description="Open the Lessons tab in Library and click Add to My Lessons on any ready lesson."
                />
            ) : (
                <div className="my-lessons__sections">
                    <section className="my-lessons__section">
                        <div className="my-lessons__section-header">
                            <div>
                                <h2 className="my-lessons__section-title">Not completed</h2>
                                <p className="my-lessons__section-description">
                                    Continue lessons that are still in progress.
                                </p>
                            </div>
                            <span className="my-lessons__section-count">
                                {incompleteLessons.length} lesson{incompleteLessons.length === 1 ? "" : "s"}
                            </span>
                        </div>

                        {incompleteLessons.length === 0 ? (
                            <LessonEmptyState
                                title="All lessons completed"
                                description="Completed lessons are collected below."
                            />
                        ) : (
                            <LessonsGrid
                                lessons={incompleteLessons}
                                showUnenrollAction
                                showProgressStatus
                                onUnenrollLesson={handleUnenrollLesson}
                            />
                        )}
                    </section>

                    <section className="my-lessons__section">
                        <div className="my-lessons__section-header">
                            <div>
                                <h2 className="my-lessons__section-title">Completed</h2>
                                <p className="my-lessons__section-description">
                                    Finished lessons stay here for review.
                                </p>
                            </div>
                            <span className="my-lessons__section-count">
                                {completedLessons.length} lesson{completedLessons.length === 1 ? "" : "s"}
                            </span>
                        </div>

                        {completedLessons.length === 0 ? (
                            <LessonEmptyState
                                title="No completed lessons yet"
                                description="Open a lesson and click Complete at the end when you finish reading."
                            />
                        ) : (
                            <LessonsGrid
                                lessons={completedLessons}
                                showUnenrollAction
                                showProgressStatus
                                onUnenrollLesson={handleUnenrollLesson}
                            />
                        )}
                    </section>
                </div>
            )}

            {myLessonsQuery.hasNextPage && (
                <div className="my-lessons__load-more">
                    <Button
                        variant="secondary"
                        onClick={() => void myLessonsQuery.fetchNextPage()}
                        disabled={myLessonsQuery.isFetchingNextPage}
                    >
                        {myLessonsQuery.isFetchingNextPage ? "Loading…" : "Load more"}
                    </Button>
                </div>
            )}

            <Toast
                open={toast.open}
                message={toast.message}
                severity={toast.severity}
                onClose={() => setToast((prev) => ({ ...prev, open: false }))}
            />

            <RemoveCompletedLessonDialog
                lesson={lessonPendingRemoval}
                onCancel={() => setLessonPendingRemoval(null)}
                onConfirm={async () => {
                    const lesson = lessonPendingRemoval;
                    if (!lesson) {
                        return;
                    }
                    setLessonPendingRemoval(null);
                    await removeLesson(lesson);
                }}
            />
        </>
    );
}
