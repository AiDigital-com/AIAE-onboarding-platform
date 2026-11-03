package com.aidigital.aionboarding.domain.learning.repositories;

import com.aidigital.aionboarding.domain.learning.entities.UserRoadmap;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface UserRoadmapRepository
    extends JpaRepository<UserRoadmap, UserRoadmap.UserRoadmapId>, UserRoadmapRepositoryCustom {

    List<UserRoadmap> findByIdUserId(Long userId);

    List<UserRoadmap> findByIdRoadmapId(Long roadmapId);

    @Query("""
        SELECT ur
        FROM UserRoadmap ur
        JOIN FETCH ur.user
        WHERE ur.id.roadmapId = :roadmapId
        ORDER BY ur.enrolledAt DESC
        """)
    List<UserRoadmap> findByRoadmapIdWithUser(@Param("roadmapId") Long roadmapId);

    @Query("""
        SELECT ur
        FROM UserRoadmap ur
        WHERE ur.id.userId = :userId
          AND ur.id.roadmapId IN :roadmapIds
        """)
    List<UserRoadmap> findByUserIdAndRoadmapIds(
        @Param("userId") Long userId,
        @Param("roadmapIds") Collection<Long> roadmapIds
    );

    @Query("""
        SELECT ur
        FROM UserRoadmap ur
        WHERE ur.id.userId IN :userIds
          AND ur.id.roadmapId = :roadmapId
        """)
    List<UserRoadmap> findByUserIdsAndRoadmapId(
        @Param("userIds") Collection<Long> userIds,
        @Param("roadmapId") Long roadmapId
    );

    /**
     * Counts enrollments per roadmap in one grouped query, for the assignee count shown on
     * roadmap cards.
     *
     * @param roadmapIds roadmaps to count enrollments for
     * @return one row per roadmap that has at least one enrollment
     */
    @Query("""
        SELECT new com.aidigital.aionboarding.domain.learning.repositories.RoadmapEnrollmentCountProjection(
            ur.id.roadmapId, COUNT(ur))
        FROM UserRoadmap ur
        WHERE ur.id.roadmapId IN :roadmapIds
        GROUP BY ur.id.roadmapId
        """)
    List<RoadmapEnrollmentCountProjection> countByRoadmapIds(@Param("roadmapIds") Collection<Long> roadmapIds);

    /**
     * Loads at most {@code limit} most recently enrolled learners per roadmap in one query, for
     * the avatar stack shown on roadmap cards. Bounded per roadmap by a window function so a
     * roadmap with hundreds of enrollees never loads them all here.
     *
     * @param roadmapIds roadmaps to preview enrollees for
     * @param limit      maximum enrollees per roadmap
     * @return preview rows ordered by roadmap, newest enrollment first
     */
    @Query(value = """
        SELECT roadmap_id AS roadmapId,
               user_id AS userId,
               name,
               email,
               avatar_color AS avatarColor,
               avatar_storage_key AS avatarStorageKey
        FROM (
            SELECT ur.roadmap_id, ur.user_id, u.name, u.email, u.avatar_color, u.avatar_storage_key,
                   ROW_NUMBER() OVER (PARTITION BY ur.roadmap_id ORDER BY ur.enrolled_at DESC, ur.user_id) AS rn
            FROM user_roadmaps ur
            JOIN users u ON u.id = ur.user_id
            WHERE ur.roadmap_id IN (:roadmapIds)
        ) ranked
        WHERE rn <= :limit
        ORDER BY roadmap_id, rn
        """, nativeQuery = true)
    List<RoadmapAssigneePreviewProjection> findAssigneePreviews(
        @Param("roadmapIds") Collection<Long> roadmapIds,
        @Param("limit") int limit
    );

    /**
     * Bulk-deletes the direct roadmap enrollment rows for a set of users and one roadmap.
     *
     * @param userIds   users whose roadmap enrollment is being revoked
     * @param roadmapId roadmap being revoked
     * @return number of deleted roadmap-enrollment rows
     */
    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM UserRoadmap ur WHERE ur.id.userId IN :userIds AND ur.id.roadmapId = :roadmapId")
    int deleteByUserIdsAndRoadmapId(@Param("userIds") Collection<Long> userIds, @Param("roadmapId") Long roadmapId);
}
