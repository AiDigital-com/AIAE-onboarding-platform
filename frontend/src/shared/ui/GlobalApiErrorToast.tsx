import { useEffect, useRef, useState } from "react";
import { API_ERROR_EVENT, type ApiErrorEventDetail } from "@/shared/api/apiErrorEvents";
import { Toast } from "./Toast";

const DEDUPE_WINDOW_MS = 2500;

/** Displays background API errors that are not owned by a page-level form handler. */
export function GlobalApiErrorToast() {
    const [message, setMessage] = useState("");
    const lastMessageRef = useRef({ message: "", shownAt: 0 });

    useEffect(() => {
        const handleApiError = (event: Event) => {
            const detail = (event as CustomEvent<ApiErrorEventDetail>).detail;
            const nextMessage = detail?.message?.trim();
            if (!nextMessage) {
                return;
            }

            const now = Date.now();
            const last = lastMessageRef.current;
            if (last.message === nextMessage && now - last.shownAt < DEDUPE_WINDOW_MS) {
                return;
            }

            lastMessageRef.current = { message: nextMessage, shownAt: now };
            setMessage(nextMessage);
        };

        window.addEventListener(API_ERROR_EVENT, handleApiError);
        return () => window.removeEventListener(API_ERROR_EVENT, handleApiError);
    }, []);

    return (
        <Toast
            open={Boolean(message)}
            message={message}
            severity="error"
            onClose={() => setMessage("")}
        />
    );
}
