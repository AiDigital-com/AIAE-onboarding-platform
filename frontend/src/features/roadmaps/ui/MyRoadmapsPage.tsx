import { useState } from "react";
import { FeatureEmptyState } from "@/shared/ui/FeatureEmptyState";
import { ErrorAlert } from "@/shared/ui/ErrorAlert";
import { LoadingBlock } from "@/shared/ui/LoadingBlock";
import { Toast, ToastSeverity } from "@/shared/ui/Toast";
import { getApiErrorMessage } from "@/shared/lib/apiError";
import { useMyRoadmapsQuery } from "../api/useRoadmapsQuery";
import { useUnenrollRoadmapMutation } from "../api/useUnenrollRoadmapMutation";
import type { RoadmapView } from "../types";
import { RoadmapsGrid } from "./RoadmapsGrid";
import "./roadmaps.css";

/** My Roadmaps page content — enrolled paths with unenroll support. */
export function MyRoadmapsPage() {
    const { data: roadmaps = [], isLoading, error } = useMyRoadmapsQuery();
    const unenrollMutation = useUnenrollRoadmapMutation();
    const [localRoadmaps, setLocalRoadmaps] = useState<RoadmapView[] | null>(null);
    const [toast, setToast] = useState<{ open: boolean; message: string; severity: ToastSeverity }>(
        {
            open: false,
            message: "",
            severity: "success",
        },
    );

    const visibleRoadmaps = localRoadmaps ?? roadmaps;

    const handleUnenrollRoadmap = async (roadmap: RoadmapView) => {
        setLocalRoadmaps((current) => (current ?? roadmaps).filter((item) => item.id !== roadmap.id));

        try {
            await unenrollMutation.mutateAsync(roadmap.id);
            setToast({
                open: true,
                message: "Roadmap removed from your learning plan.",
                severity: "success",
            });
            setLocalRoadmaps(null);
        } catch (unenrollError) {
            setLocalRoadmaps((current) =>
                [...(current ?? []), roadmap].sort((a, b) => {
                    const left = new Date(b.enrolledAt || b.createdAt).getTime();
                    const right = new Date(a.enrolledAt || a.createdAt).getTime();
                    return left - right;
                }),
            );
            setToast({
                open: true,
                message: getApiErrorMessage(unenrollError, "Failed to remove roadmap."),
                severity: "error",
            });
        }
    };

    if (isLoading) {
        return <LoadingBlock label="Loading roadmaps…" />;
    }

    if (error) {
        return <ErrorAlert message={getApiErrorMessage(error, "Failed to load roadmaps.")} />;
    }

    return (
        <div className="roadmaps-page">
            <div className="roadmaps-page__intro">
                <p className="roadmaps-page__eyebrow">Knowledge base</p>
                <h1 className="roadmaps-page__title">My Roadmaps</h1>
                <p className="roadmaps-page__subtitle">
                    Curated learning paths built from existing lessons.
                </p>
            </div>

            {visibleRoadmaps.length === 0 ? (
                <FeatureEmptyState
                    title="No roadmaps added yet"
                    description="Open the Roadmaps tab in Library and subscribe to a roadmap."
                />
            ) : (
                <RoadmapsGrid roadmaps={visibleRoadmaps} onUnenrollRoadmap={handleUnenrollRoadmap} />
            )}

            <Toast
                open={toast.open}
                message={toast.message}
                severity={toast.severity}
                onClose={() => setToast((prev) => ({ ...prev, open: false }))}
            />
        </div>
    );
}
