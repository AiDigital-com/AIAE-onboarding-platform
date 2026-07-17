import { useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import ArrowBackOutlinedIcon from "@mui/icons-material/ArrowBackOutlined";
import CheckCircleIcon from "@mui/icons-material/CheckCircle";
import CheckCircleOutlineOutlinedIcon from "@mui/icons-material/CheckCircleOutlineOutlined";
import HighlightOffOutlinedIcon from "@mui/icons-material/HighlightOffOutlined";
import QuizOutlinedIcon from "@mui/icons-material/QuizOutlined";
import RestartAltOutlinedIcon from "@mui/icons-material/RestartAltOutlined";
import { ConfettiBurst } from "@/shared/ui/ConfettiBurst";
import { RoadmapCompletionCelebration } from "@/shared/ui/RoadmapCompletionCelebration";
import { Toast, type ToastSeverity } from "@/shared/ui/Toast";
import type {
    ActivityAttemptV1,
    CompletedRoadmapSummaryV1,
    LessonActivityV1,
    LessonV1,
} from "@/features/lessons/api/types";
import {
    useResetActivityProgressMutation,
    useSubmitActivityProgressMutation,
} from "../api/useActivityProgressMutations";
import {
    PASSING_SCORE,
    buildAttemptQuestions,
    buildInitialQuestions,
    formatAttemptDate,
    getContinuePathHref,
    getCorrectAnswers,
    getOptionState,
    getQuestions,
    getQuizQuestionTypeLabel,
    getSavedResults,
} from "../lib/quizHelpers";
import "./quiz-activity-player.css";

interface Props {
    lesson: LessonV1;
    activity: LessonActivityV1;
    initialAttempts?: ActivityAttemptV1[];
    lessonActivities?: LessonActivityV1[];
}

export function QuizActivityPlayer({
    lesson,
    activity,
    initialAttempts = [],
    lessonActivities = [],
}: Props) {
    const submitMutation = useSubmitActivityProgressMutation();
    const resetMutation = useResetActivityProgressMutation();
    const questions = useMemo(() => getQuestions(activity), [activity]);
    const savedResults = useMemo(() => getSavedResults(activity), [activity]);

    const [attemptQuestions, setAttemptQuestions] = useState(() => buildInitialQuestions(questions));
    const [answers, setAnswers] = useState(() =>
        savedResults
            ? savedResults.map((result) => result.selectedAnswers || [])
            : questions.map(() => [] as string[]),
    );
    const [results, setResults] = useState(savedResults);
    const [score, setScore] = useState<number | null>(activity.progress?.score ?? null);
    const [attempts, setAttempts] = useState(initialAttempts);
    const [activities, setActivities] = useState(lessonActivities);
    const [isConfettiActive, setIsConfettiActive] = useState(false);
    const [completedRoadmapsCelebration, setCompletedRoadmapsCelebration] = useState<
        CompletedRoadmapSummaryV1[]
    >([]);
    const [toast, setToast] = useState<{ open: boolean; message: string; severity: ToastSeverity }>({
        open: false,
        message: "",
        severity: "success",
    });

    const answeredCount = answers.filter((selected) => selected.length > 0).length;
    const progressValue = questions.length ? Math.round((answeredCount / questions.length) * 100) : 0;
    const isSubmitted = Array.isArray(results);
    const isPassed = isSubmitted && Number(score || 0) >= PASSING_SCORE;
    const canSubmit = questions.length > 0 && answeredCount === questions.length && !submitMutation.isPending;
    const continuePathHref = getContinuePathHref(lesson.id, activities, activity.id);
    const isSaving = submitMutation.isPending || resetMutation.isPending;

    useEffect(() => {
        setAttemptQuestions(buildAttemptQuestions(questions));
        setAnswers(
            savedResults
                ? savedResults.map((result) => result.selectedAnswers || [])
                : questions.map(() => [] as string[]),
        );
        setResults(savedResults);
        setScore(activity.progress?.score ?? null);
        setAttempts(initialAttempts);
        setActivities(lessonActivities);
    }, [activity.id, activity.progress?.score, initialAttempts, lessonActivities, questions, savedResults]);

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

    const handleAnswerChange = (questionIndex: number, option: string, questionType?: string) => {
        if (isSubmitted) {
            return;
        }
        setAnswers((current) =>
            current.map((selected, index) => {
                if (index !== questionIndex) {
                    return selected;
                }
                if (questionType === "multiple_choice") {
                    return selected.includes(option)
                        ? selected.filter((value) => value !== option)
                        : [...selected, option];
                }
                return [option];
            }),
        );
    };

    const handleSubmit = async () => {
        try {
            const data = await submitMutation.mutateAsync({
                lessonId: lesson.id,
                activityId: activity.id,
                answers,
            });

            const nextScore = data.attempt?.score ?? data.progress?.score ?? 0;
            const metadata = data.progress?.metadata as { results?: typeof results } | undefined;
            const nextResults =
                (data.attempt as { results?: typeof results } | null | undefined)?.results ||
                (metadata?.results ?? []);

            setScore(nextScore);
            setResults(nextResults);
            if (data.attempt?.id) {
                setAttempts((current) => [data.attempt!, ...current]);
            }
            if (Array.isArray(data.activities)) {
                setActivities(data.activities);
            }

            if (nextScore >= PASSING_SCORE) {
                setIsConfettiActive(false);
                window.setTimeout(() => setIsConfettiActive(true), 20);
            }

            if (data.completedRoadmaps?.length) {
                setCompletedRoadmapsCelebration(data.completedRoadmaps);
            }

            setToast({
                open: true,
                message:
                    nextScore >= PASSING_SCORE
                        ? data.lessonCompleted
                            ? "Quiz passed. Lesson marked as completed."
                            : "Quiz passed."
                        : "Quiz finished. Score at least 80% to pass.",
                severity: nextScore >= PASSING_SCORE ? "success" : "warning",
            });
        } catch (error) {
            console.error("Failed to submit quiz:", error);
            setToast({
                open: true,
                message: error instanceof Error ? error.message : "Failed to submit quiz.",
                severity: "error",
            });
        }
    };

    const handleRetry = async () => {
        try {
            await resetMutation.mutateAsync({ lessonId: lesson.id, activityId: activity.id });
            setAttemptQuestions(buildAttemptQuestions(questions));
            setAnswers(questions.map(() => [] as string[]));
            setResults(null);
            setScore(null);
        } catch (error) {
            console.error("Failed to reset quiz:", error);
            setToast({
                open: true,
                message: error instanceof Error ? error.message : "Failed to reset quiz.",
                severity: "error",
            });
        }
    };

    if (questions.length === 0) {
        return <div className="quiz-player__warning">This quiz activity has no questions.</div>;
    }

    return (
        <>
            <ConfettiBurst active={isConfettiActive} />
            <RoadmapCompletionCelebration
                active={completedRoadmapsCelebration.length > 0}
                roadmaps={completedRoadmapsCelebration}
            />

            <div className="quiz-player">
                <section className="quiz-player__hero">
                    <div className="quiz-player__hero-top">
                        <div>
                            <div className="quiz-player__chips">
                                <span className="quiz-player__chip quiz-player__chip--primary"><QuizOutlinedIcon /> Quiz</span>
                                <span className="quiz-player__chip">
                                    {questions.length} question{questions.length === 1 ? "" : "s"}
                                </span>
                                <span className="quiz-player__chip">{PASSING_SCORE}% to pass</span>
                                {isSubmitted && (
                                    <span
                                        className={`quiz-player__chip quiz-player__chip--${
                                            isPassed ? "success" : "warning"
                                        }`}
                                    >
                                        {isPassed ? "Passed" : "Not passed"} - {score}%
                                    </span>
                                )}
                            </div>
                            <h1 className="quiz-player__title">{activity.title || "Lesson quiz"}</h1>
                            <p className="quiz-player__subtitle">From lesson - {lesson.title}</p>
                        </div>
                        <Link className="quiz-player__back" to={`/lessons/${lesson.id}`}>
                            <ArrowBackOutlinedIcon /> Back to lesson
                        </Link>
                    </div>

                    <div className="quiz-player__progress-row">
                        <span className="quiz-player__score-count">
                            {isSubmitted
                                ? `${Math.round((Number(score || 0) / 100) * questions.length)}/${questions.length}`
                                : `${answeredCount}/${questions.length}`}
                        </span>
                        <div className="quiz-player__progress-copy">
                            <div className="quiz-player__progress-track">
                                <div
                                    className={`quiz-player__progress-bar quiz-player__progress-bar--${
                                        isSubmitted ? (isPassed ? "success" : "warning") : "active"
                                    }`}
                                    style={{ width: `${isSubmitted ? Number(score || 0) : progressValue}%` }}
                                />
                            </div>
                            <div className="quiz-player__progress-labels">
                                <span>
                                    {isSubmitted
                                        ? isPassed
                                            ? "Quiz passed"
                                            : "Try again to reach the passing score"
                                        : "Answer all questions to submit"}
                                </span>
                                <span>{PASSING_SCORE}% to pass</span>
                            </div>
                        </div>
                    </div>
                </section>

                <div className="quiz-player__questions">
                    {attemptQuestions.map((question, questionIndex) => {
                        const result = results?.[questionIndex] || null;
                        const selectedAnswers = result?.selectedAnswers || answers[questionIndex] || [];
                        const typeLabel = getQuizQuestionTypeLabel(result?.type || question.type);

                        return (
                            <article key={`${question.question}-${questionIndex}`} className="quiz-player__question">
                                <div className="quiz-player__question-head">
                                    <span className="quiz-player__question-index">{questionIndex + 1}</span>
                                    <h3>{question.question}</h3>
                                    {typeLabel && <span className="quiz-player__question-type">{typeLabel}</span>}
                                </div>

                                <div className="quiz-player__options">
                                    {(question.options || []).map((option) => {
                                        const isSelected = selectedAnswers.includes(option);
                                        const correctAnswers = getCorrectAnswers(result || question);
                                        const isCorrectOption = correctAnswers.includes(option);
                                        const isWrongSelection =
                                            isSubmitted && isSelected && !isCorrectOption;
                                        const optionState = getOptionState({
                                            isSelected,
                                            isSubmitted,
                                            isCorrectOption,
                                            isWrongSelection,
                                        });
                                        const isMissedCorrect = optionState === "correct-missed";

                                        return (
                                            <button
                                                key={option}
                                                type="button"
                                                className={`quiz-player__option quiz-player__option--${optionState}`}
                                                onClick={() => handleAnswerChange(questionIndex, option, question.type)}
                                                disabled={isSubmitted}
                                            >
                                                <span
                                                    className={`quiz-player__option-radio${
                                                        question.type === "multiple_choice"
                                                            ? " quiz-player__option-radio--check"
                                                            : ""
                                                    }`}
                                                />
                                                <span>{option}</span>
                                                {isMissedCorrect && (
                                                    <span className="quiz-player__option-missed-label">Correct answer</span>
                                                )}
                                                {optionState === "correct" && (
                                                    <CheckCircleIcon aria-hidden="true" />
                                                )}
                                                {isMissedCorrect && (
                                                    <CheckCircleOutlineOutlinedIcon aria-hidden="true" />
                                                )}
                                                {isWrongSelection && <HighlightOffOutlinedIcon aria-hidden="true" />}
                                            </button>
                                        );
                                    })}
                                </div>

                                {isSubmitted && (
                                    <div
                                        className={`quiz-player__feedback quiz-player__feedback--${
                                            result?.isCorrect ? "success" : "error"
                                        }`}
                                    >
                                        <strong>{result?.isCorrect ? "Correct" : "Incorrect"}</strong>
                                        <p>
                                            {result?.explanation ||
                                                "No explanation was provided for this question."}
                                        </p>
                                    </div>
                                )}
                            </article>
                        );
                    })}
                </div>

                {!isSubmitted && (
                    <div className="quiz-player__actions">
                        <button
                            type="button"
                            className="quiz-player__submit"
                            onClick={handleSubmit}
                            disabled={!canSubmit}
                        >
                            {isSaving
                                ? "Submitting..."
                                : canSubmit
                                  ? "Submit quiz"
                                  : "Answer all questions"}
                        </button>
                    </div>
                )}

                {isSubmitted && (
                    <div className="quiz-player__results-grid">
                        <div className="quiz-player__score-card">
                            <p className="quiz-player__score-label">Your score</p>
                            <p className="quiz-player__score-value">
                                {score ?? 0}
                                <span>%</span>
                            </p>
                            <p className="quiz-player__score-meta">
                                {results?.filter((result) => result.isCorrect).length || 0} of{" "}
                                {questions.length} correct - {isPassed ? "Passed" : "Not passed"}
                            </p>
                            <div className="quiz-player__score-actions">
                                {isPassed ? (
                                    <Link className="quiz-player__continue" to={continuePathHref}>
                                        Continue path
                                    </Link>
                                ) : (
                                    <button type="button" className="quiz-player__continue" disabled>
                                        Continue path
                                    </button>
                                )}
                                <button
                                    type="button"
                                    className="quiz-player__retry"
                                    onClick={handleRetry}
                                    disabled={isSaving}
                                >
                                    <RestartAltOutlinedIcon /> Try again
                                </button>
                            </div>
                        </div>

                        <div className="quiz-player__history">
                            <h3>Attempt history</h3>
                            <p>Every submitted attempt is saved.</p>
                            <div className="quiz-player__history-list">
                                {attempts.map((attempt) => (
                                    <div
                                        key={attempt.id}
                                        className={`quiz-player__attempt${
                                            attempt.passed ? " quiz-player__attempt--passed" : ""
                                        }`}
                                    >
                                        <div>
                                            <strong>Attempt {attempt.attemptNumber}</strong>
                                            <span>{formatAttemptDate(attempt.createdAt)}</span>
                                        </div>
                                        <span className="quiz-player__attempt-score">{attempt.score ?? 0}%</span>
                                        <span className="quiz-player__attempt-count">
                                            {attempt.correctCount}/{attempt.totalCount} correct
                                        </span>
                                    </div>
                                ))}
                                {attempts.length === 0 && (
                                    <div className="quiz-player__attempt">
                                        <span>This submitted attempt is being saved.</span>
                                    </div>
                                )}
                            </div>
                        </div>
                    </div>
                )}
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
