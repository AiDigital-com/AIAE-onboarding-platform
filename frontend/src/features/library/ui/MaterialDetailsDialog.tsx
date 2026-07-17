import { useState } from "react";
import DeleteOutlineOutlinedIcon from "@mui/icons-material/DeleteOutlineOutlined";
import DescriptionOutlinedIcon from "@mui/icons-material/DescriptionOutlined";
import EditOutlinedIcon from "@mui/icons-material/EditOutlined";
import ImageOutlinedIcon from "@mui/icons-material/ImageOutlined";
import LinkOutlinedIcon from "@mui/icons-material/LinkOutlined";
import OpenInNewOutlinedIcon from "@mui/icons-material/OpenInNewOutlined";
import SmartDisplayOutlinedIcon from "@mui/icons-material/SmartDisplayOutlined";
import { Dialog } from "@/shared/ui/Dialog";
import { Button } from "@/shared/ui/Button";
import { AI_DIGITAL_COLORS, hexToRgba } from "@/shared/lib/brandColors";
import { openStorageFile, useFilePreviewUrls } from "@/shared/api/files";
import type { LibraryMaterial } from "../api/types";
import { formatDate, formatFileSize, getYoutubeEmbedUrl } from "./library-utils";

const badgeStyles = [
    { backgroundColor: hexToRgba(AI_DIGITAL_COLORS.lime, 0.72), color: AI_DIGITAL_COLORS.midnightCharcoal },
    { backgroundColor: hexToRgba(AI_DIGITAL_COLORS.brightAqua, 0.62), color: AI_DIGITAL_COLORS.midnightCharcoal },
    { backgroundColor: hexToRgba(AI_DIGITAL_COLORS.pink, 0.26), color: AI_DIGITAL_COLORS.midnightCharcoal },
    { backgroundColor: hexToRgba(AI_DIGITAL_COLORS.skywave, 0.5), color: AI_DIGITAL_COLORS.midnightCharcoal },
    { backgroundColor: hexToRgba(AI_DIGITAL_COLORS.digitalLilac, 0.48), color: AI_DIGITAL_COLORS.midnightCharcoal },
];

function MaterialBadge({ label, index = 0 }: { label: string; index?: number }) {
    const style = badgeStyles[index % badgeStyles.length];
    return (
        <span className="library-chip" style={style} title={label}>
            {label}
        </span>
    );
}

function StoredImage({ previewUrl, alt }: { previewUrl?: string; alt: string }) {
    return previewUrl ? (
        <img src={previewUrl} alt={alt} style={{ width: "100%", borderRadius: 12 }} />
    ) : null;
}

interface MaterialDetailsDialogProps {
    material: LibraryMaterial | null;
    open: boolean;
    isDeleting?: boolean;
    allowDelete?: boolean;
    canEdit?: boolean;
    canDelete?: boolean;
    onClose: () => void;
    onDelete: (material: LibraryMaterial) => void;
    onEdit: () => void;
}

export function MaterialDetailsDialog({
    material,
    open,
    isDeleting = false,
    allowDelete = true,
    canEdit = true,
    canDelete = true,
    onClose,
    onDelete,
    onEdit,
}: MaterialDetailsDialogProps) {
    const [isConfirmDialogOpen, setIsConfirmDialogOpen] = useState(false);

    const imageAttachments = material?.attachments?.filter((item) => item.kind === "image") || [];
    const imageStorageKeys = imageAttachments
        .map((attachment) => attachment.storageKey)
        .filter((key): key is string => Boolean(key));
    const { data: previewUrlByStorageKey } = useFilePreviewUrls(imageStorageKeys);

    if (!material) {
        return null;
    }

    const fileAttachments = material.attachments?.filter((item) => item.kind === "file") || [];

    return (
        <>
            <Dialog
                open={open}
                onClose={isDeleting ? undefined : onClose}
                size="lg"
                richHeader
                flushBody
                title={
                    <div>
                        <h2 style={{ margin: 0, fontSize: 28, fontWeight: 800 }}>{material.title}</h2>
                        <div className="library-card__tags" style={{ marginTop: 8 }}>
                            <MaterialBadge label={`Added ${formatDate(material.createdAt)}`} index={0} />
                            {material.createdBy && <MaterialBadge label={`By ${material.createdBy}`} index={1} />}
                            {(material.tags || []).map((tag, index) => (
                                <MaterialBadge key={tag} label={tag} index={index + 2} />
                            ))}
                            {material.youtubeUrls?.length > 0 && (
                                <MaterialBadge label={`${material.youtubeUrls.length} video(s)`} index={3} />
                            )}
                            {material.links?.length > 0 && (
                                <MaterialBadge label={`${material.links.length} link(s)`} index={4} />
                            )}
                            {imageAttachments.length > 0 && (
                                <MaterialBadge label={`${imageAttachments.length} image(s)`} index={5} />
                            )}
                            {fileAttachments.length > 0 && (
                                <MaterialBadge label={`${fileAttachments.length} file(s)`} index={6} />
                            )}
                            {material.hasText && <MaterialBadge label="Text included" index={7} />}
                        </div>
                        {material.description && (
                            <p style={{ color: "#80808e", margin: "12px 0 0" }}>{material.description}</p>
                        )}
                        <div style={{ display: "flex", gap: 8, marginTop: 8 }}>
                            {canEdit && (
                                <Button variant="ghost" size="sm" onClick={onEdit} disabled={isDeleting}>
                                    <EditOutlinedIcon /> Edit material
                                </Button>
                            )}
                            {allowDelete && canDelete && (
                                <Button
                                    variant="danger"
                                    size="sm"
                                    onClick={() => setIsConfirmDialogOpen(true)}
                                    disabled={isDeleting}
                                >
                                    <DeleteOutlineOutlinedIcon /> Delete material
                                </Button>
                            )}
                        </div>
                    </div>
                }
                footer={
                    <Button variant="ghost" onClick={onClose} disabled={isDeleting}>
                        Close
                    </Button>
                }
            >
                <div className="library-form-body">
                    {material.hasText && (
                        <section>
                            <h3>Text</h3>
                            <pre style={{ whiteSpace: "pre-wrap" }}>{material.text ?? "Loading..."}</pre>
                        </section>
                    )}

                    {material.youtubeUrls?.length > 0 && (
                        <section>
                            <h3><SmartDisplayOutlinedIcon /> Videos</h3>
                            {material.youtubeUrls.map((url) => {
                                const embedUrl = getYoutubeEmbedUrl(url);
                                return (
                                    <article key={url}>
                                        {embedUrl && (
                                            <iframe
                                                src={embedUrl}
                                                title={`YouTube video for ${material.title}`}
                                                allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share"
                                                allowFullScreen
                                                style={{ width: "100%", aspectRatio: "16 / 9", border: 0 }}
                                            />
                                        )}
                                        <a href={url} target="_blank" rel="noreferrer" className="material-link">
                                            {url}
                                        </a>
                                    </article>
                                );
                            })}
                        </section>
                    )}

                    {imageAttachments.length > 0 && (
                        <section>
                            <h3><ImageOutlinedIcon /> Images</h3>
                            <div className="library-grid library-grid--materials">
                                {imageAttachments.map((attachment) => (
                                    <article key={attachment.id || attachment.storageKey} className="library-card">
                                        <StoredImage
                                            previewUrl={attachment.storageKey ? previewUrlByStorageKey?.[attachment.storageKey] : undefined}
                                            alt={attachment.name}
                                        />
                                        <strong>{attachment.name}</strong>
                                        <span>{formatFileSize(attachment.size)}</span>
                                        {attachment.storageKey && (
                                            <button type="button" onClick={() => void openStorageFile(attachment.storageKey!)}>
                                                Open <OpenInNewOutlinedIcon />
                                            </button>
                                        )}
                                    </article>
                                ))}
                            </div>
                        </section>
                    )}

                    {material.links?.length > 0 && (
                        <section>
                            <h3><LinkOutlinedIcon /> Links</h3>
                            {material.links.map((url) => (
                                <p key={url}>
                                    <a href={url} target="_blank" rel="noreferrer" className="material-link">
                                        {url}
                                    </a>
                                </p>
                            ))}
                        </section>
                    )}

                    {fileAttachments.length > 0 && (
                        <section>
                            <h3><DescriptionOutlinedIcon /> Files</h3>
                            {fileAttachments.map((attachment) => (
                                <article key={attachment.id || attachment.storageKey}>
                                    <strong>{attachment.name}</strong>
                                    <p>
                                        {[attachment.mimeType, formatFileSize(attachment.size)].filter(Boolean).join(" • ")}
                                    </p>
                                    {attachment.storageKey && (
                                        <button type="button" onClick={() => void openStorageFile(attachment.storageKey!)}>
                                            Open file <OpenInNewOutlinedIcon />
                                        </button>
                                    )}
                                </article>
                            ))}
                        </section>
                    )}
                </div>
            </Dialog>

            <Dialog
                open={allowDelete && canDelete && isConfirmDialogOpen}
                onClose={() => !isDeleting && setIsConfirmDialogOpen(false)}
                size="sm"
                title="Delete material?"
                flushBody
                footer={
                    <>
                        <Button variant="ghost" onClick={() => setIsConfirmDialogOpen(false)} disabled={isDeleting}>
                            Cancel
                        </Button>
                        <Button variant="danger" onClick={() => onDelete(material)} disabled={isDeleting}>
                            {isDeleting ? "Deleting..." : "Delete permanently"}
                        </Button>
                    </>
                }
            >
                <div className="library-form-body">
                    <p>
                        This action will permanently remove <strong>{material.title}</strong>.
                    </p>
                    <p style={{ color: "#80808e" }}>
                        All related videos, links, text references, and uploaded files will be removed from the library.
                    </p>
                </div>
            </Dialog>
        </>
    );
}
