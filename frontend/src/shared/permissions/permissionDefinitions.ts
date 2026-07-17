export interface PermissionDefinitionMeta {
    key: string;
    label: string;
    description?: string;
    group: string;
}

const PERMISSION_GROUPS = [
    {
        title: "Admin",
        permissions: [
            {
                key: "admin.manage_roles",
                label: "Manage roles",
                description: "Assign and remove team lead roles.",
            },
            {
                key: "permissions.manage_teamleads",
                label: "Manage team lead permissions",
                description: "Change permission overrides for team leads.",
            },
            {
                key: "permissions.manage_team_members",
                label: "Manage team member permissions",
                description: "Change permission overrides for members of an owned team.",
            },
        ],
    },
    {
        title: "Teams",
        permissions: [
            {
                key: "teams.manage_members",
                label: "Manage team members",
                description: "Add and remove members in manageable teams.",
            },
        ],
    },
    {
        title: "Materials",
        permissions: [
            { key: "materials.create", label: "Create materials" },
            { key: "materials.edit", label: "Edit materials" },
            { key: "materials.delete", label: "Delete materials" },
        ],
    },
    {
        title: "Lessons",
        permissions: [
            { key: "lessons.create", label: "Create lessons" },
            { key: "lessons.manage", label: "Edit/delete lessons" },
            { key: "lessons.publish_archive", label: "Publish/archive lessons" },
            { key: "lessons.manage_activities", label: "Manage activities" },
            { key: "lessons.manage_assets", label: "Manage lesson assets" },
        ],
    },
    {
        title: "Roadmaps",
        permissions: [
            { key: "roadmaps.create", label: "Create roadmaps" },
            { key: "roadmaps.manage", label: "Edit/delete roadmaps" },
        ],
    },
    {
        title: "Learning",
        permissions: [
            { key: "learning.enroll", label: "Enroll in lessons/roadmaps" },
            { key: "learning.assign", label: "Assign lessons/roadmaps" },
            { key: "learning.complete", label: "Complete lessons/activities" },
            { key: "learning.ask", label: "Ask lesson assistant" },
        ],
    },
] as const;

export const PERMISSION_DEFINITIONS: PermissionDefinitionMeta[] = PERMISSION_GROUPS.flatMap(
    (group) =>
        group.permissions.map((permission) => ({
            ...permission,
            group: group.title,
        })),
);
