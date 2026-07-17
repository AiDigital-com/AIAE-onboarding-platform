import { Link } from "react-router-dom";
import AccountTreeOutlinedIcon from "@mui/icons-material/AccountTreeOutlined";
import ArrowForwardOutlinedIcon from "@mui/icons-material/ArrowForwardOutlined";
import CheckOutlinedIcon from "@mui/icons-material/CheckOutlined";
import { AI_DIGITAL_COLORS } from "@/shared/lib/brandColors";
import type { RoadmapView } from "../types";
import "./roadmaps.css";

const ACCENTS = [
    AI_DIGITAL_COLORS.yvesKleinBlue,
    AI_DIGITAL_COLORS.lime,
    AI_DIGITAL_COLORS.pink,
];

interface Progress {
    activeStep: number;
    completedCount: number;
    percent: number;
    totalCount: number;
}

function getRoadmapProgress(roadmap: RoadmapView): Progress {
    const completedCount = roadmap.lessons.filter((lesson) => lesson.isCompleted).length;
    const totalCount = roadmap.lessons.length;
    const firstIncompleteIndex = roadmap.lessons.findIndex((lesson) => !lesson.isCompleted);
    const activeStep = firstIncompleteIndex === -1 ? -1 : firstIncompleteIndex;

    return {
        activeStep,
        completedCount,
        percent: totalCount === 0 ? 0 : Math.round((completedCount / totalCount) * 100),
        totalCount,
    };
}

function formatDate(isoString: string) {
    try {
        return new Intl.DateTimeFormat("en", {
            year: "numeric",
            month: "short",
            day: "numeric",
        }).format(new Date(isoString));
    } catch {
        return "";
    }
}

function getPrimaryLessonHref(roadmap: RoadmapView, activeStep: number) {
    const targetLesson =
        activeStep >= 0
            ? roadmap.lessons[activeStep]
            : roadmap.lessons[roadmap.lessons.length - 1];

    return targetLesson ? `/lessons/${targetLesson.id}` : "#";
}

interface StepProps {
    lesson: RoadmapView["lessons"][number];
    index: number;
    state: "done" | "current" | "idle";
}

function RoadmapStep({ lesson, index, state }: StepProps) {
    const isDone = state === "done";
    const isCurrent = state === "current";

    return (
        <div className="roadmaps-grid__step">
            <div
                className={[
                    "roadmaps-grid__step-marker",
                    isDone ? "roadmaps-grid__step-marker--done" : "",
                    isCurrent ? "roadmaps-grid__step-marker--current" : "",
                ]
                    .filter(Boolean)
                    .join(" ")}
            >
                {isDone ? <CheckOutlinedIcon /> : index + 1}
            </div>
            <Link
                className={[
                    "roadmaps-grid__step-title",
                    isCurrent ? "roadmaps-grid__step-title--current" : "",
                    isDone ? "roadmaps-grid__step-title--done" : "",
                ]
                    .filter(Boolean)
                    .join(" ")}
                to={`/lessons/${lesson.id}`}
                onClick={(event) => event.stopPropagation()}
            >
                {lesson.title}
            </Link>
        </div>
    );
}

interface Props {
    roadmaps: RoadmapView[];
    onUnenrollRoadmap?: (roadmap: RoadmapView) => void;
}

/** Roadmap cards with progress track and actions. */
export function RoadmapsGrid({ roadmaps, onUnenrollRoadmap }: Props) {
    return (
        <div className="roadmaps-grid">
            {roadmaps.map((roadmap, roadmapIndex) => {
                const progress = getRoadmapProgress(roadmap);
                const tags = Array.isArray(roadmap.tags) ? roadmap.tags : [];
                const accentColor = ACCENTS[roadmapIndex % ACCENTS.length];
                const primaryLessonHref = getPrimaryLessonHref(roadmap, progress.activeStep);
                const isRoadmapCompleted = roadmap.isEnrolled && progress.percent === 100 && progress.totalCount > 0;
                const showContinueAction = roadmap.isEnrolled && progress.totalCount > 0 && !isRoadmapCompleted;
                const progressRatio =
                    progress.totalCount <= 1
                        ? progress.completedCount > 0
                            ? 1
                            : 0
                        : Math.min(progress.completedCount, progress.totalCount - 1) /
                          (progress.totalCount - 1);
                const trackInset =
                    progress.totalCount > 0
                        ? `${100 / (progress.totalCount * 2)}%`
                        : "50%";

                return (
                    <article
                        key={roadmap.id}
                        className="roadmaps-grid__card"
                        style={{ "--roadmap-accent": accentColor } as React.CSSProperties}
                    >
                        <div className="roadmaps-grid__header">
                            <div className="roadmaps-grid__intro">
                                <div className="roadmaps-grid__chips">
                                    <span className="roadmaps-grid__chip roadmaps-grid__chip--brand">
                                        <AccountTreeOutlinedIcon /> Roadmap
                                    </span>
                                    <span className="roadmaps-grid__chip">
                                        {progress.completedCount}/{progress.totalCount} completed
                                    </span>
                                    {roadmap.viewerCanManage && (
                                        <span className="roadmaps-grid__chip roadmaps-grid__chip--outline">
                                            Editable
                                        </span>
                                    )}
                                </div>
                                <h3 className="roadmaps-grid__title">{roadmap.title}</h3>
                                <p className="roadmaps-grid__description">
                                    {roadmap.description ||
                                        "A curated learning path built from existing lessons."}
                                </p>
                                {tags.length > 0 && (
                                    <div className="roadmaps-grid__tags">
                                        {tags.slice(0, 5).map((tag) => (
                                            <span key={tag} className="roadmaps-grid__tag" title={tag}>
                                                {tag}
                                            </span>
                                        ))}
                                        {tags.length > 5 && (
                                            <span className="roadmaps-grid__tag">
                                                +{tags.length - 5}
                                            </span>
                                        )}
                                    </div>
                                )}
                            </div>
                            <div className="roadmaps-grid__percent-block">
                                <p className="roadmaps-grid__percent">{progress.percent}%</p>
                                <p className="roadmaps-grid__meta">
                                    {roadmap.createdBy || "AI Onboarding"} - Created{" "}
                                    {formatDate(roadmap.createdAt)}
                                </p>
                            </div>
                        </div>

                        {progress.totalCount > 0 && (
                            <div className="roadmaps-grid__track-wrap">
                                <div
                                    className="roadmaps-grid__track"
                                    style={{
                                        minWidth: Math.max(520, roadmap.lessons.length * 150),
                                        paddingInline: trackInset,
                                    }}
                                >
                                    <div className="roadmaps-grid__track-line">
                                        <div
                                            className="roadmaps-grid__track-fill"
                                            style={{ width: `${progressRatio * 100}%` }}
                                        />
                                    </div>
                                    <div className="roadmaps-grid__steps">
                                        {roadmap.lessons.map((lesson, index) => (
                                            <RoadmapStep
                                                key={lesson.id}
                                                lesson={lesson}
                                                index={index}
                                                state={
                                                    lesson.isCompleted
                                                        ? "done"
                                                        : progress.activeStep === index
                                                          ? "current"
                                                          : "idle"
                                                }
                                            />
                                        ))}
                                    </div>
                                </div>
                            </div>
                        )}

                        <div className="roadmaps-grid__footer">
                            <p className="roadmaps-grid__lesson-count">
                                {progress.totalCount} lesson
                                {progress.totalCount === 1 ? "" : "s"}
                            </p>
                            <div className="roadmaps-grid__actions">
                                {isRoadmapCompleted && (
                                    <span className="roadmaps-grid__btn roadmaps-grid__btn--completed">
                                        <CheckOutlinedIcon /> Completed!
                                    </span>
                                )}
                                {showContinueAction && (
                                    <Link
                                        className="roadmaps-grid__btn roadmaps-grid__btn--primary"
                                        to={primaryLessonHref}
                                        onClick={(event) => event.stopPropagation()}
                                    >
                                        Continue path <ArrowForwardOutlinedIcon />
                                    </Link>
                                )}
                                {onUnenrollRoadmap && (
                                    <button
                                        type="button"
                                        className="roadmaps-grid__btn roadmaps-grid__btn--secondary"
                                        onClick={(event) => {
                                            event.preventDefault();
                                            event.stopPropagation();
                                            onUnenrollRoadmap(roadmap);
                                        }}
                                    >
                                        Remove path
                                    </button>
                                )}
                            </div>
                        </div>
                    </article>
                );
            })}
        </div>
    );
}
