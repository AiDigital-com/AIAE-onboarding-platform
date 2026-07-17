import { useEffect, useRef, useState, type ReactNode } from "react";

interface Props {
    children: ReactNode;
}

export function ScrollableCardText({ children }: Props) {
    const contentRef = useRef<HTMLDivElement>(null);
    const animationRef = useRef<number | null>(null);
    const targetScrollTopRef = useRef(0);
    const [hasOverflow, setHasOverflow] = useState(false);

    useEffect(() => {
        const element = contentRef.current;
        if (!element) {
            return undefined;
        }

        const updateOverflow = () => {
            setHasOverflow(element.scrollHeight > element.clientHeight + 1);
            targetScrollTopRef.current = element.scrollTop;
        };

        updateOverflow();

        if (typeof ResizeObserver === "undefined") {
            window.addEventListener("resize", updateOverflow);
            return () => window.removeEventListener("resize", updateOverflow);
        }

        const observer = new ResizeObserver(updateOverflow);
        observer.observe(element);
        return () => observer.disconnect();
    }, [children]);

    useEffect(() => {
        return () => {
            if (animationRef.current) {
                window.cancelAnimationFrame(animationRef.current);
            }
        };
    }, []);

    const animateScroll = () => {
        const element = contentRef.current;
        if (!element) {
            animationRef.current = null;
            return;
        }

        const distance = targetScrollTopRef.current - element.scrollTop;
        if (Math.abs(distance) < 0.5) {
            element.scrollTop = targetScrollTopRef.current;
            animationRef.current = null;
            return;
        }

        element.scrollTop += distance * 0.22;
        animationRef.current = window.requestAnimationFrame(animateScroll);
    };

    return (
        <div
            ref={contentRef}
            className={`flashcards-player__scroll${hasOverflow ? " flashcards-player__scroll--overflow" : ""}`}
            onWheel={(event) => {
                if (!hasOverflow) {
                    return;
                }
                event.preventDefault();
                event.stopPropagation();
                const element = event.currentTarget;
                const maxScrollTop = element.scrollHeight - element.clientHeight;
                targetScrollTopRef.current = Math.max(
                    0,
                    Math.min(maxScrollTop, targetScrollTopRef.current + event.deltaY),
                );
                if (!animationRef.current) {
                    animationRef.current = window.requestAnimationFrame(animateScroll);
                }
            }}
        >
            {children}
        </div>
    );
}
