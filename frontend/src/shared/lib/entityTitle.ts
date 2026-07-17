/** Soft limit for Material / Lesson / Roadmap titles (input stops here). */
export const MAX_ENTITY_TITLE_LENGTH = 100;

export function clampEntityTitle(title: string): string {
    return title.slice(0, MAX_ENTITY_TITLE_LENGTH);
}
