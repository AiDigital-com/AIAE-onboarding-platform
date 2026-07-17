import { useCallback } from "react";
import { useParams } from "react-router-dom";
import { ErrorAlert } from "@/shared/ui/ErrorAlert";
import { LoadingBlock } from "@/shared/ui/LoadingBlock";
import { markdownToHtml } from "@/shared/lib/lessonContent";
import { useLessonReaderQuery } from "../api/useLessonReaderQuery";
import { useSetLessonCompletionMutation } from "../api/useSetLessonCompletionMutation";
import { LessonActivityGate } from "./LessonActivityGate";
import { LessonAskAssistant } from "./LessonAskAssistant";
import { LessonAttachments, normalizeLessonAsset } from "./LessonAttachments";
import { LessonReader } from "./LessonReader";
import { LessonReadingChrome } from "./LessonReadingChrome";
import "./lesson-reader-page.css";

export function LessonReaderPage() {
    const { id = "" } = useParams();
    const { data, isLoading } = useLessonReaderQuery(id);
    const completionMutation = useSetLessonCompletionMutation();
    const initialIsCompletedFlag = data?.initialIsCompleted ?? false;
    const lessonId = data?.lesson?.id ?? null;
    const hasActivities = Boolean(data?.activities?.length);

    // Must be defined before any conditional early return to satisfy the Rules of Hooks.
    const handleReadingCompleted = useCallback(() => {
        if (lessonId === null || hasActivities || initialIsCompletedFlag || completionMutation.isPending) {
            return;
        }
        void completionMutation.mutateAsync({ lessonId, completed: true }).catch(() => {
            // Keep UI usable even if completion persistence fails.
        });
    }, [lessonId, hasActivities, initialIsCompletedFlag, completionMutation]);

    if (isLoading) {
        return <LoadingBlock label="Loading lesson…" />;
    }

    if (!data?.lesson) {
        return (
            <div className="lesson-reader-page">
                <ErrorAlert message="Lesson not found or unavailable." />
            </div>
        );
    }

    const { lesson, activities, initialIsCompleted, roadmapContext, lessonNavigation, sourceReferences } =
        data;
    const teacherVideoRaw = lesson.generationMetadata?.teacherVideo as
        | { videoUrl?: string; thumbnailUrl?: string }
        | undefined;
    const html =
        lesson.contentHtml ||
        markdownToHtml(lesson.contentMarkdown || "");

    return (
        <div className="lesson-reader-page">
            <LessonReadingChrome
                lesson={{
                    id: lesson.id,
                    title: lesson.title,
                    description: lesson.description,
                    createdBy: lesson.createdBy,
                    createdAt: lesson.createdAt,
                    updatedAt: lesson.updatedAt,
                    publishedAt: lesson.publishedAt ?? undefined,
                    teacherVideo: teacherVideoRaw ?? null,
                }}
                roadmapContext={roadmapContext}
                lessonNavigation={lessonNavigation}
                initialIsCompleted={initialIsCompleted}
                onReadingCompleted={handleReadingCompleted}
            >
                <LessonReader html={html} />
                <LessonAttachments
                    attachments={(lesson.lessonAssets ?? []).map((asset) =>
                        normalizeLessonAsset(asset as unknown as Record<string, unknown>),
                    )}
                    sourceReferences={sourceReferences}
                />
                <LessonActivityGate
                    lessonId={lesson.id}
                    activities={activities}
                    initialIsCompleted={initialIsCompleted}
                />
            </LessonReadingChrome>
            <LessonAskAssistant lessonId={lesson.id} />
        </div>
    );
}
