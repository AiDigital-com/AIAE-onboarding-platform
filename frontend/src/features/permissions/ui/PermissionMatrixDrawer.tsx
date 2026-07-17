import { useEffect, useState } from "react";
import { Drawer } from "@/shared/ui/Drawer";
import { Button } from "@/shared/ui/Button";
import { getApiErrorMessage } from "@/shared/lib/apiError";
import { useSetPermissionOverridesMutation } from "../api/useSetPermissionOverridesMutation";
import type { components } from "@/shared/api/generated/schema";
import "./permission-matrix-drawer.css";

type UserSummaryV1 = components["schemas"]["UserSummaryV1"];
type UserPermissionSnapshotV1 = components["schemas"]["UserPermissionSnapshotV1"];
type PermissionDefinitionMetaV1 = components["schemas"]["PermissionDefinitionMetaV1"];

interface Preset {
    key: string;
    label: string;
    description: string;
    permissionKeys: string[];
}

const PRESETS: Preset[] = [
    {
        key: "restricted",
        label: "Restricted",
        description: "Can manage only their own team's members.",
        permissionKeys: ["teams.manage_members"],
    },
    {
        key: "standard",
        label: "Standard lead",
        description: "Recommended set for most team leads.",
        permissionKeys: [
            "teams.manage_members",
            "permissions.manage_team_members",
            "grades.manage",
            "materials.create",
            "materials.edit",
            "materials.delete",
            "lessons.create",
            "lessons.manage",
            "lessons.publish_archive",
            "lessons.manage_activities",
            "lessons.manage_assets",
            "roadmaps.create",
            "roadmaps.manage",
            "learning.enroll",
            "learning.assign",
            "learning.complete",
            "learning.ask",
        ],
    },
    {
        key: "content-owner",
        label: "Content owner",
        description: "Standard lead, plus managing any team's structure.",
        permissionKeys: [
            "teams.manage_members",
            "permissions.manage_team_members",
            "grades.manage",
            "groups.manage",
            "materials.create",
            "materials.edit",
            "materials.delete",
            "lessons.create",
            "lessons.manage",
            "lessons.publish_archive",
            "lessons.manage_activities",
            "lessons.manage_assets",
            "roadmaps.create",
            "roadmaps.manage",
            "learning.enroll",
            "learning.assign",
            "learning.complete",
            "learning.ask",
        ],
    },
];

interface PermissionMatrixDrawerProps {
    open: boolean;
    onClose: () => void;
    user: UserSummaryV1;
    permissionState: UserPermissionSnapshotV1;
    disabledKeys?: string[];
    /** Restricts the drawer to only these permission groups (e.g. hiding Admin/Teams for a member-scoped editor). Omit to show every group. */
    allowedGroups?: string[];
    /** Omit to hide the preset row entirely — presets below assume team-lead-level capabilities. */
    presets?: Preset[];
    title?: string;
    description?: string;
    footerNote?: string;
    definitions: PermissionDefinitionMetaV1[];
    onToast: (message: string, severity?: "success" | "error" | "warning") => void;
}

/** Slide-out permission editor: presets for fast-fill, grouped checkboxes for fine control. */
export function PermissionMatrixDrawer({
    open,
    onClose,
    user,
    permissionState,
    disabledKeys = [],
    allowedGroups,
    presets = PRESETS,
    title = "Team lead permissions",
    description,
    footerNote = "Changes affect only this team lead.",
    definitions: allDefinitions,
    onToast,
}: PermissionMatrixDrawerProps) {
    const mutation = useSetPermissionOverridesMutation();
    const [draft, setDraft] = useState<Record<string, boolean>>(permissionState.effective);

    useEffect(() => {
        if (open) {
            setDraft(permissionState.effective);
        }
    }, [open, permissionState]);

    const allowedGroupSet = allowedGroups ? new Set(allowedGroups) : null;
    const definitions = allowedGroupSet
        ? allDefinitions.filter((definition) => allowedGroupSet.has(definition.group))
        : allDefinitions;

    const disabledKeySet = new Set(disabledKeys);
    const groups = new Map<string, PermissionDefinitionMetaV1[]>();
    definitions.forEach((definition) => {
        const list = groups.get(definition.group) ?? [];
        list.push(definition);
        groups.set(definition.group, list);
    });

    const comparableKeys = definitions.map((definition) => definition.code).filter((code) => !disabledKeySet.has(code));
    const activePresetKey = presets.find((preset) =>
        comparableKeys.every((key) => Boolean(draft[key]) === preset.permissionKeys.includes(key)),
    )?.key;

    const applyPreset = (preset: Preset) => {
        setDraft((prev) => {
            const next = { ...prev };
            definitions.forEach((definition) => {
                if (!disabledKeySet.has(definition.code)) {
                    next[definition.code] = preset.permissionKeys.includes(definition.code);
                }
            });
            return next;
        });
    };

    const toggle = (permissionKey: string) => {
        setDraft((prev) => ({ ...prev, [permissionKey]: !prev[permissionKey] }));
    };

    const handleClose = () => {
        if (mutation.isPending) {
            return;
        }
        onClose();
    };

    const handleSave = async () => {
        try {
            await mutation.mutateAsync({ userId: user.id, overrides: draft });
            onToast("Permissions updated.");
            onClose();
        } catch (error) {
            onToast(getApiErrorMessage(error, "Failed to update permissions."), "error");
        }
    };

    return (
        <Drawer
            open={open}
            onClose={handleClose}
            size="lg"
            title={title}
            description={description ?? `${user.name || user.email} · choose a preset or override individual permissions.`}
            footer={
                <>
                    <span className="permission-drawer__footer-note">{footerNote}</span>
                    <div className="permission-drawer__footer-actions">
                        <Button variant="ghost" onClick={handleClose} disabled={mutation.isPending}>
                            Cancel
                        </Button>
                        <Button onClick={() => void handleSave()} disabled={mutation.isPending}>
                            {mutation.isPending ? "Saving…" : "Save permissions"}
                        </Button>
                    </div>
                </>
            }
        >
            {presets.length > 0 && (
                <div className="permission-drawer__presets">
                    {presets.map((preset) => (
                        <button
                            key={preset.key}
                            type="button"
                            className={`permission-drawer__preset${
                                activePresetKey === preset.key ? " permission-drawer__preset--active" : ""
                            }`}
                            disabled={mutation.isPending}
                            onClick={() => applyPreset(preset)}
                        >
                            <b>{preset.label}</b>
                            <span>{preset.description}</span>
                        </button>
                    ))}
                </div>
            )}

            {[...groups.entries()].map(([group, groupDefinitions]) => {
                const enabledCount = groupDefinitions.filter((definition) => draft[definition.code]).length;

                return (
                    <div key={group} className="permission-drawer__group">
                        <div className="permission-drawer__group-head">
                            <span className="permission-drawer__group-title">{group}</span>
                            <span className="permission-drawer__group-pill">{enabledCount} enabled</span>
                        </div>
                        <div className="permission-drawer__grid">
                            {groupDefinitions.map((definition) => (
                                <label key={definition.code} className="permission-drawer__item">
                                    <input
                                        type="checkbox"
                                        checked={Boolean(draft[definition.code])}
                                        disabled={mutation.isPending || disabledKeySet.has(definition.code)}
                                        onChange={() => toggle(definition.code)}
                                    />
                                    <span className="permission-drawer__item-copy">
                                        <span className="permission-drawer__item-label">{definition.label}</span>
                                        {definition.description && (
                                            <span className="permission-drawer__item-description">
                                                {definition.description}
                                            </span>
                                        )}
                                    </span>
                                </label>
                            ))}
                        </div>
                    </div>
                );
            })}
        </Drawer>
    );
}
