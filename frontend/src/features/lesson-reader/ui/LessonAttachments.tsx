import { useState, type DragEvent, type SyntheticEvent } from "react";
import { LESSON_EDITOR_ASSET_DRAG_TYPE, type LessonEditorAssetDragPayload } from "@/shared/editor/lesson-editor-drag";
import { getVideoErrorMessage } from "@/shared/lib/videoError";
import DeleteOutlineOutlinedIcon from "@mui/icons-material/DeleteOutlineOutlined";
import DragIndicatorOutlinedIcon from "@mui/icons-material/DragIndicatorOutlined";
import ImageOutlinedIcon from "@mui/icons-material/ImageOutlined";
import LinkOutlinedIcon from "@mui/icons-material/LinkOutlined";
import OpenInNewOutlinedIcon from "@mui/icons-material/OpenInNewOutlined";
import SmartDisplayOutlinedIcon from "@mui/icons-material/SmartDisplayOutlined";
import { openStorageFile, useFilePreviewUrls } from "@/shared/api/files";
import { getYoutubeEmbedUrl } from "@/shared/lib/youtube";
import { useYoutubeOembedQuery } from "../api/useLessonAssetQueries";
import "./lesson-attachments.css";

export interface LessonAttachmentAsset {
    id?: string | number;
    name: string;
    kind?: string;
    mimeType?: string;
    storageKey?: string;
    previewUrl?: string;
    url?: string;
    youtubeTitle?: string;
    youtubeAuthorName?: string;
    youtubeThumbnailUrl?: string;
    linkImageUrl?: string;
    linkSiteName?: string;
    sourceId?: string | number;
    isLessonAsset?: boolean;
}

interface Props {
    attachments?: LessonAttachmentAsset[];
    sourceReferences?: Array<Record<string, unknown>>;
    layout?: "grid" | "list" | "column";
    showTitle?: boolean;
    canDelete?: boolean;
    deletingAttachmentId?: string | number | null;
    onDeleteAttachment?: (attachment: LessonAttachmentAsset) => void;
    buildEditorDragPayload?: (attachment: LessonAttachmentAsset) => LessonEditorAssetDragPayload | null;
}

export function getSourceAttachments(sourceReferences: Array<Record<string, unknown>> = []): LessonAttachmentAsset[] {
    return sourceReferences.flatMap((source) => {
        const youtubeVideos = Array.isArray(source.youtubeVideos)
            ? (source.youtubeVideos as Array<Record<string, unknown>>)
            : [];
        const youtubeMetadataByUrl = new Map(
            youtubeVideos.map((video) => [String(video.url ?? ""), video]),
        );
        const youtubeUrls = Array.isArray(source.youtubeUrls) ? source.youtubeUrls : [];
        const youtubeAssets = youtubeUrls.map((url, index) => {
            const metadata = youtubeMetadataByUrl.get(String(url)) || {};
            return {
                id: `${source.id || source.sourceNumber}-youtube-${index}`,
                name: String(metadata.title || `YouTube video ${index + 1}`),
                kind: "youtube",
                mimeType: "video/youtube",
                url: String(url),
                youtubeTitle: String(metadata.title || ""),
                youtubeAuthorName: String(metadata.authorName || ""),
                youtubeThumbnailUrl: String(metadata.thumbnailUrl || ""),
            };
        });

        const linkAssetsRaw = Array.isArray(source.linkAssets) ? source.linkAssets : [];
        const linkAssets = linkAssetsRaw.map((linkAsset, index) => {
            const link = linkAsset as Record<string, unknown>;
            return {
                id: `${source.id || source.sourceNumber}-link-${index}`,
                name: String(link.title || `Web link ${index + 1}`),
                kind: "link",
                mimeType: "text/html",
                url: String(link.url || ""),
                linkTitle: String(link.title || ""),
                linkSiteName: String(link.siteName || ""),
                linkImageUrl: String(link.imageUrl || ""),
            };
        });
        const linkAssetUrls = new Set(linkAssets.map((linkAsset) => linkAsset.url).filter(Boolean));
        const linksRaw = Array.isArray(source.links) ? source.links : [];
        const plainLinkAssets = linksRaw.flatMap((link, index) => {
            const linkRecord = typeof link === "object" && link ? (link as Record<string, unknown>) : {};
            const url = typeof link === "string" ? link : String(linkRecord.url || "");

            if (!url || linkAssetUrls.has(url)) {
                return [];
            }

            return [{
                id: `${source.id || source.sourceNumber}-plain-link-${index}`,
                name: String(linkRecord.title || `Web link ${index + 1}`),
                kind: "link",
                mimeType: "text/html",
                url,
                linkTitle: String(linkRecord.title || ""),
                linkSiteName: String(linkRecord.siteName || ""),
                linkImageUrl: String(linkRecord.imageUrl || ""),
            }];
        });

        const fileAssets = (Array.isArray(source.attachments) ? source.attachments : []).map(
            (attachment) => ({
                ...(attachment as LessonAttachmentAsset),
                sourceId: source.id as string | number | undefined,
            }),
        );

        return [...youtubeAssets, ...linkAssets, ...plainLinkAssets, ...fileAssets];
    });
}

/** Normalizes a `LessonAssetV1` (an asset attached directly to the lesson, as opposed to one
 * derived from its AI-generation source references) into the shared attachment card shape. */
export function normalizeLessonAsset(asset: Record<string, unknown>): LessonAttachmentAsset {
    if (asset.kind === "youtube") {
        return {
            id: asset.id as string | number | undefined,
            name: String(asset.title || asset.name || "YouTube video"),
            kind: "youtube",
            mimeType: "video/youtube",
            url: asset.url as string | undefined,
            youtubeTitle: String(asset.title || ""),
            youtubeAuthorName: String(
                (asset.metadata as Record<string, unknown> | undefined)?.authorName || asset.description || "",
            ),
            youtubeThumbnailUrl: String(asset.imageUrl || ""),
            isLessonAsset: true,
        };
    }

    if (asset.kind === "link") {
        return {
            id: asset.id as string | number | undefined,
            name: String(asset.title || asset.name || "Web link"),
            kind: "link",
            mimeType: "text/html",
            url: asset.url as string | undefined,
            linkImageUrl: String(asset.imageUrl || ""),
            linkSiteName: String(asset.siteName || ""),
            isLessonAsset: true,
        };
    }

    return {
        id: asset.id as string | number | undefined,
        name: String(asset.name || asset.title || "Lesson file"),
        kind: asset.kind as string | undefined,
        mimeType: String(asset.mimeType || ""),
        storageKey: String(asset.storageKey || ""),
        isLessonAsset: true,
    };
}

function isImageAttachment(attachment: LessonAttachmentAsset): boolean {
    return attachment.kind === "image" || Boolean(attachment.mimeType?.startsWith("image/"));
}

function isVideoAttachment(attachment: LessonAttachmentAsset): boolean {
    return attachment.kind === "video" || Boolean(attachment.mimeType?.startsWith("video/"));
}

function getFileBadge(attachment: LessonAttachmentAsset) {
    const mimeType = (attachment.mimeType || "").toLowerCase();
    const name = (attachment.name || "").toLowerCase();

    if (mimeType.includes("pdf") || name.endsWith(".pdf")) {
        return { label: "PDF", color: "#dc2626" };
    }
    if (mimeType.includes("word") || name.endsWith(".doc") || name.endsWith(".docx")) {
        return { label: "DOC", color: "#2563eb" };
    }
    return { label: "FILE", color: "#0009DC" };
}

function AttachmentCard({
    attachment,
    previewUrl,
    canDelete,
    isDeleting,
    onOpenAttachment,
    onDeleteAttachment,
    buildEditorDragPayload,
}: {
    attachment: LessonAttachmentAsset;
    previewUrl?: string;
    canDelete: boolean;
    isDeleting: boolean;
    onOpenAttachment: (attachment: LessonAttachmentAsset) => void;
    onDeleteAttachment?: (attachment: LessonAttachmentAsset) => void;
    buildEditorDragPayload?: (attachment: LessonAttachmentAsset) => LessonEditorAssetDragPayload | null;
}) {
    const isYoutube = attachment.kind === "youtube";
    const isLink = attachment.kind === "link";
    const isImage = isImageAttachment(attachment);
    const isVideo = isVideoAttachment(attachment);
    const [videoErrorMessage, setVideoErrorMessage] = useState<string | null>(null);
    const handleVideoError = (event: SyntheticEvent<HTMLVideoElement>) => {
        const mediaError = event.currentTarget.error;
        console.error("Lesson attachment video failed to load/play:", {
            storageKey: attachment.storageKey,
            code: mediaError?.code,
            message: mediaError?.message,
        });
        setVideoErrorMessage(getVideoErrorMessage(mediaError));
    };
    const { data: youtubeData } = useYoutubeOembedQuery(
        attachment.url,
        isYoutube && !attachment.youtubeTitle,
    );
    const filePreviewUrl = attachment.previewUrl || previewUrl || "";
    const youtubeEmbedUrl = isYoutube && attachment.url ? getYoutubeEmbedUrl(attachment.url) : null;
    const badge = getFileBadge(attachment);
    const title = isYoutube
        ? attachment.youtubeTitle || youtubeData?.title || attachment.name
        : attachment.name;
    const canOpen = Boolean(isLink || (attachment.storageKey && !isVideo));
    const canDeleteAttachment = Boolean(canDelete && attachment.isLessonAsset && attachment.id);
    const editorDragPayload = buildEditorDragPayload?.(attachment) || null;
    const canDragToEditor = Boolean(editorDragPayload);

    const handleDragStart = (event: DragEvent) => {
        if (!editorDragPayload) {
            event.preventDefault();
            return;
        }

        event.dataTransfer.effectAllowed = "copy";
        event.dataTransfer.setData(LESSON_EDITOR_ASSET_DRAG_TYPE, JSON.stringify(editorDragPayload));
        event.dataTransfer.setData("text/plain", editorDragPayload.url || editorDragPayload.previewUrl || editorDragPayload.name || title);
    };

    const dragHandle = canDragToEditor && (
        <span
            className="lesson-attachments__drag-handle"
            draggable
            role="button"
            tabIndex={0}
            aria-label={`Drag ${title} into lesson text`}
            title="Drag into lesson text"
            onDragStart={handleDragStart}
        >
            <DragIndicatorOutlinedIcon fontSize="small" />
        </span>
    );

    const deleteButton = canDeleteAttachment && (
        <button
            type="button"
            aria-label={`Delete ${title}`}
            className="lesson-attachments__delete"
            onClick={(event) => {
                event.preventDefault();
                event.stopPropagation();
                if (!isDeleting) {
                    onDeleteAttachment?.(attachment);
                }
            }}
            disabled={isDeleting}
        >
            {isDeleting ? (
                <span className="lesson-attachments__delete-spinner" aria-hidden="true" />
            ) : (
                <DeleteOutlineOutlinedIcon fontSize="small" />
            )}
        </button>
    );

    // Video and YouTube assets play inline instead of opening in a new tab — wrapping a native
    // <video>/<iframe> player in a clickable "open" button would fight the player's own controls.
    if (isVideo || isYoutube) {
        return (
            <div className="lesson-attachments__item">
                {dragHandle}
                {deleteButton}
                <div className="lesson-attachments__player-card">
                    {isYoutube && youtubeEmbedUrl ? (
                        <iframe
                            className="lesson-attachments__player"
                            src={youtubeEmbedUrl}
                            title={title}
                            allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share"
                            allowFullScreen
                        />
                    ) : isVideo && filePreviewUrl && !videoErrorMessage ? (
                        <video
                            className="lesson-attachments__player"
                            src={filePreviewUrl}
                            controls
                            preload="metadata"
                            onError={handleVideoError}
                        />
                    ) : isVideo && videoErrorMessage ? (
                        <div className="lesson-attachments__player lesson-attachments__player--error">
                            {videoErrorMessage}
                        </div>
                    ) : (
                        <div className="lesson-attachments__preview lesson-attachments__preview--video">
                            <SmartDisplayOutlinedIcon />
                        </div>
                    )}
                    <span className="lesson-attachments__name">{title}</span>
                    {isYoutube && (
                        <span className="lesson-attachments__meta">
                            {attachment.youtubeAuthorName || youtubeData?.authorName || "YouTube video"}
                        </span>
                    )}
                </div>
            </div>
        );
    }

    return (
        <div className="lesson-attachments__item">
            {dragHandle}
            {deleteButton}
            <button
                type="button"
                className="lesson-attachments__card"
                disabled={!canOpen}
                onClick={() => onOpenAttachment(attachment)}
            >
                <div
                    className={`lesson-attachments__preview lesson-attachments__preview--${attachment.kind || "file"}`}
                >
                    {isLink && attachment.linkImageUrl ? (
                        <img src={attachment.linkImageUrl} alt="" />
                    ) : isImage && filePreviewUrl ? (
                        <img src={filePreviewUrl} alt="" />
                    ) : isLink ? (
                        <LinkOutlinedIcon />
                    ) : isImage ? (
                        <ImageOutlinedIcon />
                    ) : (
                        <span style={{ color: badge.color }}>{badge.label}</span>
                    )}
                </div>
                <span className="lesson-attachments__name">{title}</span>
                {isLink && (
                    <span className="lesson-attachments__meta">
                        {attachment.linkSiteName || "Web source"}
                    </span>
                )}
                {canOpen && <OpenInNewOutlinedIcon aria-hidden="true" />}
            </button>
        </div>
    );
}

export function LessonAttachments({
    attachments = [],
    sourceReferences = [],
    layout = "grid",
    showTitle = true,
    canDelete = false,
    deletingAttachmentId = null,
    onDeleteAttachment,
    buildEditorDragPayload,
}: Props) {
    // Assets attached directly to the lesson and materials used to generate it are independent,
    // non-overlapping populations — both render, matching the editor's own Assets panel (see
    // LessonDetailsDialog's `allAssets = [...lessonAssets, ...sourceAttachments]`).
    const resolvedAttachments = [...attachments, ...getSourceAttachments(sourceReferences)];

    const previewStorageKeys = resolvedAttachments
        .filter((attachment) => (isImageAttachment(attachment) || isVideoAttachment(attachment)) && attachment.storageKey && !attachment.previewUrl)
        .map((attachment) => attachment.storageKey!);
    const { data: previewUrlByStorageKey } = useFilePreviewUrls(previewStorageKeys);

    const handleOpenAttachment = async (attachment: LessonAttachmentAsset) => {
        if ((attachment.kind === "youtube" || attachment.kind === "link") && attachment.url) {
            window.open(attachment.url, "_blank", "noopener,noreferrer");
            return;
        }

        if (!attachment.storageKey) {
            return;
        }

        try {
            await openStorageFile(attachment.storageKey);
        } catch (error) {
            console.error("Failed to open attachment:", error);
        }
    };

    if (resolvedAttachments.length === 0) {
        return null;
    }

    return (
        <section className={`lesson-attachments lesson-attachments--${layout}`}>
            {showTitle && <h2 className="lesson-attachments__title">Attachments</h2>}
            <div className="lesson-attachments__grid">
                {resolvedAttachments.map((attachment, index) => (
                    <AttachmentCard
                        key={attachment.id || `${attachment.sourceId}-${attachment.name}-${index}`}
                        attachment={attachment}
                        previewUrl={attachment.storageKey ? previewUrlByStorageKey?.[attachment.storageKey] : undefined}
                        canDelete={canDelete}
                        isDeleting={deletingAttachmentId != null && String(deletingAttachmentId) === String(attachment.id)}
                        onOpenAttachment={handleOpenAttachment}
                        onDeleteAttachment={onDeleteAttachment}
                        buildEditorDragPayload={buildEditorDragPayload}
                    />
                ))}
            </div>
        </section>
    );
}
