import { useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import ArrowBackOutlinedIcon from "@mui/icons-material/ArrowBackOutlined";
import ArrowForwardOutlinedIcon from "@mui/icons-material/ArrowForwardOutlined";
import CheckCircleOutlineOutlinedIcon from "@mui/icons-material/CheckCircleOutlineOutlined";
import RestartAltOutlinedIcon from "@mui/icons-material/RestartAltOutlined";
import StyleOutlinedIcon from "@mui/icons-material/StyleOutlined";
import VisibilityOutlinedIcon from "@mui/icons-material/VisibilityOutlined";
import { ConfettiBurst } from "@/shared/ui/ConfettiBurst";
import { RoadmapCompletionCelebration } from "@/shared/ui/RoadmapCompletionCelebration";
import { Toast, type ToastSeverity } from "@/shared/ui/Toast";
import type {
    CompletedRoadmapSummaryV1,
    LessonActivityV1,
    LessonV1,
} from "@/features/lessons/api/types";
import {
    useResetActivityProgressMutation,
    useSubmitActivityProgressMutation,
} from "../api/useActivityProgressMutations";
import { getContinuePathHref } from "../lib/quizHelpers";
import { getCards } from "../lib/flashcardsHelpers";
import { ScrollableCardText } from "./ScrollableCardText";
import "./flashcards-activity-player.css";

interface Props {
    lesson: LessonV1;
    activity: LessonActivityV1;
    lessonActivities?: LessonActivityV1[];
}

export function FlashcardsActivityPlayer({ lesson, activity, lessonActivities = [] }: Props) {
    const submitMutation = useSubmitActivityProgressMutation();
    const resetMutation = useResetActivityProgressMutation();
    const cards = useMemo(() => getCards(activity), [activity]);

    const [currentIndex, setCurrentIndex] = useState(0);
    const [isFlipped, setIsFlipped] = useState(false);
    const [revealedIndexes, setRevealedIndexes] = useState<Set<number>>(() =>
        activity.progress?.isCompleted ? new Set(cards.map((_, index) => index)) : new Set(),
    );
    const [isCompleted, setIsCompleted] = useState(Boolean(activity.progress?.isCompleted));
    const [isConfettiActive, setIsConfettiActive] = useState(false);
    const [completedRoadmapsCelebration, setCompletedRoadmapsCelebration] = useState<
        CompletedRoadmapSummaryV1[]
    >([]);
    const [toast, setToast] = useState<{ open: boolean; message: string; severity: ToastSeverity }>({
        open: false,
        message: "",
        severity: "success",
    });

    const currentCard = cards[currentIndex] || null;
    const reviewedCount = revealedIndexes.size;
    const progressValue = cards.length ? Math.round((reviewedCount / cards.length) * 100) : 0;
    const allCardsSeen = cards.length > 0 && reviewedCount === cards.length;
    const isSaving = submitMutation.isPending || resetMutation.isPending;
    const continuePathHref = getContinuePathHref(lesson.id, lessonActivities, activity.id);

    useEffect(() => {
        setIsFlipped(false);
    }, [currentIndex]);

    useEffect(() => {
        if (!isConfettiActive) {
            return undefined;
        }
        const timeoutId = window.setTimeout(() => setIsConfettiActive(false), 2600);
        return () => window.clearTimeout(timeoutId);
    }, [isConfettiActive]);

    useEffect(() => {
        if (completedRoadmapsCelebration.length === 0) {
            return undefined;
        }
        const timeoutId = window.setTimeout(() => setCompletedRoadmapsCelebration([]), 6200);
        return () => window.clearTimeout(timeoutId);
    }, [completedRoadmapsCelebration]);

    const goToCard = (nextIndex: number) => {
        if (nextIndex < 0 || nextIndex >= cards.length) {
            return;
        }
        setCurrentIndex(nextIndex);
    };

    const handleRestart = () => {
        setCurrentIndex(0);
        setIsFlipped(false);
        setRevealedIndexes(new Set());
    };

    const handleFlip = () => {
        setIsFlipped((prev) => {
            const nextIsFlipped = !prev;
            if (nextIsFlipped) {
                setRevealedIndexes((current) => new Set([...current, currentIndex]));
            }
            return nextIsFlipped;
        });
    };

    const handleComplete = async () => {
        try {
            const data = await submitMutation.mutateAsync({
                lessonId: lesson.id,
                activityId: activity.id,
                reviewedCards: reviewedCount,
            });

            setIsConfettiActive(false);
            window.setTimeout(() => setIsConfettiActive(true), 20);
            setIsCompleted(true);

            if (data.completedRoadmaps?.length) {
                setCompletedRoadmapsCelebration(data.completedRoadmaps);
            }

            setToast({
                open: true,
                message: data.lessonCompleted
                    ? "Activity complete. Lesson marked as completed."
                    : "Activity complete.",
                severity: "success",
            });
        } catch (error) {
            console.error("Failed to complete flashcards:", error);
            setToast({
                open: true,
                message: error instanceof Error ? error.message : "Failed to complete activity.",
                severity: "error",
            });
        }
    };

    const handleMarkIncomplete = async () => {
        try {
            await resetMutation.mutateAsync({ lessonId: lesson.id, activityId: activity.id });
            setIsCompleted(false);
            setCurrentIndex(0);
            setIsFlipped(false);
            setRevealedIndexes(new Set());
            setToast({
                open: true,
                message: "Activity marked as incomplete.",
                severity: "success",
            });
        } catch (error) {
            console.error("Failed to reset flashcards:", error);
            setToast({
                open: true,
                message: error instanceof Error ? error.message : "Failed to reset activity.",
                severity: "error",
            });
        }
    };

    if (cards.length === 0) {
        return <div className="flashcards-player__warning">This flashcard activity has no cards.</div>;
    }

    return (
        <>
            <ConfettiBurst active={isConfettiActive} />
            <RoadmapCompletionCelebration
                active={completedRoadmapsCelebration.length > 0}
                roadmaps={completedRoadmapsCelebration}
            />

            <div className="flashcards-player">
                <section className="flashcards-player__hero">
                    <div className="flashcards-player__hero-top">
                        <div>
                            <div className="flashcards-player__chips">
                                <span className="flashcards-player__chip flashcards-player__chip--primary">
                                    <StyleOutlinedIcon /> Flashcards
                                </span>
                                <span className="flashcards-player__chip">
                                    {reviewedCount} of {cards.length} revealed
                                </span>
                                {isCompleted && (
                                    <span className="flashcards-player__chip flashcards-player__chip--success">
                                        <CheckCircleOutlineOutlinedIcon /> Completed
                                    </span>
                                )}
                            </div>
                            <h1 className="flashcards-player__title">{activity.title || "Flashcards"}</h1>
                            <p className="flashcards-player__subtitle">From lesson - {lesson.title}</p>
                        </div>
                        <Link className="flashcards-player__back" to={`/lessons/${lesson.id}`}>
                            <ArrowBackOutlinedIcon /> Back to lesson
                        </Link>
                    </div>
                    <div className="flashcards-player__progress-track">
                        <div className="flashcards-player__progress-bar" style={{ width: `${progressValue}%` }} />
                    </div>
                    <p className="flashcards-player__progress-label">{progressValue}% revealed</p>
                </section>

                <div className="flashcards-player__stage">
                    <div className={`flashcards-player__card${isFlipped ? " is-flipped" : ""}`}>
                        {[
                            { side: "front", label: "Prompt", content: currentCard.front, helper: "Click to reveal the answer" },
                            { side: "back", label: "Answer", content: currentCard.back, helper: "Click to hide answer" },
                        ].map((side) => (
                            <button
                                key={side.side}
                                type="button"
                                className={`flashcards-player__face flashcards-player__face--${side.side}`}
                                onClick={handleFlip}
                            >
                                <span
                                    className={`flashcards-player__face-chip flashcards-player__face-chip--${side.side}`}
                                >
                                    {side.label}
                                </span>
                                <ScrollableCardText>
                                    <p className={`flashcards-player__face-text flashcards-player__face-text--${side.side}`}>
                                        {side.content}
                                    </p>
                                </ScrollableCardText>
                                <span className="flashcards-player__face-helper"><VisibilityOutlinedIcon /> {side.helper}</span>
                            </button>
                        ))}
                    </div>
                </div>

                <div className="flashcards-player__controls">
                    <div className="flashcards-player__pager">
                        <button type="button" onClick={() => goToCard(currentIndex - 1)} disabled={currentIndex === 0}>
                            <ArrowBackOutlinedIcon />
                        </button>
                        <span>
                            {currentIndex + 1} <small>/ {cards.length}</small>
                        </span>
                        <button
                            type="button"
                            onClick={() => goToCard(currentIndex + 1)}
                            disabled={currentIndex === cards.length - 1}
                        >
                            <ArrowForwardOutlinedIcon />
                        </button>
                    </div>

                    <div className="flashcards-player__actions">
                        <button type="button" className="flashcards-player__restart" onClick={handleRestart}>
                            <RestartAltOutlinedIcon /> Restart
                        </button>
                        <button
                            type="button"
                            className={`flashcards-player__complete${
                                isCompleted ? " flashcards-player__complete--done" : ""
                            }`}
                            onClick={isCompleted ? handleMarkIncomplete : handleComplete}
                            disabled={(!isCompleted && !allCardsSeen) || isSaving}
                        >
                            {isSaving
                                ? "Saving..."
                                : isCompleted
                                  ? "Mark as incomplete"
                                  : allCardsSeen
                                    ? "Mark complete"
                                    : "Reveal all cards first"}
                        </button>
                        {isCompleted && (
                            <Link className="flashcards-player__continue" to={continuePathHref}>
                                Continue path
                            </Link>
                        )}
                    </div>
                </div>
            </div>

            <Toast
                open={toast.open}
                message={toast.message}
                severity={toast.severity}
                onClose={() => setToast((prev) => ({ ...prev, open: false }))}
            />
        </>
    );
}
