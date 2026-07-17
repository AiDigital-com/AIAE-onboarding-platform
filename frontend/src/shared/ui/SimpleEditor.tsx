import { useCallback } from "react";
// Ported TipTap bundle — typed via ambient module declaration.
import { SimpleEditor as RawSimpleEditor } from "@/shared/editor/tiptap-templates/simple/simple-editor";
import { apiClient } from "@/shared/api/client";
import { getApiErrorMessage } from "@/shared/api/apiError";
import { fetchFilePreviewUrl } from "@/shared/api/files";

export interface SimpleEditorProps {
    content?: string;
    editable?: boolean;
    onChange?: (html: string) => void;
    className?: string;
    onImageUpload?: (file: File) => Promise<{ src: string; alt?: string; title?: string }>;
    onVideoUpload?: (file: File) => Promise<{ src: string; title?: string; storageKey?: string }>;
}

async function defaultImageUpload(file: File) {
    const formData = new FormData();
    formData.append("file", file);

    const { data, error } = await apiClient.POST("/api/v1/lessons/upload-file", {
        body: formData as never,
        bodySerializer: (body) => body as unknown as FormData,
    });

    if (error) {
        throw new Error(getApiErrorMessage(error, `Failed to upload file: ${file.name}`));
    }

    const storageKey = data?.storageKey;
    if (!storageKey) {
        throw new Error(`Failed to upload file: ${file.name}`);
    }

    return {
        src: await fetchFilePreviewUrl(storageKey),
        alt: file.name,
        title: file.name,
    };
}

export function SimpleEditor({
    onImageUpload,
    onVideoUpload,
    onChange,
    ...props
}: SimpleEditorProps) {
    const handleImageUpload = useCallback(
        (file: File) => (onImageUpload ?? defaultImageUpload)(file),
        [onImageUpload],
    );

    return (
        <RawSimpleEditor
            {...props}
            onChange={onChange}
            onImageUpload={handleImageUpload}
            onVideoUpload={onVideoUpload}
        />
    );
}
