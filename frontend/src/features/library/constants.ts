import type { LibraryTabDefinition } from "./api/types";

/** Every Library tab definition, in display order, before permission filtering. */
export const ALL_LIBRARY_TABS: LibraryTabDefinition[] = [
    { value: "materials", label: "Materials" },
    { value: "lessons", label: "Lessons" },
    { value: "roadmaps", label: "Roadmaps" },
];

/** Fallback tab when the requested/URL tab is not visible to the current viewer. */
export const DEFAULT_LIBRARY_TAB = "lessons";
