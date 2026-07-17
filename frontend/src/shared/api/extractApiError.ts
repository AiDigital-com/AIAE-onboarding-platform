/** Pull a human-readable message from an openapi-fetch error payload. */
export function extractApiError(error: unknown, fallback = "Request failed."): string {
    if (!error || typeof error !== "object") {
        return fallback;
    }

    const record = error as Record<string, unknown>;
    if (typeof record.message === "string" && record.message.trim()) {
        return record.message;
    }
    if (typeof record.error === "string" && record.error.trim()) {
        return record.error;
    }

    return fallback;
}

export class BlockedEndpointError extends Error {
    constructor(public readonly endpoint: string) {
        super(`Blocked endpoint: ${endpoint}`);
        this.name = "BlockedEndpointError";
    }
}
