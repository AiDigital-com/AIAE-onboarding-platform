/** Debounce applied to the people-or-teams search box before it hits the server. */
export const ASSIGN_SEARCH_DEBOUNCE_MS = 300;

/** People rows requested from the server for one picker query. */
export const ASSIGN_PEOPLE_PAGE_SIZE = 20;

/** Avatars shown in a card's or summary's stack before the "+N" overflow. */
export const ASSIGNEE_AVATAR_STACK_SIZE = 4;

/** Avatars shown in the dialog summary row before the "+N" overflow. */
export const SUMMARY_AVATAR_STACK_SIZE = 3;

export const AUTO_ASSIGN_LABEL = "New users";
export const AUTO_ASSIGN_DESCRIPTION = "Automatically assign to everyone at their first login";
export const AUTO_ASSIGN_BADGE_LABEL = "Auto-assigned to new users";
export const AUTO_ASSIGN_OFF_WARNING =
    "New users won't get this roadmap anymore. People who already have it keep it.";
export const AUTO_ASSIGN_ON_TOAST = "Auto-assign on: new users will get this roadmap at first login";
export const AUTO_ASSIGN_OFF_TOAST = "Auto-assign off";
export const AUTO_ASSIGN_ADMIN_ONLY_HINT = "Only an admin can change this setting";

export const PICKER_PLACEHOLDER = "Search by name, email or team…";
export const PICKER_PLACEHOLDER_MORE = "Add more…";
export const ASSIGNED_FILTER_PLACEHOLDER = "Find in assigned list…";
export const DISCARD_CHANGES_PROMPT = "Discard your unsaved changes?";

export const PRIMARY_ACTION_ASSIGN = "Assign";
export const PRIMARY_ACTION_SAVE = "Save changes";
export const PRIMARY_ACTION_REMOVE = "Remove";
