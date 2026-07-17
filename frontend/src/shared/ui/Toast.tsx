import { useCallback, useEffect, useRef, useState, type MouseEvent } from "react";
import { createPortal } from "react-dom";
import CloseOutlinedIcon from "@mui/icons-material/CloseOutlined";
import "./toast.css";

export type ToastSeverity = "success" | "error" | "warning";

const DEFAULT_AUTO_CLOSE_MS = 3000;
const TOAST_EXIT_MS = 180;

interface ToastProps {
    open: boolean;
    message: string;
    severity?: ToastSeverity;
    autoCloseMs?: number;
    onClose: () => void;
}

export function Toast({
    open,
    message,
    severity = "success",
    autoCloseMs = DEFAULT_AUTO_CLOSE_MS,
    onClose,
}: ToastProps) {
    const [shouldRender, setShouldRender] = useState(open && Boolean(message));
    const [isClosing, setIsClosing] = useState(false);
    const closeTimerRef = useRef<number | null>(null);
    const exitTimerRef = useRef<number | null>(null);
    // Parents typically pass an inline onClose whose identity changes every render.
    // Reading it through a ref keeps the auto-close effect below off that identity,
    // so parent re-renders (e.g. an open dialog) cannot keep resetting the timer.
    const onCloseRef = useRef(onClose);
    onCloseRef.current = onClose;
    const shouldRenderRef = useRef(shouldRender);
    shouldRenderRef.current = shouldRender;

    const clearTimers = useCallback(() => {
        if (closeTimerRef.current !== null) {
            window.clearTimeout(closeTimerRef.current);
            closeTimerRef.current = null;
        }
        if (exitTimerRef.current !== null) {
            window.clearTimeout(exitTimerRef.current);
            exitTimerRef.current = null;
        }
    }, []);

    const closeWithAnimation = useCallback(() => {
        clearTimers();
        setIsClosing(true);
        exitTimerRef.current = window.setTimeout(() => {
            setShouldRender(false);
            setIsClosing(false);
            onCloseRef.current();
            exitTimerRef.current = null;
        }, TOAST_EXIT_MS);
    }, [clearTimers]);

    const handleCloseClick = useCallback((event: MouseEvent<HTMLButtonElement>) => {
        event.preventDefault();
        event.stopPropagation();
        closeWithAnimation();
    }, [closeWithAnimation]);

    useEffect(() => {
        clearTimers();

        if (!open || !message) {
            if (shouldRenderRef.current) {
                setIsClosing(true);
                exitTimerRef.current = window.setTimeout(() => {
                    setShouldRender(false);
                    setIsClosing(false);
                    exitTimerRef.current = null;
                }, TOAST_EXIT_MS);
            }
            return clearTimers;
        }

        setShouldRender(true);
        setIsClosing(false);

        if (autoCloseMs > 0) {
            closeTimerRef.current = window.setTimeout(closeWithAnimation, autoCloseMs);
        }

        return clearTimers;
    }, [autoCloseMs, clearTimers, closeWithAnimation, message, open]);

    if (!shouldRender || !message) {
        return null;
    }

    const toast = (
        <div
            className={[
                "ui-toast",
                `ui-toast--${severity}`,
                isClosing ? "ui-toast--closing" : "",
            ]
                .filter(Boolean)
                .join(" ")}
            role="status"
            aria-live="polite"
        >
            <p className="ui-toast__message">{message}</p>
            <button
                type="button"
                className="ui-toast__close"
                aria-label="Close notification"
                onClick={handleCloseClick}
            >
                <CloseOutlinedIcon />
            </button>
        </div>
    );

    if (typeof document === "undefined") {
        return toast;
    }

    return createPortal(toast, document.body);
}
