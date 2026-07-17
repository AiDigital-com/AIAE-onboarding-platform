import { useMemo, useState } from "react";
import CheckOutlinedIcon from "@mui/icons-material/CheckOutlined";
import AutoAwesomeOutlinedIcon from "@mui/icons-material/AutoAwesomeOutlined";
import InfoOutlinedIcon from "@mui/icons-material/InfoOutlined";
import { Autocomplete, TextField } from "@mui/material";
import { Button } from "@/shared/ui/Button";
import { useTaskTray } from "@/shared/context/TaskTrayContext";
import { SimpleEditor } from "@/shared/ui/SimpleEditor";
import { clampEntityTitle, MAX_ENTITY_TITLE_LENGTH } from "@/shared/lib/entityTitle";
import {
    lessonTagFieldInputProps,
    normalizeLessonTagInput,
    suggestedLessonTags,
} from "@/shared/lib/lessonTags";
import { useCreateLessonMutation } from "../api/useLessonMutations";
import { toLessonContentPreview } from "../api/normalizers";
import type { LibraryLesson, LibraryMaterial } from "../api/types";
import type { components } from "@/shared/api/generated/schema";

type LessonV1 = components["schemas"]["LessonV1"];

/** Widens a freshly created/generated LessonV1 into a LibraryLesson, deriving list-card preview fields. */
function toLibraryLesson(lesson: LessonV1): LibraryLesson {
    return {
        ...(lesson as unknown as LibraryLesson),
        contentHtmlPreview: toLessonContentPreview(lesson.contentHtml),
        contentMarkdownPreview: toLessonContentPreview(lesson.contentMarkdown),
    };
}

const depthOptions = [
    { value: "intro", label: "Intro" },
    { value: "standard", label: "Standard" },
    { value: "deep", label: "Deep" },
];

const toneOptions = [
    { value: "clear", label: "Clear" },
    { value: "friendly", label: "Friendly" },
    { value: "course-like", label: "Course-like" },
];

// Course Article and Internal Wiki Page are hidden for now — the generation prompt doesn't
// yet differentiate between formats, so offering them would be misleading.
const formatOptions = [{ value: "structured theoretical lesson", label: "Structured Lesson" }];

interface LessonPromptFormProps {
    materials: LibraryMaterial[];
    onLessonGenerated?: (lesson: LibraryLesson) => void | Promise<void>;
    onLessonGenerationStarted?: () => void;
}

export function LessonPromptForm({
    materials,
    onLessonGenerated,
    onLessonGenerationStarted,
}: LessonPromptFormProps) {
    const { addTask, updateTask } = useTaskTray();
    const createLesson = useCreateLessonMutation();
    const [mode, setMode] = useState<"ai" | "manual">("ai");
    const [selectedMaterialIds, setSelectedMaterialIds] = useState<number[]>([]);
    const [materialQuery, setMaterialQuery] = useState("");
    const [userInstructions, setUserInstructions] = useState("");
    const [depth, setDepth] = useState("standard");
    const [tone, setTone] = useState("clear");
    const [desiredFormat, setDesiredFormat] = useState("structured theoretical lesson");
    const [tags, setTags] = useState<string[]>([]);
    const [manualTitle, setManualTitle] = useState("");
    const [manualDescription, setManualDescription] = useState("");
    const [manualContentHtml, setManualContentHtml] = useState(
        "<h1>Lesson title</h1><p>Start writing the lesson here.</p>",
    );
    const [submitAction, setSubmitAction] = useState("");
    const [statusMessage, setStatusMessage] = useState("");
    const [errorMessage, setErrorMessage] = useState("");

    const filteredMaterials = useMemo(() => {
        const normalizedQuery = materialQuery.trim().toLowerCase();
        if (!normalizedQuery) {
            return materials;
        }

        return materials.filter((material) => {
            const searchableText = [material.title, material.description, ...(material.tags || [])]
                .filter(Boolean)
                .join(" ")
                .toLowerCase();
            return searchableText.includes(normalizedQuery);
        });
    }, [materialQuery, materials]);

    const canSubmit = selectedMaterialIds.length > 0 || userInstructions.trim().length > 0;
    const canSubmitManual =
        manualTitle.trim().length > 0 &&
        manualContentHtml.replace(/<[^>]+>/g, " ").replace(/\s+/g, " ").trim().length > 0;
    const isSubmitting = createLesson.isPending;

    const handleMaterialToggle = (materialId: number) => {
        setSelectedMaterialIds((prev) =>
            prev.includes(materialId) ? prev.filter((id) => id !== materialId) : [...prev, materialId],
        );
    };

    const submitLessonRequest = async (action: "generate") => {
        setStatusMessage("");
        setErrorMessage("");

        if (!canSubmit) {
            setErrorMessage("Select at least one material or describe what the lesson should be about.");
            return;
        }

        let taskId: string | null = null;

        try {
            setSubmitAction(action);
            taskId = addTask({
                title: "Generating lesson",
                description: selectedMaterialIds.length
                    ? `Preparing ${selectedMaterialIds.length} source material(s)...`
                    : "Preparing prompt instructions...",
            });

            onLessonGenerationStarted?.();
            if (taskId) {
                updateTask(taskId, { description: "Generating lesson with AI..." });
            }

            const data = await createLesson.mutateAsync({
                action,
                materialIds: selectedMaterialIds,
                userInstructions,
                depth,
                tone,
                desiredFormat,
                tags,
            });

            setStatusMessage("Lesson generated and saved.");

            if (data?.lesson && onLessonGenerated) {
                if (taskId) {
                    updateTask(taskId, { description: "Refreshing lesson library..." });
                }
                await onLessonGenerated(toLibraryLesson(data.lesson));
                if (taskId) {
                    updateTask(taskId, {
                        status: "success",
                        description: data.lesson.title
                            ? `Lesson ready: ${data.lesson.title}`
                            : "Lesson generated successfully.",
                    });
                }
            }
        } catch (error) {
            const message = error instanceof Error ? error.message : "Lesson request failed.";
            setErrorMessage(message);
            if (taskId) {
                updateTask(taskId, { status: "error", description: message });
            }
        } finally {
            setSubmitAction("");
        }
    };

    const submitManualLesson = async () => {
        setStatusMessage("");
        setErrorMessage("");

        if (!canSubmitManual) {
            setErrorMessage("Add a title and lesson content before saving.");
            return;
        }

        try {
            setSubmitAction("create-manual");
            const data = await createLesson.mutateAsync({
                action: "create-manual",
                title: manualTitle,
                description: manualDescription,
                contentHtml: manualContentHtml,
                tags,
            });

            setStatusMessage("Lesson created successfully.");
            setManualTitle("");
            setManualDescription("");
            setManualContentHtml("<h1>Lesson title</h1><p>Start writing the lesson here.</p>");
            setTags([]);

            if (data?.lesson && onLessonGenerated) {
                await onLessonGenerated(toLibraryLesson(data.lesson));
            }
        } catch (error) {
            setErrorMessage(error instanceof Error ? error.message : "Manual lesson creation failed.");
        } finally {
            setSubmitAction("");
        }
    };

    const handleGenerateLesson = (event: React.FormEvent) => {
        event.preventDefault();
        if (mode === "manual") {
            submitManualLesson();
            return;
        }
        submitLessonRequest("generate");
    };

    return (
        <form onSubmit={handleGenerateLesson}>
            <div className="library-form-body">
                <div>
                    <h2 className="library-form-hero-title">Create lesson</h2>
                    <p className="library-form-hero-copy">
                        Generate a lesson with AI, or paste a ready lesson without AI changes.
                    </p>
                    <div className="library-segmented" style={{ marginTop: 20 }}>
                        {[
                            { value: "ai", label: "Generate with AI" },
                            { value: "manual", label: "Ready lesson" },
                        ].map((tab) => (
                            <button
                                key={tab.value}
                                type="button"
                                className={[
                                    "library-segmented__btn",
                                    mode === tab.value ? "library-segmented__btn--active" : "",
                                ]
                                    .filter(Boolean)
                                    .join(" ")}
                                onClick={() => {
                                    setMode(tab.value as "ai" | "manual");
                                    setStatusMessage("");
                                    setErrorMessage("");
                                }}
                            >
                                {tab.label}
                            </button>
                        ))}
                    </div>
                </div>

                {errorMessage && <p className="library-alert library-alert--error">{errorMessage}</p>}
                {statusMessage && <p className="library-alert library-alert--success">{statusMessage}</p>}

                {mode === "manual" ? (
                    <>
                        <label className="library-field">
                            <span className="library-field__label">Lesson title</span>
                            <input
                                className="library-field__control"
                                value={manualTitle}
                                onChange={(event) => setManualTitle(clampEntityTitle(event.target.value))}
                                placeholder="e.g. Performance Max Campaign Setup"
                                maxLength={MAX_ENTITY_TITLE_LENGTH}
                                required
                            />
                            <small>
                                {manualTitle.length}/{MAX_ENTITY_TITLE_LENGTH}
                            </small>
                        </label>
                        <label className="library-field">
                            <span className="library-field__label">Description</span>
                            <textarea
                                className="library-field__textarea"
                                rows={2}
                                value={manualDescription}
                                onChange={(event) => setManualDescription(event.target.value)}
                                placeholder="Short summary shown on lesson cards."
                            />
                            <small>Short summary shown on lesson cards.</small>
                        </label>
                        <div className="library-field">
                            <span className="library-field__label">Tags</span>
                            <Autocomplete
                                multiple
                                freeSolo
                                options={suggestedLessonTags as unknown as string[]}
                                value={tags}
                                onChange={(_e, next) => setTags(normalizeLessonTagInput(next))}
                                slotProps={{ popper: { sx: { zIndex: 1500 } } }}
                                renderInput={(params) => (
                                    <TextField
                                        {...params}
                                        label="Tags"
                                        placeholder="Add a tag"
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
                            <small>
                                Optional categories for filtering. Max {lessonTagFieldInputProps.maxLength} characters
                                per tag.
                            </small>
                        </div>
                        <section>
                            <p className="library-form-section-label">Lesson content</p>
                            <small>Paste or write. Headings, lists and rich formatting are preserved.</small>
                            <div style={{ minHeight: 500, border: "1.5px solid #e3e5ff", borderRadius: 12 }}>
                                <SimpleEditor
                                    content={manualContentHtml}
                                    editable
                                    onChange={(nextHtml) => setManualContentHtml(nextHtml)}
                                    className="manual-lesson-editor"
                                />
                            </div>
                        </section>
                    </>
                ) : (
                    <>
                        <section>
                            <div style={{ display: "flex", justifyContent: "space-between", gap: 12 }}>
                                <p className="library-form-section-label">Source materials</p>
                                <span className="library-chip library-chip--accent library-chip--counter">
                                    {selectedMaterialIds.length} selected
                                </span>
                            </div>
                            <div className="library-search__input-wrap" style={{ marginBottom: 12 }}>
                                <input
                                    className="library-search__input"
                                    value={materialQuery}
                                    placeholder="Search materials by title..."
                                    onChange={(event) => setMaterialQuery(event.target.value)}
                                />
                                <span style={{ color: "#80808e", fontSize: 11 }}>
                                    {filteredMaterials.length} of {materials.length}
                                </span>
                            </div>
                            <div className="library-material-picker">
                                {filteredMaterials.map((material) => {
                                    const isSelected = selectedMaterialIds.includes(material.id);
                                    return (
                                        <button
                                            key={material.id}
                                            type="button"
                                            className={[
                                                "library-material-option",
                                                isSelected ? "library-material-option--selected" : "",
                                            ]
                                                .filter(Boolean)
                                                .join(" ")}
                                            onClick={() => handleMaterialToggle(material.id)}
                                        >
                                            <span>{isSelected && <CheckOutlinedIcon />}</span>
                                            <span className="library-material-option__body">
                                                <strong className="library-material-option__title">{material.title}</strong>
                                                {material.description && (
                                                    <p className="library-material-option__description">{material.description}</p>
                                                )}
                                            </span>
                                        </button>
                                    );
                                })}
                            </div>
                        </section>

                        <label className="library-field">
                            <span className="library-field__label">Extra instructions</span>
                            <textarea
                                className="library-field__textarea"
                                rows={3}
                                value={userInstructions}
                                onChange={(event) => setUserInstructions(event.target.value)}
                                placeholder="e.g. Keep the explanation beginner-friendly, expand examples for new joiners..."
                            />
                            <small>
                                Tell the model anything specific: tone, examples to keep, things to skip.
                            </small>
                        </label>

                        <div className="library-field">
                            <span className="library-field__label">Tags</span>
                            <Autocomplete
                                multiple
                                freeSolo
                                options={suggestedLessonTags as unknown as string[]}
                                value={tags}
                                onChange={(_e, next) => setTags(normalizeLessonTagInput(next))}
                                slotProps={{ popper: { sx: { zIndex: 1500 } } }}
                                renderInput={(params) => (
                                    <TextField
                                        {...params}
                                        label="Tags"
                                        placeholder="Add a tag"
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
                            <small>
                                Optional categories for filtering. Max {lessonTagFieldInputProps.maxLength} characters
                                per tag.
                            </small>
                        </div>

                        <div className="library-filters-grid">
                            <label className="library-field">
                                <span className="library-field__label">Depth</span>
                                <select className="library-field__select" value={depth} onChange={(event) => setDepth(event.target.value)}>
                                    {depthOptions.map((option) => (
                                        <option key={option.value} value={option.value}>
                                            {option.label}
                                        </option>
                                    ))}
                                </select>
                            </label>
                            <label className="library-field">
                                <span className="library-field__label">Tone</span>
                                <select className="library-field__select" value={tone} onChange={(event) => setTone(event.target.value)}>
                                    {toneOptions.map((option) => (
                                        <option key={option.value} value={option.value}>
                                            {option.label}
                                        </option>
                                    ))}
                                </select>
                            </label>
                            <label className="library-field">
                                <span className="library-field__label">Format</span>
                                <select
                                    className="library-field__select"
                                    value={desiredFormat}
                                    onChange={(event) => setDesiredFormat(event.target.value)}
                                >
                                    {formatOptions.map((option) => (
                                        <option key={option.value} value={option.value}>
                                            {option.label}
                                        </option>
                                    ))}
                                </select>
                            </label>
                        </div>
                    </>
                )}
            </div>

            <div className="library-form-body" style={{ borderTop: "1px solid #e3e5ff", background: "#f9f9f9" }}>
                <div style={{ display: "flex", justifyContent: "space-between", gap: 12, alignItems: "center" }}>
                    <p className="library-form-footer-note">
                        <InfoOutlinedIcon />
                        {mode === "ai"
                            ? `${selectedMaterialIds.length} source${selectedMaterialIds.length === 1 ? "" : "s"} selected`
                            : "Saved directly, no AI processing"}
                    </p>
                    <Button
                        type="submit"
                        variant="primary"
                        disabled={isSubmitting || (mode === "manual" ? !canSubmitManual : !canSubmit)}
                    >
                        {mode === "ai" && !isSubmitting && <AutoAwesomeOutlinedIcon />}
                        {mode === "manual"
                            ? isSubmitting && submitAction === "create-manual"
                                ? "Creating..."
                                : "Create lesson"
                            : isSubmitting && submitAction === "generate"
                              ? "Generating..."
                              : "Generate lesson"}
                    </Button>
                </div>
            </div>
        </form>
    );
}
