import { ReactNode } from "react";
import { Link } from "react-router-dom";
import AddLinkOutlinedIcon from "@mui/icons-material/AddLinkOutlined";
import ArrowForwardOutlinedIcon from "@mui/icons-material/ArrowForwardOutlined";
import AutoAwesomeOutlinedIcon from "@mui/icons-material/AutoAwesomeOutlined";
import EditNoteOutlinedIcon from "@mui/icons-material/EditNoteOutlined";
import LibraryBooksOutlinedIcon from "@mui/icons-material/LibraryBooksOutlined";
import PlayCircleOutlineOutlinedIcon from "@mui/icons-material/PlayCircleOutlineOutlined";
import SchoolOutlinedIcon from "@mui/icons-material/SchoolOutlined";
import TaskAltOutlinedIcon from "@mui/icons-material/TaskAltOutlined";
import { useAuthMeQuery } from "@/shared/auth/useAuthMeQuery";
import { useHasPermission } from "@/shared/auth/useHasPermission";
import { LoadingBlock } from "@/shared/ui/LoadingBlock";
import "./home.css";

interface Step {
    title: string;
    description: string;
    icon: ReactNode;
}

const builderSteps: Step[] = [
    {
        title: "Add materials to Library",
        description:
            "Collect source files, links, YouTube videos, images, and notes in one place.",
        icon: <AddLinkOutlinedIcon />,
    },
    {
        title: "Generate or paste a lesson",
        description:
            "Create a lesson from selected materials, or add a ready lesson manually.",
        icon: <AutoAwesomeOutlinedIcon />,
    },
    {
        title: "Edit lesson and add activities",
        description:
            "Polish the content, add tags, generate a quiz, and create flashcards.",
        icon: <EditNoteOutlinedIcon />,
    },
    {
        title: "Publish a learning path",
        description:
            "Assemble lessons into roadmaps so the team knows what to complete next.",
        icon: <LibraryBooksOutlinedIcon />,
    },
];

const learnerSteps: Step[] = [
    {
        title: "Subscribe to a lesson",
        description:
            "Open Library, add relevant lessons to My Lessons, or join a roadmap.",
        icon: <SchoolOutlinedIcon />,
    },
    {
        title: "Read and ask questions",
        description:
            "Study the lesson and use the assistant when a concept needs extra context.",
        icon: <PlayCircleOutlineOutlinedIcon />,
    },
    {
        title: "Complete activities",
        description:
            "Review flashcards, pass quizzes, and track your completion progress.",
        icon: <TaskAltOutlinedIcon />,
    },
];

interface StepCardProps {
    step: Step;
    index: number;
    compact?: boolean;
}

function StepCard({ step, index, compact = false }: StepCardProps) {
    return (
        <article
            className={`home-step-card${compact ? " home-step-card--compact" : ""}`}
        >
            <div className="home-step-card__header">
                <div className="home-step-card__icon" aria-hidden="true">
                    {step.icon}
                </div>
                <span className="home-step-card__index">{index + 1}</span>
            </div>
            <div className="home-step-card__body">
                <h3 className="home-step-card__title">{step.title}</h3>
                <p className="home-step-card__description">{step.description}</p>
            </div>
        </article>
    );
}

interface FlowSectionProps {
    title: string;
    subtitle: string;
    steps: Step[];
    actionHref: string;
    actionLabel: string;
    compact?: boolean;
}

function FlowSection({
    title,
    subtitle,
    steps,
    actionHref,
    actionLabel,
    compact = false,
}: FlowSectionProps) {
    return (
        <section className="home-flow">
            <div className="home-flow__header">
                <div className="home-flow__intro">
                    <h2 className="home-flow__title">{title}</h2>
                    <p className="home-flow__subtitle">{subtitle}</p>
                </div>
                <Link className="home-flow__action" to={actionHref}>
                    {actionLabel}
                    <ArrowForwardOutlinedIcon />
                </Link>
            </div>
            <div
                className={`home-flow__grid${compact ? " home-flow__grid--compact" : ""}`}
                style={{ "--home-flow-columns": steps.length } as React.CSSProperties}
            >
                {steps.map((step, index) => (
                    <StepCard key={step.title} step={step} index={index} compact={compact} />
                ))}
            </div>
        </section>
    );
}

/** Onboarding home — hero, quick links, and learner/creator flow sections. */
export function HomePage() {
    const { data: currentUser, isLoading } = useAuthMeQuery();
    const hasPermission = useHasPermission();
    const role = currentUser?.user?.role;
    const hasCreatorRole = role === "admin" || role === "teamlead";
    const canCreateContent = hasCreatorRole && (
        hasPermission("lessons.create") ||
        hasPermission("lessons.manage") ||
        hasPermission("materials.create")
    );

    if (isLoading) {
        return <LoadingBlock label="Loading…" />;
    }

    const displayName =
        currentUser?.user?.name ?? currentUser?.user?.email ?? "there";

    return (
        <div className="home">
            <section className="home-hero">
                <div className="home-hero__layout">
                    <div className="home-hero__content">
                        <div className="home-hero__brand-row">
                            <img
                                className="home-hero__logo"
                                src="/aidlogo.png"
                                alt="AI Onboarding"
                                width={44}
                                height={44}
                            />
                            <span className="home-hero__welcome">
                                {`Welcome, ${displayName}`}
                            </span>
                        </div>

                        <div className="home-hero__copy">
                            <h1 className="home-hero__headline">
                                Build onboarding once, then let people learn it clearly.
                            </h1>
                            <p className="home-hero__lede">
                                Use the platform either as a team lead creating structured
                                lessons, or as a team member completing assigned learning.
                            </p>
                        </div>

                        <div className="home-hero__actions">
                            <Link className="home-hero__btn home-hero__btn--primary" to="/library">
                                <LibraryBooksOutlinedIcon />
                                Open Library
                            </Link>
                            <Link className="home-hero__btn home-hero__btn--secondary" to="/lessons">
                                <SchoolOutlinedIcon />
                                My Lessons
                            </Link>
                        </div>
                    </div>

                    <aside className="home-hero__aside" aria-label="Platform areas">
                        {[
                            ["Library", "Source materials and generated lessons"],
                            ["Lessons", "Your personal learning queue"],
                            ["Roadmaps", "Guided learning paths"],
                        ].map(([label, description]) => (
                            <div key={label} className="home-hero__aside-item">
                                <p className="home-hero__aside-label">{label}</p>
                                <p className="home-hero__aside-description">{description}</p>
                            </div>
                        ))}
                    </aside>
                </div>
            </section>

            <FlowSection
                title="For learners"
                subtitle="Focus on learning: subscribe to lessons, read them, complete activities, and keep progress visible."
                steps={learnerSteps}
                actionHref="/lessons"
                actionLabel="Start learning"
                compact
            />

            {canCreateContent && (
                <FlowSection
                    title="For lesson creators"
                    subtitle="Create the learning content: start from raw materials, generate lessons, refine them, and add activities."
                    steps={builderSteps}
                    actionHref="/library"
                    actionLabel="Create content"
                />
            )}
        </div>
    );
}
