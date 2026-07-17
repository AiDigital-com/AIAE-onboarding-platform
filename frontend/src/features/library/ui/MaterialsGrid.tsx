import { useState, type ReactNode } from "react";
import DescriptionOutlinedIcon from "@mui/icons-material/DescriptionOutlined";
import ImageOutlinedIcon from "@mui/icons-material/ImageOutlined";
import LinkOutlinedIcon from "@mui/icons-material/LinkOutlined";
import SmartDisplayOutlinedIcon from "@mui/icons-material/SmartDisplayOutlined";
import TextSnippetOutlinedIcon from "@mui/icons-material/TextSnippetOutlined";
import { useFilePreviewUrls } from "@/shared/api/files";
import type { LibraryMaterial } from "../api/types";
import { formatDate, getYoutubeThumbnail } from "./library-utils";

interface MaterialsGridProps {
    materials: LibraryMaterial[];
    onOpenMaterial: (material: LibraryMaterial) => void;
}

function StoredImage({ previewUrl, alt }: { previewUrl?: string; alt: string }) {
    return previewUrl ? <img src={previewUrl} alt={alt} /> : null;
}

function MaterialPlaceholder({ material }: { material: LibraryMaterial }) {
    if (material.youtubeUrls?.length > 0) {
        return (
            <div className="material-placeholder material-placeholder--youtube">
                <SmartDisplayOutlinedIcon />
            </div>
        );
    }
    if (material.linkAssets?.length > 0) {
        const link = material.linkAssets[0];
        const primary = link.title || link.siteName || "Web link";
        const secondary = link.siteName || link.description;
        return (
            <div className="material-placeholder material-placeholder--link">
                <LinkOutlinedIcon />
                <span className="material-placeholder__title">{primary}</span>
                {secondary && <span className="material-placeholder__sub">{secondary}</span>}
            </div>
        );
    }
    if (material.attachments?.some((a) => a.kind === "file")) {
        const file = material.attachments.find((a) => a.kind === "file");
        const ext = file?.name?.split(".").pop()?.toUpperCase() || "FILE";
        return (
            <div className="material-placeholder material-placeholder--file">
                <DescriptionOutlinedIcon />
                <span className="material-placeholder__chip">{ext}</span>
                {file?.name && <span className="material-placeholder__sub">{file.name}</span>}
            </div>
        );
    }
    if (material.hasText) {
        return (
            <div className="material-placeholder material-placeholder--text">
                <TextSnippetOutlinedIcon />
                <span className="material-placeholder__sub">Text</span>
            </div>
        );
    }
    return (
        <div className="material-placeholder">
            <DescriptionOutlinedIcon />
            <span className="material-placeholder__sub">Material preview</span>
        </div>
    );
}

function LinkPreviewImage({ src, alt, material }: { src: string; alt: string; material: LibraryMaterial }) {
    const [failed, setFailed] = useState(false);
    if (failed) return <MaterialPlaceholder material={material} />;
    return (
        <img
            src={src}
            alt={alt}
            className="library-card__link-img"
            onError={() => setFailed(true)}
        />
    );
}

function getMaterialMetaItems(material: LibraryMaterial) {
    const items: Array<{ label: string; icon: ReactNode }> = [];

    if (material.youtubeUrls?.length > 0) {
        items.push({ label: `${material.youtubeUrls.length} YouTube`, icon: <SmartDisplayOutlinedIcon /> });
    }
    if (material.links?.length > 0) {
        items.push({ label: `${material.links.length} link${material.links.length === 1 ? "" : "s"}`, icon: <LinkOutlinedIcon /> });
    }
    if (material.hasText) {
        items.push({ label: "Text", icon: <TextSnippetOutlinedIcon /> });
    }
    if (material.attachments?.some((item) => item.kind === "file")) {
        items.push({ label: "Files", icon: <DescriptionOutlinedIcon /> });
    }
    if (material.attachments?.some((item) => item.kind === "image")) {
        items.push({ label: "Images", icon: <ImageOutlinedIcon /> });
    }

    return items;
}

export function MaterialsGrid({ materials, onOpenMaterial }: MaterialsGridProps) {
    const [expandedContent, setExpandedContent] = useState<Set<number>>(() => new Set());

    const previewStorageKeys = materials.flatMap((material) => {
        const imageAttachment = material.attachments?.find((item) => item.kind === "image");
        return [material.coverImageStorageKey, imageAttachment?.storageKey].filter(
            (key): key is string => Boolean(key),
        );
    });
    const { data: previewUrlByStorageKey } = useFilePreviewUrls(previewStorageKeys);

    const toggleExpanded = (event: React.MouseEvent, materialId: number) => {
        event.preventDefault();
        event.stopPropagation();
        setExpandedContent((prev) => {
            const next = new Set(prev);
            if (next.has(materialId)) {
                next.delete(materialId);
            } else {
                next.add(materialId);
            }
            return next;
        });
    };

    return (
        <div className="library-grid library-grid--materials">
            {materials.map((material) => {
                const tags = Array.isArray(material.tags) ? material.tags : [];
                const areTagsExpanded = expandedContent.has(material.id);
                const visibleTags = areTagsExpanded ? tags : tags.slice(0, 2);
                const hiddenTagCount = Math.max(tags.length - 2, 0);
                const metaItems = getMaterialMetaItems(material);
                const META_THRESHOLD = 3;
                const areMetaExpanded = expandedContent.has(material.id);
                const visibleMeta = areMetaExpanded ? metaItems : metaItems.slice(0, META_THRESHOLD);
                const hiddenMetaCount = Math.max(metaItems.length - META_THRESHOLD, 0);
                const firstYoutubeUrl = material.youtubeUrls?.[0] || "";
                const youtubeThumbnail = firstYoutubeUrl ? getYoutubeThumbnail(firstYoutubeUrl) : null;
                const linkPreview = material.linkAssets?.find((item) => item.imageUrl) || material.linkAssets?.[0];
                const imagePreview = material.attachments?.find((item) => item.kind === "image");
                const coverStorageKey = material.coverImageStorageKey || undefined;

                return (
                    <article
                        key={material.id}
                        className="library-card"
                        onClick={() => onOpenMaterial(material)}
                    >
                        <div className="library-card__preview-wrap">
                            <div className="library-card__preview">
                                {coverStorageKey ? (
                                    <StoredImage previewUrl={previewUrlByStorageKey?.[coverStorageKey]} alt={material.title} />
                                ) : youtubeThumbnail ? (
                                    <img src={youtubeThumbnail} alt={material.title} />
                                ) : linkPreview?.imageUrl ? (
                                    <LinkPreviewImage src={linkPreview.imageUrl} alt={linkPreview.title || material.title} material={material} />
                                ) : imagePreview ? (
                                    <StoredImage
                                        previewUrl={previewUrlByStorageKey?.[imagePreview.storageKey]}
                                        alt={material.title}
                                    />
                                ) : (
                                    <MaterialPlaceholder material={material} />
                                )}
                            </div>
                        </div>

                        <h3 className="library-card__title">{material.title}</h3>
                        {material.description && (
                            <p className="library-card__description">{material.description}</p>
                        )}

                        {tags.length > 0 && (
                            <div className="library-card__tags">
                                {visibleTags.map((tag) => (
                                    <span key={tag} className="library-chip" title={tag}>
                                        <span className="library-chip__label">{tag}</span>
                                    </span>
                                ))}
                                {hiddenTagCount > 0 && !areTagsExpanded && (
                                    <button
                                        type="button"
                                        className="library-chip library-chip--muted"
                                        onClick={(event) => toggleExpanded(event, material.id)}
                                    >
                                        +{hiddenTagCount} more
                                    </button>
                                )}
                                {hiddenTagCount > 0 && areTagsExpanded && (
                                    <button
                                        type="button"
                                        className="library-chip library-chip--muted"
                                        onClick={(event) => toggleExpanded(event, material.id)}
                                    >
                                        Less
                                    </button>
                                )}
                            </div>
                        )}

                        {metaItems.length > 0 && (
                            <div className="library-card__meta">
                                {visibleMeta.map((item) => (
                                    <span key={item.label} className="library-meta-chip">
                                        {item.icon} {item.label}
                                    </span>
                                ))}
                                {hiddenMetaCount > 0 && !areMetaExpanded && (
                                    <button
                                        type="button"
                                        className="library-chip library-chip--muted"
                                        onClick={(event) => toggleExpanded(event, material.id)}
                                    >
                                        +{hiddenMetaCount} more
                                    </button>
                                )}
                                {hiddenMetaCount > 0 && areMetaExpanded && (
                                    <button
                                        type="button"
                                        className="library-chip library-chip--muted"
                                        onClick={(event) => toggleExpanded(event, material.id)}
                                    >
                                        Less
                                    </button>
                                )}
                            </div>
                        )}

                        <footer className="library-card__footer">
                            <span>{material.createdBy || "Unknown author"}</span>
                            <span>- {formatDate(material.createdAt)}</span>
                        </footer>
                    </article>
                );
            })}
        </div>
    );
}
