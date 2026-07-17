import { useEffect, useMemo, useState } from "react";
import CloudUploadOutlinedIcon from "@mui/icons-material/CloudUploadOutlined";
import DeleteOutlineOutlinedIcon from "@mui/icons-material/DeleteOutlineOutlined";
import type { components } from "@/shared/api/generated/schema";
import {
    AI_DIGITAL_AVATAR_COLORS,
} from "@/shared/lib/brandColors";
import { UserAvatar } from "@/shared/ui/UserAvatar";
import { getApiErrorMessage } from "@/shared/lib/apiError";
import { useUploadAvatarMutation } from "../api/useUploadAvatarMutation";
import { useUpdateProfileMutation } from "../api/useUpdateProfileMutation";
import "./profile.css";

type UserSummaryV1 = components["schemas"]["UserSummaryV1"];

interface Props {
    open: boolean;
    user: UserSummaryV1 | null;
    onClose: () => void;
    onSaved?: (user: UserSummaryV1) => void;
}

/** Profile settings dialog with avatar upload and color picker. */
export function ProfileDialog({ open, user, onClose, onSaved }: Props) {
    const uploadAvatarMutation = useUploadAvatarMutation();
    const updateProfileMutation = useUpdateProfileMutation();
    const [name, setName] = useState("");
    const [position, setPosition] = useState("");
    const [avatarStorageKey, setAvatarStorageKey] = useState("");
    const [avatarColor, setAvatarColor] = useState<string>(AI_DIGITAL_AVATAR_COLORS[0]);
    const [avatarFile, setAvatarFile] = useState<File | null>(null);
    const [avatarPreviewUrl, setAvatarPreviewUrl] = useState("");
    const [error, setError] = useState("");

    const isSaving = uploadAvatarMutation.isPending || updateProfileMutation.isPending;

    useEffect(() => {
        if (!open || !user) {
            return;
        }

        setName(user.name || "");
        setPosition(user.position || "");
        setAvatarStorageKey(user.avatarStorageKey || "");
        setAvatarColor(user.avatarColor || AI_DIGITAL_AVATAR_COLORS[0]);
        setAvatarFile(null);
        setAvatarPreviewUrl("");
        setError("");
    }, [open, user]);

    useEffect(() => {
        if (!avatarFile) {
            setAvatarPreviewUrl("");
            return undefined;
        }

        const objectUrl = URL.createObjectURL(avatarFile);
        setAvatarPreviewUrl(objectUrl);
        return () => URL.revokeObjectURL(objectUrl);
    }, [avatarFile]);

    const previewUser = useMemo(
        () => ({
            ...user,
            name,
            position,
            avatarStorageKey,
            avatarColor,
        }),
        [avatarColor, avatarStorageKey, name, position, user],
    );

    if (!open || !user) {
        return null;
    }

    const handleAvatarChange = (event: React.ChangeEvent<HTMLInputElement>) => {
        const file = event.target.files?.[0] || null;
        event.target.value = "";

        if (!file) {
            return;
        }

        const ALLOWED_TYPES = new Set(["image/jpeg", "image/png", "image/webp", "image/gif"]);
        if (!ALLOWED_TYPES.has(file.type)) {
            setError("Upload a JPG, PNG, WEBP, or GIF image.");
            return;
        }

        if (file.size > 2 * 1024 * 1024) {
            setError("Avatar must be smaller than 2 MB.");
            return;
        }

        setAvatarFile(file);
        setError("");
    };

    const saveProfile = async () => {
        const normalizedName = name.trim();
        if (!normalizedName) {
            setError("Name is required.");
            return;
        }

        try {
            setError("");
            let nextAvatarStorageKey = avatarStorageKey;

            if (avatarFile) {
                const uploadData = await uploadAvatarMutation.mutateAsync(avatarFile);
                nextAvatarStorageKey = uploadData.storageKey;
            }

            const profile = await updateProfileMutation.mutateAsync({
                name: normalizedName,
                position: position.trim(),
                avatarStorageKey: nextAvatarStorageKey || undefined,
                avatarColor,
            });

            onSaved?.(profile.user);
            onClose();
        } catch (saveError) {
            setError(getApiErrorMessage(saveError, "Failed to save profile."));
        }
    };

    return (
        <div
            className="profile-dialog__backdrop"
            role="presentation"
            onClick={isSaving ? undefined : onClose}
        >
            <div
                className="profile-dialog"
                role="dialog"
                aria-modal="true"
                aria-labelledby="profile-dialog-title"
                onClick={(event) => event.stopPropagation()}
            >
                <div className="profile-dialog__header">
                    <h2 id="profile-dialog-title">Profile settings</h2>
                </div>

                <div className="profile-dialog__body">
                    {error && <p className="profile-dialog__error">{error}</p>}

                    <div className="profile-dialog__avatar-row">
                        {avatarPreviewUrl ? (
                            <img
                                className="profile-dialog__avatar-preview"
                                src={avatarPreviewUrl}
                                alt=""
                            />
                        ) : (
                            <UserAvatar user={previewUser} size={72} />
                        )}

                        <div className="profile-dialog__avatar-actions">
                            <label className="profile-dialog__upload-btn">
                                <CloudUploadOutlinedIcon /> Upload avatar
                                <input
                                    type="file"
                                    accept="image/*"
                                    hidden
                                    disabled={isSaving}
                                    onChange={handleAvatarChange}
                                />
                            </label>
                            {(avatarStorageKey || avatarFile) && (
                                <button
                                    type="button"
                                    className="profile-dialog__remove-btn"
                                    disabled={isSaving}
                                    onClick={() => {
                                        setAvatarFile(null);
                                        setAvatarStorageKey("");
                                    }}
                                >
                                    <DeleteOutlineOutlinedIcon /> Remove avatar
                                </button>
                            )}
                        </div>
                    </div>

                    <div className="profile-dialog__colors">
                        <p className="profile-dialog__label">Default avatar color</p>
                        <div className="profile-dialog__color-grid">
                            {AI_DIGITAL_AVATAR_COLORS.map((color) => {
                                const isSelected = avatarColor === color;
                                return (
                                    <button
                                        key={color}
                                        type="button"
                                        aria-label={`Use avatar color ${color}`}
                                        className={[
                                            "profile-dialog__color",
                                            isSelected ? "profile-dialog__color--selected" : "",
                                        ]
                                            .filter(Boolean)
                                            .join(" ")}
                                        style={{ backgroundColor: color }}
                                        disabled={isSaving}
                                        onClick={() => setAvatarColor(color)}
                                    />
                                );
                            })}
                        </div>
                    </div>

                    <label className="profile-dialog__field">
                        <span>Name</span>
                        <input
                            value={name}
                            required
                            disabled={isSaving}
                            onChange={(event) => setName(event.target.value)}
                        />
                    </label>

                    <label className="profile-dialog__field">
                        <span>Position</span>
                        <input
                            value={position}
                            disabled={isSaving}
                            placeholder="Product designer, QA engineer, team lead..."
                            onChange={(event) => setPosition(event.target.value)}
                        />
                    </label>

                    <div className="profile-dialog__role">
                        <p className="profile-dialog__label">Account role</p>
                        <p className="profile-dialog__role-value">{user.role || "member"}</p>
                    </div>
                </div>

                <div className="profile-dialog__footer">
                    <button
                        type="button"
                        className="profile-dialog__cancel"
                        disabled={isSaving}
                        onClick={onClose}
                    >
                        Cancel
                    </button>
                    <button
                        type="button"
                        className="profile-dialog__save"
                        disabled={isSaving}
                        onClick={() => void saveProfile()}
                    >
                        {isSaving ? "Saving..." : "Save"}
                    </button>
                </div>
            </div>
        </div>
    );
}
