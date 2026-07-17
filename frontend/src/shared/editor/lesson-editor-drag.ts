export const LESSON_EDITOR_ASSET_DRAG_TYPE = "application/x-aidigital-lesson-editor-asset";

export interface LessonEditorAssetDragPayload {
    kind?: string;
    mimeType?: string;
    name?: string;
    storageKey?: string;
    previewUrl?: string;
    url?: string;
    title?: string;
}

export function isLessonEditorAssetDragPayload(value: unknown): value is LessonEditorAssetDragPayload {
    if (!value || typeof value !== "object") {
        return false;
    }

    const payload = value as LessonEditorAssetDragPayload;
    return Boolean(payload.url || payload.storageKey || payload.previewUrl);
}
