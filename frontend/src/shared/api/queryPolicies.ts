/** Identity + permissions data — fetched once per authenticated session, never refetched on navigation. */
export const SESSION_QUERY_OPTIONS = {
    staleTime: Infinity,
} as const;

/** Long-lived signed media URLs — valid for ~24 h from the server. */
export const SIGNED_MEDIA_QUERY_OPTIONS = {
    staleTime: 20 * 60 * 60 * 1000,
    gcTime: 20 * 60 * 60 * 1000,
    retry: false,
} as const;

/** Lists that change rarely and are cheap to re-serve (tags, assignees, reference data). */
export const REFERENCE_QUERY_OPTIONS = {
    staleTime: 60 * 60 * 1000,
} as const;

/** User-facing collection lists (materials, lessons, roadmaps). */
export const COLLECTION_QUERY_OPTIONS = {
    staleTime: 5 * 60 * 1000,
} as const;

/** Single-resource detail views (lesson reader, activity player). */
export const DETAIL_QUERY_OPTIONS = {
    staleTime: 5 * 60 * 1000,
} as const;

/** Frequently-polled dashboard widgets. */
export const DASHBOARD_QUERY_OPTIONS = {
    staleTime: 60 * 1000,
} as const;
