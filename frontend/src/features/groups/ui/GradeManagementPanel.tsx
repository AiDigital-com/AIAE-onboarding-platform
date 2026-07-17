import { useState } from "react";
import AddOutlinedIcon from "@mui/icons-material/AddOutlined";
import { LoadingSpinner } from "@/shared/ui/LoadingSpinner";
import { FeatureEmptyState } from "@/shared/ui/FeatureEmptyState";
import { getApiErrorMessage } from "@/shared/lib/apiError";
import { useAllGradesQuery } from "../api/useGradesQuery";
import {
    useActivateGradeMutation,
    useCreateGradeMutation,
    useDeactivateGradeMutation,
    useUpdateGradeMutation,
} from "../api/useGradeMutations";
import type { GradeV1 } from "../api/types";

interface GradeManagementPanelProps {
    onToast: (message: string, severity?: "success" | "error" | "warning") => void;
}

/**
 * Grade dictionary management: create, rename, deactivate. Requires grades.manage — granted to
 * Admin and Team Lead by default, since grade values are configurable by both.
 */
export function GradeManagementPanel({ onToast }: GradeManagementPanelProps) {
    const gradesQuery = useAllGradesQuery(true);
    const createMutation = useCreateGradeMutation();
    const updateMutation = useUpdateGradeMutation();
    const deactivateMutation = useDeactivateGradeMutation();
    const activateMutation = useActivateGradeMutation();

    const [newGradeName, setNewGradeName] = useState("");
    const [gradeNameError, setGradeNameError] = useState("");
    const [isCreating, setIsCreating] = useState(false);
    const [renamingGrade, setRenamingGrade] = useState<GradeV1 | null>(null);
    const [renameValue, setRenameValue] = useState("");
    const [pendingGradeId, setPendingGradeId] = useState<number | null>(null);

    const grades = gradesQuery.data ?? [];
    const activeGrades = grades.filter((grade) => grade.isActive);
    const inactiveGrades = grades.filter((grade) => !grade.isActive);

    const createGrade = async () => {
        const trimmed = newGradeName.trim();
        if (!trimmed) {
            setGradeNameError("Grade name is required.");
            onToast("Please enter a grade name.", "error");
            return;
        }
        try {
            setIsCreating(true);
            setGradeNameError("");
            await createMutation.mutateAsync(trimmed);
            setNewGradeName("");
            onToast("Grade created.");
        } catch (error) {
            const message = getApiErrorMessage(error, "Failed to create grade.");
            const isDuplicate =
                /already exists/i.test(message) || /conflict/i.test(message) || /409/.test(message);
            const displayMessage = isDuplicate ? "A grade with this name already exists." : message;
            setGradeNameError(displayMessage);
            onToast(displayMessage, "error");
        } finally {
            setIsCreating(false);
        }
    };

    const startRename = (grade: GradeV1) => {
        setRenamingGrade(grade);
        setRenameValue(grade.name);
    };

    const saveRename = async () => {
        if (!renamingGrade) {
            return;
        }
        const trimmed = renameValue.trim();
        if (!trimmed) {
            onToast("Grade name cannot be empty.", "warning");
            return;
        }
        try {
            setPendingGradeId(renamingGrade.id);
            await updateMutation.mutateAsync({ gradeId: renamingGrade.id, name: trimmed });
            onToast("Grade renamed.");
            setRenamingGrade(null);
        } catch (error) {
            onToast(getApiErrorMessage(error, "Failed to rename grade."), "error");
        } finally {
            setPendingGradeId(null);
        }
    };

    const deactivateGrade = async (grade: GradeV1) => {
        try {
            setPendingGradeId(grade.id);
            await deactivateMutation.mutateAsync(grade.id);
            onToast("Grade deactivated.");
        } catch (error) {
            onToast(getApiErrorMessage(error, "Failed to deactivate grade."), "error");
        } finally {
            setPendingGradeId(null);
        }
    };

    const activateGrade = async (grade: GradeV1) => {
        try {
            setPendingGradeId(grade.id);
            await activateMutation.mutateAsync(grade.id);
            onToast("Grade activated.");
        } catch (error) {
            onToast(getApiErrorMessage(error, "Failed to activate grade."), "error");
        } finally {
            setPendingGradeId(null);
        }
    };

    const renderGradeRow = (grade: GradeV1) => {
        const isPending = pendingGradeId === grade.id;
        const isRenaming = renamingGrade?.id === grade.id;

        return (
            <div key={grade.id} className="group-grade-row">
                {isRenaming ? (
                    <input
                        className="group-grade-row__rename-input"
                        type="text"
                        value={renameValue}
                        autoFocus
                        disabled={isPending}
                        onChange={(event) => setRenameValue(event.target.value)}
                        onKeyDown={(event) => {
                            if (event.key === "Enter") {
                                void saveRename();
                            } else if (event.key === "Escape") {
                                setRenamingGrade(null);
                            }
                        }}
                        onBlur={() => void saveRename()}
                    />
                ) : (
                    <span className="group-grade-row__name">{grade.name}</span>
                )}

                {!grade.isActive && <span className="group-grade-row__status">Inactive</span>}

                {!isRenaming && (
                    <div className="group-grade-row__actions">
                        {grade.isActive ? (
                            <>
                                <button
                                    type="button"
                                    className="group-grade-row__action"
                                    disabled={isPending}
                                    onClick={() => startRename(grade)}
                                >
                                    Rename
                                </button>
                                <button
                                    type="button"
                                    className="group-grade-row__action group-grade-row__action--danger"
                                    disabled={isPending}
                                    onClick={() => void deactivateGrade(grade)}
                                >
                                    Deactivate
                                </button>
                            </>
                        ) : (
                            <button
                                type="button"
                                className="group-grade-row__action"
                                disabled={isPending}
                                onClick={() => void activateGrade(grade)}
                            >
                                Activate
                            </button>
                        )}
                    </div>
                )}
            </div>
        );
    };

    return (
        <div className="group-grade-panel">
            {gradesQuery.isLoading ? (
                <LoadingSpinner label="Loading grades" />
            ) : grades.length === 0 ? (
                <FeatureEmptyState
                    title="No grades yet"
                    description="Create a grade below, e.g. Junior, Middle, or Senior."
                />
            ) : (
                <>
                    {activeGrades.length > 0 && (
                        <div className="group-grade-panel__block">
                            <h3 className="group-grade-panel__block-title">Active</h3>
                            <div className="group-grade-list">{activeGrades.map(renderGradeRow)}</div>
                        </div>
                    )}
                    {inactiveGrades.length > 0 && (
                        <div className="group-grade-panel__block">
                            <h3 className="group-grade-panel__block-title">Inactive</h3>
                            <div className="group-grade-list">{inactiveGrades.map(renderGradeRow)}</div>
                        </div>
                    )}
                </>
            )}

            <div className="group-add-form group-add-form--drawer">
                <input
                    className={`group-add-form__input${gradeNameError ? " group-add-form__input--error" : ""}`}
                    type="text"
                    value={newGradeName}
                    placeholder="e.g. Middle"
                    disabled={isCreating}
                    aria-invalid={Boolean(gradeNameError)}
                    onChange={(event) => {
                        setNewGradeName(event.target.value);
                        if (gradeNameError) {
                            setGradeNameError("");
                        }
                    }}
                    onKeyDown={(event) => {
                        if (event.key === "Enter") {
                            void createGrade();
                        }
                    }}
                />
                <button
                    type="button"
                    className="group-add-form__submit"
                    disabled={isCreating || !newGradeName.trim()}
                    onClick={() => void createGrade()}
                >
                    {isCreating ? "Creating..." : <><AddOutlinedIcon /> Add grade</>}
                </button>
                {gradeNameError && <p className="group-add-form__error">{gradeNameError}</p>}
            </div>
        </div>
    );
}
