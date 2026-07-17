import { Link } from "react-router-dom";
import QuizOutlinedIcon from "@mui/icons-material/QuizOutlined";
import StyleOutlinedIcon from "@mui/icons-material/StyleOutlined";
import type { LessonActivityV1 } from "@/features/lessons/api/types";
import { LessonCompletionButton } from "./LessonCompletionButton";
import "./lesson-activity-gate.css";

const PASSING_SCORE = 80;

function getActivityLabel(activity: LessonActivityV1): string {
    if (activity.type === "quiz") {
        return `${activity.itemCount} question quiz`;
    }
    if (activity.type === "flashcards") {
        return `${activity.itemCount} flashcards`;
    }
    return "Unsupported activity";
}

function isActivityPassed(activity: LessonActivityV1): boolean {
    if (activity.type === "quiz") {
        return Boolean(activity.progress?.isCompleted) && Number(activity.progress?.score || 0) >= PASSING_SCORE;
    }
    return Boolean(activity.progress?.isCompleted);
}

function getActivitySortWeight(activity: LessonActivityV1): number {
    if (activity.type === "flashcards") {
        return 0;
    }
    if (activity.type === "quiz") {
        return 1;
    }
    return 2;
}

function getActivityStatus(activity: LessonActivityV1): { label: string; tone: "success" | "warning" | "default" } {
    if (activity.type === "quiz" && activity.progress) {
        const score = Number(activity.progress?.score || 0);
        if (activity.progress.status === "failed" || score < PASSING_SCORE) {
            return {
                label: `Quiz not passed: not enough points (${score}%)`,
                tone: "warning",
            };
        }
        return {
            label: `Quiz passed: ${score}%`,
            tone: "success",
        };
    }

    if (isActivityPassed(activity)) {
        return { label: "Completed", tone: "success" };
    }

    return { label: "Not completed", tone: "default" };
}

interface Props {
    lessonId: number;
    activities?: LessonActivityV1[];
    initialIsCompleted?: boolean;
}

export function LessonActivityGate({
    lessonId,
    activities = [],
    initialIsCompleted = false,
}: Props) {
    if (activities.length === 0) {
        return (
            <LessonCompletionButton lessonId={lessonId} initialIsCompleted={initialIsCompleted} />
        );
    }

    const supportedActivities = activities.filter(
        (activity) => activity.type === "flashcards" || activity.type === "quiz",
    );
    const sortedActivities = [...activities].sort(
        (firstActivity, secondActivity) =>
            getActivitySortWeight(firstActivity) - getActivitySortWeight(secondActivity),
    );
    const completedCount = activities.filter(isActivityPassed).length;
    const allCompleted = completedCount === activities.length;

    return (
        <section className="lesson-activity-gate">
            <div className="lesson-activity-gate__intro">
                <div className="lesson-activity-gate__badges">
                    <span
                        className={`lesson-activity-gate__badge${
                            allCompleted ? " lesson-activity-gate__badge--success" : ""
                        }`}
                    >
                        {allCompleted ? "Activities completed" : "Practice activities"}
                    </span>
                    <span className="lesson-activity-gate__badge lesson-activity-gate__badge--outline">
                        {completedCount}/{activities.length} complete
                    </span>
                </div>
                <h2 className="lesson-activity-gate__title">Lesson activities</h2>
                <p className="lesson-activity-gate__description">
                    Open activities in any order. Flashcards are shown first, then quizzes.
                </p>
            </div>

            <div className="lesson-activity-gate__list">
                {sortedActivities.map((activity) => {
                    const isActivitySupported = supportedActivities.includes(activity);
                    const activityStatus = getActivityStatus(activity);

                    return (
                        <article key={activity.id} className="lesson-activity-gate__item">
                            <div className="lesson-activity-gate__item-main">
                                <div
                                    className={`lesson-activity-gate__icon lesson-activity-gate__icon--${activity.type}`}
                                >
                                    {activity.type === "quiz" ? <QuizOutlinedIcon /> : <StyleOutlinedIcon />}
                                </div>
                                <div className="lesson-activity-gate__item-copy">
                                    <h3>{activity.title || "Practice activity"}</h3>
                                    <div className="lesson-activity-gate__item-badges">
                                        <span className="lesson-activity-gate__chip">{getActivityLabel(activity)}</span>
                                        <span
                                            className={`lesson-activity-gate__chip lesson-activity-gate__chip--${activityStatus.tone}`}
                                        >
                                            {activityStatus.label}
                                        </span>
                                    </div>
                                </div>
                            </div>

                            {isActivitySupported ? (
                                <Link
                                    className={`lesson-activity-gate__open${
                                        isActivityPassed(activity) ? " lesson-activity-gate__open--review" : ""
                                    }`}
                                    to={`/lessons/${lessonId}/activities/${activity.id}`}
                                >
                                    {isActivityPassed(activity) ? "Review" : "Open"}
                                </Link>
                            ) : (
                                <button type="button" className="lesson-activity-gate__open" disabled>
                                    Unavailable
                                </button>
                            )}
                        </article>
                    );
                })}
            </div>

            {allCompleted && (
                <LessonCompletionButton lessonId={lessonId} initialIsCompleted={initialIsCompleted} />
            )}
        </section>
    );
}
