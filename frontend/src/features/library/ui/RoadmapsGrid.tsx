import { type CSSProperties, useEffect, useRef, useState } from "react";
import { Link } from "react-router-dom";
import AccountTreeOutlinedIcon from "@mui/icons-material/AccountTreeOutlined";
import ArrowDropDownOutlinedIcon from "@mui/icons-material/ArrowDropDownOutlined";
import ArrowForwardOutlinedIcon from "@mui/icons-material/ArrowForwardOutlined";
import CheckOutlinedIcon from "@mui/icons-material/CheckOutlined";
import PlaylistAddOutlinedIcon from "@mui/icons-material/PlaylistAddOutlined";
import { Button } from "@/shared/ui/Button";
import { AI_DIGITAL_COLORS } from "@/shared/lib/brandColors";
import type { LibraryRoadmap } from "../api/types";
import { formatDate } from "./library-utils";

const ACCENTS = [AI_DIGITAL_COLORS.yvesKleinBlue, AI_DIGITAL_COLORS.lime, AI_DIGITAL_COLORS.pink];
/** Rough height of the 3-item enrollment menu, used to flip it upward when there's no room below. */
const ENROLLMENT_MENU_ESTIMATED_HEIGHT = 160;

function getRoadmapProgress(roadmap: LibraryRoadmap) {
    const lessons = roadmap.lessons || [];
    const completedCount = lessons.filter((lesson) => lesson.isCompleted).length;
    const totalCount = lessons.length;
    const firstIncompleteIndex = lessons.findIndex((lesson) => !lesson.isCompleted);
    const activeStep = firstIncompleteIndex === -1 ? -1 : firstIncompleteIndex;

    return {
        activeStep,
        completedCount,
        percent: totalCount === 0 ? 0 : Math.round((completedCount / totalCount) * 100),
        totalCount,
    };
}

interface RoadmapsGridProps {
    roadmaps?: LibraryRoadmap[];
    onEnrollRoadmap?: (roadmap: LibraryRoadmap) => void;
    onUnenrollRoadmap?: (roadmap: LibraryRoadmap) => void;
    onAssignRoadmap?: (roadmap: LibraryRoadmap) => void;
    onAssignRoadmapToTeam?: (roadmap: LibraryRoadmap) => void;
    onOpenRoadmap?: (roadmap: LibraryRoadmap) => void;
    canAssignLearning?: boolean;
}

export function RoadmapsGrid({
    roadmaps = [],
    onEnrollRoadmap,
    onUnenrollRoadmap,
    onAssignRoadmap,
    onAssignRoadmapToTeam,
    onOpenRoadmap,
    canAssignLearning = false,
}: RoadmapsGridProps) {
    const enrollmentMenuRef = useRef<HTMLDivElement>(null);
    const [enrollmentMenu, setEnrollmentMenu] = useState<{
        roadmap: LibraryRoadmap | null;
        anchor: DOMRect | null;
        openUpward: boolean;
    }>({ roadmap: null, anchor: null, openUpward: false });

    useEffect(() => {
        if (!enrollmentMenu.roadmap) {
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
            if (target.closest("[data-library-roadmap-enrollment-trigger]")) {
                return;
            }
            setEnrollmentMenu({ roadmap: null, anchor: null, openUpward: false });
        };

        const handleKeyDown = (event: KeyboardEvent) => {
            if (event.key === "Escape") {
                setEnrollmentMenu({ roadmap: null, anchor: null, openUpward: false });
            }
        };

        document.addEventListener("pointerdown", handlePointerDown);
        document.addEventListener("keydown", handleKeyDown);
        return () => {
            document.removeEventListener("pointerdown", handlePointerDown);
            document.removeEventListener("keydown", handleKeyDown);
        };
    }, [enrollmentMenu.roadmap]);

    return (
        <>
            <div className="library-roadmaps">
                {roadmaps.map((roadmap, roadmapIndex) => {
                    const progress = getRoadmapProgress(roadmap);
                    const canOpenRoadmap = Boolean(onOpenRoadmap && roadmap.viewerCanManage);
                    const tags = Array.isArray(roadmap.tags) ? roadmap.tags : [];
                    const accentColor = ACCENTS[roadmapIndex % ACCENTS.length];
                    const primaryLesson =
                        progress.activeStep >= 0
                            ? roadmap.lessons?.[progress.activeStep]
                            : roadmap.lessons?.[(roadmap.lessons?.length || 1) - 1];
                    const primaryLessonHref = primaryLesson ? `/lessons/${primaryLesson.id}` : "#";
                    const showContinueAction = roadmap.isEnrolled && progress.totalCount > 0;

                    return (
                        <article
                            key={roadmap.id}
                            className={[
                                "library-roadmap-card",
                                canOpenRoadmap ? "library-roadmap-card--clickable" : "",
                            ]
                                .filter(Boolean)
                                .join(" ")}
                            style={{ "--roadmap-accent": accentColor } as CSSProperties}
                            onClick={canOpenRoadmap ? () => onOpenRoadmap?.(roadmap) : undefined}
                        >
                            <div className="library-roadmap-card__head">
                                <div className="library-roadmap-card__info">
                                    <div className="library-card__tags">
                                        <span className="library-meta-chip">
                                            <AccountTreeOutlinedIcon /> Roadmap
                                        </span>
                                        <span className="library-chip">
                                            {progress.completedCount}/{progress.totalCount} completed
                                        </span>
                                        {roadmap.viewerCanManage && (
                                            <span className="library-chip">Editable</span>
                                        )}
                                    </div>
                                    <h3 className="library-roadmap-card__title">{roadmap.title}</h3>
                                    <p className="library-roadmap-card__description">
                                        {roadmap.description || "A curated learning path built from existing lessons."}
                                    </p>
                                    {tags.length > 0 && (
                                        <div className="library-card__tags" style={{ marginTop: 12 }}>
                                            {tags.slice(0, 5).map((tag) => (
                                                <span key={tag} className="library-chip" title={tag}>
                                                    <span className="library-chip__label">{tag}</span>
                                                </span>
                                            ))}
                                            {tags.length > 5 && (
                                                <span className="library-chip">+{tags.length - 5}</span>
                                            )}
                                        </div>
                                    )}
                                </div>
                                <div className="library-roadmap-card__stats">
                                    <p className="library-roadmap-card__percent">{progress.percent}%</p>
                                    <p className="library-roadmap-card__meta">
                                        {roadmap.createdBy || "AI Onboarding"} — Created{" "}
                                        {formatDate(roadmap.createdAt)}
                                    </p>
                                </div>
                            </div>

                            {progress.totalCount > 0 && (
                                <div className="library-roadmap-steps">
                                    <div
                                        className="library-roadmap-steps__inner"
                                        style={{
                                            minWidth: Math.max(520, progress.totalCount * 150),
                                        }}
                                    >
                                        <div
                                            className="library-roadmap-steps__track"
                                            style={{
                                                left: `${100 / (progress.totalCount * 2)}%`,
                                                right: `${100 / (progress.totalCount * 2)}%`,
                                            }}
                                        >
                                            <div
                                                className="library-roadmap-steps__track-fill"
                                                style={{
                                                    width: `${
                                                        progress.totalCount <= 1
                                                            ? progress.completedCount > 0 ? 100 : 0
                                                            : (Math.min(progress.completedCount, progress.totalCount - 1) /
                                                              (progress.totalCount - 1)) * 100
                                                    }%`,
                                                }}
                                            />
                                        </div>
                                        {roadmap.lessons?.map((lesson, index) => {
                                            // Use per-lesson completion, not position order — reordering
                                            // lessons must not move the checkmark to a different lesson.
                                            const stepState = lesson.isCompleted
                                                ? "done"
                                                : progress.activeStep === index
                                                  ? "current"
                                                  : "idle";
                                            return (
                                                <div key={lesson.id} className="library-roadmap-step">
                                                    <div
                                                        className={`library-roadmap-step__circle library-roadmap-step__circle--${stepState}`}
                                                    >
                                                        {stepState === "done" ? (
                                                            <CheckOutlinedIcon style={{ fontSize: 17 }} />
                                                        ) : (
                                                            index + 1
                                                        )}
                                                    </div>
                                                    <Link
                                                        to={`/lessons/${lesson.id}`}
                                                        className={`library-roadmap-step__label library-roadmap-step__label--${stepState}`}
                                                        onClick={(event) => event.stopPropagation()}
                                                    >
                                                        {lesson.title}
                                                    </Link>
                                                </div>
                                            );
                                        })}
                                    </div>
                                </div>
                            )}

                            <footer className="library-card__footer">
                                <span>
                                    {progress.totalCount} lesson{progress.totalCount === 1 ? "" : "s"}
                                </span>
                                <div style={{ display: "flex", gap: 8 }}>
                                    {showContinueAction && (
                                        <Link
                                            to={primaryLessonHref}
                                            className="ui-btn ui-btn--primary ui-btn--sm"
                                            onClick={(event) => event.stopPropagation()}
                                        >
                                            Continue path <ArrowForwardOutlinedIcon style={{ fontSize: 16 }} />
                                        </Link>
                                    )}
                                    {(onEnrollRoadmap || onUnenrollRoadmap) && (
                                        <Button
                                            size="sm"
                                            variant={roadmap.isEnrolled ? "ghost" : "primary"}
                                            startIcon={!roadmap.isEnrolled ? <PlaylistAddOutlinedIcon style={{ fontSize: 16 }} /> : undefined}
                                            endIcon={canAssignLearning ? <ArrowDropDownOutlinedIcon style={{ fontSize: 16 }} /> : undefined}
                                            data-library-roadmap-enrollment-trigger
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
                                                        roadmap: current.roadmap?.id === roadmap.id ? null : roadmap,
                                                        anchor: current.roadmap?.id === roadmap.id ? null : anchor,
                                                        openUpward,
                                                    }));
                                                    return;
                                                }
                                                setEnrollmentMenu({ roadmap: null, anchor: null, openUpward: false });
                                                if (roadmap.isEnrolled) {
                                                    onUnenrollRoadmap?.(roadmap);
                                                    return;
                                                }
                                                onEnrollRoadmap?.(roadmap);
                                            }}
                                        >
                                            {canAssignLearning
                                                ? roadmap.isEnrolled
                                                    ? "Added..."
                                                    : "Add..."
                                                : roadmap.isEnrolled
                                                  ? "Remove path"
                                                  : "Subscribe"}
                                        </Button>
                                    )}
                                </div>
                            </footer>
                        </article>
                    );
                })}
            </div>

            {enrollmentMenu.roadmap && enrollmentMenu.anchor && (
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
                            const roadmap = enrollmentMenu.roadmap;
                            setEnrollmentMenu({ roadmap: null, anchor: null, openUpward: false });
                            if (!roadmap) return;
                            if (roadmap.isEnrolled) {
                                onUnenrollRoadmap?.(roadmap);
                                return;
                            }
                            onEnrollRoadmap?.(roadmap);
                        }}
                    >
                        {enrollmentMenu.roadmap.isEnrolled ? "Remove from My Roadmaps" : "Add to My Roadmaps"}
                    </button>
                    <button
                        type="button"
                        className="library-menu__item"
                        onClick={() => {
                            const roadmap = enrollmentMenu.roadmap;
                            setEnrollmentMenu({ roadmap: null, anchor: null, openUpward: false });
                            onAssignRoadmap?.(roadmap!);
                        }}
                    >
                        Assign to users...
                    </button>
                    <button
                        type="button"
                        className="library-menu__item"
                        onClick={() => {
                            const roadmap = enrollmentMenu.roadmap;
                            setEnrollmentMenu({ roadmap: null, anchor: null, openUpward: false });
                            onAssignRoadmapToTeam?.(roadmap!);
                        }}
                    >
                        Assign to team...
                    </button>
                </div>
            )}
        </>
    );
}
