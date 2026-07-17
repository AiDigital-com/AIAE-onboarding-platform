import { useEffect } from "react";
import { confetti } from "@tsparticles/confetti";

export const confettiColors = [
    "#00E676",
    "#10B981",
    "#84CC16",
    "#22D3EE",
    "#00B8FF",
    "#2563EB",
    "#7C3AED",
    "#EC4899",
    "#FF3D71",
    "#F97316",
    "#FACC15",
    "#FFFFFF",
];

interface Props {
    active?: boolean;
}

export function ConfettiBurst({ active = false }: Props) {
    useEffect(() => {
        if (!active) {
            return;
        }

        const launchConfetti = async () => {
            await Promise.all([
                confetti({
                    count: 200,
                    spread: 86,
                    startVelocity: 64,
                    decay: 0.9,
                    gravity: 1,
                    ticks: 240,
                    position: { x: 50, y: 100 },
                    colors: confettiColors,
                    shapes: ["square", "circle"],
                    scalar: 1.05,
                    zIndex: 2200,
                    disableForReducedMotion: true,
                }),
                confetti({
                    count: 200,
                    angle: 70,
                    spread: 58,
                    startVelocity: 56,
                    position: { x: 0, y: 92 },
                    colors: confettiColors,
                    scalar: 0.9,
                    zIndex: 2200,
                    disableForReducedMotion: true,
                }),
                confetti({
                    count: 200,
                    angle: 110,
                    spread: 58,
                    startVelocity: 56,
                    position: { x: 100, y: 92 },
                    colors: confettiColors,
                    scalar: 0.9,
                    zIndex: 2200,
                    disableForReducedMotion: true,
                }),
            ]);
        };

        launchConfetti().catch((error) => {
            console.error("Failed to launch confetti:", error);
        });
    }, [active]);

    return null;
}
