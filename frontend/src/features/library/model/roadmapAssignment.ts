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
    /** Members matching the grade filter, once the preview has resolved; undefined = whole team. */
    matchedMembersCount?: number;
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
 * What Save will do, derived from the picked items and staged removals. `newPeopleCount` adds
 * explicitly picked people who do not have the roadmap yet and the (grade-filtered) size of each
 * picked team; whether some team members already hold it is only known server-side.
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
    onTeamPreview: (groupId: number, matchedMembersCount: number | undefined) => void;
}

export interface RoadmapAssignedListProps {
    assignees: LearningAssigneeV1[];
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
}

export interface RoadmapAssignDialogProps {
    open: boolean;
    roadmap: LibraryRoadmap | null;
    /** Whether the viewer may change the "auto-assign to new users" setting (admins only). */
    canToggleAutoAssign: boolean;
    onClose: () => void;
    onNotify: (message: string, severity?: "success" | "error", undo?: () => void) => void;
}
