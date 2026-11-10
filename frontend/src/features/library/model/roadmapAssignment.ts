import type { components } from "@/shared/api/generated/schema";
import type { AssignableUserV1, LibraryRoadmap } from "../api/types";

export type LearningAssigneeV1 = components["schemas"]["LearningAssigneeV1"];
export type RoadmapGroupAssignmentV1 = components["schemas"]["RoadmapGroupAssignmentV1"];
export type GroupSummaryV1 = components["schemas"]["GroupSummaryV1"];
export type GradeV1 = components["schemas"]["GradeV1"];
export type RoadmapAssigneePreviewV1 = components["schemas"]["RoadmapAssigneePreviewV1"];

/** A person picked in the assign dialog but not yet saved. */
export interface PickedPerson {
    type: "person";
    id: number;
    name: string;
    email: string;
}

/** A team (group) picked in the assign dialog but not yet saved, with its optional grade filter. */
export interface PickedTeam {
    type: "team";
    id: number;
    name: string;
    membersCount: number;
    gradeIds: number[];
    /** Ids of the members the (grade-filtered) team brings, once the preview has resolved. */
    matchedUserIds?: number[];
}

export type PickedItem = PickedPerson | PickedTeam;

/** Key of a staged removal: `p:<userId>` for a person, `t:<groupId>` for a team. */
export type RemovalKey = `p:${number}` | `t:${number}`;

/** How a person currently holds the roadmap; a person can have several sources at once. */
export interface AssigneeSources {
    direct: boolean;
    autoAssigned: boolean;
    viaGroupNames: string[];
}

/** One search result row of the people-or-teams picker. */
export type PickerResult =
    | {
          type: "person";
          user: AssignableUserV1;
          isAssigned: boolean;
          isSelected: boolean;
      }
    | {
          type: "team";
          group: GroupSummaryV1;
          isAssigned: boolean;
          isSelected: boolean;
      };

/**
 * What Save will do, derived from the picked items and staged removals. `newPeopleCount` is the
 * number of distinct people who do not have the roadmap yet, across explicitly picked people and
 * the (grade-filtered) members of every picked team. A team whose members are still being
 * previewed counts as its full size.
 */
export interface AssignmentPlan {
    newPeopleCount: number;
    pickedTeamCount: number;
    removalCount: number;
}

export interface RoadmapAssignPickerProps {
    roadmapId: number;
    query: string;
    picked: PickedItem[];
    results: PickerResult[];
    grades: GradeV1[];
    isSearching: boolean;
    disabled?: boolean;
    onQueryChange: (value: string) => void;
    onPick: (result: PickerResult) => void;
    onUnpick: (index: number) => void;
    onTeamGradesChange: (groupId: number, gradeIds: number[]) => void;
    onTeamPreview: (groupId: number, matchedUserIds: number[] | undefined) => void;
}

export interface RoadmapAssignedListProps {
    /** Assignees the viewer may manage; the backend omits everyone else, including the viewer. */
    assignees: LearningAssigneeV1[];
    /** Every enrolled person, including those not in `assignees`; null/undefined = unknown. */
    totalPeopleCount?: number | null;
    teams: RoadmapGroupAssignmentV1[];
    removing: ReadonlySet<RemovalKey>;
    isLoading: boolean;
    disabled?: boolean;
    highlightedUserId?: number | null;
    onStageRemoval: (key: RemovalKey) => void;
    onUndoRemoval: (key: RemovalKey) => void;
}

export interface LibraryRoadmapsGridProps {
    roadmaps?: LibraryRoadmap[];
    onAssignRoadmap?: (roadmap: LibraryRoadmap) => void;
    onOpenRoadmap?: (roadmap: LibraryRoadmap) => void;
    canAssignLearning?: boolean;
    /** Shows the ⋯ menu with Duplicate on every card (including "View only" roadmaps). */
    canDuplicate?: boolean;
    onDuplicateRoadmap?: (roadmap: LibraryRoadmap) => void;
    /** Id of a just-duplicated roadmap: its card is scrolled into view and briefly highlighted. */
    highlightedRoadmapId?: number | null;
}

export interface RoadmapAssignDialogProps {
    open: boolean;
    roadmap: LibraryRoadmap | null;
    /** Whether the viewer may change the "auto-assign to new users" setting (admins only). */
    canToggleAutoAssign: boolean;
    onClose: () => void;
    onNotify: (message: string, severity?: "success" | "error", undo?: () => void) => void;
}
