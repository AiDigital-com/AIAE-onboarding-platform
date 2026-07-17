import type { JSONContent } from "@tiptap/core";

import "@/shared/editor/styles/variables.css";
import "@/shared/editor/styles/keyframe-animations.css";

export { SimpleEditor } from "@/shared/editor/tiptap-templates/simple/simple-editor";

export type UploadedImageAsset = {
    src: string;
    alt?: string;
    title?: string;
    storageKey?: string;
};

export type SimpleEditorProps = {
    content?: string;
    editable?: boolean;
    onChange?: (html: string, json: JSONContent) => void;
    onImageUpload?: (file: File) => Promise<UploadedImageAsset | null>;
    className?: string;
};
