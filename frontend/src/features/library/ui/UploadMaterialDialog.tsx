import { useEffect, useRef, useState } from "react";
import AttachFileOutlinedIcon from "@mui/icons-material/AttachFileOutlined";
import CloseOutlinedIcon from "@mui/icons-material/CloseOutlined";
import ImageOutlinedIcon from "@mui/icons-material/ImageOutlined";
import { Autocomplete, TextField, Tooltip, IconButton } from "@mui/material";
import { Dialog } from "@/shared/ui/Dialog";
import { Button } from "@/shared/ui/Button";
import { LoadingSpinner } from "@/shared/ui/LoadingSpinner";
import { clampEntityTitle, MAX_ENTITY_TITLE_LENGTH } from "@/shared/lib/entityTitle";
import {
    lessonTagFieldInputProps,
    normalizeLessonTagInput,
    suggestedLessonTags,
} from "@/shared/lib/lessonTags";
import { useFilePreviewUrl } from "@/shared/api/files";
import { useUploadMaterialFileMutation } from "../api/useMaterialMutations";
import type { LibraryMaterial } from "../api/types";
import { DiscardChangesDialog } from "./DiscardChangesDialog";

const acceptedFileTypes = [".doc", ".docx", ".xls", ".xlsx", ".pdf", ".png", ".jpg", ".jpeg", ".webp"].join(",");

interface MaterialFormState {
    title: string;
    description: string;
    youtubeInput: string;
    youtubeUrls: string[];
    links: string;
    text: string;
    tags: string[];
    existingAttachments: LibraryMaterial["attachments"];
    newAttachments: File[];
    coverImageStorageKey: string;
    coverImageOriginalName: string;
    coverImageMimeType: string;
}

function buildInitialForm(material: LibraryMaterial | null): MaterialFormState {
    const extra = material as (LibraryMaterial & { coverImageOriginalName?: string; coverImageMimeType?: string }) | null;
    return {
        title: clampEntityTitle(material?.title || ""),
        description: material?.description || "",
        youtubeInput: "",
        youtubeUrls: material?.youtubeUrls || [],
        links: (material?.links || []).join("\n"),
        text: material?.text || "",
        tags: normalizeLessonTagInput(material?.tags || []),
        existingAttachments: material?.attachments || [],
        newAttachments: [],
        coverImageStorageKey: material?.coverImageStorageKey || "",
        coverImageOriginalName: extra?.coverImageOriginalName || "",
        coverImageMimeType: extra?.coverImageMimeType || "",
    };
}

function isValidYoutubeUrl(url: string) {
    return /^(https?:\/\/)?(www\.)?(youtube\.com|youtu\.be)\//i.test(url.trim());
}

function serializeMaterialForm(form: MaterialFormState): string {
    return JSON.stringify({
        title: form.title,
        description: form.description,
        youtubeInput: form.youtubeInput,
        youtubeUrls: form.youtubeUrls,
        links: form.links,
        text: form.text,
        tags: form.tags,
        existingAttachmentKeys: form.existingAttachments.map((item) => item.storageKey).sort(),
        newAttachments: form.newAttachments.map((file) => `${file.name}:${file.size}:${file.lastModified}`),
        coverImageStorageKey: form.coverImageStorageKey,
        coverImageOriginalName: form.coverImageOriginalName,
        coverImageMimeType: form.coverImageMimeType,
    });
}

interface UploadMaterialDialogProps {
    open: boolean;
    onClose: () => void;
    onSave: (form: MaterialFormState) => void;
    onValidationError?: (message: string) => void;
    isSaving?: boolean;
    mode?: "create" | "edit";
    initialMaterial?: LibraryMaterial | null;
    resetKey?: number;
}

export function UploadMaterialDialog({
    open,
    onClose,
    onSave,
    onValidationError,
    isSaving = false,
    mode = "create",
    initialMaterial = null,
    resetKey = 0,
}: UploadMaterialDialogProps) {
    const [form, setForm] = useState(buildInitialForm(initialMaterial));
    const [baselineForm, setBaselineForm] = useState(() => serializeMaterialForm(buildInitialForm(initialMaterial)));
    const [errors, setErrors] = useState<Record<string, string>>({});
    const [isUploadingCover, setIsUploadingCover] = useState(false);
    const [coverError, setCoverError] = useState("");
    const [isDiscardOpen, setIsDiscardOpen] = useState(false);
    const coverFileInputRef = useRef<HTMLInputElement | null>(null);
    const uploadCoverFile = useUploadMaterialFileMutation();
    const { data: coverPreviewUrl } = useFilePreviewUrl(form.coverImageStorageKey || undefined);
    const isEditMode = mode === "edit";
    const isDirty = serializeMaterialForm(form) !== baselineForm;

    useEffect(() => {
        if (!open) {
            setIsDiscardOpen(false);
            return;
        }
        const nextForm = buildInitialForm(initialMaterial);
        setForm(nextForm);
        setBaselineForm(serializeMaterialForm(nextForm));
        setErrors({});
        setCoverError("");
        setIsDiscardOpen(false);
    }, [initialMaterial, resetKey, open]);

    const requestClose = () => {
        if (isSaving) {
            return;
        }
        if (isDirty) {
            setIsDiscardOpen(true);
            return;
        }
        onClose();
    };

    const handleDiscard = () => {
        setIsDiscardOpen(false);
        onClose();
    };

    const handleCoverImageChange = async (file?: File | null) => {
        if (!file) return;
        if (!file.type?.startsWith("image/")) {
            setCoverError("Choose an image file.");
            if (coverFileInputRef.current) coverFileInputRef.current.value = "";
            return;
        }
        if (file.size > 10 * 1024 * 1024) {
            setCoverError("Cover image must be under 10 MB.");
            if (coverFileInputRef.current) coverFileInputRef.current.value = "";
            return;
        }
        try {
            setIsUploadingCover(true);
            setCoverError("");
            const uploadData = await uploadCoverFile.mutateAsync(file);
            setForm((prev) => ({
                ...prev,
                coverImageStorageKey: (uploadData as any)?.storageKey || "",
                coverImageOriginalName: file.name,
                coverImageMimeType: file.type || "image/*",
            }));
        } catch (error) {
            setCoverError(error instanceof Error ? error.message : "Failed to upload cover image.");
        } finally {
            setIsUploadingCover(false);
            if (coverFileInputRef.current) coverFileInputRef.current.value = "";
        }
    };

    const handleRemoveCoverImage = () => {
        setForm((prev) => ({ ...prev, coverImageStorageKey: "", coverImageOriginalName: "", coverImageMimeType: "" }));
        setCoverError("");
    };

    const handleChange = (field: keyof MaterialFormState) => (
        event: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>,
    ) => {
        const value = field === "title" ? clampEntityTitle(event.target.value) : event.target.value;
        setForm((prev) => ({ ...prev, [field]: value }));
        setErrors((prev) => ({ ...prev, [field]: "", content: "" }));
    };

    const commitYoutubeInput = (rawValue: string, existingUrls = form.youtubeUrls) => {
        const candidates = rawValue.split(/\s+/).map((item) => item.trim()).filter(Boolean);
        if (candidates.length === 0) {
            return { nextYoutubeUrls: existingUrls, hasError: false };
        }

        const invalidCandidate = candidates.find((item) => !isValidYoutubeUrl(item));
        if (invalidCandidate) {
            setErrors((prev) => ({ ...prev, youtubeInput: "Only valid YouTube links are allowed here." }));
            return { nextYoutubeUrls: existingUrls, hasError: true };
        }

        const uniqueCandidates = candidates.filter((item) => !existingUrls.includes(item));
        if (uniqueCandidates.length === 0) {
            setErrors((prev) => ({ ...prev, youtubeInput: "This YouTube link is already added." }));
            return { nextYoutubeUrls: existingUrls, hasError: true };
        }

        const nextYoutubeUrls = [...existingUrls, ...uniqueCandidates];
        setForm((prev) => ({ ...prev, youtubeInput: "", youtubeUrls: nextYoutubeUrls }));
        setErrors((prev) => ({ ...prev, youtubeInput: "", content: "" }));
        return { nextYoutubeUrls, hasError: false };
    };

    const getFormErrors = (formToValidate = form) => {
        const nextErrors: Record<string, string> = {};
        if (!formToValidate.title.trim()) {
            nextErrors.title = "Title is required.";
        }

        const hasAnyContent = Boolean(
            formToValidate.youtubeUrls.length > 0 ||
                formToValidate.links.trim() ||
                formToValidate.text.trim() ||
                formToValidate.existingAttachments.length > 0 ||
                formToValidate.newAttachments.length > 0,
        );

        if (!hasAnyContent) {
            nextErrors.content = "Add at least one content source.";
        }

        if (
            formToValidate.youtubeInput.trim() &&
            formToValidate.youtubeInput.split(/\s+/).some((item) => item.trim() && !isValidYoutubeUrl(item))
        ) {
            nextErrors.youtubeInput = "Only valid YouTube links are allowed here.";
        }

        return nextErrors;
    };

    const handleSubmit = () => {
        const { nextYoutubeUrls, hasError } = commitYoutubeInput(form.youtubeInput);
        if (hasError) {
            onValidationError?.("Only valid YouTube links are allowed here.");
            return;
        }

        const nextForm = { ...form, youtubeInput: "", youtubeUrls: nextYoutubeUrls };
        const nextErrors = getFormErrors(nextForm);
        setErrors(nextErrors);
        if (Object.keys(nextErrors).length > 0) {
            onValidationError?.(nextErrors.title || nextErrors.content || nextErrors.youtubeInput || "Check the material form.");
            return;
        }

        onSave(nextForm);
    };

    return (
        <>
        <Dialog
            open={open}
            onClose={isSaving ? undefined : requestClose}
            size="md"
            flushBody
            title={isEditMode ? "Edit material" : "Add material"}
            closeDisabled={isSaving}
            closeLabel="Close material dialog"
            footer={
                <>
                    <Button variant="ghost" onClick={requestClose} disabled={isSaving}>
                        Cancel
                    </Button>
                    <Button variant="primary" onClick={handleSubmit} disabled={isSaving}>
                        {isSaving
                            ? isEditMode
                                ? "Saving..."
                                : "Creating..."
                            : isEditMode
                              ? "Save material"
                              : "Create material"}
                    </Button>
                </>
            }
        >
            <div className="library-form-body">
                <div style={{ display: "flex", alignItems: "flex-start", justifyContent: "space-between", gap: 8 }}>
                    <div>
                        <h2 className="library-form-hero-title">{isEditMode ? "Edit material" : "Add material"}</h2>
                        <p className="library-form-hero-copy">
                            Add source content for lessons: videos, links, files, images, or text notes.
                        </p>
                    </div>
                    <div style={{ display: "flex", alignItems: "center", gap: 6, flexShrink: 0, paddingTop: 4 }}>
                        <Tooltip title={coverError || (coverPreviewUrl ? "Replace cover image" : "Upload cover image")}>
                            <span>
                                <IconButton
                                    size="small"
                                    aria-label={coverPreviewUrl ? "Replace material cover image" : "Upload material cover image"}
                                    onClick={() => coverFileInputRef.current?.click()}
                                    disabled={isUploadingCover || isSaving}
                                    sx={{
                                        width: 34, height: 34, borderRadius: 999,
                                        color: coverPreviewUrl ? "#fff" : "#0009DC",
                                        border: "1px solid rgba(0,9,220,0.2)",
                                        backgroundColor: coverPreviewUrl ? "#0009DC" : "#fff",
                                        backgroundImage: coverPreviewUrl ? `url("${coverPreviewUrl}")` : "none",
                                        backgroundSize: "cover",
                                        backgroundPosition: "center",
                                        boxShadow: coverPreviewUrl ? "inset 0 0 0 999px rgba(0,9,220,0.28)" : "none",
                                        "&:hover": { color: "#fff", backgroundColor: "#0009DC" },
                                        "&.Mui-disabled": { color: "#aaa", backgroundColor: "#fff", backgroundImage: "none" },
                                    }}
                                >
                                    <ImageOutlinedIcon sx={{ fontSize: 17 }} />
                                </IconButton>
                            </span>
                        </Tooltip>
                        {coverPreviewUrl && (
                            <Tooltip title="Remove cover image">
                                <span>
                                    <IconButton
                                        size="small"
                                        aria-label="Remove material cover image"
                                        onClick={handleRemoveCoverImage}
                                        disabled={isUploadingCover || isSaving}
                                        sx={{
                                            width: 34, height: 34, borderRadius: 999,
                                            color: "#D62F2F",
                                            border: "1px solid rgba(214,47,47,0.28)",
                                            backgroundColor: "#fff",
                                            "&:hover": { borderColor: "#D62F2F", backgroundColor: "rgba(214,47,47,0.05)" },
                                            "&.Mui-disabled": { color: "#aaa", backgroundColor: "#fff" },
                                        }}
                                    >
                                        <CloseOutlinedIcon sx={{ fontSize: 17 }} />
                                    </IconButton>
                                </span>
                            </Tooltip>
                        )}
                        <input
                            ref={coverFileInputRef}
                            type="file"
                            accept="image/*"
                            hidden
                            onChange={(event) => handleCoverImageChange(event.target.files?.[0])}
                        />
                    </div>
                </div>
                {(coverError || isUploadingCover) && (
                    <div style={{ display: "flex", alignItems: "center", gap: 6 }}>
                        {isUploadingCover && <LoadingSpinner label="Uploading cover" size="sm" />}
                        {coverError && <span className="library-alert library-alert--error">{coverError}</span>}
                    </div>
                )}
                <label className="library-field">
                    <span className="library-field__label">Title</span>
                    <input
                        className="library-field__control"
                        value={form.title}
                        onChange={handleChange("title")}
                        maxLength={MAX_ENTITY_TITLE_LENGTH}
                        aria-invalid={Boolean(errors.title)}
                    />
                    <small>
                        {form.title.length}/{MAX_ENTITY_TITLE_LENGTH}
                    </small>
                    {errors.title && <span className="library-alert library-alert--error">{errors.title}</span>}
                </label>

                <label className="library-field">
                    <span className="library-field__label">Short Description</span>
                    <textarea
                        className="library-field__textarea"
                        rows={2}
                        value={form.description}
                        onChange={handleChange("description")}
                    />
                </label>

                <Autocomplete
                    multiple
                    freeSolo
                    options={suggestedLessonTags as unknown as string[]}
                    value={form.tags}
                    slotProps={{ popper: { sx: { zIndex: 1500 } } }}
                    onChange={(_event, nextTags) =>
                        setForm((prev) => ({ ...prev, tags: normalizeLessonTagInput(nextTags) }))
                    }
                    size="small"
                    renderInput={(params) => (
                        <TextField
                            {...params}
                            label="Tags"
                            placeholder="Add a tag"
                            helperText={`Optional categories for filtering. Max ${lessonTagFieldInputProps.maxLength} characters per tag.`}
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

                <section>
                    <p className="library-form-section-label">YouTube videos</p>
                    <label className="library-field">
                        <span className="library-field__label">Paste YouTube URL and press Enter</span>
                        <input
                            className="library-field__control"
                            placeholder="https://www.youtube.com/watch?v=..."
                            value={form.youtubeInput}
                            onChange={handleChange("youtubeInput")}
                            onKeyDown={(event) => {
                                if (event.key === "Enter" || event.key === " ") {
                                    event.preventDefault();
                                    commitYoutubeInput(form.youtubeInput);
                                }
                            }}
                            onBlur={() => commitYoutubeInput(form.youtubeInput)}
                        />
                        <small>
                            {errors.youtubeInput ||
                                "A YouTube link will be added automatically on Enter, space, blur, or save."}
                        </small>
                    </label>
                    <div className="library-card__tags">
                        {form.youtubeUrls.map((url) => (
                            <button
                                key={url}
                                type="button"
                                className="library-chip library-chip--accent library-chip--removable library-chip--youtube"
                                title={url}
                                onClick={() =>
                                    setForm((prev) => ({
                                        ...prev,
                                        youtubeUrls: prev.youtubeUrls.filter((item) => item !== url),
                                    }))
                                }
                            >
                                <span className="library-chip__label">{url}</span>
                                <span className="library-chip__icon"><CloseOutlinedIcon /></span>
                            </button>
                        ))}
                    </div>
                </section>

                <section>
                    <p className="library-form-section-label">Links</p>
                    <label className="library-field">
                        <span className="library-field__label">One link per line</span>
                        <textarea
                            className="library-field__textarea"
                            rows={3}
                            placeholder={"https://example.com\nhttps://docs.example.com"}
                            value={form.links}
                            onChange={handleChange("links")}
                        />
                    </label>
                </section>

                <section>
                    <p className="library-form-section-label">Text</p>
                    <label className="library-field">
                        <span className="library-field__label">Text Content</span>
                        <textarea
                            className="library-field__textarea"
                            rows={5}
                            value={form.text}
                            onChange={handleChange("text")}
                        />
                    </label>
                </section>

                <section>
                    <p className="library-form-section-label">Files and images</p>
                    <label className="library-field">
                        <span className="ui-btn ui-btn--ghost" style={{ display: "inline-flex" }}>
                            <AttachFileOutlinedIcon /> {isEditMode ? "Add More Files" : "Choose Files"}
                        </span>
                        <input
                            hidden
                            type="file"
                            multiple
                            accept={acceptedFileTypes}
                            onChange={(event) => {
                                const selectedFiles = Array.from(event.target.files || []);
                                setForm((prev) => ({
                                    ...prev,
                                    newAttachments: [...prev.newAttachments, ...selectedFiles],
                                }));
                                if (selectedFiles.length > 0) {
                                    setErrors((prev) => ({ ...prev, content: "" }));
                                }
                                event.target.value = "";
                            }}
                        />
                    </label>
                    <p>Supported: doc, docx, xls, xlsx, pdf, png, jpg, jpeg, webp</p>

                    {form.existingAttachments.length > 0 && (
                        <div style={{ marginTop: 12 }}>
                            <strong>Existing attachments</strong>
                            <div className="library-card__tags" style={{ marginTop: 8 }}>
                                {form.existingAttachments.map((attachment) => (
                                    <button
                                        key={attachment.id || attachment.storageKey}
                                        type="button"
                                        className="library-chip library-chip--removable library-chip--attachment"
                                        title={attachment.name}
                                        onClick={() =>
                                            setForm((prev) => ({
                                                ...prev,
                                                existingAttachments: prev.existingAttachments.filter(
                                                    (item) => item.storageKey !== attachment.storageKey,
                                                ),
                                            }))
                                        }
                                    >
                                        <span className="library-chip__label">{attachment.name}</span>
                                        <span className="library-chip__icon"><CloseOutlinedIcon /></span>
                                    </button>
                                ))}
                            </div>
                        </div>
                    )}

                    {form.newAttachments.length > 0 && (
                        <div style={{ marginTop: form.existingAttachments.length > 0 ? 20 : 12 }}>
                            <strong>New attachments</strong>
                            <div className="library-card__tags" style={{ marginTop: 8 }}>
                                {form.newAttachments.map((file) => (
                                    <button
                                        key={`${file.name}-${file.size}-${file.lastModified}`}
                                        type="button"
                                        className="library-chip library-chip--removable library-chip--attachment"
                                        title={file.name}
                                        onClick={() =>
                                            setForm((prev) => ({
                                                ...prev,
                                                newAttachments: prev.newAttachments.filter((item) => item !== file),
                                            }))
                                        }
                                    >
                                        <span className="library-chip__label">{file.name}</span>
                                        <span className="library-chip__icon"><CloseOutlinedIcon /></span>
                                    </button>
                                ))}
                            </div>
                        </div>
                    )}
                </section>
            </div>
        </Dialog>

        <DiscardChangesDialog
            open={isDiscardOpen}
            onKeepEditing={() => setIsDiscardOpen(false)}
            onDiscard={handleDiscard}
            disabled={isSaving}
        />
        </>
    );
}
