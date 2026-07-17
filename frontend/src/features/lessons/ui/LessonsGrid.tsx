import { Link } from "react-router-dom";
import OndemandVideoOutlinedIcon from "@mui/icons-material/OndemandVideoOutlined";
import QuizOutlinedIcon from "@mui/icons-material/QuizOutlined";
import RemoveCircleOutlineOutlinedIcon from "@mui/icons-material/RemoveCircleOutlineOutlined";
import StyleOutlinedIcon from "@mui/icons-material/StyleOutlined";
import { getLessonCoverBackground } from "@/shared/lib/brandColors";
import { lessonPreviewText } from "@/shared/lib/lessonPreviewText";
import { useFilePreviewUrls } from "@/shared/api/files";
import type { EnrolledLessonCard } from "../api/types";
import "./lessons-grid.css";

const CARD = {
    ink: "#0B0B0B",
    slate: "#33344A",
    mute: "#80808E",
    blue: "#0009DC",
    blue50: "#F5F5FE",
    blue100: "#E5E5FA",
    blue200: "#C7C7F0",
    bg3: "#F2F1F3",
    orange: "#FF642D",
    success: "rgb(34,158,90)",
};

const STATUS_PALETTE: Record<string, { fg: string; bg: string; dot: string }> = {
    ready: { fg: CARD.success, bg: "rgba(34,158,90,0.10)", dot: CARD.success },
    draft: { fg: CARD.orange, bg: "rgba(255,100,45,0.10)", dot: CARD.orange },
    generating: { fg: CARD.orange, bg: "rgba(255,100,45,0.10)", dot: CARD.orange },
    failed: { fg: "#D92D20", bg: "rgba(217,45,32,0.10)", dot: "#D92D20" },
    archived: { fg: CARD.mute, bg: "rgba(128,128,142,0.12)", dot: CARD.mute },
    private: { fg: CARD.mute, bg: "rgba(128,128,142,0.12)", dot: CARD.mute },
};

function getPublicationLabel(lesson: EnrolledLessonCard): string {
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

/**
 * Builds a plain-text preview from the bounded content preview fields the backend already
 * truncates to a short length — never the full lesson body.
 */
function getLessonPreview(lesson: EnrolledLessonCard): string {
    return lessonPreviewText(lesson);
}

function LessonCover({ previewUrl }: { previewUrl?: string }) {
    return previewUrl ? <img className="lessons-grid__cover-image" src={previewUrl} alt="" /> : null;
}

interface Props {
    lessons: EnrolledLessonCard[];
    onUnenrollLesson?: (lesson: EnrolledLessonCard) => void;
    showUnenrollAction?: boolean;
    showProgressStatus?: boolean;
}

export function LessonsGrid({
    lessons,
    onUnenrollLesson,
    showUnenrollAction = false,
    showProgressStatus = false,
}: Props) {
    const coverStorageKeys = lessons
        .map((lesson) => lesson.coverImageStorageKey)
        .filter((key): key is string => Boolean(key));
    const { data: previewUrlByStorageKey } = useFilePreviewUrls(coverStorageKeys);

    return (
        <div className="lessons-grid">
            {lessons.map((lesson) => {
                const flashcardCount = lesson.flashcardCount ?? 0;
                const quizCount = lesson.quizCount ?? 0;
                const tags = Array.isArray(lesson.tags) ? lesson.tags : [];
                const publicationLabel = getPublicationLabel(lesson);
                const statusPalette = STATUS_PALETTE[publicationLabel] || STATUS_PALETTE.private;
                const hasActivities = flashcardCount > 0 || quizCount > 0;
                const hasTeacherVideo = Boolean(lesson.hasTeacherVideo);
                const hasCoverImage = Boolean(lesson.coverImageStorageKey);
                const visibleTags = tags.slice(0, 2);
                const hiddenTagCount = Math.max(tags.length - 2, 0);

                return (
                    <Link key={lesson.id} className="lessons-grid__card" to={`/lessons/${lesson.id}`}>
                        <div
                            className="lessons-grid__cover"
                            style={{
                                background: hasCoverImage ? CARD.blue50 : getLessonCoverBackground(lesson),
                            }}
                        >
                            {hasCoverImage && (
                                <LessonCover previewUrl={previewUrlByStorageKey?.[lesson.coverImageStorageKey!]} />
                            )}
                            <span
                                className="lessons-grid__status"
                                style={{ color: statusPalette.fg, backgroundColor: "rgba(255,255,255,0.95)" }}
                            >
                                <span
                                    className="lessons-grid__status-dot"
                                    style={{ backgroundColor: statusPalette.dot }}
                                />
                                {publicationLabel}
                            </span>
                        </div>

                        <h3 className="lessons-grid__title">{lesson.title}</h3>

                        {showProgressStatus && (
                            <span
                                className={`lessons-grid__progress lessons-grid__progress--${
                                    lesson.isCompleted ? "done" : "pending"
                                }`}
                            >
                                {lesson.isCompleted ? "Completed" : "Not completed"}
                            </span>
                        )}

                        <p className="lessons-grid__preview">{getLessonPreview(lesson)}</p>

                        {tags.length > 0 && (
                            <div className="lessons-grid__tags">
                                {visibleTags.map((tag) => (
                                    <span key={tag} className="lessons-grid__tag" title={tag}>
                                        {tag}
                                    </span>
                                ))}
                                {hiddenTagCount > 0 && (
                                    <span className="lessons-grid__tag lessons-grid__tag--muted">
                                        +{hiddenTagCount} more
                                    </span>
                                )}
                            </div>
                        )}

                        {hasActivities && (
                            <div className="lessons-grid__activities">
                                {flashcardCount > 0 && (
                                    <span className="lessons-grid__activity lessons-grid__activity--flashcards">
                                        <StyleOutlinedIcon />
                                        {flashcardCount} {flashcardCount === 1 ? "flashcard set" : "flashcard sets"}
                                    </span>
                                )}
                                {quizCount > 0 && (
                                    <span className="lessons-grid__activity lessons-grid__activity--quiz">
                                        <QuizOutlinedIcon />
                                        {quizCount} {quizCount === 1 ? "quiz" : "quizzes"}
                                    </span>
                                )}
                            </div>
                        )}

                        {hasTeacherVideo && (
                            <span className="lessons-grid__activity lessons-grid__activity--video">
                                <OndemandVideoOutlinedIcon /> Teacher video
                            </span>
                        )}

                        <div className="lessons-grid__footer">
                            <span className="lessons-grid__author" title={lesson.createdBy || "AI Onboarding"}>
                                {lesson.createdBy || "AI Onboarding"}
                            </span>
                            {showUnenrollAction && (
                                <button
                                    type="button"
                                    className="lessons-grid__remove"
                                    onClick={(event) => {
                                        event.preventDefault();
                                        event.stopPropagation();
                                        onUnenrollLesson?.(lesson);
                                    }}
                                >
                                    <RemoveCircleOutlineOutlinedIcon /> Remove
                                </button>
                            )}
                        </div>
                    </Link>
                );
            })}
        </div>
    );
}
