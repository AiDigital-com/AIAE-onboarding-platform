import { useEffect, useRef, useState } from "react";
import CheckCircleOutlineOutlinedIcon from "@mui/icons-material/CheckCircleOutlineOutlined";
import ArrowDropDownOutlinedIcon from "@mui/icons-material/ArrowDropDownOutlined";
import OndemandVideoOutlinedIcon from "@mui/icons-material/OndemandVideoOutlined";
import PlaylistAddOutlinedIcon from "@mui/icons-material/PlaylistAddOutlined";
import QuizOutlinedIcon from "@mui/icons-material/QuizOutlined";
import StyleOutlinedIcon from "@mui/icons-material/StyleOutlined";
import { Button } from "@/shared/ui/Button";
import { getLessonCoverBackground } from "@/shared/lib/brandColors";
import { lessonPreviewText } from "@/shared/lib/lessonPreviewText";
import { useFilePreviewUrls } from "@/shared/api/files";
import type { LibraryLesson } from "../api/types";

/** Rough height of the 3-item enrollment menu, used to flip it upward when there's no room below. */
const ENROLLMENT_MENU_ESTIMATED_HEIGHT = 160;

const STATUS_PALETTE: Record<string, { fg: string }> = {
    ready: { fg: "rgb(34,158,90)" },
    draft: { fg: "#ff642d" },
    generating: { fg: "#ff642d" },
    failed: { fg: "#d92d20" },
    archived: { fg: "#80808e" },
    private: { fg: "#80808e" },
};

function getPublicationLabel(lesson: LibraryLesson) {
    if (lesson.isArchived || lesson.publicationStatus === "archived") {
        return "archived";
    }
    if (lesson.status !== "ready") {
        return lesson.status;
    }
    if (!lesson.isPublished) {
        return "draft";
    }
    return lesson.status;
}

function getEnrollmentActionLabel(lesson: LibraryLesson, enrolledLabel: string, defaultLabel: string) {
    if (lesson.isEnrolled) {
        return enrolledLabel;
    }
    if (lesson.isArchived) {
        return "Archived";
    }
    if (lesson.status === "draft") {
        return "Draft...";
    }
    if (lesson.status === "generating") {
        return "Generating...";
    }
    if (lesson.status === "failed") {
        return "Failed";
    }
    return lesson.isPublished ? defaultLabel : "Draft...";
}

function getActivityCounts(activities: NonNullable<LibraryLesson["activities"]>) {
    return activities.reduce(
        (counts, activity) => ({
            flashcards: counts.flashcards + (activity.type === "flashcards" ? 1 : 0),
            quizzes: counts.quizzes + (activity.type === "quiz" ? 1 : 0),
        }),
        { flashcards: 0, quizzes: 0 },
    );
}

function getLessonPreview(lesson: LibraryLesson) {
    return lessonPreviewText(lesson);
}

function LessonCover({ previewUrl }: { previewUrl?: string }) {
    return previewUrl ? <img src={previewUrl} alt="" /> : null;
}

interface LessonsGridProps {
    lessons?: LibraryLesson[];
    onOpenLesson?: (lesson: LibraryLesson) => void;
    onEnrollLesson?: (lesson: LibraryLesson) => void;
    onUnenrollLesson?: (lesson: LibraryLesson) => void;
    onAssignLesson?: (lesson: LibraryLesson) => void;
    showEnrollmentAction?: boolean;
    canAssignLearning?: boolean;
}

export function LessonsGrid({
    lessons = [],
    onOpenLesson,
    onEnrollLesson,
    onUnenrollLesson,
    onAssignLesson,
    showEnrollmentAction = false,
    canAssignLearning = false,
}: LessonsGridProps) {
    const enrollmentMenuRef = useRef<HTMLDivElement>(null);
    const [enrollmentMenu, setEnrollmentMenu] = useState<{
        lesson: LibraryLesson | null;
        anchor: DOMRect | null;
        openUpward: boolean;
    }>({
        lesson: null,
        anchor: null,
        openUpward: false,
    });
    const [expandedTagLessonIds, setExpandedTagLessonIds] = useState<Set<number>>(() => new Set());

    const coverStorageKeys = lessons
        .map((lesson) => lesson.coverImageStorageKey)
        .filter((key): key is string => Boolean(key));
    const { data: previewUrlByStorageKey } = useFilePreviewUrls(coverStorageKeys);

    const toggleExpandedTags = (event: React.MouseEvent, lessonId: number) => {
        event.preventDefault();
        event.stopPropagation();
        setExpandedTagLessonIds((prev) => {
            const next = new Set(prev);
            if (next.has(lessonId)) {
                next.delete(lessonId);
            } else {
                next.add(lessonId);
            }
            return next;
        });
    };

    useEffect(() => {
        if (!enrollmentMenu.lesson) {
            return undefined;
        }

        const handlePointerDown = (event: PointerEvent) => {
            const target = event.target;
            if (!(target instanceof Element)) {
                return;
            }
            if (enrollmentMenuRef.current?.contains(target)) {
                return;
            }
            if (target.closest("[data-library-lesson-enrollment-trigger]")) {
                return;
            }
            setEnrollmentMenu({ lesson: null, anchor: null, openUpward: false });
        };

        const handleKeyDown = (event: KeyboardEvent) => {
            if (event.key === "Escape") {
                setEnrollmentMenu({ lesson: null, anchor: null, openUpward: false });
            }
        };

        document.addEventListener("pointerdown", handlePointerDown);
        document.addEventListener("keydown", handleKeyDown);
        return () => {
            document.removeEventListener("pointerdown", handlePointerDown);
            document.removeEventListener("keydown", handleKeyDown);
        };
    }, [enrollmentMenu.lesson]);

    return (
        <>
            <div className="library-grid library-grid--lessons">
                {lessons.map((lesson) => {
                    const activities = Array.isArray(lesson.activities) ? lesson.activities : [];
                    const activityCounts = getActivityCounts(activities);
                    const tags = Array.isArray(lesson.tags) ? lesson.tags : [];
                    const publicationLabel = getPublicationLabel(lesson);
                    const statusPalette = STATUS_PALETTE[publicationLabel] || STATUS_PALETTE.private;
                    const hasCoverImage = Boolean(lesson.coverImageStorageKey);
                    const hasActivities = activityCounts.flashcards > 0 || activityCounts.quizzes > 0;
                    const hasTeacherVideo = Boolean(
                        (lesson.generationMetadata?.teacherVideo as { videoUrl?: string } | undefined)?.videoUrl,
                    );
                    const isEnrollmentDisabled =
                        lesson.status !== "ready" || !lesson.isPublished || Boolean(lesson.isArchived);
                    const isActionDisabled = canAssignLearning
                        ? lesson.status !== "ready" || Boolean(lesson.isArchived)
                        : isEnrollmentDisabled;
                    const areTagsExpanded = expandedTagLessonIds.has(lesson.id);
                    const visibleTags = areTagsExpanded ? tags : tags.slice(0, 2);
                    const hiddenTagCount = Math.max(tags.length - 2, 0);

                    return (
                        <article
                            key={lesson.id}
                            className="library-card"
                            onClick={() => onOpenLesson?.(lesson)}
                        >
                            <div className="library-card__preview-wrap">
                                <div
                                    className="library-card__preview"
                                    style={{
                                        background: hasCoverImage ? "#f5f5fe" : getLessonCoverBackground(lesson),
                                    }}
                                >
                                    {hasCoverImage && (
                                        <LessonCover
                                            previewUrl={previewUrlByStorageKey?.[lesson.coverImageStorageKey!]}
                                        />
                                    )}
                                </div>
                                <span
                                    className="library-status-chip library-card__status"
                                    style={{ color: statusPalette.fg }}
                                >
                                    {publicationLabel}
                                </span>
                            </div>

                            <h3 className="library-card__title">{lesson.title}</h3>
                            <p className="library-card__description">{getLessonPreview(lesson)}</p>

                            {tags.length > 0 && (
                                <div className="library-card__tags">
                                    {visibleTags.map((tag) => (
                                        <span key={tag} className="library-chip" title={tag}>
                                            <span className="library-chip__label">{tag}</span>
                                        </span>
                                    ))}
                                    {hiddenTagCount > 0 && !areTagsExpanded && (
                                        <button
                                            type="button"
                                            className="library-chip library-chip--muted"
                                            onClick={(event) => toggleExpandedTags(event, lesson.id)}
                                        >
                                            +{hiddenTagCount} more
                                        </button>
                                    )}
                                    {hiddenTagCount > 0 && areTagsExpanded && (
                                        <button
                                            type="button"
                                            className="library-chip library-chip--muted"
                                            onClick={(event) => toggleExpandedTags(event, lesson.id)}
                                        >
                                            Less
                                        </button>
                                    )}
                                </div>
                            )}

                            {hasActivities && (
                                <div className="library-card__meta">
                                    {activityCounts.flashcards > 0 && (
                                        <span className="library-meta-chip">
                                            <StyleOutlinedIcon />
                                            {activityCounts.flashcards}{" "}
                                            {activityCounts.flashcards === 1 ? "flashcard set" : "flashcard sets"}
                                        </span>
                                    )}
                                    {activityCounts.quizzes > 0 && (
                                        <span className="library-meta-chip library-meta-chip--success">
                                            <QuizOutlinedIcon />
                                            {activityCounts.quizzes}{" "}
                                            {activityCounts.quizzes === 1 ? "quiz" : "quizzes"}
                                        </span>
                                    )}
                                </div>
                            )}

                            {hasTeacherVideo && (
                                <div className="library-card__meta">
                                    <span className="library-meta-chip library-meta-chip--video">
                                        <OndemandVideoOutlinedIcon /> Teacher video
                                    </span>
                                </div>
                            )}

                            <footer className="library-card__footer">
                                <span className="library-card__footer-author">{lesson.createdBy || "AI Onboarding"}</span>
                                {showEnrollmentAction && (
                                    <Button
                                        className="library-card__enrollment-action"
                                        size="sm"
                                        variant={lesson.isEnrolled ? "ghost" : "primary"}
                                        disabled={isActionDisabled}
                                        data-library-lesson-enrollment-trigger
                                        onClick={(event) => {
                                            event.preventDefault();
                                            event.stopPropagation();
                                            if (canAssignLearning) {
                                                const anchor = (event.currentTarget as HTMLElement).getBoundingClientRect();
                                                const spaceBelow = window.innerHeight - anchor.bottom;
                                                const openUpward =
                                                    spaceBelow < ENROLLMENT_MENU_ESTIMATED_HEIGHT &&
                                                    anchor.top > spaceBelow;
                                                setEnrollmentMenu((current) => ({
                                                    lesson: current.lesson?.id === lesson.id ? null : lesson,
                                                    anchor: current.lesson?.id === lesson.id ? null : anchor,
                                                    openUpward,
                                                }));
                                                return;
                                            }
                                            setEnrollmentMenu({
                                                lesson: null,
                                                anchor: null,
                                                openUpward: false,
                                            });
                                            if (lesson.isEnrolled) {
                                                onUnenrollLesson?.(lesson);
                                                return;
                                            }
                                            onEnrollLesson?.(lesson);
                                        }}
                                    >
                                        {lesson.isEnrolled ? <CheckCircleOutlineOutlinedIcon /> : <PlaylistAddOutlinedIcon />}
                                        {canAssignLearning
                                            ? getEnrollmentActionLabel(lesson, "Added...", "Add...")
                                            : getEnrollmentActionLabel(lesson, "Remove from My Lessons", "Add to My Lessons")}
                                        {canAssignLearning && <ArrowDropDownOutlinedIcon />}
                                    </Button>
                                )}
                            </footer>
                        </article>
                    );
                })}
            </div>

            {enrollmentMenu.lesson && enrollmentMenu.anchor && (
                <div
                    ref={enrollmentMenuRef}
                    className="library-menu"
                    style={
                        enrollmentMenu.openUpward
                            ? {
                                  position: "fixed",
                                  bottom: window.innerHeight - enrollmentMenu.anchor.top + 4,
                                  left: enrollmentMenu.anchor.left,
                              }
                            : {
                                  position: "fixed",
                                  top: enrollmentMenu.anchor.bottom + 4,
                                  left: enrollmentMenu.anchor.left,
                              }
                    }
                >
                    <button
                        type="button"
                        className="library-menu__item"
                        onClick={() => {
                            const lesson = enrollmentMenu.lesson;
                            setEnrollmentMenu({ lesson: null, anchor: null, openUpward: false });
                            if (!lesson) return;
                            if (lesson.isEnrolled) {
                                onUnenrollLesson?.(lesson);
                                return;
                            }
                            onEnrollLesson?.(lesson);
                        }}
                    >
                        {enrollmentMenu.lesson.isEnrolled ? "Remove from My Lessons" : "Add to My Lessons"}
                    </button>
                    <button
                        type="button"
                        className="library-menu__item"
                        onClick={() => {
                            const lesson = enrollmentMenu.lesson;
                            setEnrollmentMenu({ lesson: null, anchor: null, openUpward: false });
                            onAssignLesson?.(lesson!);
                        }}
                    >
                        Assign to users...
                    </button>
                </div>
            )}
        </>
    );
}
