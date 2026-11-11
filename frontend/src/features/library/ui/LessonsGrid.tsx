import { useState } from "react";
import CheckCircleOutlineOutlinedIcon from "@mui/icons-material/CheckCircleOutlineOutlined";
import LockOutlinedIcon from "@mui/icons-material/LockOutlined";
import OndemandVideoOutlinedIcon from "@mui/icons-material/OndemandVideoOutlined";
import PersonAddAltOutlinedIcon from "@mui/icons-material/PersonAddAltOutlined";
import PlaylistAddOutlinedIcon from "@mui/icons-material/PlaylistAddOutlined";
import PublicOutlinedIcon from "@mui/icons-material/PublicOutlined";
import QuizOutlinedIcon from "@mui/icons-material/QuizOutlined";
import StyleOutlinedIcon from "@mui/icons-material/StyleOutlined";
import UnarchiveOutlinedIcon from "@mui/icons-material/UnarchiveOutlined";
import { Button } from "@/shared/ui/Button";
import { getLessonCoverBackground } from "@/shared/lib/brandColors";
import { lessonPreviewText } from "@/shared/lib/lessonPreviewText";
import { useFilePreviewUrls } from "@/shared/api/files";
import type { LibraryLesson } from "../api/types";
import type { CardActionId, CardActionItem } from "../model/cardActions";
import { CardActionsButton, CardActionsMenu } from "./CardActionsMenu";
import { useCardActionsMenu } from "./useCardActionsMenu";

const STATUS_PALETTE: Record<string, { fg: string }> = {
    ready: { fg: "rgb(34,158,90)" },
    generating: { fg: "#ff642d" },
    failed: { fg: "#d92d20" },
    archived: { fg: "#80808e" },
};

/**
 * Returns the single status shown on the cover: 'archived', 'failed', 'generating' or 'ready'.
 * There is no "draft" status: the backend's short-lived pre-generation state reads as
 * 'generating', and an unpublished ready lesson is simply Ready + Private. Visibility
 * (public/private) is a separate axis, shown by {@link getVisibilityLabel} instead.
 */
function getPublicationLabel(lesson: LibraryLesson) {
    if (lesson.isArchived || lesson.publicationStatus === "archived") {
        return "archived";
    }
    if (lesson.status === "draft") {
        return "generating";
    }
    return lesson.status;
}

/**
 * Returns the accessible label for the visibility icon shown beside the status chip of a ready
 * or archived lesson, or {@code null} while the lesson has no content yet (generating, failed).
 */
function getVisibilityLabel(lesson: LibraryLesson, publicationLabel: string) {
    if (publicationLabel !== "ready" && publicationLabel !== "archived") {
        return null;
    }
    return lesson.isPublished
        ? "Public — visible to everyone in the Library"
        : "Private — visible in the Library only to assigned users";
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
    onAssignLesson?: (lesson: LibraryLesson) => void;
    /** Restores an archived lesson; the Restore button shows only when this returns true. */
    canRestoreLesson?: (lesson: LibraryLesson) => boolean;
    onRestoreLesson?: (lesson: LibraryLesson) => void;
    showEnrollmentAction?: boolean;
    canAssignLearning?: boolean;
    /** Status chip on the cover. Off for learners, who only ever see ready lessons. */
    showStatus?: boolean;
    /** Items of a lesson's ⋯ menu; an empty list hides the button for that card. */
    getLessonActions?: (lesson: LibraryLesson) => CardActionItem[];
    onLessonAction?: (lesson: LibraryLesson, actionId: CardActionId) => void;
}

export function LessonsGrid({
    lessons = [],
    onOpenLesson,
    onEnrollLesson,
    onAssignLesson,
    canRestoreLesson,
    onRestoreLesson,
    showEnrollmentAction = false,
    canAssignLearning = false,
    showStatus = true,
    getLessonActions,
    onLessonAction,
}: LessonsGridProps) {
    const actionsMenu = useCardActionsMenu();
    const actionsMenuLesson = actionsMenu.anchor
        ? lessons.find((lesson) => lesson.id === actionsMenu.anchor?.itemId)
        : undefined;
    const actionsMenuItems = actionsMenuLesson ? (getLessonActions?.(actionsMenuLesson) ?? []) : [];
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

    return (
        <>
            <div className="library-grid library-grid--lessons">
                {lessons.map((lesson) => {
                    const activities = Array.isArray(lesson.activities) ? lesson.activities : [];
                    const activityCounts = getActivityCounts(activities);
                    const tags = Array.isArray(lesson.tags) ? lesson.tags : [];
                    const publicationLabel = getPublicationLabel(lesson);
                    const statusPalette = STATUS_PALETTE[publicationLabel] || STATUS_PALETTE.archived;
                    const visibilityLabel = getVisibilityLabel(lesson, publicationLabel);
                    const hasCoverImage = Boolean(lesson.coverImageStorageKey);
                    const hasActivities = activityCounts.flashcards > 0 || activityCounts.quizzes > 0;
                    const hasTeacherVideo = Boolean(
                        (lesson.generationMetadata?.teacherVideo as { videoUrl?: string } | undefined)?.videoUrl,
                    );
                    const isReady = publicationLabel === "ready";
                    const canAssign = canAssignLearning && isReady;
                    const canRestore =
                        publicationLabel === "archived" && Boolean(canRestoreLesson?.(lesson) && onRestoreLesson);
                    // Learners add public lessons themselves; assigned ones are already theirs.
                    const canEnroll = !canAssignLearning && isReady && Boolean(lesson.isPublished);
                    const areTagsExpanded = expandedTagLessonIds.has(lesson.id);
                    const visibleTags = areTagsExpanded ? tags : tags.slice(0, 2);
                    const hiddenTagCount = Math.max(tags.length - 2, 0);
                    const hasCardActions = (getLessonActions?.(lesson) ?? []).length > 0;

                    return (
                        <article
                            key={lesson.id}
                            className="library-card"
                            onClick={() => onOpenLesson?.(lesson)}
                            onContextMenu={
                                hasCardActions
                                    ? (event) => actionsMenu.openFromContextMenu(event, lesson.id)
                                    : undefined
                            }
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
                                <div className="library-card__badges">
                                    {visibilityLabel && (
                                        <span
                                            className="library-card__visibility"
                                            title={visibilityLabel}
                                            aria-label={visibilityLabel}
                                        >
                                            {lesson.isPublished ? <PublicOutlinedIcon /> : <LockOutlinedIcon />}
                                        </span>
                                    )}
                                    {showStatus && (
                                        <span
                                            className="library-status-chip library-card__status"
                                            style={{ color: statusPalette.fg }}
                                        >
                                            {publicationLabel}
                                        </span>
                                    )}
                                </div>
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
                                {showEnrollmentAction && canAssign && (
                                    <Button
                                        size="sm"
                                        variant="primary"
                                        startIcon={<PersonAddAltOutlinedIcon />}
                                        onClick={(event) => {
                                            event.stopPropagation();
                                            actionsMenu.close();
                                            onAssignLesson?.(lesson);
                                        }}
                                    >
                                        Assign
                                    </Button>
                                )}
                                {showEnrollmentAction && canRestore && (
                                    <Button
                                        size="sm"
                                        variant="secondary"
                                        startIcon={<UnarchiveOutlinedIcon />}
                                        onClick={(event) => {
                                            event.stopPropagation();
                                            actionsMenu.close();
                                            onRestoreLesson?.(lesson);
                                        }}
                                    >
                                        Restore
                                    </Button>
                                )}
                                {showEnrollmentAction && !canAssignLearning && lesson.isEnrolled && (
                                    <span className="library-card__enrollment-status">
                                        <CheckCircleOutlineOutlinedIcon className="library-card__enrollment-status-icon" /> In My Lessons
                                    </span>
                                )}
                                {showEnrollmentAction && canEnroll && !lesson.isEnrolled && (
                                    <Button
                                        className="library-card__enrollment-action"
                                        size="sm"
                                        variant="primary"
                                        startIcon={<PlaylistAddOutlinedIcon />}
                                        onClick={(event) => {
                                            event.stopPropagation();
                                            onEnrollLesson?.(lesson);
                                        }}
                                    >
                                        Add to My Lessons
                                    </Button>
                                )}
                                {hasCardActions && (
                                    <CardActionsButton
                                        label={`More actions for ${lesson.title}`}
                                        expanded={actionsMenu.anchor?.itemId === lesson.id}
                                        onClick={(event) => actionsMenu.toggleFromButton(event, lesson.id)}
                                    />
                                )}
                            </footer>
                        </article>
                    );
                })}
            </div>

            {actionsMenu.anchor && actionsMenuLesson && actionsMenuItems.length > 0 && (
                <CardActionsMenu
                    anchor={actionsMenu.anchor}
                    items={actionsMenuItems}
                    label={`Actions for ${actionsMenuLesson.title}`}
                    onSelect={(actionId) => onLessonAction?.(actionsMenuLesson, actionId)}
                    onClose={actionsMenu.close}
                />
            )}
        </>
    );
}
