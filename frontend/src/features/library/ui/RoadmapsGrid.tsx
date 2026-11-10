import { useEffect, type CSSProperties, type MouseEvent } from "react";
import { Link } from "react-router-dom";

import AccountTreeOutlinedIcon from "@mui/icons-material/AccountTreeOutlined";
import ArrowForwardOutlinedIcon from "@mui/icons-material/ArrowForwardOutlined";
import BoltOutlinedIcon from "@mui/icons-material/BoltOutlined";
import CheckOutlinedIcon from "@mui/icons-material/CheckOutlined";
import PersonAddAltOutlinedIcon from "@mui/icons-material/PersonAddAltOutlined";

import { getRoadmapPathActionLabel } from "@/features/lessons/lib/continuePath";
import { Button } from "@/shared/ui/Button";
import { UserAvatar } from "@/shared/ui/UserAvatar";
import type { LibraryRoadmap } from "../api/types";
import { ASSIGNEE_AVATAR_STACK_SIZE, AUTO_ASSIGN_BADGE_LABEL } from "../constants/roadmapAssignment";
import { ROADMAP_CARD_ACCENTS } from "../constants/roadmapCard";
import type { LibraryRoadmapsGridProps } from "../model/roadmapAssignment";
import { DuplicateMenuButton } from "./DuplicateMenuButton";
import { formatDate } from "./library-utils";

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

/**
 * Library roadmap cards. Assignment lives in one "Assign" action per card (people, teams, and
 * the auto-assign setting all sit in the same dialog); self-enrollment is not offered here,
 * since a manager assigns the roadmap rather than subscribing to it.
 */
export function RoadmapsGrid({
    roadmaps = [],
    onAssignRoadmap,
    onOpenRoadmap,
    canAssignLearning = false,
    canDuplicate = false,
    onDuplicateRoadmap,
    highlightedRoadmapId = null,
}: LibraryRoadmapsGridProps) {
    // Bring a just-duplicated roadmap into view once its card is rendered.
    useEffect(() => {
        if (highlightedRoadmapId == null) {
            return;
        }
        document
            .querySelector(`[data-library-roadmap-id="${highlightedRoadmapId}"]`)
            ?.scrollIntoView?.({ block: "center", behavior: "smooth" });
    }, [highlightedRoadmapId, roadmaps]);

    return (
        <div className="library-roadmaps">
            {roadmaps.map((roadmap, roadmapIndex) => {
                const progress = getRoadmapProgress(roadmap);
                const canOpenRoadmap = Boolean(onOpenRoadmap && roadmap.viewerCanManage);
                const canAssign = Boolean(canAssignLearning && onAssignRoadmap);
                const tags = Array.isArray(roadmap.tags) ? roadmap.tags : [];
                const accentColor = ROADMAP_CARD_ACCENTS[roadmapIndex % ROADMAP_CARD_ACCENTS.length];
                const primaryLesson =
                    progress.activeStep >= 0
                        ? roadmap.lessons?.[progress.activeStep]
                        : roadmap.lessons?.[(roadmap.lessons?.length || 1) - 1];
                const primaryLessonHref = primaryLesson ? `/lessons/${primaryLesson.id}` : "#";
                const showContinueAction = roadmap.isEnrolled && progress.totalCount > 0;
                const assigneeCount = roadmap.assigneeCount ?? null;
                const assigneePreview = roadmap.assigneePreview ?? [];
                const openAssign = (event: MouseEvent) => {
                    event.preventDefault();
                    event.stopPropagation();
                    onAssignRoadmap?.(roadmap);
                };

                return (
                    <article
                        key={roadmap.id}
                        className={[
                            "library-roadmap-card",
                            canOpenRoadmap ? "library-roadmap-card--clickable" : "",
                            roadmap.id === highlightedRoadmapId ? "library-roadmap-card--just-added" : "",
                        ]
                            .filter(Boolean)
                            .join(" ")}
                        data-library-roadmap-id={roadmap.id}
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
                                    {roadmap.autoAssignNewUsers &&
                                        (canAssign ? (
                                            <button
                                                type="button"
                                                className="library-chip library-chip--accent library-roadmap-card__auto-chip library-roadmap-card__auto-badge"
                                                title="Open assignment settings"
                                                onClick={openAssign}
                                            >
                                                <BoltOutlinedIcon /> {AUTO_ASSIGN_BADGE_LABEL}
                                            </button>
                                        ) : (
                                            <span className="library-chip library-chip--accent library-roadmap-card__auto-chip">
                                                <BoltOutlinedIcon /> {AUTO_ASSIGN_BADGE_LABEL}
                                            </span>
                                        ))}
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
                            <div className="library-roadmap-card__meta-row">
                                <span>
                                    {progress.totalCount} lesson{progress.totalCount === 1 ? "" : "s"}
                                </span>
                                {assigneeCount !== null && (
                                    <span className="library-roadmap-card__assignees">
                                        {assigneeCount > 0 ? (
                                            <>
                                                <span className="library-roadmap-card__avatars" aria-hidden="true">
                                                    {assigneePreview.slice(0, ASSIGNEE_AVATAR_STACK_SIZE).map((assignee) => (
                                                        <UserAvatar
                                                            key={assignee.userId}
                                                            user={{
                                                                name: assignee.name,
                                                                email: assignee.email,
                                                                avatarColor: assignee.avatarColor ?? undefined,
                                                                avatarStorageKey: assignee.avatarStorageKey ?? undefined,
                                                            }}
                                                            size={26}
                                                        />
                                                    ))}
                                                    {assigneeCount > ASSIGNEE_AVATAR_STACK_SIZE && (
                                                        <span className="library-roadmap-card__avatar-more">
                                                            +{assigneeCount - ASSIGNEE_AVATAR_STACK_SIZE}
                                                        </span>
                                                    )}
                                                </span>
                                                <span>{assigneeCount} assigned</span>
                                            </>
                                        ) : (
                                            <span>Not assigned yet</span>
                                        )}
                                    </span>
                                )}
                            </div>
                            <div className="library-roadmap-card__actions">
                                {showContinueAction && (
                                    <Link
                                        to={primaryLessonHref}
                                        className="ui-btn ui-btn--primary ui-btn--sm"
                                        onClick={(event) => event.stopPropagation()}
                                    >
                                        {getRoadmapPathActionLabel(progress.completedCount)}{" "}
                                        <ArrowForwardOutlinedIcon style={{ fontSize: 16 }} />
                                    </Link>
                                )}
                                {canAssign && (
                                    <Button
                                        size="sm"
                                        variant="primary"
                                        startIcon={<PersonAddAltOutlinedIcon style={{ fontSize: 16 }} />}
                                        onClick={openAssign}
                                    >
                                        Assign
                                    </Button>
                                )}
                                {canDuplicate && (
                                    <DuplicateMenuButton
                                        label={`More actions for ${roadmap.title}`}
                                        onDuplicate={() => onDuplicateRoadmap?.(roadmap)}
                                    />
                                )}
                            </div>
                        </footer>
                    </article>
                );
            })}
        </div>
    );
}
