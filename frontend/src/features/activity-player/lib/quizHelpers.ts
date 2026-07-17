import type { LessonActivityV1 } from "@/features/lessons/api/types";

export const PASSING_SCORE = 80;

export interface QuizQuestion {
    type?: string;
    question: string;
    options: string[];
    correctAnswer?: string;
    correctAnswers?: string[];
    explanation?: string;
}

export interface QuizResultItem {
    type?: string;
    selectedAnswers?: string[];
    correctAnswers?: string[];
    isCorrect?: boolean;
    explanation?: string;
}

export function getCorrectAnswers(question: {
    correctAnswer?: string;
    correctAnswers?: string[];
}): string[] {
    if (Array.isArray(question.correctAnswers) && question.correctAnswers.length > 0) {
        return question.correctAnswers.filter(Boolean);
    }
    return question.correctAnswer ? [question.correctAnswer] : [];
}

const QUIZ_QUESTION_TYPE_LABELS: Record<string, string> = {
    true_false: "True / False",
    fill_in_blanks_with_options: "Fill in the blank",
};

/** Returns a short question-type label for display, or an empty string for multiple_choice. */
export function getQuizQuestionTypeLabel(type?: string): string {
    return (type && QUIZ_QUESTION_TYPE_LABELS[type]) || "";
}

export function getQuestions(activity: LessonActivityV1): QuizQuestion[] {
    const payload = activity.payload as { items?: QuizQuestion[] } | undefined;
    return Array.isArray(payload?.items) ? payload.items : [];
}

export function getSavedResults(activity: LessonActivityV1): QuizResultItem[] | null {
    const metadata = activity.progress?.metadata as { results?: QuizResultItem[] } | undefined;
    return Array.isArray(metadata?.results) ? metadata.results : null;
}

export function shuffleItems<T>(items: T[]): T[] {
    const shuffledItems = [...items];
    for (let index = shuffledItems.length - 1; index > 0; index -= 1) {
        const randomIndex = Math.floor(Math.random() * (index + 1));
        [shuffledItems[index], shuffledItems[randomIndex]] = [
            shuffledItems[randomIndex],
            shuffledItems[index],
        ];
    }
    return shuffledItems;
}

export function buildAttemptQuestions(questions: QuizQuestion[]): QuizQuestion[] {
    return questions.map((question) => ({
        ...question,
        options: shuffleItems(Array.isArray(question.options) ? question.options : []),
    }));
}

export function buildInitialQuestions(questions: QuizQuestion[]): QuizQuestion[] {
    return questions.map((question) => ({
        ...question,
        options: Array.isArray(question.options) ? question.options : [],
    }));
}

export function isActivityComplete(activity: LessonActivityV1): boolean {
    if (activity.type === "quiz") {
        return Boolean(activity.progress?.completedAt) && Number(activity.progress?.score || 0) >= PASSING_SCORE;
    }
    return Boolean(activity.progress?.isCompleted);
}

export function getContinuePathHref(
    lessonId: number,
    activities: LessonActivityV1[],
    currentActivityId: number,
): string {
    const currentIndex = activities.findIndex((item) => item.id === currentActivityId);
    const followingActivities = currentIndex >= 0 ? activities.slice(currentIndex + 1) : activities;
    const nextIncompleteActivity = followingActivities.find((item) => !isActivityComplete(item));

    if (nextIncompleteActivity) {
        return `/lessons/${lessonId}/activities/${nextIncompleteActivity.id}`;
    }

    return `/lessons/${lessonId}`;
}

export function formatAttemptDate(value?: string): string {
    if (!value) {
        return "Just now";
    }
    return new Intl.DateTimeFormat("en-US", {
        dateStyle: "medium",
        timeStyle: "short",
    }).format(new Date(value));
}

export type OptionState = "idle" | "selected" | "correct" | "correct-missed" | "incorrect";

/**
 * A correct option the learner did NOT select (`correct-missed`) must render distinctly from a
 * correct option they DID select (`correct`) — otherwise a partially-correct multiple_choice
 * answer looks identical to a fully-correct one in the review screen.
 */
export function getOptionState({
    isSelected,
    isSubmitted,
    isCorrectOption,
    isWrongSelection,
}: {
    isSelected: boolean;
    isSubmitted: boolean;
    isCorrectOption: boolean;
    isWrongSelection: boolean;
}): OptionState {
    if (isSubmitted && isCorrectOption) {
        return isSelected ? "correct" : "correct-missed";
    }
    if (isSubmitted && isWrongSelection) {
        return "incorrect";
    }
    if (isSelected) {
        return "selected";
    }
    return "idle";
}
