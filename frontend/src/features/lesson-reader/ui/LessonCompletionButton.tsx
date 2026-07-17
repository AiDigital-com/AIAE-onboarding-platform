import { useEffect, useState } from "react";
import CheckCircleOutlineOutlinedIcon from "@mui/icons-material/CheckCircleOutlineOutlined";
import RadioButtonUncheckedOutlinedIcon from "@mui/icons-material/RadioButtonUncheckedOutlined";
import { ConfettiBurst } from "@/shared/ui/ConfettiBurst";
import { RoadmapCompletionCelebration } from "@/shared/ui/RoadmapCompletionCelebration";
import { Toast, type ToastSeverity } from "@/shared/ui/Toast";
import type { CompletedRoadmapSummaryV1 } from "@/features/lessons/api/types";
import { useSetLessonCompletionMutation } from "../api/useSetLessonCompletionMutation";
import "./lesson-completion-button.css";

interface Props {
    lessonId: number;
    initialIsCompleted?: boolean;
}

export function LessonCompletionButton({ lessonId, initialIsCompleted = false }: Props) {
    const completionMutation = useSetLessonCompletionMutation();
    const [isCompleted, setIsCompleted] = useState(initialIsCompleted);
    const [toast, setToast] = useState<{ open: boolean; message: string; severity: ToastSeverity }>({
        open: false,
        message: "",
        severity: "success",
    });
    const [isConfettiActive, setIsConfettiActive] = useState(false);
    const [completedRoadmapsCelebration, setCompletedRoadmapsCelebration] = useState<
        CompletedRoadmapSummaryV1[]
    >([]);

    useEffect(() => {
        setIsCompleted(initialIsCompleted);
    }, [initialIsCompleted]);

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

    const handleToggleCompletion = async () => {
        const nextIsCompleted = !isCompleted;
        setIsCompleted(nextIsCompleted);

        try {
            const data = await completionMutation.mutateAsync({
                lessonId,
                completed: nextIsCompleted,
            });

            const enrollment = data.enrollment;
            setIsCompleted(
                enrollment && "isCompleted" in enrollment ? Boolean(enrollment.isCompleted) : nextIsCompleted,
            );
            const completedRoadmaps = data.completedRoadmaps || [];

            if (nextIsCompleted) {
                setIsConfettiActive(false);
                window.setTimeout(() => setIsConfettiActive(true), 20);
            }

            if (completedRoadmaps.length > 0) {
                setToast((prev) => ({ ...prev, open: false }));
                setCompletedRoadmapsCelebration(completedRoadmaps);
            } else {
                setToast({
                    open: true,
                    message: nextIsCompleted
                        ? "Lesson marked as completed."
                        : "Lesson marked as not completed.",
                    severity: "success",
                });
            }
        } catch (error) {
            console.error("Failed to update lesson progress:", error);
            setIsCompleted(!nextIsCompleted);
            setToast({
                open: true,
                message: error instanceof Error ? error.message : "Failed to update lesson progress.",
                severity: "error",
            });
        }
    };

    return (
        <>
            <ConfettiBurst active={isConfettiActive} />
            <RoadmapCompletionCelebration
                active={completedRoadmapsCelebration.length > 0}
                roadmaps={completedRoadmapsCelebration}
            />

            <div className="lesson-completion-button">
                <button
                    type="button"
                    className={`lesson-completion-button__btn${
                        isCompleted ? " lesson-completion-button__btn--done" : ""
                    }`}
                    disabled={completionMutation.isPending}
                    onClick={handleToggleCompletion}
                >
                    {isCompleted ? <CheckCircleOutlineOutlinedIcon /> : <RadioButtonUncheckedOutlinedIcon />}
                    {completionMutation.isPending
                        ? "Saving..."
                        : isCompleted
                          ? "Completed"
                          : "Complete"}
                </button>
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
