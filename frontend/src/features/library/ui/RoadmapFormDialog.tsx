import { useEffect, useMemo, useRef, useState } from "react";
import ArrowDownwardOutlinedIcon from "@mui/icons-material/ArrowDownwardOutlined";
import ArrowUpwardOutlinedIcon from "@mui/icons-material/ArrowUpwardOutlined";
import DeleteOutlineOutlinedIcon from "@mui/icons-material/DeleteOutlineOutlined";
import { Autocomplete, TextField } from "@mui/material";
import { Dialog } from "@/shared/ui/Dialog";
import { Button } from "@/shared/ui/Button";
import { clampEntityTitle, MAX_ENTITY_TITLE_LENGTH } from "@/shared/lib/entityTitle";
import {
    lessonTagFieldInputProps,
    normalizeLessonTagInput,
    suggestedLessonTags,
} from "@/shared/lib/lessonTags";
import type { LibraryLesson, LibraryRoadmap } from "../api/types";
import { DiscardChangesDialog } from "./DiscardChangesDialog";

interface RoadmapFormState {
    title: string;
    description: string;
    tags: string[];
    selectedLessons: LibraryLesson[];
}

/** Ordered lesson ids from the roadmap card payload (lessonIds preferred, lessons as fallback). */
function getRoadmapLessonIds(roadmap: LibraryRoadmap | null | undefined): number[] {
    if (!roadmap) {
        return [];
    }
    if (Array.isArray(roadmap.lessonIds) && roadmap.lessonIds.length > 0) {
        return roadmap.lessonIds;
    }
    return (roadmap.lessons || []).map((lesson) => lesson.id).filter((id) => Number.isFinite(id));
}

/**
 * Resolve selected lessons for the edit form.
 * Prefer the full catalog when loaded; otherwise seed immediately from roadmap.lessons so the
 * first open is not empty while the picker query is still in flight.
 */
function resolveSelectedLessons(
    initialRoadmap: LibraryRoadmap | null,
    lessons: LibraryLesson[],
): LibraryLesson[] {
    if (!initialRoadmap) {
        return [];
    }

    const lessonIds = getRoadmapLessonIds(initialRoadmap);
    if (lessonIds.length === 0) {
        return [];
    }

    const catalogById = new Map(
        lessons
            .filter((lesson) => lesson.status === "ready" && (lesson.isPublished || lesson.publicationStatus === "published"))
            .map((lesson) => [lesson.id, lesson]),
    );
    const roadmapLessonById = new Map((initialRoadmap.lessons || []).map((lesson) => [lesson.id, lesson]));

    return lessonIds
        .map((lessonId) => {
            const fromCatalog = catalogById.get(lessonId);
            if (fromCatalog) {
                return fromCatalog;
            }
            const fromRoadmap = roadmapLessonById.get(lessonId);
            if (!fromRoadmap) {
                return null;
            }
            return {
                id: fromRoadmap.id,
                title: fromRoadmap.title || `Lesson ${fromRoadmap.id}`,
                description: fromRoadmap.description || "",
                status: fromRoadmap.status || "ready",
                publicationStatus: "published",
                contentMarkdownPreview: "",
                contentHtmlPreview: "",
                isPublished: true,
                isArchived: false,
                tags: [],
                createdBy: "",
                createdAt: fromRoadmap.createdAt || "",
                updatedAt: fromRoadmap.createdAt || "",
            } as LibraryLesson;
        })
        .filter(Boolean) as LibraryLesson[];
}

function buildInitialForm(initialRoadmap: LibraryRoadmap | null, lessons: LibraryLesson[]): RoadmapFormState {
    if (initialRoadmap) {
        return {
            title: clampEntityTitle(initialRoadmap.title || ""),
            description: initialRoadmap.description || "",
            tags: normalizeLessonTagInput(initialRoadmap.tags || []),
            selectedLessons: resolveSelectedLessons(initialRoadmap, lessons),
        };
    }

    return { title: "", description: "", tags: [], selectedLessons: [] };
}

function serializeRoadmapForm(form: RoadmapFormState): string {
    return JSON.stringify({
        title: form.title,
        description: form.description,
        tags: form.tags,
        lessonIds: form.selectedLessons.map((lesson) => lesson.id),
    });
}

interface RoadmapFormDialogProps {
    open: boolean;
    lessons: LibraryLesson[];
    isSaving?: boolean;
    isDeleting?: boolean;
    mode?: "create" | "edit";
    initialRoadmap?: LibraryRoadmap | null;
    onClose: () => void;
    onSave: (formData: { title: string; description: string; tags: string[]; lessonIds: number[] }) => void;
    onDelete?: (roadmap: LibraryRoadmap) => void;
    onValidationError?: (message: string) => void;
}

export function RoadmapFormDialog({
    open,
    lessons,
    isSaving = false,
    isDeleting = false,
    mode = "create",
    initialRoadmap = null,
    onClose,
    onSave,
    onDelete,
    onValidationError,
}: RoadmapFormDialogProps) {
    const [form, setForm] = useState(() => buildInitialForm(initialRoadmap, lessons));
    const [baselineForm, setBaselineForm] = useState(() =>
        serializeRoadmapForm(buildInitialForm(initialRoadmap, lessons)),
    );
    const [errors, setErrors] = useState<Record<string, string>>({});
    const [pendingLessonId, setPendingLessonId] = useState("");
    const [isDiscardOpen, setIsDiscardOpen] = useState(false);
    const isEditMode = mode === "edit";
    const expectedLessonCount = getRoadmapLessonIds(initialRoadmap).length;
    const isDirty = serializeRoadmapForm(form) !== baselineForm;
    const baselineFormRef = useRef(baselineForm);
    baselineFormRef.current = baselineForm;

    // Reset form whenever the dialog opens or the edited roadmap changes.
    useEffect(() => {
        if (!open) {
            setIsDiscardOpen(false);
            return;
        }
        const nextForm = buildInitialForm(initialRoadmap, lessons);
        setForm(nextForm);
        setBaselineForm(serializeRoadmapForm(nextForm));
        setErrors({});
        setPendingLessonId("");
        setIsDiscardOpen(false);
    }, [open, initialRoadmap?.id]);

    // Enrich selected lessons when the lazy picker catalog arrives (or grows).
    // When the form is still at baseline, also move the baseline so hydration is not "dirty".
    useEffect(() => {
        if (!open || !isEditMode || lessons.length === 0 || expectedLessonCount === 0) {
            return;
        }
        setForm((prev) => {
            const resolved = resolveSelectedLessons(initialRoadmap, lessons);
            if (resolved.length === 0) {
                return prev;
            }
            const prevIds = prev.selectedLessons.map((lesson) => lesson.id).join(",");
            const nextIds = resolved.map((lesson) => lesson.id).join(",");
            const prevNeedsEnrichment = prev.selectedLessons.some(
                (lesson) => !lessons.some((catalogLesson) => catalogLesson.id === lesson.id),
            );
            if (prevIds === nextIds && !prevNeedsEnrichment && prev.selectedLessons.length === resolved.length) {
                return prev;
            }
            const nextForm = { ...prev, selectedLessons: resolved };
            if (serializeRoadmapForm(prev) === baselineFormRef.current) {
                setBaselineForm(serializeRoadmapForm(nextForm));
            }
            return nextForm;
        });
    }, [open, isEditMode, lessons, initialRoadmap, expectedLessonCount]);

    const requestClose = () => {
        if (isSaving || isDeleting) {
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

    const readyLessons = useMemo(
        () => lessons.filter((lesson) => lesson.status === "ready" && lesson.isPublished),
        [lessons],
    );
    const availableLessons = useMemo(() => {
        const selectedLessonIds = new Set(form.selectedLessons.map((lesson) => lesson.id));
        return readyLessons.filter((lesson) => !selectedLessonIds.has(lesson.id));
    }, [form.selectedLessons, readyLessons]);

    const validateForm = () => {
        const nextErrors: Record<string, string> = {};
        if (!form.title.trim()) {
            nextErrors.title = "Title is required.";
        }
        if (form.selectedLessons.length === 0) {
            nextErrors.selectedLessons = "Select at least one lesson.";
        }
        setErrors(nextErrors);
        return Object.keys(nextErrors).length === 0;
    };

    const handleSubmit = () => {
        if (!validateForm()) {
            const message =
                (!form.title.trim() && "Title is required.") ||
                (form.selectedLessons.length === 0 && "Select at least one lesson.") ||
                "Check the roadmap form.";
            onValidationError?.(message);
            return;
        }

        onSave({
            title: form.title.trim(),
            description: form.description.trim(),
            tags: form.tags,
            lessonIds: form.selectedLessons.map((lesson) => lesson.id),
        });
    };

    const moveSelectedLesson = (fromIndex: number, toIndex: number) => {
        setForm((prev) => {
            if (toIndex < 0 || toIndex >= prev.selectedLessons.length || fromIndex === toIndex) {
                return prev;
            }
            const nextLessons = [...prev.selectedLessons];
            const [movedLesson] = nextLessons.splice(fromIndex, 1);
            nextLessons.splice(toIndex, 0, movedLesson);
            return { ...prev, selectedLessons: nextLessons };
        });
    };

    return (
        <>
        <Dialog
            open={open}
            onClose={isSaving || isDeleting ? undefined : requestClose}
            size="md"
            flushBody
            title={isEditMode ? "Edit roadmap" : "Create roadmap"}
            closeDisabled={isSaving || isDeleting}
            closeLabel="Close roadmap dialog"
            footer={
                <>
                    {isEditMode && (
                        <Button
                            variant="danger"
                            onClick={() => initialRoadmap && onDelete?.(initialRoadmap)}
                            disabled={isSaving || isDeleting}
                            style={{ marginRight: "auto" }}
                        >
                            {isDeleting ? "Deleting..." : <><DeleteOutlineOutlinedIcon /> Delete Roadmap</>}
                        </Button>
                    )}
                    <Button variant="ghost" onClick={requestClose} disabled={isSaving || isDeleting}>
                        Cancel
                    </Button>
                    <Button
                        variant="primary"
                        onClick={handleSubmit}
                        disabled={isSaving || isDeleting || readyLessons.length === 0}
                    >
                        {isSaving
                            ? isEditMode
                                ? "Saving..."
                                : "Creating..."
                            : isEditMode
                              ? "Save Roadmap"
                              : "Create Roadmap"}
                    </Button>
                </>
            }
        >
            <div className="library-form-body">
                <div>
                    <h2 className="library-form-hero-title">{isEditMode ? "Edit roadmap" : "Create roadmap"}</h2>
                    <p className="library-form-hero-copy">
                        Assemble a learning path from ready lessons and set the order learners will follow.
                    </p>
                </div>

                <label className="library-field">
                    <span className="library-field__label">Title</span>
                    <input
                        className="library-field__control"
                        value={form.title}
                        onChange={(event) =>
                            setForm((prev) => ({ ...prev, title: clampEntityTitle(event.target.value) }))
                        }
                        maxLength={MAX_ENTITY_TITLE_LENGTH}
                    />
                    <small>
                        {form.title.length}/{MAX_ENTITY_TITLE_LENGTH}
                    </small>
                    {errors.title && <span className="library-alert library-alert--error">{errors.title}</span>}
                </label>

                <label className="library-field">
                    <span className="library-field__label">Description</span>
                    <textarea
                        className="library-field__textarea"
                        rows={3}
                        value={form.description}
                        placeholder="Describe what this roadmap helps people learn."
                        onChange={(event) => setForm((prev) => ({ ...prev, description: event.target.value }))}
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
                            helperText={`Roadmap tags merge with lesson tags. Max ${lessonTagFieldInputProps.maxLength} characters per tag.`}
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

                {form.selectedLessons.length > 0 && (
                    <section>
                        <p className="library-form-section-label">Lesson order</p>
                        {form.selectedLessons.map((lesson, index) => (
                            <div
                                key={lesson.id}
                                style={{
                                    display: "grid",
                                    gridTemplateColumns: "32px 1fr auto",
                                    gap: 8,
                                    alignItems: "center",
                                    marginBottom: 8,
                                    padding: 10,
                                    border: "1.5px solid #e3e5ff",
                                    borderRadius: 12,
                                }}
                            >
                                <span className="library-chip library-chip--accent library-chip--index">{index + 1}</span>
                                <strong>{lesson.title}</strong>
                                <div style={{ display: "flex", gap: 4 }}>
                                    <Button
                                        size="sm"
                                        variant="ghost"
                                        onClick={() => moveSelectedLesson(index, index - 1)}
                                        disabled={index === 0 || isSaving || isDeleting}
                                    >
                                        <ArrowUpwardOutlinedIcon />
                                    </Button>
                                    <Button
                                        size="sm"
                                        variant="ghost"
                                        onClick={() => moveSelectedLesson(index, index + 1)}
                                        disabled={index === form.selectedLessons.length - 1 || isSaving || isDeleting}
                                    >
                                        <ArrowDownwardOutlinedIcon />
                                    </Button>
                                    <Button
                                        size="sm"
                                        variant="danger"
                                        onClick={() =>
                                            setForm((prev) => ({
                                                ...prev,
                                                selectedLessons: prev.selectedLessons.filter((item) => item.id !== lesson.id),
                                            }))
                                        }
                                        disabled={isSaving || isDeleting}
                                    >
                                        <DeleteOutlineOutlinedIcon />
                                    </Button>
                                </div>
                            </div>
                        ))}
                    </section>
                )}

                <label className="library-field">
                    <span className="library-field__label">Add lesson</span>
                    <select
                        className="library-field__select"
                        value={pendingLessonId}
                        onChange={(event) => {
                            const lesson = availableLessons.find((item) => String(item.id) === event.target.value);
                            if (!lesson) {
                                setPendingLessonId("");
                                return;
                            }
                            setForm((prev) => ({
                                ...prev,
                                selectedLessons: [...prev.selectedLessons, lesson],
                                tags: normalizeLessonTagInput([
                                    ...prev.tags,
                                    ...(Array.isArray(lesson.tags) ? lesson.tags : []),
                                ]),
                            }));
                            setPendingLessonId("");
                            setErrors((prev) => ({ ...prev, selectedLessons: "" }));
                        }}
                    >
                        <option value="">Choose a lesson to append</option>
                        {availableLessons.map((lesson) => (
                            <option key={lesson.id} value={lesson.id}>
                                {lesson.title}
                            </option>
                        ))}
                    </select>
                    {errors.selectedLessons ? (
                        <span className="library-alert library-alert--error">{errors.selectedLessons}</span>
                    ) : (
                        <small>Selected lessons are added to the end of the roadmap.</small>
                    )}
                </label>

                {readyLessons.length === 0 && (
                    <p style={{ color: "#80808e" }}>
                        Create at least one ready lesson before building a roadmap.
                    </p>
                )}
            </div>
        </Dialog>

        <DiscardChangesDialog
            open={isDiscardOpen}
            onKeepEditing={() => setIsDiscardOpen(false)}
            onDiscard={handleDiscard}
            disabled={isSaving || isDeleting}
        />
        </>
    );
}
