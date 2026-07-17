import { useParams } from "react-router-dom";
import { ErrorAlert } from "@/shared/ui/ErrorAlert";
import { LoadingBlock } from "@/shared/ui/LoadingBlock";
import { useActivityPlayerQuery } from "../api/useActivityPlayerQuery";
import { FlashcardsActivityPlayer } from "./FlashcardsActivityPlayer";
import { QuizActivityPlayer } from "./QuizActivityPlayer";
import "./activity-player-page.css";

export function ActivityPlayerPage() {
    const { id = "", activityId = "" } = useParams();
    const { data, isLoading } = useActivityPlayerQuery(id, activityId);

    if (isLoading) {
        return <LoadingBlock label="Loading activity…" />;
    }

    if (!data) {
        return (
            <div className="activity-player-page">
                <ErrorAlert message="Activity not found or unavailable." />
            </div>
        );
    }

    const { lesson, activity, attempts, lessonActivities } = data;

    return (
        <div className="activity-player-page">
            {activity.type === "quiz" ? (
                <QuizActivityPlayer
                    lesson={lesson}
                    activity={activity}
                    initialAttempts={attempts}
                    lessonActivities={lessonActivities}
                />
            ) : (
                <FlashcardsActivityPlayer lesson={lesson} activity={activity} lessonActivities={lessonActivities} />
            )}
        </div>
    );
}
