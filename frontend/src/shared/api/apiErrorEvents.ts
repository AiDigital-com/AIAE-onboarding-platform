export const API_ERROR_EVENT = "aionboarding:api-error";

export interface ApiErrorEventDetail {
    message: string;
}

/** Emits a user-visible API error notification for background requests. */
export function notifyApiError(message: string) {
    if (!message.trim() || typeof window === "undefined") {
        return;
    }

    window.dispatchEvent(
        new CustomEvent<ApiErrorEventDetail>(API_ERROR_EVENT, {
            detail: { message },
        }),
    );
}
