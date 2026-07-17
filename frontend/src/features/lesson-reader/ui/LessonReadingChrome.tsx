import { useEffect, useMemo, useRef, useState, type ReactNode } from "react";
import { Link } from "react-router-dom";
import ArrowBackOutlinedIcon from "@mui/icons-material/ArrowBackOutlined";
import ArrowForwardOutlinedIcon from "@mui/icons-material/ArrowForwardOutlined";
import ArticleOutlinedIcon from "@mui/icons-material/ArticleOutlined";
import GridViewOutlinedIcon from "@mui/icons-material/GridViewOutlined";
import NotesOutlinedIcon from "@mui/icons-material/NotesOutlined";
import OndemandVideoOutlinedIcon from "@mui/icons-material/OndemandVideoOutlined";
import { UserAvatar } from "@/shared/ui/UserAvatar";
import type { LessonNavigation, RoadmapLessonContext, TeacherVideoView } from "@/features/lessons/api/types";
import "./lesson-reading-chrome.css";

interface LessonChromeModel {
    id: number;
    title: string;
    description?: string;
    createdBy?: string;
    updatedAt?: string;
    publishedAt?: string;
    createdAt?: string;
    teacherVideo?: TeacherVideoView | null;
}

interface SectionItem {
    id: string;
    label: string;
    level: 2 | 3;
}

function SectionIcon({ index }: { index: number }) {
    if (index % 3 === 1) {
        return <GridViewOutlinedIcon />;
    }
    if (index % 3 === 2) {
        return <NotesOutlinedIcon />;
    }
    return <ArticleOutlinedIcon />;
}

interface Props {
    lesson: LessonChromeModel;
    roadmapContext?: RoadmapLessonContext | null;
    lessonNavigation?: LessonNavigation;
    initialIsCompleted?: boolean;
    onReadingCompleted?: () => void;
    children: ReactNode;
}

function readingProgressStorageKey(lessonId: number): string {
    return `lesson-reading-progress:${lessonId}`;
}

function slugify(value: string, fallback: string): string {
    const slug = value
        .toLowerCase()
        .trim()
        .replace(/[^a-z0-9]+/gi, "-")
        .replace(/^-+|-+$/g, "");
    return slug || fallback;
}

function formatDate(value?: string): string {
    if (!value) {
        return "";
    }
    return new Intl.DateTimeFormat("en", {
        month: "short",
        day: "numeric",
        year: "numeric",
    }).format(new Date(value));
}

export function LessonReadingChrome({
    lesson,
    roadmapContext,
    lessonNavigation,
    initialIsCompleted = false,
    onReadingCompleted,
    children,
}: Props) {
    const contentRef = useRef<HTMLElement>(null);
    const sectionsRef = useRef<SectionItem[]>([]);
    const maxProgressRef = useRef(0);
    const completionSentRef = useRef(false);
    const pendingActiveIdRef = useRef("");
    const pendingScrollTargetRef = useRef<number | null>(null);
    const pendingScrollTimeoutRef = useRef<number | null>(null);
    const [sections, setSections] = useState<SectionItem[]>([]);
    const [activeId, setActiveId] = useState("");
    const [progress, setProgress] = useState(() => {
        if (initialIsCompleted) {
            return 100;
        }
        try {
            const stored = window.localStorage.getItem(readingProgressStorageKey(lesson.id));
            const parsed = stored ? Number(stored) : 0;
            return Number.isFinite(parsed) ? Math.min(100, Math.max(0, parsed)) : 0;
        } catch {
            return 0;
        }
    });

    const authorName = lesson.createdBy || "AI Digital";
    const teacherVideo = lesson.teacherVideo || {};
    const hasTeacherVideo = Boolean(teacherVideo.videoUrl);
    const updatedDate = formatDate(lesson.updatedAt || lesson.publishedAt || lesson.createdAt);
    const eyebrow = roadmapContext
        ? `Lesson - ${String(roadmapContext.lessonNumber).padStart(2, "0")} of ${roadmapContext.title}`
        : "Lesson";
    const sectionItems = useMemo(() => sections.slice(0, 12), [sections]);

    useEffect(() => {
        window.scrollTo({ top: 0, left: 0, behavior: "instant" as ScrollBehavior });
    }, [lesson.id]);

    useEffect(() => {
        const root = contentRef.current;
        if (!root) {
            return undefined;
        }

        // Re-detect on every content DOM change. Lesson HTML is injected via
        // dangerouslySetInnerHTML and swapped again once async storage previews
        // resolve (see useResolvedStorageHtml), which replaces the heading nodes
        // along with the ids assigned here. Without re-detection those ids are
        // lost and sidebar section clicks resolve to nothing. A MutationObserver
        // keeps the section ids in sync with the live DOM.
        const detectSections = () => {
            const headings = Array.from(root.querySelectorAll(".lesson-reader h2, .lesson-reader h3"));
            const usedIds = new Set<string>();
            const items = headings.map((heading, index) => {
                const label = heading.textContent?.trim() || `Section ${index + 1}`;
                let id = heading.id || slugify(label, `section-${index + 1}`);
                let suffix = 2;
                while (usedIds.has(id)) {
                    id = `${id}-${suffix}`;
                    suffix += 1;
                }
                usedIds.add(id);
                heading.id = id;
                return {
                    id,
                    label,
                    level: heading.tagName.toLowerCase() === "h3" ? 3 : 2,
                } as SectionItem;
            });

            sectionsRef.current = items;
            setSections(items);
            setActiveId((current) => current || items[0]?.id || "");
        };

        detectSections();

        // Only childList/subtree — the id assignment above mutates attributes,
        // which we intentionally do not observe to avoid a re-detection loop.
        const observer = new MutationObserver(() => detectSections());
        observer.observe(root, { childList: true, subtree: true });

        return () => observer.disconnect();
    }, [children]);

    useEffect(() => {
        completionSentRef.current = initialIsCompleted;
        if (initialIsCompleted) {
            maxProgressRef.current = 100;
            setProgress(100);
            return;
        }
        try {
            const stored = window.localStorage.getItem(readingProgressStorageKey(lesson.id));
            const parsed = stored ? Number(stored) : 0;
            const restored = Number.isFinite(parsed) ? Math.min(100, Math.max(0, parsed)) : 0;
            maxProgressRef.current = restored;
            setProgress(restored);
        } catch {
            maxProgressRef.current = 0;
            setProgress(0);
        }
    }, [initialIsCompleted, lesson.id]);

    useEffect(() => {
        let animationFrameId: number | null = null;

        const updateReadingState = () => {
            const root = contentRef.current;
            if (!root) {
                return;
            }

            const rect = root.getBoundingClientRect();
            const total = Math.max(1, rect.height - window.innerHeight * 0.72);
            const read = Math.min(Math.max(-rect.top + window.innerHeight * 0.12, 0), total);
            const pageBottomReached =
                window.scrollY + window.innerHeight >= document.documentElement.scrollHeight - 4;
            const nextProgress = pageBottomReached ? 100 : Math.round((read / total) * 100);

            maxProgressRef.current = Math.max(maxProgressRef.current, nextProgress);
            setProgress(maxProgressRef.current);
            try {
                window.localStorage.setItem(
                    readingProgressStorageKey(lesson.id),
                    String(maxProgressRef.current),
                );
            } catch {
                // Ignore storage failures (private mode / quota).
            }
            if (maxProgressRef.current >= 100 && !completionSentRef.current) {
                completionSentRef.current = true;
                onReadingCompleted?.();
            }

            if (pendingActiveIdRef.current && pendingScrollTargetRef.current !== null) {
                const reachedTarget =
                    Math.abs(window.scrollY - pendingScrollTargetRef.current) < 8;
                if (reachedTarget) {
                    pendingActiveIdRef.current = "";
                    pendingScrollTargetRef.current = null;
                } else {
                    setActiveId(pendingActiveIdRef.current);
                    return;
                }
            }

            const currentSections = sectionsRef.current;
            if (currentSections.length > 0) {
                const markerTop = 150;
                const sectionHeadings = currentSections
                    .map((section) => document.getElementById(section.id))
                    .filter(Boolean) as HTMLElement[];
                const currentHeading = sectionHeadings.reduce<HTMLElement | null>((current, heading) => {
                    if (heading.getBoundingClientRect().top <= markerTop) {
                        return heading;
                    }
                    return current;
                }, sectionHeadings[0] ?? null);

                if (currentHeading?.id) {
                    setActiveId(currentHeading.id);
                }
            }
        };

        const scheduleUpdate = () => {
            if (animationFrameId) {
                return;
            }
            animationFrameId = window.requestAnimationFrame(() => {
                animationFrameId = null;
                updateReadingState();
            });
        };

        updateReadingState();
        window.addEventListener("scroll", scheduleUpdate, { passive: true });
        window.addEventListener("resize", scheduleUpdate);

        return () => {
            if (animationFrameId) {
                window.cancelAnimationFrame(animationFrameId);
            }
            if (pendingScrollTimeoutRef.current) {
                window.clearTimeout(pendingScrollTimeoutRef.current);
            }
            window.removeEventListener("scroll", scheduleUpdate);
            window.removeEventListener("resize", scheduleUpdate);
        };
    }, [lesson.id, onReadingCompleted]);

    const scrollToSection = (id: string) => {
        const element = document.getElementById(id);
        if (!element) {
            return;
        }

        setActiveId(id);
        const targetTop = element.getBoundingClientRect().top + window.scrollY - 132;
        const normalizedTargetTop = Math.max(0, targetTop);
        pendingActiveIdRef.current = id;
        pendingScrollTargetRef.current = normalizedTargetTop;

        if (pendingScrollTimeoutRef.current) {
            window.clearTimeout(pendingScrollTimeoutRef.current);
        }

        pendingScrollTimeoutRef.current = window.setTimeout(() => {
            pendingActiveIdRef.current = "";
            pendingScrollTargetRef.current = null;
        }, 900);

        window.scrollTo({ top: normalizedTargetTop, behavior: "smooth" });
    };

    return (
        <div className="lesson-reading-chrome">
            <aside className="lesson-reading-chrome__sidebar">
                <div className="lesson-reading-chrome__sidebar-top">
                    <Link className="lesson-reading-chrome__back-icon" to="/lessons" aria-label="Back to lessons">
                        <ArrowBackOutlinedIcon />
                    </Link>
                    <div className="lesson-reading-chrome__sidebar-meta">
                        <p className="lesson-reading-chrome__sidebar-eyebrow">
                            {roadmapContext
                                ? `Lesson - ${String(roadmapContext.lessonNumber).padStart(2, "0")}`
                                : "Lesson"}
                        </p>
                        <p className="lesson-reading-chrome__sidebar-title">{lesson.title}</p>
                    </div>
                </div>

                <p className="lesson-reading-chrome__sections-label">Sections</p>
                <nav className="lesson-reading-chrome__sections">
                    {hasTeacherVideo && (
                        <button
                            type="button"
                            className={`lesson-reading-chrome__section-btn lesson-reading-chrome__section-btn--video${
                                activeId === "teacher-video" ? " is-active" : ""
                            }`}
                            onClick={() => scrollToSection("teacher-video")}
                        >
                            <OndemandVideoOutlinedIcon />
                            <span className="lesson-reading-chrome__section-label">Teacher video</span>
                        </button>
                    )}
                    {sectionItems.length === 0 ? (
                        <p className="lesson-reading-chrome__sections-empty">Sections will appear here.</p>
                    ) : (
                        sectionItems.map((section, index) => (
                            <button
                                key={section.id}
                                type="button"
                                className={`lesson-reading-chrome__section-btn${
                                    section.id === activeId ? " is-active" : ""
                                }${section.level === 3 ? " lesson-reading-chrome__section-btn--nested" : ""}`}
                                onClick={() => scrollToSection(section.id)}
                            >
                                <SectionIcon index={index} />
                                <span className="lesson-reading-chrome__section-label">{section.label}</span>
                            </button>
                        ))
                    )}
                </nav>

                <div className="lesson-reading-chrome__progress-block">
                    <p className="lesson-reading-chrome__progress-label">Progress</p>
                    <div className="lesson-reading-chrome__progress-track">
                        <div
                            className="lesson-reading-chrome__progress-bar"
                            style={{ width: `${progress}%` }}
                        />
                    </div>
                    <p className="lesson-reading-chrome__progress-value">{progress}% read</p>
                </div>
            </aside>

            <article className="lesson-reading-chrome__article" ref={contentRef}>
                <div className="lesson-reading-chrome__sheet">
                    <Link className="lesson-reading-chrome__back-mobile" to="/lessons">
                        <ArrowBackOutlinedIcon /> Back to My Lessons
                    </Link>

                    <header className="lesson-reading-chrome__hero">
                        <p className="lesson-reading-chrome__eyebrow">{eyebrow}</p>
                        <h1 className="lesson-reading-chrome__title">{lesson.title}</h1>
                        {lesson.description && (
                            <p className="lesson-reading-chrome__description">{lesson.description}</p>
                        )}
                    </header>

                    <div className="lesson-reading-chrome__author-row">
                        <UserAvatar user={{ name: authorName }} size={44} />
                        <div className="lesson-reading-chrome__author-copy">
                            <p className="lesson-reading-chrome__author-name">{authorName}</p>
                            <p className="lesson-reading-chrome__author-meta">
                                {updatedDate ? `Updated ${updatedDate}` : "Lesson author"}
                            </p>
                        </div>
                        <span className="lesson-reading-chrome__internal-badge">Internal</span>
                    </div>

                    {hasTeacherVideo && (
                        <div id="teacher-video" className="lesson-reading-chrome__video">
                            <video
                                src={teacherVideo.videoUrl}
                                poster={teacherVideo.thumbnailUrl || undefined}
                                controls
                                playsInline
                                preload="metadata"
                            />
                        </div>
                    )}

                    {children}

                    <div className="lesson-reading-chrome__nav-row">
                        {lessonNavigation?.previous ? (
                            <Link
                                className="lesson-reading-chrome__nav-btn lesson-reading-chrome__nav-btn--prev"
                                to={`/lessons/${lessonNavigation.previous.id}`}
                            >
                                <ArrowBackOutlinedIcon /> Previous lesson
                            </Link>
                        ) : (
                            <button
                                type="button"
                                className="lesson-reading-chrome__nav-btn lesson-reading-chrome__nav-btn--prev"
                                disabled
                            >
                                <ArrowBackOutlinedIcon /> Previous lesson
                            </button>
                        )}

                        {lessonNavigation?.next ? (
                            <Link
                                className="lesson-reading-chrome__nav-btn lesson-reading-chrome__nav-btn--next"
                                to={`/lessons/${lessonNavigation.next.id}`}
                            >
                                Next lesson <ArrowForwardOutlinedIcon />
                            </Link>
                        ) : (
                            <button
                                type="button"
                                className="lesson-reading-chrome__nav-btn lesson-reading-chrome__nav-btn--next"
                                disabled
                            >
                                Next lesson <ArrowForwardOutlinedIcon />
                            </button>
                        )}
                    </div>
                </div>
            </article>
        </div>
    );
}
