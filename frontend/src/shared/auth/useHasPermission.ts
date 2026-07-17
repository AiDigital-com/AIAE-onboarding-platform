import { useCallback } from "react";
import { usePermissionsQuery } from "./usePermissionsQuery";

/** Returns a checker for the authenticated caller's own effective permission keys. */
export function useHasPermission() {
    const { data: permissionsData } = usePermissionsQuery();

    return useCallback(
        (key: string) => Boolean(permissionsData?.permissions.effective?.[key]),
        [permissionsData],
    );
}
