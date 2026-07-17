export const groupsKeys = {
    all: ["groups"] as const,
    orgStats: ["groups", "org-stats"] as const,
    detail: (groupId: number) => [...groupsKeys.all, "detail", groupId] as const,
    members: (groupId: number, search: string | undefined, page: number) =>
        [...groupsKeys.all, "detail", groupId, "members", { search, page }] as const,
    candidateUsers: (groupId: number, forLeads: boolean, search: string | undefined) =>
        [...groupsKeys.all, "detail", groupId, "candidate-users", { forLeads, search }] as const,
    grades: ["grades"] as const,
    gradesAll: ["grades", "all"] as const,
    roadmapAssignments: (roadmapId: number) => ["roadmaps", roadmapId, "group-assignments"] as const,
    groupRoadmapAssignments: (groupId: number) =>
        [...groupsKeys.all, "detail", groupId, "roadmap-assignments"] as const,
};
