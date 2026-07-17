import { useQuery } from "@tanstack/react-query";
import { apiClient } from "@/shared/api/client";
import { toRoadmapView } from "../types";
import { roadmapsKeys } from "./queryKeys";

/** Enrolled roadmaps for the My Roadmaps page, filtered server-side. */
export function useMyRoadmapsQuery() {
    return useQuery({
        queryKey: roadmapsKeys.my(),
        queryFn: async () => {
            const { data, error } = await apiClient.POST("/api/v1/roadmaps/search", {
                body: { assignedToMe: true, page: 0, size: 100, sort: "createdAt", direction: "desc" },
            });
            if (error) throw error;
            return data?.roadmaps ?? [];
        },
        select: (data) => data.map((roadmap) => toRoadmapView(roadmap)),
    });
}
