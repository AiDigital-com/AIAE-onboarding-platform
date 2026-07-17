import { useEffect } from "react";
import { confetti } from "@tsparticles/confetti";
import CheckCircleOutlineOutlinedIcon from "@mui/icons-material/CheckCircleOutlineOutlined";
import { confettiColors } from "./ConfettiBurst";
import "./roadmap-completion-celebration.css";

function randomInRange(min: number, max: number): number {
    return Math.random() * (max - min) + min;
}

function getCelebrationTitle(roadmaps: Array<{ title: string }>): string {
    if (roadmaps.length === 1) {
        return roadmaps[0].title;
    }

    return `${roadmaps.length} roadmaps completed`;
}

interface Props {
    active?: boolean;
    roadmaps?: Array<{ id: number; title: string }>;
}

export function RoadmapCompletionCelebration({ active = false, roadmaps = [] }: Props) {
    useEffect(() => {
        if (!active) {
            return undefined;
        }

        const duration = 9000;
        const animationEnd = Date.now() + duration;
        const defaults = {
            startVelocity: 34,
            spread: 360,
            ticks: 90,
            zIndex: 2300,
            colors: confettiColors,
            shapes: ["square", "circle", "star"],
            scalar: 1,
            disableForReducedMotion: true,
        };

        const intervalId = window.setInterval(() => {
            const timeLeft = animationEnd - Date.now();

            if (timeLeft <= 0) {
                window.clearInterval(intervalId);
                return;
            }

            const count = Math.max(12, Math.round(70 * (timeLeft / duration)));

            confetti({
                ...defaults,
                count,
                position: { x: randomInRange(10, 30), y: randomInRange(-20, 35) },
            });
            confetti({
                ...defaults,
                count,
                position: { x: randomInRange(70, 90), y: randomInRange(-20, 35) },
            });
        }, 250);

        return () => window.clearInterval(intervalId);
    }, [active]);

    if (!active) {
        return null;
    }

    return (
        <div className="roadmap-celebration" aria-live="polite">
            <article className="roadmap-celebration__card">
                <div className="roadmap-celebration__icon" aria-hidden="true">
                    <CheckCircleOutlineOutlinedIcon />
                </div>
                <p className="roadmap-celebration__eyebrow">Roadmap Completed</p>
                <h2 className="roadmap-celebration__title">{getCelebrationTitle(roadmaps)}</h2>
                <p className="roadmap-celebration__body">
                    Nice work. Every lesson in this roadmap is complete.
                </p>
                <p className="roadmap-celebration__score">100% complete</p>
            </article>
        </div>
    );
}
