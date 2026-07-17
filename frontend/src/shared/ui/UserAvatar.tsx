import { useFilePreviewUrl } from "@/shared/api/files";
import "./user-avatar.css";

interface UserLike {
    id?: string | number;
    name?: string;
    email?: string;
    avatarStorageKey?: string;
    avatarColor?: string;
}

interface UserAvatarProps {
    user: UserLike;
    size?: number;
    /** Pre-resolved avatar URL from a batch lookup; when set, skips this instance's own fetch. */
    previewUrl?: string;
}

function getInitials(user: UserLike): string {
    const source = String(user.name || user.email || "User").trim();
    return (source[0] || "U").toUpperCase();
}

function getAvatarColor(user: UserLike): string {
    return user.avatarColor || "#0009DC";
}

export function UserAvatar({ user, size = 34, previewUrl }: UserAvatarProps) {
    const { data: fetchedAvatarUrl } = useFilePreviewUrl(previewUrl ? undefined : user.avatarStorageKey);
    const avatarUrl = previewUrl || fetchedAvatarUrl;
    const style = {
        width: size,
        height: size,
        fontSize: Math.max(11, Math.round(size * 0.38)),
        backgroundColor: getAvatarColor(user),
    };

    if (avatarUrl) {
        return (
            <img
                className="user-avatar user-avatar--image"
                style={style}
                src={avatarUrl}
                alt={user.name || user.email || "User avatar"}
            />
        );
    }

    return (
        <span className="user-avatar" style={style} aria-hidden="true">
            {getInitials(user)}
        </span>
    );
}
