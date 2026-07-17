import { useEffect, useMemo, useRef, useState } from "react";
import type { ReactNode } from "react";
import {
    Alert,
    Autocomplete,
    Box,
    Button,
    Checkbox,
    Chip,
    CircularProgress,
    Dialog,
    DialogActions,
    DialogContent,
    FormControl,
    InputLabel,
    MenuItem,
    IconButton,
    Paper,
    Radio,
    Select,
    Stack,
    Tab,
    Tabs,
    TextField,
    Tooltip,
    Typography,
} from "@mui/material";
import CloseOutlinedIcon from "@mui/icons-material/CloseOutlined";
import AddOutlinedIcon from "@mui/icons-material/AddOutlined";
import ArchiveOutlinedIcon from "@mui/icons-material/ArchiveOutlined";
import AttachFileOutlinedIcon from "@mui/icons-material/AttachFileOutlined";
import ErrorOutlineOutlinedIcon from "@mui/icons-material/ErrorOutlineOutlined";
import AutoAwesomeOutlinedIcon from "@mui/icons-material/AutoAwesomeOutlined";
import DeleteOutlineOutlinedIcon from "@mui/icons-material/DeleteOutlineOutlined";
import EditOutlinedIcon from "@mui/icons-material/EditOutlined";
import ImageOutlinedIcon from "@mui/icons-material/ImageOutlined";
import LinkOutlinedIcon from "@mui/icons-material/LinkOutlined";
import OndemandVideoOutlinedIcon from "@mui/icons-material/OndemandVideoOutlined";
import QuizOutlinedIcon from "@mui/icons-material/QuizOutlined";
import RocketLaunchOutlinedIcon from "@mui/icons-material/RocketLaunchOutlined";
import SaveOutlinedIcon from "@mui/icons-material/SaveOutlined";
import StyleOutlinedIcon from "@mui/icons-material/StyleOutlined";
import UnarchiveOutlinedIcon from "@mui/icons-material/UnarchiveOutlined";
import ViewSidebarOutlinedIcon from "@mui/icons-material/ViewSidebarOutlined";
import { LessonAttachments, getSourceAttachments } from "@/features/lesson-reader/ui/LessonAttachments";
import type { LessonAttachmentAsset } from "@/features/lesson-reader/ui/LessonAttachments";
import type { LessonEditorAssetDragPayload } from "@/shared/editor/lesson-editor-drag";
import { SimpleEditor } from "@/shared/ui/SimpleEditor";
import { fetchFilePreviewUrl, useFilePreviewUrls } from "@/shared/api/files";
import { markdownToHtml } from "@/shared/lib/lessonContent";
import { hexToRgba } from "@/shared/lib/brandColors";
import { clampEntityTitle, MAX_ENTITY_TITLE_LENGTH } from "@/shared/lib/entityTitle";
import { inferVideoMimeType, isLikelyVideoFile } from "@/shared/lib/videoFileValidation";
import {
    lessonTagFieldInputProps,
    normalizeLessonTagInput,
    suggestedLessonTags,
} from "@/shared/lib/lessonTags";
import { useQueryClient } from "@tanstack/react-query";
import { libraryQueryKeys } from "../api/queryKeys";
import {
    useAddLessonAssetMutation,
    useChangeLessonStatusMutation,
    useDeleteActivityMutation,
    useDeleteLessonAssetMutation,
    useDeleteLessonMutation,
    useGenerateActivityMutation,
    useReviseLessonMutation,
    useTeacherVideoMutation,
    useUpdateActivityMutation,
    useUpdateLessonContentMutation,
    useUploadLessonFileMutation,
    useUploadLessonVideoMutation,
} from "../api/useLessonMutations";
import type { LibraryLesson, LibraryMaterial, QuizQuestionTypeV1 } from "../api/types";
import { DiscardChangesDialog } from "./DiscardChangesDialog";
import { Dialog as SharedDialog } from "@/shared/ui/Dialog";
import { Button as SharedButton } from "@/shared/ui/Button";
import { LessonReader } from "./LessonReader";

// Loose shapes for the dynamic generationMetadata blob and saved activities.
interface SourceReference {
    id: number | string;
    sourceNumber?: number | string;
    title?: string;
    links?: unknown[];
    youtubeUrls?: unknown[];
    attachments?: unknown[];
}

interface DialogActivity {
    id: number;
    type: string;
    title?: string;
    itemCount?: number;
    payload?: {
        title?: string;
        items?: Array<{
            type?: string;
            question?: string;
            options?: string[];
            correctAnswer?: string | string[];
            correctAnswers?: string[];
            explanation?: string;
        }>;
        cards?: Array<{ front?: string; back?: string; explanation?: string }>;
    };
}

interface QuizDraftItem {
    type: QuizQuestionTypeV1;
    question: string;
    options: string[];
    correctAnswers: string[];
    explanation: string;
    /** Previous non-T/F options restored when switching type back from True/False. */
    savedOptions?: string[];
    savedCorrectAnswers?: string[];
}

interface QuizDraft {
    id: number;
    type: string;
    title: string;
    items: QuizDraftItem[];
}

const quizQuestionTypeOptions: Array<{ value: QuizQuestionTypeV1; label: string }> = [
    { value: "single_choice", label: "Single choice" },
    { value: "multiple_choice", label: "Multiple choice" },
    { value: "true_false", label: "True / False" },
    { value: "fill_in_blanks_with_options", label: "Fill in the blanks" },
];

const TRUE_FALSE_OPTIONS = ["True", "False"];
const FILL_IN_BLANKS_MARKER = "_____";
const MIN_QUIZ_OPTIONS = 2;
const MAX_QUIZ_OPTIONS = 6;
const MIN_LESSON_CONTENT_CHARS = 200;

function getPlainTextLength(htmlOrMarkdown: string): number {
    return htmlOrMarkdown
        .replace(/<[^>]+>/g, " ")
        .replace(/&nbsp;/gi, " ")
        .replace(/\s+/g, " ")
        .trim().length;
}

function validateLessonTitleAndContent(title: string, contentHtml: string, contentMarkdown = ""): string | null {
    if (!title.trim()) {
        return "Title is required.";
    }
    if (title.trim().length > MAX_ENTITY_TITLE_LENGTH) {
        return `Title must be at most ${MAX_ENTITY_TITLE_LENGTH} characters.`;
    }
    const contentLength = Math.max(getPlainTextLength(contentHtml), getPlainTextLength(contentMarkdown));
    if (contentLength === 0) {
        return "Lesson content is required.";
    }
    return null;
}

function normalizeCorrectAnswers(raw: unknown): string[] {
    if (Array.isArray(raw)) {
        return raw.map((value) => String(value ?? "").trim()).filter(Boolean);
    }
    if (typeof raw === "string" && raw.trim()) {
        return [raw.trim()];
    }
    return [];
}

function isTrueFalseOptions(options: string[]): boolean {
    return (
        options.length === TRUE_FALSE_OPTIONS.length &&
        options.every((option, index) => option === TRUE_FALSE_OPTIONS[index])
    );
}

function cloneOptions(options: string[] | undefined): string[] | undefined {
    return options ? [...options] : undefined;
}

function cloneAnswers(answers: string[] | undefined): string[] | undefined {
    return answers ? [...answers] : undefined;
}

/** Options worth restoring after a True/False detour (not the T/F pair itself). */
function stashableOptions(options: string[]): string[] | undefined {
    if (options.length < MIN_QUIZ_OPTIONS || isTrueFalseOptions(options)) {
        return undefined;
    }
    return [...options];
}

function defaultMcOptions(): string[] {
    return Array.from({ length: 4 }, () => "");
}

function switchQuizQuestionType(item: QuizDraftItem, nextType: QuizQuestionTypeV1): QuizDraftItem {
    if (nextType === item.type) {
        return item;
    }

    const leavingTrueFalse = item.type === "true_false";
    const enteringTrueFalse = nextType === "true_false";

    if (enteringTrueFalse) {
        const stashed = stashableOptions(item.options) ?? cloneOptions(item.savedOptions);
        const stashedCorrect =
            stashableOptions(item.options) != null
                ? cloneAnswers(item.correctAnswers)
                : cloneAnswers(item.savedCorrectAnswers);
        return {
            ...item,
            type: nextType,
            savedOptions: stashed,
            savedCorrectAnswers: stashedCorrect,
            options: [...TRUE_FALSE_OPTIONS],
            correctAnswers: item.correctAnswers
                .filter((answer) => TRUE_FALSE_OPTIONS.includes(answer))
                .slice(0, 1),
        };
    }

    if (leavingTrueFalse) {
        const restoredOptions =
            item.savedOptions && item.savedOptions.length >= MIN_QUIZ_OPTIONS
                ? [...item.savedOptions]
                : defaultMcOptions();
        const restoredCorrect =
            item.savedCorrectAnswers && item.savedCorrectAnswers.length > 0
                ? nextType === "multiple_choice"
                    ? [...item.savedCorrectAnswers]
                    : item.savedCorrectAnswers.slice(0, 1)
                : [];
        return {
            ...item,
            type: nextType,
            options: restoredOptions,
            correctAnswers: restoredCorrect,
            // Keep stash so further T/F round-trips still restore the same MC set.
            savedOptions: restoredOptions,
            savedCorrectAnswers: restoredCorrect,
        };
    }

    // multiple_choice ↔ fill_in_blanks: keep free-form options intact.
    return {
        ...item,
        type: nextType,
        correctAnswers: nextType === "multiple_choice" ? item.correctAnswers : item.correctAnswers.slice(0, 1),
        savedOptions: stashableOptions(item.options) ?? cloneOptions(item.savedOptions),
        savedCorrectAnswers: cloneAnswers(item.correctAnswers) ?? cloneAnswers(item.savedCorrectAnswers),
    };
}

function validateQuizDraft(draft: QuizDraft): string | null {
    for (let index = 0; index < draft.items.length; index += 1) {
        const item = draft.items[index];
        const questionNumber = index + 1;
        if (!item.question.trim()) {
            return `Question ${questionNumber}: add a question.`;
        }
        if (item.type === "fill_in_blanks_with_options") {
            const markerCount = (item.question.match(/_{3,}/g) || []).length;
            if (markerCount !== 1) {
                return `Question ${questionNumber}: include exactly one blank marker (${FILL_IN_BLANKS_MARKER}).`;
            }
        }
        const filledOptions =
            item.type === "true_false"
                ? TRUE_FALSE_OPTIONS
                : item.options.map((option) => option.trim()).filter(Boolean);
        if (filledOptions.length < MIN_QUIZ_OPTIONS) {
            return `Question ${questionNumber}: add at least ${MIN_QUIZ_OPTIONS} answer options.`;
        }
        const uniqueOptions = new Set(filledOptions.map((option) => option.toLowerCase()));
        if (uniqueOptions.size !== filledOptions.length) {
            return `Question ${questionNumber}: option text must be unique within a question.`;
        }
        const correctAnswers = item.correctAnswers.map((value) => value.trim()).filter(Boolean);
        if (correctAnswers.length === 0) {
            return `Question ${questionNumber}: select at least one correct answer.`;
        }
        const optionSet = new Set(filledOptions);
        if (correctAnswers.some((answer) => !optionSet.has(answer))) {
            return `Question ${questionNumber}: correct answer must match an option.`;
        }
        if (item.type !== "multiple_choice" && correctAnswers.length > 1) {
            return `Question ${questionNumber}: only one correct answer is allowed for this type.`;
        }
    }
    return null;
}

function validateFlashcardsDraft(draft: FlashcardsDraft): string | null {
    if (draft.cards.length === 0) {
        return "Add at least one flashcard.";
    }
    for (let index = 0; index < draft.cards.length; index += 1) {
        const card = draft.cards[index];
        if (!card.front.trim() || !card.back.trim()) {
            return `Card ${index + 1}: Front and Back are required.`;
        }
    }
    return null;
}

function toQuizSaveItems(draft: QuizDraft) {
    return draft.items.map((item) => ({
        type: item.type,
        question: item.question,
        options: item.type === "true_false" ? TRUE_FALSE_OPTIONS : item.options,
        correctAnswer: item.correctAnswers[0] || "",
        correctAnswers: item.correctAnswers,
        explanation: item.explanation,
    }));
}

interface FlashcardsDraft {
    id: number;
    type: string;
    title: string;
    cards: Array<{ front: string; back: string; explanation: string }>;
}

type ActivityDraft = QuizDraft | FlashcardsDraft;

interface TeacherVideoMeta {
    status?: string;
    videoId?: string;
    videoUrl?: string;
    thumbnailUrl?: string;
    duration?: number;
}

function formatDateTime(isoString?: string) {
    try {
        return new Intl.DateTimeFormat("en", {
            year: "numeric",
            month: "short",
            day: "numeric",
            hour: "2-digit",
            minute: "2-digit",
        }).format(new Date(isoString || ""));
    } catch {
        return "";
    }
}

const revisionOptions = [
    { value: "simpler", label: "Simpler" },
    { value: "deeper", label: "Deeper" },
    { value: "examples", label: "More examples" },
    { value: "structured", label: "Better structure" },
    { value: "shorter", label: "Shorter" },
];

const activityTypeOptions = [
    { value: "quiz", label: "Quiz", min: 3, max: 20, defaultCount: 8 },
    { value: "flashcards", label: "Flashcards", min: 5, max: 40, defaultCount: 12 },
] as const;

const teacherVideoActiveStatuses = new Set(["pending", "processing", "generating"]);
const teacherVideoRefreshBufferMs = 6 * 60 * 60 * 1000;

function getSignedUrlExpiresAt(url = "") {
    if (!url) {
        return null;
    }

    try {
        const expires = new URL(url).searchParams.get("Expires");
        const expiresSeconds = Number.parseInt(expires || "", 10);

        return Number.isFinite(expiresSeconds) ? expiresSeconds * 1000 : null;
    } catch {
        return null;
    }
}

function shouldRefreshTeacherVideoUrl(teacherVideo: TeacherVideoMeta = {}) {
    if (!teacherVideo.videoId || teacherVideoActiveStatuses.has(teacherVideo.status || "")) {
        return false;
    }

    if (!teacherVideo.videoUrl) {
        return true;
    }

    const expiresAt = getSignedUrlExpiresAt(teacherVideo.videoUrl);

    return expiresAt !== null && expiresAt <= Date.now() + teacherVideoRefreshBufferMs;
}

const LESSON_DIALOG_COLORS = {
    blue: "#0009DC",
    ink: "#0B0B0B",
    slate: "#33344A",
    mute: "#80808E",
    blue50: "#F5F5FE",
    blue100: "#E3E5FF",
    blue200: "#CBD0FF",
    success: "#229E5A",
};

const lessonActionButtonSx = {
    borderRadius: 999,
    fontSize: 12,
    fontWeight: 800,
    letterSpacing: "0.06em",
    textTransform: "uppercase",
} as const;

const lessonSecondaryButtonSx = {
    ...lessonActionButtonSx,
    borderColor: LESSON_DIALOG_COLORS.blue200,
    color: LESSON_DIALOG_COLORS.blue,
    backgroundColor: "#fff",
    "&:hover": {
        borderColor: LESSON_DIALOG_COLORS.blue,
        backgroundColor: LESSON_DIALOG_COLORS.blue50,
    },
};

const lessonPrimaryButtonSx = {
    ...lessonActionButtonSx,
    backgroundColor: LESSON_DIALOG_COLORS.blue,
    color: "#fff",
    boxShadow: "0 8px 18px rgba(0, 9, 220, 0.22)",
    "&:hover": {
        backgroundColor: LESSON_DIALOG_COLORS.blue,
        boxShadow: "0 10px 22px rgba(0, 9, 220, 0.26)",
    },
};

const lessonDarkButtonSx = {
    ...lessonActionButtonSx,
    backgroundColor: LESSON_DIALOG_COLORS.ink,
    color: "#fff",
    boxShadow: "none",
    "&:hover": {
        backgroundColor: LESSON_DIALOG_COLORS.ink,
        boxShadow: "none",
    },
};

const lessonDangerButtonSx = {
    ...lessonActionButtonSx,
    borderColor: "rgba(214, 47, 47, 0.28)",
    color: "#D62F2F",
    backgroundColor: "#fff",
    "&:hover": {
        borderColor: "#D62F2F",
        backgroundColor: "rgba(214, 47, 47, 0.05)",
    },
};

const activityTextFieldSx = {
    "& .MuiOutlinedInput-root": {
        borderRadius: 1.5,
        backgroundColor: "#fff",
        "& fieldset": { borderColor: LESSON_DIALOG_COLORS.blue200 },
        "&:hover fieldset": { borderColor: LESSON_DIALOG_COLORS.blue },
        "&.Mui-focused fieldset": { borderColor: LESSON_DIALOG_COLORS.blue },
    },
    "& .MuiInputLabel-root": {
        color: LESSON_DIALOG_COLORS.mute,
        fontSize: 13,
        fontWeight: 700,
    },
    "& .MuiInputBase-input": {
        color: LESSON_DIALOG_COLORS.ink,
        fontSize: 14,
        lineHeight: 1.45,
    },
};

const activityPlainFieldSx = {
    "& .MuiInputLabel-root": {
        color: LESSON_DIALOG_COLORS.mute,
        fontSize: 12,
        fontWeight: 800,
        letterSpacing: "0.04em",
    },
    "& .MuiInputBase-root": {
        color: LESSON_DIALOG_COLORS.ink,
        fontSize: 15,
        lineHeight: 1.45,
    },
    "& .MuiInputBase-input": {
        py: 0.75,
    },
    "& .MuiInput-underline:before": {
        borderBottomColor: "rgba(0, 9, 220, 0.14)",
    },
    "& .MuiInput-underline:hover:not(.Mui-disabled):before": {
        borderBottomColor: "rgba(0, 9, 220, 0.28)",
    },
    "& .MuiInput-underline:after": {
        borderBottomColor: LESSON_DIALOG_COLORS.blue,
    },
};

const activityCardSx = {
    p: { xs: 1.5, md: 2.25 },
    borderRadius: 1.75,
    border: `1px solid ${LESSON_DIALOG_COLORS.blue100}`,
    backgroundColor: "#fff",
};

const activityBadgeSx = {
    height: 28,
    borderRadius: 999,
    px: 1.25,
    color: LESSON_DIALOG_COLORS.blue,
    backgroundColor: LESSON_DIALOG_COLORS.blue50,
    fontSize: 11,
    fontWeight: 800,
    letterSpacing: "0.06em",
    textTransform: "uppercase",
};

function getActivityTypeSettings(type: string) {
    return activityTypeOptions.find((option) => option.value === type) || activityTypeOptions[0];
}

/**
 * Deleting a lesson asset removes its storage-backed authorization record, so any inline
 * image/video node in the lesson content still referencing that storage key would otherwise
 * be left dangling (its preview URL starts 403ing once the asset row is gone). Strips those
 * nodes out of the content HTML so the deleted asset disappears from the editor too.
 */
function stripStorageKeyFromHtml(html: string, storageKey: string): string {
    if (!html || !storageKey || typeof window === "undefined") {
        return html;
    }

    const parser = new DOMParser();
    const doc = parser.parseFromString(html, "text/html");
    const matches = doc.querySelectorAll(
        `img[data-storage-key="${CSS.escape(storageKey)}"], video[data-storage-key="${CSS.escape(storageKey)}"], a[data-storage-key="${CSS.escape(storageKey)}"]`,
    );

    if (matches.length === 0) {
        return html;
    }

    matches.forEach((element) => element.remove());
    return doc.body.innerHTML;
}

function buildLessonAssetEditorDragPayload(attachment: LessonAttachmentAsset): LessonEditorAssetDragPayload | null {
    if (!attachment.url && !attachment.storageKey && !attachment.previewUrl) {
        return null;
    }

    return {
        kind: attachment.kind,
        mimeType: attachment.mimeType,
        name: attachment.name,
        title: attachment.youtubeTitle || attachment.name,
        storageKey: attachment.storageKey,
        previewUrl: attachment.previewUrl,
        url: attachment.url,
    };
}

function DetailPanel({ title, children }: { title: string; children: ReactNode }) {
    return (
        <Box>
            <Typography
                sx={{
                    mb: 1.25,
                    color: LESSON_DIALOG_COLORS.mute,
                    fontSize: 11,
                    fontWeight: 800,
                    letterSpacing: "0.08em",
                    lineHeight: 1,
                    textTransform: "uppercase",
                }}
            >
                {title}
            </Typography>

            <Box
                sx={{
                    p: 1.25,
                    borderRadius: 1.25,
                    border: `1px solid ${LESSON_DIALOG_COLORS.blue200}`,
                    backgroundColor: "#fff",
                }}
            >
                {children}
            </Box>
        </Box>
    );
}

function normalizeLessonAssetForCard(asset: Record<string, any>) {
    if (asset.kind === "youtube") {
        return {
            id: asset.id,
            name: asset.title || asset.name || "YouTube video",
            kind: "youtube",
            mimeType: "video/youtube",
            url: asset.url,
            youtubeTitle: asset.title || "",
            youtubeAuthorName: asset.metadata?.authorName || asset.description || "",
            youtubeThumbnailUrl: asset.imageUrl || "",
            sourceTitle: "Lesson asset",
            isLessonAsset: true,
        };
    }

    if (asset.kind === "link") {
        return {
            id: asset.id,
            name: asset.title || asset.name || "Web link",
            kind: "link",
            mimeType: "text/html",
            url: asset.url,
            linkTitle: asset.title || "",
            linkDescription: asset.description || "",
            linkImageUrl: asset.imageUrl || "",
            linkSiteName: asset.siteName || "",
            sourceTitle: "Lesson asset",
            isLessonAsset: true,
        };
    }

    return {
        id: asset.id,
        name: asset.name || asset.title || "Lesson file",
        kind: asset.kind,
        mimeType: asset.mimeType || "",
        size: asset.size || 0,
        storageKey: asset.storageKey || "",
        sourceTitle: "Lesson asset",
        isLessonAsset: true,
    };
}

function getActivityItems(activity?: DialogActivity) {
    if (activity?.type === "flashcards") {
        return Array.isArray(activity.payload?.cards) ? activity.payload.cards : [];
    }

    return Array.isArray(activity?.payload?.items) ? activity!.payload!.items : [];
}

function createActivityDraft(activity?: DialogActivity): ActivityDraft | null {
    if (!activity) {
        return null;
    }

    if (activity.type === "flashcards") {
        return {
            id: activity.id,
            type: activity.type,
            title: activity.title || activity.payload?.title || "Lesson flashcards",
            cards: getActivityItems(activity).map((card: any) => ({
                front: card.front || "",
                back: card.back || "",
                explanation: card.explanation || "",
            })),
        };
    }

    return {
        id: activity.id,
        type: activity.type,
        title: activity.title || activity.payload?.title || "Lesson quiz",
        items: getActivityItems(activity).map((item: any) => {
            const type = (item.type as QuizQuestionTypeV1) || "multiple_choice";
            const rawOptions = Array.isArray(item.options) ? item.options.map((value: unknown) => String(value ?? "")) : [];
            const options =
                type === "true_false"
                    ? TRUE_FALSE_OPTIONS
                    : rawOptions.length >= MIN_QUIZ_OPTIONS
                      ? rawOptions
                      : Array.from({ length: MIN_QUIZ_OPTIONS }, (_, index) => rawOptions[index] || "");
            const correctAnswers = normalizeCorrectAnswers(item.correctAnswers ?? item.correctAnswer);
            return {
                type,
                question: item.question || "",
                options,
                correctAnswers,
                explanation: item.explanation || "",
            };
        }),
    };
}

function ActivityEditor({
    activity,
    draft,
    onDraftChange,
    disabled,
}: {
    activity: DialogActivity | null;
    draft: ActivityDraft | null;
    onDraftChange: (updater: (current: Record<number, ActivityDraft>) => Record<number, ActivityDraft>) => void;
    disabled: boolean;
}) {
    if (!activity || !draft) {
        return (
            <Stack spacing={1.5} sx={{ p: { xs: 2, md: 3 } }}>
                <Typography variant="h6" sx={{ fontWeight: 900 }}>
                    Activity not found
                </Typography>
                <Typography color="text.secondary">
                    Generate an activity first, then it will appear as an editor tab here.
                </Typography>
            </Stack>
        );
    }

    const isFlashcards = activity.type === "flashcards";
    const itemCount = isFlashcards ? (draft as FlashcardsDraft).cards.length : (draft as QuizDraft).items.length;

    const updateDraft = (updater: (current: ActivityDraft) => ActivityDraft) => {
        onDraftChange((current) => {
            const currentDraft = current[activity.id] || createActivityDraft(activity)!;

            return {
                ...current,
                [activity.id]: updater(currentDraft),
            };
        });
    };

    const updateQuizItem = (itemIndex: number, nextItem: QuizDraft["items"][number]) => {
        updateDraft((currentDraft) => ({
            ...(currentDraft as QuizDraft),
            items: (currentDraft as QuizDraft).items.map((item, index) => (index === itemIndex ? nextItem : item)),
        }));
    };

    const updateCard = (cardIndex: number, nextCard: FlashcardsDraft["cards"][number]) => {
        updateDraft((currentDraft) => ({
            ...(currentDraft as FlashcardsDraft),
            cards: (currentDraft as FlashcardsDraft).cards.map((card, index) => (index === cardIndex ? nextCard : card)),
        }));
    };

    return (
        <Stack sx={{ minHeight: 0, height: "100%", backgroundColor: "#fff" }}>
            <Box
                sx={{
                    px: { xs: 2, md: 3.5 },
                    py: 2.25,
                    borderBottom: `1px solid ${LESSON_DIALOG_COLORS.blue100}`,
                    backgroundColor: "#fff",
                }}
            >
                <Stack direction={{ xs: "column", md: "row" }} spacing={1.5} sx={{ justifyContent: "space-between" }}>
                    <Stack direction="row" spacing={1.25} sx={{ alignItems: "center" }}>
                        <Box
                            sx={{
                                width: 38,
                                height: 38,
                                borderRadius: "50%",
                                display: "grid",
                                placeItems: "center",
                                color: LESSON_DIALOG_COLORS.blue,
                                backgroundColor: LESSON_DIALOG_COLORS.blue50,
                            }}
                        >
                            {isFlashcards ? <StyleOutlinedIcon fontSize="small" /> : <QuizOutlinedIcon fontSize="small" />}
                        </Box>
                        <Box>
                            <Typography
                                sx={{
                                    color: LESSON_DIALOG_COLORS.blue,
                                    fontSize: 11,
                                    fontWeight: 800,
                                    letterSpacing: "0.08em",
                                    lineHeight: 1,
                                    mb: 0.75,
                                    textTransform: "uppercase",
                                }}
                            >
                                Activity editor
                            </Typography>
                            <Typography sx={{ color: LESSON_DIALOG_COLORS.ink, fontSize: 24, fontWeight: 900, lineHeight: 1 }}>
                                {isFlashcards ? "Flashcards editor" : "Quiz editor"}
                            </Typography>
                        </Box>
                    </Stack>

                    <Chip
                        label={`${itemCount} item${itemCount === 1 ? "" : "s"}`}
                        size="small"
                        sx={{
                            ...activityBadgeSx,
                            alignSelf: { xs: "flex-start", md: "center" },
                        }}
                    />
                </Stack>
            </Box>

            <Box
                sx={{
                    flex: "1 1 auto",
                    minHeight: 0,
                    overflow: "auto",
                    p: { xs: 2, md: 3.5 },
                    backgroundColor: "#fff",
                }}
            >
                <Stack spacing={2.25}>
                    <TextField
                        label={isFlashcards ? "Flashcards title" : "Quiz title"}
                        value={draft.title}
                        onChange={(event) => updateDraft((currentDraft) => ({ ...currentDraft, title: event.target.value }))}
                        disabled={disabled}
                        fullWidth
                        sx={activityTextFieldSx}
                    />

                    {isFlashcards ? (
                        <>
                            {(draft as FlashcardsDraft).cards.map((card, cardIndex) => (
                                <Paper key={`card-${cardIndex}`} elevation={0} sx={activityCardSx}>
                                    <Stack spacing={1.5}>
                                        <Stack
                                            direction="row"
                                            spacing={1}
                                            sx={{ alignItems: "center", justifyContent: "space-between" }}
                                        >
                                            <Chip label={`Card ${cardIndex + 1}`} size="small" sx={activityBadgeSx} />
                                            <IconButton
                                                aria-label="Remove flashcard"
                                                size="small"
                                                disabled={disabled || (draft as FlashcardsDraft).cards.length <= 1}
                                                onClick={() =>
                                                    updateDraft((currentDraft) => ({
                                                        ...(currentDraft as FlashcardsDraft),
                                                        cards: (currentDraft as FlashcardsDraft).cards.filter(
                                                            (_, index) => index !== cardIndex,
                                                        ),
                                                    }))
                                                }
                                                sx={{
                                                    color: "#D62F2F",
                                                    "&.Mui-disabled": { color: LESSON_DIALOG_COLORS.mute },
                                                }}
                                            >
                                                <DeleteOutlineOutlinedIcon fontSize="small" />
                                            </IconButton>
                                        </Stack>
                                        <TextField
                                            label="Front"
                                            value={card.front}
                                            onChange={(event) => updateCard(cardIndex, { ...card, front: event.target.value })}
                                            disabled={disabled}
                                            multiline
                                            minRows={2}
                                            fullWidth
                                            sx={activityTextFieldSx}
                                        />
                                        <TextField
                                            label="Back"
                                            value={card.back}
                                            onChange={(event) => updateCard(cardIndex, { ...card, back: event.target.value })}
                                            disabled={disabled}
                                            multiline
                                            minRows={2}
                                            fullWidth
                                            sx={activityTextFieldSx}
                                        />
                                        <TextField
                                            label="Explanation"
                                            value={card.explanation}
                                            onChange={(event) =>
                                                updateCard(cardIndex, { ...card, explanation: event.target.value })
                                            }
                                            disabled={disabled}
                                            multiline
                                            minRows={2}
                                            fullWidth
                                            sx={activityTextFieldSx}
                                        />
                                    </Stack>
                                </Paper>
                            ))}
                            <Button
                                variant="outlined"
                                startIcon={<AddOutlinedIcon />}
                                disabled={disabled}
                                onClick={() =>
                                    updateDraft((currentDraft) => ({
                                        ...(currentDraft as FlashcardsDraft),
                                        cards: [...(currentDraft as FlashcardsDraft).cards, { front: "", back: "", explanation: "" }],
                                    }))
                                }
                                sx={{ ...lessonSecondaryButtonSx, alignSelf: "flex-start" }}
                            >
                                Add card
                            </Button>
                        </>
                    ) : (
                        <>
                            {(draft as QuizDraft).items.map((item, itemIndex) => (
                                <Paper key={`question-${itemIndex}`} elevation={0} sx={activityCardSx}>
                                    <Stack spacing={1.5}>
                                        <Stack
                                            direction="row"
                                            spacing={1}
                                            sx={{ alignItems: "center", justifyContent: "space-between" }}
                                        >
                                            <Chip label={`Question ${itemIndex + 1}`} size="small" sx={activityBadgeSx} />
                                            <Stack direction="row" spacing={1} sx={{ alignItems: "center" }}>
                                                <Select
                                                    value={item.type}
                                                    onChange={(event) => {
                                                        const nextType = event.target.value as QuizQuestionTypeV1;
                                                        updateQuizItem(itemIndex, switchQuizQuestionType(item, nextType));
                                                    }}
                                                    disabled={disabled}
                                                    size="small"
                                                    variant="standard"
                                                    sx={{ fontSize: 13, fontWeight: 700 }}
                                                >
                                                    {quizQuestionTypeOptions.map((option) => (
                                                        <MenuItem key={option.value} value={option.value}>
                                                            {option.label}
                                                        </MenuItem>
                                                    ))}
                                                </Select>
                                                <IconButton
                                                    aria-label="Remove question"
                                                    size="small"
                                                    disabled={disabled || (draft as QuizDraft).items.length <= 1}
                                                    onClick={() =>
                                                        updateDraft((currentDraft) => ({
                                                            ...(currentDraft as QuizDraft),
                                                            items: (currentDraft as QuizDraft).items.filter(
                                                                (_, index) => index !== itemIndex,
                                                            ),
                                                        }))
                                                    }
                                                    sx={{
                                                        color: "#D62F2F",
                                                        "&.Mui-disabled": { color: LESSON_DIALOG_COLORS.mute },
                                                    }}
                                                >
                                                    <DeleteOutlineOutlinedIcon fontSize="small" />
                                                </IconButton>
                                            </Stack>
                                        </Stack>
                                        <TextField
                                            label="Question"
                                            value={item.question}
                                            helperText={
                                                item.type === "fill_in_blanks_with_options"
                                                    ? `Include exactly one blank marker: ${FILL_IN_BLANKS_MARKER}`
                                                    : undefined
                                            }
                                            onChange={(event) => updateQuizItem(itemIndex, { ...item, question: event.target.value })}
                                            disabled={disabled}
                                            variant="standard"
                                            multiline
                                            minRows={2}
                                            fullWidth
                                            sx={activityPlainFieldSx}
                                        />
                                        {item.type === "true_false" ? (
                                            <Stack spacing={0.75}>
                                                {TRUE_FALSE_OPTIONS.map((option) => {
                                                    const isCorrectOption = item.correctAnswers.includes(option);

                                                    return (
                                                        <Box
                                                            key={option}
                                                            sx={{
                                                                display: "grid",
                                                                gridTemplateColumns: "auto minmax(0, 1fr)",
                                                                gap: 1,
                                                                alignItems: "center",
                                                                px: 1.25,
                                                                py: 0.75,
                                                                borderRadius: 999,
                                                                border: isCorrectOption
                                                                    ? `1.5px solid ${LESSON_DIALOG_COLORS.blue}`
                                                                    : "1.5px solid transparent",
                                                                backgroundColor: isCorrectOption ? LESSON_DIALOG_COLORS.blue50 : "#fff",
                                                            }}
                                                        >
                                                            <Radio
                                                                checked={isCorrectOption}
                                                                disabled={disabled}
                                                                onChange={() =>
                                                                    updateQuizItem(itemIndex, { ...item, correctAnswers: [option] })
                                                                }
                                                                size="small"
                                                                sx={{
                                                                    color: LESSON_DIALOG_COLORS.blue200,
                                                                    "&.Mui-checked": { color: LESSON_DIALOG_COLORS.blue },
                                                                }}
                                                            />
                                                            <Typography sx={{ fontSize: 14, color: LESSON_DIALOG_COLORS.ink }}>
                                                                {option}
                                                            </Typography>
                                                        </Box>
                                                    );
                                                })}
                                            </Stack>
                                        ) : (
                                            <Stack spacing={0.75}>
                                                {item.options.map((option, optionIndex) => {
                                                    const isCorrectOption = Boolean(option) && item.correctAnswers.includes(option);

                                                    return (
                                                        <Box
                                                            key={`option-${optionIndex}`}
                                                            sx={{
                                                                display: "grid",
                                                                gridTemplateColumns: "auto minmax(0, 1fr) auto",
                                                                gap: 1,
                                                                alignItems: "center",
                                                                px: 1.25,
                                                                py: 0.75,
                                                                borderRadius: 999,
                                                                border: isCorrectOption
                                                                    ? `1.5px solid ${LESSON_DIALOG_COLORS.blue}`
                                                                    : "1.5px solid transparent",
                                                                backgroundColor: isCorrectOption ? LESSON_DIALOG_COLORS.blue50 : "#fff",
                                                            }}
                                                        >
                                                            {item.type === "multiple_choice" ? (
                                                                <Checkbox
                                                                    checked={isCorrectOption}
                                                                    disabled={disabled || !option.trim()}
                                                                    onChange={() => {
                                                                        if (!option.trim()) {
                                                                            return;
                                                                        }
                                                                        const nextCorrect = isCorrectOption
                                                                            ? item.correctAnswers.filter((answer) => answer !== option)
                                                                            : [...item.correctAnswers, option];
                                                                        updateQuizItem(itemIndex, {
                                                                            ...item,
                                                                            correctAnswers: nextCorrect,
                                                                        });
                                                                    }}
                                                                    size="small"
                                                                    sx={{
                                                                        color: LESSON_DIALOG_COLORS.blue200,
                                                                        "&.Mui-checked": { color: LESSON_DIALOG_COLORS.blue },
                                                                    }}
                                                                />
                                                            ) : (
                                                                <Radio
                                                                    checked={isCorrectOption}
                                                                    disabled={disabled || !option.trim()}
                                                                    onChange={() =>
                                                                        updateQuizItem(itemIndex, {
                                                                            ...item,
                                                                            correctAnswers: option.trim() ? [option] : [],
                                                                        })
                                                                    }
                                                                    size="small"
                                                                    sx={{
                                                                        color: LESSON_DIALOG_COLORS.blue200,
                                                                        "&.Mui-checked": { color: LESSON_DIALOG_COLORS.blue },
                                                                    }}
                                                                />
                                                            )}
                                                            <TextField
                                                                label={`Option ${optionIndex + 1}`}
                                                                value={option}
                                                                onChange={(event) => {
                                                                    const nextValue = event.target.value;
                                                                    const nextOptions = item.options.map((currentOption, index) =>
                                                                        index === optionIndex ? nextValue : currentOption,
                                                                    );
                                                                    const nextCorrectAnswers = item.correctAnswers
                                                                        .map((answer) => (answer === option ? nextValue : answer))
                                                                        .filter((answer) => Boolean(answer.trim()));

                                                                    updateQuizItem(itemIndex, {
                                                                        ...item,
                                                                        options: nextOptions,
                                                                        correctAnswers:
                                                                            item.type === "multiple_choice"
                                                                                ? nextCorrectAnswers
                                                                                : nextCorrectAnswers.slice(0, 1),
                                                                        // Keep stash in sync while authoring free-form options.
                                                                        savedOptions:
                                                                            item.type === "true_false"
                                                                                ? item.savedOptions
                                                                                : stashableOptions(nextOptions) ?? item.savedOptions,
                                                                        savedCorrectAnswers:
                                                                            item.type === "true_false"
                                                                                ? item.savedCorrectAnswers
                                                                                : nextCorrectAnswers,
                                                                    });
                                                                }}
                                                                disabled={disabled}
                                                                variant="standard"
                                                                size="small"
                                                                fullWidth
                                                                sx={activityPlainFieldSx}
                                                            />
                                                            <IconButton
                                                                aria-label={`Remove option ${optionIndex + 1}`}
                                                                size="small"
                                                                disabled={disabled || item.options.length <= MIN_QUIZ_OPTIONS}
                                                                onClick={() => {
                                                                    const removed = item.options[optionIndex];
                                                                    updateQuizItem(itemIndex, {
                                                                        ...item,
                                                                        options: item.options.filter((_, index) => index !== optionIndex),
                                                                        correctAnswers: item.correctAnswers.filter(
                                                                            (answer) => answer !== removed,
                                                                        ),
                                                                    });
                                                                }}
                                                                sx={{
                                                                    color: "#D62F2F",
                                                                    "&.Mui-disabled": { color: LESSON_DIALOG_COLORS.mute },
                                                                }}
                                                            >
                                                                <CloseOutlinedIcon fontSize="small" />
                                                            </IconButton>
                                                        </Box>
                                                    );
                                                })}
                                                <Button
                                                    variant="text"
                                                    startIcon={<AddOutlinedIcon />}
                                                    disabled={disabled || item.options.length >= MAX_QUIZ_OPTIONS}
                                                    onClick={() =>
                                                        updateQuizItem(itemIndex, {
                                                            ...item,
                                                            options: [...item.options, ""],
                                                        })
                                                    }
                                                    sx={{
                                                        alignSelf: "flex-start",
                                                        minHeight: 32,
                                                        px: 1,
                                                        color: LESSON_DIALOG_COLORS.blue,
                                                        fontSize: 12,
                                                        fontWeight: 700,
                                                        textTransform: "none",
                                                    }}
                                                >
                                                    Add option
                                                </Button>
                                            </Stack>
                                        )}
                                        <TextField
                                            label="Explanation"
                                            value={item.explanation}
                                            onChange={(event) => updateQuizItem(itemIndex, { ...item, explanation: event.target.value })}
                                            disabled={disabled}
                                            variant="standard"
                                            multiline
                                            minRows={2}
                                            fullWidth
                                            sx={activityPlainFieldSx}
                                        />
                                    </Stack>
                                </Paper>
                            ))}
                            <Button
                                variant="outlined"
                                startIcon={<AddOutlinedIcon />}
                                disabled={disabled}
                                onClick={() =>
                                    updateDraft((currentDraft) => ({
                                        ...(currentDraft as QuizDraft),
                                        items: [
                                            ...(currentDraft as QuizDraft).items,
                                            {
                                                type: "single_choice",
                                                question: "",
                                                options: ["", "", "", ""],
                                                correctAnswers: [],
                                                explanation: "",
                                            },
                                        ],
                                    }))
                                }
                                sx={{ ...lessonSecondaryButtonSx, alignSelf: "flex-start" }}
                            >
                                Add question
                            </Button>
                        </>
                    )}
                </Stack>
            </Box>
        </Stack>
    );
}

interface LessonDetailsDialogProps {
    lesson: LibraryLesson | null;
    open: boolean;
    onClose: () => void;
    onOpenSourceMaterial?: (materialId: number) => void;
    onLessonDeleted?: (lessonId: number) => void | Promise<void>;
    onLessonUpdated?: (lesson: LibraryLesson, options?: { silent?: boolean }) => void | Promise<void>;
    canPublish?: boolean;
    onValidationError?: (message: string) => void;
    /** Content edit / revise / archive / delete. Must be false for Member role. */
    canManageLesson?: boolean;
    /** Activity generate / edit. Must be false for Member role. */
    canManageActivities?: boolean;
}

export function LessonDetailsDialog({
    lesson,
    open,
    onClose,
    onOpenSourceMaterial,
    onLessonDeleted,
    onLessonUpdated,
    canPublish = false,
    onValidationError,
    canManageLesson = false,
    canManageActivities = false,
}: LessonDetailsDialogProps) {
    const queryClient = useQueryClient();
    const updateLessonM = useUpdateLessonContentMutation();
    const changeStatusM = useChangeLessonStatusMutation();
    const deleteLessonM = useDeleteLessonMutation();
    const reviseLessonM = useReviseLessonMutation();
    const generateActivityM = useGenerateActivityMutation();
    const updateActivityM = useUpdateActivityMutation();
    const deleteActivityM = useDeleteActivityMutation();
    const uploadFileM = useUploadLessonFileMutation();
    const uploadVideoM = useUploadLessonVideoMutation();
    const addAssetM = useAddLessonAssetMutation();
    const deleteAssetM = useDeleteLessonAssetMutation();
    const teacherVideoM = useTeacherVideoMutation();

    const [isEditing, setIsEditing] = useState(false);
    const [isConfirmDeleteOpen, setIsConfirmDeleteOpen] = useState(false);
    const [isConfirmArchiveOpen, setIsConfirmArchiveOpen] = useState(false);
    const [isDiscardOpen, setIsDiscardOpen] = useState(false);
    const discardClosesDialogRef = useRef(false);
    const [deleteError, setDeleteError] = useState("");
    const [archiveError, setArchiveError] = useState("");
    const [revisionRequest, setRevisionRequest] = useState("");
    const [selectedRevisionOptions, setSelectedRevisionOptions] = useState<string[]>([]);
    const [revisionError, setRevisionError] = useState("");
    const [pendingRevision, setPendingRevision] = useState<{
        previousHtml: string;
        previousTitle: string;
        previousMarkdown: string;
        revisedLesson: LibraryLesson;
    } | null>(null);
    const [contentSnapshots, setContentSnapshots] = useState<
        Array<{ id: string; label: string; html: string; title: string; markdown: string; createdAt: string }>
    >([]);
    const [generateConfirm, setGenerateConfirm] = useState<"append" | null>(null);
    const [isReplacingActivity, setIsReplacingActivity] = useState(false);
    const [thinContentConfirmOpen, setThinContentConfirmOpen] = useState(false);
    const [activityType, setActivityType] = useState<"quiz" | "flashcards">("quiz");
    const [activityCount, setActivityCount] = useState<number | string>(8);
    const [activityError, setActivityError] = useState("");
    const [activitySuccess, setActivitySuccess] = useState("");
    const [teacherVideoError, setTeacherVideoError] = useState("");
    const [teacherVideoSuccess, setTeacherVideoSuccess] = useState("");
    const [activeView, setActiveView] = useState<"lesson" | "quiz" | "flashcards">("lesson");
    const [activeQuizActivityId, setActiveQuizActivityId] = useState<number | null>(null);
    const [activeFlashcardsActivityId, setActiveFlashcardsActivityId] = useState<number | null>(null);
    const [activityDrafts, setActivityDrafts] = useState<Record<number, ActivityDraft>>({});
    const [activitySaveError, setActivitySaveError] = useState("");
    const [activitySaveSuccess, setActivitySaveSuccess] = useState("");
    const [assetUrl, setAssetUrl] = useState("");
    const [assetError, setAssetError] = useState("");
    const [isAddingAsset, setIsAddingAsset] = useState(false);
    const [deletingAssetId, setDeletingAssetId] = useState<string | number | null>(null);
    const [isUploadingCover, setIsUploadingCover] = useState(false);
    const assetFileInputRef = useRef<HTMLInputElement | null>(null);
    const coverFileInputRef = useRef<HTMLInputElement | null>(null);

    const initialHtml = useMemo(() => {
        return lesson?.contentHtml || markdownToHtml(lesson?.contentMarkdown || "");
    }, [lesson]);
    const lessonTagsKey = useMemo(() => {
        return normalizeLessonTagInput(lesson?.tags || []).join("\n");
    }, [lesson?.tags]);
    const lessonResetKey = `${lesson?.title || ""}\n${lessonTagsKey}`;
    const lessonExtra = lesson as
        | (LibraryLesson & {
              coverImageOriginalName?: string;
              coverImageMimeType?: string;
              updatedAt?: string;
              createdAt?: string;
          })
        | null;
    const [draftHtml, setDraftHtml] = useState(initialHtml);
    const [draftTitle, setDraftTitle] = useState(clampEntityTitle(lesson?.title || ""));
    const [draftTags, setDraftTags] = useState<string[]>(() => normalizeLessonTagInput(lesson?.tags || []));
    const [draftCoverImage, setDraftCoverImage] = useState(() => ({
        storageKey: lesson?.coverImageStorageKey || "",
        originalName: lessonExtra?.coverImageOriginalName || "",
        mimeType: lessonExtra?.coverImageMimeType || "",
    }));
    const [coverError, setCoverError] = useState("");
    const [isRightPanelCollapsed, setIsRightPanelCollapsed] = useState(false);

    const metadata = (lesson?.generationMetadata || {}) as Record<string, any>;
    const teacherVideo: TeacherVideoMeta = metadata.teacherVideo || {};
    const teacherVideoPollingStatus = teacherVideo.status;
    const teacherVideoPollingVideoId = teacherVideo.videoId;
    const teacherVideoAutoRefreshStatus = teacherVideo.status;
    const teacherVideoAutoRefreshVideoId = teacherVideo.videoId;
    const teacherVideoAutoRefreshVideoUrl = teacherVideo.videoUrl;

    const isSaving = updateLessonM.isPending;
    const statusAction = changeStatusM.variables?.payload?.action;
    const isPublishing = changeStatusM.isPending && (statusAction === "publish" || statusAction === "restore");
    const isArchiving = changeStatusM.isPending && statusAction === "archive";
    const isDeleting = deleteLessonM.isPending;
    const isDeletingAsset = deleteAssetM.isPending;
    const isRevising = reviseLessonM.isPending;
    const isGeneratingActivity = generateActivityM.isPending;
    const isSavingActivity = updateActivityM.isPending;
    const isGeneratingTeacherVideo = teacherVideoM.generate.isPending;
    const isCheckingTeacherVideo = teacherVideoM.refresh.isPending;
    const isDeletingTeacherVideo = teacherVideoM.remove.isPending;

    useEffect(() => {
        setIsEditing(false);
        setIsConfirmDeleteOpen(false);
        setIsConfirmArchiveOpen(false);
        setDeleteError("");
        setArchiveError("");
        setAssetError("");
        setDeletingAssetId(null);
        setDraftHtml(initialHtml);
        setDraftTitle(clampEntityTitle(lesson?.title || ""));
        setDraftTags(normalizeLessonTagInput(lesson?.tags || []));
        setDraftCoverImage({
            storageKey: lesson?.coverImageStorageKey || "",
            originalName: lessonExtra?.coverImageOriginalName || "",
            mimeType: lessonExtra?.coverImageMimeType || "",
        });
        setCoverError("");
        setRevisionRequest("");
        setSelectedRevisionOptions([]);
        setRevisionError("");
        setPendingRevision(null);
        setActivityType("quiz");
        setActivityCount(8);
        setActivityError("");
        setActivitySuccess("");
        setTeacherVideoError("");
        setTeacherVideoSuccess("");
        setActiveView("lesson");
        setActivityDrafts({});
        setActivitySaveError("");
        setActivitySaveSuccess("");
        setAssetUrl("");
        setAssetError("");
        setIsAddingAsset(false);
        setIsRightPanelCollapsed(false);
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [initialHtml, lesson?.id, lessonResetKey]);

    useEffect(() => {
        if (!open) {
            setActivityDrafts({});
            setActivitySaveError("");
            setActivitySaveSuccess("");
            setPendingRevision(null);
            setRevisionError("");
            setActivityError("");
            setActivitySuccess("");
        }
    }, [open]);

    useEffect(() => {
        if (isEditing) {
            setIsRightPanelCollapsed(false);
        }
    }, [isEditing]);

    // Resolve the cover-image preview URL through the batch endpoint so opening a lesson dialog
    // does not mix the old single-preview request with the assets batch request.
    const coverPreviewStorageKeys = draftCoverImage.storageKey ? [draftCoverImage.storageKey] : [];
    const { data: coverPreviewUrlByStorageKey } = useFilePreviewUrls(coverPreviewStorageKeys);
    const coverPreviewSrc = draftCoverImage.storageKey
        ? coverPreviewUrlByStorageKey?.[draftCoverImage.storageKey]
        : undefined;

    // Poll teacher-video status while a render is in flight.
    useEffect(() => {
        if (
            !open ||
            !lesson?.id ||
            !teacherVideoPollingVideoId ||
            !teacherVideoActiveStatuses.has(teacherVideoPollingStatus || "")
        ) {
            return undefined;
        }

        let isCancelled = false;

        const intervalId = window.setInterval(async () => {
            try {
                const data = await teacherVideoM.refresh.mutateAsync(lesson.id);
                if (!isCancelled && data?.lesson) {
                    await onLessonUpdated?.(data.lesson as unknown as LibraryLesson, { silent: true });
                    if ((data as any).teacherVideo?.status === "completed") {
                        setTeacherVideoSuccess("Teacher video is ready.");
                    }
                }
            } catch (error) {
                if (!isCancelled) {
                    console.error("Failed to refresh teacher video:", error);
                    setTeacherVideoError(error instanceof Error ? error.message : "Failed to refresh teacher video.");
                }
            }
        }, 10000);

        return () => {
            isCancelled = true;
            window.clearInterval(intervalId);
        };
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [lesson?.id, onLessonUpdated, open, teacherVideoPollingStatus, teacherVideoPollingVideoId]);

    // Refresh a signed teacher-video URL before it expires.
    useEffect(() => {
        if (
            !open ||
            isCheckingTeacherVideo ||
            !shouldRefreshTeacherVideoUrl({
                status: teacherVideoAutoRefreshStatus,
                videoId: teacherVideoAutoRefreshVideoId,
                videoUrl: teacherVideoAutoRefreshVideoUrl,
            })
        ) {
            return undefined;
        }

        let isCancelled = false;

        (async () => {
            try {
                setTeacherVideoError("");
                const data = await teacherVideoM.refresh.mutateAsync(lesson!.id);
                if (!isCancelled && data?.lesson) {
                    await onLessonUpdated?.(data.lesson as unknown as LibraryLesson, { silent: true });
                }
            } catch (error) {
                if (!isCancelled) {
                    console.error("Failed to refresh expiring teacher video URL:", error);
                    setTeacherVideoError(error instanceof Error ? error.message : "Failed to refresh teacher video.");
                }
            }
        })();

        return () => {
            isCancelled = true;
        };
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [
        isCheckingTeacherVideo,
        lesson?.id,
        onLessonUpdated,
        open,
        teacherVideoAutoRefreshStatus,
        teacherVideoAutoRefreshVideoId,
        teacherVideoAutoRefreshVideoUrl,
    ]);

    const rawActivities = Array.isArray(lesson?.activities) ? (lesson!.activities as unknown as DialogActivity[]) : [];

    useEffect(() => {
        const canOpenActivities = canManageActivities;
        const hasQuiz = rawActivities.some((activity) => activity.type === "quiz");
        const hasFlashcards = rawActivities.some((activity) => activity.type === "flashcards");

        if (
            ((activeView === "quiz" || activeView === "flashcards") && !canOpenActivities) ||
            (activeView === "quiz" && !hasQuiz) ||
            (activeView === "flashcards" && !hasFlashcards)
        ) {
            setActiveView("lesson");
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [activeView, canManageActivities, lesson?.activities]);

    if (!lesson) {
        return null;
    }

    const preparedMaterials = (metadata.preparedMaterials || {}) as Record<string, any>;
    const legacyMetadataReferences = (preparedMaterials.sourceReferences || []) as SourceReference[];
    // Priority: typed contract (lesson.sourceReferences) → legacy generationMetadata → lookup by materialIds.
    // The materialIds branch reads the already-loaded materials cache only — no extra request.
    const cachedMaterials = queryClient.getQueryData<LibraryMaterial[]>(libraryQueryKeys.materials);
    const sourceReferences: SourceReference[] =
        lesson.sourceReferences && lesson.sourceReferences.length > 0
            ? (lesson.sourceReferences as unknown as SourceReference[])
            : legacyMetadataReferences.length > 0
              ? legacyMetadataReferences
              : ((lesson.materialIds || [])
                    .map((id) => cachedMaterials?.find((material) => material.id === id))
                    .filter(Boolean) as unknown as SourceReference[]);
    const sourceAttachments = getSourceAttachments(sourceReferences as unknown as Array<Record<string, unknown>>);
    const lessonAssets = ((lesson.lessonAssets || []) as Array<Record<string, any>>).map(normalizeLessonAssetForCard);
    const allAssets = [...lessonAssets, ...sourceAttachments];
    const revisionHistory = Array.isArray(metadata.revisionHistory) ? (metadata.revisionHistory as any[]) : [];
    const lastRevision = revisionHistory[revisionHistory.length - 1] || null;
    const activities = rawActivities;
    // Prefer explicit parent-computed permission props. Never fall back to a stale
    // lesson.viewerCanManage flag that may be wrong for Member viewers.
    const canManageCurrentLesson = Boolean(canManageLesson);
    const canManageCurrentActivities = Boolean(canManageActivities);
    const canGenerateTeacherVideo = Boolean(lesson.viewerCanGenerateTeacherVideo);
    // Each "Generate quiz"/"Generate flashcards" click creates a new activity row rather than
    // replacing an existing one, so a lesson can have several quizzes/flashcard sets. Sort by id
    // (creation order) so numbering and tab order stay stable as new ones are added.
    const quizActivities = activities.filter((activity) => activity.type === "quiz").sort((a, b) => a.id - b.id);
    const flashcardsActivities = activities
        .filter((activity) => activity.type === "flashcards")
        .sort((a, b) => a.id - b.id);
    const quizActivity =
        quizActivities.find((activity) => activity.id === activeQuizActivityId) || quizActivities[0] || null;
    const flashcardsActivity =
        flashcardsActivities.find((activity) => activity.id === activeFlashcardsActivityId) ||
        flashcardsActivities[0] ||
        null;
    const activeActivity =
        canManageCurrentActivities && activeView === "quiz"
            ? quizActivity
            : canManageCurrentActivities && activeView === "flashcards"
              ? flashcardsActivity
              : null;
    const activeActivityDraft = activeActivity
        ? activityDrafts[activeActivity.id] || createActivityDraft(activeActivity)
        : null;
    const activitySettings = getActivityTypeSettings(activityType);
    const hasAssets = allAssets.length > 0;
    const isRightPanelVisible = !isRightPanelCollapsed;
    const isLessonArchived = lesson.isArchived || lesson.publicationStatus === "archived";
    const canPublishLesson =
        lesson.status === "ready" &&
        !lesson.isPublished &&
        !isLessonArchived &&
        (canPublish || canManageCurrentLesson);
    const lastEditedAt = lessonExtra?.updatedAt || lessonExtra?.createdAt;
    const lastEditedLabel = lastEditedAt ? `Edited ${formatDateTime(lastEditedAt)}` : "Edited date unknown";
    const visibleTags = isEditing ? draftTags : normalizeLessonTagInput(lesson.tags || []);
    const isTeacherVideoActive = teacherVideoActiveStatuses.has(teacherVideo.status || "");
    const teacherVideoStatusLabel = teacherVideo.status ? teacherVideo.status.replace(/_/g, " ") : "not generated";

    const handleSave = async () => {
        const validationError = validateLessonTitleAndContent(draftTitle, draftHtml);
        if (validationError) {
            onValidationError?.(validationError);
            return;
        }

        try {
            const data = await updateLessonM.mutateAsync({
                id: lesson.id,
                payload: {
                    title: draftTitle.trim(),
                    contentHtml: draftHtml,
                    tags: draftTags,
                    coverImageStorageKey: draftCoverImage.storageKey,
                    coverImageOriginalName: draftCoverImage.originalName,
                    coverImageMimeType: draftCoverImage.mimeType,
                },
            });
            if (data?.lesson) {
                await onLessonUpdated?.(data.lesson as unknown as LibraryLesson);
            }
            setIsEditing(false);
        } catch (error) {
            console.error("Failed to save lesson:", error);
            onValidationError?.(error instanceof Error ? error.message : "Failed to save lesson.");
        }
    };

    const handlePublish = async () => {
        const title = isEditing ? draftTitle : lesson.title || "";
        const contentHtml = isEditing ? draftHtml : lesson.contentHtml || "";
        const contentMarkdown = isEditing ? "" : lesson.contentMarkdown || "";
        const validationError = validateLessonTitleAndContent(title, contentHtml, contentMarkdown);
        if (validationError) {
            const msg = isEditing
                ? `${validationError} Save a complete lesson before publishing.`
                : `${validationError} Add a title and content before publishing.`;
            onValidationError?.(msg);
            return;
        }

        if (isEditing && isLessonEditDirty) {
            onValidationError?.("Save your changes before publishing.");
            return;
        }

        try {
            const data = await changeStatusM.mutateAsync({ id: lesson.id, payload: { action: "publish" } });
            if (data?.lesson) {
                await onLessonUpdated?.(data.lesson as unknown as LibraryLesson);
            }
        } catch (error) {
            console.error("Failed to publish lesson:", error);
            onValidationError?.(error instanceof Error ? error.message : "Failed to publish lesson.");
        }
    };

    const handleArchive = async () => {
        try {
            setArchiveError("");
            const data = await changeStatusM.mutateAsync({ id: lesson.id, payload: { action: "archive" } });
            if (data?.lesson) {
                await onLessonUpdated?.(data.lesson as unknown as LibraryLesson);
            }
            setIsConfirmArchiveOpen(false);
        } catch (error) {
            console.error("Failed to archive lesson:", error);
            setArchiveError(error instanceof Error ? error.message : "Failed to archive lesson.");
        }
    };

    const handleRestore = async () => {
        try {
            setRevisionError("");
            const data = await changeStatusM.mutateAsync({ id: lesson.id, payload: { action: "restore" } });
            if (data?.lesson) {
                await onLessonUpdated?.(data.lesson as unknown as LibraryLesson);
            }
        } catch (error) {
            console.error("Failed to restore lesson:", error);
            setRevisionError(error instanceof Error ? error.message : "Failed to restore lesson.");
        }
    };

    const resetLessonDrafts = () => {
        setDraftHtml(initialHtml);
        setDraftTitle(clampEntityTitle(lesson.title || ""));
        setDraftTags(normalizeLessonTagInput(lesson.tags || []));
        setDraftCoverImage({
            storageKey: lesson.coverImageStorageKey || "",
            originalName: lessonExtra?.coverImageOriginalName || "",
            mimeType: lessonExtra?.coverImageMimeType || "",
        });
        setCoverError("");
        setIsEditing(false);
    };

    const isLessonEditDirty =
        isEditing &&
        (draftHtml !== initialHtml ||
            draftTitle !== (lesson.title || "") ||
            JSON.stringify(draftTags) !== JSON.stringify(normalizeLessonTagInput(lesson.tags || [])) ||
            draftCoverImage.storageKey !== (lesson.coverImageStorageKey || ""));
    const isActivityDirty = Object.keys(activityDrafts).length > 0;
    const isDirty = isLessonEditDirty || isActivityDirty;

    const handleCancelEdit = () => {
        if (isLessonEditDirty) {
            discardClosesDialogRef.current = false;
            setIsDiscardOpen(true);
            return;
        }
        resetLessonDrafts();
    };

    const requestClose = () => {
        if (isSaving || isPublishing || isArchiving || isDeleting || isRevising || isGeneratingActivity || isSavingActivity || isUploadingCover) {
            return;
        }
        if (isDirty) {
            discardClosesDialogRef.current = true;
            setIsDiscardOpen(true);
            return;
        }
        onClose();
    };

    const handleDiscardChanges = () => {
        resetLessonDrafts();
        setActivityDrafts({});
        setActivitySaveError("");
        setActivitySaveSuccess("");
        setIsDiscardOpen(false);
        if (discardClosesDialogRef.current) {
            discardClosesDialogRef.current = false;
            onClose();
        }
    };

    const handleCoverImageChange = async (file?: File | null) => {
        if (!file) {
            return;
        }

        if (!file.type?.startsWith("image/")) {
            setCoverError("Choose an image file.");
            if (coverFileInputRef.current) {
                coverFileInputRef.current.value = "";
            }
            return;
        }

        if (file.size > 10 * 1024 * 1024) {
            setCoverError("Cover image must be under 10 MB.");
            if (coverFileInputRef.current) {
                coverFileInputRef.current.value = "";
            }
            return;
        }

        try {
            setIsUploadingCover(true);
            setCoverError("");

            const uploadData = await uploadFileM.mutateAsync(file);

            setDraftCoverImage({
                storageKey: (uploadData as any)?.storageKey || "",
                originalName: file.name,
                mimeType: file.type || "image/*",
            });
        } catch (error) {
            console.error("Failed to upload lesson cover image:", error);
            setCoverError(error instanceof Error ? error.message : "Failed to upload cover image.");
        } finally {
            setIsUploadingCover(false);
            if (coverFileInputRef.current) {
                coverFileInputRef.current.value = "";
            }
        }
    };

    const handleRemoveCoverImage = () => {
        setDraftCoverImage({ storageKey: "", originalName: "", mimeType: "" });
        setCoverError("");
    };

    const handleAddUrlAsset = async () => {
        if (!assetUrl.trim()) {
            setAssetError("Add a link or YouTube URL first.");
            return;
        }

        try {
            setIsAddingAsset(true);
            setAssetError("");

            const data = await addAssetM.mutateAsync({
                lessonId: lesson.id,
                payload: { kind: "url", url: assetUrl.trim() },
            });

            setAssetUrl("");
            if (data?.lesson) {
                await onLessonUpdated?.(data.lesson as unknown as LibraryLesson);
            }
        } catch (error) {
            console.error("Failed to add lesson URL asset:", error);
            setAssetError(error instanceof Error ? error.message : "Failed to add asset.");
        } finally {
            setIsAddingAsset(false);
        }
    };

    const handleAddFileAsset = async (file?: File | null) => {
        if (!file) {
            return;
        }

        const isVideo = isLikelyVideoFile(file);

        try {
            setIsAddingAsset(true);
            setAssetError("");

            // Videos upload directly to object storage (presigned PUT) — they routinely exceed
            // the multipart proxy's 10MB cap used for images/other files.
            const uploadData = isVideo
                ? await uploadVideoM.mutateAsync(file)
                : await uploadFileM.mutateAsync(file);

            const data = await addAssetM.mutateAsync({
                lessonId: lesson.id,
                payload: {
                    kind: isVideo ? "video" : file.type.startsWith("image/") ? "image" : "file",
                    originalName: file.name,
                    storageKey: (uploadData as any)?.storageKey || "",
                    mimeType: (uploadData as any)?.mimeType || file.type || inferVideoMimeType(file.name) || "application/octet-stream",
                    sizeBytes: file.size,
                },
            });

            if (data?.lesson) {
                await onLessonUpdated?.(data.lesson as unknown as LibraryLesson);
            }
        } catch (error) {
            console.error("Failed to add lesson file asset:", error);
            const message = error instanceof Error
                ? error.message
                : isVideo ? "Failed to upload video." : "Failed to add file asset.";
            if (isVideo) {
                onValidationError?.(message);
            } else {
                setAssetError(message);
            }
        } finally {
            setIsAddingAsset(false);
            if (assetFileInputRef.current) {
                assetFileInputRef.current.value = "";
            }
        }
    };

    const uploadLessonImageAsset = async (file: File) => {
        if (!file || !file.type?.startsWith("image/")) {
            throw new Error("Only image files can be pasted into the editor.");
        }

        try {
            setIsAddingAsset(true);
            setAssetError("");

            const uploadData = await uploadFileM.mutateAsync(file);
            const storageKey = (uploadData as any)?.storageKey || "";

            const assetData = await addAssetM.mutateAsync({
                lessonId: lesson.id,
                payload: {
                    kind: "image",
                    originalName: file.name,
                    storageKey,
                    mimeType: file.type || "application/octet-stream",
                    sizeBytes: file.size,
                },
            });

            if (assetData?.lesson) {
                await onLessonUpdated?.(assetData.lesson as unknown as LibraryLesson);
            }

            const previewUrl = await fetchFilePreviewUrl(storageKey);

            return { src: previewUrl, alt: file.name, title: file.name, storageKey };
        } catch (error) {
            console.error("Failed to paste lesson image asset:", error);
            setAssetError(error instanceof Error ? error.message : "Failed to paste image asset.");
            throw error;
        } finally {
            setIsAddingAsset(false);
        }
    };

    const uploadLessonVideoAsset = async (file: File) => {
        if (!file || !isLikelyVideoFile(file)) {
            throw new Error("Only video files can be inserted into the editor.");
        }

        try {
            setIsAddingAsset(true);
            setAssetError("");

            const uploadData = await uploadVideoM.mutateAsync(file);
            const storageKey = (uploadData as any)?.storageKey || "";

            const assetData = await addAssetM.mutateAsync({
                lessonId: lesson.id,
                payload: {
                    kind: "video",
                    originalName: file.name,
                    storageKey,
                    mimeType: (uploadData as any)?.mimeType || file.type || inferVideoMimeType(file.name) || "application/octet-stream",
                    sizeBytes: file.size,
                },
            });

            if (assetData?.lesson) {
                await onLessonUpdated?.(assetData.lesson as unknown as LibraryLesson);
            }

            const previewUrl = await fetchFilePreviewUrl(storageKey);

            return { src: previewUrl, title: file.name, storageKey };
        } catch (error) {
            console.error("Failed to insert lesson video asset:", error);
            const message = error instanceof Error ? error.message : "Failed to insert video asset.";
            onValidationError?.(message);
            throw error;
        } finally {
            setIsAddingAsset(false);
        }
    };

    const handleDeleteLessonAsset = async (attachment: { id?: string | number; name?: string; storageKey?: string }) => {
        const assetId = Number(attachment.id);
        if (!Number.isFinite(assetId)) {
            setAssetError("Asset id is missing.");
            return;
        }

        try {
            setAssetError("");
            setDeletingAssetId(attachment.id ?? assetId);
            const data = await deleteAssetM.mutateAsync({ lessonId: lesson.id, assetId });
            if (data?.lesson) {
                await onLessonUpdated?.(data.lesson as unknown as LibraryLesson);
            }
            if (attachment.storageKey) {
                setDraftHtml((currentHtml) => stripStorageKeyFromHtml(currentHtml, attachment.storageKey!));
            }
        } catch (error) {
            console.error("Failed to delete lesson asset:", error);
            setAssetError(error instanceof Error ? error.message : "Failed to delete asset.");
        } finally {
            setDeletingAssetId(null);
        }
    };

    const handleDelete = async () => {
        try {
            setDeleteError("");
            await deleteLessonM.mutateAsync(lesson.id);
            await onLessonDeleted?.(lesson.id);
            setIsConfirmDeleteOpen(false);
        } catch (error) {
            console.error("Failed to delete lesson:", error);
            setDeleteError(error instanceof Error ? error.message : "Failed to delete lesson.");
        }
    };

    const handleToggleRevisionOption = (value: string) => {
        setSelectedRevisionOptions((prev) =>
            prev.includes(value) ? prev.filter((item) => item !== value) : [...prev, value],
        );
    };

    const handleRevise = async () => {
        if (!revisionRequest.trim() && selectedRevisionOptions.length === 0) {
            setRevisionError("Add revision notes or select at least one revision option.");
            return;
        }

        try {
            setRevisionError("");
            const previousHtml = lesson.contentHtml || markdownToHtml(lesson.contentMarkdown || "");
            const previousTitle = lesson.title || "";
            const previousMarkdown = lesson.contentMarkdown || "";
            const data = await reviseLessonM.mutateAsync({
                id: lesson.id,
                payload: { revisionRequest, selectedOptions: selectedRevisionOptions },
            });

            setRevisionRequest("");
            setSelectedRevisionOptions([]);
            if (data?.lesson) {
                const revisedLesson = data.lesson as unknown as LibraryLesson;
                setContentSnapshots((current) => [
                    ...current,
                    {
                        id: `${Date.now()}`,
                        label: revisionRequest.trim() || selectedRevisionOptions.join(", ") || "AI revision",
                        html: previousHtml,
                        title: previousTitle,
                        markdown: previousMarkdown,
                        createdAt: new Date().toISOString(),
                    },
                ].slice(-8));
                setPendingRevision({
                    previousHtml,
                    previousTitle,
                    previousMarkdown,
                    revisedLesson,
                });
                await onLessonUpdated?.(revisedLesson, { silent: true });
            }
        } catch (error) {
            console.error("Failed to revise lesson:", error);
            setRevisionError(error instanceof Error ? error.message : "Failed to revise lesson.");
        }
    };

    const handleAcceptRevision = () => {
        setPendingRevision(null);
    };

    const handleDiscardRevision = async () => {
        if (!pendingRevision) {
            return;
        }
        try {
            setRevisionError("");
            const data = await updateLessonM.mutateAsync({
                id: lesson.id,
                payload: {
                    title: pendingRevision.previousTitle,
                    contentHtml: pendingRevision.previousHtml,
                    tags: normalizeLessonTagInput(lesson.tags || []),
                    coverImageStorageKey: lesson.coverImageStorageKey || "",
                    coverImageOriginalName: lessonExtra?.coverImageOriginalName || "",
                    coverImageMimeType: lessonExtra?.coverImageMimeType || "",
                },
            });
            setPendingRevision(null);
            if (data?.lesson) {
                await onLessonUpdated?.(data.lesson as unknown as LibraryLesson);
            }
        } catch (error) {
            console.error("Failed to discard revision:", error);
            setRevisionError(error instanceof Error ? error.message : "Failed to discard revision.");
        }
    };

    const handleRestoreSnapshot = async (snapshotId: string) => {
        const snapshot = contentSnapshots.find((entry) => entry.id === snapshotId);
        if (!snapshot) {
            return;
        }
        try {
            setRevisionError("");
            const data = await updateLessonM.mutateAsync({
                id: lesson.id,
                payload: {
                    title: snapshot.title,
                    contentHtml: snapshot.html,
                    tags: normalizeLessonTagInput(lesson.tags || []),
                    coverImageStorageKey: lesson.coverImageStorageKey || "",
                    coverImageOriginalName: lessonExtra?.coverImageOriginalName || "",
                    coverImageMimeType: lessonExtra?.coverImageMimeType || "",
                },
            });
            setPendingRevision(null);
            if (data?.lesson) {
                await onLessonUpdated?.(data.lesson as unknown as LibraryLesson);
            }
        } catch (error) {
            console.error("Failed to restore revision:", error);
            setRevisionError(error instanceof Error ? error.message : "Failed to restore version.");
        }
    };

    const getLessonContentLength = () => {
        const text = (lesson.contentHtml || lesson.contentMarkdown || "")
            .replace(/<[^>]+>/g, " ")
            .replace(/\s+/g, " ")
            .trim();
        return text.length;
    };

    const focusActivity = (type: "quiz" | "flashcards", activityId: number) => {
        setActiveView(type);
        if (type === "quiz") {
            setActiveQuizActivityId(activityId);
        } else {
            setActiveFlashcardsActivityId(activityId);
        }
    };

    const handleActivityTypeChange = (nextType: "quiz" | "flashcards") => {
        const nextSettings = getActivityTypeSettings(nextType);

        setActivityType(nextType);
        setActivityCount(nextSettings.defaultCount);
        setActivityError("");
        setActivitySuccess("");
    };

    const runGenerateActivity = async () => {
        const normalizedCount = Number.parseInt(String(activityCount), 10);

        if (Number.isNaN(normalizedCount) || normalizedCount < activitySettings.min || normalizedCount > activitySettings.max) {
            setActivityError(`Choose a number between ${activitySettings.min} and ${activitySettings.max}.`);
            return;
        }

        try {
            setActivityError("");
            setActivitySuccess("");
            setThinContentConfirmOpen(false);
            setGenerateConfirm(null);

            const data = await generateActivityM.mutateAsync({
                id: lesson.id,
                payload: { type: activityType, count: normalizedCount },
            });

            setActivitySuccess("Activity generated and saved.");
            if (data?.lesson) {
                const updatedActivities = Array.isArray((data.lesson as any)?.activities)
                    ? ((data.lesson as any).activities as DialogActivity[])
                    : [];
                const newestOfType = updatedActivities
                    .filter((activity) => activity.type === activityType)
                    .sort((a, b) => b.id - a.id)[0];
                await onLessonUpdated?.(data.lesson as unknown as LibraryLesson);
                if (newestOfType) {
                    focusActivity(activityType, newestOfType.id);
                }
            } else if ((data as any)?.activity) {
                const activity = (data as any).activity as DialogActivity;
                await onLessonUpdated?.({
                    ...lesson,
                    activities: [activity, ...activities] as unknown as LibraryLesson["activities"],
                } as LibraryLesson);
                focusActivity(activity.type as "quiz" | "flashcards", activity.id);
            }
        } catch (error) {
            console.error("Failed to generate lesson activity:", error);
            setActivityError(error instanceof Error ? error.message : "Failed to generate activity.");
        }
    };

    const handleGenerateActivity = async () => {
        const normalizedCount = Number.parseInt(String(activityCount), 10);

        if (Number.isNaN(normalizedCount) || normalizedCount < activitySettings.min || normalizedCount > activitySettings.max) {
            setActivityError(`Choose a number between ${activitySettings.min} and ${activitySettings.max}.`);
            return;
        }

        if (getLessonContentLength() < MIN_LESSON_CONTENT_CHARS) {
            setThinContentConfirmOpen(true);
            return;
        }

        const existingOfType = activities.filter((activity) => activity.type === activityType);
        if (existingOfType.length > 0) {
            setGenerateConfirm("append");
            return;
        }

        await runGenerateActivity();
    };

    const runReplaceActivity = async () => {
        const idsToReplace = activities
            .filter((activity) => activity.type === activityType)
            .map((activity) => activity.id);

        setIsReplacingActivity(true);
        try {
            await runGenerateActivity();
            for (const activityId of idsToReplace) {
                try {
                    const data = await deleteActivityM.mutateAsync({ lessonId: lesson.id, activityId });
                    if (data?.activities) {
                        await onLessonUpdated?.({
                            ...lesson,
                            activities: data.activities as unknown as LibraryLesson["activities"],
                        } as LibraryLesson);
                    }
                } catch (error) {
                    console.error("Failed to remove previous activity:", error);
                    setActivityError(
                        error instanceof Error ? error.message : "Failed to remove the previous activity.",
                    );
                }
            }
        } finally {
            setIsReplacingActivity(false);
        }
    };

    const handleGenerateTeacherVideo = async () => {
        try {
            setTeacherVideoError("");
            setTeacherVideoSuccess("");

            const data = await teacherVideoM.generate.mutateAsync(lesson.id);

            setTeacherVideoSuccess("Teacher video generation started. Status will refresh automatically.");
            if (data?.lesson) {
                await onLessonUpdated?.(data.lesson as unknown as LibraryLesson, { silent: true });
            }
        } catch (error) {
            console.error("Failed to generate teacher video:", error);
            setTeacherVideoError(error instanceof Error ? error.message : "Failed to generate teacher video.");
        }
    };

    const handleRefreshTeacherVideo = async () => {
        if (!teacherVideo.videoId) {
            return;
        }

        try {
            setTeacherVideoError("");
            const data = await teacherVideoM.refresh.mutateAsync(lesson.id);

            if ((data as any)?.teacherVideo?.status === "completed") {
                setTeacherVideoSuccess("Teacher video is ready.");
            }
            if (data?.lesson) {
                await onLessonUpdated?.(data.lesson as unknown as LibraryLesson, { silent: true });
            }
        } catch (error) {
            console.error("Failed to refresh teacher video:", error);
            setTeacherVideoError(error instanceof Error ? error.message : "Failed to refresh teacher video.");
        }
    };

    const handleDeleteTeacherVideo = async () => {
        if (!teacherVideo.videoId && !teacherVideo.videoUrl) {
            return;
        }

        try {
            setTeacherVideoError("");
            setTeacherVideoSuccess("");

            const data = (await teacherVideoM.remove.mutateAsync(lesson.id)) as { lesson?: unknown };

            setTeacherVideoSuccess("Teacher video removed from this lesson.");
            if (data?.lesson) {
                await onLessonUpdated?.(data.lesson as unknown as LibraryLesson, { silent: true });
            }
        } catch (error) {
            console.error("Failed to remove teacher video:", error);
            setTeacherVideoError(error instanceof Error ? error.message : "Failed to remove teacher video.");
        }
    };

    const handleSaveActivity = async () => {
        if (!activeActivity || !activeActivityDraft) {
            return;
        }

        try {
            setActivitySaveError("");
            setActivitySaveSuccess("");

            if (activeActivityDraft.type === "flashcards") {
                const validationError = validateFlashcardsDraft(activeActivityDraft as FlashcardsDraft);
                if (validationError) {
                    setActivitySaveError(validationError);
                    return;
                }
            } else {
                const validationError = validateQuizDraft(activeActivityDraft as QuizDraft);
                if (validationError) {
                    setActivitySaveError(validationError);
                    return;
                }
            }

            const payload =
                activeActivityDraft.type === "flashcards"
                    ? {
                          title: (activeActivityDraft as FlashcardsDraft).title,
                          cards: (activeActivityDraft as FlashcardsDraft).cards,
                      }
                    : {
                          title: (activeActivityDraft as QuizDraft).title,
                          items: toQuizSaveItems(activeActivityDraft as QuizDraft) as never,
                      };

            const data = await updateActivityM.mutateAsync({
                lessonId: lesson.id,
                activityId: activeActivity.id,
                payload,
            });

            setActivityDrafts((current) => {
                const nextDrafts = { ...current };
                delete nextDrafts[activeActivity.id];
                return nextDrafts;
            });
            setActivitySaveSuccess("Activity saved.");
            const updatedActivity = (data as { activity?: DialogActivity })?.activity;
            const responseLesson = data?.lesson as LibraryLesson | undefined;
            if (responseLesson || updatedActivity) {
                const nextActivities = (
                    (responseLesson?.activities as DialogActivity[] | undefined) ||
                    activities
                ).map((activity) =>
                    updatedActivity && activity.id === updatedActivity.id
                        ? { ...activity, ...updatedActivity }
                        : activity,
                );
                if (updatedActivity && !nextActivities.some((activity) => activity.id === updatedActivity.id)) {
                    nextActivities.unshift(updatedActivity);
                }
                await onLessonUpdated?.({
                    ...(responseLesson || lesson),
                    activities: nextActivities as unknown as LibraryLesson["activities"],
                } as LibraryLesson);
            }
        } catch (error) {
            console.error("Failed to save lesson activity:", error);
            setActivitySaveError(error instanceof Error ? error.message : "Failed to update activity.");
        }
    };

    return (
        <>
        <Dialog
            open={open}
            onClose={requestClose}
            fullWidth
            maxWidth="xl"
            slotProps={{
                paper: {
                    sx: {
                        width: "calc(100vw - 40px)",
                        maxWidth: 1180,
                        height: "calc(100vh - 50px)",
                        maxHeight: 760,
                        borderRadius: "20px",
                        display: "flex",
                        flexDirection: "column",
                        minHeight: 0,
                        overflow: "hidden",
                        backgroundColor: "#fff",
                        border: 0,
                        boxShadow: "0 40px 80px rgba(11, 11, 11, 0.25)",
                    },
                },
            }}
        >
            <DialogContent
                dividers
                sx={{
                    flex: "1 1 auto",
                    height: 0,
                    minHeight: 0,
                    overflow: "hidden",
                    display: "flex",
                    flexDirection: "column",
                    p: 0,
                    borderColor: LESSON_DIALOG_COLORS.blue100,
                    backgroundColor: "#fff",
                }}
            >
                <Box
                    sx={{
                        display: "grid",
                        gridTemplateColumns: {
                            xs: "1fr",
                            lg: activeView === "lesson" && isRightPanelVisible ? "minmax(0, 1fr) 340px" : "minmax(0, 1fr)",
                        },
                        gap: 0,
                        alignItems: "stretch",
                        flex: "1 1 auto",
                        height: "100%",
                        minHeight: 0,
                        overflow: "hidden",
                    }}
                >
                    <Paper
                        elevation={0}
                        sx={{
                            borderRadius: 0,
                            border: 0,
                            backgroundColor: "#fff",
                            minHeight: 0,
                            overflow: "hidden",
                            display: "flex",
                            flexDirection: "column",
                            boxShadow: "none",
                        }}
                    >
                        <Box
                            sx={{
                                flex: "0 0 auto",
                                display: "flex",
                                alignItems: "center",
                                justifyContent: "space-between",
                                px: { xs: 2, md: 3.5 },
                                py: 2.5,
                                minHeight: 76,
                                borderBottom: `1px solid ${LESSON_DIALOG_COLORS.blue100}`,
                                backgroundColor: "#fff",
                            }}
                        >
                            <Tabs
                                value={activeView}
                                onChange={(_event, nextView) => {
                                    setActiveView(nextView);
                                    setActivitySaveError("");
                                    setActivitySaveSuccess("");
                                }}
                                variant="scrollable"
                                scrollButtons={false}
                                sx={{
                                    minHeight: 32,
                                    "& .MuiTabs-indicator": { display: "none" },
                                    "& .MuiTabs-flexContainer": { gap: 0.5 },
                                    "& .MuiTab-root": {
                                        minHeight: 32,
                                        minWidth: 0,
                                        px: 1.75,
                                        py: 1,
                                        borderRadius: 999,
                                        color: LESSON_DIALOG_COLORS.mute,
                                        fontSize: 12,
                                        fontWeight: 700,
                                        letterSpacing: "0.04em",
                                        lineHeight: 1,
                                        textTransform: "uppercase",
                                        transition: "background-color 120ms ease, color 120ms ease",
                                    },
                                    "& .Mui-selected": {
                                        color: LESSON_DIALOG_COLORS.blue,
                                        backgroundColor: LESSON_DIALOG_COLORS.blue50,
                                    },
                                }}
                            >
                                <Tab value="lesson" label="Reading" />
                                {canManageCurrentActivities && flashcardsActivities.length > 0 && (
                                    <Tab
                                        value="flashcards"
                                        label={
                                            flashcardsActivities.length > 1
                                                ? `Flashcards (${flashcardsActivities.length})`
                                                : "Flashcards"
                                        }
                                    />
                                )}
                                {canManageCurrentActivities && quizActivities.length > 0 && (
                                    <Tab
                                        value="quiz"
                                        label={
                                            quizActivities.length > 1
                                                ? `Quiz (${quizActivities.length})`
                                                : "Quiz"
                                        }
                                    />
                                )}
                            </Tabs>

                            <Stack direction="row" spacing={1} sx={{ alignItems: "center" }}>
                                <Box
                                    sx={{
                                        display: { xs: "none", sm: "flex" },
                                        alignItems: "center",
                                        gap: 1,
                                        minHeight: 30,
                                        px: 1.5,
                                        borderRadius: 999,
                                        color: LESSON_DIALOG_COLORS.success,
                                        backgroundColor: hexToRgba(LESSON_DIALOG_COLORS.success, 0.1),
                                    }}
                                >
                                    <Box
                                        sx={{
                                            width: 6,
                                            height: 6,
                                            flex: "0 0 auto",
                                            borderRadius: "50%",
                                            backgroundColor: LESSON_DIALOG_COLORS.success,
                                        }}
                                    />
                                    <Typography
                                        component="span"
                                        sx={{
                                            color: LESSON_DIALOG_COLORS.success,
                                            fontSize: 11,
                                            fontWeight: 700,
                                            letterSpacing: "0.04em",
                                            lineHeight: 1,
                                            textTransform: "uppercase",
                                            whiteSpace: "nowrap",
                                        }}
                                    >
                                        {lastEditedLabel}
                                    </Typography>
                                </Box>

                                {isEditing && (
                                    <>
                                        <Tooltip title={coverError || (coverPreviewSrc ? "Replace cover image" : "Upload cover image")}>
                                            <span>
                                                <IconButton
                                                    size="small"
                                                    aria-label={coverPreviewSrc ? "Replace lesson cover image" : "Upload lesson cover image"}
                                                    onClick={() => coverFileInputRef.current?.click()}
                                                    disabled={!canManageCurrentLesson || isUploadingCover || isSaving || isDeleting}
                                                    sx={{
                                                        width: 34,
                                                        height: 34,
                                                        borderRadius: 999,
                                                        color: coverPreviewSrc ? "#fff" : LESSON_DIALOG_COLORS.blue,
                                                        border: `1px solid ${LESSON_DIALOG_COLORS.blue200}`,
                                                        backgroundColor: coverPreviewSrc ? LESSON_DIALOG_COLORS.blue : "#fff",
                                                        backgroundImage: coverPreviewSrc ? `url("${coverPreviewSrc}")` : "none",
                                                        backgroundSize: "cover",
                                                        backgroundPosition: "center",
                                                        boxShadow: coverPreviewSrc ? "inset 0 0 0 999px rgba(0, 9, 220, 0.28)" : "none",
                                                        "&:hover": {
                                                            color: "#fff",
                                                            backgroundColor: LESSON_DIALOG_COLORS.blue,
                                                            boxShadow: coverPreviewSrc ? "inset 0 0 0 999px rgba(0, 9, 220, 0.38)" : "none",
                                                        },
                                                        "&.Mui-disabled": {
                                                            color: LESSON_DIALOG_COLORS.mute,
                                                            backgroundColor: "#fff",
                                                            backgroundImage: "none",
                                                        },
                                                    }}
                                                >
                                                    <ImageOutlinedIcon sx={{ fontSize: 17 }} />
                                                </IconButton>
                                            </span>
                                        </Tooltip>
                                        {coverPreviewSrc && (
                                            <Tooltip title="Remove cover image">
                                                <span>
                                                    <IconButton
                                                        size="small"
                                                        aria-label="Remove lesson cover image"
                                                        onClick={handleRemoveCoverImage}
                                                        disabled={!canManageCurrentLesson || isUploadingCover || isSaving || isDeleting}
                                                        sx={{
                                                            width: 34,
                                                            height: 34,
                                                            borderRadius: 999,
                                                            color: "#D62F2F",
                                                            border: "1px solid rgba(214, 47, 47, 0.28)",
                                                            backgroundColor: "#fff",
                                                            "&:hover": {
                                                                borderColor: "#D62F2F",
                                                                backgroundColor: "rgba(214, 47, 47, 0.05)",
                                                            },
                                                            "&.Mui-disabled": {
                                                                color: LESSON_DIALOG_COLORS.mute,
                                                                backgroundColor: "#fff",
                                                            },
                                                        }}
                                                    >
                                                        <DeleteOutlineOutlinedIcon sx={{ fontSize: 17 }} />
                                                    </IconButton>
                                                </span>
                                            </Tooltip>
                                        )}
                                        <Box
                                            component="input"
                                            type="file"
                                            accept="image/*"
                                            ref={coverFileInputRef}
                                            onChange={(event) =>
                                                handleCoverImageChange((event.target as HTMLInputElement).files?.[0])
                                            }
                                            sx={{ display: "none" }}
                                        />
                                    </>
                                )}

                                <Tooltip title={isRightPanelCollapsed ? "Show sidebar" : "Hide sidebar"}>
                                    <IconButton
                                        size="small"
                                        aria-label={isRightPanelCollapsed ? "Show lesson sidebar" : "Hide lesson sidebar"}
                                        onClick={() => setIsRightPanelCollapsed((prev) => !prev)}
                                        sx={{
                                            width: 30,
                                            height: 30,
                                            borderRadius: 999,
                                            color: isRightPanelCollapsed ? LESSON_DIALOG_COLORS.blue : "#fff",
                                            border: `1px solid ${LESSON_DIALOG_COLORS.blue200}`,
                                            backgroundColor: isRightPanelCollapsed ? "#fff" : LESSON_DIALOG_COLORS.blue,
                                            "&:hover": {
                                                backgroundColor: isRightPanelCollapsed ? LESSON_DIALOG_COLORS.blue50 : LESSON_DIALOG_COLORS.blue,
                                            },
                                        }}
                                    >
                                        <ViewSidebarOutlinedIcon fontSize="small" />
                                    </IconButton>
                                </Tooltip>

                                <Tooltip title="Close">
                                    <IconButton
                                        size="small"
                                        aria-label="Close lesson details"
                                        onClick={requestClose}
                                        disabled={
                                            isSaving ||
                                            isPublishing ||
                                            isArchiving ||
                                            isDeleting ||
                                            isRevising ||
                                            isGeneratingActivity ||
                                            isSavingActivity ||
                                            isUploadingCover
                                        }
                                        sx={{
                                            width: 34,
                                            height: 34,
                                            borderRadius: 999,
                                            color: LESSON_DIALOG_COLORS.ink,
                                            border: `1px solid ${LESSON_DIALOG_COLORS.blue200}`,
                                            backgroundColor: "#fff",
                                            "&:hover": {
                                                color: LESSON_DIALOG_COLORS.blue,
                                                backgroundColor: LESSON_DIALOG_COLORS.blue50,
                                            },
                                            "&.Mui-disabled": {
                                                color: LESSON_DIALOG_COLORS.mute,
                                                backgroundColor: "#fff",
                                            },
                                        }}
                                    >
                                        <CloseOutlinedIcon sx={{ fontSize: 17 }} />
                                    </IconButton>
                                </Tooltip>
                            </Stack>
                        </Box>

                        {activeView === "quiz" && quizActivities.length > 1 && (
                            <Box
                                sx={{
                                    flex: "0 0 auto",
                                    display: "flex",
                                    alignItems: "center",
                                    gap: 1.25,
                                    px: { xs: 2, md: 3.5 },
                                    py: 1.25,
                                    borderBottom: `1px solid ${LESSON_DIALOG_COLORS.blue100}`,
                                    backgroundColor: LESSON_DIALOG_COLORS.blue50,
                                }}
                            >
                                <Typography
                                    sx={{
                                        color: LESSON_DIALOG_COLORS.mute,
                                        fontSize: 11,
                                        fontWeight: 800,
                                        letterSpacing: "0.06em",
                                        textTransform: "uppercase",
                                        whiteSpace: "nowrap",
                                    }}
                                >
                                    Select quiz
                                </Typography>
                                <Box
                                    sx={{
                                        display: "inline-flex",
                                        flexWrap: "wrap",
                                        gap: 0.5,
                                        p: 0.5,
                                        borderRadius: 999,
                                        backgroundColor: "#fff",
                                        border: `1px solid ${LESSON_DIALOG_COLORS.blue100}`,
                                    }}
                                >
                                    {quizActivities.map((activity, index) => {
                                        const isActive = quizActivity?.id === activity.id;
                                        return (
                                            <Button
                                                key={activity.id}
                                                size="small"
                                                onClick={() => {
                                                    setActiveQuizActivityId(activity.id);
                                                    setActivitySaveError("");
                                                    setActivitySaveSuccess("");
                                                }}
                                                sx={{
                                                    minHeight: 30,
                                                    px: 1.5,
                                                    borderRadius: 999,
                                                    boxShadow: "none",
                                                    textTransform: "none",
                                                    fontSize: 12,
                                                    fontWeight: 700,
                                                    color: isActive ? "#fff" : LESSON_DIALOG_COLORS.blue,
                                                    backgroundColor: isActive
                                                        ? LESSON_DIALOG_COLORS.blue
                                                        : "transparent",
                                                    "&:hover": {
                                                        boxShadow: "none",
                                                        backgroundColor: isActive
                                                            ? LESSON_DIALOG_COLORS.blue
                                                            : LESSON_DIALOG_COLORS.blue50,
                                                    },
                                                }}
                                            >
                                                Quiz {index + 1}
                                                {activity.itemCount != null ? ` · ${activity.itemCount}` : ""}
                                            </Button>
                                        );
                                    })}
                                </Box>
                            </Box>
                        )}

                        {activeView === "flashcards" && flashcardsActivities.length > 1 && (
                            <Box
                                sx={{
                                    flex: "0 0 auto",
                                    display: "flex",
                                    alignItems: "center",
                                    gap: 1.25,
                                    px: { xs: 2, md: 3.5 },
                                    py: 1.25,
                                    borderBottom: `1px solid ${LESSON_DIALOG_COLORS.blue100}`,
                                    backgroundColor: LESSON_DIALOG_COLORS.blue50,
                                }}
                            >
                                <Typography
                                    sx={{
                                        color: LESSON_DIALOG_COLORS.mute,
                                        fontSize: 11,
                                        fontWeight: 800,
                                        letterSpacing: "0.06em",
                                        textTransform: "uppercase",
                                        whiteSpace: "nowrap",
                                    }}
                                >
                                    Select set
                                </Typography>
                                <Box
                                    sx={{
                                        display: "inline-flex",
                                        flexWrap: "wrap",
                                        gap: 0.5,
                                        p: 0.5,
                                        borderRadius: 999,
                                        backgroundColor: "#fff",
                                        border: `1px solid ${LESSON_DIALOG_COLORS.blue100}`,
                                    }}
                                >
                                    {flashcardsActivities.map((activity, index) => {
                                        const isActive = flashcardsActivity?.id === activity.id;
                                        return (
                                            <Button
                                                key={activity.id}
                                                size="small"
                                                onClick={() => {
                                                    setActiveFlashcardsActivityId(activity.id);
                                                    setActivitySaveError("");
                                                    setActivitySaveSuccess("");
                                                }}
                                                sx={{
                                                    minHeight: 30,
                                                    px: 1.5,
                                                    borderRadius: 999,
                                                    boxShadow: "none",
                                                    textTransform: "none",
                                                    fontSize: 12,
                                                    fontWeight: 700,
                                                    color: isActive ? "#fff" : LESSON_DIALOG_COLORS.blue,
                                                    backgroundColor: isActive
                                                        ? LESSON_DIALOG_COLORS.blue
                                                        : "transparent",
                                                    "&:hover": {
                                                        boxShadow: "none",
                                                        backgroundColor: isActive
                                                            ? LESSON_DIALOG_COLORS.blue
                                                            : LESSON_DIALOG_COLORS.blue50,
                                                    },
                                                }}
                                            >
                                                Set {index + 1}
                                                {activity.itemCount != null ? ` · ${activity.itemCount}` : ""}
                                            </Button>
                                        );
                                    })}
                                </Box>
                            </Box>
                        )}

                        {activeView !== "lesson" && canManageCurrentActivities ? (
                            <Stack sx={{ minHeight: 0, overflow: "hidden", height: "100%" }}>
                                {(activitySaveError || activitySaveSuccess) && (
                                    <Box sx={{ p: { xs: 1.5, md: 2 }, pb: 0 }}>
                                        {activitySaveError && <Alert severity="error">{activitySaveError}</Alert>}
                                        {activitySaveSuccess && <Alert severity="success">{activitySaveSuccess}</Alert>}
                                    </Box>
                                )}
                                <ActivityEditor
                                    activity={activeActivity}
                                    draft={activeActivityDraft}
                                    onDraftChange={setActivityDrafts}
                                    disabled={
                                        !canManageCurrentActivities ||
                                        isSavingActivity ||
                                        isDeleting ||
                                        isSaving ||
                                        isRevising ||
                                        isGeneratingActivity
                                    }
                                />
                            </Stack>
                        ) : lesson.status === "failed" && !isEditing ? (
                            <Stack spacing={1.5} sx={{ p: { xs: 2, md: 3 } }}>
                                <Stack direction="row" spacing={1} sx={{ alignItems: "center" }}>
                                    <ErrorOutlineOutlinedIcon color="error" />
                                    <Typography variant="h6" sx={{ fontWeight: 800 }}>
                                        Generation failed
                                    </Typography>
                                </Stack>
                                <Typography color="text.secondary">{lesson.errorMessage || "No error message was saved."}</Typography>
                            </Stack>
                        ) : (
                            <Stack sx={{ minHeight: 0, overflow: "hidden", height: "100%" }}>
                                <Box
                                    sx={{
                                        minHeight: 0,
                                        flex: "1 1 auto",
                                        overflow: isEditing ? "hidden" : "auto",
                                        display: isEditing ? "flex" : "block",
                                        flexDirection: isEditing ? "column" : undefined,
                                        backgroundColor: "#fff",
                                        cursor: isEditing ? "text" : "default",
                                    }}
                                >
                                    <Box
                                        sx={{
                                            flex: "0 0 auto",
                                            maxWidth: isEditing ? "calc(100% - 64px)" : isRightPanelVisible ? 860 : "none",
                                            mx: 0,
                                            px: { xs: 3, md: isEditing ? 5 : 7 },
                                            pt: { xs: 3, md: isEditing ? 4 : 7 },
                                            pb: 0,
                                        }}
                                    >
                                        <Typography
                                            sx={{
                                                mb: 1.25,
                                                color: LESSON_DIALOG_COLORS.blue,
                                                fontSize: 11,
                                                fontWeight: 700,
                                                letterSpacing: "0.08em",
                                                lineHeight: 1.1,
                                                textTransform: "uppercase",
                                            }}
                                        >
                                            Lesson - Preview
                                        </Typography>

                                        {isEditing ? (
                                            <Box
                                                component="input"
                                                value={draftTitle}
                                                onChange={(event) =>
                                                    setDraftTitle(clampEntityTitle((event.target as HTMLInputElement).value))
                                                }
                                                placeholder="Lesson title"
                                                maxLength={MAX_ENTITY_TITLE_LENGTH}
                                                sx={{
                                                    display: "block",
                                                    width: "100%",
                                                    border: `1px dashed ${LESSON_DIALOG_COLORS.blue200}`,
                                                    outline: 0,
                                                    borderRadius: 1,
                                                    px: 1.5,
                                                    py: 1,
                                                    color: LESSON_DIALOG_COLORS.ink,
                                                    backgroundColor: LESSON_DIALOG_COLORS.blue50,
                                                    fontFamily: '"Barlow Semi Condensed", Inter, Arial, sans-serif',
                                                    fontSize: { xs: 28, md: 34 },
                                                    fontWeight: 900,
                                                    lineHeight: 1.02,
                                                    letterSpacing: 0,
                                                    "&::placeholder": { color: LESSON_DIALOG_COLORS.mute },
                                                    "&:focus": { borderColor: LESSON_DIALOG_COLORS.blue },
                                                }}
                                            />
                                        ) : (
                                            <Typography
                                                component="h1"
                                                sx={{
                                                    m: 0,
                                                    maxWidth: isRightPanelVisible ? 760 : 1180,
                                                    color: LESSON_DIALOG_COLORS.ink,
                                                    fontFamily: '"Barlow Semi Condensed", Inter, Arial, sans-serif',
                                                    fontSize: { xs: 52, md: 82 },
                                                    fontWeight: 900,
                                                    letterSpacing: 0,
                                                    lineHeight: 0.92,
                                                    wordBreak: "break-word",
                                                }}
                                            >
                                                {draftTitle || lesson.title}
                                            </Typography>
                                        )}
                                        {isEditing && (
                                            <Typography
                                                sx={{
                                                    mt: 0.75,
                                                    color: LESSON_DIALOG_COLORS.mute,
                                                    fontSize: 12,
                                                    fontWeight: 600,
                                                }}
                                            >
                                                {draftTitle.length}/{MAX_ENTITY_TITLE_LENGTH}
                                            </Typography>
                                        )}

                                        {!isEditing && lesson.description && (
                                            <Typography
                                                sx={{
                                                    maxWidth: 660,
                                                    mt: 1.5,
                                                    color: "#4C5065",
                                                    fontSize: { xs: 18, md: 23 },
                                                    lineHeight: 1.38,
                                                }}
                                            >
                                                {lesson.description}
                                            </Typography>
                                        )}
                                    </Box>

                                    {isEditing ? (
                                        <SimpleEditor
                                            content={draftHtml}
                                            editable
                                            onChange={(nextHtml) => setDraftHtml(nextHtml)}
                                            onImageUpload={uploadLessonImageAsset}
                                            onVideoUpload={uploadLessonVideoAsset}
                                            className="lesson-details-editor"
                                        />
                                    ) : (
                                        <Box
                                            sx={{
                                                width: "100%",
                                                maxWidth: isRightPanelVisible ? 860 : "none",
                                                px: { xs: 3, md: 7 },
                                                py: { xs: 3, md: 6 },
                                            }}
                                        >
                                            <LessonReader html={draftHtml} />
                                        </Box>
                                    )}
                                </Box>
                            </Stack>
                        )}
                    </Paper>

                    {activeView === "lesson" && isRightPanelVisible && (
                        <Stack
                            spacing={2.5}
                            sx={{
                                minWidth: 0,
                                minHeight: 0,
                                overflow: "auto",
                                px: 3,
                                py: 3,
                                borderLeft: `1px solid ${LESSON_DIALOG_COLORS.blue100}`,
                                backgroundColor: "#F9F9F9",
                            }}
                        >
                            <Box>
                                <Typography
                                    sx={{
                                        mb: 1.25,
                                        color: LESSON_DIALOG_COLORS.mute,
                                        fontSize: 11,
                                        fontWeight: 800,
                                        letterSpacing: "0.08em",
                                        lineHeight: 1,
                                        textTransform: "uppercase",
                                    }}
                                >
                                    Source material
                                </Typography>

                                {sourceReferences.length === 0 ? (
                                    <Box
                                        sx={{
                                            p: 1.5,
                                            borderRadius: 1.25,
                                            border: `1px solid ${LESSON_DIALOG_COLORS.blue200}`,
                                            backgroundColor: "#fff",
                                        }}
                                    >
                                        <Typography sx={{ color: LESSON_DIALOG_COLORS.mute, fontSize: 12, fontWeight: 600 }}>
                                            No source snapshot found.
                                        </Typography>
                                    </Box>
                                ) : (
                                    <Stack spacing={1}>
                                        {sourceReferences.map((source, index) => (
                                            <Box
                                                key={source.id}
                                                component={onOpenSourceMaterial ? "button" : "div"}
                                                type={onOpenSourceMaterial ? "button" : undefined}
                                                onClick={onOpenSourceMaterial ? () => onOpenSourceMaterial(Number(source.id)) : undefined}
                                                sx={{
                                                    width: "100%",
                                                    display: "grid",
                                                    gridTemplateColumns: "36px minmax(0, 1fr)",
                                                    gap: 1.25,
                                                    alignItems: "center",
                                                    p: 1.25,
                                                    border: `1px solid ${LESSON_DIALOG_COLORS.blue200}`,
                                                    borderRadius: 1.25,
                                                    backgroundColor: "#fff",
                                                    textAlign: "left",
                                                    cursor: onOpenSourceMaterial ? "pointer" : "default",
                                                    font: "inherit",
                                                    transition: "border-color 120ms ease, background-color 120ms ease",
                                                    "&:hover": onOpenSourceMaterial
                                                        ? {
                                                              borderColor: LESSON_DIALOG_COLORS.blue,
                                                              backgroundColor: LESSON_DIALOG_COLORS.blue50,
                                                          }
                                                        : undefined,
                                                }}
                                            >
                                                <Box
                                                    sx={{
                                                        width: 36,
                                                        height: 36,
                                                        display: "grid",
                                                        placeItems: "center",
                                                        borderRadius: 1,
                                                        color: LESSON_DIALOG_COLORS.blue,
                                                        backgroundColor: LESSON_DIALOG_COLORS.blue50,
                                                        fontSize: 13,
                                                        fontWeight: 900,
                                                    }}
                                                >
                                                    {index + 1}
                                                </Box>
                                                <Box sx={{ minWidth: 0 }}>
                                                    <Typography
                                                        sx={{
                                                            color: LESSON_DIALOG_COLORS.ink,
                                                            fontSize: 13,
                                                            fontWeight: 700,
                                                            lineHeight: 1.25,
                                                            overflow: "hidden",
                                                            textOverflow: "ellipsis",
                                                            whiteSpace: "nowrap",
                                                        }}
                                                    >
                                                        {source.title}
                                                    </Typography>
                                                    <Typography sx={{ mt: 0.35, color: LESSON_DIALOG_COLORS.mute, fontSize: 11, fontWeight: 600 }}>
                                                        {(source.links?.length || 0) + (source.youtubeUrls?.length || 0)} link(s) -{" "}
                                                        {source.attachments?.length || 0} attachment(s)
                                                    </Typography>
                                                </Box>
                                            </Box>
                                        ))}
                                    </Stack>
                                )}
                            </Box>

                            <Box>
                                <Typography
                                    sx={{
                                        mb: 1.25,
                                        color: LESSON_DIALOG_COLORS.mute,
                                        fontSize: 11,
                                        fontWeight: 800,
                                        letterSpacing: "0.08em",
                                        lineHeight: 1,
                                        textTransform: "uppercase",
                                    }}
                                >
                                    Assets
                                </Typography>

                                <Box
                                    sx={{
                                        minWidth: 0,
                                        p: 1.25,
                                        borderRadius: 1.25,
                                        border: `1px solid ${LESSON_DIALOG_COLORS.blue200}`,
                                        backgroundColor: "#fff",
                                    }}
                                >
                                    <Stack spacing={1.5}>
                                        {hasAssets && (
                                            <Box sx={{ minWidth: 0, overflow: "hidden", "& > *": { borderRadius: 1.25 } }}>
                                                <LessonAttachments
                                                    attachments={allAssets}
                                                    layout="list"
                                                    showTitle={false}
                                                    canDelete={canManageCurrentLesson && !isAddingAsset && !isDeleting && !isDeletingAsset}
                                                    deletingAttachmentId={deletingAssetId}
                                                    onDeleteAttachment={handleDeleteLessonAsset}
                                                    buildEditorDragPayload={isEditing ? buildLessonAssetEditorDragPayload : undefined}
                                                />
                                            </Box>
                                        )}

                                        {hasAssets && <Box sx={{ height: 1, backgroundColor: LESSON_DIALOG_COLORS.blue100 }} />}

                                        {assetError && <Alert severity="error">{assetError}</Alert>}
                                        <TextField
                                            placeholder="Link or YouTube URL"
                                            value={assetUrl}
                                            onChange={(event) => setAssetUrl(event.target.value)}
                                            size="small"
                                            fullWidth
                                            disabled={!canManageCurrentLesson || isAddingAsset || isDeleting}
                                            sx={{
                                                "& .MuiOutlinedInput-root": {
                                                    borderRadius: 1.25,
                                                    backgroundColor: "#fff",
                                                    "& fieldset": { borderColor: LESSON_DIALOG_COLORS.blue200 },
                                                    "&:hover fieldset": { borderColor: LESSON_DIALOG_COLORS.blue },
                                                    "&.Mui-focused fieldset": { borderColor: LESSON_DIALOG_COLORS.blue },
                                                },
                                                "& .MuiInputBase-input": { fontSize: 13 },
                                            }}
                                        />
                                        <Stack direction="row" spacing={1} useFlexGap sx={{ flexWrap: "wrap" }}>
                                            <Button
                                                variant="contained"
                                                size="small"
                                                startIcon={<LinkOutlinedIcon />}
                                                onClick={handleAddUrlAsset}
                                                disabled={!canManageCurrentLesson || isAddingAsset || isDeleting || !assetUrl.trim()}
                                                sx={{
                                                    height: 32,
                                                    borderRadius: 999,
                                                    boxShadow: "none",
                                                    backgroundColor: LESSON_DIALOG_COLORS.blue,
                                                    fontSize: 11,
                                                    fontWeight: 800,
                                                    letterSpacing: "0.04em",
                                                    textTransform: "uppercase",
                                                    "&:hover": { backgroundColor: LESSON_DIALOG_COLORS.blue, boxShadow: "none" },
                                                }}
                                            >
                                                {isAddingAsset ? "Adding..." : "Add link"}
                                            </Button>
                                            <Button
                                                variant="outlined"
                                                size="small"
                                                startIcon={<AttachFileOutlinedIcon />}
                                                onClick={() => assetFileInputRef.current?.click()}
                                                disabled={!canManageCurrentLesson || isAddingAsset || isDeleting}
                                                sx={{
                                                    height: 32,
                                                    borderRadius: 999,
                                                    borderColor: LESSON_DIALOG_COLORS.blue200,
                                                    color: LESSON_DIALOG_COLORS.blue,
                                                    fontSize: 11,
                                                    fontWeight: 800,
                                                    letterSpacing: "0.04em",
                                                    textTransform: "uppercase",
                                                    "&:hover": {
                                                        borderColor: LESSON_DIALOG_COLORS.blue,
                                                        backgroundColor: LESSON_DIALOG_COLORS.blue50,
                                                    },
                                                }}
                                            >
                                                File
                                            </Button>
                                        </Stack>
                                        <Box
                                            component="input"
                                            type="file"
                                            ref={assetFileInputRef}
                                            onChange={(event) => handleAddFileAsset((event.target as HTMLInputElement).files?.[0])}
                                            sx={{ display: "none" }}
                                        />
                                    </Stack>
                                </Box>
                            </Box>

                            <Box>
                                <Typography
                                    sx={{
                                        mb: 1.25,
                                        color: LESSON_DIALOG_COLORS.mute,
                                        fontSize: 11,
                                        fontWeight: 800,
                                        letterSpacing: "0.08em",
                                        lineHeight: 1,
                                        textTransform: "uppercase",
                                    }}
                                >
                                    Tags
                                </Typography>

                                <Box
                                    sx={{
                                        p: 1.25,
                                        borderRadius: 1.25,
                                        border: `1px solid ${LESSON_DIALOG_COLORS.blue200}`,
                                        backgroundColor: "#fff",
                                    }}
                                >
                                    {isEditing ? (
                                        <Autocomplete
                                            multiple
                                            freeSolo
                                            options={suggestedLessonTags}
                                            value={draftTags}
                                            onChange={(_event, nextTags) => setDraftTags(normalizeLessonTagInput(nextTags))}
                                            disabled={!canManageCurrentLesson || isSaving || isDeleting}
                                            sx={{
                                                "& .MuiOutlinedInput-root": {
                                                    alignItems: "flex-start",
                                                    minHeight: 42,
                                                    p: 0.5,
                                                    borderRadius: 1.25,
                                                    color: LESSON_DIALOG_COLORS.ink,
                                                    backgroundColor: "#fff",
                                                    "& fieldset": { borderColor: "transparent" },
                                                    "&:hover fieldset": { borderColor: "transparent" },
                                                    "&.Mui-focused fieldset": { borderColor: LESSON_DIALOG_COLORS.blue },
                                                },
                                                "& .MuiInputBase-input": {
                                                    minWidth: "120px",
                                                    py: "7px !important",
                                                    color: LESSON_DIALOG_COLORS.ink,
                                                    fontSize: 13,
                                                },
                                                "& .MuiChip-root": {
                                                    height: 28,
                                                    borderRadius: 999,
                                                    color: LESSON_DIALOG_COLORS.blue,
                                                    backgroundColor: LESSON_DIALOG_COLORS.blue50,
                                                    fontSize: 12,
                                                    fontWeight: 700,
                                                },
                                            }}
                                            renderInput={(params) => (
                                                <TextField
                                                    {...params}
                                                    placeholder={draftTags.length > 0 ? "Add a tag" : "Add tags"}
                                                    size="small"
                                                    slotProps={{
                                                        ...params.slotProps,
                                                        htmlInput: {
                                                            ...params.slotProps.htmlInput,
                                                            ...lessonTagFieldInputProps,
                                                        },
                                                    }}
                                                />
                                            )}
                                        />
                                    ) : visibleTags.length > 0 ? (
                                        <Box sx={{ display: "flex", flexWrap: "wrap", gap: 0.75, minWidth: 0 }}>
                                            {visibleTags.map((tag) => (
                                                <Chip
                                                    key={tag}
                                                    label={tag}
                                                    size="small"
                                                    title={tag}
                                                    sx={{
                                                        height: 28,
                                                        maxWidth: "11rem",
                                                        borderRadius: 999,
                                                        color: LESSON_DIALOG_COLORS.blue,
                                                        backgroundColor: LESSON_DIALOG_COLORS.blue50,
                                                        fontSize: 12,
                                                        fontWeight: 700,
                                                        "& .MuiChip-label": {
                                                            px: 1.25,
                                                            overflow: "hidden",
                                                            textOverflow: "ellipsis",
                                                        },
                                                    }}
                                                />
                                            ))}
                                        </Box>
                                    ) : (
                                        <Typography sx={{ color: LESSON_DIALOG_COLORS.mute, fontSize: 12, fontWeight: 600 }}>
                                            No tags yet.
                                        </Typography>
                                    )}
                                </Box>
                            </Box>

                            {canManageCurrentLesson && lesson.status !== "failed" && (
                                <DetailPanel title="Revise lesson">
                                    <Stack spacing={1.5}>
                                        <Box>
                                            <Typography sx={{ color: LESSON_DIALOG_COLORS.slate, fontSize: 13, lineHeight: 1.45 }}>
                                                Describe what exactly and how should change.
                                            </Typography>
                                        </Box>

                                        {revisionError && <Alert severity="error">{revisionError}</Alert>}

                                        {pendingRevision && (
                                            <Alert
                                                severity="info"
                                                action={
                                                    <Stack direction="row" spacing={0.75}>
                                                        <Button color="inherit" size="small" onClick={handleAcceptRevision}>
                                                            Accept
                                                        </Button>
                                                        <Button color="inherit" size="small" onClick={() => void handleDiscardRevision()}>
                                                            Discard
                                                        </Button>
                                                    </Stack>
                                                }
                                            >
                                                AI revision applied. Accept to keep it, or discard to restore the previous version.
                                            </Alert>
                                        )}

                                        <Stack direction="row" spacing={0.75} useFlexGap sx={{ flexWrap: "wrap" }}>
                                            {revisionOptions.map((option) => {
                                                const isSelected = selectedRevisionOptions.includes(option.value);

                                                return (
                                                    <Chip
                                                        key={option.value}
                                                        label={option.label}
                                                        clickable
                                                        onClick={() => handleToggleRevisionOption(option.value)}
                                                        sx={{
                                                            height: 28,
                                                            borderRadius: 999,
                                                            border: `1px solid ${isSelected ? LESSON_DIALOG_COLORS.blue : LESSON_DIALOG_COLORS.blue200}`,
                                                            color: isSelected ? "#fff" : LESSON_DIALOG_COLORS.blue,
                                                            backgroundColor: isSelected ? LESSON_DIALOG_COLORS.blue : LESSON_DIALOG_COLORS.blue50,
                                                            fontSize: 12,
                                                            fontWeight: 700,
                                                            "& .MuiChip-label": { px: 1.25 },
                                                            "&:hover": {
                                                                backgroundColor: isSelected ? LESSON_DIALOG_COLORS.blue : "#fff",
                                                            },
                                                        }}
                                                    />
                                                );
                                            })}
                                        </Stack>

                                        <TextField
                                            value={revisionRequest}
                                            onChange={(event) => setRevisionRequest(event.target.value)}
                                            minRows={4}
                                            multiline
                                            placeholder="Example: keep the factual content, but make the explanation less course-like and add one clear example for naming conventions."
                                            fullWidth
                                            disabled={isDeleting || isSaving || isRevising}
                                            sx={{
                                                "& .MuiOutlinedInput-root": {
                                                    borderRadius: 1.5,
                                                    backgroundColor: "#fff",
                                                    "& fieldset": { borderColor: LESSON_DIALOG_COLORS.blue200 },
                                                    "&:hover fieldset": { borderColor: LESSON_DIALOG_COLORS.blue },
                                                    "&.Mui-focused fieldset": { borderColor: LESSON_DIALOG_COLORS.blue },
                                                },
                                                "& .MuiInputBase-input": {
                                                    color: LESSON_DIALOG_COLORS.ink,
                                                    fontSize: 13,
                                                    lineHeight: 1.5,
                                                },
                                            }}
                                        />

                                        {lastRevision && (
                                            <Box
                                                sx={{
                                                    p: 1.25,
                                                    borderRadius: 1.25,
                                                    backgroundColor: LESSON_DIALOG_COLORS.blue50,
                                                    border: `1px solid ${LESSON_DIALOG_COLORS.blue100}`,
                                                }}
                                            >
                                                <Typography
                                                    sx={{
                                                        color: LESSON_DIALOG_COLORS.blue,
                                                        fontSize: 10,
                                                        fontWeight: 800,
                                                        letterSpacing: "0.08em",
                                                        textTransform: "uppercase",
                                                        mb: 0.5,
                                                    }}
                                                >
                                                    Last revision
                                                </Typography>
                                                <Typography sx={{ color: LESSON_DIALOG_COLORS.mute, fontSize: 11, fontWeight: 700, mb: 0.5 }}>
                                                    {formatDateTime(lastRevision.revisedAt)} • {lastRevision.revisionBrief?.changeScope || "substantial"}
                                                </Typography>
                                                {lastRevision.revisionRequest && (
                                                    <Typography sx={{ color: LESSON_DIALOG_COLORS.slate, fontSize: 12, lineHeight: 1.4 }}>
                                                        {lastRevision.revisionRequest}
                                                    </Typography>
                                                )}
                                            </Box>
                                        )}

                                        {contentSnapshots.length > 0 && (
                                            <Stack spacing={0.75}>
                                                <Typography
                                                    sx={{
                                                        color: LESSON_DIALOG_COLORS.blue,
                                                        fontSize: 10,
                                                        fontWeight: 800,
                                                        letterSpacing: "0.08em",
                                                        textTransform: "uppercase",
                                                    }}
                                                >
                                                    Restore previous version
                                                </Typography>
                                                {contentSnapshots
                                                    .slice()
                                                    .reverse()
                                                    .map((snapshot) => (
                                                        <Box
                                                            key={snapshot.id}
                                                            sx={{
                                                                display: "flex",
                                                                alignItems: "center",
                                                                justifyContent: "space-between",
                                                                gap: 1,
                                                                p: 1,
                                                                borderRadius: 1,
                                                                border: `1px solid ${LESSON_DIALOG_COLORS.blue100}`,
                                                            }}
                                                        >
                                                            <Box sx={{ minWidth: 0 }}>
                                                                <Typography sx={{ fontSize: 12, fontWeight: 700, color: LESSON_DIALOG_COLORS.ink }}>
                                                                    {snapshot.label}
                                                                </Typography>
                                                                <Typography sx={{ fontSize: 11, color: LESSON_DIALOG_COLORS.mute }}>
                                                                    {formatDateTime(snapshot.createdAt)}
                                                                </Typography>
                                                            </Box>
                                                            <Button
                                                                size="small"
                                                                variant="outlined"
                                                                onClick={() => void handleRestoreSnapshot(snapshot.id)}
                                                                disabled={isSaving || isRevising}
                                                            >
                                                                Restore
                                                            </Button>
                                                        </Box>
                                                    ))}
                                            </Stack>
                                        )}

                                        <Button
                                            variant="contained"
                                            startIcon={<AutoAwesomeOutlinedIcon />}
                                            onClick={handleRevise}
                                            disabled={isDeleting || isSaving || isRevising}
                                            sx={{
                                                alignSelf: "flex-start",
                                                minHeight: 36,
                                                px: 2.25,
                                                borderRadius: 999,
                                                boxShadow: "none",
                                                backgroundColor: LESSON_DIALOG_COLORS.blue,
                                                fontSize: 11,
                                                fontWeight: 800,
                                                letterSpacing: "0.06em",
                                                textTransform: "uppercase",
                                                "&:hover": { backgroundColor: LESSON_DIALOG_COLORS.blue, boxShadow: "none" },
                                            }}
                                        >
                                            {isRevising ? "Revising..." : "Revise lesson"}
                                        </Button>
                                    </Stack>
                                </DetailPanel>
                            )}

                            {canManageCurrentActivities && lesson.status !== "failed" && (
                                <DetailPanel title="Generate activity">
                                    <Stack spacing={1.5}>
                                        <Box>
                                            <Typography sx={{ color: LESSON_DIALOG_COLORS.slate, fontSize: 13, lineHeight: 1.45 }}>
                                                Create a saved quiz or flashcards from this lesson. Passing flow comes later.
                                            </Typography>
                                        </Box>

                                        {activityError && <Alert severity="error">{activityError}</Alert>}
                                        {activitySuccess && <Alert severity="success">{activitySuccess}</Alert>}

                                        <FormControl fullWidth size="small">
                                            <InputLabel id="activity-type-label">Activity type</InputLabel>
                                            <Select
                                                labelId="activity-type-label"
                                                value={activityType}
                                                label="Activity type"
                                                onChange={(event) => handleActivityTypeChange(event.target.value as "quiz" | "flashcards")}
                                                disabled={isDeleting || isSaving || isRevising || isGeneratingActivity}
                                                sx={{
                                                    borderRadius: 1.5,
                                                    fontSize: 13,
                                                    "& .MuiOutlinedInput-notchedOutline": { borderColor: LESSON_DIALOG_COLORS.blue200 },
                                                    "&:hover .MuiOutlinedInput-notchedOutline": { borderColor: LESSON_DIALOG_COLORS.blue },
                                                    "&.Mui-focused .MuiOutlinedInput-notchedOutline": { borderColor: LESSON_DIALOG_COLORS.blue },
                                                }}
                                            >
                                                {activityTypeOptions.map((option) => (
                                                    <MenuItem key={option.value} value={option.value}>
                                                        {option.label}
                                                    </MenuItem>
                                                ))}
                                            </Select>
                                        </FormControl>

                                        <TextField
                                            label={activityType === "quiz" ? "Questions" : "Cards"}
                                            type="number"
                                            value={activityCount}
                                            onChange={(event) => setActivityCount(event.target.value)}
                                            size="small"
                                            fullWidth
                                            slotProps={{ htmlInput: { min: activitySettings.min, max: activitySettings.max } }}
                                            helperText={`Allowed: ${activitySettings.min}-${activitySettings.max}`}
                                            disabled={
                                                isDeleting ||
                                                isSaving ||
                                                isPublishing ||
                                                isArchiving ||
                                                isRevising ||
                                                isGeneratingActivity
                                            }
                                            sx={{
                                                "& .MuiOutlinedInput-root": {
                                                    borderRadius: 1.5,
                                                    "& fieldset": { borderColor: LESSON_DIALOG_COLORS.blue200 },
                                                    "&:hover fieldset": { borderColor: LESSON_DIALOG_COLORS.blue },
                                                    "&.Mui-focused fieldset": { borderColor: LESSON_DIALOG_COLORS.blue },
                                                },
                                                "& .MuiInputBase-input": { fontSize: 13 },
                                                "& .MuiFormHelperText-root": {
                                                    color: LESSON_DIALOG_COLORS.mute,
                                                    fontSize: 11,
                                                    fontWeight: 600,
                                                    mx: 0,
                                                },
                                            }}
                                        />

                                        {activities.length > 0 && (
                                            <Box
                                                sx={{
                                                    p: 1.25,
                                                    borderRadius: 1.25,
                                                    backgroundColor: LESSON_DIALOG_COLORS.blue50,
                                                    border: `1px solid ${LESSON_DIALOG_COLORS.blue100}`,
                                                }}
                                            >
                                                <Typography
                                                    sx={{
                                                        color: LESSON_DIALOG_COLORS.blue,
                                                        fontSize: 10,
                                                        fontWeight: 800,
                                                        letterSpacing: "0.08em",
                                                        textTransform: "uppercase",
                                                        mb: 0.75,
                                                    }}
                                                >
                                                    Saved activities
                                                </Typography>
                                                <Stack spacing={0.75}>
                                                    {activities.slice(0, 3).map((activity) => (
                                                        <Typography
                                                            key={activity.id}
                                                            sx={{ color: LESSON_DIALOG_COLORS.slate, fontSize: 12, fontWeight: 600 }}
                                                        >
                                                            {activity.title || activity.type} - {activity.itemCount} item(s)
                                                        </Typography>
                                                    ))}
                                                </Stack>
                                            </Box>
                                        )}

                                        <Button
                                            variant="contained"
                                            startIcon={<QuizOutlinedIcon />}
                                            onClick={() => void handleGenerateActivity()}
                                            disabled={
                                                isDeleting ||
                                                isSaving ||
                                                isPublishing ||
                                                isArchiving ||
                                                isRevising ||
                                                isGeneratingActivity
                                            }
                                            sx={{
                                                alignSelf: "flex-start",
                                                minHeight: 36,
                                                px: 2.25,
                                                borderRadius: 999,
                                                boxShadow: "none",
                                                backgroundColor: LESSON_DIALOG_COLORS.blue,
                                                fontSize: 11,
                                                fontWeight: 800,
                                                letterSpacing: "0.06em",
                                                textTransform: "uppercase",
                                                "&:hover": { backgroundColor: LESSON_DIALOG_COLORS.blue, boxShadow: "none" },
                                            }}
                                        >
                                            {isGeneratingActivity ? "Generating..." : "Generate activity"}
                                        </Button>
                                    </Stack>
                                </DetailPanel>
                            )}

                            {(teacherVideo.videoUrl || (isEditing && canGenerateTeacherVideo && lesson.status !== "failed")) && (
                                <DetailPanel title="Teacher video">
                                    <Stack spacing={1.25}>
                                        {teacherVideo.videoUrl ? (
                                            <Box
                                                sx={{
                                                    overflow: "hidden",
                                                    borderRadius: 1.25,
                                                    border: `1px solid ${LESSON_DIALOG_COLORS.blue100}`,
                                                    backgroundColor: "#000",
                                                }}
                                            >
                                                <Box
                                                    component="video"
                                                    src={teacherVideo.videoUrl}
                                                    poster={teacherVideo.thumbnailUrl || undefined}
                                                    controls
                                                    playsInline
                                                    preload="metadata"
                                                    sx={{ width: "100%", aspectRatio: "16 / 9", display: "block", backgroundColor: "#000" }}
                                                />
                                            </Box>
                                        ) : (
                                            <Typography sx={{ color: LESSON_DIALOG_COLORS.slate, fontSize: 13, lineHeight: 1.45 }}>
                                                Generate a short 45-60 second teacher avatar summary for this lesson.
                                            </Typography>
                                        )}

                                        {teacherVideoError && <Alert severity="error">{teacherVideoError}</Alert>}
                                        {teacherVideoSuccess && <Alert severity="success">{teacherVideoSuccess}</Alert>}

                                        {(teacherVideo.videoId || isEditing) && (
                                            <Box
                                                sx={{
                                                    p: 1.25,
                                                    borderRadius: 1.25,
                                                    backgroundColor: LESSON_DIALOG_COLORS.blue50,
                                                    border: `1px solid ${LESSON_DIALOG_COLORS.blue100}`,
                                                }}
                                            >
                                                <Typography
                                                    sx={{
                                                        color: LESSON_DIALOG_COLORS.blue,
                                                        fontSize: 10,
                                                        fontWeight: 800,
                                                        letterSpacing: "0.08em",
                                                        textTransform: "uppercase",
                                                        mb: 0.5,
                                                    }}
                                                >
                                                    Status
                                                </Typography>
                                                <Stack direction="row" spacing={0.75} sx={{ alignItems: "center" }}>
                                                    {(isTeacherVideoActive || isCheckingTeacherVideo) && (
                                                        <CircularProgress size={14} thickness={5} sx={{ color: LESSON_DIALOG_COLORS.blue }} />
                                                    )}
                                                    <Typography
                                                        sx={{ color: LESSON_DIALOG_COLORS.slate, fontSize: 12, fontWeight: 700, textTransform: "capitalize" }}
                                                    >
                                                        {isCheckingTeacherVideo
                                                            ? "Checking status..."
                                                            : isTeacherVideoActive
                                                              ? `${teacherVideoStatusLabel}...`
                                                              : teacherVideoStatusLabel}
                                                    </Typography>
                                                </Stack>
                                                {isTeacherVideoActive && (
                                                    <Typography sx={{ mt: 0.5, color: LESSON_DIALOG_COLORS.mute, fontSize: 11, fontWeight: 600, lineHeight: 1.35 }}>
                                                        Rendering can take a few minutes. You can close this lesson and come back later.
                                                    </Typography>
                                                )}
                                                {teacherVideo.duration && (
                                                    <Typography sx={{ mt: 0.35, color: LESSON_DIALOG_COLORS.mute, fontSize: 11, fontWeight: 600 }}>
                                                        Duration: {Math.round(teacherVideo.duration)} sec
                                                    </Typography>
                                                )}
                                                {teacherVideo.videoUrl && (
                                                    <Button
                                                        href={teacherVideo.videoUrl}
                                                        target="_blank"
                                                        rel="noreferrer"
                                                        size="small"
                                                        sx={{
                                                            mt: 1,
                                                            minHeight: 28,
                                                            px: 1.25,
                                                            borderRadius: 999,
                                                            color: LESSON_DIALOG_COLORS.blue,
                                                            fontSize: 11,
                                                            fontWeight: 800,
                                                            letterSpacing: "0.04em",
                                                            textTransform: "uppercase",
                                                        }}
                                                    >
                                                        Open video
                                                    </Button>
                                                )}
                                            </Box>
                                        )}

                                        {isEditing && canGenerateTeacherVideo && lesson.status !== "failed" && (
                                            <Stack direction="row" spacing={1} useFlexGap sx={{ flexWrap: "wrap" }}>
                                                <Button
                                                    variant="contained"
                                                    startIcon={<OndemandVideoOutlinedIcon />}
                                                    onClick={handleGenerateTeacherVideo}
                                                    disabled={
                                                        !canGenerateTeacherVideo ||
                                                        !canManageCurrentLesson ||
                                                        isDeleting ||
                                                        isSaving ||
                                                        isPublishing ||
                                                        isArchiving ||
                                                        isRevising ||
                                                        isGeneratingActivity ||
                                                        isGeneratingTeacherVideo ||
                                                        isDeletingTeacherVideo ||
                                                        isTeacherVideoActive
                                                    }
                                                    sx={{
                                                        minHeight: 36,
                                                        px: 2.25,
                                                        borderRadius: 999,
                                                        boxShadow: "none",
                                                        backgroundColor: LESSON_DIALOG_COLORS.blue,
                                                        fontSize: 11,
                                                        fontWeight: 800,
                                                        letterSpacing: "0.06em",
                                                        textTransform: "uppercase",
                                                        "&:hover": { backgroundColor: LESSON_DIALOG_COLORS.blue, boxShadow: "none" },
                                                    }}
                                                >
                                                    {isGeneratingTeacherVideo
                                                        ? "Starting..."
                                                        : teacherVideo.videoId
                                                          ? "Regenerate video"
                                                          : "Generate video"}
                                                </Button>

                                                {teacherVideo.videoId && (
                                                    <Button
                                                        variant="outlined"
                                                        onClick={handleRefreshTeacherVideo}
                                                        disabled={isCheckingTeacherVideo || isGeneratingTeacherVideo || isDeletingTeacherVideo}
                                                        sx={lessonSecondaryButtonSx}
                                                    >
                                                        Refresh
                                                    </Button>
                                                )}

                                                {(teacherVideo.videoId || teacherVideo.videoUrl) && (
                                                    <Button
                                                        variant="outlined"
                                                        color="error"
                                                        onClick={handleDeleteTeacherVideo}
                                                        disabled={isDeletingTeacherVideo || isCheckingTeacherVideo || isGeneratingTeacherVideo}
                                                        sx={lessonDangerButtonSx}
                                                    >
                                                        {isDeletingTeacherVideo ? "Removing..." : "Remove video"}
                                                    </Button>
                                                )}
                                            </Stack>
                                        )}
                                    </Stack>
                                </DetailPanel>
                            )}
                        </Stack>
                    )}
                </Box>
            </DialogContent>

            <DialogActions
                sx={{
                    px: { xs: 2, md: 3.5 },
                    py: 1.75,
                    flex: "0 0 auto",
                    gap: 1,
                    flexWrap: "wrap",
                    borderTop: `1px solid ${LESSON_DIALOG_COLORS.blue100}`,
                    backgroundColor: "#fff",
                }}
            >
                {isEditing && (
                    <Stack direction="row" spacing={1} useFlexGap sx={{ mr: "auto", flexWrap: "wrap" }}>
                        <Button
                            onClick={() => {
                                if (isLessonArchived) {
                                    handleRestore();
                                    return;
                                }
                                setArchiveError("");
                                setIsConfirmArchiveOpen(true);
                            }}
                            variant="outlined"
                            startIcon={isLessonArchived ? <UnarchiveOutlinedIcon /> : <ArchiveOutlinedIcon />}
                            disabled={
                                !canManageCurrentLesson ||
                                (isLessonArchived && lesson.status !== "ready") ||
                                isSaving ||
                                isPublishing ||
                                isArchiving ||
                                isDeleting ||
                                isRevising ||
                                isGeneratingActivity ||
                                isSavingActivity ||
                                isUploadingCover
                            }
                            sx={lessonSecondaryButtonSx}
                        >
                            {isLessonArchived
                                ? isPublishing
                                    ? "Restoring..."
                                    : "Restore lesson"
                                : isArchiving
                                  ? "Archiving..."
                                  : "Archive lesson"}
                        </Button>
                        <Button
                            onClick={() => {
                                setDeleteError("");
                                setIsConfirmDeleteOpen(true);
                            }}
                            variant="outlined"
                            startIcon={<DeleteOutlineOutlinedIcon />}
                            disabled={
                                !canManageCurrentLesson ||
                                isSaving ||
                                isPublishing ||
                                isArchiving ||
                                isDeleting ||
                                isRevising ||
                                isGeneratingActivity ||
                                isSavingActivity ||
                                isUploadingCover
                            }
                            sx={lessonDangerButtonSx}
                        >
                            Delete lesson
                        </Button>
                    </Stack>
                )}

                {activeView !== "lesson" && canManageCurrentActivities ? (
                    <Button
                        onClick={handleSaveActivity}
                        variant="contained"
                        startIcon={<SaveOutlinedIcon />}
                        disabled={
                            !canManageCurrentActivities ||
                            isDeleting ||
                            isSaving ||
                            isPublishing ||
                            isArchiving ||
                            isRevising ||
                            isGeneratingActivity ||
                            isSavingActivity ||
                            isUploadingCover
                        }
                        sx={lessonPrimaryButtonSx}
                    >
                        {isSavingActivity ? "Saving activity..." : "Save activity"}
                    </Button>
                ) : isEditing ? (
                    <>
                        <Button
                            onClick={handleCancelEdit}
                            variant="outlined"
                            disabled={isSaving || isPublishing || isArchiving || isDeleting || isGeneratingActivity || isSavingActivity || isUploadingCover}
                            sx={lessonSecondaryButtonSx}
                        >
                            Cancel
                        </Button>
                        <Button
                            onClick={() => void handleSave()}
                            variant="contained"
                            startIcon={<SaveOutlinedIcon />}
                            disabled={
                                !canManageCurrentLesson ||
                                isSaving ||
                                isPublishing ||
                                isArchiving ||
                                isDeleting ||
                                isRevising ||
                                isGeneratingActivity ||
                                isSavingActivity ||
                                isUploadingCover
                            }
                            sx={lessonPrimaryButtonSx}
                        >
                            {isSaving ? "Saving..." : "Save changes"}
                        </Button>
                    </>
                ) : (
                    <>
                        {canPublishLesson && (
                            <Button
                                onClick={() => void handlePublish()}
                                variant="contained"
                                startIcon={<RocketLaunchOutlinedIcon />}
                                disabled={
                                    !canManageCurrentLesson ||
                                    isDeleting ||
                                    isSaving ||
                                    isPublishing ||
                                    isArchiving ||
                                    isRevising ||
                                    isGeneratingActivity ||
                                    isSavingActivity ||
                                    isUploadingCover
                                }
                                sx={lessonDarkButtonSx}
                            >
                                {isPublishing ? "Publishing..." : "Publish lesson"}
                            </Button>
                        )}
                        {canManageCurrentLesson && (
                            <Button
                                onClick={() => setIsEditing(true)}
                                variant="contained"
                                startIcon={<EditOutlinedIcon />}
                                disabled={
                                    isDeleting ||
                                    isSaving ||
                                    isPublishing ||
                                    isArchiving ||
                                    isRevising ||
                                    isGeneratingActivity ||
                                    isSavingActivity ||
                                    isUploadingCover
                                }
                                sx={lessonPrimaryButtonSx}
                            >
                                {lesson.status === "failed" ? "Write lesson content" : "Edit lesson"}
                            </Button>
                        )}
                    </>
                )}
            </DialogActions>

            <SharedDialog
                open={thinContentConfirmOpen}
                onClose={() => setThinContentConfirmOpen(false)}
                size="sm"
                title="Lesson content is thin"
                flushBody
                footer={
                    <>
                        <SharedButton variant="ghost" onClick={() => setThinContentConfirmOpen(false)}>
                            Cancel
                        </SharedButton>
                        <SharedButton
                            variant="primary"
                            onClick={() => {
                                const existingOfType = activities.filter((activity) => activity.type === activityType);
                                if (existingOfType.length > 0) {
                                    setThinContentConfirmOpen(false);
                                    setGenerateConfirm("append");
                                    return;
                                }
                                void runGenerateActivity();
                            }}
                            disabled={isGeneratingActivity}
                        >
                            Continue
                        </SharedButton>
                    </>
                }
            >
                <div className="library-form-body">
                    <p style={{ color: "#80808e", margin: 0 }}>
                        This lesson has very little content. The generated activities may not be accurate. Continue
                        anyway?
                    </p>
                </div>
            </SharedDialog>

            <SharedDialog
                open={Boolean(generateConfirm)}
                onClose={() => setGenerateConfirm(null)}
                size="sm"
                title={activityType === "quiz" ? "A quiz already exists" : "Flashcards already exist"}
                flushBody
                footer={
                    <>
                        <SharedButton
                            variant="ghost"
                            onClick={() => setGenerateConfirm(null)}
                            disabled={isGeneratingActivity || isReplacingActivity}
                        >
                            Cancel
                        </SharedButton>
                        <SharedButton
                            variant="ghost"
                            onClick={() => void runReplaceActivity()}
                            disabled={isGeneratingActivity || isReplacingActivity}
                        >
                            {isReplacingActivity ? "Replacing..." : "Replace existing"}
                        </SharedButton>
                        <SharedButton
                            variant="primary"
                            onClick={() => void runGenerateActivity()}
                            disabled={isGeneratingActivity || isReplacingActivity}
                        >
                            {isGeneratingActivity && !isReplacingActivity ? "Generating..." : "Create additional"}
                        </SharedButton>
                    </>
                }
            >
                <div className="library-form-body">
                    <p style={{ color: "#80808e", margin: 0 }}>
                        {activityType === "quiz"
                            ? "A quiz already exists for this lesson. Replace it with the newly generated one, create an additional quiz, or cancel to keep only the existing one."
                            : "Flashcards already exist for this lesson. Replace them with the newly generated set, create an additional set, or cancel to keep only the existing one."}
                    </p>
                </div>
            </SharedDialog>

            <SharedDialog
                open={isConfirmArchiveOpen}
                onClose={() => {
                    if (!isArchiving) {
                        setIsConfirmArchiveOpen(false);
                    }
                }}
                size="sm"
                title="Archive lesson?"
                flushBody
                footer={
                    <>
                        <SharedButton onClick={() => setIsConfirmArchiveOpen(false)} disabled={isArchiving}>
                            Cancel
                        </SharedButton>
                        <SharedButton
                            variant="danger"
                            onClick={handleArchive}
                            disabled={!canManageCurrentLesson || isArchiving}
                        >
                            {isArchiving ? "Archiving..." : "Archive lesson"}
                        </SharedButton>
                    </>
                }
            >
                <div className="library-form-body">
                    <p>
                        This will archive <strong>{lesson.title}</strong>.
                    </p>
                    <p style={{ color: "#80808e" }}>
                        Archived lessons are hidden from published library views and cannot be added to My Lessons
                        until restored.
                    </p>
                    {archiveError && (
                        <p className="library-alert library-alert--error">{archiveError}</p>
                    )}
                </div>
            </SharedDialog>

            <SharedDialog
                open={isConfirmDeleteOpen}
                onClose={() => {
                    if (!isDeleting) {
                        setIsConfirmDeleteOpen(false);
                    }
                }}
                size="sm"
                title="Delete lesson?"
                flushBody
                footer={
                    <>
                        <SharedButton onClick={() => setIsConfirmDeleteOpen(false)} disabled={isDeleting}>
                            Cancel
                        </SharedButton>
                        <SharedButton
                            variant="danger"
                            onClick={handleDelete}
                            disabled={!canManageCurrentLesson || isDeleting}
                        >
                            {isDeleting ? "Deleting..." : "Delete permanently"}
                        </SharedButton>
                    </>
                }
            >
                <div className="library-form-body">
                    <p>
                        This action will permanently remove <strong>{lesson.title}</strong>.
                    </p>
                    <p style={{ color: "#80808e" }}>
                        The lesson will be removed from the library and from every user&apos;s My Lessons.
                    </p>
                    {deleteError && (
                        <p className="library-alert library-alert--error">{deleteError}</p>
                    )}
                </div>
            </SharedDialog>
        </Dialog>

        <DiscardChangesDialog
            open={isDiscardOpen}
            onKeepEditing={() => {
                discardClosesDialogRef.current = false;
                setIsDiscardOpen(false);
            }}
            onDiscard={handleDiscardChanges}
            disabled={isSaving || isPublishing || isArchiving || isDeleting || isRevising || isGeneratingActivity || isSavingActivity}
        />
        </>
    );
}
