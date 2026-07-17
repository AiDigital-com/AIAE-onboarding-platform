type ApiErrorBody = {
    error?: string;
    message?: string;
};

/** Extract a user-visible message from an openapi-fetch error payload. */
export function getApiErrorMessage(error: unknown, fallback: string): string {
    if (!error) {
        return fallback;
    }

    if (typeof error === "string") {
        return error;
    }

    if (error instanceof Error && error.message) {
        return error.message;
    }

    if (typeof error === "object") {
        const body = error as ApiErrorBody;
        if (typeof body.error === "string" && body.error.trim()) {
            return body.error.trim();
        }
        if (typeof body.message === "string" && body.message.trim()) {
            return body.message.trim();
        }
    }

    return fallback;
}
