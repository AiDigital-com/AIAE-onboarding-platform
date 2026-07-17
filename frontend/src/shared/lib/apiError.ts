/** Extract a user-facing message from an openapi-fetch error payload. */
export function getApiErrorMessage(error: unknown, fallback = "Something went wrong."): string {
    if (error && typeof error === "object") {
        const record = error as Record<string, unknown>;
        if (typeof record.message === "string" && record.message.trim()) {
            return record.message;
        }
        if (typeof record.error === "string" && record.error.trim()) {
            return record.error;
        }
    }

    if (error instanceof Error && error.message.trim()) {
        return error.message;
    }

    return fallback;
}
