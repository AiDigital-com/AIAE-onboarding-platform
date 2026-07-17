import { useState } from "react";
import type { GradeV1 } from "../api/types";
import "@/shared/ui/select.css";

interface InlineGradeSelectProps {
    userId: number;
    gradeId: number | null | undefined;
    gradeName: string | null | undefined;
    grades: GradeV1[];
    canEdit: boolean;
    onChange: (userId: number, gradeId: number | null) => Promise<void>;
}

/**
 * Click-to-edit grade badge. Shows the current grade name (or a "No grade" warning chip) as
 * static text; clicking swaps it for a `<select>` that saves immediately on change.
 */
export function InlineGradeSelect({
    userId,
    gradeId,
    gradeName,
    grades,
    canEdit,
    onChange,
}: InlineGradeSelectProps) {
    const [isEditing, setIsEditing] = useState(false);
    const [isSaving, setIsSaving] = useState(false);

    if (!canEdit) {
        return gradeName ? (
            <span className="group-grade-badge">{gradeName}</span>
        ) : (
            <span className="group-grade-badge group-grade-badge--none">No grade</span>
        );
    }

    if (!isEditing) {
        return (
            <button
                type="button"
                className={
                    gradeName
                        ? "group-grade-badge group-grade-badge--editable"
                        : "group-grade-badge group-grade-badge--none group-grade-badge--editable"
                }
                onClick={() => setIsEditing(true)}
            >
                {gradeName || "No grade"}
            </button>
        );
    }

    return (
        <select
            className="ui-select group-grade-select"
            autoFocus
            disabled={isSaving}
            defaultValue={gradeId ?? ""}
            onBlur={() => setIsEditing(false)}
            onChange={async (event) => {
                const value = event.target.value;
                const nextGradeId = value === "" ? null : Number(value);
                setIsSaving(true);
                try {
                    await onChange(userId, nextGradeId);
                } finally {
                    setIsSaving(false);
                    setIsEditing(false);
                }
            }}
        >
            <option value="">No grade</option>
            {grades.map((grade) => (
                <option key={grade.id} value={grade.id}>
                    {grade.name}
                </option>
            ))}
        </select>
    );
}
