import { useEffect, useState } from "react";
import { fetchFilePreviewUrls } from "@/shared/api/files";

function imageLoadingPlaceholder() {
    const placeholder = document.createElement("span");
    placeholder.className = "lesson-reader__image-loader";
    placeholder.setAttribute("role", "status");
    placeholder.setAttribute("aria-label", "Loading image");

    const spinner = document.createElement("span");
    spinner.className = "lesson-reader__image-loader-spinner";
    spinner.setAttribute("aria-hidden", "true");

    const label = document.createElement("span");
    label.className = "lesson-reader__image-loader-label";
    label.textContent = "Loading image";

    placeholder.append(spinner, label);
    return placeholder;
}

function imageErrorPlaceholder() {
    const placeholder = document.createElement("span");
    placeholder.className = "lesson-reader__image-loader lesson-reader__image-loader--error";
    placeholder.setAttribute("role", "status");
    placeholder.textContent = "Image could not be loaded.";
    return placeholder;
}

function videoErrorPlaceholder() {
    const placeholder = document.createElement("div");
    placeholder.className = "lesson-reader__image-loader lesson-reader__image-loader--error";
    placeholder.setAttribute("role", "status");
    placeholder.textContent = "Video could not be loaded.";
    return placeholder;
}

function preloadImage(src: string) {
    return new Promise<boolean>((resolve) => {
        const image = new Image();
        image.onload = () => resolve(true);
        image.onerror = () => resolve(false);
        image.src = src;
    });
}

/**
 * Rewrites persisted inline storage images/videos/file links to fresh signed preview URLs.
 *
 * Lesson HTML stores the stable storage key in data-storage-key because signed
 * object-store URLs expire. Rendering resolves those keys through the protected
 * backend preview endpoint using the authenticated API client.
 */
export function useResolvedStorageHtml(html: string) {
    const [resolvedHtml, setResolvedHtml] = useState(html);

    useEffect(() => {
        let isActive = true;

        if (!html || typeof window === "undefined") {
            setResolvedHtml(html);
            return () => {
                isActive = false;
            };
        }

        const parser = new DOMParser();
        const document = parser.parseFromString(html, "text/html");
        const images = Array.from(document.querySelectorAll<HTMLImageElement>("img[data-storage-key]"));
        const videos = Array.from(document.querySelectorAll<HTMLVideoElement>("video[data-storage-key]"));
        const links = Array.from(document.querySelectorAll<HTMLAnchorElement>("a[data-storage-key]"));
        const imageStorageKeys = new Set(
            images
                .map((image) => image.getAttribute("data-storage-key")?.trim())
                .filter((storageKey): storageKey is string => Boolean(storageKey)),
        );
        const storageKeys = Array.from(
            new Set(
                [...images, ...videos, ...links]
                    .map((element) => element.getAttribute("data-storage-key")?.trim())
                    .filter((storageKey): storageKey is string => Boolean(storageKey)),
            ),
        );

        if (storageKeys.length === 0) {
            setResolvedHtml(html);
            return () => {
                isActive = false;
            };
        }

        const loadingDocument = parser.parseFromString(html, "text/html");
        loadingDocument.querySelectorAll<HTMLImageElement>("img[data-storage-key]").forEach((image) => {
            image.replaceWith(imageLoadingPlaceholder());
        });
        setResolvedHtml(loadingDocument.body.innerHTML);

        fetchFilePreviewUrls(storageKeys)
            .catch(() => ({}) as Record<string, string>)
            .then((previewUrlByStorageKey) =>
                Promise.all(
                    storageKeys.map(async (storageKey) => {
                        const previewUrl = previewUrlByStorageKey[storageKey] ?? "";
                        if (!previewUrl) {
                            return [storageKey, "", false] as const;
                        }
                        const isLoaded = imageStorageKeys.has(storageKey) ? await preloadImage(previewUrl) : true;
                        return [storageKey, previewUrl, isLoaded] as const;
                    }),
                ),
            )
            .then((entries) => {
                if (!isActive) {
                    return;
                }

                const imageStateByStorageKey = new Map(entries.map(([storageKey, previewUrl, isLoaded]) => (
                    [storageKey, { previewUrl, isLoaded }]
                )));
                images.forEach((image) => {
                    const storageKey = image.getAttribute("data-storage-key")?.trim();
                    const imageState = storageKey ? imageStateByStorageKey.get(storageKey) : null;
                    if (imageState?.previewUrl && imageState.isLoaded) {
                        image.setAttribute("src", imageState.previewUrl);
                        return;
                    }
                    image.replaceWith(imageErrorPlaceholder());
                });
                videos.forEach((video) => {
                    const storageKey = video.getAttribute("data-storage-key")?.trim();
                    const previewUrl = storageKey ? imageStateByStorageKey.get(storageKey)?.previewUrl : "";
                    if (previewUrl) {
                        video.setAttribute("src", previewUrl);
                        return;
                    }
                    video.replaceWith(videoErrorPlaceholder());
                });
                links.forEach((link) => {
                    const storageKey = link.getAttribute("data-storage-key")?.trim();
                    const previewUrl = storageKey ? imageStateByStorageKey.get(storageKey)?.previewUrl : "";
                    if (previewUrl) {
                        link.setAttribute("href", previewUrl);
                        link.setAttribute("target", "_blank");
                        link.setAttribute("rel", "noopener noreferrer");
                        return;
                    }
                    link.removeAttribute("href");
                    link.setAttribute("aria-disabled", "true");
                    link.setAttribute("title", "File could not be loaded.");
                });

                setResolvedHtml(document.body.innerHTML);
            });

        return () => {
            isActive = false;
        };
    }, [html]);

    return resolvedHtml;
}
