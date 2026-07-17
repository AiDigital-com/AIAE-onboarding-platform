export const roadmapsKeys = {
    all: ["roadmaps"] as const,
    my: () => [...roadmapsKeys.all, "my"] as const,
};
