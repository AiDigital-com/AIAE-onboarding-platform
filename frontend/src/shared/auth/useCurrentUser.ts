import { useAuthMeQuery } from "./useAuthMeQuery";

/** Current user profile derived from {@link useAuthMeQuery}. */
export function useCurrentUser() {
    const query = useAuthMeQuery();

    return {
        ...query,
        user: query.data?.user ?? null,
    };
}
